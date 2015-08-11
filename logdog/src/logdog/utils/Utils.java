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

package logdog.utils;

import java.io.IOException;

public class Utils {

    public static boolean emptyString(String str) {
        return str == null || str.trim().length() == 0;
    }

    public static boolean isLinux() {
        String opsys = System.getProperty("os.name");
        return opsys.equals("Linux");
    }

    /**
     * Set property 'sys.kernel.log' to either "default" or "logcat".
     *
     * @param useLogCat if true set property to "logcat" else "default"
     */
    public static void adbSetKernelLogProp(final boolean useLogCat) {
        // Run on separate thread in case the device is not avialable yet.
        new Thread(new Runnable() {
            @Override
            public void run() {
                Runtime rt = Runtime.getRuntime();
                try {
                    rt.exec("adb -d root");
                    rt.exec("adb -d wait-for-device");
                    Thread.sleep(200);
                    rt.exec(String.format("adb -d shell setprop sys.kernel.log %s",
                                          useLogCat ? "logcat" : "default"));
                } catch (IOException excep) {
                    Logger.logExcep(excep);
                } catch (InterruptedException excep) {
                    Logger.logExcep(excep);
                }
            }
        }).start();
    }
}
