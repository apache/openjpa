package org.apache.openjpa.util;

/**
 * {@link OpenJPAId} subclass appropriate for boolean fields.
 *
 * @author Dianne Richards
 * @since 2.1.0
 */
public class BooleanId extends OpenJPAId {
    
    private final boolean key;
    
    public BooleanId(Class cls, Boolean key) {
        this(cls, key.booleanValue());
    }

    public BooleanId(Class cls, String key) {
        this(cls, Boolean.parseBoolean(key));
    }

    public BooleanId(Class cls, boolean key) {
        super(cls);
        this.key = key;
    }
    
    public BooleanId(Class cls, boolean key, boolean subs) {
        super(cls, subs);
        this.key = key;
    }
    
    public boolean getId() {
        return key;
    }

    @Override
    public Object getIdObject() {
        return Boolean.valueOf(key);
    }
    
    public String toString() {
        return Boolean.toString(key);
    }

    @Override
    protected boolean idEquals(OpenJPAId other) {
        return key == ((BooleanId) other).key;
    }

    @Override
    protected int idHash() {
        return Boolean.valueOf(key).hashCode();
    }

}
