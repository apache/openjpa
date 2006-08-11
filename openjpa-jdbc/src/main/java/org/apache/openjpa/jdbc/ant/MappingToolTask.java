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
package org.apache.openjpa.jdbc.ant;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.conf.JDBCConfigurationImpl;
import org.apache.openjpa.jdbc.meta.MappingTool;
import org.apache.openjpa.jdbc.schema.SchemaTool;
import org.apache.openjpa.lib.ant.AbstractTask;
import org.apache.openjpa.lib.conf.ConfigurationImpl;
import org.apache.openjpa.lib.util.Files;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.util.MultiLoaderClassResolver;

/**
 * Executes the {@link MappingTool} on the specified files.
 * This task can take the following arguments:
 * <ul>
 * <li><code>action</code></li>
 * <li><code>meta</code></li>
 * <li><code>schemaAction</code></li>
 * <li><code>dropTables</code></li>
 * <li><code>ignoreErrors</code></li>
 * <li><code>readSchema</code></li>
 * <li><code>primaryKeys</code></li>
 * <li><code>foreignKeys</code></li>
 * <li><code>indexes</code></li>
 * <li><code>file</code></li>
 * <li><code>schemaFile</code></li>
 * <li><code>sqlFile</code></li>
 * </ul> Of these arguments, only <code>action</code> is required.
 */
public class MappingToolTask
    extends AbstractTask {

    private static final Localizer _loc = Localizer.forPackage
        (MappingToolTask.class);

    protected MappingTool.Flags flags = new MappingTool.Flags();
    protected String file = null;
    protected String schemaFile = null;
    protected String sqlFile = null;

    /**
     * Set the enumerated MappingTool action type.
     */
    public void setAction(Action act) {
        flags.action = act.getValue();
    }

    /**
     * Set the enumerated SchemaTool action type.
     */
    public void setSchemaAction(SchemaAction act) {
        flags.schemaAction = act.getValue();
    }

    /**
     * Set whether the MappingTool should read the full schema.
     */
    public void setReadSchema(boolean readSchema) {
        flags.readSchema = readSchema;
    }

    /**
     * Set whether we want the MappingTool to ignore SQL errors.
     */
    public void setIgnoreErrors(boolean ignoreErrors) {
        flags.ignoreErrors = ignoreErrors;
    }

    /**
     * Set whether the MappingTool should drop tables.
     */
    public void setDropTables(boolean dropTables) {
        flags.dropTables = dropTables;
    }

    /**
     * Set whether to drop OpenJPA tables.
     */
    public void setOpenJPATables(boolean openjpaTables) {
        flags.openjpaTables = openjpaTables;
    }

    /**
     * Set whether the MappingTool should drop sequences.
     */
    public void setDropSequences(boolean dropSequences) {
        flags.dropSequences = dropSequences;
    }

    /**
     * Set whether the MappingTool should manipulate sequences.
     */
    public void setSequences(boolean sequences) {
        flags.sequences = sequences;
    }

    /**
     * Set whether to generate primary key information.
     */
    public void setPrimaryKeys(boolean pks) {
        flags.primaryKeys = pks;
    }

    /**
     * Set whether to generate foreign key information.
     */
    public void setForeignKeys(boolean fks) {
        flags.foreignKeys = fks;
    }

    /**
     * Set whether to generate index information.
     */
    public void setIndexes(boolean idxs) {
        flags.indexes = idxs;
    }

    /**
     * Set the output file we want the MappingTool to write to.
     */
    public void setFile(String file) {
        this.file = file;
    }

    /**
     * Set the output file for an XML representation of the planned schema.
     */
    public void setSchemaFile(String schemaFile) {
        this.schemaFile = schemaFile;
    }

    /**
     * Set the output file we want the MappingTool to write a SQL script to.
     */
    public void setSQLFile(String sqlFile) {
        this.sqlFile = sqlFile;
    }

    /**
     * Set whether this action applies to metadata as well as mappings.
     */
    public void setMeta(boolean meta) {
        flags.meta = meta;
    }

    protected ConfigurationImpl newConfiguration() {
        return new JDBCConfigurationImpl();
    }

    protected void executeOn(String[] files)
        throws Exception {
        if (MappingTool.ACTION_IMPORT.equals(flags.action))
            assertFiles(files);

        ClassLoader loader = getClassLoader();
        if (flags.meta && MappingTool.ACTION_ADD.equals(flags.action))
            flags.metaDataFile = Files.getFile(file, loader);
        else
            flags.mappingWriter = Files.getWriter(file, loader);
        flags.schemaWriter = Files.getWriter(schemaFile, loader);
        flags.sqlWriter = Files.getWriter(sqlFile, loader);

        MultiLoaderClassResolver resolver = new MultiLoaderClassResolver();
        resolver.addClassLoader(loader);
        resolver.addClassLoader(MappingTool.class.getClassLoader());
        JDBCConfiguration conf = (JDBCConfiguration) getConfiguration();
        conf.setClassResolver(resolver);

        if (!MappingTool.run(conf, files, flags, loader))
            throw new BuildException(_loc.get("bad-conf", "MappingToolTask")
                .getMessage());
    }

    public static class Action
        extends EnumeratedAttribute {

        public String[] getValues() {
            return MappingTool.ACTIONS;
        }
    }

    public static class SchemaAction
        extends EnumeratedAttribute {

        public String[] getValues() {
            String[] actions = new String[SchemaTool.ACTIONS.length + 1];
            System.arraycopy(SchemaTool.ACTIONS, 0, actions, 0,
                SchemaTool.ACTIONS.length);
            actions[actions.length - 1] = "none";
            return actions;
        }
    }
}

