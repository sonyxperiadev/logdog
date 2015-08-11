/**
 * Copyright (c) 2013, Sony Mobile Communications Inc
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the copyright holder nor the names of its
 *       contributors may be used to endorse or promote products derived from
 *       this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * This file is part of logdog.
 */

package logdog;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import logdog.utils.Logger;

public class Prefs {

    // The preferences are stored in ~/.java/.userPrefs/logdog on Ubuntu.
    private static final String PREFS_NOTIFY_USER_NEW_VERSION = "notify_user_new_version";
    private static final String PREFS_LATEST_CHECKED_VERSION = "latest_checked_version";
    private static final String PREFS_SHAPES_IN_CHARTS = "shapes_in_charts";
    private static final String PREFS_WEBBROWSER_SEARCH = "webbrowser_search";

    private Preferences mPrefs;

    public enum Directory {
        LOGDOG_FILES("current_directory"),
        BLACKLIST_FILES("blacklist_directory"),
        LOGSOURCE_FILES("logsource_directory");

        private String mName;

        private Directory(String name) {
            mName = name;
        }

        public String getName() {
            return mName;
        }
    }

    public Prefs() {
        mPrefs = Preferences.userNodeForPackage(logdog.class);
        if (mPrefs == null) {
            System.out.println("Prefs: User preferences could not be created.");
        }
    }

    public boolean ok() {
        return mPrefs != null;
    }

    private boolean okThrow() throws BackingStoreException {
        if (mPrefs == null) {
            throw new BackingStoreException("Prefs: User preferences is not valid");
        }
        return true;
    }

    void sync() {
        if (ok()) {
            try {
                mPrefs.sync();
            } catch (BackingStoreException excep) {
                Logger.logExcep(excep);
            }
        }
    }

    public String getDirectory(Directory directory) {
        if (ok()) {
            return mPrefs.get(directory.getName(), System.getProperty("user.home"));
        }
        return System.getProperty("user.home");
    }

    public void putDirectory(Directory directory, String path) {
        try {
            if (okThrow()) {
                mPrefs.put(directory.getName(), path);
            }
        } catch (BackingStoreException excep) {
            Logger.logExcep(excep);
        }
    }

    String getLatesteCheckedVersion() {
        if (ok()) {
            return mPrefs.get(PREFS_LATEST_CHECKED_VERSION, logdog.sThisVersion);
        }
        return logdog.sThisVersion;
    }

    void putLatesteCheckedVersion(String version) {
        try {
            if (okThrow()) {
                mPrefs.put(PREFS_LATEST_CHECKED_VERSION, version);
            }
        } catch (BackingStoreException excep) {
            Logger.logExcep(excep);
        }
    }

    boolean getNotifyUserNewVersion() {
        if (ok()) {
            return mPrefs.getBoolean(PREFS_NOTIFY_USER_NEW_VERSION, true);
        }
        return false;
    }

    void putNotifyUserNewVersion(boolean notify) {
        try {
            if (okThrow()) {
                mPrefs.putBoolean(PREFS_NOTIFY_USER_NEW_VERSION, notify);
            }
        } catch (BackingStoreException excep) {
            Logger.logExcep(excep);
        }
    }

    public boolean getShapesInCharts() {
        if (ok()) {
            return mPrefs.getBoolean(PREFS_SHAPES_IN_CHARTS, true);
        }
        return false;
    }

    public void putShapesInCharts(boolean shapesInCharts) {
        try {
            if (okThrow()) {
                mPrefs.putBoolean(PREFS_SHAPES_IN_CHARTS, shapesInCharts);
            }
        } catch (BackingStoreException excep) {
            Logger.logExcep(excep);
        }
    }

    public String getWebBrowserSearchString() {
        if (ok()) {
            return mPrefs.get(PREFS_WEBBROWSER_SEARCH, null);
        }
        return null;
    }

    public void putWebBrowserSearchString(String search) {
        try {
            if (okThrow()) {
                mPrefs.put(PREFS_WEBBROWSER_SEARCH, search);
            }
        } catch (BackingStoreException excep) {
            Logger.logExcep(excep);
        }
    }
}
