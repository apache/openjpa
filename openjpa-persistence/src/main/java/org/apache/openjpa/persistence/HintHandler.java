package org.apache.openjpa.persistence;

import static org.apache.openjpa.kernel.QueryHints.HINT_IGNORE_PREPARED_QUERY;
import static org.apache.openjpa.kernel.QueryHints.HINT_INVALIDATE_PREPARED_QUERY;
import static org.apache.openjpa.kernel.QueryHints.HINT_RESULT_COUNT;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.enhance.Reflection;
import org.apache.openjpa.kernel.FetchConfiguration;
import org.apache.openjpa.kernel.Filters;
import org.apache.openjpa.kernel.exps.AggregateListener;
import org.apache.openjpa.kernel.exps.FilterListener;
import org.apache.openjpa.lib.conf.ProductDerivation;
import org.apache.openjpa.lib.conf.ProductDerivations;
import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.lib.util.StringDistance;


/**
 * Manages query hint keys and handles their values on behalf of a owning
 * {@link QueryImpl}. Uses specific knowledge of hint keys declared in
 * different parts of the system.
 * 
 * This receiver collects hint keys from different parts of the system. The
 * keys are implicitly or explicitly declared by several different mechanics.
 * This receiver sets the values on behalf of a owning {@link QueryImpl}
 * based on the its specific knowledge of these keys.
 * 
 * The hint keys from following sources are collected and handled: 
 * 
 * 1. {@link org.apache.openjpa.kernel.QueryHints} interface declares hint keys
 *    as public static final fields. These fields are collected by reflection.
 *    The values are handled by invoking methods on the owning {@link QueryImpl}
 *    
 * 2. Some hint keys are collected from bean-style property names of {@link 
 *    JDBCFetchPlan} by {@link Reflection#getBeanStylePropertyNames(Class) 
 *    reflection} and prefixed with <code>openjpa.FetchPlan</code>. 
 *    Their values are used to set the corresponding property of {@link 
 *    FetchPlan} via {@link #hintToSetter(FetchPlan, String, Object) reflection}
 *      
 * 3. Currently defined <code>javax.persistence.*</code> hint keys have  
 *    a equivalent counterpart to one of these FetchPlan keys. 
 *    The JPA keys are mapped to equivalent FetchPlan hint keys.
 *    
 * 4. Some keys directly invoke setters or add listeners to the owning 
 *    {@link QueryImpl}. These hint keys are statically declared in 
 *    this receiver itself. 
 *    
 * 5. ProductDerivation may introduce their own query hint keys via {@link 
 *    ProductDerivation#getSupportedQueryHints()}. Their values are set in the 
 *    {@link FetchConfiguration#setHint(String, Object)}
 *     
 *  A hint key is classified into one of the following three categories:
 *  
 *  1. Supported: A key is known to this receiver as collected from different 
 *     parts of the system. The value of a supported key is recorded and 
 *     available via {@link #getHints()} method. 
 *  2. Recognized: A key is not known to this receiver but has a prefix which
 *     is known to this receiver. The value of a recognized key is not recorded 
 *     but its value is available via {@link FetchConfiguration#getHint(String)}
 *  3. Unrecognized: A key is neither supported nor recognized. The value of a 
 *     unrecognized key is neither recorded nor set anywhere.
 *  
 *  If an incompatible value is supplied for a supported key, a non-fatal
 *  {@link ArgumentException} is raised.
 *  
 * @author Pinaki Poddar
 *
 * @since 2.0.0
 * 
 * @nojavadoc
 */
public class HintHandler {
    private final QueryImpl owner;
    private Map<String, Object> _hints;
    private Set<String> _supportedKeys;
    private Set<String> _supportedPrefixes;
    
    static final String PREFIX_JPA = "javax.persistence.";
    static final String PREFIX_FETCHPLAN = "openjpa.FetchPlan.";
    
    // These keys are directly handled in {@link QueryImpl} class.
    // Declaring a public static final String variable in this class will 
    // make it register as a supported hint key
    // if you do not want that then annotate as {@link Reflectable(false)}.
    public static final String HINT_SUBCLASSES = "openjpa.Subclasses";
    public static final String HINT_FILTER_LISTENER = "openjpa.FilterListener";
    public static final String HINT_FILTER_LISTENERS = 
        "openjpa.FilterListeners";
    public static final String HINT_AGGREGATE_LISTENER = 
        "openjpa.AggregateListener";
    public static final String HINT_AGGREGATE_LISTENERS = 
        "openjpa.AggregateListeners";
    
    // JPA Specification 2.0 keys are mapped to equivalent FetchPlan keys
    public static Map<String,String> _jpaKeys = new TreeMap<String, String>();
    static {
        _jpaKeys.put(addPrefix(PREFIX_JPA, "query.timeout"), 
            addPrefix(PREFIX_FETCHPLAN, "QueryTimeout"));
        _jpaKeys.put(addPrefix(PREFIX_JPA, "lock.timeout"), 
            addPrefix(PREFIX_FETCHPLAN, "LockTimeout"));
    }
    
    private static final String DOT = ".";
    private static final String BLANK = "";
    private static final Localizer _loc = Localizer.forPackage(
        HintHandler.class);
    
    HintHandler(QueryImpl impl) {
        owner = impl;
    }
    
    /**
     * Gets all the recorded hint keys and their values.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getHints() {
        return _hints == null ? Collections.EMPTY_MAP 
            : Collections.unmodifiableMap(_hints);
    }
    
    /**
     * Record a key-value pair only only if the given key is supported.
     * 
     * @return FALSE if the key is unrecognized. 
     *         null (i.e. MAY BE) if the key is recognized, but not supported.
     *         TRUE if the key is supported.
     */
    private Boolean record(String hint, Object value) {
        if (hint == null)
            return Boolean.FALSE;
        if (isSupported(hint)) {
            if (_hints == null)
                _hints = new TreeMap<String, Object>();
            _hints.put(hint, value);
            return Boolean.TRUE;
        }
        
        Log log = owner.getDelegate().getBroker().getConfiguration()
            .getLog(OpenJPAConfiguration.LOG_RUNTIME);
        String possible = StringDistance.getClosestLevenshteinDistance(hint, 
            getSupportedHints());
        if (log.isWarnEnabled()) {
            log.warn(_loc.get("bad-query-hint", hint, possible));
        }
        return (isKnownHintPrefix(hint)) ? null : Boolean.FALSE;
    }
    
    /**
     * Gets all the supported hint keys. The set of supported hint keys is
     * statically determined by collecting hint keys from the ProductDerivations
     * and reflecting upon some of the known classes.
     */
    public Set<String> getSupportedHints() {
        if (_supportedKeys == null) {
            _supportedKeys = new TreeSet<String>(new HintKeyComparator());
            _supportedPrefixes = new TreeSet<String>();
            
            _supportedKeys.addAll(Reflection.getFieldValues(
                org.apache.openjpa.kernel.QueryHints.class, 
                Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL, 
                String.class));

            _supportedKeys.addAll(addPrefix(PREFIX_FETCHPLAN, 
                Reflection.getBeanStylePropertyNames(
                    owner.getFetchPlan().getClass())));

            _supportedKeys.addAll(_jpaKeys.keySet());

            _supportedKeys.addAll(Reflection.getFieldValues(
                HintHandler.class, 
                Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL, 
                String.class));

            _supportedKeys.addAll(ProductDerivations.getSupportedQueryHints());    
            
            for (String key : _supportedKeys) {
                _supportedPrefixes.add(getPrefixOf(key));
            }
        }
        return _supportedKeys;
    }
    
    /**
     * Add a hint key to the set of supported hint keys.
     */
    public void addHintKey(String key) {
        getSupportedHints().add(key);
        _supportedPrefixes.add(getPrefixOf(key));
    }
    
    public Set<String> getKnownPrefixes() {
        getSupportedHints();
        return _supportedPrefixes;
    }
    
    /**
     * Affirms the given key matches one of the supported keys.
     */
    private boolean isSupported(String key) {
        return getSupportedHints().contains(key);
    }
    
    /**
     * Affirms the given key has a prefix that matches with any of the 
     * supported prefixes.
     */
    private boolean isSupportedPrefix(String key) {
        return getKnownPrefixes().contains(getPrefixOf(key));
    }
    
    static Set<String> addPrefix(String prefix, Set<String> original) {
        Set<String> result = new TreeSet<String>();
        String join = prefix.endsWith(DOT) ? BLANK : DOT;
        for (String o : original)
            result.add(prefix + join + o);
        return result;
    }
    
    static String addPrefix(String prefix, String original) {
        String join = prefix.endsWith(DOT) ? BLANK : DOT;
        return prefix + join + original;
    }
    
    private static String removePrefix(String key, String prefix) {
        if (prefix == null)
            return key;
        if (!prefix.endsWith(DOT))
            prefix = prefix + DOT;
        if (key != null && key.startsWith(prefix))
            return key.substring(prefix.length());
        return key;
    }
    
    static String getPrefixOf(String key) {
        int index = key == null ? -1 : key.indexOf(DOT);
        return (index != -1) ? key.substring(0,index) : key;
    }
    
    private boolean isKnownHintPrefix(String key) {
        String prefix = getPrefixOf(key);
        return getKnownPrefixes().contains(prefix);
    }
    
    public static boolean hasPrefix(String key, String prefix) {
        if (key == null || prefix == null)
            return false;
        if (!prefix.endsWith(DOT))
            prefix = prefix + DOT;
        return key.startsWith(prefix);
    }
    
    public void setHint(String key, Object value) {
        owner.lock();
        try {
            setHintInternal(key, value);
        } finally {
            owner.unlock();
        }
    }
    
    private void setHintInternal(String key, Object value) {
        Boolean record = record(key, value);
        FetchConfiguration fetch = owner.getDelegate().getFetchConfiguration();
        ClassLoader loader = owner.getDelegate().getBroker().getClassLoader();
        if (record == Boolean.FALSE)
            return;
        if (record == null) {
            fetch.setHint(key, value);
            return;
        }
        try {
            if (HINT_SUBCLASSES.equals(key)) {
                if (value instanceof String)
                    value = Boolean.valueOf((String) value);
                owner.setSubclasses(((Boolean) value).booleanValue());
            } else if (HINT_FILTER_LISTENER.equals(key))
                owner.addFilterListener(Filters.hintToFilterListener(value, 
                    loader));
            else if (HINT_FILTER_LISTENERS.equals(key)) {
                FilterListener[] arr = Filters.hintToFilterListeners(value, 
                    loader);
                for (int i = 0; i < arr.length; i++)
                    owner.addFilterListener(arr[i]);
            } else if (HINT_AGGREGATE_LISTENER.equals(key))
                owner.addAggregateListener(Filters.hintToAggregateListener(
                    value, loader));
            else if (HINT_AGGREGATE_LISTENERS.equals(key)) {
                AggregateListener[] arr = Filters.hintToAggregateListeners(
                        value, loader);
                for (int i = 0; i < arr.length; i++)
                    owner.addAggregateListener(arr[i]);
            } else if (isFetchPlanHint(key)) {
                if (requiresTransaction(key))
                    ((FetchPlanImpl)owner.getFetchPlan()).getDelegate()
                        .setHint(key, value);
                else 
                    hintToSetter(owner.getFetchPlan(), 
                        getFetchPlanProperty(key), value);
            } else if (HINT_RESULT_COUNT.equals(key)) {
                int v = (Integer)Filters.convert(value, Integer.class);
                if (v < 0)
                    throw new ArgumentException(_loc.get("bad-query-hint-value", 
                        key, value), null,  null, false);
                    fetch.setHint(key, v);
            }  else if (HINT_INVALIDATE_PREPARED_QUERY.equals(key)) {
                fetch.setHint(key, Filters.convert(value, Boolean.class));
                owner.invalidatePreparedQuery();
            } else if (HINT_IGNORE_PREPARED_QUERY.equals(key)) {
                fetch.setHint(key, Filters.convert(value, Boolean.class));
                owner.ignorePreparedQuery();
            } else { // default 
                fetch.setHint(key, value);
            }
            return;
        } catch (IllegalArgumentException iae) {
            throw new ArgumentException(_loc.get("bad-query-hint-value", 
                key, value), null,  null, false);
        } catch (ClassCastException ce) {
            throw new ArgumentException(_loc.get("bad-query-hint-value", 
                key, ce.getMessage()), null,  null, false);
        } catch (Exception e) {
            throw PersistenceExceptions.toPersistenceException(e);
        }
    }
    
    private boolean isFetchPlanHint(String key) {
        return key.startsWith(PREFIX_FETCHPLAN) 
           || (_jpaKeys.containsKey(key) && isFetchPlanHint(_jpaKeys.get(key)));
    }
    
    private boolean requiresTransaction(String key) {
        return key.endsWith("LockMode");
    }
    
    private String getFetchPlanProperty(String key) {
        if (key.startsWith(PREFIX_FETCHPLAN))
            return removePrefix(key, PREFIX_FETCHPLAN);
        else if (_jpaKeys.containsKey(key))
            return getFetchPlanProperty(_jpaKeys.get(key));
        else
            return key;
    }
    
    private void hintToSetter(FetchPlan fetchPlan, String k, Object value) {
        if (fetchPlan == null || k == null)
            return;

        Method setter = Reflection.findSetter(fetchPlan.getClass(), k, true);
        Class paramType = setter.getParameterTypes()[0];
        if (Enum.class.isAssignableFrom(paramType) && value instanceof String)
            value = Enum.valueOf(paramType, (String) value);

        Filters.hintToSetter(fetchPlan, k, value);
    }
    
    public static class HintKeyComparator implements Comparator<String> {
        public int compare(String s1, String s2) {
            if (getPrefixOf(s1).equals(getPrefixOf(s2))) {
                int n1 = countDots(s1);
                int n2 = countDots(s2);
                return (n1 == n2) ? s1.compareTo(s2) : (n1 - n2);
            } else
                return s1.compareTo(s2);
        }
        
        public int countDots(String s) {
            if (s == null || s.length() == 0)
                return 0;
            int index = s.indexOf(DOT);
            return (index == -1) ? 0 : countDots(s.substring(index+1)) + 1;
        }
    }
}
