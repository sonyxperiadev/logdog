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


import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import logdog.utils.Logger;

/**
 * Class for defining and managing trigger events i.e. when a
 * LogSource should be started or stopped.
 */
public class LogSourceTriggerList {

    private Map<LogLineMatcher, Pattern> mPatterns = new HashMap<LogLineMatcher, Pattern>(6);

    // Don't change the names in this enum because they are mapped
    // directly to what's in the xml file and in the UI (LLMView).
    public enum Type {
        None,
        Resume,
        Pause
    };

    static Type parse(String triggerType) {
        for (Type type : Type.values()) {
            if (type.toString().equals(triggerType)) {
                return type;
            }
        }
        return Type.None;
    }

    /**
     * Constructor.
     */
    public LogSourceTriggerList() {
    }

    public Type getTriggerType(String logLine) {
        //TODO Is this method fast enough?
        for (Map.Entry<LogLineMatcher, Pattern> entry : mPatterns.entrySet()) {
            Matcher matcher = entry.getValue().matcher(logLine);
            if (matcher.find()) {
                return entry.getKey().getTriggerType();
            }
        }
        return Type.None;
    }

    public void add(LogLineMatcher llm) {
        if (mPatterns.containsKey(llm)) {
            Logger.log("LogSourceTriggerList: attempted to add the same LLM more than once.");
        } else {
            try {
                String regExp = llm.getRegExp().trim();
                Pattern pattern = LogSource.compileRegExp(regExp);
                mPatterns.put(llm, pattern);
            } catch (PatternSyntaxException excep) {
                Logger.logExcep(excep);
            }
        }
    }

    public void remove(LogLineMatcher llm) {
        if (mPatterns.containsKey(llm)) {
            mPatterns.remove(llm);
        } else {
            Logger.log("LogSourceTriggerList: attempted to remove no existing LLM.");
        }
    }
}
