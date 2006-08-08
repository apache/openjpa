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

import java.lang.reflect.InvocationTargetException;

import org.apache.openjpa.kernel.Broker;
import org.apache.openjpa.util.Exceptions;
import org.apache.openjpa.util.OpenJPAException;
import org.apache.openjpa.util.RuntimeExceptionTranslator;
import org.apache.openjpa.util.StoreException;
import org.apache.openjpa.util.UserException;

/**
 * Converts from OpenJPA to persistence exception types.
 *
 * @author Abe White
 * @author Marc Prud'hommeaux
 * @nojavadoc
 */
public class PersistenceExceptions
    extends Exceptions {

    public static final RuntimeExceptionTranslator TRANSLATOR =
        new RuntimeExceptionTranslator() {
            public RuntimeException translate(RuntimeException re) {
                return PersistenceExceptions.toPersistenceException(re);
            }
        };

    /**
     * Returns a {@link RuntimeExceptionTranslator} that will perform
     * the correct exception translation as well as roll back the
     * current transaction when for all but {@link NoResultException}
     * and {@link NonUniqueResultException} in accordance with
     * section 3.7 of the EJB 3.0 specification.
     */
    static RuntimeExceptionTranslator getRollbackTranslator
        (final OpenJPAEntityManager em) {
        return new RuntimeExceptionTranslator() {
            private boolean throwing = false;

            public RuntimeException translate(RuntimeException re) {
                RuntimeException ex = toPersistenceException(re);
                if (!(ex instanceof NonUniqueResultException)
                    && !(ex instanceof NoResultException)
                    && !throwing) {
                    try {
                        throwing = true;
                        if (em.isOpen() && em.isActive())
                            em.setRollbackOnly();
                    } finally {
                        // handle re-entrancy
                        throwing = false;
                    }
                }

                return ex;
            }
        };
    }

    /**
     * Convert the given throwable to the proper persistence exception.
     */
    public static RuntimeException toPersistenceException(Throwable t) {
        return (RuntimeException) translateException(t, true);
    }

    /**
     * Translate the given exception.
     *
     * @param checked whether to translate checked exceptions
     */
    private static Throwable translateException(Throwable t, boolean checked) {
        if (isPersistenceException(t))
            return t;

        // immediately throw errors
        if (t instanceof Error)
            throw (Error) t;

        OpenJPAException ke;
        if (!(t instanceof OpenJPAException)) {
            if (!checked || t instanceof RuntimeException)
                return t;
            ke = new org.apache.openjpa.util.GeneralException(t.getMessage());
            ke.setStackTrace(t.getStackTrace());
            return ke;
        }

        // if only nested exception is a persistence one, return it directly
        ke = (OpenJPAException) t;
        if (ke.getNestedThrowables().length == 1
            && isPersistenceException(ke.getCause()))
            return ke.getCause();

        // RuntimeExceptions thrown from callbacks should be thrown directly
        if (ke.getType() == OpenJPAException.USER
            && ke.getSubtype() == UserException.CALLBACK
            && ke.getNestedThrowables().length == 1) {
            Throwable e = ke.getCause();
            if (e instanceof InvocationTargetException)
                e = e.getCause();

            if (e instanceof RuntimeException)
                return e;
        }

        // perform intelligent translation of openjpa exceptions
        switch (ke.getType()) {
            case OpenJPAException.STORE:
                return translateStoreException(ke);
            case OpenJPAException.USER:
                return translateUserException(ke);
            default:
                return translateGeneralException(ke);
        }
    }

    /**
     * Translate the given store exception.
     */
    private static Throwable translateStoreException(OpenJPAException ke) {
        Exception e;
        switch (ke.getSubtype()) {
            case StoreException.OBJECT_NOT_FOUND:
                e = new org.apache.openjpa.persistence.EntityNotFoundException
                    (ke.getMessage(), getNestedThrowables(ke),
                        getFailedObject(ke), ke.isFatal());
                break;
            case StoreException.OPTIMISTIC:
                e = new org.apache.openjpa.persistence.OptimisticLockException
                    (ke.getMessage(), getNestedThrowables(ke),
                        getFailedObject(ke), ke.isFatal());
                break;
            case StoreException.OBJECT_EXISTS:
                e = new org.apache.openjpa.persistence.EntityExistsException
                    (ke.getMessage(), getNestedThrowables(ke),
                        getFailedObject(ke), ke.isFatal());
                break;
            default:
                e = new org.apache.openjpa.persistence.PersistenceException
                    (ke.getMessage(), getNestedThrowables(ke),
                        getFailedObject(ke), ke.isFatal());
        }
        e.setStackTrace(ke.getStackTrace());
        return e;
    }

    /**
     * Translate the given user exception.
     */
    private static Exception translateUserException(OpenJPAException ke) {
        Exception e;
        switch (ke.getSubtype()) {
            case UserException.NO_TRANSACTION:
                e = new 
                    org.apache.openjpa.persistence.TransactionRequiredException
                        (ke.getMessage(), getNestedThrowables(ke),
                            getFailedObject(ke), ke.isFatal());
                break;
            case UserException.INVALID_STATE:
                e = new org.apache.openjpa.persistence.InvalidStateException
                    (ke.getMessage(), getNestedThrowables(ke),
                        getFailedObject(ke), ke.isFatal());
                break;
            default:
                e = new org.apache.openjpa.persistence.ArgumentException
                    (ke.getMessage(), getNestedThrowables(ke),
                        getFailedObject(ke), ke.isFatal());
        }
        e.setStackTrace(ke.getStackTrace());
        return e;
    }

    /**
     * Translate the given general exception.
     */
    private static Throwable translateGeneralException(OpenJPAException ke) {
        Exception e = new org.apache.openjpa.persistence.PersistenceException
            (ke.getMessage(), getNestedThrowables(ke),
                getFailedObject(ke), ke.isFatal());
        e.setStackTrace(ke.getStackTrace());
        return e;
    }

    /**
     * Return true if the given exception is a persistence exception.
     */
    private static boolean isPersistenceException(Throwable t) {
        return t.getClass().getName()
            .startsWith("org.apache.openjpa.persistence.");
    }

    /**
     * Translate the nested throwables of the given openjpa exception into
     * nested throwables for a persistence exception.
     */
    private static Throwable[] getNestedThrowables(OpenJPAException ke) {
        Throwable[] nested = ke.getNestedThrowables();
        if (nested.length == 0)
            return nested;

        Throwable[] trans = new Throwable[nested.length];
        for (int i = 0; i < nested.length; i++)
            trans[i] = translateException(nested[i], false);
        return trans;
    }

    /**
     * Return the failed object for the given exception, performing any
     * necessary conversions.
     */
    private static Object getFailedObject(OpenJPAException ke) {
        Object o = ke.getFailedObject();
        if (o == null)
            return null;
        if (o instanceof Broker)
            return OpenJPAPersistence.toEntityManager((Broker) o);
        return OpenJPAPersistence.fromOpenJPAObjectId(o);
    }

    /**
     * Helper method to extract a nested exception from an internal nested
     * array in a safe way.
     */
    static Throwable getCause(Throwable[] nested) {
        if (nested == null || nested.length == 0)
            return null;
		return nested[0];
	}
}
