package com.logicdrop.gitlab;

import android.app.ListFragment;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.logicdrop.gitlab.models.Session;

import org.gitlab.api.GitlabAPI;
import org.gitlab.api.models.GitlabProject;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Main fragment that displays lists of user projects
 */
public class GitLabFragment extends ListFragment {

    static GitlabAPI mGitLabAPI;
    private List<GitlabProject> mGitlabProjects;
    private ProgressDialog progressDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        Session mSession = GitLabActivity.sSession;
        mGitLabAPI = new GitlabAPI(mSession.getHostURL(), mSession.getPrivateToken());

        progressDialog = new ProgressDialog(getActivity());
        progressDialog.setMessage("Loading...");
        progressDialog.show();
        new FetchAllProjects().execute();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list, container, false);

        final SwipeRefreshLayout mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_container);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        new FetchAllProjects().execute();
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
        Intent intent = new Intent(getActivity(), ProjectActivity.class);
        intent.putExtra("project_ID", mGitlabProjects.get(position).getId());
        intent.putExtra("title", mGitlabProjects.get(position).getNameWithNamespace());
        startActivity(intent);
    }

    /**
     * Method call when user creates new project and is displayed in the list
     */
    protected void refresh() {
        new FetchAllProjects().execute();
    }

    /**
     * AsyncTask to fetch all projects
     */
    private class FetchAllProjects extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            try {
                mGitlabProjects = mGitLabAPI.getProjects();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            setListAdapter(new ProjectAdapter(mGitlabProjects));
            progressDialog.dismiss();
        }

    }

    /**
     * Custom adapter to display all projects
     */
    private class ProjectAdapter extends ArrayAdapter<GitlabProject> {

        public ProjectAdapter(List<GitlabProject> gitlabProjects) {
            super(getActivity(), android.R.layout.simple_list_item_1, gitlabProjects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) convertView = getActivity().getLayoutInflater().inflate(R.layout.list_item_project, parent, false);

            TextView projectNameTV, lastActivityTV;
            projectNameTV = (TextView) convertView.findViewById(R.id.project_name);
            lastActivityTV = (TextView) convertView.findViewById(R.id.last_activity);

            projectNameTV.setText(mGitlabProjects.get(position).getNameWithNamespace());
            long difference = (new Date().getTime() - mGitlabProjects.get(position).getLastActivity().getTime()) / 1000;
            long time;
            if (difference < 60) {
                lastActivityTV.setText("Last activity: less than a minute ago");
            } else if (difference < 3600) {
                time = TimeUnit.SECONDS.toMinutes(difference);
                if (time == 1) lastActivityTV.setText("Last activity: about " + time + " minute ago");
                else lastActivityTV.setText("Last activity: about " + time + " minutes ago");
            } else if (difference < 86400) {
                time = TimeUnit.SECONDS.toHours(difference);
                if (time == 1) lastActivityTV.setText("Last activity: about " + time + " hour ago");
                else lastActivityTV.setText("Last activity: about " + time + " hours ago");
            }  else if (difference < 2678400) {
                time = TimeUnit.SECONDS.toDays(difference);
                if (time == 1) lastActivityTV.setText("Last activity: " + time + " day ago");
                else lastActivityTV.setText("Last activity: " + time + " days ago");
            } else if (difference < 31536000) {
                time = TimeUnit.SECONDS.toDays(difference) / 31;
                if (time == 1) lastActivityTV.setText("Last activity: " + time + " month ago");
                else lastActivityTV.setText("Last activity: " + time + " months ago");
            } else {
                time = TimeUnit.SECONDS.toDays(difference) / 365;
                if (time == 1) lastActivityTV.setText("Last activity: " + time + " year ago");
                else lastActivityTV.setText("Last activity: " + time + " years ago");
            }

            return convertView;
        }

    }

}
