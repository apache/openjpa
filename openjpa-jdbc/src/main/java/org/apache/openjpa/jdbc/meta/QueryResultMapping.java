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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.sql.Joins;
import org.apache.openjpa.lib.meta.SourceTracker;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.lib.xml.Commentable;
import org.apache.openjpa.meta.JavaTypes;
import org.apache.openjpa.meta.MetaDataModes;
import org.apache.openjpa.util.MetaDataException;
import serp.util.Strings;

/**
 * Mapping of a query result set to scalar and/or persistence-capable
 * object-level values.
 *
 * @author Pinaki Poddar
 * @author Abe White
 */
public class QueryResultMapping
    implements MetaDataModes, SourceTracker, Commentable {

    private static final Localizer _loc = Localizer.forPackage
        (QueryResultMapping.class);

    private final String _name;
    private final MappingRepository _repos;
    private File _file = null;
    private Object _scope = null;
    private int _srcType = SRC_OTHER;
    private int _mode = MODE_QUERY;
    private Class _class = null;
    private int _idx = 0;
    private String[] _comments = null;
    private List _colList = null;
    private List _pcList = null;

    private PCResult[] _pcs = null;
    private Object[] _cols = null;

    /**
     * Construct with the given name.
     */
    QueryResultMapping(String name, MappingRepository repos) {
        _name = name;
        _repos = repos;
    }

    /**
     * Return the name for this query result.
     */
    public String getName() {
        return _name;
    }

    /**
     * The class that defines this query result, or null if none.
     */
    public Class getDefiningType() {
        return _class;
    }

    /**
     * The class that defines this query result, or null if none.
     */
    public void setDefiningType(Class cls) {
        _class = cls;
    }

    /**
     * Ids of mapped scalar columns in the result. These will typically be
     * column names.
     *
     * @see org.apache.openjpa.jdbc.sql.Result
     */
    public Object[] getColumnResults() {
        if (_cols == null) {
            Object[] cols;
            if (_colList == null)
                cols = new Object[0];
            else
                cols = _colList.toArray();
            _cols = cols;
        }
        return _cols;
    }

    /**
     * Add the id of a mapped column in the query result. This will typically
     * be a column name.
     *
     * @see org.apache.openjpa.jdbc.sql.Result
     */
    public void addColumnResult(Object id) {
        _cols = null;
        if (_colList == null)
            _colList = new ArrayList();
        _colList.add(id);
    }

    /**
     * Return the mapped persistence-capable types in the query result.
     */
    public PCResult[] getPCResults() {
        if (_pcs == null) {
            PCResult[] pcs;
            if (_pcList == null)
                pcs = new PCResult[0];
            else
                pcs = (PCResult[]) _pcList.toArray
                    (new PCResult[_pcList.size()]);
            _pcs = pcs;
        }
        return _pcs;
    }

    /**
     * Add a mapped persistence-capable result with the given candidate type.
     */
    public PCResult addPCResult(Class candidate) {
        _pcs = null;
        PCResult pc = new PCResult(candidate);
        if (_pcList == null)
            _pcList = new ArrayList();
        _pcList.add(pc);
        return pc;
    }

    /**
     * The source mode of this query result.
     */
    public int getSourceMode() {
        return _mode;
    }

    /**
     * The source mode of this query result.
     */
    public void setSourceMode(int mode) {
        _mode = mode;
    }

    /**
     * Relative order of result mapping in metadata.
     */
    public int getListingIndex() {
        return _idx;
    }

    /**
     * Relative order of result mapping in metadata.
     */
    public void setListingIndex(int idx) {
        _idx = idx;
    }

    public String toString() {
        return _name;
    }

    ///////////////
    // Commentable
    ///////////////

    public String[] getComments() {
        return (_comments == null) ? EMPTY_COMMENTS : _comments;
    }

    public void setComments(String[] comments) {
        _comments = comments;
    }

    ////////////////////////////////
    // SourceTracker implementation
    ////////////////////////////////

    public File getSourceFile() {
        return _file;
    }

    public Object getSourceScope() {
        return _scope;
    }

    public int getSourceType() {
        return _srcType;
    }

    public void setSource(File file, Object scope, int srcType) {
        _file = file;
        _scope = scope;
        _srcType = srcType;
    }

    public String getResourceName() {
        return (_class == null) ? _name : _class.getName() + ":" + _name;
    }

    /**
     * A persistence-capable result.
     */
    public class PCResult {

        /**
         * Path token to represent a discriminator.
         */
        public static final String DISCRIMINATOR = "<discriminator>";

        private final Class _candidate;
        private ClassMapping _candidateMap = null;
        private Map _rawMappings = null; // string->object
        private Map _mappings = null; // list->columnmap
        private Map _eager = null; // list->fetchinfo
        private FetchInfo _fetchInfo = null; // for top-level

        /**
         * Supply candidate type on construction.
         */
        private PCResult(Class candidate) {
            _candidate = candidate;
        }

        /**
         * The result candidate class.
         */
        public Class getCandidateType() {
            return _candidate;
        }

        /**
         * Candidate mapping.
         */
        public ClassMapping getCandidateTypeMapping() {
            if (_candidateMap == null)
                _candidateMap = _repos.getMapping(_candidate, null, true);
            return _candidateMap;
        }

        /**
         * Return the raw mapping paths supplied with {@link #addMapping}, or
         * empty array if none.
         */
        public String[] getMappingPaths() {
            if (_rawMappings == null)
                return new String[0];
            Collection keys = _rawMappings.keySet();
            return (String[]) keys.toArray(new String[keys.size()]);
        }

        /**
         * Return the mapping id for the given path supplied with
         * {@link #addMapping}, or null if none.
         */
        public Object getMapping(String path) {
            return (_rawMappings == null) ? null : _rawMappings.get(path);
        }

        /**
         * Map the given path to the given result id.
         */
        public void addMapping(String path, Object id) {
            if (StringUtils.isEmpty(path))
                throw new MetaDataException(_loc.get("null-path",
                    QueryResultMapping.this, _candidate));

            _mappings = null;
            _eager = null;
            _fetchInfo = null;
            if (_rawMappings == null)
                _rawMappings = new HashMap();
            _rawMappings.put(path, id);
        }

        /**
         * Map the given request onto a result id.
         *
         * @param path stack of data requests (see
         * {@link org.apache.openjpa.jdbc.sql.Result#startDataRequest})
         * @param id requested id or column (see
         * {@link org.apache.openjpa.jdbc.sql.Result} APIs)
         * @param joins requested joins, or null
         * @return the id or column to fetch from the result
         * (typically a column name)
         */
        public Object map(List path, Object id, Joins joins) {
            if (_rawMappings == null || !(id instanceof Column))
                return id;

            resolve();
            ColumnMap cm = (ColumnMap) _mappings.get(path);
            return (cm == null) ? id : cm.map((Column) id);
        }

        /**
         * Return true if the mapped result contains eager data for the given
         * field at the given path.
         *
         * @param path stack of data requests (see
         * {@link org.apache.openjpa.jdbc.sql.Result#startDataRequest})
         */
        public boolean hasEager(List path, FieldMapping field) {
            if (_rawMappings == null)
                return false;

            resolve();
            if (path.isEmpty())
                return _fetchInfo.eager.get(field.getIndex());
            if (_eager == null)
                return false;
            FetchInfo info = (FetchInfo) _eager.get(path);
            return info != null && info.eager.get(field.getIndex());
        }

        /**
         * Return the field indexes to exclude when loading data for the
         * given path.
         */
        public BitSet getExcludes(List path) {
            if (_rawMappings == null)
                return null;

            resolve();
            if (path.isEmpty())
                return _fetchInfo.excludes;
            if (_eager == null)
                return null;
            FetchInfo info = (FetchInfo) _eager.get(path);
            return (info == null) ? null : info.excludes;
        }

        /**
         * Resolve internal datastructures from raw mappings.
         */
        private synchronized void resolve() {
            if (_rawMappings == null || _mappings != null)
                return;

            _mappings = new HashMap();
            _fetchInfo = new FetchInfo(getCandidateTypeMapping());

            Map.Entry entry;
            for (Iterator itr = _rawMappings.entrySet().iterator();
                itr.hasNext();) {
                entry = (Map.Entry) itr.next();
                resolveMapping((String) entry.getKey(), entry.getValue());
            }
        }

        /**
         * Resolve the given mapping path.
         */
        private void resolveMapping(String path, Object id) {
            // build up path to second-to-last token
            String[] tokens = Strings.split(path, ".", 0);
            List rpath = new ArrayList(tokens.length);
            ClassMapping candidate = getCandidateTypeMapping();
            FieldMapping fm = null;
            for (int i = 0; i < tokens.length - 1; i++) {
                fm = candidate.getFieldMapping(tokens[i]);
                if (fm == null)
                    throw new MetaDataException(_loc.get("bad-path",
                        QueryResultMapping.this, _candidate, path));

                if (fm.getEmbeddedMapping() != null) {
                    recordIncluded(candidate, rpath, fm);
                    candidate = fm.getEmbeddedMapping();
                } else
                    candidate = fm.getTypeMapping();
                if (candidate == null)
                    throw new MetaDataException(_loc.get("untraversable-path",
                        QueryResultMapping.this, _candidate, path));
                rpath.add(fm);
            }

            String lastToken = tokens[tokens.length - 1];
            if (DISCRIMINATOR.equals(lastToken)) {
                Discriminator discrim = candidate.getDiscriminator();
                rpath.add(discrim);
                assertSingleColumn(discrim.getColumns(), path);
                _mappings.put(rpath, new SingleColumnMap(id));
            } else {
                FieldMapping last = candidate.getFieldMapping(lastToken);
                if (last == null)
                    throw new MetaDataException(_loc.get("untraversable-path",
                        QueryResultMapping.this, _candidate, path));
                Column[] cols = last.getColumns();
                if (last.isVersion())
                    cols = candidate.getVersion().getColumns();
                assertSingleColumn(cols, path);
                Column col = cols[0];
                
                // special-case oid fields, since path lists supplied for
                // them at runtime don't include the embedded fields
                if (fm != null && fm.getDeclaredTypeCode() == JavaTypes.OID) {
                    addComplexColumnMapping(fm, rpath, col, id);
                    return;
                }

                if (fm != null && fm.getForeignKey() != null) {
                    // if the last field is one of the joinables used in the
                    // relation's foreign key, map to relation field path.
                    // otherwise, record that we have an eager result
                    Column fkCol = fm.getForeignKey().getColumn(col);
                    if (fkCol != null)
                        addComplexColumnMapping(fm, new ArrayList(rpath),
                            fkCol, id);
                    else {
                        recordEager(candidate, rpath, fm);
                        recordIncluded(candidate, rpath, last);
                    }
                } else
                    recordIncluded(candidate, rpath, last);

                // map to related field path. because the SingleColumnMap
                // doesn't test the requested column, it will accept
                // requests for both the fk col or the related field col
                rpath.add(last);
                _mappings.put(rpath, new SingleColumnMap(id));
            }
        }

        /**
         * Create an appropriate column mapping for the given field.
         */
        private void addComplexColumnMapping(FieldMapping fm, List rpath,
            Column col, Object id) {
            if (fm.getColumns().length == 1)
                _mappings.put(rpath, new SingleColumnMap(id));
            else {
                MultiColumnMap mcm = (MultiColumnMap) _mappings.get(rpath);
                if (mcm == null) {
                    mcm = new MultiColumnMap(fm.getColumns());
                    _mappings.put(rpath, mcm);
                }
                mcm.set(col, id);
            }
        }

        /**
         * For now, we only allow mappings with a single column. In the
         * future we might introduce a syntax to map multiple columns.
         */
        private void assertSingleColumn(Column[] cols, String path) {
            if (cols.length != 1)
                throw new MetaDataException(_loc.get("num-cols-path",
                    QueryResultMapping.this, _candidate, path));
        }

        /**
         * Record that there may be eager data for the given field at the given
         * path.
         */
        private void recordEager(ClassMapping candidate, List path,
            FieldMapping fm) {
            if (path.size() == 1) {
                _fetchInfo.eager.set(fm.getIndex());
                _fetchInfo.excludes.clear(fm.getIndex());
            } else {
                // record at previous path
                List copy = new ArrayList(path.size() - 1);
                for (int i = 0; i < copy.size(); i++)
                    copy.add(path.get(i));

                if (_eager == null)
                    _eager = new HashMap();
                FetchInfo info = (FetchInfo) _eager.get(copy);
                if (info == null) {
                    info = new FetchInfo(candidate);
                    _eager.put(copy, info);
                }
                info.eager.set(fm.getIndex());
                info.excludes.clear(fm.getIndex());
            }
        }

        /**
         * Record that the field at the given path is included in the results.
         */
        private void recordIncluded(ClassMapping candidate, List path,
            FieldMapping fm) {
            if (path.isEmpty())
                _fetchInfo.excludes.clear(fm.getIndex());
            else {
                if (_eager == null)
                    _eager = new HashMap();
                FetchInfo info = (FetchInfo) _eager.get(path);
                if (info == null) {
                    info = new FetchInfo(candidate);
                    _eager.put(new ArrayList(path), info);
                }
                info.excludes.clear(fm.getIndex());
            }
        }
    }

    /**
     * Fetch information.
     */
    private static class FetchInfo {

        /**
         * Indexes of fields to exclude from loading.
         */
        public final BitSet excludes;

        /**
         * Indexes of eager fields.
         */
        public final BitSet eager;

        public FetchInfo(ClassMapping type) {
            FieldMapping[] fms = type.getFieldMappings();
            eager = new BitSet(fms.length);
            excludes = new BitSet(fms.length);
            for (int i = 0; i < fms.length; i++)
                if (!fms[i].isPrimaryKey())
                    excludes.set(i);
        }
    }

    /**
     * Mapping of columns to result ids.
     */
    private static interface ColumnMap {

        /**
         * Return the result id for the given column, or the given colum
         * if none.
         */
        public Object map(Column col);
    }

    /**
     * {@link ColumnMap} specialized for a single column.
     */
    private static class SingleColumnMap
        implements ColumnMap {

        private final Object _id;

        public SingleColumnMap(Object id) {
            _id = id;
        }

        public Object map(Column col) {
            return _id;
        }

        public String toString() {
            return _id.toString();
        }
    }

    /**
     * {@link ColumnMap} specialized for a multiple columns.
     * Maps columns in linear time.
     */
    private static class MultiColumnMap
        implements ColumnMap {

        private final List _cols;
        private final Object[] _ids;

        public MultiColumnMap(Column[] cols) {
            _cols = Arrays.asList(cols);
            _ids = new Object[cols.length];
        }

        public Object map(Column col) {
            int idx = _cols.indexOf(col);
            return (idx == -1) ? col : _ids[idx];
        }

        public void set(Column col, Object id) {
            int idx = _cols.indexOf(col);
            if (idx != -1)
                _ids[idx] = id;
        }

        public String toString() {
            return _cols + "=" + Arrays.asList(_ids);
        }
    }
}
