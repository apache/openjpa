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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;


/**
 * A viewer for source code.
 * The input to this viewer is a set of *.html file names which may or may not have anchors.
 * 
 * @author Pinaki Poddar
 *
 */
@SuppressWarnings("serial")
public class SourceCodeViewer extends JPanel implements ActionListener {
    private final JEditorPane _editor;
    private final JComboBox   _bookmarks;
    private Map<String, File> _files = new TreeMap<String, File>();
    
    public SourceCodeViewer(String dir) {
        super(true);
        setLayout(new BorderLayout());
        
        
        _editor = new JEditorPane();
        _editor.setEditorKit(new HTMLEditorKit());
        _editor.setEditable(false);
        
        File root = getFile(dir);
        if (root != null) {
            List<File> files = new FileScanner("html", true).scan(root);
            for (File f : files) {
                String fileName = f.getAbsolutePath();
                String key = fileName.substring(
                        root.getAbsolutePath().length()+1, 
                        fileName.length()- "html".length()-1)
                        .replace(File.separatorChar,'.');
                _files.put(key, f);
            }
        }
        
        DefaultComboBoxModel model = new DefaultComboBoxModel();
        for (String key : _files.keySet()) {
            model.addElement(key);
        }
        _bookmarks = new JComboBox(model);
        _editor.setEditable(false);
        _bookmarks.setEditable(false);
        
        _bookmarks.addActionListener(this);
        
        add(new JScrollPane(_editor), BorderLayout.CENTER);
        JPanel topPanel = new JPanel();
        ((FlowLayout)topPanel.getLayout()).setAlignment(FlowLayout.LEADING);
        topPanel.add(new JLabel("Select File "));
        topPanel.add(_bookmarks);
        topPanel.add(Box.createHorizontalGlue());
        add(topPanel, BorderLayout.NORTH);
    }
    
    
   
    public void actionPerformed(ActionEvent e) {
        String name = (String)_bookmarks.getSelectedItem();
        try {
            HTMLDocument doc = new HTMLDocument();
            _editor.read(new FileInputStream(_files.get(name)), doc);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    /**
     * Check the given string for a matching file. The string is first
     * tested to see if it is an existing file path. If it does not
     * represent an existing file, it is checked as a resource name of a
     * file. 
     * @param name the file path or resource name
     */
    private File getFile(String name) {
        if (name == null)
            return null;

        File file = new File(name);
        if (file.exists())
            return file;

        URL url = Thread.currentThread().getContextClassLoader().getResource(name); 
        if (url == null) {
            url = getClass().getClassLoader().getResource(name);
        }
        if (url != null) {
            String urlFile = url.getFile();
            if (urlFile != null) {
                File rsrc = new File(URLDecoder.decode(urlFile));
                if (rsrc.exists())
                    return rsrc;
            }
        }

        return null;
    }

    
    InputStream getResource(String resource) {
        InputStream stream = getClass().getResourceAsStream(resource);
        if (stream == null) {
            stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
        }
        return stream;
    }
    
    public static void main(String[] args) throws Exception {
        SourceCodeViewer viewer = new SourceCodeViewer(args.length == 0 ? "source" : args[0]);
        JFrame frame = new JFrame("Source Code Viewer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(viewer);
        frame.pack();
        frame.setVisible(true);
    }
    
    

}
