/*
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.jdbc.schema;

import java.io.File;

import org.apache.openjpa.lib.meta.SourceTracker;

/**
 * Represents a database sequence.
 *
 * @author Abe White
 */
public class Sequence
    extends ReferenceCounter
    implements Comparable, SourceTracker {

    private String _name = null;
    private String _fullName = null;
    private Schema _schema = null;
    private String _schemaName = null;
    private int _initial = 1;
    private int _increment = 1;
    private int _cache = 0;

    // keep track of source
    private File _source = null;
    private int _srcType = SRC_OTHER;

    /**
     * Default constructor.
     */
    public Sequence() {
    }

    /**
     * Constructor.
     *
     * @param name the sequence name
     * @param schema the sequence schema
     */
    public Sequence(String name, Schema schema) {
        setName(name);
        if (schema != null)
            setSchemaName(schema.getName());
        _schema = schema;
    }

    /**
     * Called when the sequence is removed from its schema.
     */
    void remove() {
        _schema = null;
        _fullName = null;
    }

    /**
     * Return the schema for the sequence.
     */
    public Schema getSchema() {
        return _schema;
    }

    /**
     * The sequence's schema name.
     */
    public String getSchemaName() {
        return _schemaName;
    }

    /**
     * The sequence's schema name. You can only call this method on sequences
     * whose schema object is not set.
     */
    public void setSchemaName(String name) {
        if (getSchema() != null)
            throw new IllegalStateException();
        _schemaName = name;
        _fullName = null;
    }

    /**
     * Return the name of the sequence.
     */
    public String getName() {
        return _name;
    }

    /**
     * Set the name of the sequence. This method can only be called on
     * sequences that are not part of a schema.
     */
    public void setName(String name) {
        if (getSchema() != null)
            throw new IllegalStateException();
        _name = name;
        _fullName = null;
    }

    /**
     * Return the sequence name, including schema, using '.' as the
     * catalog separator.
     */
    public String getFullName() {
        if (_fullName == null) {
            Schema schema = getSchema();
            if (schema == null || schema.getName() == null)
                _fullName = getName();
            else
                _fullName = schema.getName() + "." + getName();
        }
        return _fullName;
    }

    /**
     * The sequence's initial value.
     */
    public int getInitialValue() {
        return _initial;
    }

    /**
     * The sequence's initial value.
     */
    public void setInitialValue(int initial) {
        _initial = initial;
    }

    /**
     * The sequence's increment.
     */
    public int getIncrement() {
        return _increment;
    }

    /**
     * The sequence's increment.
     */
    public void setIncrement(int increment) {
        _increment = increment;
    }

    /**
     * The sequence's cache size.
     */
    public int getAllocate() {
        return _cache;
    }

    /**
     * The sequence's cache size.
     */
    public void setAllocate(int cache) {
        _cache = cache;
    }

    public File getSourceFile() {
        return _source;
    }

    public Object getSourceScope() {
        return null;
    }

    public int getSourceType() {
        return _srcType;
    }

    public void setSource(File source, int srcType) {
        _source = source;
        _srcType = srcType;
    }

    public String getResourceName() {
        return getFullName();
    }

    public int compareTo(Object other) {
        String name = getFullName();
        String otherName = ((Sequence) other).getFullName();
        if (name == null && otherName == null)
            return 0;
        if (name == null)
            return 1;
        if (otherName == null)
            return -1;
        return name.compareTo(otherName);
    }

    public String toString() {
        return getFullName();
    }
}
