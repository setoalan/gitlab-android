package com.logicdrop.gitlab;

import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ListFragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.gitlab.api.models.GitlabIssue;
import org.gitlab.api.models.GitlabMilestone;
import org.gitlab.api.models.GitlabProject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Fragment class for displaying a list of issues
 */
public class IssuesFragment extends ListFragment {

    static String mMilestone, mLabel;
    static List<GitlabIssue> mGitlabIssues, mGitlabIssuesLabel, mGitlabIssuesMilestone, mGitlabIssuesBoth;
    static List<GitlabMilestone> mGitlabMilestones;
    static ArrayList<String> mLabels, mMilestoneTitles;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        mMilestone = "NONE";
        mLabel = "NONE";
        new FetchAllIssues().execute();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list, container, false);

        final SwipeRefreshLayout mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_container);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        new FetchAllIssues().execute();
                        mSwipeRefreshLayout.setRefreshing(false);
                    }
                }, 2000);
            }
        });
        mSwipeRefreshLayout.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        return view;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Intent intent = new Intent(getActivity(), IssueActivity.class);
        intent.putExtra("location", position);
        if (mLabel.equals("NONE") && mMilestone.equals("NONE")) {
            intent.putExtra("project_ID", mGitlabIssues.get(position).getProjectId());
            intent.putExtra("issue_ID", mGitlabIssues.get(position).getId());
            intent.putExtra("issue_IID", mGitlabIssues.get(position).getIid());
            intent.putExtra("title", mGitlabIssues.get(position).getTitle());
        } else if (mLabel.equals("NONE") && !mMilestone.equals("NONE")) {
            intent.putExtra("project_ID", mGitlabIssuesMilestone.get(position).getProjectId());
            intent.putExtra("issue_ID", mGitlabIssuesMilestone.get(position).getId());
            intent.putExtra("issue_IID", mGitlabIssuesMilestone.get(position).getIid());
            intent.putExtra("title", mGitlabIssuesMilestone.get(position).getTitle());
        } else if (!mLabel.equals("NONE") && mMilestone.equals("NONE")) {
            intent.putExtra("project_ID", mGitlabIssuesLabel.get(position).getProjectId());
            intent.putExtra("issue_ID", mGitlabIssuesLabel.get(position).getId());
            intent.putExtra("issue_IID", mGitlabIssuesLabel.get(position).getIid());
            intent.putExtra("title", mGitlabIssuesLabel.get(position).getTitle());
        } else {
            intent.putExtra("project_ID", mGitlabIssuesBoth.get(position).getProjectId());
            intent.putExtra("issue_ID", mGitlabIssuesBoth.get(position).getId());
            intent.putExtra("issue_IID", mGitlabIssuesBoth.get(position).getIid());
            intent.putExtra("title", mGitlabIssuesBoth.get(position).getTitle());
        }
        startActivity(intent);
    }

    /**
     * Method call to refresh the list of issues
     */
    protected void refresh() {
        new FetchAllIssues().execute();
    }

    /**
     * Update list view to view desired milestone
     * @param milestone CharSequence of milestone
     */
    protected void displayMilestone(CharSequence milestone) {
        mMilestone = milestone.toString();
        if (!milestone.equals("NONE") && mLabel.equals("NONE")) {
            mGitlabIssuesMilestone = new ArrayList<GitlabIssue>();
            for (GitlabIssue gitlabIssue : mGitlabIssues) {
                if (gitlabIssue.getMilestone() != null && gitlabIssue.getMilestone().getTitle().equals(mMilestone))
                    mGitlabIssuesMilestone.add(gitlabIssue);
            }
            setListAdapter(new IssuesAdapter(mGitlabIssuesMilestone));
        } else if (!milestone.equals("NONE") && !mLabel.equals("NONE")) {
            mGitlabIssuesBoth = new ArrayList<GitlabIssue>();
            for (GitlabIssue gitlabIssue : mGitlabIssues) {
                if (gitlabIssue.getMilestone() != null && gitlabIssue.getMilestone().getTitle().equals(mMilestone)) {
                    for (String string : gitlabIssue.getLabels()) {
                        if (string.equals(mLabel))
                            mGitlabIssuesBoth.add(gitlabIssue);
                    }
                }
            }
            setListAdapter(new IssuesAdapter(mGitlabIssuesBoth));
        } else if (milestone.equals("NONE") && !mLabel.equals("NONE")) {
            mGitlabIssuesLabel = new ArrayList<GitlabIssue>();
            for (GitlabIssue gitlabIssue : mGitlabIssues) {
                for (String string : gitlabIssue.getLabels()) {
                    if (string.equals(mLabel))
                        mGitlabIssuesLabel.add(gitlabIssue);
                }
            }
            setListAdapter(new IssuesAdapter(mGitlabIssuesLabel));
        } else {
            setListAdapter(new IssuesAdapter(mGitlabIssues));
        }
    }

    /**
     * Update list view to view desired label
     * @param label CharSequence of label
     */
    protected void displayLabel(CharSequence label) {
        mLabel = label.toString();
        if (!mLabel.equals("NONE") && mMilestone.equals("NONE")) {
            mGitlabIssuesLabel = new ArrayList<GitlabIssue>();
            for (GitlabIssue gitlabIssue : mGitlabIssues) {
                for (String string : gitlabIssue.getLabels()) {
                    if (string.equals(mLabel))
                        mGitlabIssuesLabel.add(gitlabIssue);
                }
            }
            setListAdapter(new IssuesAdapter(mGitlabIssuesLabel));
        } else if (!mLabel.equals("NONE") && !mMilestone.equals("NONE")) {
            mGitlabIssuesBoth = new ArrayList<GitlabIssue>();
            for (GitlabIssue gitlabIssue : mGitlabIssues) {
                if (gitlabIssue.getMilestone() != null && gitlabIssue.getMilestone().getTitle().equals(mMilestone)) {
                    for (String string : gitlabIssue.getLabels()) {
                        if (string.equals(mLabel))
                            mGitlabIssuesBoth.add(gitlabIssue);
                    }
                }
            }
            setListAdapter(new IssuesAdapter(mGitlabIssuesBoth));
        } else if (mLabel.equals("NONE") && !mMilestone.equals("NONE")) {
            mGitlabIssuesMilestone = new ArrayList<GitlabIssue>();
            for (GitlabIssue gitlabIssue : mGitlabIssues) {
                if (gitlabIssue.getMilestone() != null && gitlabIssue.getMilestone().getTitle().equals(mMilestone))
                    mGitlabIssuesMilestone.add(gitlabIssue);
            }
            setListAdapter(new IssuesAdapter(mGitlabIssuesMilestone));
        } else {
            setListAdapter(new IssuesAdapter(mGitlabIssues));
        }
    }

    protected void displayBoth(CharSequence milestone, CharSequence label) {
        mMilestone = milestone.toString();
        mLabel = label.toString();
        mGitlabIssuesBoth = new ArrayList<GitlabIssue>();
        for (GitlabIssue gitlabIssue : mGitlabIssues) {
            if (gitlabIssue.getMilestone() != null && gitlabIssue.getMilestone().getTitle().equals(mMilestone)) {
                for (String string : gitlabIssue.getLabels()) {
                    if (string.equals(mLabel))
                        mGitlabIssuesBoth.add(gitlabIssue);
                }
            }
        }
        setListAdapter(new IssuesAdapter(mGitlabIssuesLabel));
    }

    /**
     * AsyncTask to fetch all issues
     */
    private class FetchAllIssues extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            try {
                int projectID = getActivity().getIntent().getIntExtra("project_ID", 0);
                ProjectActivity.projectID = projectID;
                GitlabProject gitlabProject = new GitlabProject();
                gitlabProject.setId(projectID);
                mGitlabIssues = GitLabFragment.mGitLabAPI.getIssues(gitlabProject);
                mGitlabMilestones = GitLabFragment.mGitLabAPI.getMilestones(projectID);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            mLabels = new ArrayList<String>();
            mLabels.add("NONE");
            for (GitlabIssue gitlabIssue : mGitlabIssues) {
                for (String string : gitlabIssue.getLabels()) {
                    if (!mLabels.contains(string))
                        mLabels.add(string);
                }
            }
            mMilestoneTitles = new ArrayList<String>();
            mMilestoneTitles.add("NONE");
            for (GitlabMilestone gitlabMilestone : mGitlabMilestones)
                mMilestoneTitles.add(gitlabMilestone.getTitle());
            if (mLabel.equals("NONE") && mMilestone.equals("NONE")) {
                setListAdapter(new IssuesAdapter(mGitlabIssues));
            } else if (mLabel.equals("NONE") && !mMilestone.equals("NONE")) {
                displayMilestone(mMilestone);
            } else if (!mLabel.equals("NONE") && mMilestone.equals("NONE")) {
                displayLabel(mLabel);
            } else {
                displayBoth(mLabel, mMilestone);
            }
        }

    }

    /**
     * Custom adapter to display issues
     */
    private class IssuesAdapter extends ArrayAdapter<GitlabIssue> {

        private final List<GitlabIssue> mGitlabIssuesData;

        public IssuesAdapter(List<GitlabIssue> gitlabIssues) {
            super(getActivity(), android.R.layout.simple_list_item_1, gitlabIssues);
            mGitlabIssuesData = gitlabIssues;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) convertView = getActivity().getLayoutInflater().inflate(R.layout.list_item_issues, parent, false);

            TextView issueIDTV, titleTV, stateTV, labelsTV, milestoneTV, assigneeTV, updateTV;
            issueIDTV = (TextView) convertView.findViewById(R.id.issue_id_tv);
            titleTV = (TextView) convertView.findViewById(R.id.title_tv);
            stateTV = (TextView) convertView.findViewById(R.id.state_tv);
            labelsTV = (TextView) convertView.findViewById(R.id.labels_tv);
            milestoneTV = (TextView) convertView.findViewById(R.id.milestone_tv);
            assigneeTV = (TextView) convertView.findViewById(R.id.assignee_tv);
            updateTV = (TextView) convertView.findViewById(R.id.update_tv);

            GitlabIssue gitlabIssue = mGitlabIssuesData.get(position);

            issueIDTV.setText("#"+ gitlabIssue.getIid());
            titleTV.setText(gitlabIssue.getTitle());
            if (gitlabIssue.getState().equals("opened") || gitlabIssue.getState().equals("reopened")) {
                if (gitlabIssue.getState().equals("opened"))
                    stateTV.setText("Open");
                else
                    stateTV.setText("Reopened");
                stateTV.setTextColor(Color.rgb(0, 200, 0));
            } else {
                stateTV.setText("Closed");
                stateTV.setTextColor(Color.RED);
            }

            if (gitlabIssue.getMilestone() != null) {
                milestoneTV.setVisibility(View.VISIBLE);
                milestoneTV.setText(gitlabIssue.getMilestone().getTitle());
            } else {
                milestoneTV.setVisibility(View.GONE);
            }
            if (gitlabIssue.getLabels().length != 0) {
                String labels = "";
                for (String string : gitlabIssue.getLabels())
                    labels += "<" + string + "> ";
                labelsTV.setVisibility(View.VISIBLE);
                labelsTV.setText(labels);
            } else {
                labelsTV.setVisibility(View.GONE);
            }
            if (gitlabIssue.getAssignee() != null)
                assigneeTV.setText("assigned to " + gitlabIssue.getAssignee().getName());
            else
                assigneeTV.setText("unassigned");
            long difference = (new Date().getTime() - gitlabIssue.getUpdatedAt().getTime()) / 1000;
            long time;
            if (difference < 60) {
                updateTV.setText("updated less than a minute ago");
            } else if (difference < 3600) {
                time = TimeUnit.SECONDS.toMinutes(difference);
                if (time == 1) updateTV.setText("updated about " + time + " minute ago");
                else updateTV.setText("updated " + time + " minutes ago");
            } else if (difference < 86400) {
                time = TimeUnit.SECONDS.toHours(difference);
                if (time == 1) updateTV.setText("updated about " + time + " hour ago");
                else updateTV.setText("updated " + time + " hours ago");
            }  else if (difference < 2678400) {
                time = TimeUnit.SECONDS.toDays(difference);
                if (time == 1) updateTV.setText("updated about " + time + " day ago");
                else updateTV.setText("updated " + time + " days ago");
            } else if (difference < 31536000) {
                time = TimeUnit.SECONDS.toDays(difference) / 31;
                if (time == 1) updateTV.setText("updated about " + time + " month ago");
                else updateTV.setText("updated " + time + " months ago");
            } else {
                time = TimeUnit.SECONDS.toDays(difference) / 365;
                if (time == 1) updateTV.setText("updated " + time + " year ago");
                else updateTV.setText("updated " + time + " years ago");
            }

            return convertView;
        }

    }

}
