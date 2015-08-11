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

import java.awt.Cursor;
import java.io.File;
import java.util.ArrayList;

import javax.swing.JFrame;
import logdog.model.LogLineMatcher;
import logdog.model.LogLineMatcher.Group;
import logdog.model.LogLineMatcherManager;
import logdog.view.LLMView;

/**
 * This is the controller for creating and editing LogLineMatchers
 * using a dialog.
 */
public class LLMController {

    private LogLineMatcherManager mLLMMgr;
    private LLMView mLLMView;

    public LogLineMatcherManager getLLMMgr() {return mLLMMgr;};

    /**
     * Constructor.
     *
     * @param llmMgr
     */
    public LLMController(LogLineMatcherManager llmMgr) {
        assert llmMgr != null : "LLMController: llmMgr is null";
        mLLMMgr = llmMgr;
    }

    /**
     * Create a new set of LogLineMatchers.
     *
     * @param owner
     * @param isNew
     * @param pasteAsRegExp
     */
    public void displayLLMView(JFrame owner, boolean isNew, String pasteAsRegExp) {

        try {
            owner.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            mLLMView = new LLMView(owner, isNew, pasteAsRegExp, this);

            // Don't call mLLMView.pack() because then non-visible items
            // will disappear.
            RefineryUtilities.centerFrameOnScreen(mLLMView);
        } finally {
            owner.setCursor(Cursor.getDefaultCursor());
        }
        if (mLLMView != null) {
            mLLMView.setVisible(true);
        }
    }

    /**
     * Called when the edit dialog is about to be dismissed.
     *
     */
    public void editDone() {
        if (mLLMView != null) {
            mLLMView.die();
        }
    }

    public void editSave() {
        mLLMMgr.editSave();  // throws
    }

    public void editCancel() {
        mLLMMgr.editCancel();
    }

    public void editDelete(LogLineMatcher llm) {
        mLLMMgr.editDelete(llm);
    }

    public void editAdd() {
        mLLMMgr.editAdd();
    }

    public void moveLLM(int fromIndex, int toIndex) {
        mLLMMgr.moveLLM(fromIndex, toIndex);  // fires LLMEditListener.onMoved()
    }

    public void editDeleteGroup(LogLineMatcher llm, int index) {
        mLLMMgr.editDeleteGroup(llm, index);
    }

    public void editAddGroup(LogLineMatcher llm, Group group) {
        mLLMMgr.editAddGroup(llm, group);
    }

    /**
     * Move a LogLineMatcher.Group in LogLineMatcher.mGroups_Edit from
     * one position to another. Called when editing an LLM.
     *
     * @param llm
     * @param fromIndex
     * @param toIndex
     */
    public void editMoveGroup(LogLineMatcher llm, int fromIndex, int toIndex) {
        mLLMMgr.editMoveGroup(llm, fromIndex, toIndex);  // fires LLMEditListener.onGroupMoved()
    }

    public boolean hasLLMs() {
        return mLLMMgr.getLLMCount() > 0;
    }

    public ArrayList<LogLineMatcher> getLLMs() {
        return mLLMMgr.getLLMs();
    }

    public void editUpdateLLMMgr(boolean useTimeDuration, String timeDurationValue,
                                 boolean useCountDuration, String countDurationValue) {
        mLLMMgr.editUpdate(useTimeDuration, timeDurationValue, useCountDuration, countDurationValue);
    }

    public boolean editUpdateLLM(LogLineMatcher llm, String name, boolean event, boolean enabled,
                                 boolean timeDiff, String sourceName, String regExp,
                                 LogLineMatcher.Groups groups, int presentationId,
                                 String triggerType) {
        if (llm != null) {
            return llm.editUpdate(name, event, enabled, timeDiff, sourceName, regExp,
                                  groups, presentationId, triggerType);
        }
        return false;
    }

    public boolean hasFilePath() {
        File xmlPath = mLLMMgr.getFilePath();
        if (xmlPath != null && xmlPath.canWrite()) {
            return true;
        }
        return false;
    }

    public void setFilePath(File xmlPath) {
        mLLMMgr.setFilePath(xmlPath);
    }
}
