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
package org.apache.openjpa.enhance;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

public class TestAsmAdaptor
{
    @Test
    public void isEnhanced()
    {
        assertTrue(AsmAdaptor.isEnhanced(bytes(Enhanced.class)));
        assertTrue(AsmAdaptor.isEnhanced(bytes(TransitivelyEnhanced.class)));
        assertFalse(AsmAdaptor.isEnhanced(bytes(NotEnhanced.class)));
    }

    private byte[] bytes(final Class<?> type)
    {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
        final InputStream stream = Thread.currentThread().getContextClassLoader()
            .getResourceAsStream(type.getName().replace('.', '/') + ".class");
        try
        {
            int c;
            byte[] buffer = new byte[1024];
            while ((c = stream.read(buffer)) >= 0)
            {
                baos.write(buffer, 0, c);
            }
        }
        catch (IOException e)
        {
            fail(e.getMessage());
        }
        finally
        {
            try
            {
                stream.close();
            }
            catch (IOException e)
            {
                // no-op
            }
        }
        return baos.toByteArray();
    }

    public static class NotEnhanced
    {
    }

    public static class TransitivelyEnhanced extends Enhanced
    {
    }

    public static class Enhanced implements PersistenceCapable // just a mock for the test
    {
        @Override
        public int pcGetEnhancementContractVersion()
        {
            return 0;
        }

        @Override
        public Object pcGetGenericContext()
        {
            return null;
        }

        @Override
        public StateManager pcGetStateManager()
        {
            return null;
        }

        @Override
        public void pcReplaceStateManager(StateManager sm)
        {

        }

        @Override
        public void pcProvideField(int fieldIndex)
        {

        }

        @Override
        public void pcProvideFields(int[] fieldIndices)
        {

        }

        @Override
        public void pcReplaceField(int fieldIndex)
        {

        }

        @Override
        public void pcReplaceFields(int[] fieldIndex)
        {

        }

        @Override
        public void pcCopyFields(Object fromObject, int[] fields)
        {

        }

        @Override
        public void pcDirty(String fieldName)
        {

        }

        @Override
        public Object pcFetchObjectId()
        {
            return null;
        }

        @Override
        public Object pcGetVersion()
        {
            return null;
        }

        @Override
        public boolean pcIsDirty()
        {
            return false;
        }

        @Override
        public boolean pcIsTransactional()
        {
            return false;
        }

        @Override
        public boolean pcIsPersistent()
        {
            return false;
        }

        @Override
        public boolean pcIsNew()
        {
            return false;
        }

        @Override
        public boolean pcIsDeleted()
        {
            return false;
        }

        @Override
        public Boolean pcIsDetached()
        {
            return null;
        }

        @Override
        public PersistenceCapable pcNewInstance(StateManager sm, boolean clear)
        {
            return null;
        }

        @Override
        public PersistenceCapable pcNewInstance(StateManager sm, Object obj, boolean clear)
        {
            return null;
        }

        @Override
        public Object pcNewObjectIdInstance()
        {
            return null;
        }

        @Override
        public Object pcNewObjectIdInstance(Object obj)
        {
            return null;
        }

        @Override
        public void pcCopyKeyFieldsToObjectId(Object obj)
        {

        }

        @Override
        public void pcCopyKeyFieldsToObjectId(FieldSupplier supplier, Object obj)
        {

        }

        @Override
        public void pcCopyKeyFieldsFromObjectId(FieldConsumer consumer, Object obj)
        {

        }

        @Override
        public Object pcGetDetachedState()
        {
            return null;
        }

        @Override
        public void pcSetDetachedState(Object state)
        {

        }
    }
}



