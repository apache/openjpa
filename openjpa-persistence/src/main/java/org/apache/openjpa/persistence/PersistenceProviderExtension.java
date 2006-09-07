package org.apache.openjpa.persistence;

import org.apache.openjpa.kernel.BrokerFactory;

/**
 * Allows the Persistence Provider to be supply their own Configuration 
 * Provider and EntityManagerFactory.
 * 
 * @author Pinaki Poddar
 * @since 0.4.1
 * @nojavadoc
 */
public interface PersistenceProviderExtension {

	/**
	 * Gets the Configuration Provider implementation for this receiver.
	 */
	public ConfigurationProviderImpl newConfigurationProviderImpl();
	
	/**
	 * Gets the EntityManagerFactory given a Broker Factory.
	 */
	public OpenJPAEntityManagerFactory toEntityManagerFactory(BrokerFactory 
		factory);
}
