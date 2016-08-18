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
package org.apache.openjpa.meta;

import java.util.ArrayList;
import java.util.List;

import org.apache.openjpa.kernel.QueryLanguages;



/**
 * Extends {@link QueryMetaData} to allow multiple {@link QueryMetaData#getResultType() result class} or 
 * {@link QueryMetaData#getResultSetMappingName() mapping names}. 
 * <br>
 * Designed for mapping the results from a Stored Procudure that can produce more than one {@link java.sql.ResultSet},
 * each being mapped with a different mapping specification.
 *
 *
 * @author Pinaki Poddar
 *
 */
@SuppressWarnings("serial")
public class MultiQueryMetaData extends QueryMetaData {
    private final String _procedureName;
    private final boolean _isTemporary;
    private final List<QueryMetaData> _parts = new ArrayList<QueryMetaData>();
    private final List<Parameter> _params = new ArrayList<MultiQueryMetaData.Parameter>();

    /**
     * Create this meta data given a scope of definition, a logical identifier, a procedure name
     * and whether its usage is temporary.
     * @param scope defining scope
     * @param logicalName name as an identifier
     * @param procedureName name of the database procedure
     */
    public MultiQueryMetaData(Class<?> scope, String logicalName, String procedureName, boolean isTemporary) {
        super(logicalName, false);
        setLanguage(QueryLanguages.LANG_STORED_PROC);
        setSource(null, scope, -1, null);
        _procedureName = procedureName;
        _isTemporary = isTemporary;
    }

    public String getProcedureName() {
        return _procedureName;
    }

    public List<QueryMetaData> getComponents() {
        return _parts;
    }

    /**
     * Affirms if this metadata is ephimeral.
     * Ephimenral metadata is removed from the repository after usage.
     * @return
     */
    public boolean isEphimeral() {
        return _isTemporary;
    }

    @Override
    public void setResultSetMappingName(String name) {
        throw new UnsupportedOperationException("Not allowed to set mapping name. It is automatically set");
    }

    @Override
    public void setResultType(Class cls) {
        throw new UnsupportedOperationException("Not allowed to set result type. It is automatically set");
    }

    public void addComponent(Class<?> resultClass) {
        QueryMetaData part = newQueryMetaData();
        part.setResultType(resultClass);
        _parts.add(part);
    }

    public void addComponent(String mappingName) {
        QueryMetaData part = newQueryMetaData();
        part.setResultSetMappingName(mappingName);
        _parts.add(part);
    }

    private QueryMetaData newQueryMetaData() {
        QueryMetaData part = new QueryMetaData(getName() + "#" + _parts.size(), false);
        part.setLanguage(getLanguage());
        part.setSource(null, getDefiningType(), -1, null);
        return part;
    }


    /**
     * Gets the component metadata at the given part index.
     * @param i a valid integer index
     * @return
     */
    public QueryMetaData getComponent(int i) {
        if (i < 0 || i >= _parts.size()) {
            throw new ArrayIndexOutOfBoundsException("Invalid index " + i
                    + ". Available " + _parts.size() + " parts");
        }
        return _parts.get(i);
    }

    /**
     * Gets the number of component metadata contained in this metada.
     */
    public int getComponentCount() {
        return _parts.size();
    }

    /**
     * Registers the given parameter.
     * @param p
     */
    public void registerParameter(Parameter p) {
        _params.add(p);
    }

    public List<Parameter> getParameters() {
        return _params;
    }

    public int getParameterCount() {
        return _params.size();
    }


    /**
     * A parameter
     *
     */
    public static class Parameter {
        public enum Mode {IN,OUT,INOUT,CURSOR};
        private final String name;
        private final Class<?> type;
        private final Mode mode;
        private final int position;

        public Parameter(String name, Class<?> type, Mode mode) {
            this.name = name;
            this.type = type;
            this.mode = mode;
            this.position = -1;
        }

        public Parameter(int position, Class<?> type, Mode mode) {
            this.name = null;
            this.type = type;
            this.mode = mode;
            this.position = position;
        }

        public int getPosition() {
            return position;
        }

        public String getName() {
            return name;
        }

        public Class<?> getType() {
            return type;
        }

        public Mode getMode() {
            return mode;
        }
    }



}
