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
package org.apache.openjpa.persistence;

import java.util.Collection;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.LockModeType;
import javax.persistence.Query;

import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.ee.ManagedRuntime;
import org.apache.openjpa.kernel.AutoClear;
import org.apache.openjpa.kernel.AutoDetach;
import org.apache.openjpa.kernel.ConnectionRetainModes;
import org.apache.openjpa.kernel.DetachState;
import org.apache.openjpa.kernel.RestoreState;
import org.apache.openjpa.lib.util.Closeable;

/**
 * Interface implemented by OpenJPA entity managers.
 *
 * @since 0.4.0
 * @author Abe White
 * @published
 */
public interface OpenJPAEntityManager
    extends EntityManager, EntityTransaction, javax.resource.cci.Connection,
    javax.resource.cci.LocalTransaction, javax.resource.spi.LocalTransaction,
    Closeable, ConnectionRetainModes, DetachState, RestoreState, AutoDetach,
    AutoClear {

    /**
     * Return the factory that produced this entity manager.
     */
    public OpenJPAEntityManagerFactory getEntityManagerFactory();

    /**
     * Return the configuration associated with this entity manager.
     */
    public OpenJPAConfiguration getConfiguration();

    /**
     * Return the (mutable) fetch plan for loading objects from this
     * entity manager.
     */
    public FetchPlan getFetchPlan();

    /**
     * Return the connection retain mode for this entity manager.
     *
     * @see ConnectionRetainModes
     */
    public int getConnectionRetainMode();

    /**
     * Whether this entity manager is using managed transactions.
     */
    public boolean isManaged();

    /**
     * Return the managed runtime in use.
     */
    public ManagedRuntime getManagedRuntime();

    /**
     * Whether to check for a global transaction upon every managed,
     * non-transactional operation. Defaults to false.
     */
    public boolean getSyncWithManagedTransactions();

    /**
     * Whether to check for a global transaction upon every managed,
     * non-transactional operation. Defaults to false.
     */
    public void setSyncWithManagedTransactions(boolean resync);

    /**
     * Return the current thread's class loader at the time this entity
     * manager was obtained from the factory.
     */
    public ClassLoader getClassLoader();

    /**
     * Return the connection user name.
     */
    public String getConnectionUserName();

    /**
     * Return the connection password.
     */
    public String getConnectionPassword();

    /**
     * Whether the entity manager or its managed instances are used in a
     * multithreaded environment.
     */
    public boolean getMultithreaded();

    /**
     * Whether the entity manager or its managed instances are used in a
     * multithreaded environment.
     */
    public void setMultithreaded(boolean multi);

    /**
     * Whether to take into account changes in the transaction when executing
     * a query or iterating an extent.
     */
    public boolean getIgnoreChanges();

    /**
     * Whether to take into account changes in the transaction when executing
     * a query or iterating an extent.
     */
    public void setIgnoreChanges(boolean ignore);

    /**
     * Whether to allow nontransactional access to persistent state.
     */
    public boolean getNontransactionalRead();

    /**
     * Whether to allow nontransactional access to persistent state.
     */
    public void setNontransactionalRead(boolean read);

    /**
     * Whether to allow nontransactional changes to persistent state.
     */
    public boolean getNontransactionalWrite();

    /**
     * Whether to allow nontransactional changes to persistent state.
     */
    public void setNontransactionalWrite(boolean write);

    /**
     * Whether to use optimistic transactional semantics.
     */
    public boolean getOptimistic();

    /**
     * Whether to use optimistic transactional semantics.
     */
    public void setOptimistic(boolean opt);

    /**
     * Whether to restore an object's original state on rollback.
     *
     * @see RestoreState
     */
    public int getRestoreState();

    /**
     * Whether to restore an object's original state on rollback.
     *
     * @see RestoreState
     */
    public void setRestoreState(int restore);

    /**
     * Whether objects retain their persistent state on transaction commit.
     */
    public boolean getRetainState();

    /**
     * Whether objects retain their persistent state on transaction commit.
     */
    public void setRetainState(boolean retain);

    /**
     * Detach mode constant to determine which fields are part of the
     * detached graph.
     *
     * @see DetachState
     */
    public int getDetachState();

    /**
     * Detach mode constant to determine which fields are part of the
     * detached graph.
     *
     * @see DetachState
     */
    public void setDetachState(int mode);

    /**
     * Whether to clear state when entering a transaction.
     *
     * @see AutoClear
     */
    public int getAutoClear();

    /**
     * Whether to clear state when entering a transaction.
     *
     * @see AutoClear
     */
    public void setAutoClear(int clear);

    /**
     * Bit flags marked in {@link AutoDetach} which indicate when persistent
     * managed objects should be automatically detached in-place.
     */
    public int getAutoDetach();

    /**
     * Bit flags marked in {@link AutoDetach} which indicate when persistent
     * managed objects should be automatically detached in-place.
     */
    public void setAutoDetach(int flags);

    /**
     * Bit flags marked in {@link AutoDetach} which indicate when persistent
     * managed objects should be automatically detached in-place.
     */
    public void setAutoDetach(int flag, boolean on);

    /**
     * Whether to also evict an object from the store cache when it is
     * evicted through this entity manager.
     */
    public boolean getEvictFromStoreCache();

    /**
     * Whether to also evict an object from the store cache when it is
     * evicted through this entity manager.
     */
    public void setEvictFromStoreCache(boolean evict);

    /**
     * Whether objects accessed during this transaction will be added to the
     * store cache. Defaults to true.
     *
     * @since 0.3.4
     */
    public boolean getPopulateStoreCache();

    /**
     * Whether to populate the store cache with objects used by this
     * transaction. Defaults to true.
     *
     * @since 0.3.4
     */
    public void setPopulateStoreCache(boolean cache);

    /**
     * Whether memory usage is reduced during this transaction at the expense
     * of possibly more aggressive data cache evictions.
     *
     * @since 0.3.4
     */
    public boolean isLargeTransaction();

    /**
     * If a large number of objects will be created, modified, or deleted
     * during this transaction setting this option to true will reduce memory
     * usage if you perform periodic flushes.
     *
     * @since 0.3.4
     */
    public void setLargeTransaction(boolean largeTransaction);

    /**
     * Put the specified key-value pair into the map of user objects. Use
     * a value of null to remove the key.
     */
    public Object putUserObject(Object key, Object val);

    /**
     * Get the value for the specified key from the map of user objects.
     */
    public Object getUserObject(Object key);

    //////////
    // Events
    //////////

    /**
     * Register a listener for transaction-related events.
     */
    public void addTransactionListener(Object listener);

    /**
     * Remove a listener for transaction-related events.
     */
    public void removeTransactionListener(Object listener);

    /**
     * Register a listener for lifecycle-related events on the specified
     * classes. If the classes are null, all events will be propagated to
     * the listener.
     */
    public void addLifecycleListener(Object listener, Class... classes);

    /**
     * Remove a listener for lifecycle-related events.
     */
    public void removeLifecycleListener(Object listener);

    ///////////
    // Lookups
    ///////////

    /**
     * Return the objects with the given oids.
     *
     * @param oids the oids of the objects to return
     * @return the objects that were looked up, in the
     * same order as the oids parameter.
     * @see #find(Class,Object)
     */
    public <T> T[] findAll(Class<T> cls, Object... oids);

    /**
     * Return the objects with the given oids.
     *
     * @param oids the oids of the objects to return
     * @return the objects that were looked up, in the
     * same order as the oids parameter.
     * @see #find(Class,Object)
     */
    public <T> Collection<T> findAll(Class<T> cls, Collection oids);

    /**
     * Return the cached instance for the given oid/object, or null if not
     * cached.
     *
     * @param oid the object's id
     * @return the cached object, or null if not cached
     */
    public <T> T findCached(Class<T> cls, Object oid);

    /**
     * Return the application identity class the given persistent class uses
     * for object ids, or null if not a type that uses application identity.
     */
    public Class getObjectIdClass(Class pcClass);

    ////////////////
    // Transactions
    ////////////////

    /**
     * Issue a commit and then start a new transaction. This is identical to:
     * <pre> manager.commit (); manager.begin ();
     * </pre> except that the entity manager's internal atomic lock is utilized,
     * so this method can be safely executed from multiple threads.
     *
     * @see #commit()
     * @see #begin()
     */
    public void commitAndResume();

    /**
     * Issue a rollback and then start a new transaction. This is identical to:
     * <pre> manager.rollback (); manager.begin ();
     * </pre> except that the entity manager's internal atomic lock is utilized,
     * so this method can be safely executed from multiple threads.
     *
     * @see #rollback()
     * @see #begin()
     */
    public void rollbackAndResume();

    /**
     * Mark the current transaction for rollback.
     */
    public void setRollbackOnly();

    /**
     * Return whether the current transaction has been marked for rollback.
     */
    public boolean getRollbackOnly();

    /**
     * Set a transactional savepoint where operations after this savepoint
     * will be rolled back.
     */
    public void setSavepoint(String name);

    /**
     * Rollback the current transaction to the last savepoint.
     * Savepoints set after this one will become invalid.
     */
    public void rollbackToSavepoint();

    /**
     * Rollback the current transaction to the given savepoint name.
     * Savepoints set after this one will become invalid.
     */
    public void rollbackToSavepoint(String name);

    /**
     * Release the last set savepoint and any resources associated with it.
     * The given savepoint and any set after it will become invalid.
     */
    public void releaseSavepoint();

    /**
     * Release the savepoint and any resources associated with it.
     * The given savepoint and any set after it will become invalid.
     */
    public void releaseSavepoint(String name);

    /**
     * Run pre-flush actions on transactional objects, including
     * persistence-by-reachability, inverse relationship management,
     * deletion of dependent instances, and instance callbacks.
     * Transaction listeners are not invoked.
     *
     * @since 0.3.3
     */
    public void preFlush();

    /**
     * Validate the changes made in this transaction, reporting any optimistic
     * violations, constraint violations, etc. In a datastore transaction or
     * a flushed optimistic transaction, this method will act just like
     * {@link #flush()}. In an optimistic transaction that has not yet begun a
     * datastore-level transaction, however, it will only report exceptions
     * that would occur on flush, without retaining any datastore resources.
     */
    public void validateChanges();

    /**
     * Whether a store transaction is active.
     */
    public boolean isStoreActive();

    /**
     * Begins a store transaction if one isn't already started. The
     * entity manager must already be in a logical transaction.
     */
    public void beginStore();

    ////////////////////
    // Object Lifecycle
    ////////////////////

    /**
     * Whether the given objects are managed.
     */
    public boolean containsAll(Object... pcs);

    /**
     * Whether the given objects are managed.
     */
    public boolean containsAll(Collection pcs);

    /**
     * Persist the given objects.
     */
    public void persistAll(Object... pcs);

    /**
     * Persist the given objects.
     */
    public void persistAll(Collection pcs);

    /**
     * Delete the given persistent objects.
     */
    public void removeAll(Object... pcs);

    /**
     * Delete the given persistent objects.
     */
    public void removeAll(Collection pcs);

    /**
     * Release the given object from management. This operation is not
     * recursive.
     */
    public void release(Object pc);

    /**
     * Release the given object from management. This operation is not
     * recursive.
     */
    public void releaseAll(Object... pcs);

    /**
     * Release the given objects from management. This operation is not
     * recursive.
     */
    public void releaseAll(Collection pcs);

    /**
     * Immediately load the given object's persistent fields. One might
     * use this action to make sure that an instance's fields are loaded
     * before transitioning it to transient. Note that this action is not
     * recursive. Any related objects that are loaded will not necessarily
     * have their fields loaded.
     */
    public void retrieve(Object pc);

    /**
     * Retrieve the persistent state of the given objects.
     *
     * @see #retrieve
     */
    public void retrieveAll(Object... pcs);

    /**
     * Retrieve the persistent state of the given objects.
     *
     * @see #retrieve
     */
    public void retrieveAll(Collection pcs);

    /**
     * Refresh the state of the given objects.
     */
    public void refreshAll(Object... pcs);

    /**
     * Refresh the state of the given objects.
     */
    public void refreshAll(Collection pcs);

    /**
     * Refresh all transactional objects.
     */
    public void refreshAll();

    /**
     * Evict the given object.
     */
    public void evict(Object pc);

    /**
     * Evict the given objects.
     */
    public void evictAll(Object... pcs);

    /**
     * Evict the given objects.
     */
    public void evictAll(Collection pcs);

    /**
     * Evict all clean objects.
     */
    public void evictAll();

    /**
     * Evict all persistent-clean and persistent-nontransactional
     * instances in the extent of the given class (including subclasses).
     */
    public void evictAll(Class cls);

    /**
     * Evict all persistent-clean and persistent-nontransactional
     * instances in the given {@link Extent}.
     */
    public void evictAll(Extent extent);

    /**
     * Detach the specified object from the entity manager.
     *
     * @param pc the instance to detach
     * @return the detached instance
     */
    public <T> T detach(T pc);

    /**
     * Detach the specified objects from the entity manager.
     *
     * @param pcs the instances to detach
     * @return the detached instances
     */
    public Collection detachAll(Collection pcs);

    /**
     * Detach the specified objects from the entity manager.
     *
     * @param pcs the instances to detach
     * @return the detached instances
     */
    public Object[] detachAll(Object... pcs);

    /**
     * Merge the specified objects into the entity manager.
     *
     * @param pcs instances to import
     * @return the re-attached instances
     */
    public Object[] mergeAll(Object... pcs);

    /**
     * Merge the specified detached objects into the entity manager.
     *
     * @param pcs Collection of instances to import
     * @return the re-attached instances
     */
    public Collection mergeAll(Collection pcs);

    /**
     * Make the given object transactional.
     *
     * @param pc instance to make transactional
     * @param updateVersion if true, the instance's version will be
     * incremented at the next flush
     */
    public void transactional(Object pc, boolean updateVersion);

    /**
     * Make the given objects transactional.
     *
     * @param objs instances to make transactional
     * @param updateVersion if true, the instance's version will be
     * incremented at the next flush
     */
    public void transactionalAll(Collection objs, boolean updateVersion);

    /**
     * Make the given objects transactional.
     *
     * @param objs instances to make transactional
     * @param updateVersion if true, the instance's version will be
     * incremented at the next flush
     */
    public void transactionalAll(Object[] objs, boolean updateVersion);

    /**
     * Make the given object nontransactional.
     */
    public void nontransactional(Object pc);

    /**
     * Make the given objects nontransactional.
     */
    public void nontransactionalAll(Collection objs);

    /**
     * Make the given objects nontransactional.
     */
    public void nontransactionalAll(Object[] objs);

    ////////////////////////////
    // Extent, Query, Generator
    ////////////////////////////

    /**
     * Return the named generator defined in the metadata.
     */
    public Generator getNamedGenerator(String name);

    /**
     * Returns a {@link Generator} for the datastore identity values of the
     * specified type, or null if the type is unmanaged or its identity
     * cannot be represented by a sequence.
     */
    public Generator getIdGenerator(Class forClass);

    /**
     * Returns a {@link Generator} for the generated values of the specified
     * type, or null if the field is not generated.
     */
    public Generator getFieldGenerator(Class forClass, String fieldName);

    /**
     * Return an extent of the given class, optionally including subclasses.
     */
    public <T> Extent<T> createExtent(Class<T> cls, boolean subs);

    public OpenJPAQuery createQuery(String query);

    public OpenJPAQuery createNamedQuery(String name);

    public OpenJPAQuery createNativeQuery(String sql);

    public OpenJPAQuery createNativeQuery(String sql, Class resultClass);

    public OpenJPAQuery createNativeQuery(String sql, String resultMapping);

    /**
     * Create a new query from the given one.
     */
    public OpenJPAQuery createQuery(Query query);

    /**
     * Create a new query in the given language.
     */
    public OpenJPAQuery createQuery(String language, String query);

    ///////////
    // Locking
    ///////////

    /**
     * Return the lock mode of the given instance, or null if not locked.
     */
    public LockModeType getLockMode(Object pc);

    /**
     * Ensure that the given instance is locked at the given lock level.
     *
     * @param pc the object to lock
     * @param mode the lock level to use
     * @param timeout the number of milliseconds to wait for the lock before
     * giving up, or -1 for no limit
     */
    public void lock(Object pc, LockModeType mode, int timeout);

    /**
     * Ensure that the given instance is locked at the current lock level, as
     * set in the {@link FetchPlan} for the entity manager.
     */
    public void lock(Object pc);

    /**
     * Ensure that the given instances are locked at the given lock level.
     *
     * @param pcs the objects to lock
     * @param mode the lock level to use
     * @param timeout the number of milliseconds to wait for the lock before
     * giving up, or -1 for no limit
     */
    public void lockAll(Collection pcs, LockModeType mode, int timeout);

    /**
     * Ensure that the given instances are locked at the current lock level,
     * as set in the {@link FetchPlan} for the entity manager.
     */
    public void lockAll(Collection pcs);

    /**
     * Ensure that the given instances are locked at the given lock level.
     *
     * @param pcs the objects to lock
     * @param mode the lock level to use
     * @param timeout the number of milliseconds to wait for the lock before
     * giving up, or -1 for no limit
     */
    public void lockAll(Object[] pcs, LockModeType mode, int timeout);

    /**
     * Ensure that the given instances are locked at the current lock level,
     * as set in the {@link FetchPlan} for the entity manager.
     */
    public void lockAll(Object... pcs);

    //////////////
    // Connection
    //////////////

    /**
     * Cancel all pending data store statements. If statements are cancelled
     * while a flush is in progress, the transaction rollback only flag will
     * be set.
     *
     * @return true if any statements were cancelled, false otherwise
     */
    public boolean cancelAll();

    /**
     * Return the connection in use by the entity manager, or a new
     * connection if none.
     */
    public Object getConnection();

    /////////
    // Cache
    /////////

    /**
     * Return a set of all managed instances.
     */
    public Collection getManagedObjects();

    /**
     * Return a set of current transaction instances.
     */
    public Collection getTransactionalObjects();

    /**
     * Return a set of instances which will become transactional upon
     * the next transaction.
     */
    public Collection getPendingTransactionalObjects();

    /**
     * Return a set of current dirty instances.
     */
    public Collection getDirtyObjects();

    /**
     * Whether dirty objects will be returned in the order they were dirtied.
     * Default is determined by the store manager.
     */
    public boolean getOrderDirtyObjects();

    /**
     * Whether dirty objects will be returned in the order they were dirtied.
     * Default is determined by the store manager.
     */
    public void setOrderDirtyObjects(boolean order);

    /**
     * Mark the given class as dirty within the current transaction.
     */
    public void dirtyClass(Class cls);

    /**
     * Return the set of classes that have been made persistent in the current
     * transaction.
     */
    public Collection<Class> getPersistedClasses();

    /**
     * Return the set of classes that have been deleted in the current
     * transaction.
     */
    public Collection<Class> getRemovedClasses();

    /**
     * Return the set of classes for objects that have been modified
     * in the current transaction.
     */
    public Collection<Class> getUpdatedClasses();

    /**
     * Create a new instance of type <code>cls</code>. If <code>cls</code> is
     * an interface or an abstract class whose abstract methods follow the
     * JavaBeans convention, this method will create a concrete implementation
     * according to the metadata that defines the class. If <code>cls</code>
     * is a non-final concrete type that has metadata but does not implement
     * {@link org.apache.openjpa.enhance.PersistenceCapable}, this method will create a
     * subclass of the type that does implement
     * {@link org.apache.openjpa.enhance.PersistenceCapable},
     * following the property-based persistent attribute access rules, or
     * will raise an exception if the class does not meet the requirements
     * for subclassing. Otherwise, this will return an instance of the
     * specified class.
     */
    public <T> T createInstance(Class<T> cls);

    /**
     * Make the named field of the given object dirty.
     */
    public void dirty(Object o, String field);

    /**
     * Return the oid of the given instance.
     */
    public Object getObjectId(Object o);

    /**
     * Return whether the given object is dirty.
     */
    public boolean isDirty(Object o);

    /**
     * Return whether the given object is transactional.
     */
    public boolean isTransactional(Object o);

    /**
     * Return whether the given object is persistent.
     */
    public boolean isPersistent(Object o);

    /**
     * Return whether the given object was made persistent in the current
     * transaction.
     */
    public boolean isNewlyPersistent(Object o);

    /**
     * Return whether the given object is deleted.
     */
    public boolean isRemoved(Object o);

    /**
     * Returns <code>true</code> if <code>pc</code> is a detached object
	 * (one that can be reattached to a {@link EntityManager} via a call
	 * to {@link EntityManager#merge}); otherwise returns
	 * <code>false</code>.
	 */
	public boolean isDetached (Object o);

	/**
	 * Returns the current version indicator for <code>o</code>.
	 */
	public Object getVersion (Object o);
}
