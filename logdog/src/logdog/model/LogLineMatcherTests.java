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

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.Random;

public class LogLineMatcherTests {

    public static LogLineMatcher createValidLLM(int id, int groupCount) {
        // 4 groups in this regexp:
        String mValidRegExp = ".*?V \\[PRM\\] .*? colorTemp\\[([0-9].*?)\\] aeLevel\\[([0-9].*?)\\] gain\\[([0-9].*?)\\] convert-gain\\[([0-9].*?)\\] .*?";
        Random random = new Random();
        int sourceIndex = random.nextInt(LogSource.COUNT);
        String logSourceName = LogSource.sSourceName[sourceIndex];

        boolean hasGroups = groupCount > 0;
        LogLineMatcher.Groups groups = hasGroups ? new LogLineMatcher.Groups(groupCount) : null;

        if (hasGroups) {
            String[] exGroupNames = {
                "colorTemp",
                "aeLevel",
                "gain",
                "convert-gain"
            };

            for (int index = 0; index < groupCount; ++index) {
                groups.add(new LogLineMatcher.Group(exGroupNames[index % exGroupNames.length]));
            }
        }

        int llmCount = 1;
        LogLineMatcherManager llmMgr = new LogLineMatcherManager(null, llmCount);
        return new LogLineMatcher(String.format("Some valid matcher for %s %d", logSourceName, id),
                                  false, true, false, logSourceName, mValidRegExp, groups, 0,
                                  LogSourceTriggerList.Type.None.toString(), llmMgr);
    }

    private LogLineMatcher createInvalidLLM() {
        try {
            int llmCount = 1;
            LogLineMatcherManager llmMgr = new LogLineMatcherManager(null, llmCount);
            return new LogLineMatcher("Some invalid matcher", false, true, false,
                                      LogSource.sSourceName[LogSource.LOGCAT_MAIN],
                                      "\\d{2:freed ([0-9.*?)K, .*?$", null, 0,
                                      LogSourceTriggerList.Type.None.toString(), llmMgr);
        } catch (LogLineMatcher.InvalidException excep) {
        }
        return null;
    }

    @Test
    public void constructValidRegExp() {
        try {
            LogLineMatcher llm = createValidLLM(1, 4);
            assertNotNull(llm);
            assertTrue(llm.isValid(false));
        } catch (Throwable th) {
            assert(false);
        }
    }

    @Test
    public void constructInvalidRegExp() {
        LogLineMatcher llm = createInvalidLLM();
        assertNull(llm);
    }

   // @Test
    // public void saveOneValidToFile() {
    //     File xmlPath = new File("./test_save_one_valid.logdog");
    //     xmlPath.delete();

    //     LogLineMatcher llm = createValidLLM(1, 4);
    //     assertNotNull(llm);
    //     assertTrue(llm.isValid());
    //     ArrayList<LogLineMatcher> llms = new ArrayList<LogLineMatcher>(1);
    //     llms.add(llm);

    //     assertTrue(LogLineMatcher.saveToFile(xmlPath, llms));
    // }

    // @Test
    // public void saveManyValidToFile() {
    //     File xmlPath = new File("./test_save_many_valid.logdog");
    //     xmlPath.delete();

    //     int count = 20;
    //     ArrayList<LogLineMatcher> llms = new ArrayList<LogLineMatcher>(count);
    //     for(int index = 0; index < count; ++index) {
    //         LogLineMatcher llm = createValidLLM(index, index);
    //         assertNotNull(llm);
    //         assertTrue(llm.isValid());
    //         llms.add(llm);
    //     }

    //     assertTrue(LogLineMatcher.saveToFile(xmlPath, llms));
    // }

    // @Test
    // public void saveOneInvalidToFile() {
    //     LogLineMatcher llm = createInvalidLLM();
    //     assertNull(llm);

    //     ArrayList<LogLineMatcher> llms = new ArrayList<LogLineMatcher>(1);
    //     llms.add(llm);

    //     File xmlPath = new File("./test_save_one_invalid.logdog");
    //     xmlPath.delete();

    //     assertTrue(!LogLineMatcher.saveToFile(xmlPath, llms));
    // }

    // private void saveLoadCompareWithGroupNames(int groupCount) {
    //     LogLineMatcher llm = createValidLLM(1, groupCount);
    //     assertNotNull(llm);
    //     assertTrue(llm.isValid());

    //     File xmlPath = new File("./test_save_load_compare.logdog");
    //     xmlPath.delete();
    //     ArrayList<LogLineMatcher> llmSaved = new ArrayList<LogLineMatcher>(1);
    //     llmSaved.add(llm);
    //     assertTrue(LogLineMatcher.saveToFile(xmlPath, llmSaved));

    //     ArrayList<LogLineMatcher> llmLoaded;
    //     try {
    //         llmLoaded = LogLineMatcher.loadFromFile(xmlPath);
    //         assertNotNull(llmLoaded);
    //         assertTrue(llmLoaded.equals(llmSaved));
    //     } catch (IOException e) {
    //         assertTrue(false);
    //     }
    // }

    // @Test
    // public void saveLoadCompareWithGroupNames() {
    //     saveLoadCompareWithGroupNames(4);
    // }

    // @Test
    // public void saveLoadCompareWithoutGroupNames() {
    //     saveLoadCompareWithGroupNames(0);
    // }

    // @Test
    // public void loadFromFile() {
    //     File xmlPath = new File("./test/logdog/test_load.logdog");
    //     ArrayList<LogLineMatcher> llms;
    //     try {
    //         llms = LogLineMatcher.loadFromFile(xmlPath);
    //         assertNotNull(llms);
    //         assertEquals(llms.size(), 20);
    //     } catch (IOException e) {
    //         assertTrue(false);
    //     }
    // }
}
