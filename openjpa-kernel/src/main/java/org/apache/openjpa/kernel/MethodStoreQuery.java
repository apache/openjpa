/*
 * Copyright 2006 The Apache Software Foundation.
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.kernel;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.LinkedMap;
import org.apache.openjpa.lib.rop.ListResultObjectProvider;
import org.apache.openjpa.lib.rop.RangeResultObjectProvider;
import org.apache.openjpa.lib.rop.ResultObjectProvider;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.util.Exceptions;
import org.apache.openjpa.util.ImplHelper;
import org.apache.openjpa.util.OpenJPAException;
import org.apache.openjpa.util.UserException;

/**
 * A query that is executed by a user-defined method.
 *
 * @author Abe White
 * @nojavadoc
 */
public class MethodStoreQuery extends AbstractStoreQuery {

    public static final String LANGUAGE = QueryLanguages.LANG_METHODQL;
    private static final Class[] ARGS_DATASTORE = new Class[]{
        StoreContext.class, ClassMetaData.class, boolean.class, Map.class,
        FetchConfiguration.class };
    private static final Class[] ARGS_INMEM = new Class[]{
        StoreContext.class, ClassMetaData.class, boolean.class,
        Object.class, Map.class, FetchConfiguration.class };
    private static final int OBJ_INDEX = 3;
    private static final Localizer _loc = Localizer.forPackage
        (MethodStoreQuery.class);
    private LinkedMap _params = null;

    public void invalidateCompilation() {
        if (_params != null)
            _params.clear();
    }

    public Executor newInMemoryExecutor(ClassMetaData meta, boolean subs) {
        return new MethodExecutor(this, meta, subs, true);
    }

    public Executor newDataStoreExecutor(ClassMetaData meta, boolean subs) {
        return new MethodExecutor(this, meta, subs, false);
    }

    public boolean supportsInMemoryExecution() {
        return true;
    }

    public boolean supportsDataStoreExecution() {
        return true;
    }

    /**
     * Parse the parameter declarations.
     */
    private LinkedMap bindParameterTypes() {
        ctx.lock();
        try {
            if (_params != null)
                return _params;
            String params = ctx.getParameterDeclaration();
            if (params == null)
                return EMPTY_PARAMS;
            List decs = Filters.parseDeclaration(params, ',', "parameters");
            if (_params == null)
                _params = new LinkedMap((int) (decs.size() / 2 * 1.33 + 1));
            String name;
            Class cls;
            for (int i = 0; i < decs.size(); i += 2) {
                name = (String) decs.get(i);
                cls = ctx.classForName(name, null);
                if (cls == null)
                    throw new UserException(_loc.get("bad-param-type", name));
                _params.put(decs.get(i + 1), cls);
            }
            return _params;
        }
        finally {
            ctx.unlock();
        }
    }

    /**
     * Uses a user-defined method named by the filter string to execute the
     * query.
     */
    private static class MethodExecutor extends AbstractExecutor
        implements Executor {

        private final ClassMetaData _meta;
        private final boolean _subs;
        private final boolean _inMem;
        private Method _meth = null;

        public MethodExecutor(MethodStoreQuery q, ClassMetaData candidate,
            boolean subclasses, boolean inMem) {
            _meta = candidate;
            _subs = subclasses;
            _inMem = inMem;
        }

        public ResultObjectProvider executeQuery(StoreQuery q,
            Object[] params, boolean lrs, long startIdx, long endIdx) {
            // convert the parameters into a map
            Map paramMap;
            if (params.length == 0)
                paramMap = Collections.EMPTY_MAP;
            else {
                Map paramTypes = q.getContext().getParameterTypes();
                paramMap = new HashMap((int) (params.length * 1.33 + 1));
                int idx = 0;
                for (Iterator itr = paramTypes.keySet().iterator();
                    itr.hasNext(); idx++)
                    paramMap.put(itr.next(), params[idx]);
            }
            return executeQuery(q, paramMap, lrs, startIdx, endIdx);
        }

        public ResultObjectProvider executeQuery(StoreQuery q,
            Map params, boolean lrs, long startIdx, long endIdx) {
            FetchConfiguration fetch = q.getContext().getFetchConfiguration();
            StoreContext sctx = q.getContext().getStoreContext();
            ResultObjectProvider rop;
            Object[] args;
            if (_inMem) {
                args = new Object[]{ sctx, _meta, (_subs) ? Boolean.TRUE
                    : Boolean.FALSE, null, params, fetch };
                Iterator itr = null;
                Collection coll = q.getContext().getCandidateCollection();
                if (coll == null) {
                    Extent ext = q.getContext().getQuery().
                        getCandidateExtent();
                    itr = ext.iterator();
                } else itr = coll.iterator();
                List results = new ArrayList();
                try {
                    Object obj;
                    while (itr.hasNext()) {
                        obj = itr.next();
                        if (obj == null
                            || !_meta.getDescribedType().isInstance(obj))
                            continue;
                        args[OBJ_INDEX] = obj;
                        if (((Boolean) invoke(q, args)).booleanValue())
                            results.add(obj);
                    }
                }
                finally {
                    ImplHelper.close(itr);
                }
                rop = new ListResultObjectProvider(results);
            } else {
                // datastore
                args = new Object[]{ sctx, _meta, (_subs) ? Boolean.TRUE
                    : Boolean.FALSE, params, fetch };
                rop = (ResultObjectProvider) invoke(q, args);
            }
            if (startIdx != 0 || endIdx != Long.MAX_VALUE)
                rop = new RangeResultObjectProvider(rop, startIdx, endIdx);
            return rop;
        }

        /**
         * Invoke the internal method with the given arguments, returning the
         * result.
         */
        private Object invoke(StoreQuery q, Object[] args) {
            Method meth = _meth;
            if (meth == null) {
                String methName = q.getContext().getQueryString();
                if (methName == null || methName.length() == 0)
                    throw new UserException(_loc.get("no-method"));
                int dotIdx = methName.lastIndexOf('.');
                Class cls;
                if (dotIdx == -1)
                    cls = _meta.getDescribedType();
                else {
                    cls = q.getContext().classForName(methName.substring(0,
                        dotIdx), null);
                    if (cls == null)
                        throw new UserException(_loc.get("bad-method-class",
                            methName.substring(0, dotIdx), methName));
                    methName = methName.substring(dotIdx + 1);
                }
                Class[] types = (_inMem) ? ARGS_INMEM : ARGS_DATASTORE;
                try {
                    meth = cls.getMethod(methName, types);
                } catch (Exception e) {
                    String msg = (_inMem) ? "bad-inmem-method"
                        : "bad-datastore-method";
                    throw new UserException(_loc.get(msg, methName, cls));
                }
                if (!Modifier.isStatic(meth.getModifiers()))
                    throw new UserException(_loc.get("method-not-static",
                        meth));
                _meth = meth;
            }
            try {
                return meth.invoke(null, args);
            } catch (OpenJPAException ke) {
                throw ke;
            } catch (Exception e) {
                throw new UserException(_loc.get("method-error", _meth,
                    Exceptions.toString(Arrays.asList(args))), e);
            }
        }

        public LinkedMap getParameterTypes(StoreQuery q) {
            return ((MethodStoreQuery) q).bindParameterTypes();
        }
    }
}
