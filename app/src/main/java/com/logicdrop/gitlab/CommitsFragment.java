package com.logicdrop.gitlab;

import android.app.ProgressDialog;
import android.content.Intent;
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

import org.gitlab.api.models.GitlabCommit;
import org.gitlab.api.models.GitlabMergeRequest;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Fragment class for displaying a list of commits
 */
public class CommitsFragment extends ListFragment {

    private int projectID;
    private String mBranchName = "master";
    static List<GitlabCommit> mGitlabCommits;

    private ProgressDialog progressDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        progressDialog = new ProgressDialog(getActivity());
        progressDialog.setMessage("Loading...");
        progressDialog.show();
        new FetchAllCommits(mBranchName).execute();
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
                        new FetchAllCommits(mBranchName).execute();
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
    public void onResume() {
        super.onResume();
        if (mBranchName != null) getActivity().getActionBar().setSubtitle(mBranchName);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Intent intent = new Intent(getActivity(), CommitActivity.class);
        intent.putExtra("project_ID", projectID);
        intent.putExtra("commit_ID", mGitlabCommits.get(position).getId());
        intent.putExtra("commit_short_ID", mGitlabCommits.get(position).getShortId());
        intent.putExtra("title", mGitlabCommits.get(position).getTitle());
        startActivity(intent);
    }

    /**
     * Method call when changing branches
     * @param branch Branch location in model
     */
    protected void changeBranch(int branch) {
        new FetchAllCommits(ProjectActivity.mGitlabBranches.get(branch).getName()).execute();
    }

    /**
     * AsyncTask for fetching all commits
     */
    private class FetchAllCommits extends AsyncTask<Void, Void, Void> {

        public FetchAllCommits(String branchName) {
            mBranchName = branchName;
            getActivity().getActionBar().setSubtitle(mBranchName);
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                projectID = getActivity().getIntent().getIntExtra("project_ID", 0);
                GitlabMergeRequest gitlabMergeRequest = new GitlabMergeRequest();
                gitlabMergeRequest.setSourceProjectId(projectID);
                gitlabMergeRequest.setSourceBranch(mBranchName);
                mGitlabCommits = GitLabFragment.mGitLabAPI.getCommits(gitlabMergeRequest);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            setListAdapter(new CommitsAdapter(mGitlabCommits));
            progressDialog.dismiss();
        }

    }

    /**
     * Custom adapter for displaying list of commits
     */
    private class CommitsAdapter extends ArrayAdapter<GitlabCommit> {

        public CommitsAdapter(List<GitlabCommit> gitlabCommitList) {
            super(getActivity(), android.R.layout.simple_list_item_1, gitlabCommitList);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null)
                convertView = getActivity().getLayoutInflater().inflate(R.layout.list_item_commits, parent, false);

            TextView shortIDTV, titleTV, authorTV, dateTV;
            shortIDTV = (TextView) convertView.findViewById(R.id.short_id_tv);
            titleTV = (TextView) convertView.findViewById(R.id.title_tv);
            authorTV = (TextView) convertView.findViewById(R.id.author_tv);
            dateTV = (TextView) convertView.findViewById(R.id.date_tv);

            GitlabCommit gitlabCommit = mGitlabCommits.get(position);

            shortIDTV.setText(gitlabCommit.getShortId());
            titleTV.setText(gitlabCommit.getTitle());
            authorTV.setText(gitlabCommit.getAuthorName());
            long difference = (new Date().getTime() - gitlabCommit.getCreatedAt().getTime()) / 1000;
            long time;
            if (difference < 60) {
                dateTV.setText("less than a minute ago");
            } else if (difference < 3600) {
                time = TimeUnit.SECONDS.toMinutes(difference);
                if (time == 1) dateTV.setText("about " + time + " minute ago");
                else dateTV.setText("about " + time + " minutes ago");
            } else if (difference < 86400) {
                time = TimeUnit.SECONDS.toHours(difference);
                if (time == 1) dateTV.setText("about " + time + " hour ago");
                else dateTV.setText("about " + time + " hours ago");
            }  else if (difference < 2678400) {
                time = TimeUnit.SECONDS.toDays(difference);
                if (time == 1) dateTV.setText(time + " day ago");
                else dateTV.setText(time + " days ago");
            } else if (difference < 31536000) {
                time = TimeUnit.SECONDS.toDays(difference) / 31;
                if (time == 1) dateTV.setText(time + " month ago");
                else dateTV.setText(time + " months ago");
            } else {
                time = TimeUnit.SECONDS.toDays(difference) / 365;
                if (time == 1) dateTV.setText(time + " year ago");
                else dateTV.setText(time + " years ago");
            }

            return convertView;
        }

    }

}
