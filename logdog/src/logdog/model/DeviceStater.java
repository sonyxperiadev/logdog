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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import logdog.utils.Logger;

/**
 * Class for polling the Android device state.
 *
 */
public class DeviceStater extends Thread {

    public static final int POLL_INTERVAL_MS = 3000;

    public static interface DeviceListener {
        void onDeviceChanged(final DeviceStater stater);
    }

    // We only support one listener:
    public void setListener(DeviceListener listener) {
        mListener = listener;
    }
    private DeviceListener mListener;

    // Device state
    public enum DEVICE_STATE {
        UNKNOWN,
        NOT_AVAILABLE,
        AVAILABLE
    };
    private DEVICE_STATE mDeviceState = DEVICE_STATE.UNKNOWN;

    public enum KERNEL_LOG {
        UNKNOWN,
        DEFAULT,
        LOGCAT
    };
    private KERNEL_LOG mKernelLog = KERNEL_LOG.UNKNOWN;

    public DEVICE_STATE getDeviceState() {
        return mDeviceState;
    }

    public KERNEL_LOG getKernelLog() {
        return mKernelLog;
    }

    public DeviceStater() {
        setName("DeviceStater");
    }

    private KERNEL_LOG getKernelLog(Runtime rt) throws IOException {
        Process process = rt.exec("adb -d shell getprop sys.kernel.log");
        InputStream stream = process.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        String kernelLog = reader.readLine();
        if (kernelLog != null) {
            if (kernelLog.equals("logcat")) {
                return KERNEL_LOG.LOGCAT;
            }
            if (kernelLog.equals("default")) {
                return KERNEL_LOG.DEFAULT;
            }
        }
        return KERNEL_LOG.UNKNOWN;

    }

    private DEVICE_STATE getDeviceState(Runtime rt) throws IOException {
        // TODO? Consider replacing this with code from DDMS using a
        // socket instead (openAdbConnection() in DeviceMonitor.java,
        // initAdbSocketAddr() in AndroidDebugBridge.java etc).
        Process process = rt.exec("adb -d get-state");
        InputStream stream = process.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        String deviceState = reader.readLine();
        if (deviceState == null) {
            return DEVICE_STATE.UNKNOWN;
        }
        if (deviceState.equals("device")) {
            return DEVICE_STATE.AVAILABLE;
        }
        return DEVICE_STATE.NOT_AVAILABLE;
    }

    @Override
    public void run() {
        if (mListener != null) {
            mListener.onDeviceChanged(this);
        }

        Runtime rt = Runtime.getRuntime();
        while (!isInterrupted()) {  // does not reset the interrupt flag
            DEVICE_STATE newDeviceState = DEVICE_STATE.UNKNOWN;
            KERNEL_LOG kernelLog = KERNEL_LOG.UNKNOWN;
            try {
                Thread.sleep(POLL_INTERVAL_MS);
                if (mListener == null) {
                    continue;
                }

                newDeviceState = getDeviceState(rt);
                kernelLog = getKernelLog(rt);
            } catch (IOException excep) {
                Logger.logExcep(excep);
            } catch (InterruptedException excep) {
                // This should take us out of the while loop.
            } finally {
                if (mListener != null &&
                    (newDeviceState != mDeviceState || kernelLog != mKernelLog)) {
                    mDeviceState = newDeviceState;
                    mKernelLog = kernelLog;
                    mListener.onDeviceChanged(this);
                }
            }
        }
    }
}
