package org.apache.openjpa.slice.jdbc;

import org.apache.openjpa.kernel.StoreQuery;
import org.apache.openjpa.kernel.exps.QueryExpressions;
import org.apache.openjpa.kernel.exps.Value;
import org.apache.openjpa.lib.rop.ResultObjectProvider;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.util.InternalException;
import org.apache.openjpa.util.UserException;

/**
 * Aggregates individual single query results from different databases.
 * 
 * @author Pinaki Poddar 
 *
 */
public class UniqueResultObjectProvider implements ResultObjectProvider {
    private final ResultObjectProvider[] _rops;
    private final StoreQuery _query;
    private final QueryExpressions[] _exps;
    private Object _single;
    private boolean _opened;
    
    private static final String COUNT = "Count";
    private static final String MAX   = "Max";
    private static final String MIN   = "Min";
    private static final String SUM   = "Sum";
    
    private static final Localizer _loc =
        Localizer.forPackage(UniqueResultObjectProvider.class);
    
    public UniqueResultObjectProvider(ResultObjectProvider[] rops, 
            StoreQuery q, QueryExpressions[] exps) {
        _rops = rops;
        _query = q;
        _exps = exps;
    }
    
    public boolean absolute(int pos) throws Exception {
        return false;
    }

    public void close() throws Exception {
        _opened = false;
        for (ResultObjectProvider rop:_rops)
            rop.close();
    }

    public Object getResultObject() throws Exception {
        if (!_opened)
            throw new InternalException(_loc.get("not-open"));
        return _single;
    }

    public void handleCheckedException(Exception e) {
        _rops[0].handleCheckedException(e);
    }

    public boolean next() throws Exception {
        if (!_opened) {
            open();
        }
            
        if (_single != null)
            return false;
        
        Value[] values = _exps[0].projections;
        Object[] single = new Object[values.length]; 
        for (int i=0; i<values.length; i++) {
            Value v = values[i];
            boolean isAggregate = v.isAggregate();
            String op = v.getClass().getSimpleName();
            for (ResultObjectProvider rop:_rops) {
                rop.next();
                Object[] row = (Object[]) rop.getResultObject();
                if (isAggregate) {
                    if (COUNT.equals(op)) {
                        single[i] = count(single[i], row[i]);
                    } else if (MAX.equals(op)) {
                        single[i] = max(single[i], row[i]);
                    } else if (MIN.equals(op)) {
                        single[i] = min(single[i], row[i]);
                    } else if (SUM.equals(op)) {
                        single[i] = sum(single[i], row[i]);
                    } else {
                        throw new UnsupportedOperationException
                            (_loc.get("aggregate-unsupported", op).toString());
                    }
                } else {
                    single[i] = row[i];
                }
            }
        }
        _single = single;
        return true;
    }
    
    Object count(Object current, Object other) {
        if (current == null)
            return other;
        return ((Number)current).longValue() + ((Number)other).longValue();
    }
    
    Object max(Object current, Object other) {
        if (current == null)
            return other;
        
        return Math.max(((Number)current).doubleValue(), 
                ((Number)other).doubleValue());
    }
    
    Object min(Object current, Object other) {
        if (current == null)
            return other;
        return Math.min(((Number)current).doubleValue(), 
                ((Number)other).doubleValue());
    }
    
    Object sum(Object current, Object other) {
        if (current == null)
            return other;
        return (((Number)current).doubleValue() +
                ((Number)other).doubleValue());
    }



    public void open() throws Exception {
        for (ResultObjectProvider rop:_rops)
            rop.open();
        _opened = true;
    }

    public void reset() throws Exception {
        _single = null;
        for (ResultObjectProvider rop : _rops) {
            rop.reset();
        }
    }

    public int size() throws Exception {
        return 1;
    }

    public boolean supportsRandomAccess() {
         return false;
    }
}
