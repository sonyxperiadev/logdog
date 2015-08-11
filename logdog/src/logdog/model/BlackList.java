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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import logdog.utils.Logger;
import logdog.utils.Utils;

/**
 * This class maintains a list of regular expressions. Currently used
 * by LogSource to skip log lines and to listen for log lines that are
 * trigger events.
 */
public class BlackList {

    private StringBuilder mRegExps = new StringBuilder(400);
    private ArrayList<Pattern> mPatterns = new ArrayList<Pattern>(10);
    private BlackListListener mListener;

    void setListener(BlackListListener listener) {
        mListener = listener;
    }

    void removeListener() {
        mListener = null;
    }

    public interface BlackListListener {
        void onModified(boolean dirty);
    }

    private void notifyListener(boolean dirty) {
        if (mListener != null) {
            mListener.onModified(dirty);
        }
    }

    /**
     * Constructor.
     *
     * @param logSource Must be non-null.
     */
    public BlackList(LogSource logSource) {
        assert logSource != null : "BlackList c-tor: parameter 'logSource' is null";
    }

    public String toString() {
        return mRegExps.toString();
    }

    /**
     * Read lines from the given file, compiles the regexp for each line and
     * adds them to mRegExps and mPatterns.
     *
     * @param file
     *
     * @return -1 if successful else the line number of the first failing regexp.
     * @throws IOException
     */
    int readFrom(File file) throws IOException {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            clearNoNotify();
            int line = -1;
            String regExp = null;
            while ((regExp = reader.readLine()) != null) {
                ++line;
                if (!addNoNotify(regExp)) {
                    return line;
                }
            }
            return -1;  // success
        } catch (FileNotFoundException excep) {
            Logger.logExcep(excep);
            throw excep;
        } catch (IOException excep) {
            Logger.logExcep(excep);
            throw excep;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException excep) {
                    Logger.logExcep(excep);
                }
            }
            notifyListener(false);
        }
    }

    void saveTo(File file) throws IOException {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(file));
            String[] regExps = mRegExps.toString().split("\n");
            for (String regExp : regExps) {
                writer.write(regExp);
                writer.write("\n");
            }
        } catch (FileNotFoundException excep) {
            Logger.logExcep(excep);
            throw excep;
        } catch (IOException excep) {
            Logger.logExcep(excep);
            throw excep;
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException excep) {
                    Logger.logExcep(excep);
                }
            }
        }
    }

    private void clearNoNotify() {
        mRegExps.setLength(0);
        mPatterns.clear();
    }

    void clear() {
        clearNoNotify();
        notifyListener(false);  // not considered dirty
    }

    boolean add(String regExp) {
        boolean ret = addNoNotify(regExp);
        notifyListener(true);
        return ret;
    }

    boolean replaceBlackList(String blacklistLines) {
        boolean ret = false;
        if (blacklistLines != null) {
            clear();
            String[] regExpArray = blacklistLines.split("\n");
            for (String regExp : regExpArray) {
                if (!Utils.emptyString(regExp)) {
                    addNoNotify(regExp);
                }
            }
            notifyListener(true);
            ret = true;
        }

        return ret;
    }

    public boolean addNoNotify(String regExp) {
        try {
            regExp = regExp.trim();
            Pattern pattern = LogSource.compileRegExp(regExp);
            if (mRegExps.length() > 0) {
                mRegExps.append('\n');
            }
            mRegExps.append(regExp);
            mPatterns.add(pattern);
        } catch (PatternSyntaxException excep) {
            Logger.logExcep(excep);
            return false;
        }
        return true;
    }

    public boolean found(String logLine) {
        if (mPatterns.size() > 0) {
            for (Pattern pattern : mPatterns) {
                Matcher matcher = pattern.matcher(logLine);
                if (matcher.find()) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean hasItems() {
        return mPatterns.size() > 0;
    }
}
