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

package logdog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;

import logdog.controller.MainController;
import logdog.utils.ConsoleLogger;
import logdog.utils.FileLogger;
import logdog.utils.Logger;
import logdog.utils.Utils;
import logdog.view.Splash;
import logdog.view.UIUtils;

public class logdog {

    private static final String sName = "logdog";
    static final String sThisVersion = "0.9.5";

    public static boolean DEBUG = false;

    private static final String NEWER_VERSION_TITLE = "Newer version";
    private static final String NEWER_VERSION_MSG_FORMAT =
        "\nThere is a newer version of %1$s available (%s). You are using version %s.\n\n" +
        "Please exit the program and run this command in a terminal window to upgrade:\n" +
        "$ sudo apt-get update\n$ sudo apt-get install %1$s\n\n";
    private static final String DONT_SHOW_AGAIN = "Do not show this message again.";

    public static String getFriendlyVersion() {
        return String.format("%s %s", sName, sThisVersion);
    }

    private static void parseArgs(String[] args) {
        if (args.length <= 0) {
            return;
        }
        for (int argIdx = 0; argIdx < args.length; ++argIdx) {
            final String crntOption = args[argIdx];
            if (crntOption.equals("-lf")) {
                Logger.addListener(new FileLogger());
            } else if (crntOption.equals("-lc")) {
                Logger.addListener(new ConsoleLogger());
            } else if (crntOption.equals("-d")) {
                DEBUG = true;
            } else if (crntOption.equals("-h")) {
                System.out.printf("usage: %s [-lf] [-lc] [-d] [-h]\n", sName);
                System.out.println("-lf log to file");
                System.out.println("-lc log to console");
                System.out.println("-d  debug, you also need either -lf or -lc");
                System.out.println("-h  this help");
                System.exit(0);
            } else if (!Utils.emptyString(crntOption)) {
                System.out.printf("Unknown option: %s\n", crntOption);
            }
        }
    }

    public static void main(String[] args) {
        parseArgs(args);
        Prefs prefs = new Prefs();
        UIUtils.load(prefs);
        new Splash("/resources/splash-whitebg.png");

        // Model created when loading files.
        MainController mainController = new MainController();
        mainController.showChartView(prefs);
    }
}
