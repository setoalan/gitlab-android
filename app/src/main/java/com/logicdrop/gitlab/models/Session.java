package com.logicdrop.gitlab.models;

/**
 * Model class for a session
 */
public class Session {

    private String hostURL;
    private String privateToken;

    public String getHostURL() {
        return hostURL;
    }

    public void setHostURL(String hostURL) {
        this.hostURL = hostURL;
    }

    public String getPrivateToken() {
        return privateToken;
    }

    public void setPrivateToken(String privateToken) {
        this.privateToken = privateToken;
    }

}
