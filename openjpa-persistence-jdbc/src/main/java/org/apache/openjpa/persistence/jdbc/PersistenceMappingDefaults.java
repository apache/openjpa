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
package org.apache.openjpa.persistence.jdbc;

import org.apache.openjpa.jdbc.meta.ClassMapping;
import org.apache.openjpa.jdbc.meta.Discriminator;
import org.apache.openjpa.jdbc.meta.FieldMapping;
import org.apache.openjpa.jdbc.meta.MappingDefaultsImpl;
import org.apache.openjpa.jdbc.meta.ValueMapping;
import org.apache.openjpa.jdbc.meta.Version;
import org.apache.openjpa.jdbc.meta.strats.FlatClassStrategy;
import org.apache.openjpa.jdbc.meta.strats.NoneDiscriminatorStrategy;
import org.apache.openjpa.jdbc.meta.strats.NoneVersionStrategy;
import org.apache.openjpa.jdbc.meta.strats.NumberVersionStrategy;
import org.apache.openjpa.jdbc.meta.strats.SubclassJoinDiscriminatorStrategy;
import org.apache.openjpa.jdbc.meta.strats.ValueMapDiscriminatorStrategy;
import org.apache.openjpa.jdbc.meta.strats.VerticalClassStrategy;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.schema.Schema;
import org.apache.openjpa.jdbc.schema.Table;
import org.apache.openjpa.jdbc.sql.JoinSyntaxes;
import org.apache.openjpa.meta.JavaTypes;
import serp.util.Strings;

/**
 * Supplies default mapping information in accordance with JPA spec.
 *
 * @author Steve Kim
 * @author Abe White
 * @nojavadoc
 */
public class PersistenceMappingDefaults
    extends MappingDefaultsImpl {

    private boolean _prependFieldNameToJoinTableInverseJoinColumns = true;

    public PersistenceMappingDefaults() {
        setDefaultMissingInfo(true);
        setStoreEnumOrdinal(true);
        setOrderLists(false);
        setAddNullIndicator(false);
        setDiscriminatorColumnName("DTYPE");
    }

    /**
     * Whether to prepend the field name to the default name of inverse join
     * columns within join tables.  Defaults to true per spec, but set to false
     * for compatibility with older versions of OpenJPA.
     */
    public boolean getPrependFieldNameToJoinTableInverseJoinColumns() {
        return _prependFieldNameToJoinTableInverseJoinColumns;
    }

    /**
     * Whether to prepend the field name to the default name of inverse join
     * columns within join tables.  Defaults to true per spec, but set to false
     * for compatibility with older versions of OpenJPA.
     */
    public void setPrependFieldNameToJoinTableInverseJoinColumns(boolean val) {
        _prependFieldNameToJoinTableInverseJoinColumns = val;
    }

    @Override
    public Object getStrategy(Version vers, boolean adapt) {
        Object strat = super.getStrategy(vers, adapt);
        ClassMapping cls = vers.getClassMapping();
        if (strat != null || cls.getJoinablePCSuperclassMapping() != null
            || cls.getVersionField() != null)
            return strat;

        if (vers.getMappingInfo().getColumns().isEmpty())
            return NoneVersionStrategy.getInstance();
        return new NumberVersionStrategy();
    }

    @Override
    public Object getStrategy(Discriminator disc, boolean adapt) {
        Object strat = super.getStrategy(disc, adapt);
        ClassMapping cls = disc.getClassMapping();
        if (strat != null || cls.getJoinablePCSuperclassMapping() != null
            || disc.getMappingInfo().getValue() != null)
            return strat;

        // don't use a column-based discriminator approach unless user has set
        // a column explicitly or is using flat inheritance explicitly
        if (!disc.getMappingInfo().getColumns().isEmpty())
            return new ValueMapDiscriminatorStrategy();

        ClassMapping base = cls;
        while (base.getMappingInfo().getHierarchyStrategy() == null
            && base.getPCSuperclassMapping() != null)
            base = base.getPCSuperclassMapping();

        strat = base.getMappingInfo().getHierarchyStrategy();
        if (FlatClassStrategy.ALIAS.equals(strat))
            return new ValueMapDiscriminatorStrategy();
        if (VerticalClassStrategy.ALIAS.equals(strat)
            && dict.joinSyntax != JoinSyntaxes.SYNTAX_TRADITIONAL)
            return new SubclassJoinDiscriminatorStrategy();
        return NoneDiscriminatorStrategy.getInstance();
    }

    @Override
    public String getTableName(ClassMapping cls, Schema schema) {
        if (cls.getTypeAlias() != null)
            return cls.getTypeAlias();
        return Strings.getClassName(cls.getDescribedType()).replace('$', '_');
    }

    @Override
    public String getTableName(FieldMapping fm, Schema schema) {
        // base name is table of defining type + '_'
        String name = fm.getDefiningMapping().getTable().getName() + "_";

        // if this is an assocation table, spec says to suffix with table of
        // the related type. spec doesn't cover other cases; we're going to
        // suffix with the field name
        ClassMapping rel = fm.getElementMapping().getTypeMapping();
        boolean assoc = rel != null && rel.getTable() != null
            && fm.getTypeCode() != JavaTypes.MAP;
        if (assoc)
            name += rel.getTable().getName();
        else
            name += fm.getName();
        return name.replace('$', '_');
    }

    @Override
    public void populateJoinColumn(FieldMapping fm, Table local, Table foreign,
        Column col, Object target, int pos, int cols) {
        // only use spec defaults with column targets
        if (!(target instanceof Column))
            return;

        // if this is a bidi relation, prefix with inverse field name, else
        // prefix with owning entity name
        FieldMapping[] inverses = fm.getInverseMappings();
        String name;
        if (inverses.length > 0)
            name = inverses[0].getName();
        else
            name = fm.getDefiningMapping().getTypeAlias();
        String targetName = ((Column) target).getName();
        String tempName = null;
        if ((name.length() + targetName.length()) >= dict.maxColumnNameLength)
            tempName = name.substring(0, dict.maxColumnNameLength
                    - targetName.length() - 1);
        // suffix with '_' + target column
        if (tempName == null)
            tempName = name;
        name = tempName + "_" + targetName;
        name = dict.getValidColumnName(name, foreign);
        col.setName(name);
    }

    @Override
    public void populateForeignKeyColumn(ValueMapping vm, String name,
        Table local, Table foreign, Column col, Object target, boolean inverse,
        int pos, int cols) {
        boolean elem = vm == vm.getFieldMapping().getElement()
            && vm.getFieldMapping().getTypeCode() != JavaTypes.MAP;

        // if this is a non-inverse collection element key, it must be in
        // a join table: if we're not prepending the field name, leave the
        // default
        if (!_prependFieldNameToJoinTableInverseJoinColumns && !inverse && elem)
            return;

        // otherwise jpa always uses <field>_<pkcol> for column name, even
        // when only one col
        if (target instanceof Column) {
            if (elem)
                name = vm.getFieldMapping().getName();

            if (isRemoveHungarianNotation())
                name = removeHungarianNotation(name);

            name = name + "_" + ((Column) target).getName();
            // No need to check for uniqueness.
            name = dict.getValidColumnName(name, local, false);
            col.setName(name);
        }
    }

    @Override
    public void populateColumns(Version vers, Table table, Column[] cols) {
        // check for version field and use its name as column name
        FieldMapping fm = vers.getClassMapping().getVersionFieldMapping();
        if (fm != null && cols.length == 1)
            cols[0].setName(fm.getName());
        else
            super.populateColumns(vers, table, cols);
    }
}
