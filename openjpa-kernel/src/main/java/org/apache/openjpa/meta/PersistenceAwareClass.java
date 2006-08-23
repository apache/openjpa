package org.apache.openjpa.meta;

import java.io.File;

import org.apache.openjpa.lib.meta.SourceTracker;
import org.apache.openjpa.lib.xml.Commentable;

public class PersistenceAwareClass 
	implements Comparable, SourceTracker,Commentable,MetaDataContext,
	MetaDataModes {

    private final MetaDataRepository _repos;
	private final Class _class;
	
    private File _srcFile = null;
    private int _srcType = SRC_OTHER;
    private String[] _comments = null;
    private int _listIndex = -1;
    private int _srcMode = MODE_META | MODE_MAPPING;
	
	public PersistenceAwareClass(Class cls, MetaDataRepository repos) {
		_repos = repos;
		_class = cls;
	}
	
	public String getName() {
		return _class.getName();
	}
	
	public MetaDataRepository getRepository() {
		return _repos;
	}
	
	public Class getDescribedType() {
		return _class;
	}
	
    public File getSourceFile() {
        return _srcFile;
    }

    public Object getSourceScope() {
        return null;
    }

    public int getSourceType() {
        return _srcType;
    }

    public void setSource(File file, int srcType) {
        _srcFile = file;
        _srcType = srcType;
    }

    public String getResourceName() {
        return _class.getName();
    }

    /**
     * The source mode this metadata has been loaded under.
     */
    public int getSourceMode() {
        return _srcMode;
    }

    /**
     * The source mode this metadata has been loaded under.
     */
    public void setSourceMode(int mode) {
        _srcMode = mode;
    }

    /**
     * The source mode this metadata has been loaded under.
     */
    public void setSourceMode(int mode, boolean on) {
        if (mode == MODE_NONE)
            _srcMode = mode;
        else if (on)
            _srcMode |= mode;
        else
            _srcMode &= ~mode;
    }

    /**
     * The index in which this class was listed in the metadata. Defaults to
     * <code>-1</code> if this class was not listed in the metadata.
     */
    public int getListingIndex() {
        return _listIndex;
    }

    /**
     * The index in which this field was listed in the metadata. Defaults to
     * <code>-1</code> if this class was not listed in the metadata.
     */
    public void setListingIndex(int index) {
        _listIndex = index;
    }

    ///////////////
    // Commentable
    ///////////////

    public String[] getComments() {
        return (_comments == null) ? ClassMetaData.EMPTY_COMMENTS : _comments;
    }

    public void setComments(String[] comments) {
        _comments = comments;
    }
    
    public int compareTo(Object other) {
        if (other == this)
            return 0;
        if (other instanceof PersistenceAwareClass)
        	return 1;
        return _class.getName().compareTo(((ClassMetaData) other).
            getDescribedType().getName());
    }

}
