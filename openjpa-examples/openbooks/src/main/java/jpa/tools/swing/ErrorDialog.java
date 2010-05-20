/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
*/
package jpa.tools.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;

@SuppressWarnings("serial")
public class ErrorDialog extends JDialog {
    private static List<String> filters = Arrays.asList("java.awt.", "javax.swing.", "sun.reflect.");
    private static AttributeSet red, black;
    static {
        StyleContext ctx = StyleContext.getDefaultStyleContext();
        red = ctx.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, Color.RED);
        red = ctx.addAttribute(red, StyleConstants.Bold, true);
        red = ctx.addAttribute(red, StyleConstants.FontSize, 12);
        red = ctx.addAttribute(red, StyleConstants.FontFamily, "Courier");
        black = ctx.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, Color.BLACK);
        black = ctx.addAttribute(black, StyleConstants.Bold, false);
        black = ctx.addAttribute(black, StyleConstants.FontFamily, "Courier");
    }

    public ErrorDialog(JComponent owner, Throwable t) {
        super();
        setModal(true);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
//        Icon icon = Images.ERROR;
//        setIconImage(((ImageIcon)icon).getImage());
        addException(t);
        pack();
        SwingHelper.position(this, owner);
    }
    
    public ErrorDialog(Throwable t) {
        this(null, t);
    }
    
    void addException(Throwable t) {
        setTitle("Error");
        String txt = t.getClass().getName() + ":" + t.getLocalizedMessage();
        JTextArea message = new JTextArea(txt);
        message.setLineWrap(true);
        message.setWrapStyleWord(true);
        message.setForeground(Color.RED);
        message.setText(txt);
        message.setEditable(false);
        
        JTextPane window = new JTextPane();
        printStackTrace(t, window);
        window.setEditable(false);
        JScrollPane pane = new JScrollPane(window, 
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, 
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        pane.setPreferredSize(new Dimension(400, 200));
        pane.setBorder(BorderFactory.createTitledBorder("Stacktrace"));
        pane.setPreferredSize(new Dimension(400, 200));
        JPanel main = new JPanel();
        main.setLayout(new BorderLayout());
        main.add(message, BorderLayout.NORTH);
        main.add(pane, BorderLayout.CENTER);
        JPanel buttonPanel = new JPanel();
        JButton ok = new JButton("OK");
        ok.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ErrorDialog.this.dispose();
            }
        });
        buttonPanel.add(ok);
        main.add(buttonPanel, BorderLayout.SOUTH);
        getContentPane().add(main);
    }
    static String NEWLINE = "\r\n";
    StringBuilder printStackTrace(Throwable t, JTextPane text) {
        String message = t.getClass().getName() + ": " + t.getMessage() + NEWLINE;
        text.setCaretPosition(text.getDocument().getLength());
        text.setCharacterAttributes(red, false);
        text.replaceSelection(message);
        StackTraceElement[] traces = t.getStackTrace();
        text.setCharacterAttributes(black, false);
        for (StackTraceElement e : traces) {
            if (!isFiltered(e.getClassName())) {
                String str = "   " + e.toString() + NEWLINE;
                text.setCaretPosition(text.getDocument().getLength());
                text.replaceSelection(str);
            }
        }
        
        Throwable cause = t.getCause();
        if (cause !=null && cause != t) {
            printStackTrace(cause, text);
        }
        return null;
    }
    
    StringBuilder toString(StackTraceElement[] traces) {
        StringBuilder error = new StringBuilder();
        for (StackTraceElement e : traces) {
            if (!isFiltered(e.getClassName())) {
                String str = e.toString();
                error.append(str).append("\r\n");
            }
        }
        return error;
    }
    
    private boolean isFiltered(String className) {
        for (String s : filters) {
            if (className.startsWith(s))
                return true;
        }
        return false;
    }
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        new ErrorDialog(new IllegalArgumentException(
                "This is test error with very long line of error message that should not be in a single line"))
        .setVisible(true);
    }

}
