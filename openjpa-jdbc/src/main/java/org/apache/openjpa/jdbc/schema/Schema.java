/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */
package org.apache.openjpa.jdbc.schema;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;

/**
 * Represents a database schema.
 *
 * @author Abe White
 */
public class Schema
    implements Comparable, Serializable {

    private String _name = null;
    private SchemaGroup _group = null;
    private Map _tableMap = null;
    private Map _seqMap = null;

    // cache
    private Table[] _tables = null;
    private Sequence[] _seqs = null;

    /**
     * Default constructor.
     */
    public Schema() {
    }

    /**
     * Constructor.
     *
     * @param name the schema name, if any
     * @param group the schema's owning group
     */
    public Schema(String name, SchemaGroup group) {
        setName(name);
        _group = group;
    }

    /**
     * Called when the schema is removed from its group. Invalidates the
     * schema and removes all its member tables.
     */
    void remove() {
        Table[] tabs = getTables();
        for (int i = 0; i < tabs.length; i++)
            removeTable(tabs[i]);
        Sequence[] seqs = getSequences();
        for (int i = 0; i < seqs.length; i++)
            removeSequence(seqs[i]);
        _group = null;
    }

    /**
     * Return the schema's group.
     */
    public SchemaGroup getSchemaGroup() {
        return _group;
    }

    /**
     * Return the name of the schema, or null if none.
     */
    public String getName() {
        return _name;
    }

    /**
     * Set the name of the schema. This method can only be used for schemas
     * not attached to a group.
     */
    public void setName(String name) {
        if (getSchemaGroup() != null)
            throw new IllegalStateException();
        _name = StringUtils.trimToNull(name);
    }

    /**
     * Return the schema's tables.
     */
    public Table[] getTables() {
        if (_tables == null)
            _tables = (_tableMap == null) ? new Table[0] : (Table[])
                _tableMap.values().toArray(new Table[_tableMap.size()]);
        return _tables;
    }

    /**
     * Return the table with the given name, or null if none.
     */
    public Table getTable(String name) {
        if (name == null || _tableMap == null)
            return null;
        return (Table) _tableMap.get(name.toUpperCase());
    }

    /**
     * Add a table to the schema.
     */
    public Table addTable(String name) {
        SchemaGroup group = getSchemaGroup();
        Table tab;
        if (group != null) {
            group.addName(name, true);
            tab = group.newTable(name, this);
        } else
            tab = new Table(name, this);
        if (_tableMap == null)
            _tableMap = Collections.synchronizedMap(new TreeMap());
        _tableMap.put(name.toUpperCase(), tab);
        _tables = null;
        return tab;
    }

    /**
     * Remove the given table from the schema.
     *
     * @return true if the table was removed, false if not in the schema
     */
    public boolean removeTable(Table tab) {
        if (tab == null || _tableMap == null)
            return false;

        Table cur = (Table) _tableMap.get(tab.getName().toUpperCase());
        if (!cur.equals(tab))
            return false;

        _tableMap.remove(tab.getName().toUpperCase());
        _tables = null;
        SchemaGroup group = getSchemaGroup();
        if (group != null)
            group.removeName(tab.getName());
        tab.remove();
        return true;
    }

    /**
     * Import a table from another schema.	 Note that this method does
     * <strong>not</strong> import foreign keys, indexes, or unique constraints.
     */
    public Table importTable(Table table) {
        if (table == null)
            return null;

        Table copy = addTable(table.getName());
        Column[] cols = table.getColumns();
        for (int i = 0; i < cols.length; i++)
            copy.importColumn(cols[i]);

        copy.importPrimaryKey(table.getPrimaryKey());
        return copy;
    }

    /**
     * Return the schema's sequences.
     */
    public Sequence[] getSequences() {
        if (_seqs == null)
            _seqs = (_seqMap == null) ? new Sequence[0] : (Sequence[])
                _seqMap.values().toArray(new Sequence[_seqMap.size()]);
        return _seqs;
    }

    /**
     * Return the sequence with the given name, or null if none.
     */
    public Sequence getSequence(String name) {
        if (name == null || _seqMap == null)
            return null;
        return (Sequence) _seqMap.get(name.toUpperCase());
    }

    /**
     * Add a sequence to the schema.
     */
    public Sequence addSequence(String name) {
        SchemaGroup group = getSchemaGroup();
        Sequence seq;
        if (group != null) {
            group.addName(name, true);
            seq = group.newSequence(name, this);
        } else
            seq = new Sequence(name, this);
        if (_seqMap == null)
            _seqMap = Collections.synchronizedMap(new TreeMap());
        _seqMap.put(name.toUpperCase(), seq);
        _seqs = null;
        return seq;
    }

    /**
     * Remove the given sequence from the schema.
     *
     * @return true if the sequence was removed, false if not in the schema
     */
    public boolean removeSequence(Sequence seq) {
        if (seq == null || _seqMap == null)
            return false;

        Sequence cur = (Sequence) _seqMap.get(seq.getName().toUpperCase());
        if (!cur.equals(seq))
            return false;

        _seqMap.remove(seq.getName().toUpperCase());
        _seqs = null;
        SchemaGroup group = getSchemaGroup();
        if (group != null)
            group.removeName(seq.getName());
        seq.remove();
        return true;
    }

    /**
     * Import a sequence from another schema.
     */
    public Sequence importSequence(Sequence seq) {
        if (seq == null)
            return null;

        Sequence copy = addSequence(seq.getName());
        copy.setInitialValue(seq.getInitialValue());
        copy.setIncrement(seq.getIncrement());
        copy.setAllocate(seq.getAllocate());
        return copy;
    }

    public int compareTo(Object other) {
        String name = getName();
        String otherName = ((Schema) other).getName();
        if (name == null && otherName == null)
            return 0;
        if (name == null)
            return 1;
        if (otherName == null)
            return -1;
        return name.compareTo(otherName);
    }

    public String toString() {
        return getName();
    }
}
