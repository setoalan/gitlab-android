package com.logicdrop.gitlab;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Toast;

import com.logicdrop.gitlab.services.PostMilestone;

import org.gitlab.api.models.GitlabBranch;
import org.gitlab.api.models.GitlabProject;

import java.io.IOException;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * Activity class that holds all the views of a project
 */
public class ProjectActivity extends FragmentActivity {

    static int projectID;
    static List<GitlabBranch> mGitlabBranches;
    private int currentPage, labelSelected, milestoneSelected, branchSelected;
    private CharSequence[] mBranchNames;
    private ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setTitle(getIntent().getStringExtra("title"));

        projectID = getIntent().getIntExtra("project_ID", 0);
        milestoneSelected = 0;
        labelSelected = 0;

        ProjectAdapter mProjectAdapter = new ProjectAdapter(getSupportFragmentManager());
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mProjectAdapter);
        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

            @Override
            public void onPageSelected(int position) {
                currentPage = position;
                invalidateOptionsMenu();
            }

            @Override
            public void onPageScrollStateChanged(int state) {}
        });

        new FetchBranchesTask().execute();
    }

    /**
     * AsyncTask to fetch all branches of a project
     */
    private class FetchBranchesTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            try {
                GitlabProject gitlabProject = new GitlabProject();
                gitlabProject.setId(projectID);
                mGitlabBranches = GitLabFragment.mGitLabAPI.getBranches(gitlabProject);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            branchSelected = mGitlabBranches.size() - 1;
            mBranchNames = new CharSequence[mGitlabBranches.size()];
            for (int i=0; i< mBranchNames.length; i++)
                mBranchNames[i] = mGitlabBranches.get(i).getName();
        }
    }

    /**
     * Custom adapter to display fragments
     */
    private class ProjectAdapter extends FragmentPagerAdapter {

        public ProjectAdapter(android.support.v4.app.FragmentManager fragmentManager) {
            super(fragmentManager);
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return new CommitsFragment();
                case 1:
                    return new IssuesFragment();
                default:
                    return new MergeRequestsFragment();
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Commits";
                case 1:
                    return "Issues";
                default:
                    return "Merge Requests";
            }
        }

    }

    /**
     * Dialog popup to add an issue
     * @param context Interface to global information about an application environment
     */
    private void addIssue(final Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final LayoutInflater inflater = getLayoutInflater();
        final View view = inflater.inflate(R.layout.dialog_issue, null);
        final EditText titleET = (EditText) view.findViewById(R.id.title_et);
        titleET.setHint("required");
        CheckBox stateCB = (CheckBox) view.findViewById(R.id.state_cb);
        stateCB.setVisibility(View.GONE);
        builder.setView(view)
                .setTitle("Add Issue to Project")
                .setPositiveButton("Add", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        EditText descriptionET, labelsET;
                        descriptionET = (EditText) view.findViewById(R.id.description_et);
                        labelsET = (EditText) view.findViewById(R.id.labels_et);
                        String title = titleET.getText().toString();
                        String description = descriptionET.getText().toString();
                        String labels = labelsET.getText().toString();
                        if (titleET.getText().toString().equals(""))
                            Toast.makeText(context, "Title Required", Toast.LENGTH_LONG).show();
                        else
                            new PushNewIssue(title, description, labels).execute();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {}
                });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * AsyncTask to push new issue to GitLab
     */
    private class PushNewIssue extends AsyncTask<Void, Void, Void> {

        private final String mTitle, mDescription, mLabels;

        public PushNewIssue(String title, String description, String labels) {
            mTitle = title;
            mDescription = description;
            mLabels = labels;
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                GitLabFragment.mGitLabAPI.createIssue(projectID, 0, 0, mLabels, mDescription, mTitle);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            IssuesFragment issuesFragment = (IssuesFragment) getSupportFragmentManager()
                    .findFragmentByTag("android:switcher:" + mViewPager.getId() + ":1");
            issuesFragment.refresh();
        }

    }

    /**
     * Dialog popup to create a milestone
     * @param context Interface to global information about an application environment
     */
    private void createMilestone(final Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final LayoutInflater inflater = getLayoutInflater();
        final View view = inflater.inflate(R.layout.dialog_milestone, null);
        builder.setView(view)
                .setTitle("Create New Milestone")
                .setPositiveButton("Create", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        EditText titleET = (EditText) view.findViewById(R.id.title_et);
                        EditText descriptionET = (EditText) view.findViewById(R.id.description_et);
                        DatePicker dueDateDP = (DatePicker) view.findViewById(R.id.due_date_dp);
                        String title = titleET.getText().toString();
                        String description = descriptionET.getText().toString();
                        GregorianCalendar dueDate = new GregorianCalendar(dueDateDP.getYear(), dueDateDP.getMonth(), dueDateDP.getDayOfMonth());
                        if (titleET.getText().toString().equals(""))
                            Toast.makeText(context, "Title Required", Toast.LENGTH_LONG).show();
                        else
                            new PushNewMilestoneTask(context, title, description, dueDate).execute();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {}
                });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * AsyncTask to push new milestone to GitLab
     */
    private class PushNewMilestoneTask extends AsyncTask<Void, Void, Void> {

        private final Context mContext;
        private final String mTitle, mDescription;
        private final GregorianCalendar mDueDate;

        public PushNewMilestoneTask(Context context, String title, String description, GregorianCalendar dueDate) {
            mContext = context;
            mTitle = title;
            mDescription = description;
            mDueDate = dueDate;
        }

        @Override
        protected Void doInBackground(Void... params) {
            new PostMilestone(mContext, projectID, mTitle, mDescription, mDueDate);
            return null;
        }

    }

    /**
     * Dialog popup to display milestones
     */
    private void displayMilestones() {
        final CharSequence[] milestones = new CharSequence[IssuesFragment.mGitlabMilestones.size() + 1];
        milestones[0] = "NONE";
        for (int i=1; i<milestones.length; i++)
            milestones[i] = IssuesFragment.mGitlabMilestones.get(i - 1).getTitle();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Milestones")
                .setSingleChoiceItems(milestones, milestoneSelected, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        milestoneSelected = which;
                        dialog.dismiss();
                        IssuesFragment issuesFragment = (IssuesFragment) getSupportFragmentManager()
                                .findFragmentByTag("android:switcher:" + mViewPager.getId() + ":1");
                        issuesFragment.displayMilestone(milestones[which]);
                    }
                });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Dialog popup to display labels
     */
    private void displayLabels() {
        final CharSequence[] labels = IssuesFragment.mLabels.toArray(new CharSequence[IssuesFragment.mLabels.size()]);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Labels")
                .setSingleChoiceItems(labels, labelSelected, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        labelSelected = which;
                        dialog.dismiss();
                        IssuesFragment issuesFragment = (IssuesFragment) getSupportFragmentManager()
                                .findFragmentByTag("android:switcher:" + mViewPager.getId() + ":1");
                        issuesFragment.displayLabel(labels[which]);
                    }
                });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Dialog popup to change branch
     */
    private void changeBranch() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Change Branch")
                .setSingleChoiceItems(mBranchNames, branchSelected, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        branchSelected = which;
                        dialog.dismiss();
                        CommitsFragment commitsFragment = (CommitsFragment) getSupportFragmentManager()
                                .findFragmentByTag("android:switcher:" + mViewPager.getId() + ":0");
                        commitsFragment.changeBranch(which);
                    }
                });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.add_issue_m:
                addIssue(this);
                break;
            case R.id.create_milestone_m:
                createMilestone(this);
                break;
            case R.id.milestones_m:
                displayMilestones();
                break;
            case R.id.labels_m:
                displayLabels();
                break;
            case R.id.change_branch_m:
                changeBranch();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (currentPage == 1) {
            menu.findItem(R.id.add_issue_m).setVisible(true);
            menu.findItem(R.id.create_milestone_m).setVisible(true);
            menu.findItem(R.id.milestones_m).setVisible(true);
            menu.findItem(R.id.labels_m).setVisible(true);
        } else {
            menu.findItem(R.id.add_issue_m).setVisible(false);
            menu.findItem(R.id.create_milestone_m).setVisible(false);
            menu.findItem(R.id.milestones_m).setVisible(false);
            menu.findItem(R.id.labels_m).setVisible(false);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_project_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

}
