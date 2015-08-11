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



public class LogSourceTests {

    @Test
    public void registerValidMatcher() {
        try {
            LogLineMatcher llm = LogLineMatcherTests.createValidLLM(1, 2);
            assertNotNull(llm);
            assertTrue(llm.isValid(false));

            LogSource logSource = LogSource.findLogSource("logcat_main", true);
            logSource.addListener(llm);
            assertNotNull(logSource);
            assertTrue(logSource.isAlive());
        } catch (Throwable th) {
            assert(false);
        }
    }

    @Test
    public void registerInvalidLogSource() {
        try {
            LogLineMatcher llm = LogLineMatcherTests.createValidLLM(1, 4);
            assertNotNull(llm);
            assertTrue(llm.isValid(false));

            // This will also start the LogSource
            LogSource logSource = LogSource.findLogSource("jabba_log", true);
            assertNull(logSource);
        } catch (Throwable th) {
            assert(false);
        }
    }

    @Test
    public void registerNullMatcher() {
        try {
            // This will also start the LogSource
                LogSource logSource = LogSource.findLogSource("logcat_system", true);
            assertNull(logSource);
        } catch (Throwable th) {
            assert(false);
        }
    }
}
