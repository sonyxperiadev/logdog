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

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

/**
 * This class implements a button showing a menu when clicked. It does
 * currently not support an accelerator key.
 */
@SuppressWarnings("serial")
public class MenuButton extends JButton {

    private final JPopupMenu mPopupMenu;
    private MenuButtonListener mListener;

    // We use our own lof and there we don't use borders.
    private Border mEmptyBorder = new EmptyBorder(2, 4, 2, 4);

    void setListener(MenuButtonListener listener) {
        mListener = listener;
    }

    void removeListener() {
        mListener = null;
    }

    private void notifyListener(int id) {
        if (mListener != null) {
            mListener.onSelected(id);
        }
    }

    public interface MenuButtonListener {
        public void onPopup();
        public void onSelected(int id);
    }

    /**
     * Constructor.
     */
    MenuButton(String btnText, String tooltipText, boolean opaqueNoBorder) {
        super(btnText);

        // LOF
        if (opaqueNoBorder) {
            setOpaque(false);
            setBorder(mEmptyBorder);
        }

        setToolTipText(tooltipText);

        mPopupMenu = new JPopupMenu();
        mPopupMenu.setBorder(new LineBorder(Color.gray));

        addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent evt) {
                    if (mListener != null) {
                        mListener.onPopup();
                    }
                    showMenu();
                }
            });
    }

    private void showMenu() {
        mPopupMenu.show(this, 0, getHeight());
    }

    /**
     * Add a menu item to the popup menu associating it with an id.
     *
     * @param id
     * @param text
     */
    void add(final int id, String text, String tooltipText) {
        JMenuItem mi = new JMenuItem(new AbstractAction(text) {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    MenuButton.this.notifyListener(id);
                }
            });
        mi.setToolTipText(tooltipText);
        mPopupMenu.add(mi);
    }

    void add(Action action) {
        JMenuItem mi = new JMenuItem(action);
        mPopupMenu.add(mi);
    }
}
