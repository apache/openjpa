package jpa.tools.swing;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileScanner {
    private String ext;
    boolean recursive;
    
    public FileScanner(String ext, boolean recurse) {
        this.ext = ext;
        this.recursive = recurse;
    }
    
    /**
     * Scans the given  
     * @param root
     * @return
     */
    public List<File> scan(File dir) {
        List<File> bag = new ArrayList<File>();
        scan(dir, bag);
        return bag;
    }
    
    private void scan(File dir, List<File> bag) {
        if (dir == null || !dir.exists() || !dir.isDirectory())
            return;
        File[] all = dir.listFiles();
        for (File f : all) {
            if (ext == null || f.getName().endsWith(ext)) {
                bag.add(f);
            }
            if (recursive && f.isDirectory()) {
                scan(f, bag);
            }
        }
    }
    
}
