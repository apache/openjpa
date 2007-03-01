package org.apache.openjpa.conf;

import org.apache.openjpa.lib.conf.PluginValue;
import org.apache.openjpa.lib.conf.Configuration;
import org.apache.openjpa.kernel.BrokerImpl;
import org.apache.openjpa.kernel.FinalizingBrokerImpl;
import org.apache.openjpa.util.InternalException;

/**
 * Custom {@link PluginValue} that can efficiently create {@link BrokerImpl}
 * instances.
 *
 * @since 0.9.7
 */
public class BrokerValue 
    extends PluginValue {

    public static final String KEY = "BrokerImpl";
    public static final String NON_FINALIZING_ALIAS = "non-finalizing";
    public static final String DEFAULT_ALIAS = "default";

    private BrokerImpl _templateBroker;

    public BrokerValue() {
        super(KEY, false);
        String[] aliases = new String[] {
            DEFAULT_ALIAS, FinalizingBrokerImpl.class.getName(),
            NON_FINALIZING_ALIAS, BrokerImpl.class.getName(), 
        };
        setAliases(aliases);
        setDefault(aliases[0]);
        setString(aliases[0]);        
    }

    public Object newInstance(String clsName, Class type, Configuration conf,
        boolean fatal) {
        // This is not synchronized. If there are concurrent invocations
        // while _templateBroker is null, we'll just end up with extra
        // template brokers, which will get safely garbage collected.
        if (_templateBroker == null)
            _templateBroker = (BrokerImpl) super.newInstance(clsName, type,
                conf, fatal);
        try {
            return _templateBroker.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalException(e);
        }
    }
}