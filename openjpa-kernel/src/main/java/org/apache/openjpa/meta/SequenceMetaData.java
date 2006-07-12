/*
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.meta;

import java.io.File;

import org.apache.commons.lang.StringUtils;
import org.apache.openjpa.conf.SeqValue;
import org.apache.openjpa.kernel.Seq;
import org.apache.openjpa.lib.conf.Configurations;
import org.apache.openjpa.lib.conf.PluginValue;
import org.apache.openjpa.lib.meta.SourceTracker;
import org.apache.openjpa.lib.util.Closeable;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.lib.xml.Commentable;
import org.apache.openjpa.util.MetaDataException;
import org.apache.openjpa.util.OpenJPAException;

/**
 * <p>Metadata about a named sequence.</p>
 *
 * @author Abe White
 * @since 4.0
 */
public class SequenceMetaData
    implements SourceTracker, MetaDataContext, Closeable, Commentable {

    /**
     * Sequence name that means to use the system default sequence.
     */
    public static final String NAME_SYSTEM = "system";

    /**
     * Default plugin alias name; every back end should have some 'native'
     * sequence implementation.
     */
    public static final String IMPL_NATIVE = "native";

    /**
     * Time-based sequence values.
     */
    public static final String IMPL_TIME = "time";

    // plugin property names for standard props
    private static final String PROP_SEQUENCE = "Sequence";
    private static final String PROP_INITIAL_VALUE = "InitialValue";
    private static final String PROP_ALLOCATE = "Allocate";
    private static final String PROP_INCREMENT = "Increment";

    private static final Localizer _loc = Localizer.forPackage
        (SequenceMetaData.class);

    private final MetaDataRepository _repos;
    private final String _name;
    private int _type = Seq.TYPE_DEFAULT;
    private String _plugin = IMPL_NATIVE;
    private SequenceFactory _factory = null;
    private File _source = null;
    private Object _scope = null;
    private int _srcType = SRC_OTHER;
    private String[] _comments = null;
    private String _sequence = null;
    private int _increment = -1;
    private int _allocate = -1;
    private int _initial = -1;

    // instantiated lazily
    private Seq _instance = null;

    /**
     * Constructor; supply sequence name.
     */
    public SequenceMetaData(String name, MetaDataRepository repos) {
        _name = name;
        _repos = repos;
    }

    /**
     * The owning repository.
     */
    public MetaDataRepository getRepository() {
        return _repos;
    }

    /**
     * The sequence name.
     */
    public String getName() {
        return _name;
    }

    public File getSourceFile() {
        return _source;
    }

    public Object getSourceScope() {
        return _scope;
    }

    public int getSourceType() {
        return _srcType;
    }

    public void setSource(File file, Object scope, int srcType) {
        _source = file;
        _scope = scope;
        _srcType = srcType;
    }

    public String getResourceName() {
        return _name;
    }

    /**
     * The sequence type.
     */
    public int getType() {
        return _type;
    }

    /**
     * The sequence type.
     */
    public void setType(int type) {
        _type = type;
    }

    /**
     * Native sequence name.
     */
    public String getSequence() {
        return _sequence;
    }

    /**
     * Native sequence name.
     */
    public void setSequence(String sequence) {
        _sequence = sequence;
    }

    /**
     * Sequence increment, or -1 for default.
     */
    public int getIncrement() {
        return _increment;
    }

    /**
     * Sequence increment, or -1 for default.
     */
    public void setIncrement(int increment) {
        _increment = increment;
    }

    /**
     * Sequence values to allocate, or -1 for default.
     */
    public int getAllocate() {
        return _allocate;
    }

    /**
     * Sequence values to allocate, or -1 for default.
     */
    public void setAllocate(int allocate) {
        _allocate = allocate;
    }

    /**
     * Initial sequence value, or -1 for default.
     */
    public int getInitialValue() {
        return _initial;
    }

    /**
     * Initial sequence value, or -1 for default.
     */
    public void setInitialValue(int initial) {
        _initial = initial;
    }

    /**
     * Plugin string describing the {@link Seq}.
     */
    public String getSequencePlugin() {
        return _plugin;
    }

    /**
     * Plugin string describing the {@link Seq}.
     */
    public void setSequencePlugin(String plugin) {
        _plugin = plugin;
    }

    /**
     * A factory to transform spec sequences produced by user factories into
     * the OpenJPA sequence type.
     */
    public SequenceFactory getSequenceFactory() {
        return _factory;
    }

    /**
     * A factory to transform spec sequences produced by user factories into
     * the OpenJPA sequence type.
     */
    public void setSequenceFactory(SequenceFactory factory) {
        _factory = factory;
    }

    /**
     * Return the initialized sequence instance.
     */
    public synchronized Seq getInstance(ClassLoader envLoader) {
        if (_instance == null)
            _instance = instantiate(envLoader);
        return _instance;
    }

    /**
     * Create a new uninitialized instance of this sequence.
     */
    protected Seq instantiate(ClassLoader envLoader) {
        if (NAME_SYSTEM.equals(_name))
            return _repos.getConfiguration().getSequenceInstance();

        try {
            PluginValue plugin = newPluginValue("sequence-plugin");
            plugin.setString(_plugin);
            String clsName = plugin.getClassName();

            Class cls = Class.forName(clsName, true,
                Seq.class.getClassLoader());
            StringBuffer props = new StringBuffer();
            if (plugin.getProperties() != null)
                props.append(plugin.getProperties());
            addStandardProperties(props);

            // allow user-class specification of either our sequence
            // interface or a factory class
            Seq seq;
            if (Seq.class.isAssignableFrom(cls)) {
                seq = (Seq) cls.newInstance();
                Configurations.configureInstance(seq,
                    _repos.getConfiguration(), props.toString());
                seq.setType(_type);
            } else if (_factory != null)
                seq = _factory.toSequence(cls, props.toString());
            else
                throw new MetaDataException(_loc.get("not-seq-cls", _name,
                    cls));
            return seq;
        }
        catch (OpenJPAException ke) {
            throw ke;
        }
        catch (Exception e) {
            throw new MetaDataException(_loc.get("cant-init-seq", _name)).
                setCause(e);
        }
    }

    /**
     * Create a new plugin value for sequences.  Returns a standard
     * {@link SeqValue} by default.
     */
    protected PluginValue newPluginValue(String property) {
        return new SeqValue(property);
    }

    /**
     * Add standard properties to the given properties buffer.
     */
    protected void addStandardProperties(StringBuffer props) {
        appendProperty(props, PROP_SEQUENCE, _sequence);
        appendProperty(props, PROP_INITIAL_VALUE, _initial);
        appendProperty(props, PROP_ALLOCATE, _allocate);
        appendProperty(props, PROP_INCREMENT, _increment);
    }

    /**
     * Add a string property to the buffer.  Nothing will be added if value
     * is null or empty string.
     */
    protected void appendProperty(StringBuffer props, String name, String val) {
        if (StringUtils.isEmpty(val))
            return;
        if (props.length() > 0)
            props.append(",");
        props.append(name).append("=").append(val);
    }

    /**
     * Add an int property to the buffer.  Nothing will be added if value
     * is -1.
     */
    protected void appendProperty(StringBuffer props, String name, int val) {
        if (val == -1)
            return;
        if (props.length() > 0)
            props.append(",");
        props.append(name).append("=").append(val);
    }

    /**
     * Close user sequence instance.
     */
    public void close() {
        if (_instance != null && !NAME_SYSTEM.equals(_name))
            try {
                _instance.close();
            } catch (Exception e) {
            }
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

    /**
     *	Allow facades to supply adapters from a spec sequence type to the
     *	OpenJPA sequence type.
     */
    public static interface SequenceFactory {

        /**
         *	Transform the given class named in metadata into a sequence.
         */
		public Seq toSequence (Class cls, String props)
			throws Exception;
	}
}
