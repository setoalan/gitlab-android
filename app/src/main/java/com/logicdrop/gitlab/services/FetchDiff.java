package com.logicdrop.gitlab.services;

import android.net.Uri;

import com.logicdrop.gitlab.models.Diff;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * HTTP GET request for diffs of a commit
 */
public class FetchDiff {

    /**
     * HTTP GET request for diffs of a commit
     * @param hostURL Host URL where json requests are called
     * @param projectID ID of project
     * @param commitID ID of commit
     * @param privateToken Private token of user
     * @return ArrayList of all diffs from a commit
     */
    public ArrayList<Diff> fetchDiff(String hostURL, int projectID, String commitID, String privateToken)  {
        String url = Uri.parse(hostURL + "/api/v3/projects/" + projectID + "/repository/commits/" + commitID + "/diff/?private_token=" + privateToken).toString();
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
                throw new IOException(status.getReasonPhrase());
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return deserialize(result);
    }

    /**
     * Deserialize JSON result into a ArrayList of Diff
     * @param result JSON string
     * @return Requested ArrayList of diffs of a commit
     */
    private ArrayList<Diff> deserialize(String result) {
        ArrayList<Diff> diffs = new ArrayList<Diff>();
        try {
            JSONArray jsonArray = new JSONArray(result);
            for (int i = 0; i < jsonArray.length(); i++) {
                Diff diff = new Diff();
                diff.setNewPath(jsonArray.getJSONObject(i).getString("new_path"));
                diff.setDiff(jsonArray.getJSONObject(i).getString("diff"));
                diffs.add(diff);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return diffs;
    }

}
