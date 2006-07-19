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
package org.apache.openjpa.jdbc.meta;

import org.apache.openjpa.jdbc.meta.strats.NoneVersionStrategy;
import org.apache.openjpa.jdbc.meta.strats.SuperclassVersionStrategy;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.schema.Index;
import org.apache.openjpa.jdbc.schema.SchemaGroup;
import org.apache.openjpa.jdbc.schema.Table;

/**
 * Information about the mapping from a version indicator to the schema, in
 * raw form. The columns and tables used in mapping info will not be part of
 * the {@link SchemaGroup} used at runtime. Rather, they will be structs
 * with the relevant pieces of information filled in.
 *
 * @author Abe White
 */
public class VersionMappingInfo
    extends MappingInfo {

    /**
     * Return the columns set for this version, based on the given templates.
     */
    public Column[] getColumns(Version version, Column[] tmplates,
        boolean adapt) {
        Table table = version.getClassMapping().getTable();
        version.getMappingRepository().getMappingDefaults().populateColumns
            (version, table, tmplates);
        return createColumns(version, null, tmplates, table, adapt);
    }

    /**
     * Return the index to set on the version columns, or null if none.
     */
    public Index getIndex(Version version, Column[] cols, boolean adapt) {
        Index idx = null;
        if (cols.length > 0)
            idx = version.getMappingRepository().getMappingDefaults().
                getIndex(version, cols[0].getTable(), cols);
        return createIndex(version, null, idx, cols, adapt);
    }

    /**
     * Synchronize internal information with the mapping data for the given
     * version.
     */
    public void syncWith(Version version) {
        clear(false);

        ClassMapping cls = version.getClassMapping();
        Column[] cols = version.getColumns();

        setColumnIO(version.getColumnIO());
        syncColumns(version, cols, false);
        syncIndex(version, version.getIndex());

        if (version.getStrategy() == null
            || version.getStrategy()instanceof SuperclassVersionStrategy)
            return;

        // explicit version strategy if:
        // - unmapped class and version mapped
        // - mapped base class
        // - mapped subclass that doesn't rely on superclass version
        String strat = version.getStrategy().getAlias();
        if ((!cls.isMapped() && !NoneVersionStrategy.ALIAS.equals(strat))
            || (cls.isMapped()
            && cls.getJoinablePCSuperclassMapping() == null))
            setStrategy(strat);
    }
}
