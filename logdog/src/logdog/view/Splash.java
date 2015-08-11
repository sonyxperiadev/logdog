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

import javax.imageio.ImageIO;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;

import logdog.logdog;
import logdog.utils.Logger;

@SuppressWarnings("serial")
public class Splash extends JWindow {

    private BufferedImage mImage;
    private int mWidth;
    private int mHeight;
    private Font mPlainFont = new Font("Arial", Font.ITALIC, 12);

    public Splash(String resPath) {
        try {
            URL imageURL = Splash.class.getResource(resPath);
            mImage = ImageIO.read(imageURL);

            mWidth = mImage.getWidth(null);
            mHeight = mImage.getHeight(null);

            Graphics2D g2d = (Graphics2D) mImage.getGraphics();
            g2d.setFont(mPlainFont);
            g2d.setColor(new Color(128, 128, 128));
            String logdogVersion = logdog.getFriendlyVersion();
            Rectangle2D stringRect = g2d.getFontMetrics().getStringBounds(logdogVersion, g2d);
            g2d.drawString(logdogVersion, (int) (mWidth - stringRect.getWidth()) / 2,
                           (int) (mHeight - stringRect.getHeight() / 2 + 2));

            setSize(mWidth, mHeight);
            setLocationRelativeTo(null);
            setAlwaysOnTop(true);  // Needed for Windows
            setVisible(true);
            paintLoop();
        } catch (IOException excep) {
            excep.printStackTrace();
        }
    }

    private void paintLoop() {
        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    for (int i = 0; i < 20; i++) {
                        SwingUtilities.invokeAndWait(mRepaintRunnable);
                        Thread.sleep(100);
                    }
                    dispose();
                } catch (InvocationTargetException excep) {
                    excep.printStackTrace();
                } catch (InterruptedException excep) {
                    excep.printStackTrace();
                }
            }
        }).start();
    }

    private Runnable mRepaintRunnable = new Runnable() {
        @Override
        public void run() {
            if (logdog.DEBUG) {
                Logger.log("Splash.mRepaintRunnable.run() entering");
            }
            repaint();
        }
    };

    public void paint(Graphics graphics) {
        if (logdog.DEBUG) {
            Logger.log("Splash.paint() entering");
        }
        if (graphics instanceof Graphics2D) {
            graphics.drawImage(mImage, 0, 0, null);
        }
    }
}
