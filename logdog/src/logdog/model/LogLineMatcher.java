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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import logdog.model.LogSourceTriggerList.Type;
import logdog.utils.Logger;
import logdog.utils.Utils;

public class LogLineMatcher implements LogSourceListener {

    /*------ Static class members and methods ------*/

    static final String XML_LLM_ELEM = "LogLineMatcher";
    private static final String XML_NAME_ELEM = "Name";
    private static final String XML_EVENT_ELEM = "Event";
    private static final String XML_TIMEDIFF_ELEM = "TimeDiff";
    private static final String XML_ENABLED_ELEM = "Enabled";
    private static final String XML_SOURCE_ELEM = "Source";
    private static final String XML_REGEXP_ELEM = "RegExp";
    // Keep "GroupNames" for backwards compatability altough it should
    // really be called "Groups". Also, each element will be called
    // "Name" which really should be "Group":
    private static final String XML_GROUPNAMES_ELEM = "GroupNames";
    private static final String XML_PRESENTATIONID_ELEM = "PresentationId";
    private static final String XML_TRIGGER_ELEM = "Trigger";

    // String resources.
    private static final String LLM_INVALID_STR = "LogLineMatcher '%s' is not valid:\n%s";
    private static final String UNKNOWN_LLM_NAME_STR = "<unknown name>";
    private static final String LLMMGR_IS_NULL_STR = "parameter 'llmMgr' cannot be null";
    private static final String INVALID_LOGSOURCE_STR = "Invalid log source name '%s'.";
    private static final String UNKNOWN_LOGSOURCE_NAME_STR = "<unknown log source name>";
    private static final String EMPTY_LOGSOURCE_NAME_STR = "Empty log source name.";
    private static final String MISSING_LLMMGR_LOGSOURCE_STR = "Missing LLMMgr for log source name '%s'.";
    private static final String EMPTY_LLM_NAME_STR = "Name is missing or empty.";
    private static final String EMPTY_REGEXP_NAME_STR = "Regexp is missing or empty.";
    private static final String EMPTY_GROUP_NAME_STR = "Regexp group name is missing or empty.";
    private static final String INVALID_REGEXP_STR = "Invalid regexp '%s'\n%s.";
    private static final String NUMBERFORMATEXCEP_STR =
        "LogLineMatcher '%s': groupNo=%d isHex=%s strVal=%s\nNumberFormatException: %s";

    static LogLineMatcher createFromXml(Node llmElem, LogLineMatcherManager llmMgr) {
        String llmName = null;
        String llmSourceName = null;
        boolean llmEvent = false;
        boolean llmEnabled = true;
        boolean llmTimeDiff = false;
        String llmRegExp = null;
        Groups llmGroups = null;
        int llmPresentationId = 0;
        String llmTriggerType = LogSourceTriggerList.Type.None.toString();

        for (Node llmChildElem = llmElem.getFirstChild(); llmChildElem != null;
             llmChildElem = llmChildElem.getNextSibling()) {

            Node childElem = llmChildElem.getFirstChild();
            if (childElem == null) {  // for newlines, "#text"
                continue;
            }

            String llmElemName = llmChildElem.getNodeName();
            String llmElemValue = childElem.getNodeValue();

            if (llmElemName.equals(XML_NAME_ELEM)) {
                llmName = llmElemValue;
            } else if (llmElemName.equals(XML_EVENT_ELEM)) {
                llmEvent = llmElemValue.equals("true");
            } else if (llmElemName.equals(XML_ENABLED_ELEM)) {
                llmEnabled = llmElemValue.equals("true");
            } else if (llmElemName.equals(XML_TIMEDIFF_ELEM)) {
                llmTimeDiff = llmElemValue.equals("true");
            } else if (llmElemName.equals(XML_SOURCE_ELEM)) {
                llmSourceName = llmElemValue;
            } else if (llmElemName.equals(XML_REGEXP_ELEM)) {
                llmRegExp = llmElemValue;
            } else if (llmElemName.equals(XML_GROUPNAMES_ELEM)) {
                for (Node elem = llmChildElem.getFirstChild(); elem != null;
                     elem = elem.getNextSibling()) {
                    if (elem.getNodeName().equals(XML_NAME_ELEM)) {
                        Group group = Group.createFromXml(elem);
                        if (group != null) {
                            if (llmGroups == null) {
                                llmGroups = new Groups(5);
                            }
                            llmGroups.add(group);
                        }
                    }
                }
            } else if (llmElemName.equals(XML_PRESENTATIONID_ELEM)) {
                try {
                    llmPresentationId = Integer.parseInt(llmElemValue);
                } catch (NumberFormatException excep) {
                    Logger.logExcep(excep);
                }
            } else if (llmElemName.equals(XML_TRIGGER_ELEM)) {
                llmTriggerType = llmElemValue;
            }
        }

        if (llmName != null && llmSourceName != null && llmRegExp != null) {
            try {
                //TODO Create Builder class or specific class for LogLineMatcher? Too many args now.
                return new LogLineMatcher(llmName, llmEvent, llmEnabled, llmTimeDiff, llmSourceName,
                                          llmRegExp, llmGroups, llmPresentationId, llmTriggerType, llmMgr);
            } catch (InvalidException excep) {
                Logger.logExcep(excep);
            }
        }

        return null;
    }


    /*------ Object members and methods ------*/

    private LogLineMatcherManager mLLMMgr;
    public LogLineMatcherManager getLLMMgr() {return mLLMMgr;};

    private String mName;
    private boolean mEvent;
    private boolean mEnabled;
    private boolean mActive;
    private boolean mTimeDiff;
    private LogSource mSource;
    private String mRegExp;
    private Pattern mPattern;
    private Groups mGroups;
    private int mPresentationId;
    private LogSourceTriggerList.Type mTriggerType = LogSourceTriggerList.Type.None;

    /* Members used while editing this LogLineMatcher. */
    private enum EDITMODE {
        ORG,
        MODIFIED,
        NEW,
        DELETED
    }
    private EDITMODE mEditMode = EDITMODE.ORG;
    private String mName_Edit;
    private boolean mEvent_Edit;
    private boolean mEnabled_Edit;
    private boolean mTimeDiff_Edit;
    private String mSourceName_Edit;
    private String mRegExp_Edit;
    private Groups mGroups_Edit;
    private int mPresentationId_Edit;
    private LogSourceTriggerList.Type mTriggerType_Edit = LogSourceTriggerList.Type.None;

    private Date mPrevTimeDiffDate;  // only used when mTimeDiff is true

    void setModified() {
        mEditMode = EDITMODE.MODIFIED;
    }

    void setNew() {
        mEditMode = EDITMODE.NEW;
    }

    void setDeleted() {
        mEditMode = EDITMODE.DELETED;
    }

    boolean isModified() {
        return mEditMode == EDITMODE.MODIFIED;
    }

    boolean isNew() {
        return mEditMode == EDITMODE.NEW;
    }

    boolean isDeleted() {
        return mEditMode == EDITMODE.DELETED;
    }

    public String getName() {return mName;};
    public String getDiffSeriesName() {return mName + " (time diff)";};
    public boolean isEvent() {return mEvent;};
    public boolean isEnabled() {return mEnabled;};
    public boolean hasTimeDiff() {return mTimeDiff;};
    public LogSource getSource() {return mSource;};
    public String getRegExp() {return mRegExp;};
    public Groups getGroups() {return mGroups;};
    public int getPresentationId() {return mPresentationId;};
    public String getTriggerTypeAsString() {
        return mTriggerType.toString();
    }
    public Type getTriggerType() {
        return mTriggerType;
    }

    /* These methods should only be called when in edit mode
       i.e. editBegin() has been called. */
    public String getName_Edit() {return mName_Edit;}
    public boolean getEvent_Edit() {return mEvent_Edit;}
    public boolean getEnabled_Edit() {return mEnabled_Edit;}
    public boolean getTimeDiff_Edit() {return mTimeDiff_Edit;}
    public String getSourceName_Edit() {return mSourceName_Edit;}
    public String getRegExp_Edit() {return mRegExp_Edit;}
    public Groups getGroups_Edit() {return mGroups_Edit;}
    public int getPresentationId_Edit() {return mPresentationId_Edit;}
    public Object getTriggerTypeAsString_Edit() {
        return mTriggerType_Edit.toString();
    }

    /**
     * Since there doesn't seem to be any way to get the number of
     * groups in mRegExp, we roll our own by counting left parenthesis.
     *
     * @return
     */
    public int getRegExpGroupCount() {
        int groupCount = 0;
        for (int index = 0; index < mRegExp.length(); ++index)  {
            if (mRegExp.charAt(index) == '(' && (index > 0 && mRegExp.charAt(index - 1) != '\\')) {
                ++groupCount;
            }
        }
        return groupCount;
    }

    public void setActive(boolean active) {
        mActive = active;
    }

    @SuppressWarnings("serial")
    public static class InvalidException extends RuntimeException {
        public InvalidException(final String msg) {
            super(msg);
        }
    }

    boolean isValid(boolean useEdit) {
        if (useEdit) {
            return mName_Edit != null && mSourceName_Edit != null && mRegExp_Edit != null;
        }
        return mName != null && mSource != null && mPattern != null;
    }

    public void clearState() {
        mPrevTimeDiffDate = null;
        if (mGroups != null) {
            for (int index = 0; index < mGroups.size(); ++index) {
                mGroups.get(index).clearState();
            }
        }
    }

    private void checkValid(final String op, boolean useEdit) throws InvalidException {
        if (!isValid(useEdit)) {
            throwInvalid(op);
        }
    }

    private void throwInvalid(final String op) throws InvalidException {
        String excepMsg = String.format(LLM_INVALID_STR, mName == null ? UNKNOWN_LLM_NAME_STR : mName, op);
        throw new InvalidException(excepMsg);
    }

    /**
     * Constructor
     *
     * @param name
     * @param event
     * @param enabled
     * @param timeDiff
     * @param sourceName
     * @param regExp Regular expression without the initial timestamp group.
     * @param groups
     * @param llmMgr
     *
     * @return
     */
    public LogLineMatcher(String name, boolean event, boolean enabled, boolean timeDiff,
                          String sourceName, String regExp, Groups groups, int presentationId,
                          String triggerType, LogLineMatcherManager llmMgr)
        throws InvalidException {
        init(name, event, enabled, timeDiff, sourceName, regExp, groups, presentationId, triggerType, llmMgr);
    }

    /**
     * Constructor when creating new LogLineMatchers using the
     * GUI. This version does minimal error checking and allows some
     * empty members.
     *
     * @param name
     * @param event
     * @param enabled
     * @param sourceName
     * @param llmMgr
     */
    public LogLineMatcher(String name, boolean event, boolean enabled, boolean timeDiff,
                          String sourceName, int presentationId, String triggerType,
                          LogLineMatcherManager llmMgr)
        throws InvalidException {

        assert llmMgr != null : "LogLineMatcher(): " + LLMMGR_IS_NULL_STR;
        mLLMMgr = llmMgr;

        mName = name.trim();
        mEvent = event;
        mEnabled = enabled;
        mTimeDiff = timeDiff;
        mSource = verifySourceName(sourceName);
        mRegExp = "";
        mPresentationId = presentationId;
        mTriggerType = LogSourceTriggerList.parse(triggerType);
    }

    private void init(String name, boolean event, boolean enabled, boolean timeDiff,
                      String sourceName, String regExp, Groups groups, int presentationId,
                      String triggerType, LogLineMatcherManager llmMgr)
        throws InvalidException {

        if (llmMgr == null) {
            throwInvalid(String.format(MISSING_LLMMGR_LOGSOURCE_STR, sourceName));
        }
        mLLMMgr = llmMgr;

        verify(name, sourceName, regExp);

        mName = name.trim();
        mEvent = event;
        mEnabled = enabled;
        mTimeDiff = timeDiff;
        mSource = LogSource.findLogSource(sourceName.trim(), false);
        if (mSource == null) {
            throwInvalid(String.format(INVALID_LOGSOURCE_STR,
                    sourceName == null ? UNKNOWN_LOGSOURCE_NAME_STR : sourceName));
        }

        // The compiled regexp will be prepended with the timestamp
        // expression. Because of this we must save the original regexp.
        mPattern = LogSource.compileRegExp(regExp.trim());  // throws
        mRegExp = regExp.trim();

        mGroups = groups;
        mPresentationId = presentationId;
        mTriggerType = LogSourceTriggerList.parse(triggerType);
    }

    private LogSource verifySourceName(String sourceName) {
        if (Utils.emptyString(sourceName)) {
            throwInvalid(EMPTY_LOGSOURCE_NAME_STR);
        }

        LogSource logSource = LogSource.findLogSource(sourceName.trim(), false);
        if (logSource == null) {
            throwInvalid(String.format(INVALID_LOGSOURCE_STR, sourceName));
        }
        return logSource;
    }

    private void verify(String name, String sourceName, String regExp) {

        if (name == null || name.trim().equals("")) {
            throwInvalid(EMPTY_LLM_NAME_STR);
        }

        verifySourceName(sourceName);

        if (regExp == null || regExp.trim().equals("")) {
            throwInvalid(EMPTY_REGEXP_NAME_STR);
        }

        try {
            regExp = regExp.trim();
            LogSource.compileRegExp(regExp);
        } catch (PatternSyntaxException excep) {
            Logger.logExcep(excep);
            throwInvalid(String.format(INVALID_REGEXP_STR, regExp, excep.getMessage()));
        }

        // 'groups' can be null so we need not verify them
    }

    @Override
    public String toString() {
        String ret = mName_Edit;
        // if (!mEnabled) {
        //     ret += " [disabled]";
        // }
        return ret;
    }

    private void copyToEditMembers() {
        mName_Edit = mName;
        mEvent_Edit = mEvent;
        mEnabled_Edit = mEnabled;
        mTimeDiff_Edit = mTimeDiff;
        mSourceName_Edit = mSource.getName();
        mRegExp_Edit = mRegExp;
        mGroups_Edit = mGroups != null ? mGroups.copy() : null;
        mPresentationId_Edit = mPresentationId;
        mTriggerType_Edit = mTriggerType;
    }

    /**
     * Must be called before starting an edit session. Copies current
     * values from ordinary members into the corresponding edit members.
     */
    void editBegin() {
        copyToEditMembers();
    }

    /**
     * If this is a new or a previously existing but modified
     * LogLineMatcher, then verify that everything looks ok, for
     * instance that the regexp can be compiled and that the log
     * sources exists.
     */
    void editVerify() {
        if (mEditMode == EDITMODE.MODIFIED || mEditMode == EDITMODE.NEW) {
            verify(mName_Edit, mSourceName_Edit, mRegExp_Edit);  // throws
        }
    }

    /**
     * Called during editing to update the edit members with values
     * from the GUI (LLMView).
     *
     * @param name
     * @param event
     * @param enabled
     * @param sourceName
     * @param regExp
     * @param groups
     * @param presentationId
     *
     * @return true if modified
     */
    public boolean editUpdate(String name, boolean event, boolean enabled, boolean timeDiff,
                              String sourceName, String regExp, Groups groups, int presentationId,
                              String triggerType) {
        boolean allEqual =
            mName_Edit.equals(name) &&
            mEvent_Edit == event &&
            mEnabled_Edit == enabled &&
            mTimeDiff_Edit == timeDiff &&
            mSourceName_Edit.equals(sourceName) &&
            mRegExp_Edit.equals(regExp) &&
            ((mGroups_Edit != null) ? mGroups_Edit.equals(groups) : groups == null) &&
            mPresentationId_Edit == presentationId &&
            mTriggerType_Edit.equals(triggerType);

        if (!allEqual) {
            mEditMode = EDITMODE.MODIFIED;
            mName_Edit = name;
            mEvent_Edit = event;
            mEnabled_Edit = enabled;
            mTimeDiff_Edit = timeDiff;
            mSourceName_Edit = sourceName;
            mRegExp_Edit = regExp;
            // No need to copy individual Group objects since 'groups'
            // are newed in LLMView:
            mGroups_Edit = groups;
            mPresentationId_Edit = presentationId;
            mTriggerType_Edit = LogSourceTriggerList.parse(triggerType);
        }
        return !allEqual;
    }

    void editAddGroup(Group group) {
        if (mGroups_Edit == null) {
            mGroups_Edit = new Groups(5);
        }
        mGroups_Edit.add(group);
    }

    /**
     * Restore all edit members to their original content. Added so
     * toString() will return the real name after an edit.
     */
    void editCancel() {
        copyToEditMembers();
    }

    /**
     * Called when this LogLineMatcher has succesfully been saved to
     * file. Commit all edits.
     */
    void editCommit() throws InvalidException {
        init(mName_Edit, mEvent_Edit, mEnabled_Edit, mTimeDiff_Edit, mSourceName_Edit,
             mRegExp_Edit, mGroups_Edit, mPresentationId_Edit, mTriggerType_Edit.toString(),
             mLLMMgr);  // throws
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !(other instanceof LogLineMatcher)) {
            return false;
        }

        LogLineMatcher llmOther = (LogLineMatcher) other;
        if (llmOther == this) {
            return true;
        }

        if ((mGroups == null && llmOther.mGroups != null) ||
            (mGroups != null && !mGroups.equals(llmOther.mGroups))) {
            return false;
        }

        return (mName.equals(llmOther.mName) &&
                mSource == llmOther.mSource &&
                mPattern.pattern().equals(llmOther.mPattern.pattern()));
    }

    void registerToLogSource() {
        mSource.addListener(this);
        if (mTriggerType != LogSourceTriggerList.Type.None) {
            mSource.addTrigger(this);
        }
    }

    void registerToLogSource(LogSource logSource) {
        unRegisterFromLogSource();
        mSource = logSource;
        registerToLogSource();
    }

    void unRegisterFromLogSource() {
        mSource.removeListener(this);
        mSource.removeTrigger(this);
    }

    private void createFlagXmlElem(Document doc, Element llmElement, String elemName, boolean value) {
        Element elem = doc.createElement(elemName);
        elem.appendChild(doc.createTextNode(value ? "true" : "false"));
        llmElement.appendChild(elem);
    }

    /**
     * Create xml for this LogLineMatcher if mEditMode is not
     * EDITMODE.DELETED.
     *
     * @param doc
     * @param rootElem
     * @param useEdit if true use _Edit members (NOT USED BUT KEPT CODE)
     */
    void createXml(Document doc, Element rootElem, boolean useEdit) {
        if (isDeleted()) {
            return;
        }

        try {
            checkValid("createXml", useEdit);

            Element llmElement = doc.createElement(XML_LLM_ELEM);
            rootElem.appendChild(llmElement);

            Element nameElem = doc.createElement(XML_NAME_ELEM);
            nameElem.appendChild(doc.createTextNode(useEdit ? mName_Edit : mName));
            llmElement.appendChild(nameElem);

            createFlagXmlElem(doc, llmElement, XML_EVENT_ELEM, useEdit ? mEvent_Edit : mEvent);
            createFlagXmlElem(doc, llmElement, XML_ENABLED_ELEM, useEdit ? mEnabled_Edit : mEnabled);
            createFlagXmlElem(doc, llmElement, XML_TIMEDIFF_ELEM, useEdit ? mTimeDiff_Edit : mTimeDiff);

            Element sourceElem = doc.createElement(XML_SOURCE_ELEM);
            sourceElem.appendChild(doc.createTextNode(useEdit ? mSourceName_Edit : mSource.getName()));
            llmElement.appendChild(sourceElem);

            Element regExpElem = doc.createElement(XML_REGEXP_ELEM);
            regExpElem.appendChild(doc.createTextNode(useEdit ? mRegExp_Edit : mRegExp));
            llmElement.appendChild(regExpElem);

            // Save all group names if present.
            if (useEdit) {
                if (mGroups_Edit != null && mGroups_Edit.size() > 0) {
                    mGroups_Edit.createXml(doc, llmElement);
                }
            } else if (mGroups != null && mGroups.size() > 0) {
                mGroups.createXml(doc, llmElement);
            }

            Element presentationIdElem = doc.createElement(XML_PRESENTATIONID_ELEM);
            int presentationId = useEdit ? mPresentationId_Edit : mPresentationId;
            presentationIdElem.appendChild(doc.createTextNode(Integer.toString(presentationId)));
            llmElement.appendChild(presentationIdElem);

            Element triggerElem = doc.createElement(XML_TRIGGER_ELEM);
            LogSourceTriggerList.Type triggerType = useEdit ? mTriggerType_Edit : mTriggerType;
            triggerElem.appendChild(doc.createTextNode(triggerType.toString()));
            llmElement.appendChild(triggerElem);
        } catch (InvalidException excep) {
            Logger.logExcep(excep);
        } catch (SecurityException excep) {
            Logger.logExcep(excep);
        }
    }


    // LogSourceListener

    public void onLogLine(final String logLine) {
        if (!mActive || !mEnabled) {
            return;
        }

        Matcher matcher = mPattern.matcher(logLine);
        while (matcher.find()) {
            int groupCount = mGroups != null ? mGroups.size() : 0;
            int regExpGroupCount = matcher.groupCount();
            Date date = LogSource.getDate(matcher.group(1));

            if (mTimeDiff) {
                if (mPrevTimeDiffDate != null) {
                    int seriesIndex = 0;
                    long value = date.getTime() - mPrevTimeDiffDate.getTime();
                    mLLMMgr.onMatchedLogLine(this, seriesIndex, date, value);
                }
                mPrevTimeDiffDate = date;
            }
            if (regExpGroupCount > 1) {
                if (date != null) {

                    int seriesIndex = mTimeDiff ? 1 : 0;

                    // Start from 2 since the entire expression is at
                    // index 0 and the timestamp is at index 1.
                    for (int regExpGroupNo = 2; regExpGroupNo <= regExpGroupCount; ++regExpGroupNo, ++seriesIndex) {

                        // Get the value. For now we handle decimal and hex formats.
                        //double value = Double.parseDouble(matcher.group(groupNo));
                        //System.out.println(String.format("groupNo=%d %f", groupNo, value));
                        String strVal = matcher.group(regExpGroupNo);
                        boolean isHex = (strVal.length() > 1 &&
                                         (strVal.charAt(1) == 'x' || strVal.charAt(1) == 'X'));
                        boolean isFloat = false;
                        if (isHex) {
                            strVal = strVal.substring(2);  // skip 0x
                        } else {
                            isFloat = strVal.contains(".");
                        }

                        try {
                            float value = 0.0f;
                            if (isFloat) {
                                value = Float.parseFloat(strVal);
                            } else {
                                value = Long.parseLong(strVal, isHex ? 16 : 10);
                            }

                            // Notify the manager about this matched logline so it
                            // can be forwarded to any listeners (ChartView).
                            mLLMMgr.onMatchedLogLine(this, seriesIndex, date, value);

                            int groupIndex = regExpGroupNo - 2;
                            if (groupIndex >= 0 && groupIndex < groupCount) {
                                Group group = mGroups.get(groupIndex);
                                if (group.getHasValueDiff()) {
                                    ++seriesIndex;
                                }
                                if (group.showValueDiff(value)) {
                                    mLLMMgr.onMatchedLogLine(this, seriesIndex, date,
                                                             group.getDiffValue(value));
                                }
                            }
                        } catch (NumberFormatException excep) {
                            String msg =
                                String.format(NUMBERFORMATEXCEP_STR, mName, regExpGroupNo,
                                              isHex, strVal, excep.getMessage());
                            Logger.log(msg + excep);
                        }
                    }
                }
            } else if (regExpGroupCount == 1) {
                // This is just a match i.e. we don't have any value.
                // Notify the manager about this matched logline so it
                // can be forwarded to any listeners (ChartView).
                int seriesIndex = mTimeDiff ? 1 : 0;
                int value = 1;
                mLLMMgr.onMatchedLogLine(this, seriesIndex, date, value);
            }
        }
    }


    /*------ Sub classes ------*/

    /**
     * Class representing one regular expression group having a name
     * and various attributes.
     */
    public static class Group {

        /*------ Static class members and methods ------*/

        private static final String XML_SCALE_UNIT_ATTRIB = "ScaleUnit";
        private static final String XML_SCALE_FORMAT_ATTRIB = "ScaleFormat";
        private static final String XML_SCALE_RANGE_MIN_ATTRIB = "ScaleRangeMin";
        private static final String XML_SCALE_RANGE_MAX_ATTRIB = "ScaleRangeMax";
        private static final String XML_SCALE_INCLUDE_ZERO_ATTRIB = "ScaleIncludeZero";
        private static final String XML_VALUEDIFF_ATTRIB = "ValueDiff";

        static Group createFromXml(Node elem) {
            Node textElem = elem.getFirstChild();
            if (textElem == null) {
                return null;
            }
            String name = textElem.getNodeValue();
            if (Utils.emptyString(name)) {
                return null;
            }

            NamedNodeMap nodeMap = elem.getAttributes();

            String scaleUnit = getXmlStringAttrib(XML_SCALE_UNIT_ATTRIB, nodeMap);
            String scaleFormat = getXmlStringAttrib(XML_SCALE_FORMAT_ATTRIB, nodeMap);
            String scaleRangeMin = getXmlStringAttrib(XML_SCALE_RANGE_MIN_ATTRIB, nodeMap);
            String scaleRangeMax = getXmlStringAttrib(XML_SCALE_RANGE_MAX_ATTRIB, nodeMap);

            boolean scaleIncludeZero = false;
            Node node = nodeMap.getNamedItem(XML_SCALE_INCLUDE_ZERO_ATTRIB);
            if (node != null) {
                scaleIncludeZero = node.getNodeValue().equals("true");
            }

            boolean timeDiff = false;
            node = nodeMap.getNamedItem(XML_VALUEDIFF_ATTRIB);
            if (node != null) {
                timeDiff = node.getNodeValue().equals("true");
            }

            return new Group(name, scaleUnit, scaleFormat, scaleRangeMin, scaleRangeMax,
                             scaleIncludeZero, timeDiff);
        }

        private static String getXmlStringAttrib(String attrib, NamedNodeMap nodeMap) {
            Node node = nodeMap.getNamedItem(attrib);
            if (node != null) {
                String value = node.getNodeValue();
                if (!Utils.emptyString(value)) {
                    return value;
                }
            }
            return null;
        }

        /*------ Object members and methods ------*/

        private String mName;
        private String mScaleUnit;
        private String mScaleFormat;
        boolean mHasScaleRange;
        double mScaleRangeMin;
        double mScaleRangeMax;
        private boolean mScaleIncludeZero;
        // Create series for diff in value between consecutive matched log lines:
        private boolean mHasValueDiff;
        private boolean mFirstValueDiff = true;
        private float mPrevValue;   // used when mValueDiff is true

        /**
         * Private constructor.
         *
         * @param name
         * @param scaleUnit
         * @param scaleIncludeZero
         * @param hasValueDiff
         *
         * @return
         */
         private Group(String name, String scaleUnit, String scaleFormat,
                       String scaleRangeMin, String scaleRangeMax,
                       boolean scaleIncludeZero, boolean hasValueDiff)
            throws InvalidException {
            setName(name);
            mScaleUnit = scaleUnit;
            mScaleFormat = scaleFormat;
            assignScaleRange(scaleRangeMin, scaleRangeMax);
            mScaleIncludeZero = scaleIncludeZero;
            mHasValueDiff = hasValueDiff;
        }

        public Group(String name) throws InvalidException {
            setName(name);
        }

        Group copy() {
            String scaleRangeMin = mHasScaleRange ? Double.toString(mScaleRangeMin) : null;
            String scaleRangeMax = mHasScaleRange ? Double.toString(mScaleRangeMax) : null;
            return new Group(mName, mScaleUnit, mScaleFormat, scaleRangeMin, scaleRangeMax,
                             mScaleIncludeZero, mHasValueDiff);
        }

        @Override
        public boolean equals(Object other) {
            if (other == null || !(other instanceof Group)) {
                return false;
            }

            Group otherGroup = (Group) other;
            if (otherGroup == this) {
                return true;
            }

            return mName.equals(otherGroup.mName) &&

                ((mScaleUnit != null && mScaleUnit.equals(otherGroup.mScaleUnit)) ||
                 (mScaleUnit == null && otherGroup.mScaleUnit == null)) &&

                ((mScaleFormat != null && mScaleFormat.equals(otherGroup.mScaleFormat)) ||
                 (mScaleFormat == null && otherGroup.mScaleFormat == null)) &&

                (mHasScaleRange == otherGroup.mHasScaleRange &&
                 mScaleRangeMin == otherGroup.mScaleRangeMin &&
                 mScaleRangeMax == otherGroup.mScaleRangeMax) &&

                mScaleIncludeZero == otherGroup.mScaleIncludeZero &&

                mHasValueDiff == otherGroup.mHasValueDiff;
        }

        @Override
        public String toString() {
            StringBuffer ret = new StringBuffer(50);
            ret.append(mName);
            if (!Utils.emptyString(mScaleUnit)) {
                ret.append(" [unit=").append(mScaleUnit).append("]");
            }
            if (!Utils.emptyString(mScaleFormat)) {
                ret.append(" [format=").append(mScaleFormat).append("]");
            }
            if (mHasScaleRange) {
                ret.append(" [range=").append(mScaleRangeMin).append("-").append(mScaleRangeMax).append("]");
            }
            if (mScaleIncludeZero) {
                ret.append(" [zero]");
            }
            if (mHasValueDiff) {
                ret.append(" [diff]");
            }
            return ret.toString();
        }

        void createXml(Document doc, Element groupsElement) {
            if (!Utils.emptyString(mName)) {
                Element groupElement = doc.createElement(XML_NAME_ELEM);
                groupElement.appendChild(doc.createTextNode(mName));
                groupElement.setAttribute(XML_SCALE_UNIT_ATTRIB, mScaleUnit);
                groupElement.setAttribute(XML_SCALE_FORMAT_ATTRIB, mScaleFormat);

                if (mHasScaleRange) {
                    groupElement.setAttribute(XML_SCALE_RANGE_MIN_ATTRIB, Double.toString(mScaleRangeMin));
                    groupElement.setAttribute(XML_SCALE_RANGE_MAX_ATTRIB, Double.toString(mScaleRangeMax));
                }

                groupElement.setAttribute(XML_SCALE_INCLUDE_ZERO_ATTRIB,
                                          mScaleIncludeZero ? "true" : "false");
                groupElement.setAttribute(XML_VALUEDIFF_ATTRIB, mHasValueDiff ? "true" : "false");
                groupsElement.appendChild(groupElement);
            }
        }

        public String getName() {
            return mName;
        }

        public void setName(String name) throws InvalidException {
            if (Utils.emptyString(name)) {
                throw new InvalidException(EMPTY_GROUP_NAME_STR);
            }
            mName = name;
        }

        public String getSeriesName() {
            return mName;
        }

        public String getDiffSeriesName() {
            if (mHasValueDiff) {
                return mName + " (value diff)";
            }
            return null;
        }

        public String getScaleFormat() {
            return mScaleFormat;
        }

        public void setScaleFormat(String scaleFormat) {
            mScaleFormat = scaleFormat;
        }

        public String getScaleUnit() {
            return mScaleUnit;
        }

        public void setScaleUnit(String scaleUnit) {
            mScaleUnit = scaleUnit;
        }

        public void assignScaleRange(String scaleRangeMin, String scaleRangeMax) {
            mHasScaleRange =
                !Utils.emptyString(scaleRangeMin) && !Utils.emptyString(scaleRangeMax);
            if (mHasScaleRange) {
                try {
                    mScaleRangeMin = Double.parseDouble(scaleRangeMin);
                    mScaleRangeMax = Double.parseDouble(scaleRangeMax);
                    if (mScaleRangeMin > mScaleRangeMax) {
                        mScaleRangeMin = mScaleRangeMax - 10;  // doesn't work with 1!
                    }
                } catch (NumberFormatException excep) {
                    mHasScaleRange = false;
                }
            }
        }

        public boolean hasScaleRange() {
            return mHasScaleRange;
        }

        public double getScaleRangeMin() {
            return mScaleRangeMin;
        }

        public double getScaleRangeMax() {
            return mScaleRangeMax;
        }

        public boolean getScaleIncludeZero() {
            return mScaleIncludeZero;
        }

        public void setScaleIncludeZero(boolean scaleIncludeZero) {
            mScaleIncludeZero = scaleIncludeZero;
        }

        public boolean getHasValueDiff() {
            return mHasValueDiff;
        }

        public void setHasValueDiff(boolean hasValueDiff) {
            mHasValueDiff = hasValueDiff;
        }

        /**
         * Check if a value should be added to the series showing the
         * diff value.
         *
         * @return true if the user has set use of diff value for this
         * LLM and its not the first time.
         */
        private boolean showValueDiff(float value) {
            if (!mHasValueDiff) {
                return false;
            }

            if (mFirstValueDiff) {
                mPrevValue = value;
                mFirstValueDiff = false;
                return false;
            }
            return true;
        }

        float getDiffValue(float value) {
            float ret = value - mPrevValue;
            mPrevValue = value;
            return ret;
        }

        void clearState() {
            mFirstValueDiff = true;
        }
    }

    /**
     * Class containing a set of Group objects.
     *
     */
    @SuppressWarnings("serial")
    public static class Groups extends ArrayList<Group> {

        public Groups(int capacity) {
            super(capacity);
        }

        @Override
        public boolean equals(Object other) {
            if (other == null || !(other instanceof Groups)) {
                return false;
            }

            Groups otherGroups = (Groups) other;
            if (otherGroups == this) {
                return true;
            }

            if (size() == otherGroups.size()) {
                // Verify that the groups come in the same order and
                // are equal.
                for (int index = 0; index < size(); ++index) {
                    if (!get(index).equals(otherGroups.get(index))) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }

        Groups copy() {
            int size = size();
            if (size > 0) {
                Groups groups = new Groups(size);
                for (int index = 0; index < size; ++index) {
                    groups.add(get(index).copy());
                }
                return groups;
            }
            return null;
        }

        void createXml(Document doc, Element llmElement) {
            if (size() > 0 ) {
                Element groupsElement = doc.createElement(XML_GROUPNAMES_ELEM);
                for (int index = 0; index < size(); ++index) {
                    get(index).createXml(doc, groupsElement);
                }
                llmElement.appendChild(groupsElement);
            }
        }

        private boolean validIndex(int index) {
            return index >= 0 && index < size();
        }

        boolean move(int fromIndex, int toIndex) {
            if (validIndex(fromIndex) && validIndex(toIndex)) {
                Group fromGroup = get(fromIndex);
                Group toGroup = get(toIndex);
                set(toIndex, fromGroup);
                set(fromIndex, toGroup);
                return true;
            }
            return false;
        }
    }
}
