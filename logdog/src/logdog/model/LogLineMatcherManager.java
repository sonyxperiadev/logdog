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
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import logdog.model.LogLineMatcher.Group;
import logdog.model.LogLineMatcher.Groups;
import logdog.utils.Logger;

/**
 * Model class for handling LogLineMatchers, editing, reading and
 * saving them to files etc.
 *
 */
public class LogLineMatcherManager {

    /*------ Static class members and methods ------*/

    private static final String XML_ROOT_ELEM = "LogLineMatchers";
    private static final String XML_DURATION_ELEM = "Duration";
    private static final String XML_ACTIVE_ATTRIB = "active";  // lower-case is correct
    private static final String XML_TIME_ELEM = "Time";
    private static final String XML_COUNT_ELEM = "Count";

    // String resources.
    private static final String FILE_NOT_READABLE_STR = "File '%s' is not readable";
    private static final String NEW_LLM_NAME_STR = "<New LogLinematcher %d>";

    // Make this class observable using LLMMgrListener.
    private static ArrayList<LLMMgrListener> mLLMMgrListeners = new ArrayList<LLMMgrListener>(2);
    private static ArrayList<LLMEditListener> mLLMEditListeners = new ArrayList<LLMEditListener>(2);

    public static void addLLMMgrListener(LLMMgrListener listener) {
        if (listener != null && !mLLMMgrListeners.contains(listener)) {
            mLLMMgrListeners.add(listener);
        }
    }

    public static void removeLLMMgrListener(LLMMgrListener listener) {
        if (listener != null && mLLMMgrListeners.contains(listener)) {
            mLLMMgrListeners.remove(listener);
        }
    }

    public static void addLLMEditListener(LLMEditListener listener) {
        if (listener != null && !mLLMEditListeners.contains(listener)) {
            mLLMEditListeners.add(listener);
        }
    }

    public static void removeLLMEditListener(LLMEditListener listener) {
        if (listener != null && mLLMEditListeners.contains(listener)) {
            mLLMEditListeners.remove(listener);
        }
    }

    private void readDurationXml(Document doc) {

        NodeList durationElems = doc.getElementsByTagName(XML_DURATION_ELEM);
        if (durationElems.getLength() > 0) {
            Node durationElem = durationElems.item(0);
            NamedNodeMap attribs = durationElem.getAttributes();
            Node activeNode = attribs.getNamedItem(XML_ACTIVE_ATTRIB);
            if (activeNode != null && activeNode.getNodeValue().equals(XML_TIME_ELEM)) {
                mUseTimeDuration = true;
                mUseCountDuration = false;
            } else {
                mUseTimeDuration = false;
                mUseCountDuration = true;
            }

            for (Node durationChildElem = durationElem.getFirstChild(); durationChildElem != null;
                 durationChildElem = durationChildElem.getNextSibling()) {

                Node childElem = durationChildElem.getFirstChild();
                if (childElem == null) {  // for newlines, "#text"
                    continue;
                }

                String elemName = durationChildElem.getNodeName();
                String elemValue = childElem.getNodeValue();

                if (elemName.equals(XML_TIME_ELEM)) {
                    mTimeDuration = parseIntValue(elemValue, TIMEDURATION_MIN, TIMEDURATION_MAX);
                } else if (elemName.equals(XML_COUNT_ELEM)) {
                    mCountDuration = parseIntValue(elemValue, COUNTDURATION_MIN, COUNTDURATION_MAX);
                }
            }
        }
    }

    private void readLLMXmlRegisterActivate(NodeList llmElems) {

        // Loop over all xml <LogLineMatcher> elements.
        int llmCount = llmElems.getLength();
        for (int llmIndex = 0; llmIndex < llmCount; ++llmIndex) {
            Node llmElem = llmElems.item(llmIndex);
            LogLineMatcher llm = LogLineMatcher.createFromXml(llmElem, this);
            if (llm != null) {
                // Always add the LLM so we can edit, save it etc but
                // only register to LS if the LLM is enabled.
                add(llm);
                if (llm.isEnabled()) {
                    llm.registerToLogSource();
                    notifyRegisterLLM(llm);
                }
            }
        }

        if (getLLMCount() != llmCount) {
            //TODO log something here because we have not loaded every LLM.
        }

        // Activate all LogLineMatchers in one go.
        setLLMsActivate(true);
    }

    /**
     * Load several LogLineMatcher objects from an xml file.
     *
     * @param xmlPath
     *
     * @return new LogLineMatcherManager instance, returns null on error.
     */
    public static LogLineMatcherManager createFromFile(final File xmlPath)
        throws IOException {

        try {
            if (!xmlPath.canRead()) {
                throw new IOException(String.format(FILE_NOT_READABLE_STR, xmlPath));
            }

            // Create in-memory xml document.
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(xmlPath);
            doc.getDocumentElement().normalize();

            // Get all LogLineMatcher xml elements.
            NodeList llmElems = doc.getElementsByTagName(LogLineMatcher.XML_LLM_ELEM);
            int llmCount = llmElems.getLength();

            LogLineMatcherManager llmMgr = new LogLineMatcherManager(xmlPath, llmCount);
            llmMgr.readDurationXml(doc);
            llmMgr.readLLMXmlRegisterActivate(llmElems);

            return llmMgr;

        } catch (ParserConfigurationException excep) {
            Logger.logExcep(excep);
        } catch (FactoryConfigurationError excep) {
            Logger.logExcep(excep);
        } catch (IOException excep) {
            Logger.logExcep(excep);
        } catch (SAXException excep) {
            Logger.logExcep(excep);
        } catch (SecurityException excep) {
            Logger.logExcep(excep);
        }

        return null;
    }


    /*------ Object members and methods ------*/

    File mXmlPath;

    public File getFilePath() {
        return mXmlPath;
    }

    public void setFilePath(File xmlPath) {
        mXmlPath = xmlPath;
    }

    ArrayList<LogLineMatcher> mLLMs;
    public ArrayList<LogLineMatcher> getLLMs() {
        return mLLMs;
    }

    private int mNewLLMId;

    private static final int TIMEDURATION_MIN = 10;  // minutes
    private static final int TIMEDURATION_MAX = 24 * 60; // minutes
    private static final int COUNTDURATION_MIN = 1000;
    private static final int COUNTDURATION_MAX = 100000;

    private boolean mUseTimeDuration = true;
    private int mTimeDuration = TIMEDURATION_MIN;    // minutes
    private boolean mUseCountDuration;
    private int mCountDuration = COUNTDURATION_MIN;

    private boolean mUseTimeDuration_Edit;
    private int mTimeDuration_Edit;
    boolean mUseCountDuration_Edit;
    private int mCountDuration_Edit;

    public boolean useTimeDuration() {return mUseTimeDuration;};
    public int getTimeDuration() {return mTimeDuration;};
    public boolean useCountDuration() {return mUseCountDuration;};
    public int getCountDuration() {return mCountDuration;};

    public boolean useTimeDuration_Edit() {return mUseTimeDuration_Edit;};
    public String getTimeDuration_Edit() {return String.valueOf(mTimeDuration_Edit);};
    public boolean useCountDuration_Edit() {return mUseCountDuration_Edit;};
    public String getCountDuration_Edit() {return String.valueOf(mCountDuration_Edit);};

    /**
     * Constructor.
     *
     * @param xmlPath - path to logdog xml file, allowed to be null
     * @param count - number of LogLineMatchers
     *
     * @return
     */
    public LogLineMatcherManager(final File xmlPath, int count) {
        mXmlPath = xmlPath;
        mLLMs = new ArrayList<LogLineMatcher>(count);
    }

    /**
     * Called before starting an edit session (also when
     * creating). Copies current values from ordinary members in all
     * LogLineMatchers into the corresponding edit members.
     */
    public void editBegin() {
        mNewLLMId = 0;

        mUseTimeDuration_Edit = mUseTimeDuration;
        mTimeDuration_Edit = mTimeDuration;
        mUseCountDuration_Edit = mUseCountDuration;
        mCountDuration_Edit = mCountDuration;

        for (LogLineMatcher llm : mLLMs) {
            llm.editBegin();
            llm.setModified();
        }
    }

    private static int parseIntValue(String value, int min, int max) {
        int ret = min;
        try {
            ret = Integer.parseInt(value);
        } catch (NumberFormatException excep) {
            Logger.logExcep(excep);
        }

        if (ret < min) {
            ret = min;
        } else if (ret > max) {
            ret = max;
        }
        return ret;
    }

    public void editUpdate(boolean useTimeDuration, String timeDurationValue,
                           boolean useCountDuration, String countDurationValue) {

        mUseTimeDuration_Edit = useTimeDuration;
        mTimeDuration_Edit = parseIntValue(timeDurationValue, TIMEDURATION_MIN, TIMEDURATION_MAX);

        mUseCountDuration_Edit = useCountDuration;
        mCountDuration_Edit = parseIntValue(countDurationValue, COUNTDURATION_MIN, COUNTDURATION_MAX);
    }

    public void editSave() {
        try {
            setLLMsActivate(false);
            editVerify();
            saveToFile(true);  // use content in edit members

            // This will recreate the chart panel in ChartView because we
            // cannot add/remove series, we have to get rid of the entire
            // chart...
            notifyEditCommitBegin();
            editCommit();
        } finally {
            setLLMsActivate(true);
        }
    }

    public void editCancel() {
        // Remove all *added* LogLineMatchers. They are never
        // registered with a log source so no need to unregister them.
        for (int index = 0; index < mLLMs.size();) {
            LogLineMatcher llm = mLLMs.get(index);
            if (llm.isNew()) {
                mLLMs.remove(index);
            } else {
                llm.editCancel();
                ++index;
            }
        }
    }

    public void editDelete(LogLineMatcher llm) {
        // Mark the LogLineMatcher as deleted. It will really be
        // deleted when editCommit() is called.
        llm.setDeleted();
        for (LLMEditListener listener : mLLMEditListeners) {
            listener.onDeleted(llm);
        }
    }

    public void editAdd() {
        LogLineMatcher llm =
            new LogLineMatcher(String.format(NEW_LLM_NAME_STR, ++mNewLLMId),
                               false, true, false, LogSource.getSourceNames()[0],
                               0, LogSourceTriggerList.Type.None.toString(), this);
        llm.editBegin();

        // Mark the LogLineMatcher as new. It will be deleted from
        // mLLMs if the edit is canceled by calling editCancel().
        llm.setNew();
        add(llm);
        for (LLMEditListener listener : mLLMEditListeners) {
            listener.onAdded(llm);
        }
    }

    /**
     * Move a LLM within 'mLLMs' and notify listeners (LLMView).
     *
     * @param fromIndex
     * @param toIndex
     */
    public void moveLLM(int fromIndex, int toIndex) {
        if (isValidLLMIndex(fromIndex) && isValidLLMIndex(toIndex) && fromIndex != toIndex) {
            LogLineMatcher fromLLM = mLLMs.get(fromIndex);
            LogLineMatcher toLLM = mLLMs.get(toIndex);
            mLLMs.set(toIndex, fromLLM);
            mLLMs.set(fromIndex, toLLM);

            for (LLMEditListener listener : mLLMEditListeners) {
                listener.onMoved(fromIndex, toIndex);
            }
        }
    }

    public void editAddGroup(LogLineMatcher llm, Group group) {
        if (mLLMs.contains(llm)) {
            llm.editAddGroup(group);
            for (LLMEditListener listener : mLLMEditListeners) {
                listener.onGroupAdded(llm, group);
            }
        }
    }

    public void editDeleteGroup(LogLineMatcher llm, int groupIndex) {
        if (mLLMs.contains(llm)) {
            Groups groups = llm.getGroups_Edit();
            if (groups != null) {
                groups.remove(groupIndex);
                for (LLMEditListener listener : mLLMEditListeners) {
                    listener.onGroupDeleted(llm, groupIndex);
                }
            }
        }
    }

    /**
     * Move a LogLineMatcher.Group in LogLineMatcher.mGroups_Edit from
     * one position to another. Called when editing an LLM.
     *
     * @param llm
     * @param fromGroupIndex
     * @param toGroupIndex
     */
    public void editMoveGroup(LogLineMatcher llm, int fromGroupIndex, int toGroupIndex) {
        if (mLLMs.contains(llm) && fromGroupIndex != toGroupIndex) {
            Groups groups = llm.getGroups_Edit();
            if (groups != null && groups.move(fromGroupIndex, toGroupIndex)) {
                for (LLMEditListener listener : mLLMEditListeners) {
                    listener.onGroupMoved(llm, fromGroupIndex, toGroupIndex);
                }
            }
        }
    }

    /**
     * Called when an edit session is about to be saved. Verify that
     * everything looks ok, for instance that the regexps can be
     * compiled and that the log sources exists.
     */
    private void editVerify() {
        for (LogLineMatcher llm : mLLMs) {
            llm.editVerify();  // throws LogLineMatcher.InvalidException
        }
    }

    /**
     * Called when all LogLineMatchers have successfully been saved to
     * file. Commit all edits for all LogLineMatchers.
     */
    private void editCommit() {

        mUseTimeDuration = mUseTimeDuration_Edit;
        mTimeDuration = mTimeDuration_Edit;
        mUseCountDuration = mUseCountDuration_Edit;
        mCountDuration = mCountDuration_Edit;

        for (int index = 0; index < mLLMs.size();) {
            LogLineMatcher llm = mLLMs.get(index);
            if (!llm.isNew()) {
                llm.unRegisterFromLogSource();
                notifyUnRegisterLLM(llm);
            }
            if (llm.isDeleted()) {
                mLLMs.remove(index);
            } else {
                llm.editCommit();
                llm.registerToLogSource();
                notifyRegisterLLM(llm);
                ++index;
            }
        }
    }

    private void add(LogLineMatcher llm) {
         mLLMs.add(llm);
    }

    private boolean isValidLLMIndex(int index) {
        return index >= 0 && index < getLLMCount();
    }

    public int getLLMCount() {
        return mLLMs != null ? mLLMs.size() : 0;
    }

    private void setLLMsActivate(boolean active) {
        if (mLLMs != null) {
            for (LogLineMatcher llm : mLLMs) {
                llm.setActive(active);
            }
        }
    }

    private void notifyRegisterLLM(LogLineMatcher llm) {
        for (LLMMgrListener listener : mLLMMgrListeners) {
            listener.registeringLLM(llm);
        }
    }

    private void notifyUnRegisterLLM(LogLineMatcher llm) {
        for (LLMMgrListener listener : mLLMMgrListeners) {
            listener.unRegisteringLLM(llm);
        }
    }

    private void notifyEditCommitBegin() {
        for (LLMEditListener listener : mLLMEditListeners) {
            listener.onEditCommitBegin(this);
        }
    }

    public void unRegisterAllLLMs() {
        if (mLLMs != null) {
            for (LogLineMatcher llm : mLLMs) {
                if (llm != null) {
                    llm.unRegisterFromLogSource();
                    notifyUnRegisterLLM(llm);
                }
            }
            mLLMs.clear();
        }
    }

    public void clearStateAllLLMs() {
        if (mLLMs != null) {
            for (LogLineMatcher llm : mLLMs) {
                if (llm != null) {
                    llm.clearState();
                }
            }
        }
    }

    public void clearStateLLMsPresentationId(int presentationId) {
        if (mLLMs != null) {
            for (LogLineMatcher llm : mLLMs) {
                if (llm != null && llm.getPresentationId() == presentationId) {
                    llm.clearState();
                }
            }
        }
    }

    public void setFileLogSource(FileLogSource fileLogSource) {
        for (int index = 0; index < mLLMs.size(); ++index) {
            LogLineMatcher llm = mLLMs.get(index);
            llm.unRegisterFromLogSource();
            notifyUnRegisterLLM(llm);
        }
        for (int index = 0; index < mLLMs.size(); ++index) {
            LogLineMatcher llm = mLLMs.get(index);
            llm.registerToLogSource(fileLogSource);
            notifyRegisterLLM(llm);
        }
        // We must start fileLogSource manually since it is set
        // inactive in the c-tor. In addition, all LogLineMatchers
        // need to be registered before we start feeding them.
        fileLogSource.setActive(true);
        fileLogSource.startSourcing();
    }

    /**
     * Called by LogLineMatcher objects when a log line matches its regexp.
     *
     * @param llm
     * @param seriesIndex
     * @param millis
     * @param value
     */
    public void onMatchedLogLine(LogLineMatcher llm, int seriesIndex,
                                 Date date, float value) {
        if (mLLMMgrListeners != null)  {
            for (LLMMgrListener listener : mLLMMgrListeners) {
                listener.onMatchedLogLine(llm, seriesIndex, date, value);
            }
        }
    }

    private Element createDurationXml(Document doc, boolean useEdit) {

        Element durationElem = doc.createElement(XML_DURATION_ELEM);
        String active = XML_TIME_ELEM;
        if ((useEdit && !mUseTimeDuration_Edit) ||
            (!useEdit && !mUseTimeDuration)) {
            active = XML_COUNT_ELEM;
        }
        durationElem.setAttribute(XML_ACTIVE_ATTRIB, active);

        Element timeDurationElem = doc.createElement(XML_TIME_ELEM);
        String timeDurationString = String.valueOf(useEdit ? mTimeDuration_Edit : mTimeDuration);
        timeDurationElem.appendChild(doc.createTextNode(timeDurationString));

        Element countDurationElem = doc.createElement(XML_COUNT_ELEM);
        String countDurationString = String.valueOf(useEdit ? mCountDuration_Edit : mCountDuration);
        countDurationElem.appendChild(doc.createTextNode(countDurationString));

        durationElem.appendChild(timeDurationElem);
        durationElem.appendChild(countDurationElem);

        return durationElem;
    }

    /**
     * Save this LogLineMatcherManager and all LogLineMatchers to an
     * xml file.
     *
     * @param useEdit If true the '_Edit' members are used when
     * saving. This is the case when we are saving after editing
     * LogLineMatchers in LLMView.
     */
    public void saveToFile(boolean useEdit) {

        if (mXmlPath == null) {
            throw new NoPathException();
        }

        try {
            // Create in-memory xml document.
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            Document doc = docBuilder.newDocument();
            doc.setXmlStandalone(true);
            Element rootElem = doc.createElement(XML_ROOT_ELEM);
            doc.appendChild(rootElem);

            rootElem.appendChild(createDurationXml(doc, useEdit));

            // Loop over all LogLineMatchers. Keep going even if one
            // LogLineMatcher fails.
            for(LogLineMatcher llm : mLLMs) {
                if (llm != null) {
                    llm.createXml(doc, rootElem, useEdit);  // throws
                }
            }

            // Write xml document file
            DOMSource source = new DOMSource(doc);
            StreamResult dest = new StreamResult(mXmlPath);

            TransformerFactory transFact = TransformerFactory.newInstance();
            Transformer trans = transFact.newTransformer();
            trans.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            trans.setOutputProperty(OutputKeys.INDENT, "yes");

            // Output to console for testing
            // StreamResult dest = new StreamResult(System.out);
            trans.transform(source, dest);

        } catch (ParserConfigurationException excep) {
            Logger.logExcep(excep);
        } catch (TransformerException excep) {
            Logger.logExcep(excep);
        } catch (TransformerFactoryConfigurationError excep) {
            Logger.logExcep(excep);
        }
    }

    public class NoPathException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public NoPathException() {
            super("Missing path, cannot save.");
        }
    }
}
