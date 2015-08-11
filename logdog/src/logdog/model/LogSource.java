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

package logdog.model;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.IllegalFormatException;
import java.util.regex.Pattern;

import logdog.model.BlackList.BlackListListener;
import logdog.utils.Logger;
import logdog.utils.Utils;
import logdog.Prefs;
import logdog.logdog;;

public class LogSource {

    /*------ Static class members and methods ------*/

    public static int LOGCAT_MAIN_AND_SYSTEM = 0;
    public static int LOGCAT_MAIN = 1;
    public static int LOGCAT_SYSTEM = 2;
    public static int LOGCAT_EVENTS = 3;
    public static int LOGCAT_RADIO = 4;
    public static int LOGCAT_ALL = 5;
    public static int FILE = 6;
//    public static int LOGCAT_KERNEL = 5;

    protected static final String[] sSourceName = {
        "logcat_main_and_system",
        "logcat_main",
        "logcat_system",
        "logcat_events",
        "logcat_radio",
        "logcat_all",
        "File"   // Always last since skipped when populating log source menu in ChartView
    };
    public static int COUNT = 7;
    private static ArrayList<LogSource> sLogSources = new ArrayList<LogSource>(COUNT); // capacity

    public static String[] getSourceNames() {
        return sSourceName;
    }

    protected static String sLogCatTSRegExp = "^(\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}.\\d{3})";
    // This is the position in a log line when using -v threadtime
    // where the log tag starts i.e. the timestamp, pid and tid are
    // skipped.
    private static final int sThreadtimeLogTagStartPos = 33;

    protected static SimpleDateFormat sTSFormat = new SimpleDateFormat("MM-dd HH:mm:ss.SSS");

    public static LogSource findLogSourceByName(String sourceName) {
        // Look if we already got a LogSource:
        synchronized (sLogSources)  {
            for (LogSource logSource : sLogSources) {
                if (logSource.getName().equals(sourceName)) {
                    return logSource;
                }
            }
        }
        return null;
    }

     /**
     * Find and allocate LogSource from the given log source name. If
     * found then start it if 'start' is true.
     *
     * @param sourceName
     *
     * @return null if the name is invalid else the desired LogSource
     */
    static public LogSource findLogSource(String sourceName, boolean start) {

        // Look if we already got a LogSource:
        synchronized (sLogSources)  {
            for (LogSource logSource : sLogSources) {
                if (logSource.getName().equals(sourceName)) {
                    return logSource;
                }
            }
        }

        // We haven't, create the LogSource if the name is valid:
        // The FILE LogSource should not be here since it is created per file.
        LogSource logSource = null;
        if (sourceName.equals(sSourceName[LOGCAT_MAIN_AND_SYSTEM])) {
            logSource = new LS_LogCatmainAndSystem();
        } else if (sourceName.equals(sSourceName[LOGCAT_MAIN])) {
            logSource = new LS_LogCatMain();
        } else if (sourceName.equals(sSourceName[LOGCAT_SYSTEM])) {
            logSource = new LS_LogCatSystem();
        } else if (sourceName.equals(sSourceName[LOGCAT_EVENTS])) {
            logSource = new LS_LogCatEvents();
        } else if (sourceName.equals(sSourceName[LOGCAT_RADIO])) {
            logSource = new LS_LogCatRadio();
        } else if (sourceName.equals(sSourceName[LOGCAT_ALL])) {
            logSource = new LS_LogCatAll();
        }

        if (logSource != null) {
            synchronized (sLogSources)  {
                sLogSources.add(logSource);
                if (start) {
                    logSource.start();
                }
            }
            return logSource;
        }

        return null;
    }

    public static void startLogCatMainAndSystem() {
        // This will start the source if it's not already running.
        findLogSource(sSourceName[LOGCAT_MAIN_AND_SYSTEM], true);
    }

    public static void stopAll() {
        synchronized (sLogSources)  {
            for (LogSource logSource : sLogSources) {
                logSource.stop();
            }
        }
    }

    public static void saveAllToFile(boolean saveToFile) {
        synchronized (sLogSources)  {
            for (LogSource logSource : sLogSources) {
                logSource.setSaveToFile(saveToFile);
            }
        }
    }

    public static void addLifeListener(LogSourceLifeListener listener) {
        synchronized (mLifeListeners) {
            if (listener != null && !mLifeListeners.contains(listener)) {
                mLifeListeners.add(listener);
            }
        }
    }

    public static void removeLifeListener(LogSourceLifeListener listener) {
        synchronized (mLifeListeners) {
            if (listener != null && mLifeListeners.contains(listener)) {
                mLifeListeners.remove(listener);
            }
        }
    }

    public static void addFeedListener(LogSourceFeedListener listener) {
        synchronized (mFeedListeners) {
            if (listener != null && !mFeedListeners.contains(listener)) {
                mFeedListeners.add(listener);
            }
        }
    }

    public static void removeFeedListener(LogSourceFeedListener listener) {
        synchronized (mFeedListeners) {
            if (listener != null && mFeedListeners.contains(listener)) {
                mFeedListeners.remove(listener);
            }
        }
    }

    public static String removeToLogTag(String line) {
        int lineLen = line.length();
        // Start past the timestamp, pid and tid and loop until the
        // log tag ends:
        boolean foundTagEnd = false;
        int index = sThreadtimeLogTagStartPos;
        for (; index < lineLen; ++index) {
            char ch = line.charAt(index);
            if ((foundTagEnd && ch == ' ') || (ch == '\n' || ch == '\r')) {
                break;
            } else if (ch == ':') {
                foundTagEnd = true;
            }
        }
        if (index == lineLen) {
            return null;
        }
        return line.substring(index).trim();
    }

    public static String parseToRegExp(String line) {
        int lineLen = line.length();
        StringBuilder sb = new StringBuilder((int) (lineLen * 1.5f));

        sb.append(".*?");

        for (int index = sThreadtimeLogTagStartPos; index < lineLen; ++index) {
            char ch = line.charAt(index);
            if (ch == '\n' || ch == '\r') {
                break;
            }
            switch (ch) {
            case '$':
            case '(':
            case ')':
            case '*':
            case '+':
            case '-':
            case '.':
            case '?':
            case '[':
            case '\\':
            case ']':
            case '^':
            case '{':
            case '}':
            case '|':
                sb.append("\\");
            }
            sb.append(ch);
        }
        sb.append("$");
        return sb.toString();
    }

    public static String parseToLogTag(String line) {
        int lineLen = line.length();
        StringBuilder sb = new StringBuilder(lineLen);

        sb.append(".*?");

        for (int index = sThreadtimeLogTagStartPos; index < lineLen; ++index) {
            char ch = line.charAt(index);
            if (ch == ':') {
                sb.append(ch);
                break;
            }
            if (ch == '\n' || ch == '\r') {
                break;
            }
            sb.append(ch);
        }
        sb.append(".*?");
        return sb.toString();
    }

    /**
     * Create a suitable URI to pass to OpenGrok.
     *
     * @param line This must be a log line without the time stamp, pid, tid and log tag!
     *
     * @return
     */
    public static URI createSearchURI(String line) {
        // Get a format string for the url to OpenGrok. It must
        // contain a %s for the actual search string retrieved from
        // 'line'. Something like this:
        // "http://opengrok.../search?q=%s&project=YOUR_OPENGROK_PROJECT";
        //TODO Cache the search expression?
        Prefs prefs = new Prefs();
        String searchFormat = prefs.getWebBrowserSearchString();
        if (Utils.emptyString(searchFormat) || !searchFormat.contains("%s")) {
            return null;
        }
        int lineLen = line.length();

        // Copy characters until first digit is found:
        StringBuilder sb = new StringBuilder(lineLen);
        for (int index = 0; index < lineLen; ++index) {
            char ch = line.charAt(index);
            if (Character.isDigit(ch)) {
                break;
            }
            sb.append(ch);
        }

        if (sb.length() == 0) {
            return null;
        }

        try {
            String str = sb.toString().trim();
            String searchString = URLEncoder.encode("\"" + str + "\"", "UTF-8");
            if (Utils.emptyString(searchString)) {
                return null;
            }
            return new URI(String.format(searchFormat, searchString));
        } catch (UnsupportedEncodingException excep) {
            Logger.logExcep(excep);
        } catch (URISyntaxException excep) {
            Logger.logExcep(excep);
        } catch (IllegalFormatException excep) {
            Logger.logExcep(excep);
        }
        return null;
    }

    /**
     * Control if all LogSources should feed log lines to the
     * LogSourceListeners.
     *
     * @param feed
     */
    public static void setFeedListeners(boolean feed) {
        synchronized (sLogSources)  {
            for (LogSource logSource : sLogSources) {
                logSource.mFeeding = feed;
            }
        }
    }


    /*------ Object members and methods ------*/

    // If true the log source thread is automatically created when the
    // first LogLineListener is added. If false (used by FileLogSource)
    // the log source is started manually:
    protected volatile boolean mActive = true;

    // If true then only one attempt to execute the thread command is
    // done (used by FileLogSource):
    protected boolean mOneShot;

    // Set to false to temporary pause passing log lines to the
    // LogSourceListeners:
    private volatile boolean mFeeding = true;

    protected final String mName;
    private final String mRunCmdLine;
    private Thread mThread;
    private volatile Process mProcess;
    private ArrayList<LogSourceListener> mListeners = new ArrayList<LogSourceListener>(2);
    private static ArrayList<LogSourceLifeListener> mLifeListeners =
        new ArrayList<LogSourceLifeListener>(2);
    private static ArrayList<LogSourceFeedListener> mFeedListeners =
        new ArrayList<LogSourceFeedListener>(2);
    private FileDumper mFileDumper;
    private BlackList mBlackList = new BlackList(this);
    private LogSourceTriggerList mTriggerList = new LogSourceTriggerList();

    private Color greenColor = new Color(64, 135, 64);
    private Color redColor = new Color(255, 0, 0);
    private Color orangeColor = new Color(255, 135, 64);
    private Color blueColor = new Color(64, 16, 159);

    /**
     * Constructor.
     *
     * @param name
     * @param runCmdline
     *
     * @return
     */
    public LogSource(String name, String runCmdline) {
        assert !Utils.emptyString(name) : "LogSource: 'name' cannot be empty";
        assert !Utils.emptyString(runCmdline) : "LogSource: 'commandline' cannot be empty";
        mName = name;
        mRunCmdLine = runCmdline;
        mFileDumper = new FileDumper(mName);
        if (logdog.DEBUG) {
            Logger.log(String.format("LogSource '%s': c-tor: mRunCmdLine=%s", mName, mRunCmdLine));
        }
    }

    public void addListener(LogSourceListener listener) {
        synchronized (mListeners) {
            if (listener != null && !mListeners.contains(listener)) {
                mListeners.add(listener);

                // Start the LogSource on a separate thread if the
                // first listener and the LogSource is active.
                // 'FileLogSource' is not active when created,
                // thus start() have to be called manually.
                if (mActive && mListeners.size() == 1 && mThread == null) {
                    start();
                }
            }
        }
    }

    public void removeListener(LogSourceListener listener) {
        synchronized (mListeners) {
            if (listener != null && mListeners.contains(listener)) {
                mListeners.remove(listener);

                // Skip this to prevent restarting the log source when
                // unregistering the last LogLineMatcher while editing:
                // if (mLLMs.size() == 0) {
                //     stop();
                // }
            }
        }
    }

    public LogSourceListener hasListenerOfType(Class<?> clazz) {
        for (LogSourceListener listener : mListeners) {
            if (listener.getClass() == clazz) {
                return listener;
            }
        }
        return null;
    }

    public String getName() {
        return mName;
    }

    private void setSaveToFile(boolean saveToFile) {
        if (mFileDumper.isDumping()) {
            removeListener(mFileDumper);
            mFileDumper.stop();
        }
        if (saveToFile) {
            mFileDumper.start();
            addListener(mFileDumper);
        }
    }

    public void setActive(boolean active) {
        mActive = active;
    }

    public boolean isAlive() {
        return mThread != null && mThread.isAlive();
    }

    /**
     * //TODO Make this logsource specific??
     *
     * @param tsString
     *
     * @return
     */
    public static Date getDate(String tsString) {
        Date date = null;
        try {
            date = sTSFormat.parse(tsString);
        } catch (ParseException excep) {
            Logger.logExcep(excep);
        }
        return date;
    }

    public Color getLogLineColor(String logLine) {
        // Because we use '-threadtime' in logcat the type of logline
        // will be at index 31.
        if (logLine.length() > 31) {
            switch(logLine.charAt(31))  {
            case 'I':
                return greenColor;
            case 'W':
                return orangeColor;
            case 'E':
                return redColor;
            case 'D':
                return blueColor;
            }
        }
        return Color.white;
    }

    /**
     * Add the timestamp part of the regexp first and then compile the regexp.
     *
     * @param regExp
     *
     * @return
     */
    public static Pattern compileRegExp(String regExp) {
        return Pattern.compile(sLogCatTSRegExp + regExp);
    }

    private void destroyProcess() {
        if (mProcess != null) {
            mProcess.destroy();
            mProcess = null;
        }
    }

    /**
     * Create and start thread that reads lines from the log source.
     *
     * @return
     */
    protected boolean start() {
        if (mThread != null) {
            Logger.log(String.format("LogSource '%s': start() already called i.e. mThread != null",
                                     mName));
            return false;
        }

        mThread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (logdog.DEBUG) {
                    Logger.log(String.format("LogSource '%s': entering thread", mName));
                }

                synchronized (mLifeListeners) {
                    for (LogSourceLifeListener listener : mLifeListeners) {
                        listener.onStarted(LogSource.this);
                    }
                }

                mProcess = null;
                while (!mThread.isInterrupted()) {  // does not reset the interrupt flag
                    try {
                        if (logdog.DEBUG) {
                            Logger.log(String.format("LogSource '%s': executing '%s'",
                                                     mName, mRunCmdLine));
                        }

                        Runtime rt = Runtime.getRuntime();
                        mProcess = rt.exec(mRunCmdLine);

                        InputStream stream = mProcess.getInputStream();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                        String line = null;
                        // We will wait here if adb is waiting for the device to become available.
                        while ((line = reader.readLine()) != null && !mThread.isInterrupted()) {

                            // Check if a trigger has occurred:
                            LogSourceTriggerList.Type triggerType = mTriggerList.getTriggerType(line);
                            if (triggerType == LogSourceTriggerList.Type.Resume && !mFeeding) {
                                mFeeding = true;
                                synchronized (mFeedListeners) {
                                    for (LogSourceFeedListener listener : mFeedListeners) {
                                        listener.onFeedingStarted(LogSource.this);
                                    }
                                }
                            }

                            if (line.length() == 0 || !mFeeding) {
                                continue;
                            }

                            synchronized (mListeners) {
                                char firstChar = line.charAt(0);
                                if (firstChar == '\r' || firstChar == '\n' ||
                                    mBlackList.found(line)) {
                                    continue;
                                }
                                // Notify all listening LogLineMatchers and others. Not
                                // allowed to throw.
                                for (LogSourceListener listener : mListeners) {
                                    listener.onLogLine(line);
                                }
                            }

                            if (triggerType == LogSourceTriggerList.Type.Pause && mFeeding) {
                                mFeeding = false;
                                synchronized (mFeedListeners) {
                                    for (LogSourceFeedListener listener : mFeedListeners) {
                                        listener.onFeedingStopped(LogSource.this);
                                    }
                                }
                            }
                        }
                    } catch (IOException excep) {
                        Logger.log(String.format("LogSource '%s': failed to read from log stream, " +
                                                 "retrying...\n%s", mName, excep.getMessage()));
                    } catch (RuntimeException excep) {
                        Logger.log(String.format("LogSource '%s': RuntimeException, retrying...\n%s",
                                                 mName, excep.getMessage()));
                    }
                    destroyProcess();
                    if (mOneShot) {
                        mThread.interrupt();
                    }
                }

                destroyProcess();
                mThread = null;
                if (logdog.DEBUG) {
                    Logger.log(String.format("LogSource '%s': exiting thread", mName));
                }
            }
        });
        mThread.setName("LogSource " + mName);
        mThread.start();
        return true;
    }

    private void stop() {
        if (mThread != null) {
            try {
                synchronized (mLifeListeners) {
                    for (LogSourceLifeListener listener : mLifeListeners) {
                        listener.onStopped(this);
                    }
                }
                mThread.interrupt();
                mThread.join(3000);
                if (mProcess != null && mThread.isAlive()) {
                    // This causes an InterruptedException in the
                    // thread if it is not dead yet:
                    mProcess.destroy();
                }
            } catch (InterruptedException excep) {
                Logger.logExcep(excep);
            }
        }
    }


    // BlackList stuff
    // Use 'mListeners' as sync object since used in reader thread
    // when accessing mBlackList.

    public boolean hasBlackList() {
        synchronized (mListeners) {
            return mBlackList.hasItems();
        }
    }

    public String getBlackListAsString() {
        synchronized (mListeners) {
            return mBlackList.toString();
        }
    }

    public void setBlackListListener(BlackListListener listener) {
        mBlackList.setListener(listener);
    }

    public boolean addToBlackList(String regExp) {
        synchronized (mListeners) {
            return mBlackList.add(regExp);
        }
    }

    public boolean replaceBlackList(String blacklistLines) {
        synchronized (mListeners) {
            return mBlackList.replaceBlackList(blacklistLines);
        }
    }

    public int readBlackList(File file) throws IOException {
        synchronized (mListeners) {
            return mBlackList.readFrom(file);
        }
    }

    public void saveBlackList(File file) throws IOException {
        synchronized (mListeners) {
            mBlackList.saveTo(file);
        }
    }

    public void clearBlackList() {
        synchronized (mListeners) {
            mBlackList.clear();
        }
    }

    public void addTrigger(LogLineMatcher llm) {
        mTriggerList.add(llm);
    }

    public void removeTrigger(LogLineMatcher llm) {
        mTriggerList.remove(llm);
    }
}

class LS_LogCat extends LogSource {
    public LS_LogCat(int sourceIndex, String buffername) {
        super(sSourceName[sourceIndex],
              String.format("adb -d logcat -b %s -v threadtime", buffername));
    }
}

class LS_LogCatMain extends LS_LogCat {
    public LS_LogCatMain() {
        super(LOGCAT_MAIN, "main");
    }
}

class LS_LogCatmainAndSystem extends LogSource {
    public LS_LogCatmainAndSystem() {
        super(sSourceName[LOGCAT_MAIN_AND_SYSTEM], "adb -d logcat -b main -b system -v threadtime");
    }
}

class LS_LogCatSystem extends LS_LogCat {
    public LS_LogCatSystem() {
        super(LOGCAT_SYSTEM, "system");
    }
}

class LS_LogCatEvents extends LS_LogCat {
    public LS_LogCatEvents() {
        super(LOGCAT_EVENTS, "events");
    }
}

class LS_LogCatRadio extends LS_LogCat {
    public LS_LogCatRadio() {
        super(LOGCAT_RADIO, "radio");
    }
}

/**
 * This log source emits logs for main, system, radio, events and kernel.
 */
class LS_LogCatAll extends LogSource {
    public LS_LogCatAll() {
        // Enable as much logging as possible including the kernel logs.
        super(sSourceName[LOGCAT_ALL],
              "adb -d logcat -b main -b system -b radio -b events -v threadtime");
        Utils.adbSetKernelLogProp(true);  // asynchronous
    }
}
