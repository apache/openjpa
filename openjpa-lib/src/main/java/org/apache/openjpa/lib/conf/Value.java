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
package org.apache.openjpa.lib.conf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.lib.util.ParseException;

/**
 * A value of a user-configurable property.
 * The runtime {@link Configuration configuration} is essentially comprised of a set of values.
 * <br>
 * Each Value is identified by a property key. A value can also have other {@link #getEquivalentKeys() equivalent 
 * keys}. The user can configure using either the primary or the equivalent keys. For example, the user can configure
 * the JDBC driver either by <tt>openjpa.ConnectionDriverName</tt> or <tt>javax.persistence.jdbc.driver</tt>.
 * <br>
 * A Value can be aliased. For example, a value of a fully qualified class name 
 * <tt>org.apache.openjpa.ee.ManagedRuntime</tt> can be aliased simply as <tt>managed</tt>.
 * Some values can have a {@link #isAliasListComprehensive() fixed} set of aliases such as values that represents
 * an enumeration. 
 * <br>
 * A Value may have a {@link #getDefault() default} value. Setting a default value does not imply that the vale is 
 * set to the default value. The value is explicitly set by {@link #setObject(Object) set} method. 
 * <br>
 * The values are often printed on console etc. hence the values that carry sensitive information such as 
 * password can be {@link #hide() hidden}.
 * <br>
 * Most values get frozen after an OpenJPA runtime is initialized. However, a value can be modified at runtime
 * only if it is declared as {@link #setDynamic(boolean) dynamic}. Dynamic change of a value is significant because
 * the identity of a OpenJPA runtime is hash code of {@link Configuration configuration} which, in turn, is hash code
 * of its values. This identity is critical to locate a OpenJPA runtime. The dynamic values remember their original 
 * value and that original value is used its hash code computation.
 * <br>
 * Listeners can be {@link #addListener(ValueListener) added} to a value to be notified of changes.
 * <p>
 * Value is made generic since version 2.2.0. The generic type implies the type of value e.g. Value&ltInteger&gt; 
 * holds integer value. 
 *
 * @author Marc Prud'hommeaux
 * @author Pinaki Poddar
 */
public abstract class Value<T> implements Cloneable {

    private static final String[] EMPTY_ALIASES  = new String[0];
    private static final Set<String> EMPTY_SET = Collections.emptySet();
    
    public static final String INVISIBLE = "******";
    private static final Localizer s_loc = Localizer.forPackage(Value.class);
    
    private final String _prop;
    private final Class<T> _type;
    private String loadKey;
    private String def;
    private String[] aliases;
    private String getter;
    private List<ValueListener> listeners;
    private boolean aliasListComprehensive;
    private Class<?> scope;
    private boolean isDynamic;
    private String originalValue;
    private Set<String> otherNames;
    private boolean _hidden;
    private boolean _private;
    
    /**
     * Constructor. Supply the property name.
     *
     * @see #setProperty
     */
    public Value(Class<T> type, String prop) {
    	_type = type;
        _prop  = prop;
    }

    /**
     * The property name that will be used when setting or
     * getting this value in a {@link Map}.
     */
    public String getProperty() {
        return _prop;
    }

    /**
     * Adds a moniker that is equivalent to the original property key used
     * during construction. 
     * 
     * @since 2.0.0
     */
    public void addEquivalentKey(String other) {
        if (otherNames == null)
            otherNames = new HashSet<String>();
        otherNames.add(other);
    }
    
    /**
     * Gets the unmodifiable view of the equivalent keys or an empty set if
     * no equivalent key has been added. 
     * 
     * @since 2.0.0
     */
    public Set<String> getEquivalentKeys() {
        return otherNames == null ? EMPTY_SET : Collections.unmodifiableSet(otherNames);
    }
    
    /**
     * Gets unmodifiable view of all the property keys set on this receiver.  
     * The 0-th element in the returned list is always the same as the original 
     * key returned by {@link #getProperty()} method. 
     * 
     * @since 2.0.0
     */
    public List<String> getPropertyKeys() {
        List<String> result = new ArrayList<String>(1 + (otherNames ==null ? 0 : otherNames.size()));
        result.add(getProperty());
        if (otherNames != null)
            result.addAll(otherNames);
        return Collections.unmodifiableList(result);
    }
    
    /**
     * Affirms if the given key matches the property (or any of its equivalent).
     * 
     * @since 2.0.0
     */
    public boolean matches(String p) {
        return getProperty().equals(p) || 
          (otherNames != null && otherNames.contains(p));
    }
    
    /**
     * The key under which this value was loaded, or null.
     */
    public String getLoadKey() {
        return loadKey;
    }

    /**
     * Sets key under which this value was loaded. 
     * @exception if called with a non-null key which is different from an
     * already loaded key. 
     */
    public void setLoadKey(String key) {
        if (loadKey != null && key != null && !loadKey.equals(key)) 
            throw new ParseException(s_loc.get("multiple-load-key", loadKey, key));
        loadKey = key;
    }

    /**
     * Aliases for the value in the form key1, value1, key2, value2, ...
     * All alias values must be in string form.
     */
    public String[] getAliases() {
        return (aliases == null) ? EMPTY_ALIASES : aliases;
    }

    /**
     * Aliases for the value in the form key1, value1, key2, value2, ...
     * All alias values must be in string form.
     * <p>
     * To avoid potential side-effects, this method copies the array passed in.
     */
    public void setAliases(String[] aliases) {
        String [] aStrings = new String[aliases.length];
        System.arraycopy(aliases, 0, aStrings, 0, aStrings.length);
        this.aliases = aStrings;
    }

    /**
     * Replaces an existing alias, or adds the given alias to the front of the
     * alias list if it does not already exist. All alias values must be in
     * string form.
     */
    public void setAlias(String key, String value) {
        aliases = setAlias(key, value, aliases);
    }
    
    /**
     * Set an alias into a current alias list, returning the new list.
     */
    protected String[] setAlias(String key, String value, String[] aliases) {
        if (aliases == null)
            aliases = EMPTY_ALIASES;
        for (int i = 0; i < aliases.length; i += 2) {
            if (key.equals(aliases[i])) {
                aliases[i + 1] = value;
                return aliases;
            }
        }

        // add as new alias
        String[] newAliases = new String[aliases.length + 2];
        System.arraycopy(aliases, 0, newAliases, 2, aliases.length);
        newAliases[0] = key;
        newAliases[1] = value;
        return newAliases;
    }

    /**
     * Whether or not the alias list defines all possible settings for this
     * value. If so, an error will be generated when attempting to invoke
     * any method on this value with an unknown option.
     */
    public final boolean isAliasListComprehensive() {
        return aliasListComprehensive;
    }

    /**
     * Whether or not the alias list defines all possible settings for this
     * value. If so, an error will be generated when attempting to invoke
     * any method on this value with an unknown option.
     */
    public final void setAliasListComprehensive(boolean aliasListIsComprehensive) {
        this.aliasListComprehensive = aliasListIsComprehensive;
    }

    /**
     * Alias the given setting.
     */
    public String alias(String str) {
        return alias(str, aliases, false);
    }

    /**
     * Alias the given setting.
     */
    protected String alias(String str, String[] aliases, boolean nullNotFound) {
        if (str != null)
            str = str.trim();
        if (aliases == null || aliases.length == 0)
            return (nullNotFound) ? null : str;

        boolean empty = str != null && str.length() == 0;
        for (int i = 1; i < aliases.length; i += 2)
            if (StringUtils.equals(str, aliases[i])
                || (empty && aliases[i] == null))
                return aliases[i - 1];
        return (nullNotFound) ? null : str;
    }

    /**
     * Unalias the given setting.
     */
    public String unalias(String str) {
        return unalias(str, aliases, false);
    }

    /**
     * Unalias the given setting.
     */
    protected String unalias(String str, String[] aliases,
        boolean nullNotFound) {
        if (str != null)
            str = str.trim();

        boolean empty = str != null && str.length() == 0;
        if (str == null || (empty && def != null))
            str = def;
        if (aliases != null)
            for (int i = 0; i < aliases.length; i += 2)
                if (StringUtils.equals(str, aliases[i])
                    || StringUtils.equals(str, aliases[i + 1])
                    || (empty && aliases[i] == null))
                    return aliases[i + 1];

        if (isAliasListComprehensive() && aliases != null)
            throw new ParseException(s_loc.get("invalid-enumerated-config",
                getProperty(), str, Arrays.asList(aliases)));

        return (nullNotFound) ? null : str;
    }

    /**
     * The default value for the property as a string.
     */
    public String getDefault() {
        return def;
    }

    /**
     * The default value for the property as a string.
     */
    public void setDefault(String def) {
        this.def = def;
    }

    /**
     * The name of the getter method for the instantiated value of this
     * property (as opposed to the string value)
     */
    public String getInstantiatingGetter() {
        return getter;
    }

    /**
     * The name of the getter method for the instantiated value of this
     * property(as opposed to the string value). If the string starts with
     * <code>this.</code>, then the getter will be looked up on the value
     * instance itself. Otherwise, the getter will be looked up on the
     * configuration instance.
     */
    public void setInstantiatingGetter(String getter) {
        this.getter = getter;
    }

    /**
     * A class defining the scope in which this value is defined. This will
     * be used by the configuration framework to look up metadata about
     * the value.
     */
    public Class<?> getScope() {
        return scope;
    }

    /**
     * A class defining the scope in which this value is defined. This will
     * be used by the configuration framework to look up metadata about
     * the value.
     */
    public void setScope(Class<?> cls) {
        scope = cls;
    }

    /**
     * Return a stringified version of this value. If the current value has
     * a short alias key, the alias key is returned.
     */
    public String getString() {
        return alias(getInternalString());
    }

    /**
     * Set this value from the given string. If the given string is null or
     * empty and a default is defined, the default is used. If the given
     * string (or default) is an alias key, it will be converted to the
     * corresponding value internally.
     * <br>
     * If this Value is being set to a non-default value for the first time
     * (as designated by <code>originalString</code> being null), then the
     * value is remembered as <em>original</em>. This original value is used
     * for equality and hashCode computation if this Value is
     * {@link #isDynamic() dynamic}. 
     *
     */
    public void setString(String val) {
    	assertChangeable();
        String str = unalias(val);
        try {
            setInternalString(str);
            if (originalValue == null && val != null && !isDefault(val)) {
            	originalValue = getString();
            }
        } catch (ParseException pe) {
            throw pe;
        } catch (RuntimeException re) {
            throw new ParseException(_prop + ": " + val, re);
        }
    }

    /**
     * Set this value as an object.
     * <br>
     * If this Value is being set to a non-default value for the first time
     * (as designated by <code>originalString</code> being null), then the
     * value is remembered as <em>original</em>. This original value is used
     * for equality and hashCode computation if this Value is
     * {@link #isDynamic() dynamic}. 
     * 
     */
    public void setObject(T obj) {
        // if setting to null set as string to get defaults into play
        if (obj == null && def != null) {
            setString(null);
        } else {
            try {
                setInternalObject(obj);
                if (originalValue == null && obj != null && !isDefault(obj)) {
                	originalValue = getString();
                }
            } catch (ParseException pe) {
                throw pe;
            } catch (RuntimeException re) {
                throw new ParseException(_prop + ": " + obj, re);
            }
        }
    }
    
    /**
     * Gets the original value. Original value denotes the Stringified form of 
     * this Value, from which it has been set, if ever. If this Value has never 
     * been set to a non-default value, then returns the default value, which 
     * itself can be null. 
     * 
     * @since 1.1.0
     */
    public String getOriginalValue() {
    	return (originalValue == null) ? getDefault() : originalValue;
    }
    
    boolean isDefault(Object val) {
    	return val != null && val.toString().equals(getDefault());
    }

    /**
     * Returns the type of the property that this Value represents.
     */
    public final Class<T> getValueType() {
    	return _type;
    }

    /**
     * Gets unmodifiable list of listeners for value changes.
     */
    public List<ValueListener> getListeners() {
        return Collections.unmodifiableList(this.listeners);
    }

    /**
     * Listener for value changes.
     */
    public void addListener(ValueListener listener) {
    	if (listener == null)
    		return;
    	if (listeners == null)
    		listeners = new ArrayList<ValueListener>();
        listeners.add(listener);
    }
    
    public void removeListener(ValueListener listener) {
    	if (listener == null)
    		return;
        listeners.remove(listener);
    }

    /**
     * Subclasses should call this method when their internal value changes.
     */
    public void valueChanged() {
        if (listeners == null) 
        	return;
        for (ValueListener listener : listeners) {
        	listener.valueChanged(this);
        }
    }
    
    /**
     * Asserts if this receiver can be changed.
     * Subclasses <em>must</em> invoke this method before changing its
     * internal state.
     * 
     * This receiver can not be changed if all of the following is true
     * <LI>this receiver is not dynamic
     * <LI>ValueListener attached to this receiver is a Configuration
     * <LI>Configuration is read-only
     */
    protected void assertChangeable() {
    	if (!isDynamic() && containsReadOnlyConfigurationAsListener()) {
        	throw new RuntimeException(s_loc.get("veto-change",
        		this.getProperty()).toString());
       	}
    }
    
    boolean containsReadOnlyConfigurationAsListener() {
    	if (listeners == null)
    		return false;
    	for (ValueListener listener : listeners) {
    		if (listener instanceof Configuration
    		&& ((Configuration)listener).isReadOnly())
    			return true;
    	}
    	return false;
    }
    
    /**
     * Sets if this receiver can be mutated even when the configuration it 
     * belongs to has been {@link Configuration#isReadOnly() frozen}.
     *  
     * @since 1.1.0
     */
    public final void setDynamic(boolean flag) {
    	isDynamic = flag;
    }
    
    /**
     * Affirms if this receiver can be mutated even when the configuration it 
     * belongs to has been {@link Configuration#isReadOnly() frozen}.
     *  
     * @since 1.1.0
     */
    public final boolean isDynamic() {
    	return isDynamic; 
    }

    /**
     * Use {@link #getOriginalValue() original value} instead of 
     * {@link #getString() current value} because they are one and the same 
     * for non-dynamic Values and ensures that modifying dynamic Values do not
     * impact equality or hashCode contract.   
     */
    public int hashCode() {
        String str = (isDynamic()) ? getOriginalValue() : getString();
        int strHash = (str == null) ? 0 : str.hashCode();
        int propHash = (_prop == null) ? 0 : _prop.hashCode();
        return strHash ^ propHash;
    }

    /**
     * Use {@link #getOriginalValue() original value} instead of 
     * {@link #getString() current value} because they are one and the same 
     * for non-dynamic Values and ensures that modifying dynamic Values do not
     * impact equality or hashCode contract.   
     */
    public boolean equals(Object other) {
        if (other == this)
            return true;
        if (!(other instanceof Value))
            return false;

        Value<T> o = (Value<T>) other;
        String thisStr = (isDynamic()) ? getOriginalValue() : getString();
        String thatStr = (isDynamic()) ? o.getOriginalValue() : o.getString();
        return (isDynamic() == o.isDynamic())
            && StringUtils.equals(_prop, o.getProperty())
            && StringUtils.equals(thisStr, thatStr);
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException cnse) {
            return null;
        }
    }

    /**
     * Affirms if the value for this Value is visible.
     * Certain sensitive value such as password can be made invisible
     * so that it is not returned to the user code.
     */
    public boolean isHidden() {
        return _hidden;
    }

    /**
     * Hides the value of this Value from being output to the caller.
     */
    public final void hide() {
        _hidden = true;
    }
    
    /**
     * Affirms if this Value is used for internal purpose only and not exposed as a supported property.
     * @see Configuration#getPropertyKeys()
     */
    public final boolean isPrivate() {
        return _private;
    }

    /**
     * Marks this Value for internal purpose only.
     */
    public final void makePrivate() {
        _private = true;
    }
    
    /**
     * Gets the external visible form of this value.
     * @return
     */
    public Object getExternal() {
    	return _hidden ? INVISIBLE : get();
    }
    
    public String toString() {
        return getProperty() + "<" + getValueType().getSimpleName() + ">:" + get();
    }
    
    
    /**
     * Get the actual data stored in this value.
     */
    public abstract T get();
    
    /**
     * Return the internal string form of this value.
     */
    protected abstract String getInternalString();

    /**
     * Set this value from the given string.
     */
    protected abstract void setInternalString(String str);

    /**
     * Set this value from an object.
     */
    protected abstract void setInternalObject(Object obj);


}
