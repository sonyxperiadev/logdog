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

package logdog.controller;

import org.jfree.ui.RefineryUtilities;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import logdog.Prefs;
import logdog.logdog;
import logdog.model.FileLogSource;
import logdog.model.LLMEditListener;
import logdog.model.LogLineMatcher;
import logdog.model.LogLineMatcher.Group;
import logdog.model.LogLineMatcherManager;
import logdog.model.LogSource;
import logdog.model.LogSourceLifeListener;
import logdog.model.DeviceStater;
import logdog.model.LogSourceListener;
import logdog.utils.Logger;
import logdog.view.ChartView;
import logdog.view.LogSourceView;

/**
 * This is the controller for all views in logdog. It wraps a
 * LogLineMatcherManager which is the main interface to the model. It
 * is also responsibile for creating the ChartView and views for all
 * log sources as they are created.
 */
public class MainController
    implements LLMEditListener, LogSourceLifeListener {

    private ChartView mChartView;
    private LogLineMatcherManager mLLMMgr;
    private DeviceStater mDeviceStaterThread;

    // List of views to be notified when a selection has been done in
    // another view, for instance when a point is selected in the
    // ChartView.
    private ArrayList<ViewSelListener> mViewSelListeners = new ArrayList<ViewSelListener>(5);

    public MainController() {
        // Subscribe to LogLineMatcher edit events.
        LogLineMatcherManager.addLLMEditListener(this);

        // Subscribe to LogSource lifecycle events i.e. start and stop
        // of LogSources.
        LogSource.addLifeListener(this);
    }

    public void showChartView(Prefs prefs) {
        // Create the chart view at screen center.
        mChartView = new ChartView(logdog.getFriendlyVersion(), this, prefs);
//        mChartView.pack();
        RefineryUtilities.centerFrameOnScreen(mChartView);
        mChartView.setVisible(true);

        startDeviceStater();

        // Now always start logcat with both main and system.
        LogSource.startLogCatMainAndSystem();
    }

    /**
     * Called from ChartView when exiting the program.
     */
    public void die() {
        LogLineMatcherManager.removeLLMEditListener(this);
        LogSource.removeLifeListener(this);
        clean();
        stopDeviceStater();
    }

    public boolean hasLLMMgr() {
        return mLLMMgr != null;
    }

    /**
     * Unregister all LogLineMatchers from their LogSource and set
     * mLLMMgr to null.
     */
    public void clean() {
        if (mLLMMgr != null) {
            mLLMMgr.unRegisterAllLLMs();
            mLLMMgr = null;
        }
    }

    public void clearStateAllLLMs() {
        if (mLLMMgr != null) {
            mLLMMgr.clearStateAllLLMs();
        }
    }

    public void clearStateLLMsPresentationId(int presentationId) {
        if (mLLMMgr != null) {
            mLLMMgr.clearStateLLMsPresentationId(presentationId);
        }
    }

    /**
     * Create DeviceStaterThread and set the chart as listener so we
     * can display if the device is available or not.
     */
    private void startDeviceStater() {
        mDeviceStaterThread = new DeviceStater();
        mDeviceStaterThread.start();
        mDeviceStaterThread.setListener(mChartView);
    }

    private void stopDeviceStater() {
        if (mDeviceStaterThread != null) {
            try {
                mDeviceStaterThread.interrupt();
                mDeviceStaterThread.join(DeviceStater.POLL_INTERVAL_MS * 2);
            } catch (InterruptedException excep) {
                Logger.logExcep(excep);
            }
        }
    }

    public void createFromFile(final File xmlPath)
        throws IOException {
        mLLMMgr = LogLineMatcherManager.createFromFile(xmlPath);
    }

    /**
     * Show a dialog and create a new set of LogLineMatchers. Can be
     * cancelled by the user therefore we do not call clean() or use
     * mLLMMgr.
     *
     * @param owner JFrame owner of the LLM edit dialog
     * @param pasteAsRegExp
     */
    public void create(JFrame owner, String pasteAsRegExp) {
        // No file yet, initially 5 LogLineMatchers:
        LogLineMatcherManager llmMgr = new LogLineMatcherManager(null, 5);
        llmMgr.editBegin();
        LLMController llmController = new LLMController(llmMgr);
        llmController.displayLLMView(owner, true, pasteAsRegExp);
    }

    /**
     * Edit the set of current LogLineMatchers using a dialog.
     *
     * @param owner JFrame owner of the dialog
     * @param pasteAsRegExp
     */
    public void edit(JFrame owner, String pasteAsRegExp) {
        if (mLLMMgr != null) {
            mLLMMgr.editBegin();
            LLMController llmController = new LLMController(mLLMMgr);
            llmController.displayLLMView(owner, false, pasteAsRegExp);
        }
    }

    /**
     * Create a new LLM based on the given regular expression.
     *
     * @param owner
     * @param pasteAsRegExp Regexp to pass to LLMView.
     */
    public void pasteAsRegExp(JFrame owner, String pasteAsRegExp) {
        if (mLLMMgr == null) {
            create(owner, pasteAsRegExp);
        } else {
            edit(owner, pasteAsRegExp);
        }
    }

    /**
     * Change log source for all LogLineMatchers to the given file
     * based log source and start it.
     *
     * @param fileLogSource
     */
    public void setFileLogSource(FileLogSource fileLogSource) {
        if (mLLMMgr != null) {
            mLLMMgr.setFileLogSource(fileLogSource);
        }
    }

    public void addSelListener(ViewSelListener listener) {
        synchronized (mViewSelListeners) {
            if (listener != null && !mViewSelListeners.contains(listener)) {
                mViewSelListeners.add(listener);
            }
        }
    }

    public void removeSelListener(ViewSelListener listener) {
        synchronized (mViewSelListeners) {
            if (listener != null && mViewSelListeners.contains(listener)) {
                mViewSelListeners.remove(listener);
            }
        }
    }

    /**
     * Notify all views except the given view that the user has made a
     * selection at the given time stamp.
     *
     * @param sender Notify all ViewSelListeners except this one.
     * @param time Time stamp at which the selection has been done by the user.
     */
    public void notifySelListeners(ViewSelListener sender, long time) {
        synchronized (mViewSelListeners)  {
            for (ViewSelListener listener : mViewSelListeners) {
                if (listener != sender) {
                    listener.onSelection(time);
                }
            }
        }
    }

    private void createLSView(LogSource logSource) {
        try {
            // Show the log window on a secondary monitor if available.
            JFrame frame = new LogSourceView(logSource, this);
            // TODO showOnScreen() doesn't work on single screen
            // systems. It will maximize the window (no resizing, no
            // min/max). Skip this for now, nice though to be able to
            // show secondary windows on a secondary screen.
            frame.setVisible(true);
//            UIUtils.showOnScreen(1, frame);
            if (mChartView != null) {
                mChartView.requestFocus();
//                UIUtils.showOnScreen(0,  mChartView);
            }
        } catch (OutOfMemoryError excep) {
            String msg = "MainController: Out of memory when creating LogSourceView.";
            Logger.log(msg);
            Logger.logExcep(excep);

            // LogSourceView uses a lot of memory, handle any failure nicely.
            JOptionPane.showMessageDialog(null, excep.getMessage(), msg, JOptionPane.OK_OPTION);
        }
    }

    public void ensureLSView(LogSource logSource) {
        LogSourceListener listener = logSource.hasListenerOfType(LogSourceView.class);
        if (listener == null) {
            createLSView(logSource);
        } else {
            ((LogSourceView) listener).toFront();
        }
    }


    // LLMEditListener

    /**
     * We end up here when clicking the Save-button when editing LogLineMatchers.
     *
     * @param llmMgr If creating a new set of LogLineMatchers this is
     * the LogLineMatcherManager newed in create(). Otherwise it is
     * 'mLLMMgr'.
     */
    public void onEditCommitBegin(LogLineMatcherManager llmMgr) {
        boolean newLLMMgr = llmMgr != mLLMMgr;
        if (newLLMMgr) {
            clean();
            mLLMMgr = llmMgr;
        }
        if (mChartView != null) {
            mChartView.removeChartPanels();
            mChartView.setWindowTitle(mLLMMgr.getFilePath().getName());
            mChartView.setFileLogSourceName(null);  // remove file name
        }
    }

    public void onAdded(LogLineMatcher llm) {
    }

    public void onDeleted(LogLineMatcher llm) {
    }

    public void onMoved(int fromIndex, int toIndex) {
    }

    public void onGroupAdded(LogLineMatcher llm, Group group) {
    }

    public void onGroupDeleted(LogLineMatcher llm, int groupIndex) {
    }

    public void onGroupMoved(LogLineMatcher llm, int fromIndex, int toIndex) {
    }


    // LogSourceLifeListener

    public void onStarted(LogSource logSource) {
        createLSView(logSource);
    }

    public void onStopped(LogSource logSource) {
    }
}
