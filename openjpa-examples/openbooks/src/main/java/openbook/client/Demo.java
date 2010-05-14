/*
 * Copyright 2010-2012 Pinaki Poddar
 *
 *
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
package openbook.client;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

import jpa.tools.swing.AttributeLegendView;
import jpa.tools.swing.ConfigurationViewer;
import jpa.tools.swing.ErrorDialog;
import jpa.tools.swing.GraphicOutputStream;
import jpa.tools.swing.MetamodelView;
import jpa.tools.swing.PowerPointViewer;
import jpa.tools.swing.PreparedQueryViewer;
import jpa.tools.swing.ScrollingTextPane;
import jpa.tools.swing.SourceCodeViewer;
import jpa.tools.swing.StatusBar;
import jpa.tools.swing.SwingHelper;
import openbook.domain.Customer;
import openbook.server.OpenBookService;
import openbook.server.ServiceFactory;
import openbook.util.PropertyHelper;

import org.apache.openjpa.conf.OpenJPAVersion;
import org.apache.openjpa.persistence.OpenJPAPersistence;

/**
 * A graphical user interface based client of OpenBooks for demonstration.
 *  
 * @author Pinaki Poddar
 *
 */
@SuppressWarnings("serial")
public class Demo extends JFrame implements Thread.UncaughtExceptionHandler {
    private static final Dimension TAB_VIEW = new Dimension(1400,800);
    private static final Dimension OUT_VIEW = new Dimension(1400,200);
    private static final Dimension NAV_VIEW = new Dimension(400,1000);

    /**
     * The actions invoked by this sample demonstration.
    */
    private Action _root;
    private Action _about;
    private Action _buyBook;
    private Action _deliver;
    private Action _supply;       
    private Action _viewConfig;  
    private Action _viewDomain; 
    private Action _viewData;  
    private Action _viewSource;
    private Action _viewQuery;
    
    /**
     * The primary graphic widgets used to invoke and display the results of the actions.
     */
    private JToolBar    _toolBar;
    private JTree       _navigator;
    private JTabbedPane _tabbedPane;
    private JTabbedPane _outputPane;
    private StatusBar   _statusBar;
    private ScrollingTextPane   _sqlLog;
    public static final Icon    LOGO = Images.getIcon("images/OpenBooks.jpg");
    
    private boolean _debug = Boolean.getBoolean("openbook.debug");
    
    /**
     * The handle to the service.
     */
    private OpenBookService     _service;
    private Customer            _customer;
    private Map<String, Object> _config;
    
    /**
     * Runs the demo.
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        SwingHelper.setLookAndFeel(14);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Demo demo = new Demo();
                demo.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

                demo.pack();
                SwingHelper.position(demo, null);
                demo.setVisible(true);
            }
        });
    }

    
    private Demo() {
        Thread.currentThread().setUncaughtExceptionHandler(this);
        _config = PropertyHelper.load(System.getProperty("openbook.client.config", "demo.properties"));
        
        setTitle("OpenBooks: A Sample JPA 2.0 Application");
        
        _root         = new WelcomeAction("OpenBooks", "images/OpenBooks.jpg", "OpenBooks");
        _about        = new AboutAction("About OpenBooks", "images/OpenBooks.jpg", "About OpenBooks");
        _buyBook      = new BuyBookAction("Buy", "images/Add2Cart.jpg", "Browse and Buy Books");
        _deliver      = new DeliveryAction("Deliver", "images/Deliver.jpg", "Deliver Pending Orders");
        _supply       = new SupplyAction("Supply", "images/Supply.jpg", "Supply Books");
        _viewConfig   = new ViewConfigAction("Configuration", "images/browse.png", "View Configuration");
        _viewDomain   = new ViewDomainAction("Domain", "images/DomainModel.jpg", "View Domain Model");
        _viewData     = new ViewDataAction("Data", "images/DataModel.jpg", "View Instances");
        _viewSource   = new ViewSourceAction("Source", "images/SourceCode.jpg", "View Source Code");
        _viewQuery    = new ViewQueryCacheAction("Query", "images/DataModel.jpg", "View Queries");
        
        _toolBar    = createToolBar();
        _navigator  = createNavigator();
        _tabbedPane = createTabbedView();
        _outputPane = createOutputView();
        _statusBar  = createStatusBar();
        
        JSplitPane horizontalSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        horizontalSplitPane.setContinuousLayout(true);
        horizontalSplitPane.setDividerSize(1);
        JScrollPane scrollPane = new JScrollPane(_navigator);
        scrollPane.setMinimumSize(new Dimension(NAV_VIEW.width/4, NAV_VIEW.height));
        scrollPane.setPreferredSize(NAV_VIEW);
        horizontalSplitPane.add(scrollPane);
        
        JSplitPane verticalSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        verticalSplitPane.setContinuousLayout(true);
        verticalSplitPane.setDividerSize(1);
        verticalSplitPane.add(_tabbedPane);
        verticalSplitPane.add(_outputPane);
        horizontalSplitPane.add(verticalSplitPane);
        
        Container content = getContentPane();
        content.add(_toolBar, BorderLayout.PAGE_START);
        content.add(horizontalSplitPane, BorderLayout.CENTER);
        content.add(_statusBar, BorderLayout.SOUTH);
        
        _root.actionPerformed(null);
    }
    
    /**
     * Gets the handle to OpenBooks service. 
     */
    public OpenBookService getService() {
        if (_service == null) {
            final String unitName = PropertyHelper.getString(_config, "openbook.unit", 
                    OpenBookService.DEFAULT_UNIT_NAME);
            
            SwingWorker<OpenBookService, Void> getService = new SwingWorker<OpenBookService, Void> () {
                @Override
                protected OpenBookService doInBackground() throws Exception {
                    return ServiceFactory.getService(unitName);
                }
                
            };
            getService.execute();
            try {
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                _service = getService.get(10, TimeUnit.SECONDS);
            } catch (Exception t) {
                new ErrorDialog(t).setVisible(true);
            } finally {
                setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }
        }
        return _service;
    }
    
    public Customer getCustomer() {
        if (_customer == null) {
            SwingWorker<Customer, Void> task = new SwingWorker<Customer, Void> () {
                @Override
                protected Customer doInBackground() throws Exception {
                    return getService().login("guest");
                }
                
            };
            task.execute();
            try {
                _customer = task.get(1, TimeUnit.SECONDS);
            } catch (Exception t) {
                new ErrorDialog(t).setVisible(true);
            }
        }
        return _customer;
    }
    
    @Override
    public void uncaughtException(Thread t, Throwable e) {
        if (SwingUtilities.isEventDispatchThread()) {
            new ErrorDialog(e);
        } else {
            e.printStackTrace();
        }
    }
    
    
    private JToolBar  createToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.add(_buyBook);
        toolBar.add(_deliver);
        toolBar.add(_supply);
        Dimension d = new Dimension(40, 32);
        toolBar.addSeparator(d);
        
        toolBar.add(_viewConfig);
        toolBar.add(_viewDomain);
        toolBar.add(_viewData);
        toolBar.add(_viewSource);
        toolBar.add(_viewQuery);
        
        toolBar.addSeparator(d);
        
        toolBar.add(Box.createHorizontalGlue());
        toolBar.add(_about);
        toolBar.add(Box.createHorizontalStrut(2));
        return toolBar;
    }
    
    private StatusBar createStatusBar() {
        return new StatusBar();
    }


    /**
     * Abstract root of all Action objects helps to locate/configure visual action parameters such as
     * tooltip text or image.
     * 
     * @author Pinaki Poddar
     *
     */
    public abstract class OpenBookAction extends AbstractAction {
        public OpenBookAction(Map<String,Object> props, String key) {
            this(PropertyHelper.getString(props, key + "name",    ""),
                 PropertyHelper.getString(props, key + "icon",    null),
                 PropertyHelper.getString(props, key + "tooltip", ""),
                 PropertyHelper.getString(props, key + "help",    ""));
        }
        
        public OpenBookAction(String name, String iconLocation, String tooltip) {
            this(name, iconLocation, tooltip, tooltip);
        }
        
        public OpenBookAction(String name, String iconLocation, String tooltip, String helpText) {
            putValue(Action.NAME, name);
            putValue(Action.SHORT_DESCRIPTION, tooltip);
            putValue(Action.LONG_DESCRIPTION,  helpText);
            
            Icon icon = Images.getIcon(iconLocation, true);
            putValue(Action.SMALL_ICON, icon);
        }
    }
    
    public class BuyBookAction extends OpenBookAction {
        BuyBookPage         _buyBookPage;
        public BuyBookAction(String name, String iconLocation, String tooltip) {
            super(name, iconLocation, tooltip);
        }
        
        public void actionPerformed(ActionEvent e) {
            if (_buyBookPage == null) {
                _buyBookPage = new BuyBookPage(getService(), getCustomer());
            }
            showTab(_tabbedPane, "Buy Books", _buyBookPage);
            switchTab(_outputPane, _sqlLog);
        }
        
    }
    public class DeliveryAction extends OpenBookAction {
        DeliveryPage        _deliveryPage;
        public DeliveryAction(String name, String iconLocation, String tooltip) {
            super(name, iconLocation, tooltip);
        }
        public void actionPerformed(ActionEvent e) {
            if (_deliveryPage == null) {
                _deliveryPage = new DeliveryPage(getService());
            }
            showTab(_tabbedPane, "Deliver Books", _deliveryPage);
            switchTab(_outputPane, _sqlLog);
        }
        
    }
    
    public class SupplyAction extends OpenBookAction {
        SupplyPage          _supplyPage;
        public SupplyAction(String name, String iconLocation, String tooltip) {
            super(name, iconLocation, tooltip);
        }
        public void actionPerformed(ActionEvent e) {
            if (_supplyPage == null) {
                _supplyPage = new SupplyPage(getService());
            }
            showTab(_tabbedPane, "Supply Books", _supplyPage);
            switchTab(_outputPane, _sqlLog);
        }
        
    }
    
    public class ViewConfigAction extends OpenBookAction {
        ConfigurationViewer _configView;
        public ViewConfigAction(String name, String iconLocation, String tooltip) {
            super(name, iconLocation, tooltip);
        }
        public void actionPerformed(ActionEvent e) {
            if (_configView == null) {
                _configView = new ConfigurationViewer("Unit Configuration", getService().getUnit().getProperties());
                showTab(_tabbedPane, "Configuration", new JScrollPane(_configView));
            } else {
                showTab(_tabbedPane, "Configuration", _configView);
            }
        }
        
    }
    
    public class ViewDomainAction extends OpenBookAction {
        MetamodelView       _domainView;
        AttributeLegendView _legends;
        public ViewDomainAction(String name, String iconLocation, String tooltip) {
            super(name, iconLocation, tooltip);
        }
        public void actionPerformed(ActionEvent e) {
            if (_domainView == null) {
                _domainView = new MetamodelView(getService().getUnit().getMetamodel());
                _legends = new AttributeLegendView();
                showTab(_outputPane, "Legends", new JScrollPane(_legends));
            }
            showTab(_tabbedPane, "Domain Model", _domainView);
        }
        
    }

    public class ViewDataAction extends OpenBookAction {
        public ViewDataAction(String name, String iconLocation, String tooltip) {
            super(name, iconLocation, tooltip);
        }
        public void actionPerformed(ActionEvent e) {
            showTab(_tabbedPane, "Buy Books", null);
        }
        
    }
    
    public class ViewQueryCacheAction extends OpenBookAction {
        PreparedQueryViewer _queryView;
        public ViewQueryCacheAction(String name, String iconLocation, String tooltip) {
            super(name, iconLocation, tooltip);
        }
        public void actionPerformed(ActionEvent e) {
            if (_queryView == null) {
                _queryView = new PreparedQueryViewer(OpenJPAPersistence.cast(getService().getUnit()));
                showTab(_tabbedPane, "JPQL Query", new JScrollPane(_queryView));
            }
            showTab(_tabbedPane, "JPQL Queries", _queryView);
        }
        
    }
    
    public class ViewSourceAction extends OpenBookAction {
        SourceCodeViewer _sourceViewer;
        
        public ViewSourceAction(String name, String iconLocation, String tooltip) {
            super(name, iconLocation, tooltip);
        }
        
        public void actionPerformed(ActionEvent e) {
            if (_sourceViewer == null) {
                _sourceViewer = new SourceCodeViewer("source");
            }
            showTab(_tabbedPane, "Source Code", _sourceViewer);
        }
        
    }
    
    /**
     * Displays the "welcome" page.
     *  
     * @author Pinaki Poddar
     *
     */
    public class WelcomeAction extends OpenBookAction {
        PowerPointViewer    _powerpoint;
        JLabel              _logoLabel = new JLabel(LOGO);
        boolean _showPresentation = true;
        
        public WelcomeAction(String name, String iconLocation, String tooltip) {
            super(name, iconLocation, tooltip);
        }
        
        public void actionPerformed(ActionEvent e) {
            if (_powerpoint == null && _showPresentation) {
                String dir = PropertyHelper.getString(_config, "openbook.slides.dir", "slides/");
                String[] defaultSlides = { 
                                    "Slide1.JPG",
                                    "Slide2.JPG",
                                    "Slide3.JPG",
                                    "Slide4.JPG",
                                    "Slide5.JPG",
                                    "Slide6.JPG",
                                    "Slide7.JPG",
                                    "Slide8.JPG",
                                    "Slide9.JPG",
                                    "Slide10.JPG",
                                    "Slide11.JPG",
                                    "Slide12.JPG",
                                    "Slide13.JPG",
                                    "Slide14.JPG",
                                    "Slide15.JPG"};
                List<String> slides = PropertyHelper.getStringList(_config, "openbook.slides.list",
                        Arrays.asList(defaultSlides));
                try {
                    _powerpoint = new PowerPointViewer(dir, slides);
                } catch (Exception e1) {
                    _showPresentation = false;
                    System.err.println("Error while opening slide deck at " + dir + ". \r\n"+ e1);
                }
            } 
            showTab(_tabbedPane, "Home", _powerpoint != null ? _powerpoint : _logoLabel);
        }
        
    }
    public class AboutAction extends OpenBookAction {
        AboutDialog _dialog;
        
        public AboutAction(String name, String iconLocation, String tooltip) {
            super(name, iconLocation, tooltip);
        }
        
        public void actionPerformed(ActionEvent e) {
            if (_dialog == null) {
                _dialog = new AboutDialog(LOGO);
                SwingHelper.position(_dialog, Demo.this);
            }
            _dialog.setVisible(true);
        }
        
    }
    
    /**
     * Show the given tab in the given pane.
     * @param pane the tabbed pane
     * @param title title of the tab component
     * @param tab the component to show
     */
    void showTab(JTabbedPane pane, String title, Component tab) {
        if (tab == null)
            return;
        Component c = locateTab(pane, tab);
        if (c == null) {
            pane.addTab(title, tab);
            pane.setSelectedComponent(tab);
        } else {
            pane.setSelectedComponent(c);
        }
    }
    
    void switchTab(JTabbedPane pane, Component tab) {
        if (tab == null)
            return;
        Component c = locateTab(pane, tab);
        if (c == null) {
            pane.setSelectedComponent(c);
        } 
    }
    
    Component locateTab(JTabbedPane pane, Component tab) {
        int index = pane.indexOfComponent(tab);
        if (index == -1) {
            Component[] components = pane.getComponents();
            for (int i = 0; i < components.length; i++) {
                if (components[i] instanceof JScrollPane 
                && (((JScrollPane)components[i]).getViewport().getView() == tab)) {
                    return components[i];
                }
            }
        } else {
            return pane.getComponentAt(index);
        }
        return null;
    }
    
    
    private JTabbedPane createTabbedView() {
        JTabbedPane pane = new JTabbedPane();
        pane.setPreferredSize(TAB_VIEW);
        pane.setMinimumSize(new Dimension(TAB_VIEW.width, TAB_VIEW.height));
        return pane;
    }
    
    private JTabbedPane createOutputView() {
        JTabbedPane pane = new JTabbedPane();
        pane.setPreferredSize(OUT_VIEW);
        _sqlLog = new ScrollingTextPane();
        GraphicOutputStream stream = new GraphicOutputStream(_sqlLog);
        _sqlLog.setPreferredSize(TAB_VIEW);
        SQLLogger.setOutput(stream);
        pane.addTab("SQL Log", new JScrollPane(_sqlLog));
        ScrollingTextPane consoleLog = new ScrollingTextPane();
        GraphicOutputStream console = new GraphicOutputStream(consoleLog);
        System.setErr(new PrintStream(console, true));
        pane.addTab("Console", new JScrollPane(consoleLog));
        return pane;
    }
    
    /**
     * Creates the navigation tree and adds the tree nodes. Each tree node is attached with an action
     * that fires when the node is selected.  
     */
    private JTree createNavigator() {
        ActionTreeNode root = new ActionTreeNode(_root);
        DefaultMutableTreeNode app   = new DefaultMutableTreeNode("Application WorkFlows");
        DefaultMutableTreeNode views = new DefaultMutableTreeNode("Views");
        root.add(app);
        root.add(views);
        
        
        app.add(new ActionTreeNode(_buyBook));
        app.add(new ActionTreeNode(_deliver));
        app.add(new ActionTreeNode(_supply));
        
        views.add(new ActionTreeNode(_viewConfig));
        views.add(new ActionTreeNode(_viewDomain));
        views.add(new ActionTreeNode(_viewQuery));
        views.add(new ActionTreeNode(_viewData));
        views.add(new ActionTreeNode(_viewSource));
        
        
        JTree tree = new JTree(root);
        tree.setShowsRootHandles(true);
        
        tree.addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
                Object treeNode = _navigator.getLastSelectedPathComponent();
                if (treeNode instanceof ActionTreeNode) {
                    ((ActionTreeNode)treeNode)._action.actionPerformed(null);
                }
            }
        });
        tree.setCellRenderer(new TypedTreeCellRenderer());
        
        return tree;
    }
    
    /**
     * A tree node which may have an associated action.
     * 
     * @author Pinaki Poddar
     *
     */
    public static class ActionTreeNode extends DefaultMutableTreeNode {
        private final Action _action;
        public ActionTreeNode(Action action) {
            _action = action;
        }
        
        public String toString() {
            return _action.getValue(Action.SHORT_DESCRIPTION).toString();
        }
        
    }
    
    public class TypedTreeCellRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, 
                boolean leaf, int row, boolean hasFocus) { 
            return super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        }
    }
    
    public static class AboutDialog extends JDialog {
        public AboutDialog(Icon logo) {
            setModal(true);
            setLayout(new BorderLayout());
            JButton button = new JButton("<html>" 
                    + "<b>OpenBooks</b> " 
                    + "<br> using OpenJPA version " + OpenJPAVersion.MAJOR_RELEASE + "." + OpenJPAVersion.MINOR_RELEASE
                    + "<br> by JPA Team, SWG" 
                    + "<br>IBM Corporation" 
                    + "<p>"
                    + "</html>");
            button.setIcon(logo);
            button.setHorizontalTextPosition(SwingConstants.RIGHT);
            button.setEnabled(true);
            button.setBorderPainted(false);
            add(button, BorderLayout.CENTER);
            add(new JLabel(Images.getIcon("images/websphere.png")), BorderLayout.SOUTH);
            setTitle("About OpenBooks");
            setAlwaysOnTop(true);
            setResizable(false);
            pack();
        }
    }

}
