package com.logicdrop.gitlab;

import android.app.ListActivity;
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.logicdrop.gitlab.services.FetchDiff;
import com.logicdrop.gitlab.models.Diff;

import java.util.ArrayList;

/**
 * Activity class for a single commit
 */
public class CommitActivity extends ListActivity {

    private int projectID, textSize = 14;
    private String commitID;
    private ArrayList<Diff> mDiffs;
    private CommitAdapter commitAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_commit);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setTitle(getIntent().getStringExtra("commit_short_ID") + " / " + getIntent().getStringExtra("title"));

        projectID = getIntent().getIntExtra("project_ID", 0);
        commitID = getIntent().getStringExtra("commit_ID");

        new FetchDiffTask(this).execute();
    }

    /**
     * AsyncTask for fetching the diffs of a commit
     */
    private class FetchDiffTask extends AsyncTask<Void, Void, ArrayList<Diff>> {

        private final Context mContext;

        public FetchDiffTask(Context context) {
            mContext = context;
        }

        @Override
        protected ArrayList<Diff> doInBackground(Void... params) {
            return new FetchDiff().fetchDiff(GitLabActivity.sSession.getHostURL(), projectID, commitID, GitLabActivity.sSession.getPrivateToken());
        }

        @Override
        protected void onPostExecute(ArrayList<Diff> diffs) {
            mDiffs = diffs;
            commitAdapter = new CommitAdapter(mContext, mDiffs);
            setListAdapter(commitAdapter);
        }

    }

    /**
     * Custom adapter for displaying the diffs of a commit
     */
    private class CommitAdapter extends ArrayAdapter<Diff> {

        public CommitAdapter(Context context, ArrayList<Diff> data) {
            super(context, android.R.layout.simple_list_item_1, data);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) convertView = getLayoutInflater().inflate(R.layout.list_item_commit, parent, false);

            TextView newPathTV, diffTV;
            newPathTV = (TextView) convertView.findViewById(R.id.new_path_tv);
            diffTV = (TextView) convertView.findViewById(R.id.diff_tv);

            if (textSize >= 8 && textSize <= 32) {
                newPathTV.setTextSize(textSize + 4);
                diffTV.setTextSize(textSize);
            }

            newPathTV.setText(mDiffs.get(position).getNewPath());
            String diffLines[] = mDiffs.get(position).getDiff().split("\\r?\\n");
            diffTV.setText("");
            for (String diffLine : diffLines) {
                if (diffLine.startsWith("+") && !diffLine.startsWith("++")) {
                    Spannable spannable = new SpannableString(diffLine);
                    spannable.setSpan(new ForegroundColorSpan(Color.rgb(0, 200, 0)), 0, diffLine.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    diffTV.append(spannable);
                    diffTV.append("\n");
                } else if (diffLine.startsWith("-") && !diffLine.startsWith("--")) {
                    Spannable spannable = new SpannableString(diffLine);
                    spannable.setSpan(new ForegroundColorSpan(Color.RED), 0, diffLine.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    diffTV.append(spannable);
                    diffTV.append("\n");
                } else {
                    diffTV.append(diffLine + "\n");
                }
            }

            return convertView;
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_commit_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.zoom_out_m:
                if (textSize > 8) {
                    textSize -= 2;
                    commitAdapter.notifyDataSetChanged();
                }
                break;
            case R.id.zoom_in_m:
                if (textSize < 20) {
                    textSize += 2;
                    commitAdapter.notifyDataSetChanged();
                }
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return super.onOptionsItemSelected(item);
    }

}
