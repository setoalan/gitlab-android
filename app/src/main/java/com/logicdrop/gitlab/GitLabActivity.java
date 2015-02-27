package com.logicdrop.gitlab;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.logicdrop.gitlab.models.Session;

import java.io.IOException;

/**
 * Main/Start activity of application
 */
public class GitLabActivity extends Activity {

    public static boolean loggedIn;
    public static Session sSession;
    private boolean loginPage = false;
    private SharedPreferences sharedPreferences;

    private FragmentManager manager;
    private Fragment fragment;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment);

        if (!isNetworkAvailable()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("No Internet Connection Detected")
                    .setMessage("Go to your Settings on your device and set \"Wi-Fi\" to \"On\".")
                    .setCancelable(false)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            finish();
                        }
                    });
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        } else {
            sSession = new Session();

            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            loggedIn = sharedPreferences.getBoolean("logged_in", false);
            sSession.setHostURL(sharedPreferences.getString("host_URL", null));
            sSession.setPrivateToken(sharedPreferences.getString("private_token", null));

            manager = getFragmentManager();
            fragment = manager.findFragmentById(R.id.fragment_container);
            if (fragment == null) {
                if (!loggedIn) {
                    loginPage = true;
                    invalidateOptionsMenu();
                }
                fragment = (loggedIn) ? new GitLabFragment() : new LoginFragment();
                manager.beginTransaction()
                        .add(R.id.fragment_container, fragment)
                        .commit();
            }
        }
    }

    /**
     * Begin transition after user logs in
     */
    public void onPostLogin() {
        loginPage = false;
        invalidateOptionsMenu();
        fragment = new GitLabFragment();
        manager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    /**
     * Dialog popup to create a new project
     * @param context Interface to global information about an application environment
     */
    private void createProject(final Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final LayoutInflater inflater = getLayoutInflater();
        final View view = inflater.inflate(R.layout.dialog_project, null);
        builder.setView(view)
                .setTitle("Create New Project")
                .setPositiveButton("Create", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        EditText titleET = (EditText) view.findViewById(R.id.project_name_et);
                        EditText descriptionET = (EditText) view.findViewById(R.id.description_et);
                        String title = titleET.getText().toString();
                        String description = descriptionET.getText().toString();
                        if (titleET.getText().toString().equals(""))
                            Toast.makeText(context, "Title Required", Toast.LENGTH_LONG).show();
                        else
                            new PushNewProject(title, description).execute();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {}
                });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * AsyncTask to add new project to GitLab
     */
    private class PushNewProject extends AsyncTask<Void, Void, Void> {

        private final String mTitle, mDescription;

        public PushNewProject(String title, String description) {
            mTitle = title;
            mDescription = description;
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                GitLabFragment.mGitLabAPI.createProject(mTitle, null, mDescription, null, null, null, null, null, null, null, null);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            GitLabFragment gitlabFragment = (GitLabFragment) getFragmentManager().findFragmentById(R.id.fragment_container);
            gitlabFragment.refresh();
        }

    }

    /**
     * User logs out
     */
    private void logout() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("logged_in", false);
        editor.apply();
        loginPage = true;
        invalidateOptionsMenu();
        fragment = new LoginFragment();
        progressDialog.dismiss();
        manager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    /**
     * Check if network is available
     * @return Boolean that states if network is available and connected
     */
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.create_project_m:
                createProject(this);
                break;
            case R.id.logout_m:
                progressDialog = new ProgressDialog(this);
                progressDialog.setMessage("Logging out...");
                progressDialog.show();
                logout();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if(loginPage) {
            menu.findItem(R.id.create_project_m).setVisible(false);
            menu.findItem(R.id.logout_m).setVisible(false);
        } else {
            menu.findItem(R.id.create_project_m).setVisible(true);
            menu.findItem(R.id.logout_m).setVisible(true);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_gitlab_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

}
