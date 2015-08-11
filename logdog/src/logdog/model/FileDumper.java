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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import logdog.utils.Logger;
import logdog.view.UIUtils;

public class FileDumper implements LogSourceListener {

    private String mName;
    private File mFile;
    private BufferedWriter mBufferedWriter;

    public FileDumper(String name) {
        mName= name;
    }

    public FileDumper(File file) {
        mFile = file;
    }

    public boolean start() {
        if (mBufferedWriter != null && !stop()) {
            return false;
        }

        try {
            File file = mName == null ? mFile : UIUtils.getCurrentDirTimeStampedFile(mName);
            FileWriter fileWriter = new FileWriter(file.getAbsolutePath());
            mBufferedWriter = new BufferedWriter(fileWriter);
        } catch (IOException excep) {
            Logger.logExcep(excep);
            return false;
        }

        return true;
    }

    public boolean stop() {
        if (mBufferedWriter != null) {
            try {
                mBufferedWriter.close();
                mBufferedWriter = null;
            } catch (IOException excep) {
                Logger.logExcep(excep);
                return false;
            }
        }
        return true;
    }

    public boolean isDumping() {
        return mBufferedWriter != null;
    }


    /* LogSourceListener */

    public void onLogLine(String line) {
        if (mBufferedWriter != null) {
            try {
                mBufferedWriter.append(line);
                mBufferedWriter.newLine();
            } catch (IOException excep) {
                Logger.logExcep(excep);
            }
        }
    }
}
