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
package org.apache.openjpa.jdbc.meta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.meta.strats.UntypedPCValueHandler;
import org.apache.openjpa.jdbc.meta.strats.EnumValueHandler;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.schema.ForeignKey;
import org.apache.openjpa.jdbc.schema.Index;
import org.apache.openjpa.jdbc.schema.Schema;
import org.apache.openjpa.jdbc.schema.Table;
import org.apache.openjpa.jdbc.schema.Unique;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.lib.conf.Configurable;
import org.apache.openjpa.lib.conf.Configuration;
import org.apache.openjpa.lib.conf.Configurations;
import org.apache.openjpa.lib.util.JavaVersions;
import org.apache.openjpa.meta.JavaTypes;
import serp.util.Strings;

/**
 * Default implementation of {@link MappingDefaults}.
 *
 * @author Abe White
 */
public class MappingDefaultsImpl
    implements MappingDefaults, Configurable {

    private transient DBDictionary dict = null;
    private String _baseClassStrategy = null;
    private String _subclassStrategy = null;
    private String _versionStrategy = null;
    private String _discStrategy = null;
    private final Map _fieldMap = new HashMap();
    private boolean _defMissing = false;
    private boolean _classCriteria = false;
    private int _joinFKAction = ForeignKey.ACTION_NONE;
    private int _fkAction = ForeignKey.ACTION_NONE;
    private boolean _defer = false;
    private boolean _indexFK = true;
    private boolean _indexDisc = true;
    private boolean _indexVers = false;
    private boolean _orderLists = true;
    private boolean _addNullInd = false;
    private boolean _ordinalEnum = false;
    private boolean _stringifyUnmapped = false;
    private String _dsIdName = null;
    private String _versName = null;
    private String _discName = null;
    private String _orderName = null;
    private String _nullIndName = null;
    private boolean _removeHungarianNotation = false;
    private Configuration conf = null;

    /**
     * Convenient access to dictionary for mappings.
     */
    public DBDictionary getDBDictionary() {
        if (dict == null) {
            dict = ((JDBCConfiguration) conf).getDBDictionaryInstance();
        }
        return dict;
    }
    
    public boolean isRemoveHungarianNotation() {
        return _removeHungarianNotation;
    }

    public void setRemoveHungarianNotation(boolean removeHungarianNotation) {
        this._removeHungarianNotation = removeHungarianNotation;
    }

    /**
     * Default base class strategy alias.
     */
    public String getBaseClassStrategy() {
        return _baseClassStrategy;
    }

    /**
     * Default base class strategy alias.
     */
    public void setBaseClassStrategy(String baseClassStrategy) {
        _baseClassStrategy = baseClassStrategy;
    }

    /**
     * Default subclass strategy alias.
     */
    public String getSubclassStrategy() {
        return _subclassStrategy;
    }

    /**
     * Default subclass strategy alias.
     */
    public void setSubclassStrategy(String subclassStrategy) {
        _subclassStrategy = subclassStrategy;
    }

    /**
     * Default version strategy alias.
     */
    public String getVersionStrategy() {
        return _versionStrategy;
    }

    /**
     * Default version strategy alias.
     */
    public void setVersionStrategy(String versionStrategy) {
        _versionStrategy = versionStrategy;
    }

    /**
     * Default discriminator strategy alias.
     */
    public String getDiscriminatorStrategy() {
        return _discStrategy;
    }

    /**
     * Default discriminator strategy alias.
     */
    public void setDiscriminatorStrategy(String discStrategy) {
        _discStrategy = discStrategy;
    }

    /**
     * Property string mapping field type names to value handler or field
     * mapping class names. For auto-configuration.
     */
    public void setFieldStrategies(String fieldMapString) {
        Properties props = Configurations.parseProperties(fieldMapString);
        if (props != null)
            _fieldMap.putAll(props);
    }

    /**
     * Association of a field value type name with the handler or strategy
     * class name.
     */
    public void setFieldStrategy(String valueType, String handlerType) {
        if (handlerType == null)
            _fieldMap.remove(valueType);
        else
            _fieldMap.put(valueType, handlerType);
    }

    /**
     * Association of a field value type name with the handler or strategy
     * class name.
     */
    public String getFieldStrategy(String valueType) {
        return (String) _fieldMap.get(valueType);
    }

    /**
     * Whether to store enums as the ordinal value rather than the enum name.
     * Defaults to false.
     */
    public boolean getStoreEnumOrdinal() {
        return _ordinalEnum;
    }

    /**
     * Whether to store enums as the ordinal value rather than the enum name.
     * Defaults to false.
     */
    public void setStoreEnumOrdinal(boolean ordinal) {
        _ordinalEnum = ordinal;
    }

    /**
     * Whether to store a relation to an unmapped class by stringifying the
     * oid of the related object, rather than storing primary key values.
     */
    public boolean getStoreUnmappedObjectIdString() {
        return _stringifyUnmapped;
    }

    /**
     * Whether to store a relation to an unmapped class by stringifying the
     * oid of the related object, rather than storing primary key values.
     */
    public void setStoreUnmappedObjectIdString(boolean stringify) {
        _stringifyUnmapped = stringify;
    }

    /**
     * Default foreign key action for join keys. Defaults to logical keys.
     */
    public int getJoinForeignKeyDeleteAction() {
        return _joinFKAction;
    }

    /**
     * Default foreign key action for join keys. Defaults to logical keys.
     */
    public void setJoinForeignKeyDeleteAction(int joinFKAction) {
        _joinFKAction = joinFKAction;
    }

    /**
     * Default foreign key action name for join keys. Used in auto
     * configuration.
     */
    public void setJoinForeignKeyDeleteAction(String joinFKAction) {
        _joinFKAction = ForeignKey.getAction(joinFKAction);
    }

    /**
     * Default foreign key action for relation keys. Defaults to logical keys.
     */
    public int getForeignKeyDeleteAction() {
        return _fkAction;
    }

    /**
     * Default foreign key action for relation keys. Defaults to logical keys.
     */
    public void setForeignKeyDeleteAction(int fkAction) {
        _fkAction = fkAction;
    }

    /**
     * Default foreign key action name for relation keys. Used in auto
     * configuration.
     */
    public void setForeignKeyDeleteAction(String fkAction) {
        _fkAction = ForeignKey.getAction(fkAction);
    }

    /**
     * Whether to index logical foreign keys by default. Defaults to true.
     */
    public boolean getIndexLogicalForeignKeys() {
        return _indexFK;
    }

    /**
     * Whether to index logical foreign keys by default. Defaults to true.
     */
    public void setIndexLogicalForeignKeys(boolean indexFK) {
        _indexFK = indexFK;
    }

    /**
     * Whether to index discriminator columns by default. Defaults to true.
     */
    public boolean getIndexDiscriminator() {
        return _indexDisc;
    }

    /**
     * Whether to index discriminator columns by default. Defaults to true.
     */
    public void setIndexDiscriminator(boolean indexDisc) {
        _indexDisc = indexDisc;
    }

    /**
     * Whether to index version columns by default. Defaults to true.
     */
    public boolean getIndexVersion() {
        return _indexVers;
    }

    /**
     * Whether to index version columns by default. Defaults to true.
     */
    public void setIndexVersion(boolean indexVers) {
        _indexVers = indexVers;
    }

    /**
     * Whether to order lists and arrays using a dedicated ordering column
     * by default.
     */
    public boolean getOrderLists() {
        return _orderLists;
    }

    /**
     * Whether to order lists and arrays using a dedicated ordering column
     * by default.
     */
    public void setOrderLists(boolean orderLists) {
        _orderLists = orderLists;
    }

    /**
     * Whether to add a synthetic null indicator column to embedded mappings
     * by default.
     */
    public boolean getAddNullIndicator() {
        return _addNullInd;
    }

    /**
     * Whether to add a synthetic null indicator column to embedded mappings
     * by default.
     */
    public void setAddNullIndicator(boolean addNullInd) {
        _addNullInd = addNullInd;
    }

    /**
     * Whether to defer constraints by default. Defaults to false.
     */
    public boolean getDeferConstraints() {
        return _defer;
    }

    /**
     * Whether to defer constraints by default. Defaults to false.
     */
    public void setDeferConstraints(boolean defer) {
        _defer = defer;
    }

    /**
     * Default base name for datastore identity columns, or null to the
     * mapping's built-in name.
     */
    public String getDataStoreIdColumnName() {
        return _dsIdName;
    }

    /**
     * Default base name for datastore identity columns, or null to the
     * mapping's built-in name.
     */
    public void setDataStoreIdColumnName(String dsIdName) {
        _dsIdName = dsIdName;
    }

    /**
     * Default base name for version identity columns, or null to the mapping's
     * built-in name.
     */
    public String getVersionColumnName() {
        return _versName;
    }

    /**
     * Default base name for version identity columns, or null to the mapping's
     * built-in name.
     */
    public void setVersionColumnName(String versName) {
        _versName = versName;
    }

    /**
     * Default base name for discriminator columns, or null to the mapping's
     * built-in name.
     */
    public String getDiscriminatorColumnName() {
        return _discName;
    }

    /**
     * Default base name for discriminator columns, or null to the mapping's
     * built-in name.
     */
    public void setDiscriminatorColumnName(String discName) {
        _discName = discName;
    }

    /**
     * Default base name for order columns, or null to the mapping's
     * built-in name.
     */
    public String getOrderColumnName() {
        return _orderName;
    }

    /**
     * Default base name for order columns, or null to the mapping's
     * built-in name.
     */
    public void setOrderColumnName(String orderName) {
        _orderName = orderName;
    }

    /**
     * Default base name for null indicator columns, or null to the mapping's
     * built-in name.
     */
    public String getNullIndicatorColumnName() {
        return _nullIndName;
    }

    /**
     * Default base name for null indicator columns, or null to the mapping's
     * built-in name.
     */
    public void setNullIndicatorColumnName(String nullIndName) {
        _nullIndName = nullIndName;
    }

    public boolean defaultMissingInfo() {
        return _defMissing;
    }

    public void setDefaultMissingInfo(boolean defMissing) {
        _defMissing = defMissing;
    }

    public boolean useClassCriteria() {
        return _classCriteria;
    }

    public void setUseClassCriteria(boolean classCriteria) {
        _classCriteria = classCriteria;
    }

    public Object getStrategy(ClassMapping cls, boolean adapt) {
        if (adapt || defaultMissingInfo())
            return (cls.getMappedPCSuperclassMapping() == null)
                ? _baseClassStrategy : _subclassStrategy;
        return null;
    }

    public Object getStrategy(Version vers, boolean adapt) {
        ClassMapping cls = vers.getClassMapping();
        if ((adapt || defaultMissingInfo())
            && cls.getJoinablePCSuperclassMapping() == null
            && cls.getVersionField() == null)
            return _versionStrategy;
        return null;
    }

    public Object getStrategy(Discriminator disc, boolean adapt) {
        ClassMapping cls = disc.getClassMapping();
        if ((adapt || defaultMissingInfo())
            && cls.getJoinablePCSuperclassMapping() == null
            && disc.getMappingInfo().getValue() == null)
            return _discStrategy;
        return null;
    }

    public Object getStrategy(ValueMapping vm, Class type, boolean adapt) {
        Object ret = _fieldMap.get(type.getName());
        if (ret != null)
            return ret;
        if (_stringifyUnmapped && vm.getTypeMapping() != null
            && !vm.getTypeMapping().isMapped())
            return UntypedPCValueHandler.getInstance();
        if (type.isEnum() && !vm.isSerialized()) {
            EnumValueHandler enumHandler = new EnumValueHandler();
            enumHandler.setStoreOrdinal(_ordinalEnum);
            return enumHandler;
        }
        return null;
    }

    /**
     * Provides a default value for the given Discriminator. 
     * 
     * <P>
     * The type of the object returned relies on the javaType field being set on 
     * the Discriminator which is provided.
     * <TABLE border="2"> 
     * <TH>JavaType
     * <TH>Default value
     * <TBODY>
     * <TR><TD>{@link JavaTypes.INT}<TD> The hashcode of the entity name</TR>
     * <TR><TD>{@link JavaTypes.CHAR}<TD>The first character of the entity name 
     * </TR>
     * <TR><TD>{@link JavaTypes.STRING}<TD>The entity name</TR>
     * </TBODY>
     * </TABLE>
     * 
     * @param disc The discriminator that needs a default value
     * @param adapt 
     * 
     * @return A new object containing the generated Discriminator value.
     */
    public Object getDiscriminatorValue(Discriminator disc, boolean adapt) {
        if (!adapt && !defaultMissingInfo())
            return null;

        // WARNING: CHANGING THIS WILL INVALIDATE EXISTING DATA IF DEFAULTING
        // MISSING MAPPING INFO
        
        String alias = Strings.getClassName(disc.getClassMapping()
                .getTypeAlias());
        
        switch (disc.getJavaType()) {
            case JavaTypes.INT:
                return new Integer(alias.hashCode());
            case JavaTypes.CHAR:
                return new Character(alias.charAt(0)); 
            case JavaTypes.STRING:
            default:
                return alias;
        }
    }

    public String getTableName(ClassMapping cls, Schema schema) {
        String name = Strings.getClassName(cls.getDescribedType()).
            replace('$', '_');
        if (!_defMissing && getDBDictionary() != null) {
            name = getDBDictionary().getValidTableName(name, schema);
        }
        return name;
    }

    public String getTableName(FieldMapping fm, Schema schema) {
        String name = fm.getName();
        Table table = fm.getDefiningMapping().getTable();
        if (table != null) {
            String tableName = table.getName();
            if (tableName.length() > 5)
                tableName = tableName.substring(0, 5);
            name = tableName + "_" + name;
        }
        if (!_defMissing && getDBDictionary() != null){
            name = getDBDictionary().getValidTableName(name, schema);
        }
        return name;
    }

    public void populateDataStoreIdColumns(ClassMapping cls, Table table,
        Column[] cols) {
        for (int i = 0; i < cols.length; i++) {
            if (_dsIdName != null && cols.length == 1)
                cols[i].setName(_dsIdName);
            else if (_dsIdName != null)
                cols[i].setName(_dsIdName + i);
            correctName(table, cols[i]);
        }
    }

    /**
     * Correct the given column's name.
     */
    protected void correctName(Table table, Column col) {
        if (!_defMissing || _removeHungarianNotation)
        {
            String name = col.getName();
            if (_removeHungarianNotation) {
                name = removeHungarianNotation(name);
            }
            
            if (getDBDictionary() != null) {
                col.setName(getDBDictionary().getValidColumnName(name, table));
            }
        }
    }

    protected String removeHungarianNotation(String columnName) {
        char[] name = columnName.toCharArray();
        int newStart = 0;

        for (int i = 0; i < name.length; i++) {
            if (Character.isUpperCase(name[i]))
            {
                newStart = i;
                break;
            }
        }

        return columnName.substring(newStart);
    }

    public void populateColumns(Version vers, Table table, Column[] cols) {
        for (int i = 0; i < cols.length; i++) {
            if (_versName != null && cols.length == 1)
                cols[i].setName(_versName);
            else if (_versName != null) {
                if (i == 0)
                    cols[i].setName(_versName);
                else
                    cols[i].setName(_versName + "_" + i);
            } else if (_versName != null)
                cols[i].setName(_versName + i);
            correctName(table, cols[i]);
        }
    }

    public void populateColumns(Discriminator disc, Table table,
        Column[] cols) {
        for (int i = 0; i < cols.length; i++) {
            if (_discName != null && cols.length == 1)
                cols[i].setName(_discName);
            else if (_discName != null)
                cols[i].setName(_discName + i);
            correctName(table, cols[i]);
        }
    }

    public void populateJoinColumn(ClassMapping cm, Table local, Table foreign,
        Column col, Object target, int pos, int cols) {
        correctName(local, col);
    }

    public void populateJoinColumn(FieldMapping fm, Table local, Table foreign,
        Column col, Object target, int pos, int cols) {
        correctName(local, col);
    }

    public void populateForeignKeyColumn(ValueMapping vm, String name,
        Table local, Table foreign, Column col, Object target, boolean inverse,
        int pos, int cols) {
        if (cols == 1)
            col.setName(name);
        else if (target instanceof Column)
            col.setName(name + "_" + ((Column) target).getName());
        correctName(local, col);
    }

    public void populateColumns(ValueMapping vm, String name, Table table,
        Column[] cols) {
        for (int i = 0; i < cols.length; i++)
            correctName(table, cols[i]);
    }

    public boolean populateOrderColumns(FieldMapping fm, Table table,
        Column[] cols) {
        for (int i = 0; i < cols.length; i++) {
            if (_orderName != null && cols.length == 1)
                cols[i].setName(_orderName);
            else if (_orderName != null)
                cols[i].setName(_orderName + i);
            correctName(table, cols[i]);
        }
        return _orderLists && (JavaTypes.ARRAY == fm.getTypeCode()
            || List.class.isAssignableFrom(fm.getType()));
    }

    public boolean populateNullIndicatorColumns(ValueMapping vm, String name,
        Table table, Column[] cols) {
        for (int i = 0; i < cols.length; i++) {
            if (_nullIndName != null && cols.length == 1)
                cols[i].setName(_nullIndName);
            else if (_nullIndName != null)
                cols[i].setName(_nullIndName + i);
            correctName(table, cols[i]);
        }
        return _addNullInd;
    }

    public ForeignKey getJoinForeignKey(ClassMapping cls, Table local,
        Table foreign) {
        if (_joinFKAction == ForeignKey.ACTION_NONE)
            return null;
        ForeignKey fk = new ForeignKey();
        fk.setDeleteAction(_joinFKAction);
        fk.setDeferred(_defer);
        return fk;
    }

    public ForeignKey getJoinForeignKey(FieldMapping fm, Table local,
        Table foreign) {
        if (_joinFKAction == ForeignKey.ACTION_NONE)
            return null;
        ForeignKey fk = new ForeignKey();
        fk.setDeleteAction(_joinFKAction);
        fk.setDeferred(_defer);
        return fk;
    }

    public ForeignKey getForeignKey(ValueMapping vm, String name, Table local,
        Table foreign, boolean inverse) {
        if (_fkAction == ForeignKey.ACTION_NONE)
            return null;
        ForeignKey fk = new ForeignKey();
        fk.setDeleteAction(_fkAction);
        fk.setDeferred(_defer);
        return fk;
    }

    public Index getJoinIndex(FieldMapping fm, Table table, Column[] cols) {
        if (!_indexFK || fm.getJoinForeignKey() == null
            || !fm.getJoinForeignKey().isLogical())
            return null;
        if (areAllPrimaryKeyColumns(cols))
            return null;

        Index idx = new Index();
        idx.setName(getIndexName(null, table, cols));
        return idx;
    }

    /**
     * Return whether all the given columns are primary key columns.
     */
    protected boolean areAllPrimaryKeyColumns(Column[] cols) {
        for (int i = 0; i < cols.length; i++)
            if (!cols[i].isPrimaryKey())
                return false;
        return true;
    }

    /**
     * Generate an index name.
     */
    protected String getIndexName(String name, Table table, Column[] cols) {
        String toReturn = null;
        
        // always use dict for index names because no spec mandates them
        // based on defaults
        if (name == null) {
            name = cols[0].getName();
        }

        if (_removeHungarianNotation){
            name = removeHungarianNotation(name);
        }
        
        if (getDBDictionary() != null) {
            toReturn = getDBDictionary().getValidIndexName(name, table);            
        }
        
        return toReturn; 
    }

    public Index getIndex(ValueMapping vm, String name, Table table,
        Column[] cols) {
        if (!_indexFK || vm.getForeignKey() == null
            || !vm.getForeignKey().isLogical())
            return null;
        if (areAllPrimaryKeyColumns(cols))
            return null;

        Index idx = new Index();
        idx.setName(getIndexName(name, table, cols));
        return idx;
    }

    public Index getIndex(Version vers, Table table, Column[] cols) {
        if (!_indexVers)
            return null;
        Index idx = new Index();
        idx.setName(getIndexName(_versName, table, cols));
        return idx;
    }

    public Index getIndex(Discriminator disc, Table table, Column[] cols) {
        if (!_indexDisc)
            return null;
        Index idx = new Index();
        idx.setName(getIndexName(_discName, table, cols));
        return idx;
    }

    public Unique getJoinUnique(FieldMapping fm, Table table, Column[] cols) {
        return null;
    }

    public Unique getUnique(ValueMapping vm, String name, Table table,
        Column[] cols) {
        return null;
    }

    public String getPrimaryKeyName(ClassMapping cm, Table table) {
        return null;
    }

    public void installPrimaryKey(FieldMapping fm, Table table) {
    }

    ///////////////////////////////
    // Configurable implementation
    ///////////////////////////////

    public void setConfiguration(Configuration conf) {
        this.conf=conf;        
    }

    public void startConfiguration() {
    }

    public void endConfiguration() {
    }
}
