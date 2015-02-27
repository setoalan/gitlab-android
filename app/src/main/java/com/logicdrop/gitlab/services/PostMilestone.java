package com.logicdrop.gitlab.services;

import android.content.Context;
import android.net.Uri;
import android.widget.Toast;

import com.logicdrop.gitlab.GitLabActivity;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * HTTP POST request for adding a milestone
 */
public class PostMilestone {

    /**
     * HTTP POST request for adding a milestone
     * @param context Interface to global information about an application environment
     * @param projectID ID of project
     * @param title Title of milestone
     * @param description Description of milestone (if applicable)
     * @param dueDate Due date of milestone (if applicable)
     */
    public PostMilestone(Context context, int projectID, String title, String description, GregorianCalendar dueDate) {
        String url = Uri.parse(GitLabActivity.sSession.getHostURL() + "/api/v3/projects/" + projectID
                + "/milestones?private_token=" + GitLabActivity.sSession.getPrivateToken()).toString();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

        try {
            final HttpClient httpclient = new DefaultHttpClient();
            final HttpPost httpPost = new HttpPost(url);

            List<NameValuePair> pairs = new ArrayList<NameValuePair>();
            pairs.add(new BasicNameValuePair("id", Integer.toString(projectID)));
            pairs.add(new BasicNameValuePair("title", title));
            pairs.add(new BasicNameValuePair("description", description));
            pairs.add(new BasicNameValuePair("due_date", dateFormat.format(dueDate.getTime())));
            httpPost.setEntity(new UrlEncodedFormEntity(pairs));

            final HttpResponse response = httpclient.execute(httpPost);
            final StatusLine status = response.getStatusLine();

            if (status.getStatusCode() == HttpStatus.SC_OK) {
                final ByteArrayOutputStream out = new ByteArrayOutputStream();
                try {
                    response.getEntity().writeTo(out);
                    Toast.makeText(context, "Milestone: " + title + " created", Toast.LENGTH_LONG).show();
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
    }

}
