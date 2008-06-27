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

import org.apache.commons.lang.StringUtils;

/**
 * Represents a unique constraint. It can also represent a partial constraint.
 *
 * @author Abe White
 * @author Pinaki Poddar
 */
public class Unique
    extends LocalConstraint {
	private boolean _autoNaming = false;
    
	/**
     * Default constructor without a name.
     * Assumes that this constraint will set its own name automatically from
     * the names of the columns added to it.
     */
    public Unique() {
    	_autoNaming = true;
    }

    /**
     * Construct with given name.
     * Assumes that this constraint will not set its own name.
     * 
     * @param name the name of the constraint, if any
     * @param table the table of the constraint
     */
    public Unique(String name, Table table) {
        super(name, table);
    	_autoNaming = false;
    }

    public boolean isLogical() {
        return false;
    }
    
    /**
     * Adds the given column. 
     * The added column is set to non-nullable because a unique constraint
     * on the database requires that its constituent columns are NOT NULL. 
     * @see Column#setNotNull(boolean)
     * If this instance is constructing its own name, then this method also
     * has the side effect of changing its own name by appending the newly 
     * added column name to its own name. 
     */
    public void addColumn(Column col) {
    	super.addColumn(col);
    	col.setNotNull(true);
    	if (_autoNaming && getTable() == null) {
    		String prefix = createPrefix();
    		setName(prefix + "_" + chop(col.getName(), 4));
    		_autoNaming = true;
    	}
    }
    
    private String createPrefix() {
    	String currentName = getName();
    	if (StringUtils.isEmpty(currentName)) {
    		String tname = getTableName();
    		if (StringUtils.isEmpty(tname))
    			return "UNQ";
    		else
    			return "UNQ_" + chop(tname, 3);
    	}
    	return currentName;
    }
    
    private String chop(String name, int head) {
    	if (StringUtils.isEmpty(name))
    		return name;
    	return name.substring(0, Math.min(Math.max(1,head), name.length()));
    }
    
    /**
     * Set the name of the constraint. This method cannot be called if the
     * constraint already belongs to a table. Calling this method also has the
     * side-effect of implying that the instance will not auto-generate its
     * name.
     */
    public void setName(String name) {
        super.setName(name);
        _autoNaming = false;
    }


    /**
     * Return true if the structure of this primary key matches that of
     * the given one (same table, same columns).
     */
    public boolean equalsUnique(Unique unq) {
        return equalsLocalConstraint(unq);
    }

    /*
     * Affirms if this instance is currently generating its own name. No 
     * mutator because auto-naming is switched off as side-effect of user 
     * calling setName() directly. 
     */
	public boolean isAutoNaming() {
		return _autoNaming;
	}
}
