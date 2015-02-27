package com.logicdrop.gitlab.models;

/**
 * Model class for diffs
 */
public class Diff {

    private String diff;
    private String mNewPath;

    public String getDiff() {
        return diff;
    }

    public void setDiff(String diff) {
        this.diff = diff;
    }

    public String getNewPath() {
        return mNewPath;
    }

    public void setNewPath(String newPath) {
        this.mNewPath = newPath;
    }

}
