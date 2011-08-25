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
package org.apache.openjpa.kernel;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.openjpa.audit.Auditable;
import org.apache.openjpa.audit.AuditableOperation;
import org.apache.openjpa.audit.Auditor;
import org.apache.openjpa.enhance.PCRegistry;
import org.apache.openjpa.enhance.PCRegistry.RegisterClassListener;
import org.apache.openjpa.enhance.PersistenceCapable;
import org.apache.openjpa.enhance.StateManager;
import org.apache.openjpa.event.LifecycleEvent;
import org.apache.openjpa.event.LifecycleListener;
import org.apache.openjpa.event.TransactionEvent;
import org.apache.openjpa.event.TransactionListener;

/**
 * Controller for audit facility.
 * This controller performs the following basic duties:
 * <LI> Records auditable types at class laoding time
 * <LI> Listens to instance life cycle changes and transaction.
 * <LI> Collects auditable instances on instance life cycle changes.
 * <LI> Delegates real auditing to the {@link Auditor} at transaction boundary.
 * 
 * @author Pinaki Poddar
 *
 */
public class AuditManager extends InMemorySavepointManager 
	implements LifecycleListener, TransactionListener, RegisterClassListener {
	
	private Auditor _auditor;
	private final Set<Class<?>> _allTypes;
	private final Set<Class<?>> _newTypes;
	private final Set<Class<?>> _updateTypes;
	private final Set<Class<?>> _deleteTypes;
	private final Map<Broker, Set<Audited>> _saved;
	private final ReentrantLock _lock = new ReentrantLock();
	
	public AuditManager() {
		super();
		setPreFlush(false);
		_allTypes = new HashSet<Class<?>>();
		_newTypes = new HashSet<Class<?>>();
		_updateTypes = new HashSet<Class<?>>();
		_deleteTypes = new HashSet<Class<?>>();
		_saved = new ConcurrentHashMap<Broker, Set<Audited>>();
		PCRegistry.addRegisterClassListener(this);
	}
	
	/**
	 * Records all auditable classes in operation-specific sets. 
	 */
	@Override
	public void register(Class<?> cls) {
		Auditable auditable = cls.getAnnotation(Auditable.class);
		if (auditable == null) {
			return;
		}
		
		List<AuditableOperation> events = Arrays.asList(auditable.values());
		if (events.contains(AuditableOperation.ALL) || events.contains(AuditableOperation.CREATE)) {
			_newTypes.add(cls);
			_allTypes.add(cls);
		}
		if (events.contains(AuditableOperation.ALL) || events.contains(AuditableOperation.UPDATE)) {
			_updateTypes.add(cls);
			_allTypes.add(cls);
		}
		if (events.contains(AuditableOperation.ALL) || events.contains(AuditableOperation.DELETE)) {
			_deleteTypes.add(cls);
			_allTypes.add(cls);
		}
	}
	
	public void setAuditor(Auditor auditor) {
		_auditor = auditor;
	}
	
	public Auditor getAuditor() {
		return _auditor;
	}

	public Set<Class<?>> getAuditedTypes() {
		return Collections.unmodifiableSet(_allTypes);
	}

	/**
	 * Transaction callbacks.
	 */
	@Override
	public void beforeCommit(TransactionEvent event) {
		if (_auditor == null) return;
		_lock.lock();
		try {
			Broker broker = (Broker)event.getSource();
			Set<Audited> audits = _saved.get(broker);
			if (audits == null) return;
			Collection<Audited> news = new HashSet<Audited>();
			Collection<Audited> updates = new HashSet<Audited>();
			Collection<Audited> deletes = new HashSet<Audited>();
			Collection<?> instances = event.getTransactionalObjects();
			for (Object instance : instances) {
				StateManagerImpl sm = getImpl(instance);
				if (sm != null) {
					Audited audited = search(audits, sm);
					if (audited == null) {
						continue;
					}
					
					if (sm.getPCState().isNew()) {
						news.add(audited);
					} else if (sm.getPCState().isDeleted()) {
						deletes.add(audited);
					} else if (sm.getPCState().isDirty()) {
						updates.add(audited);
					}
				}
			}
			try {
				_auditor.audit(broker, news, updates, deletes);
			} catch (Exception e) {
				if (_auditor.isRollbackOnError()) {
					throw new RuntimeException("dump", e);
				} else {
					e.printStackTrace();
				}
			}
		} finally {
			_lock.unlock();
		}
	}
	
	@Override
	public void afterCommit(TransactionEvent event) {
		_saved.remove(event.getSource());
	}

	@Override
	public void afterRollback(TransactionEvent event) {
		_saved.remove(event.getSource());
	}

	@Override
	public void afterBegin(TransactionEvent event) {
	}

	@Override
	public void beforeFlush(TransactionEvent event) {
	}

	@Override
	public void afterFlush(TransactionEvent event) {
	}

	@Override
	public void afterStateTransitions(TransactionEvent event) {
	}

	@Override
	public void afterCommitComplete(TransactionEvent event) {
	}

	@Override
	public void afterRollbackComplete(TransactionEvent event) {
	}

	/**
	 * Life-cycle callbacks 
	 */
	@Override
	public void afterLoad(LifecycleEvent event) {
		save(AuditableOperation.ALL, event);
	}

	@Override
	public void afterPersist(LifecycleEvent event) {
		save(AuditableOperation.CREATE, event);
	}

	@Override
	public void beforeDelete(LifecycleEvent event) {
		save(AuditableOperation.DELETE, event);
	}

	@Override
	public void beforeDirty(LifecycleEvent event) {
		if (!isAudited(event)) {
			save(AuditableOperation.UPDATE, event);
		}
	}

	@Override
	public void beforePersist(LifecycleEvent event) {
	}

	@Override
	public void afterRefresh(LifecycleEvent event) {
	}

	@Override
	public void beforeStore(LifecycleEvent event) {
	}

	@Override
	public void afterStore(LifecycleEvent event) {
	}

	@Override
	public void beforeClear(LifecycleEvent event) {
	}

	@Override
	public void afterClear(LifecycleEvent event) {
	}

	@Override
	public void afterDelete(LifecycleEvent event) {
	}

	@Override
	public void afterDirty(LifecycleEvent event) {
	}

	@Override
	public void beforeDirtyFlushed(LifecycleEvent event) {
	}

	@Override
	public void afterDirtyFlushed(LifecycleEvent event) {
	}

	@Override
	public void beforeDetach(LifecycleEvent event) {
	}

	@Override
	public void afterDetach(LifecycleEvent event) {
	}

	@Override
	public void beforeAttach(LifecycleEvent event) {
	}

	@Override
	public void afterAttach(LifecycleEvent event) {
	}
	
	/**
	 * Support functions.
	 */
	
	/**
	 * Extracts the persistence capable instance from the source of the given event.
	 * @return null if an instance can not be extracted.
	 */
	protected PersistenceCapable getPersistenceCapable(LifecycleEvent evt) {
		Object source = evt.getSource();
		return source instanceof PersistenceCapable ? (PersistenceCapable)source : null;
	}
	
	/**
	 * Affirms if source of the given event is being audited already.
	 */
	protected boolean isAudited(LifecycleEvent event) {
		StateManagerImpl sm = getImpl(event.getSource());
		if (_saved.containsKey(sm.getBroker())) {
			return search(_saved.get(sm.getBroker()), sm) != null;
		}
		return false;
	}
	
	/**
	 * Extracts the broker from the given persistence capable instance.
	 * @param pc a persistence capable instance
	 * @return null if a Broker can notbe extracted
	 */
	protected Broker getBroker(PersistenceCapable pc) {
		if (pc == null) return null;
		Object ctx = pc.pcGetGenericContext();
		return ctx instanceof Broker ? (Broker)ctx : null;
	}
	
	/**
	 * Saves the source of the given event for auditing.
	 * @param lc
	 * @param event
	 */
	protected void save(AuditableOperation lc, LifecycleEvent event) {
		StateManagerImpl sm = getImpl(event.getSource());
		if (sm != null && isAuditable(lc, sm)) {
			Broker broker = sm.getBroker();
			
			OpenJPASavepoint savepoint = newSavepoint("", broker);
			savepoint.save(Collections.singleton(sm));
			Map<StateManagerImpl, SavepointFieldManager> states = savepoint.getStates();
			Map.Entry<StateManagerImpl, SavepointFieldManager> e = states.entrySet().iterator().next();
			PersistenceCapable copy = e.getValue().getCopy();
			copy.pcReplaceStateManager(null);
			Audited audited = new Audited(sm, copy);
			Set<Audited> audits = _saved.get(broker);
			if (audits == null) {
				audits = new HashSet<Audited>();
				_saved.put(broker, audits);
			}
			audits.add(audited);
		}
	}
	
	/**
	 * Searches the set of Auditable instances for a matching Statemanager. 
	 * @param audits
	 * @param sm
	 * @return
	 */
	private Audited search(Set<Audited> audits, StateManagerImpl sm) {
		for (Audited audit : audits) {
			if (audit.getManagedObject() == sm.getPersistenceCapable()) {
				return audit;
			}
		}
		return null;
	}
	
	/**
	 * Gets an implementation.
	 * @param instance
	 * @return
	 */
	private StateManagerImpl getImpl(Object instance) {
		if (instance instanceof PersistenceCapable) {
			StateManager sm = ((PersistenceCapable)instance).pcGetStateManager();
			if (sm instanceof StateManagerImpl) {
				return (StateManagerImpl)sm;
			} else {
				return null;
			}
		} else {
			return null;
		}
	}
	
	/**
	 * Affirms if the given state manager is auditable for the given operation.
	 * @param op an auditable operation
	 * @param sm
	 * @return
	 */
	protected boolean isAuditable(AuditableOperation op, StateManagerImpl sm) {
		if (sm == null)
			return false;
		Class<?> cls  = sm.getMetaData().getDescribedType();
		return (op == AuditableOperation.ALL    && _allTypes.contains(cls)
			 ||	op == AuditableOperation.CREATE && _newTypes.contains(cls)) 
		     ||(op == AuditableOperation.UPDATE && _updateTypes.contains(cls)) 
		     ||(op == AuditableOperation.DELETE && _deleteTypes.contains(cls));
	}
	

}
