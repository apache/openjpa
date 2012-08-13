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
     * Implies that this constraint will auto-generate its name from the names 
     * of its columns, unless later the name is set explicitly.
     */
    public Unique() {
    	_autoNaming = true;
    }

    /**
     * Construct with given name.
     * Implies that this constraint will not auto-generate its name.
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
     * Gets the name of the constraint. If no name has been set by the user
     * then this method has the side-effect of auto-generating a name from
     * the name of its columns.
     */
    public String getName() {
    	if (getTable() == null && _autoNaming) {
    		setName(createAutoName());
    		_autoNaming = true;
    	}
    	return super.getName();
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
	
	private String createAutoName() {
		Column[] columns = getColumns();
		int l = 32/Math.max(columns.length,1);
		StringBuffer autoName = new StringBuffer("UNQ_");
		for (Column column : columns)
			autoName.append(chop(column.getName(),l));
		return autoName.toString();
	}
	
    private String chop(String name, int head) {
    	if (StringUtils.isEmpty(name))
    		return name;
    	return name.substring(0, Math.min(Math.max(1,head), name.length()));
    }
}
