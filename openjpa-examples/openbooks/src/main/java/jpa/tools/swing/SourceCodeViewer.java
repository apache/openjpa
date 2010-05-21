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
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;

import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;


/**
 * A viewer for source code.
 * The input to this viewer is a root URL and set of anchors.
 * The viewer shows the anchors in a combo-box and displays the
 * corresponding HTML in the main editor. 
 * 
 * @author Pinaki Poddar
 *
 */
@SuppressWarnings("serial")
public class SourceCodeViewer extends JPanel implements ActionListener {
    private final JEditorPane _editor;
    private final JComboBox   _bookmarks;
    private Map<String, String> _anchors;
    private String _root;
    
    /**
     * Create a Source Code Viewer for a set of anchors.
     * @param root the root url 
     * @param anchors the key is a visible text and value is the URL
     * relative to the root.
     */
    public SourceCodeViewer(String root, Map<String,String> anchors) {
        super(true);
        setLayout(new BorderLayout());
        
        _anchors = anchors;
        _root = root;
        _editor = new JEditorPane();
        _editor.setContentType("text/html");
        _editor.setFont(new Font("Courier New", Font.PLAIN, 16));
        _editor.setEditable(false);
        
        DefaultComboBoxModel model = new DefaultComboBoxModel();
        for (String key : anchors.keySet()) {
            model.addElement(key);
        }
        _bookmarks = new JComboBox(model);
        _editor.setEditable(false);
        _bookmarks.setEditable(false);
        
        _bookmarks.addActionListener(this);
        
        add(new JScrollPane(_editor), BorderLayout.CENTER);
        JPanel topPanel = new JPanel();
        ((FlowLayout)topPanel.getLayout()).setAlignment(FlowLayout.LEADING);
        topPanel.add(new JLabel("Go to "));
        topPanel.add(_bookmarks);
        topPanel.add(Box.createHorizontalGlue());
        add(topPanel, BorderLayout.NORTH);
        
        if (_anchors != null && !_anchors.isEmpty())
           showPage(_anchors.keySet().iterator().next());
    }
   
    public void actionPerformed(ActionEvent e) {
        String anchor = (String)_bookmarks.getSelectedItem();
        showPage(anchor);
    }
    
    private void showPage(String anchor) {
        try {
            _editor.setPage(_root + _anchors.get(anchor));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        
    }
}
