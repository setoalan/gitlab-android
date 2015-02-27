package com.logicdrop.gitlab.services;

import android.content.Context;
import android.net.Uri;
import android.os.Looper;
import android.widget.Toast;

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
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * HTTP GET request for private token of user
 */
public class FetchToken {

    private Context mContext;

    /**
     * HTTP GET request for private token of user
     * @param context Interface to global information about an application environment
     * @param username User's username
     * @param password User's password
     * @param hostURL Host URL where json requests are called
     * @return Private token
     */
    public String fetchToken(Context context, String username, String password, String hostURL)  {
        mContext = context;
        String url = Uri.parse(hostURL + "/api/v3/session").toString();
        String result = null;

        try {
            final HttpClient httpclient = new DefaultHttpClient();
            final HttpPost httpPost = new HttpPost(url);

            List<NameValuePair> pairs = new ArrayList<NameValuePair>();
            pairs.add(new BasicNameValuePair("login", username));
            pairs.add(new BasicNameValuePair("password", password));
            httpPost.setEntity(new UrlEncodedFormEntity(pairs));

            final HttpResponse response = httpclient.execute(httpPost);
            final StatusLine status = response.getStatusLine();

            if (status.getStatusCode() == HttpStatus.SC_CREATED) {
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
                        Toast.makeText(mContext, "Invalid Login Credentials", Toast.LENGTH_SHORT).show();
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
            Thread thread = new Thread() {
                public void run() {
                    Looper.prepare();
                    Toast.makeText(mContext, "Invalid Host Address URL", Toast.LENGTH_SHORT).show();
                    Looper.loop();
                }
            };
            thread.start();
            return null;
        }

        return deserialize(result);
    }

    /**
     * Deserialize JSON result into a string
     * @param result JSON string
     * @return Requested private token of user
     */
    private String deserialize(String result) {
        String token = null;
        try {
            JSONObject jsonObject = new JSONObject(result);
            token = jsonObject.getString("private_token");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return token;
    }

}
