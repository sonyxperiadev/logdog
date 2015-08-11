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

package logdog.view;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import java.awt.ScrollPane;

import logdog.logdog;
import logdog.utils.Logger;

@SuppressWarnings("serial")
public class AboutDialog extends JDialog implements ActionListener {

    private JTextArea mTextArea;
    private JButton mButton;

    public AboutDialog(Frame owner) {
        super(owner, true);  // modal, BorderLayout
        setTitle("About " + logdog.getFriendlyVersion());

        Container contentPane = getContentPane();

        URL imageURL = getClass().getResource("/resources/splash-whitebg.png");
        try {
            BufferedImage image = ImageIO.read(imageURL);
            if (image != null) {
                ImageIcon imageIcon = new ImageIcon(image);
                JLabel imageLabel = new JLabel(imageIcon);
                contentPane.add(imageLabel, BorderLayout.WEST);
            }
        } catch (IOException excep) {
            Logger.logExcep(excep);
        }

        mTextArea = new JTextArea();
        mTextArea.setMargin(new Insets(10, 10, 10, 10));
        appendTextResource(logdog.getFriendlyVersion(), "LICENSE");
        appendTextResource("jfreechart-1.0.19", "licence-LGPL.txt");
        appendTextResource("hamcrest-core-1.3", "LICENSE.txt");

        UIUtils.useTabToChangeFocus(mTextArea);
        mTextArea.setRows(20);
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.add(mTextArea);
        contentPane.add(scrollPane, BorderLayout.CENTER);

        mButton = new JButton("OK");
        mButton.addActionListener(this);
        contentPane.add(mButton, BorderLayout.SOUTH);

        setMinimumSize(new Dimension(1100, 200));
        setResizable(true);
        pack();
        setLocationRelativeTo(null);  // center on screen

        UIUtils.closeFrameWhenEscapePressed(this.getRootPane(), this);
        setVisible(true);
    }

    private void appendTextResource(String caption, String res) {
        mTextArea.append("----------------------------------------------\n");
        mTextArea.append(caption + "\n\n");

        InputStream stream = null;
        BufferedReader reader = null;
        try {
            stream = getClass().getResourceAsStream("/resources/" + res);
            if (stream != null) {
                reader = new BufferedReader(new InputStreamReader(stream));
                if (reader != null) {
                    String line = null;
                    while ((line = reader.readLine()) != null) {
                        mTextArea.append(line);
                        mTextArea.append("\n");
                    }
                }
            }
        } catch (IOException excep) {
            Logger.logExcep(excep);
        }
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException excep) {
                Logger.logExcep(excep);
            }
        } else {
            mTextArea.append("Failed to load resource '" + res + "'\n\n");
        }
    }

    @Override
    public void actionPerformed(ActionEvent actEvent) {
        setVisible(false);
    }
}
