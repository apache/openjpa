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
package org.apache.openjpa.lib.conf;

import java.util.*;
import org.apache.commons.lang.*;
import org.apache.openjpa.lib.util.*;

/**
 * A configuration value.
 * 
 * @author Marc Prud'hommeaux
 */
public abstract class Value implements Cloneable {
    private static final String[] EMPTY_ALIASES = new String[0];
    private static final Localizer s_loc = Localizer.forPackage(Value.class);

    private String prop = null;
    private String def = null;
    private String[] aliases = null;
    private String getter = null;
    private ValueListener listen = null;
    private boolean aliasListComprehensive = false;
    private Class scope = null;

    /**
     * Constructor. Supply the property name.
     * 
     * @see #setProperty
     */
    public Value(String prop) {
        setProperty(prop);
    }

    /**
     * Default constructor.
     */
    public Value() {
    }

    /**
     * The property name that will be used when setting or
     * getting this value in a {@link Map}.
     */
    public String getProperty() {
        return prop;
    }

    /**
     * The property name that will be used when setting or
     * getting this value in a {@link Map}.
     */
    public void setProperty(String prop) {
        this.prop = prop;
    }

    /**
     * Aliases for the value in the form key1, value1, key2, value2, ...
     * All alias values must be in string form.
     */
    public String[] getAliases() {
        return(aliases == null) ? EMPTY_ALIASES : aliases;
    }

    /**
     * Aliases for the value in the form key1, value1, key2, value2, ...
     * All alias values must be in string form.
     */
    public void setAliases(String[] aliases) {
        this.aliases = aliases;
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
    public boolean isAliasListComprehensive() {
        return aliasListComprehensive;
    }

    /**
     * Whether or not the alias list defines all possible settings for this
     * value. If so, an error will be generated when attempting to invoke
     * any method on this value with an unknown option.
     */
    public void setAliasListComprehensive(boolean aliasListIsComprehensive) {
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
            return(nullNotFound) ? null : str;

        boolean empty = str != null && str.length() == 0;
        for (int i = 1; i < aliases.length; i += 2)
            if (StringUtils.equals(str, aliases[i])
                || (empty && aliases[i] == null))
                return aliases[i - 1];
        return(nullNotFound) ? null : str;
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
    protected String unalias(String str, String[] aliases, boolean nullNotFound) {
        if (str != null)
            str = str.trim();

        boolean empty = str != null && str.length() == 0;
        if (str == null || (empty && def != null))
            str = def;
        if (aliases != null)
            for (int i = 0; i < aliases.length; i += 2)
                if (StringUtils.equals(str, aliases[i])
                    || StringUtils.equals(str, aliases[i+1])
                    || (empty && aliases[i] == null))
                    return aliases[i + 1];

        if (isAliasListComprehensive() && aliases != null)
            throw new ParseException(s_loc.get("invalid-enumerated-config",
                getProperty(), str, Arrays.asList(aliases)));

        return(nullNotFound) ? null : str;
    }

    /**
     * The default value for the property as a string.
     */
    public String getDefault() {
        return def;
    }

    /**
     * The default value for the propert as a string.
     */
    public void setDefault(String def) {
        this.def = def;
    }

    /**
     * The name of the getter method for the instantiated value of this
     * property(as opposed to the string value)
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
    public Class getScope() {
        return scope;
    }

    /**
     * A class defining the scope in which this value is defined. This will
     * be used by the configuration framework to look up metadata about
     * the value.
     */
    public void setScope(Class cls) {
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
     * string(or default) is an alias key, it will be converted to the
     * corresponding value internally.
     */
    public void setString(String val) {
        String str = unalias(val);
        try {
            setInternalString(str);
        } catch (ParseException pe) {
            throw pe;
        } catch (RuntimeException re) {
            throw new ParseException(prop + ": " + val, re);
        }
    }

    /**
     * Set this value as an object.
     */
    public void setObject(Object obj) {
        // if setting to null set as string to get defaults into play
        if (obj == null && def != null)
            setString(null);
        else {
            try {
                setInternalObject(obj);
            } catch (ParseException pe) {
                throw pe;
            } catch (RuntimeException re) {
                throw new ParseException(prop + ": " + obj, re);
            }
        }
    }

    /**
     * Returns the type of the property that this Value represents.
     */
    public abstract Class getValueType();

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

    /**
     * Listener for value changes.
     */
    public ValueListener getListener() {
        return this.listen;
    }

    /**
     * Listener for value changes.
     */
    public void setListener(ValueListener listen) {
        this.listen = listen;
    }

    /**
     * Subclasses should call this method when their inernal value changes.
     */
    public void valueChanged() {
        if (listen != null)
            listen.valueChanged(this);
    }

    public int hashCode() {
        String str = getString();
        int strHash =  (str == null) ? 0 : str.hashCode();
        int propHash = (prop == null) ? 0 : prop.hashCode();
        return strHash ^ propHash;
    }

    public boolean equals(Object other) {
        if (other == this)
            return true;
        if (!(other instanceof Value))
            return false;

        Value o = (Value)other;
        return StringUtils.equals(prop, o.getProperty())
            && StringUtils.equals(getString(), o.getString());
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException cnse) {
            return null;
        }
    }
}
