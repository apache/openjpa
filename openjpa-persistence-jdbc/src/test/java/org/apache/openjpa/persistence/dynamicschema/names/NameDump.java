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
package org.apache.openjpa.persistence.dynamicschema.names;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.identifier.DBIdentifier;
import org.apache.openjpa.jdbc.identifier.DBIdentifier.DBIdentifierType;
import org.apache.openjpa.jdbc.kernel.JDBCSeq;
import org.apache.openjpa.jdbc.meta.ClassMapping;
import org.apache.openjpa.jdbc.meta.FieldMapping;
import org.apache.openjpa.jdbc.meta.MappingRepository;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.schema.ForeignKey;
import org.apache.openjpa.jdbc.schema.Index;
import org.apache.openjpa.jdbc.schema.PrimaryKey;
import org.apache.openjpa.jdbc.schema.Schema;
import org.apache.openjpa.jdbc.schema.SchemaGroup;
import org.apache.openjpa.jdbc.schema.Sequence;
import org.apache.openjpa.jdbc.schema.Table;
import org.apache.openjpa.jdbc.schema.Unique;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.kernel.Seq;
import org.apache.openjpa.meta.SequenceMetaData;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactorySPI;
import org.apache.openjpa.persistence.delimited.identifiers.Animal;
import org.apache.openjpa.persistence.delimited.identifiers.Car;
import org.apache.openjpa.persistence.delimited.identifiers.Cat;
import org.apache.openjpa.persistence.delimited.identifiers.Chevrolet;
import org.apache.openjpa.persistence.delimited.identifiers.Dog;
import org.apache.openjpa.persistence.delimited.identifiers.EntityA;
import org.apache.openjpa.persistence.delimited.identifiers.EntityB;
import org.apache.openjpa.persistence.delimited.identifiers.EntityC;
import org.apache.openjpa.persistence.delimited.identifiers.EntityD;
import org.apache.openjpa.persistence.delimited.identifiers.EntityD2;
import org.apache.openjpa.persistence.delimited.identifiers.EntityD3;
import org.apache.openjpa.persistence.delimited.identifiers.EntityD4;
import org.apache.openjpa.persistence.delimited.identifiers.EntityE;
import org.apache.openjpa.persistence.delimited.identifiers.EntityF;
import org.apache.openjpa.persistence.delimited.identifiers.EntityG;
import org.apache.openjpa.persistence.delimited.identifiers.EntityH;
import org.apache.openjpa.persistence.delimited.identifiers.EntityI;
import org.apache.openjpa.persistence.delimited.identifiers.EntityI2;
import org.apache.openjpa.persistence.delimited.identifiers.EntityI3;
import org.apache.openjpa.persistence.delimited.identifiers.EntityI4;
import org.apache.openjpa.persistence.delimited.identifiers.Pontiac;
import org.apache.openjpa.persistence.dynamicschema.Dual;
import org.apache.openjpa.persistence.dynamicschema.EntityReservedWords;
import org.apache.openjpa.persistence.dynamicschema.EntityVeryLongNames;
import org.apache.openjpa.persistence.dynamicschema.Linear;
import org.apache.openjpa.persistence.dynamicschema.Ntile;
import org.apache.openjpa.persistence.dynamicschema.RankedEntry;
import org.apache.openjpa.persistence.dynamicschema.ResWordEntity;
import org.apache.openjpa.persistence.dynamicschema.Window;

/**
 * Records the names that the mapping of a set of entities gets for a
 * dictionary: the identifiers of the tables, columns, constraints, indexes
 * and sequences, the names written into SQL, the DDL and the identifiers that
 * the names read back from the database become. Used to compare the names of
 * a dictionary configured alone with the names it gets next to dictionaries
 * of other databases.
 */
public final class NameDump {

    /**
     * The dictionary configurations to record, by a short key.
     */
    public static final Map<String, Map<String, String>> SETUPS = new LinkedHashMap<>();

    static {
        add("derby", "derby", null);
        add("postgres", "postgres", null);
        add("mysql", "mysql", null);
        add("mariadb", "mariadb", null);
        add("oracle", "oracle", null);
        add("sqlserver", "sqlserver", null);
        add("h2", "h2", null);
        add("hsql", "hsql", null);
        add("db2", "db2", null);
        add("mysql-delimitall", "mysql(DelimitIdentifiers=true)", null);
        add("derby-delimitall", "derby(DelimitIdentifiers=true)", null);
        add("mysql-compat", "mysql", "UpdatedMySQLAndMariaDBReservedWords=false");
        add("mariadb-compat", "mariadb", "UpdatedMySQLAndMariaDBReservedWords=false");
        add("mysql-nodelimsupport", "mysql(SupportsDelimitedIdentifiers=false)", null);
        // MariaDB 10.3 and later, which has sequences
        add("mariadb-seq", "mariadb(NextSequenceQuery=\"SELECT NEXTVAL({0})\")", null);
        // a configured schema that is a reserved word
        add("mysql-schema", "mysql", null);
        SETUPS.get("mysql-schema").put("openjpa.jdbc.Schema", "Current");
        add("derby-schema", "derby", null);
        SETUPS.get("derby-schema").put("openjpa.jdbc.Schema", "Current");
    }

    /**
     * The groups of entities, each mapped in a configuration of its own.
     */
    public static final List<Class<?>[]> GROUPS = List.of(
        new Class<?>[] {Order.class, User.class, Group.class, Item.class, Period.class, Session.class,
            Release.class, Current.class, Function.class, Trigger.class, Domain.class, Partition.class,
            MixedCase.class, QuotedTable.class, LowerCaseTable.class},
        new Class<?>[] {EntityReservedWords.class, ResWordEntity.class, EntityVeryLongNames.class,
            RankedEntry.class, Window.class, Linear.class, Dual.class, Ntile.class},
        new Class<?>[] {EntityA.class, EntityB.class, EntityC.class, EntityD.class, EntityD2.class,
            EntityD3.class, EntityD4.class, EntityE.class, EntityF.class, EntityG.class, EntityH.class,
            EntityI.class, EntityI2.class, EntityI3.class, EntityI4.class, Animal.class, Cat.class, Dog.class,
            Car.class, Chevrolet.class, Pontiac.class},
        new Class<?>[] {XmlDesc.class, XmlWindow.class});

    /**
     * The mapping file of the entities that are mapped in XML.
     */
    private static final String ORM = "org/apache/openjpa/persistence/dynamicschema/names/names-orm.xml";

    /**
     * A data source that refuses every connection, as the names are recorded
     * without a database.
     */
    private static final DataSource NO_CONNECTION = (DataSource) Proxy.newProxyInstance(
        NameDump.class.getClassLoader(), new Class<?>[] {DataSource.class}, (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                return switch (method.getName()) {
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    default -> "NameDump.NO_CONNECTION";
                };
            }
            throw new SQLException("The names are recorded without a database");
        });

    private NameDump() {
    }

    private static void add(String key, String dict, String compatibility) {
        Map<String, String> props = new HashMap<>();
        // what the metadata of the databases tell a connected dictionary
        props.put("openjpa.jdbc.DBDictionary", dict.contains("(")
            ? dict.replace("(", "(SupportsDelimitedIdentifiers=true,") : dict + "(SupportsDelimitedIdentifiers=true)");
        if (compatibility != null) {
            props.put("openjpa.Compatibility", compatibility);
        }
        SETUPS.put(key, props);
    }

    /**
     * Writes the names of the given setups to a directory, one file per
     * setup, to compare the names of two versions of OpenJPA: run it with
     * each setup in a JVM of its own for the names of a dictionary alone,
     * and with all setups in one JVM after <code>--interleaved</code> for
     * the names next to the other dictionaries.
     *
     * @param args the output directory, optionally
     * <code>--interleaved</code>, and the setup keys
     */
    public static void main(String[] args) throws IOException {
        Path dir = Path.of(args[0]);
        Files.createDirectories(dir);
        List<String> keys = Arrays.asList(args).subList(1, args.length);
        Map<String, String> dumps;
        if (!keys.isEmpty() && "--interleaved".equals(keys.get(0))) {
            dumps = dumpInterleaved(keys.subList(1, keys.size()));
        } else {
            dumps = new LinkedHashMap<>();
            for (String key : keys) {
                dumps.put(key, dump(key));
            }
        }
        for (Map.Entry<String, String> dump : dumps.entrySet()) {
            Files.writeString(dir.resolve(dump.getKey() + ".txt"), dump.getValue(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Configures the dictionaries of all given setups in the given order
     * first, and then maps the entities of each setup, in the reverse order,
     * so that each setup but the last maps its entities after the
     * dictionaries of other setups were configured.
     *
     * @return the names by setup key
     */
    public static Map<String, String> dumpInterleaved(List<String> keys) {
        Map<String, List<OpenJPAEntityManagerFactorySPI>> emfs = new LinkedHashMap<>();
        for (String key : keys) {
            List<OpenJPAEntityManagerFactorySPI> list = new ArrayList<>();
            for (Class<?>[] group : GROUPS) {
                OpenJPAEntityManagerFactorySPI emf = createFactory(SETUPS.get(key), group);
                ((JDBCConfiguration) emf.getConfiguration()).getDBDictionaryInstance();
                list.add(emf);
            }
            emfs.put(key, list);
        }
        List<String> reversed = new ArrayList<>(keys);
        Collections.reverse(reversed);
        Map<String, String> dumps = new LinkedHashMap<>();
        for (String key : reversed) {
            StringBuilder buf = new StringBuilder();
            List<OpenJPAEntityManagerFactorySPI> list = emfs.get(key);
            for (int i = 0; i < GROUPS.size(); i++) {
                try (OpenJPAEntityManagerFactorySPI emf = list.get(i)) {
                    buf.append(dump((JDBCConfiguration) emf.getConfiguration(), GROUPS.get(i)));
                }
            }
            dumps.put(key, buf.toString());
        }
        return dumps;
    }

    /**
     * Returns the names of all groups for the given setup.
     */
    public static String dump(String key) {
        StringBuilder buf = new StringBuilder();
        for (Class<?>[] group : GROUPS) {
            buf.append(dump(SETUPS.get(key), group));
        }
        return buf.toString();
    }

    /**
     * Creates a factory for the given setup and entities that never connects.
     */
    public static OpenJPAEntityManagerFactorySPI createFactory(Map<String, String> setup, Class<?>... types) {
        Map<String, Object> props = new HashMap<>(setup);
        props.put("openjpa.MetaDataFactory", "jpa(Types=" + Arrays.stream(types).map(Class::getName)
            .collect(Collectors.joining(";"))
            + (types[0] == XmlDesc.class ? ",Resources=" + ORM : "") + ")");
        props.put("openjpa.jdbc.SynchronizeMappings", "");
        props.put("openjpa.Log", "none");
        props.put("openjpa.ConnectionFactory", NO_CONNECTION);
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("test", props);
        return (OpenJPAEntityManagerFactorySPI) emf;
    }

    /**
     * Returns the names of the given entities for the given setup.
     */
    public static String dump(Map<String, String> setup, Class<?>... types) {
        try (OpenJPAEntityManagerFactorySPI emf = createFactory(setup, types)) {
            return dump((JDBCConfiguration) emf.getConfiguration(), types);
        }
    }

    /**
     * Returns the names of the given entities in the given configuration.
     */
    public static String dump(JDBCConfiguration conf, Class<?>... types) {
        List<String> out = new ArrayList<>();
        DBDictionary dict = conf.getDBDictionaryInstance();
        MappingRepository repos = conf.getMappingRepositoryInstance();
        List<ClassMapping> mappings = new ArrayList<>();
        Class<?>[] sorted = types.clone();
        Arrays.sort(sorted, Comparator.comparing(Class::getName));
        for (Class<?> type : sorted) {
            try {
                ClassMapping mapping = repos.getMapping(type, type.getClassLoader(), true);
                if (mapping != null) {
                    mappings.add(mapping);
                }
            } catch (RuntimeException e) {
                out.add("ERROR " + type.getSimpleName() + ": " + e.getClass().getSimpleName() + " "
                    + e.getMessage());
            }
        }
        SchemaGroup group = repos.getSchemaGroup();
        for (ClassMapping mapping : mappings) {
            try {
                addSequences(conf, mapping, group);
            } catch (RuntimeException e) {
                out.add("SEQERROR " + mapping.getDescribedType().getSimpleName() + ": "
                    + e.getClass().getSimpleName() + " " + e.getMessage());
            }
            out.add("MAPPING " + mapping.getDescribedType().getSimpleName() + " table="
                + names(dict, mapping.getTable() == null ? null : mapping.getTable().getFullIdentifier()));
            for (FieldMapping fm : mapping.getDefinedFieldMappings()) {
                StringBuilder buf = new StringBuilder("  FIELD " + fm.getName());
                if (fm.getTable() != null && fm.getTable() != mapping.getTable()) {
                    buf.append(" table=").append(names(dict, fm.getTable().getFullIdentifier()));
                }
                for (Column col : fm.getColumns()) {
                    buf.append(" col=").append(names(dict, col.getIdentifier()));
                }
                if (fm.getJoinForeignKey() != null) {
                    for (Column col : fm.getJoinForeignKey().getColumns()) {
                        buf.append(" joincol=").append(names(dict, col.getIdentifier()));
                    }
                }
                for (Column col : fm.getElementMapping().getColumns()) {
                    buf.append(" elemcol=").append(names(dict, col.getIdentifier()));
                }
                for (Column col : fm.getKeyMapping().getColumns()) {
                    buf.append(" keycol=").append(names(dict, col.getIdentifier()));
                }
                if (fm.getOrderColumn() != null) {
                    buf.append(" ordercol=").append(names(dict, fm.getOrderColumn().getIdentifier()));
                }
                out.add(buf.toString());
            }
        }
        List<String> ddl = new ArrayList<>();
        for (Schema schema : group.getSchemas()) {
            for (Table table : schema.getTables()) {
                out.add("TABLE " + names(dict, table.getFullIdentifier()) + " read="
                    + readBack(dict, table.getFullIdentifier(), DBIdentifierType.TABLE));
                for (Column col : table.getColumns()) {
                    out.add("  COLUMN " + names(dict, col.getIdentifier()) + " read="
                        + readBack(dict, col.getIdentifier(), DBIdentifierType.COLUMN));
                }
                PrimaryKey pk = table.getPrimaryKey();
                if (pk != null) {
                    out.add("  PK " + names(dict, pk.getIdentifier()) + " " + columns(dict, pk.getColumns()));
                }
                for (ForeignKey fk : table.getForeignKeys()) {
                    out.add("  FK " + names(dict, fk.getIdentifier()) + " " + columns(dict, fk.getColumns())
                        + " -> " + names(dict, fk.getPrimaryKeyTable().getFullIdentifier()));
                    ddl.addAll(sql(() -> dict.getAddForeignKeySQL(fk)));
                }
                for (Index idx : table.getIndexes()) {
                    out.add("  INDEX " + names(dict, idx.getIdentifier()) + " " + columns(dict, idx.getColumns()));
                    ddl.addAll(sql(() -> dict.getCreateIndexSQL(idx)));
                }
                for (Unique unq : table.getUniques()) {
                    out.add("  UNIQUE " + names(dict, unq.getIdentifier()) + " " + columns(dict, unq.getColumns()));
                }
                ddl.addAll(sql(() -> dict.getCreateTableSQL(table, group)));
            }
            for (Sequence seq : schema.getSequences()) {
                out.add("SEQUENCE " + names(dict, seq.getFullIdentifier()) + " read="
                    + readBack(dict, seq.getIdentifier(), DBIdentifierType.SEQUENCE));
                ddl.addAll(sql(() -> dict.getCreateSequenceSQL(seq)));
            }
        }
        out.sort(null);
        ddl.sort(null);
        StringBuilder buf = new StringBuilder();
        buf.append("=== ").append(sorted[0].getName()).append('\n');
        out.forEach(s -> buf.append(s).append('\n'));
        ddl.forEach(s -> buf.append("DDL ").append(s).append('\n'));
        return buf.toString();
    }

    private interface SqlSupplier {
        String[] get();
    }

    private static List<String> sql(SqlSupplier supplier) {
        try {
            return Arrays.asList(supplier.get());
        } catch (RuntimeException e) {
            return List.of("SQLERROR " + e.getClass().getSimpleName() + " " + e.getMessage());
        }
    }

    private static void addSequences(JDBCConfiguration conf, ClassMapping mapping, SchemaGroup group) {
        SequenceMetaData smd = mapping.getIdentitySequenceMetaData();
        if (smd != null) {
            addSequence(smd.getInstance(mapping.getDescribedType().getClassLoader()), mapping, group);
        }
        for (FieldMapping fm : mapping.getDefinedFieldMappings()) {
            smd = fm.getValueSequenceMetaData();
            if (smd != null) {
                addSequence(smd.getInstance(mapping.getDescribedType().getClassLoader()), mapping, group);
            }
        }
    }

    private static void addSequence(Seq seq, ClassMapping mapping, SchemaGroup group) {
        if (seq instanceof JDBCSeq jdbcSeq) {
            jdbcSeq.addSchema(mapping, group);
        }
    }

    private static String columns(DBDictionary dict, Column[] cols) {
        return Arrays.stream(cols).map(c -> names(dict, c.getIdentifier())).collect(Collectors.joining(",", "(", ")"));
    }

    /**
     * The identifier as OpenJPA keeps it and as it is written into SQL.
     */
    private static String names(DBDictionary dict, DBIdentifier id) {
        if (id == null || DBIdentifier.isNull(id)) {
            return "<null>";
        }
        return id.getName() + "|" + dict.toDBName(id);
    }

    /**
     * The identifier that the name of the given identifier becomes when the
     * database metadata returns it as the database stores it, and whether
     * it equals the given identifier, and the name the database metadata is
     * asked for.
     */
    private static String readBack(DBDictionary dict, DBIdentifier id, DBIdentifierType type) {
        if (id == null || DBIdentifier.isNull(id)) {
            return "<null>";
        }
        DBIdentifier stored = dict.getNamingUtil().convertSchemaCase(id.getUnqualifiedName());
        DBIdentifier read = dict.fromDBName(stored.getName(), type);
        // the name the metadata is asked for
        String lookup = dict.convertSchemaCase(id.getUnqualifiedName());
        return stored.getName() + "->" + read.getName() + (read.equals(id.getUnqualifiedName()) ? "" : " DIFFERENT")
            + " lookup=" + lookup;
    }
}
