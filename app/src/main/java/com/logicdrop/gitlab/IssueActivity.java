package com.logicdrop.gitlab;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.logicdrop.gitlab.services.FetchCommit;

import org.gitlab.api.models.GitlabCommit;
import org.gitlab.api.models.GitlabIssue;
import org.gitlab.api.models.GitlabMergeRequest;
import org.gitlab.api.models.GitlabMilestone;
import org.gitlab.api.models.GitlabNote;
import org.gitlab.api.models.GitlabUser;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Activity class for a single issue
 */
public class IssueActivity extends Activity {

    private int projectID, issueID;
    private boolean state, noLabels;
    private GitlabIssue mGitlabIssue;
    private List<GitlabNote> mGitlabNotes;
    private TextView stateTV, titleTV, descriptionTV, milestoneTV, labelsTV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_issue);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        projectID = getIntent().getIntExtra("project_ID", 0);
        issueID = getIntent().getIntExtra("issue_ID", 0);
        int issueIID = getIntent().getIntExtra("issue_IID", 0);
        getActionBar().setTitle("#" + issueIID + " / " + getIntent().getStringExtra("title"));

        int location = getIntent().getIntExtra("location", 0);
        if (location != -1) {
            if (IssuesFragment.mLabel.equals("NONE") && IssuesFragment.mMilestone.equals("NONE")) {
                mGitlabIssue = IssuesFragment.mGitlabIssues.get(location);
            } else if (IssuesFragment.mLabel.equals("NONE") && !IssuesFragment.mMilestone.equals("NONE")) {
                mGitlabIssue = IssuesFragment.mGitlabIssuesMilestone.get(location);
            } else if (!IssuesFragment.mLabel.equals("NONE") && IssuesFragment.mMilestone.equals("NONE")) {
                mGitlabIssue = IssuesFragment.mGitlabIssuesLabel.get(location);
            } else {
                mGitlabIssue = IssuesFragment.mGitlabIssuesBoth.get(location);
            }
        } else {
            mGitlabIssue = new GitlabIssue();
            mGitlabIssue.setProjectId(projectID);
            mGitlabIssue.setId(issueID);
            mGitlabIssue.setIid(issueIID);
            mGitlabIssue.setTitle(getIntent().getStringExtra("title"));
            mGitlabIssue.setState(getIntent().getStringExtra("state"));
            GitlabUser gitlabAuthor = new GitlabUser();
            gitlabAuthor.setName(getIntent().getStringExtra("author_name"));
            mGitlabIssue.setAuthor(gitlabAuthor);
            mGitlabIssue.setDescription(getIntent().getStringExtra("description"));
            GitlabMilestone gitlabMilestone = new GitlabMilestone();
            gitlabMilestone.setTitle(getIntent().getStringExtra("milestone"));
            mGitlabIssue.setMilestone(gitlabMilestone);
            mGitlabIssue.setLabels(getIntent().getStringExtra("labels").split(", "));
            GitlabUser gitlabAssignee = new GitlabUser();
            gitlabAssignee.setName(getIntent().getStringExtra("assignee_name"));
            mGitlabIssue.setAssignee(gitlabAssignee);
        }

        new FetchCommentsTask(this).execute();

        TextView issueIDTV = (TextView) findViewById(R.id.issue_id_tv);
        stateTV = (TextView) findViewById(R.id.state_tv);
        TextView authorTV = (TextView) findViewById(R.id.comment_author_tv);
        titleTV = (TextView) findViewById(R.id.title_tv);
        descriptionTV = (TextView) findViewById(R.id.description_tv);
        milestoneTV = (TextView) findViewById(R.id.milestone_tv);
        labelsTV = (TextView) findViewById(R.id.labels_tv);
        TextView assigneeTV = (TextView) findViewById(R.id.assignee_tv);

        issueIDTV.setText("Issue #" + mGitlabIssue.getIid());
        if (mGitlabIssue.getState().equals("opened")) {
            state = true;
            stateTV.setText("Open");
            stateTV.setTextColor(Color.rgb(0, 200, 0));
        } else if (mGitlabIssue.getState().equals("reopened")) {
            state = true;
            stateTV.setText("Reopened");
            stateTV.setTextColor(Color.rgb(0, 200, 0));
        }  else {
            state = false;
            stateTV.setText("Closed");
            stateTV.setTextColor(Color.RED);
        }
        authorTV.setText("Created by " + mGitlabIssue.getAuthor().getName());
        titleTV.setText(mGitlabIssue.getTitle());
        if (!mGitlabIssue.getDescription().equals("")) {
            descriptionTV.setText("");
            descriptionTV = parseGitlabMarkdown(this, true, mGitlabIssue.getDescription());
        } else {
            descriptionTV.setVisibility(View.GONE);
        }
        if (mGitlabIssue.getMilestone() != null && mGitlabIssue.getMilestone().getTitle() != null) {
            milestoneTV.setVisibility(View.VISIBLE);
            milestoneTV.setText(mGitlabIssue.getMilestone().getTitle());
        } else {
            milestoneTV.setVisibility(View.GONE);
        }
        if (mGitlabIssue.getLabels().length == 0 || mGitlabIssue.getLabels()[0].equals("")) {
            noLabels = true;
            labelsTV.setVisibility(View.GONE);
        } else {
            noLabels = false;
            String labels = "";
            for (String string : mGitlabIssue.getLabels())
                labels += "<" + string + "> ";
            labelsTV.setVisibility(View.VISIBLE);
            labelsTV.setText(labels);
        }
        if (mGitlabIssue.getAssignee() != null)
            assigneeTV.setText("Assigned to " + mGitlabIssue.getAssignee().getName());
        else
            assigneeTV.setText("Unassigned");
    }

    /**
     * AsyncTask to fetch comments of the issue
     */
    private class FetchCommentsTask extends AsyncTask<Void, Void, Void> {

        private final Context mContext;

        public FetchCommentsTask(Context context) {
            mContext = context;
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                mGitlabNotes = GitLabFragment.mGitLabAPI.getNotes(mGitlabIssue);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(final Void aVoid) {
            LinearLayout linearLayout;
            linearLayout = (LinearLayout) findViewById(R.id.comments_ll);
            linearLayout.removeAllViews();

            for (GitlabNote gitlabNote : mGitlabNotes) {
                View viewV = new View(mContext);
                viewV.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
                viewV.setBackgroundColor(Color.BLACK);

                TextView commentHeaderTV = new TextView(mContext);
                commentHeaderTV.setTextSize(18);
                commentHeaderTV.setText("\n" + gitlabNote.getAuthor().getName() + " • ");
                long difference = (new Date().getTime() - gitlabNote.getCreatedAt().getTime()) / 1000;
                long time;
                if (difference < 60) {
                    commentHeaderTV.append("less than a minute ago");
                } else if (difference < 3600) {
                    time = TimeUnit.SECONDS.toMinutes(difference);
                    if (time == 1) commentHeaderTV.append(time + " minute ago");
                    else commentHeaderTV.append(time + " minutes ago");
                } else if (difference < 86400) {
                    time = TimeUnit.SECONDS.toHours(difference);
                    if (time == 1) commentHeaderTV.append(time + " hour ago");
                    else commentHeaderTV.append(time + " hours ago");
                }  else if (difference < 2678400) {
                    time = TimeUnit.SECONDS.toDays(difference);
                    if (time == 1) commentHeaderTV.append(time + " day ago");
                    else commentHeaderTV.append(time + " days ago");
                } else if (difference < 31536000) {
                    time = TimeUnit.SECONDS.toDays(difference) / 31;
                    if (time == 1) commentHeaderTV.append(time + " month ago");
                    else commentHeaderTV.append(time + " months ago");
                } else {
                    time = TimeUnit.SECONDS.toDays(difference) / 365;
                    if (time == 1) commentHeaderTV.append(time + " year ago");
                    else commentHeaderTV.append(time + " years ago");
                }

                TextView commentBodyTV = parseGitlabMarkdown(mContext, false, gitlabNote.getBody());
                commentBodyTV.setTextSize(14);
                commentBodyTV.setPadding(32, 0, 0, 0);

                TextView commentSpaceTV = new TextView(mContext);
                commentSpaceTV.setTextSize(10);
                commentSpaceTV.setText("\n");

                linearLayout.addView(viewV);
                linearLayout.addView(commentHeaderTV);
                linearLayout.addView(commentBodyTV);
                linearLayout.addView(commentSpaceTV);
            }
        }

    }

    /**
     * Parsing strings that have links to issues, merge requests, commits, etc.
     * @param context Interface to global information about an application environment
     * @param descriptionView If the view with the string being parsed is a description view
     * @param comment Comment string
     * @return TextView containing the parsed string
     */
    private TextView parseGitlabMarkdown(final Context context, boolean descriptionView, String comment) {
        TextView textView = new TextView(this);
        if (descriptionView)
            textView = (TextView) findViewById(R.id.description_tv);
        String[] stringArray = comment.split(" ");
        for (final String string : stringArray) {
            if (string.length() == 0)
                continue;
            String stringCopy;
            SpannableString spannableString = new SpannableString(string);
            switch (string.charAt(0)) {
                case '_':
                    textView.setTypeface(null, Typeface.ITALIC);
                    spannableString = new SpannableString(string.substring(1, string.length()));
                    break;
                case '#':
                    if (string.charAt(string.length() - 1) == '_')
                        stringCopy = string.substring(0, string.length() - 1);
                    else
                        stringCopy = string;
                    final String stringFinal = stringCopy;
                    spannableString = makeLinkSpan(stringCopy, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            String linkCopy = stringFinal.substring(1, stringFinal.length());
                            for (GitlabIssue gitlabIssue : IssuesFragment.mGitlabIssues) {
                                if (gitlabIssue.getIid() == Integer.parseInt(linkCopy)) {
                                    new FetchTask(context, '#', linkCopy, gitlabIssue).execute();
                                    break;
                                }
                            }
                        }
                    });
                    break;
                case '!':
                    if (string.charAt(string.length() - 1) == '_')
                        stringCopy = string.substring(0, string.length() - 1);
                    else
                        stringCopy = string;
                    spannableString = makeLinkSpan(stringCopy, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            String linkCopy = string.substring(1, string.length() - 1);
                            for (GitlabMergeRequest gitlabMergeRequest: MergeRequestsFragment.mGitlabMergeRequests) {
                                if (gitlabMergeRequest.getIid() == Integer.parseInt(linkCopy)) {
                                    new FetchTask(context, '!', linkCopy, gitlabMergeRequest).execute();
                                    break;
                                }
                            }
                        }
                    });
                    break;
                case '@':
                    if (string.charAt(string.length() - 1) == '_')
                        stringCopy = string.substring(0, string.length() - 1);
                    else
                        stringCopy = string;
                    spannableString = makeLinkSpan(stringCopy, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Toast.makeText(context, "GitlabUser profiles not accessible", Toast.LENGTH_SHORT).show();
                        }
                    });
                    break;
                default:
                    if (string.equals("closed_") || string.equals("open_") || string.equals("reopened_") || string.equals("removed_")) {
                        spannableString = new SpannableString(string.substring(0, string.length() - 1));
                        break;
                    } else if ((string.length() == 6 || string.length() == 7) && string.matches("^[a-z0-9_]*$") &&
                            Pattern.compile("[0-9]").matcher(string).find()) {
                        // Parses string that have at least one number
                        if (string.charAt(string.length() - 1) == '_')
                            stringCopy = string.substring(0, string.length() - 1);
                        else
                            stringCopy = string;
                        spannableString = makeLinkSpan(stringCopy, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                String linkCopy = string.substring(0, string.length() - 1);
                                new FetchTask(context, ' ', linkCopy, null).execute();
                            }
                        });
                    } else if (string.charAt(string.length() - 1) == '_') {
                        spannableString = new SpannableString(string.substring(0, string.length() - 1));
                    }
            }

            textView.append(spannableString);
            textView.append(" ");
            makeLinksFocusable(textView);
        }

        return textView;
    }

    /**
     * Make a string spannable
     * @param charSequence CharSequence to be spannable
     * @param onClickListener OnClickListener
     * @return SpannableString
     */
    private SpannableString makeLinkSpan(CharSequence charSequence, View.OnClickListener onClickListener) {
        SpannableString spannableString = new SpannableString(charSequence);
        spannableString.setSpan(new ClickableString(onClickListener), 0, charSequence.length(),
                SpannableString.SPAN_INCLUSIVE_EXCLUSIVE);
        return spannableString;
    }

    /**
     * Make a TextView focusable
     * @param textView TextView to be focusable
     */
    private void makeLinksFocusable(TextView textView) {
        MovementMethod movementMethod = textView.getMovementMethod();
        if (movementMethod == null || !(movementMethod instanceof LinkMovementMethod)) {
            if (textView.getLinksClickable())
                textView.setMovementMethod(LinkMovementMethod.getInstance());
        }
    }

    /**
     * Class for a clickable string
     */
    private static class ClickableString extends ClickableSpan {

        private final View.OnClickListener mListener;

        public ClickableString(View.OnClickListener listener) {
            mListener = listener;
        }

        @Override
        public void onClick(View v) {
            mListener.onClick(v);
        }

    }

    /**
     * AsyncTask to fetch the contents of link of parsed strings
     */
    private class FetchTask extends AsyncTask<Void, Void, Object> {

        private final String mLink;
        private final char mType;
        private final Context mContext;
        private final Object mObject;

        public FetchTask(Context context, char type, String link, Object object) {
            mContext = context;
            mType = type;
            mLink = link;
            mObject = object;
        }

        @Override
        protected Object doInBackground(Void... params) {
            switch (mType) {
                case '#':
                    return mObject;
                case '!':
                    return mObject;
                default:
                    return new FetchCommit().fetchCommit(mContext, GitLabActivity.sSession.getHostURL(), projectID, mLink, GitLabActivity.sSession.getPrivateToken());
            }
        }

        @Override
        protected void onPostExecute(Object object) {
            Intent intent;
            if (object == null)
                return;
            switch (mType) {
                case '#':
                    GitlabIssue gitlabIssue = (GitlabIssue) object;
                    intent = new Intent(mContext, IssueActivity.class);
                    intent.putExtra("location", -1);
                    intent.putExtra("project_ID", projectID);
                    intent.putExtra("issue_ID", gitlabIssue.getId());
                    intent.putExtra("issue_IID", gitlabIssue.getIid());
                    intent.putExtra("title", gitlabIssue.getTitle());
                    intent.putExtra("state", gitlabIssue.getState());
                    intent.putExtra("author_name", gitlabIssue.getAuthor().getName());
                    intent.putExtra("description", gitlabIssue.getDescription());
                    if (gitlabIssue.getMilestone() != null)
                        intent.putExtra("milestone", gitlabIssue.getMilestone().getTitle());
                    String labels = "";
                    for (int i=0; i<gitlabIssue.getLabels().length; i++) {
                        labels += gitlabIssue.getLabels()[i];
                        if (i != gitlabIssue.getLabels().length - 1)
                            labels += ", ";
                    }
                    intent.putExtra("labels", labels);
                    if (gitlabIssue.getAssignee() != null)
                        intent.putExtra("assignee_name", gitlabIssue.getAssignee().getName());
                    break;
                case '!':
                    GitlabMergeRequest gitlabMergeRequest = (GitlabMergeRequest) object;
                    intent = new Intent(mContext, MergeRequestActivity.class);
                    intent.putExtra("location", -1);
                    intent.putExtra("project_ID", gitlabMergeRequest.getProjectId());
                    intent.putExtra("merge_request_ID", gitlabMergeRequest.getId());
                    intent.putExtra("merge_request_IID", gitlabMergeRequest.getIid());
                    intent.putExtra("title", gitlabMergeRequest.getTitle());
                    intent.putExtra("state", gitlabMergeRequest.getState());
                    intent.putExtra("author_name", gitlabMergeRequest.getAuthor().getName());
                    intent.putExtra("description", gitlabMergeRequest.getDescription());
                    intent.putExtra("assignee_name", gitlabMergeRequest.getAssignee().getName());
                    break;
                default:
                    GitlabCommit gitlabCommit = (GitlabCommit) object;
                    intent = new Intent(mContext, CommitActivity.class);
                    intent.putExtra("project_ID", projectID);
                    intent.putExtra("commit_ID", gitlabCommit.getId());
                    intent.putExtra("commit_short_ID", gitlabCommit.getShortId());
                    intent.putExtra("title", gitlabCommit.getTitle());
            }
            startActivity(intent);
        }

    }

    /**
     * Dialog popup to edit an issue
     * @param context Interface to global information about an application environment
     */
    private void editIssue(final Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final LayoutInflater inflater = getLayoutInflater();
        final View view = inflater.inflate(R.layout.dialog_issue, null);
        final EditText titleET, descriptionET, labelsET;
        final Spinner milestoneS;
        final CheckBox stateCB;
        titleET = (EditText) view.findViewById(R.id.title_et);
        descriptionET = (EditText) view.findViewById(R.id.description_et);
        milestoneS = (Spinner) view.findViewById(R.id.milestone_s);
        labelsET = (EditText) view.findViewById(R.id.labels_et);
        stateCB = (CheckBox) view.findViewById(R.id.state_cb);
        titleET.setText(mGitlabIssue.getTitle());
        descriptionET.setText(mGitlabIssue.getDescription());
        milestoneS.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, IssuesFragment.mMilestoneTitles));
        if (mGitlabIssue.getMilestone() != null)
            milestoneS.setSelection(IssuesFragment.mMilestoneTitles.indexOf(mGitlabIssue.getMilestone().getTitle()));
        String labels = "";
        for (int i=0; i<mGitlabIssue.getLabels().length; i++) {
            labels += mGitlabIssue.getLabels()[i];
            if (i != mGitlabIssue.getLabels().length - 1)
                labels += ", ";
        }
        labelsET.setText(labels);
        if (state)
            stateCB.setChecked(true);
        else
            stateCB.setChecked(false);

        builder.setView(view)
                .setTitle("Edit Issue")
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        String title = titleET.getText().toString();
                        String description = descriptionET.getText().toString();
                        int milestoneNum = 0;
                        for (GitlabMilestone gitlabMilestone : IssuesFragment.mGitlabMilestones) {
                            if (gitlabMilestone.getTitle().equals(IssuesFragment.mMilestoneTitles.get(milestoneS.getSelectedItemPosition()))) {
                                milestoneNum = gitlabMilestone.getId();
                                break;
                            }
                        }
                        String labels = labelsET.getText().toString();
                        boolean state = stateCB.isChecked();
                        if (titleET.getText().toString().equals(""))
                            Toast.makeText(context, "Title Required", Toast.LENGTH_LONG).show();
                        else
                            new PushEditIssue(context, labels, description, title, state, milestoneNum).execute();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        AlertDialog alertDialog = builder.create();
        alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        alertDialog.show();
    }

    /**
     * AsyncTask to push the changes of an issue to GitLab
     */
    private class PushEditIssue extends AsyncTask<Void, Void, Void> {

        private final Context mContext;
        private final String mLabels, mDescription, mTitle;
        private final int mMilestone;
        private final boolean mState;

        public PushEditIssue(Context context, String labels, String description, String title, boolean state, int milestone) {
            mContext = context;
            mLabels = labels;
            mDescription = description;
            mTitle = title;
            mState = state;
            mMilestone = milestone;
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                // Removing the only label doesn't work properly
                // If the project has no milestones, a comment will appear that the milestone is removed even when there wasn't one
                GitLabFragment.mGitLabAPI.editIssue(projectID, issueID, 0, mMilestone, mLabels, mDescription, mTitle,
                        (mState) ? GitlabIssue.Action.REOPEN : GitlabIssue.Action.CLOSE);
                mGitlabIssue = GitLabFragment.mGitLabAPI.getIssue(projectID, issueID);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (mState) {
                state = true;
                stateTV.setText("Open");
                stateTV.setTextColor(Color.rgb(0, 200, 0));
            } else {
                state = false;
                stateTV.setText("Closed");
                stateTV.setTextColor(Color.RED);
            }
            titleTV.setText(mTitle);
            descriptionTV.setText(mDescription);
            if (mGitlabIssue.getMilestone() != null) {
                if (mMilestone == 0) {
                    milestoneTV.setVisibility(View.GONE);
                } else {
                    milestoneTV.setVisibility(View.VISIBLE);
                    milestoneTV.setText(mGitlabIssue.getMilestone().getTitle());
                }
            } else {
                milestoneTV.setVisibility(View.GONE);
            }
            if (mLabels != null) {
                mGitlabIssue.setLabels(mLabels.split(", "));
                String labels = "";
                for (String string : mGitlabIssue.getLabels()) {
                    labels += "<" + string + "> ";
                    if (string.equals("")) break;
                }
                if (labels.equals("<> ")) {
                    labels = "ERROR";
                    if (!noLabels)
                        Toast.makeText(mContext, "Deleting all labels does not work properly. Please visit website to remove it.",
                                Toast.LENGTH_LONG).show();
                } else {
                    labelsTV.setVisibility(View.VISIBLE);
                }
                labelsTV.setText(labels);
            } else {
                labelsTV.setVisibility(View.GONE);
            }
            new FetchCommentsTask(mContext).execute();
        }

    }

    /**
     * Dialog popup to add a comment to an issue
     * @param context Interface to global information about an application environment
     */
    private void addComment(final Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final LayoutInflater inflater = getLayoutInflater();
        final View view = inflater.inflate(R.layout.dialog_comment, null);
        builder.setView(view)
                .setTitle("Add Comment to Issue")
                .setPositiveButton("Add", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        EditText commentET = (EditText) view.findViewById(R.id.comment_et);
                        String comment = commentET.getText().toString();
                        if (commentET.getText().toString().equals(""))
                            Toast.makeText(context, "Comment Required", Toast.LENGTH_LONG).show();
                        else
                            new PushNewComment(context, comment).execute();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {}
                });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * AsyncTask to push comment to GitLab
     */
    private class PushNewComment extends AsyncTask<Void, Void, Void> {

        private final String mComment;
        private final Context mContext;

        public PushNewComment(Context context, String comment) {
            mContext = context;
            mComment = comment;
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                GitLabFragment.mGitLabAPI.createNote(projectID, issueID, mComment);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            new FetchCommentsTask(mContext).execute();
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.edit_issue_m:
                editIssue(this);
                break;
            case R.id.add_comment_m:
                addComment(this);
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_issue_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

}
