package com.logicdrop.gitlab.services;

import android.content.Context;
import android.net.Uri;
import android.os.Looper;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.gitlab.api.models.GitlabCommit;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * HTTP GET request for a single commit
 */
public class FetchCommit {

    Context mContext;

    /**
     * HTTP GET request for requested commit
     * @param context Interface to global information about an application environment
     * @param hostURL Host URL where json requests are called
     * @param projectID ID of project
     * @param commitID ID of commit
     * @param privateToken Private token of user
     * @return GitlabCommit
     */
    public GitlabCommit fetchCommit(Context context, String hostURL, int projectID, String commitID, String privateToken) {
        mContext = context;
        String url = Uri.parse(hostURL + "/api/v3/projects/" + projectID + "/repository/commits/" + commitID + "?private_token=" + privateToken).toString();
        String result = null;

        try {
            final HttpClient httpclient = new DefaultHttpClient();
            final HttpUriRequest request = new HttpGet(url);
            final HttpResponse response = httpclient.execute(request);
            final StatusLine status = response.getStatusLine();

            if (status.getStatusCode() == HttpStatus.SC_OK) {
                final ByteArrayOutputStream out = new ByteArrayOutputStream();
                try {
                    response.getEntity().writeTo(out);
                    result = out.toString();
                } finally {
                    out.close();
                }
            } else {
                response.getEntity().getContent().close();
                Thread thread = new Thread() {
                    public void run() {
                        Looper.prepare();
                        Toast.makeText(mContext, "Commit Not Found", Toast.LENGTH_SHORT).show();
                        Looper.loop();
                    }
                };
                thread.start();
                return null;
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return deserialize(result);
    }

    /**
     * Deserialize JSON result into a GitlabCommit object
     * @param result JSON string
     * @return Requested GitlabCommit
     */
    private GitlabCommit deserialize(String result) {
        GitlabCommit gitlabCommit = new GitlabCommit();
        try {
            JSONObject jsonObject = new JSONObject(result);
            gitlabCommit.setId(jsonObject.getString("id"));
            gitlabCommit.setShortId(jsonObject.getString("short_id"));
            gitlabCommit.setTitle(jsonObject.getString("title"));
            gitlabCommit.setAuthorName(jsonObject.getString("author_name"));
            gitlabCommit.setAuthorEmail(jsonObject.getString("author_email"));
            gitlabCommit.setCreatedAt(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.000+00:00").parse(jsonObject.getString("created_at")));
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return gitlabCommit;
    }

}
