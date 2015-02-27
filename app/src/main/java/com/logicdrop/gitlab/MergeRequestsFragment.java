package com.logicdrop.gitlab;

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

import org.gitlab.api.models.GitlabMergeRequest;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Fragment class for displaying a list of merge requests
 */
public class MergeRequestsFragment extends ListFragment {

    static List<GitlabMergeRequest> mGitlabMergeRequests;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        new FetchMergeRequests().execute();
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
                        new FetchMergeRequests().execute();
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
        Intent intent = new Intent(getActivity(), MergeRequestActivity.class);
        intent.putExtra("location", position);
        intent.putExtra("project_ID", mGitlabMergeRequests.get(position).getProjectId());
        intent.putExtra("merge_request_ID", mGitlabMergeRequests.get(position).getId());
        intent.putExtra("merge_request_IID", mGitlabMergeRequests.get(position).getIid());
        intent.putExtra("title", mGitlabMergeRequests.get(position).getTitle());
        startActivity(intent);
    }

    /**
     * AsyncTask to fetch merge requests
     */
    private class FetchMergeRequests extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            try {
                int projectID = getActivity().getIntent().getIntExtra("project_ID", 0);
                mGitlabMergeRequests = GitLabFragment.mGitLabAPI.getMergeRequests(projectID);
                Collections.reverse(mGitlabMergeRequests);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            setListAdapter(new MergeRequestsAdapter(mGitlabMergeRequests));
        }

    }

    /**
     * Custom adapter to display merge requests
     */
    private class MergeRequestsAdapter extends ArrayAdapter<GitlabMergeRequest> {

        public MergeRequestsAdapter(List<GitlabMergeRequest> gitlabMergeRequest) {
            super(getActivity(), android.R.layout.simple_list_item_1, gitlabMergeRequest);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null)
                convertView = getActivity().getLayoutInflater().inflate(R.layout.list_item_merge_requests, parent, false);

            TextView iIDTV, titleTV, stateTV, authorTV, dateTV;
            iIDTV = (TextView) convertView.findViewById(R.id.i_id_tv);
            titleTV = (TextView) convertView.findViewById(R.id.title_tv);
            stateTV = (TextView) convertView.findViewById(R.id.state_tv);
            authorTV = (TextView) convertView.findViewById(R.id.author_tv);
            dateTV = (TextView) convertView.findViewById(R.id.date_tv);

            GitlabMergeRequest gitlabMergeRequest = mGitlabMergeRequests.get(position);

            iIDTV.setText("#" + gitlabMergeRequest.getIid());
            titleTV.setText(gitlabMergeRequest.getTitle());
            if (gitlabMergeRequest.getState().equals("merged"))
                stateTV.setText("√ MERGED");
            else
                stateTV.setText(gitlabMergeRequest.getSourceBranch() + " > " + gitlabMergeRequest.getTargetBranch());
            authorTV.setText("authored by " + gitlabMergeRequest.getAuthor().getName());
            long difference = (new Date().getTime() - gitlabMergeRequest.getUpdatedAt().getTime())/1000;
            long time;
            if (difference < 60) {
                dateTV.setText("updated less than a minute ago");
            } else if (difference < 3600) {
                time = TimeUnit.SECONDS.toMinutes(difference);
                if (time == 1) dateTV.setText("updated about " + time + " minute ago");
                else dateTV.setText("updated about " + time + " minutes ago");
            } else if (difference < 86400) {
                time = TimeUnit.SECONDS.toHours(difference);
                if (time == 1) dateTV.setText("updated about " + time + " hour ago");
                else dateTV.setText("updated about " + time + " hours ago");
            }  else if (difference < 2678400) {
                time = TimeUnit.SECONDS.toDays(difference);
                if (time == 1) dateTV.setText("updated " + time + " day ago");
                else dateTV.setText("updated " + time + " days ago");
            } else if (difference < 31536000) {
                time = TimeUnit.SECONDS.toDays(difference) / 31;
                if (time == 1) dateTV.setText("updated " + time + " month ago");
                else dateTV.setText("updated " + time + " months ago");
            } else {
                time = TimeUnit.SECONDS.toDays(difference) / 365;
                if (time == 1) dateTV.setText("updated " + time + " year ago");
                else dateTV.setText("updated " + time + " years ago");
            }

            return convertView;
        }
    }

}
