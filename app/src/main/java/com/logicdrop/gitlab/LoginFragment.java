package com.logicdrop.gitlab;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.logicdrop.gitlab.services.FetchToken;

/**
 * Fragment class for login screen
 */
public class LoginFragment extends Fragment {

    private String username, password, hostURL, privateToken;
    private EditText usernameET, passwordET, hostURLET, privateTokenET;
    private TextView loginTV;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        usernameET = (EditText) view.findViewById(R.id.username_et);
        passwordET = (EditText) view.findViewById(R.id.password_et);
        hostURLET = (EditText) view.findViewById(R.id.host_url_et);
        privateTokenET = (EditText) view.findViewById(R.id.private_token_et);
        Button signInBTN = (Button) view.findViewById(R.id.sign_in_btn);
        loginTV = (TextView) view.findViewById(R.id.login_tv);

        signInBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (privateTokenET.getVisibility() == View.GONE) {
                    username = usernameET.getText().toString();
                    password = passwordET.getText().toString();
                    hostURL = "http://" + hostURLET.getText().toString();
                    GitLabActivity.sSession.setHostURL(hostURL);
                    editor = sharedPreferences.edit();
                    editor.putString("host_URL", hostURL);
                    editor.apply();
                    InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
                    inputMethodManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
                    new FetchTokenTask().execute();
                } else {
                    hostURL = "http://" + hostURLET.getText().toString();
                    privateToken = privateTokenET.getText().toString();
                    GitLabActivity.sSession.setHostURL(hostURL);
                    GitLabActivity.sSession.setPrivateToken(privateToken);
                    editor = sharedPreferences.edit();
                    editor.putString("host_URL", hostURL);
                    editor.putString("private_token", privateToken);
                    editor.putBoolean("logged_in", true);
                    editor.apply();
                    InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
                    inputMethodManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
                    getFragmentManager().beginTransaction().replace(R.layout.fragment_gitlab, new GitLabFragment());
                    ((GitLabActivity) getActivity()).onPostLogin();
                }

            }
        });

        loginTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (privateTokenET.getVisibility() == View.GONE) {
                    loginTV.setText("Log in using username or email");
                    usernameET.setVisibility(View.GONE);
                    passwordET.setVisibility(View.GONE);
                    privateTokenET.setVisibility(View.VISIBLE);
                } else {
                    loginTV.setText("Log in using private token");
                    usernameET.setVisibility(View.VISIBLE);
                    passwordET.setVisibility(View.VISIBLE);
                    privateTokenET.setVisibility(View.GONE);
                }
            }
        });

        return view;
    }

    /**
     * AsyncTask to fetch user's private token
     */
    private class FetchTokenTask extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... params) {
            return new FetchToken().fetchToken(getActivity(), username, password, hostURL);
        }

        @Override
        protected void onPostExecute(String token) {
            if (token != null) {
                GitLabActivity.sSession.setPrivateToken(token);
                editor = sharedPreferences.edit();
                editor.putString("private_token", token);
                editor.putBoolean("logged_in", true);
                editor.apply();
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(getActivity().getWindow().getCurrentFocus().getWindowToken(), 0);
                getFragmentManager().beginTransaction().replace(R.layout.fragment_gitlab, new GitLabFragment());
                ((GitLabActivity) getActivity()).onPostLogin();
            }
        }

    }

}
