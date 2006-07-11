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
package org.apache.openjpa.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.apache.openjpa.enhance.FieldManager;
import org.apache.openjpa.enhance.PCRegistry;
import org.apache.openjpa.enhance.PersistenceCapable;
import org.apache.openjpa.kernel.ObjectIdStateManager;
import org.apache.openjpa.kernel.OpenJPAStateManager;
import org.apache.openjpa.kernel.StoreManager;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.FieldMetaData;
import org.apache.openjpa.meta.JavaTypes;
import org.apache.openjpa.meta.ValueStrategies;
import serp.util.Numbers;

/**
 * Utility class for manipulating application object ids.
 *
 * @author Abe White
 * @nojavadoc
 */
public class ApplicationIds {

    /**
     * Return the primary key values for the given object id. The values
     * will be returned in the same order as the metadata primary key fields.
     */
    public static Object[] toPKValues(Object oid, ClassMetaData meta) {
        if (meta == null)
            return null;
        Object[] pks;
        if (meta.isOpenJPAIdentity()) {
            pks = new Object[1];
            if (oid != null)
                pks[0] = ((OpenJPAId) oid).getIdObject();
            return pks;
        }
        // reset owning 'meta' to the owner of the primary key fields, because
        // the one passed in might be a proxy, like for embedded mappings;
        // since getPrimaryKeyFields is guaranteed to return the primary
        // keys in the order of inheritance, we are guaranteed that
        // the last element will be the most-derived class.
        FieldMetaData[] fmds = meta.getPrimaryKeyFields();
        meta = fmds[fmds.length - 1].getDeclaringMetaData();
        pks = new Object[fmds.length];
        if (oid == null)
            return pks;
        if (!Modifier.isAbstract(meta.getDescribedType().getModifiers())) {
            // copy fields from the oid
            PrimaryKeyFieldManager consumer = new PrimaryKeyFieldManager();
            consumer.setStore(pks);
            PCRegistry.copyKeyFieldsFromObjectId(meta.getDescribedType(),
                consumer, oid);
            return consumer.getStore();
        }
        // default to reflection
        if (meta.isObjectIdTypeShared())
            oid = ((ObjectId) oid).getId();
        Class oidType = oid.getClass();
        try {
            Field field;
            Method meth;
            for (int i = 0; i < fmds.length; i++) {
                if (meta.getAccessType() == ClassMetaData.ACCESS_FIELD) {
                    field = oidType.getField(fmds[i].getName());
                    pks[i] = field.get(oid);
                } else { // property
                    meth = ImplHelper.getGetter(oidType, fmds[i].getName());
                    pks[i] = meth.invoke(oid, (Object[]) null);
                }
            }
            return pks;
        } catch (OpenJPAException ke) {
            throw ke;
        } catch (Throwable t) {
            throw new GeneralException(t);
        }
    }

    /**
     * Return a new object id constructed from the given primary key values.
     */
    public static Object fromPKValues(Object[] pks, ClassMetaData meta) {
        if (meta == null || pks == null)
            return null;
        boolean convert = !meta.getRepository().getConfiguration().
            getCompatibilityInstance().getStrictIdentityValues();
        if (meta.isOpenJPAIdentity()) {
            int type = meta.getPrimaryKeyFields()[0].getDeclaredTypeCode();
            Object val = (convert) ? JavaTypes.convert(pks[0], type) : pks[0];
            switch (type) {
                case JavaTypes.BYTE:
                case JavaTypes.BYTE_OBJ:
                    if (!convert && !(val instanceof Byte))
                        throw new ClassCastException("!(x instanceof Byte)");
                    return new ByteId(meta.getDescribedType(),
                        ((Number) val).byteValue());
                case JavaTypes.CHAR:
                case JavaTypes.CHAR_OBJ:
                    return new CharId(meta.getDescribedType(),
                        ((Character) val).charValue());
                case JavaTypes.INT:
                case JavaTypes.INT_OBJ:
                    if (!convert && !(val instanceof Integer))
                        throw new ClassCastException("!(x instanceof Byte)");
                    return new IntId(meta.getDescribedType(),
                        ((Number) val).intValue());
                case JavaTypes.LONG:
                case JavaTypes.LONG_OBJ:
                    if (!convert && !(val instanceof Long))
                        throw new ClassCastException("!(x instanceof Byte)");
                    return new LongId(meta.getDescribedType(),
                        ((Number) val).longValue());
                case JavaTypes.SHORT:
                case JavaTypes.SHORT_OBJ:
                    if (!convert && !(val instanceof Short))
                        throw new ClassCastException("!(x instanceof Byte)");
                    return new ShortId(meta.getDescribedType(),
                        ((Number) val).shortValue());
                case JavaTypes.STRING:
                    return new StringId(meta.getDescribedType(), (String) val);
                case JavaTypes.DATE:
                    return new DateId(meta.getDescribedType(), (Date) val);
                case JavaTypes.OID:
                    return new ObjectId(meta.getDescribedType(), val);
                default:
                    throw new InternalException();
            }
        }
        // copy pks to oid
        if (!Modifier.isAbstract(meta.getDescribedType().getModifiers())) {
            Object oid = PCRegistry.newObjectId(meta.getDescribedType());
            PrimaryKeyFieldManager producer = new PrimaryKeyFieldManager();
            producer.setStore(pks);
            if (convert)
                producer.setMetaData(meta);
            PCRegistry.copyKeyFieldsToObjectId(meta.getDescribedType(),
                producer, oid);
            return oid;
        }
        // default to reflection
        Class oidType = meta.getObjectIdType();
        try {
            // create a new id
            Object copy = oidType.newInstance();
            // set each field
            FieldMetaData[] fmds = meta.getPrimaryKeyFields();
            Field field;
            Method meth;
            Class[] paramTypes = null;
            Object[] params = null;
            for (int i = 0; i < fmds.length; i++) {
                if (meta.getAccessType() == ClassMetaData.ACCESS_FIELD) {
                    field = oidType.getField(fmds[i].getName());
                    field.set(copy, (convert) ? JavaTypes.convert(pks[i],
                        fmds[i].getDeclaredTypeCode()) : pks[i]);
                } else { // property
                    if (paramTypes == null)
                        paramTypes = new Class[1];
                    paramTypes[0] = fmds[i].getDeclaredType();
                    meth = oidType.getMethod("set" + StringUtils.capitalize
                        (fmds[i].getName()), paramTypes);
                    if (params == null)
                        params = new Object[1];
                    params[0] = (convert) ? JavaTypes.convert(pks[i],
                        fmds[i].getDeclaredTypeCode()) : pks[i];
                    meth.invoke(copy, params);
                }
            }
            if (meta.isObjectIdTypeShared())
                copy = new ObjectId(meta.getDescribedType(), copy);
            return copy;
        } catch (Throwable t) {
            throw new GeneralException(t);
        }
    }

    /**
     * Copy the given oid value.
     */
    public static Object copy(Object oid, ClassMetaData meta) {
        if (meta == null || oid == null)
            return null;
        if (meta.isOpenJPAIdentity()) {
            // use meta type instead of oid type in case it's a subclass
            Class cls = meta.getDescribedType();
            OpenJPAId koid = (OpenJPAId) oid;
            FieldMetaData pk = meta.getPrimaryKeyFields()[0];
            switch (pk.getDeclaredTypeCode()) {
                case JavaTypes.BYTE:
                case JavaTypes.BYTE_OBJ:
                    return new ByteId(cls, ((ByteId) oid).getId(),
                        koid.hasSubclasses());
                case JavaTypes.CHAR:
                case JavaTypes.CHAR_OBJ:
                    return new CharId(cls, ((CharId) oid).getId(),
                        koid.hasSubclasses());
                case JavaTypes.INT:
                case JavaTypes.INT_OBJ:
                    return new IntId(cls, ((IntId) oid).getId(),
                        koid.hasSubclasses());
                case JavaTypes.LONG:
                case JavaTypes.LONG_OBJ:
                    return new LongId(cls, ((LongId) oid).getId(),
                        koid.hasSubclasses());
                case JavaTypes.SHORT:
                case JavaTypes.SHORT_OBJ:
                    return new ShortId(cls, ((ShortId) oid).getId(),
                        koid.hasSubclasses());
                case JavaTypes.STRING:
                    return new StringId(cls, oid.toString(),
                        koid.hasSubclasses());
                case JavaTypes.OID:
                    ClassMetaData embed = pk.getEmbeddedMetaData();
                    Object inner = koid.getIdObject();
                    if (embed != null)
                        inner = copy(inner, embed, embed.getFields());
                    return new ObjectId(cls, inner, koid.hasSubclasses());
                default:
                    throw new InternalException();
            }
        }
        // create a new pc instance of the right type, set its key fields
        // to the original oid values, then copy its key fields to a new
        // oid instance
        if (!Modifier.isAbstract(meta.getDescribedType().getModifiers())) {
            PersistenceCapable pc = PCRegistry.newInstance
                (meta.getDescribedType(), null, oid, false);
            Object copy = pc.pcNewObjectIdInstance();
            pc.pcCopyKeyFieldsToObjectId(copy);
            return copy;
        }
        Object copy =
            (!meta.isObjectIdTypeShared()) ? oid : ((ObjectId) oid).getId();
        copy = copy(copy, meta, meta.getPrimaryKeyFields());
        if (meta.isObjectIdTypeShared())
            copy = new ObjectId(meta.getDescribedType(), copy,
                ((OpenJPAId) oid).hasSubclasses());
        return copy;
    }

    /**
     * Copy the given identity object using reflection.
     */
    private static Object copy(Object oid, ClassMetaData meta,
        FieldMetaData[] fmds) {
        if (oid == null)
            return null;
        // default to using reflection
        Class oidType = oid.getClass();
        try {
            Object copy = oidType.newInstance();
            Field field;
            Method meth;
            String cap;
            Class[] paramTypes = null;
            Object[] params = null;
            for (int i = 0; i < fmds.length; i++) {
                if (fmds[i].getManagement() != FieldMetaData.MANAGE_PERSISTENT)
                    continue;
                if (meta.getAccessType() == ClassMetaData.ACCESS_FIELD) {
                    field = oidType.getField(fmds[i].getName());
                    field.set(copy, field.get(oid));
                } else { // property
                    if (paramTypes == null)
                        paramTypes = new Class[1];
                    paramTypes[0] = fmds[i].getDeclaredType();
                    cap = StringUtils.capitalize(fmds[i].getName());
                    meth = oidType.getMethod("set" + cap, paramTypes);
                    if (params == null)
                        params = new Object[1];
                    params[0] = ImplHelper.getGetter(oidType, cap).
                        invoke(oid, (Object[]) null);
                    meth.invoke(copy, params);
                }
            }
            return copy;
        } catch (OpenJPAException ke) {
            throw ke;
        } catch (Throwable t) {
            throw new GeneralException(t);
        }
    }

    /**
     * Generate an application id based on the current primary key field state
     * of the given instance.
     */
    public static Object create(PersistenceCapable pc, ClassMetaData meta) {
        if (pc == null)
            return null;
        Object oid = pc.pcNewObjectIdInstance();
        if (oid == null)
            return null;
        if (!meta.isOpenJPAIdentity()) {
            pc.pcCopyKeyFieldsToObjectId(oid);
            return oid;
        }
        FieldMetaData pk = meta.getPrimaryKeyFields()[0];
        if (pk.getDeclaredTypeCode() != JavaTypes.OID)
            return oid;
        // always copy oid object in case field value mutates or becomes
        // managed
        ObjectId objid = (ObjectId) oid;
        ClassMetaData embed = pk.getEmbeddedMetaData();
        objid.setId(copy(objid.getId(), embed, embed.getFields()));
        return objid;
    }

    /**
     * Assign an application identity object to the given state, or return
     * false if determining the application identity requires a flush.
     */
    public static boolean assign(OpenJPAStateManager sm, StoreManager store,
        boolean preFlush) {
        ClassMetaData meta = sm.getMetaData();
        if (meta.getIdentityType() != ClassMetaData.ID_APPLICATION)
            throw new InternalException();
        boolean ret;
        FieldMetaData[] pks = meta.getPrimaryKeyFields();
        if (meta.isOpenJPAIdentity()
            && pks[0].getDeclaredTypeCode() == JavaTypes.OID) {
            OpenJPAStateManager oidsm = new ObjectIdStateManager
                (sm.fetchObjectField(pks[0].getIndex()), sm, pks[0]);
            ret = assign(oidsm, store, pks[0].getEmbeddedMetaData().
                getFields(), preFlush);
            sm.storeObjectField(pks[0].getIndex(), oidsm.getManagedInstance());
        } else ret = assign(sm, store, meta.getPrimaryKeyFields(), preFlush);
        if (!ret)
            return false;
        // base oid on field values
        sm.setObjectId(create(sm.getPersistenceCapable(), meta));
        return true;
    }

    /**
     * Assign generated values to given fields.
     */
    private static boolean assign(OpenJPAStateManager sm, StoreManager store,
        FieldMetaData[] pks, boolean preFlush) {
        for (int i = 0; i < pks.length; i++)
            if (pks[i].getValueStrategy() != ValueStrategies.NONE
                && sm.isDefaultValue(pks[i].getIndex())
                && !store.assignField(sm, pks[i].getIndex(), preFlush))
                return false;
        return true;
    }

    /**
     * Helper class used to transfer pk values to/from application oids.
     */
    private static class PrimaryKeyFieldManager implements FieldManager {

        private Object[] _store = null;
        private int _index = 0;
        private ClassMetaData _meta = null;

        public void setMetaData(ClassMetaData meta) {
            _meta = meta;
        }

        public Object[] getStore() {
            return _store;
        }

        public void setStore(Object[] store) {
            _store = store;
        }

        public void storeBooleanField(int field, boolean val) {
            store((val) ? Boolean.TRUE : Boolean.FALSE);
        }

        public void storeByteField(int field, byte val) {
            store(new Byte(val));
        }

        public void storeCharField(int field, char val) {
            store(new Character(val));
        }

        public void storeShortField(int field, short val) {
            store(new Short(val));
        }

        public void storeIntField(int field, int val) {
            store(Numbers.valueOf(val));
        }

        public void storeLongField(int field, long val) {
            store(Numbers.valueOf(val));
        }

        public void storeFloatField(int field, float val) {
            store(new Float(val));
        }

        public void storeDoubleField(int field, double val) {
            store(new Double(val));
        }

        public void storeStringField(int field, String val) {
            store(val);
        }

        public void storeObjectField(int field, Object val) {
            store(val);
        }

        public boolean fetchBooleanField(int field) {
            return (retrieve(field) == Boolean.TRUE) ? true : false;
        }

        public char fetchCharField(int field) {
            return ((Character) retrieve(field)).charValue();
        }

        public byte fetchByteField(int field) {
            return ((Number) retrieve(field)).byteValue();
        }

        public short fetchShortField(int field) {
            return ((Number) retrieve(field)).shortValue();
        }

        public int fetchIntField(int field) {
            return ((Number) retrieve(field)).intValue();
        }

        public long fetchLongField(int field) {
            return ((Number) retrieve(field)).longValue();
        }

        public float fetchFloatField(int field) {
            return ((Number) retrieve(field)).floatValue();
        }

        public double fetchDoubleField(int field) {
            return ((Number) retrieve(field)).doubleValue();
        }

        public String fetchStringField(int field) {
            return (String) retrieve(field);
        }

        public Object fetchObjectField(int field) {
            return retrieve(field);
        }

        private void store(Object val) {
            _store[_index++] = val;
        }

        private Object retrieve(int field) {
            Object val = _store[_index++];
            if (_meta != null)
                val = JavaTypes.convert(val, _meta.getField(field).
                    getDeclaredTypeCode());
            return val;
        }
    }
}
