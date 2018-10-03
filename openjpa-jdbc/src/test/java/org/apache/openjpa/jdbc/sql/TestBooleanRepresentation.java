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
package org.apache.openjpa.jdbc.sql;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.openjpa.lib.jdbc.DelegatingPreparedStatement;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * Test for the {@link org.apache.openjpa.jdbc.sql.BooleanRepresentation} factory and default impls
 */
public class TestBooleanRepresentation  extends TestCase {


    public void testBooleanRepresentation() throws Exception {

        checkBooleanRepresentation("BOOLEAN", Boolean.class, Boolean.TRUE, Boolean.FALSE);
        checkBooleanRepresentation("INT_10", Integer.class, 1, 0);
        checkBooleanRepresentation("STRING_10", String.class, "1", "0");

        checkBooleanRepresentation("STRING_YN", String.class, "Y", "N");
        checkBooleanRepresentation("STRING_YN_LOWERCASE", String.class, "y", "n");

        checkBooleanRepresentation("STRING_TF", String.class, "T", "F");
        checkBooleanRepresentation("STRING_TF_LOWERCASE", String.class, "t", "f");

        // and now up to more sophisticated ones:
        checkBooleanRepresentation("oui/non", String.class, "oui", "non");

        checkBooleanRepresentation(
                "org.apache.openjpa.jdbc.sql.TestBooleanRepresentation$DummyTestBooleanRepresentation",
                String.class, "somehowtrue", "somehowfalse");
    }

    private <T> void checkBooleanRepresentation(String representationKey, final Class<T> expectedType,
                                                final T yesRepresentation, final T noRepresentation)
        throws Exception {
        ClassLoader cl = TestBooleanRepresentation.class.getClassLoader();
        BooleanRepresentation booleanRepresentation = BooleanRepresentationFactory.valueOf(representationKey, cl);
        Assert.assertNotNull(booleanRepresentation);

        DummyPreparedStatement<T> dummyPreparedStatement = new DummyPreparedStatement<>(expectedType);

        booleanRepresentation.setBoolean(dummyPreparedStatement, 1, true);
        Assert.assertEquals(yesRepresentation, dummyPreparedStatement.getBooleanRepresentationValue());

        booleanRepresentation.setBoolean(dummyPreparedStatement, 1, false);
        Assert.assertEquals(noRepresentation, dummyPreparedStatement.getBooleanRepresentationValue());


        // and also test getBoolean!
        ResultSet yesRs = (ResultSet) Proxy.newProxyInstance(cl, new Class[]{ResultSet.class},
                new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if (String.class.equals(expectedType) && !"getString".equals(method.getName()) ||
                    Boolean.class.equals(expectedType) && !"getBoolean".equals(method.getName()) ||
                    Integer.class.equals(expectedType) && !"getInt".equals(method.getName())) {
                    Assert.fail("wrong ResultSet method " + method.getName()
                                + "for expectedType " + expectedType.getName());
                }
                return yesRepresentation;
            }
        });
        Assert.assertTrue(booleanRepresentation.getBoolean(yesRs, 1));

        ResultSet noRs = (ResultSet) Proxy.newProxyInstance(cl, new Class[]{ResultSet.class},
                new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if (String.class.equals(expectedType) && !"getString".equals(method.getName()) ||
                    Boolean.class.equals(expectedType) && !"getBoolean".equals(method.getName()) ||
                    Integer.class.equals(expectedType) && !"getInt".equals(method.getName())) {
                    Assert.fail("wrong ResultSet method " + method.getName()
                                + "for expectedType " + expectedType.getName());
                }
                return noRepresentation;
            }
        });
        Assert.assertFalse(booleanRepresentation.getBoolean(noRs, 1));
    }


    /**
     * A small trick to 'intercept' the PreparedStatement call inside the BooleanRepresentation
     */
    public static class DummyPreparedStatement<T> extends DelegatingPreparedStatement {
        private final Class<T> expectedType;
        private Object booleanRepresentationValue;


        public DummyPreparedStatement(Class<T> expectedType) {
            super(null, null);
            this.expectedType = expectedType;
        }

        public T getBooleanRepresentationValue() {
            return (T) booleanRepresentationValue;
        }

        public void setBooleanRepresentationValue(T booleanRepresentationValue) {
            this.booleanRepresentationValue = booleanRepresentationValue;
        }

        @Override
        public void setBoolean(int idx, boolean b) throws SQLException {
            Assert.assertEquals(Boolean.class, expectedType);
            booleanRepresentationValue = b;
        }

        @Override
        public void setString(int idx, String s) throws SQLException {
            Assert.assertEquals(String.class, expectedType);
            booleanRepresentationValue = s;
        }

        @Override
        public void setInt(int idx, int i) throws SQLException {
            Assert.assertEquals(Integer.class, expectedType);
            booleanRepresentationValue = i;
        }
    }

    public static class DummyTestBooleanRepresentation implements BooleanRepresentation<String> {
        @Override
        public void setBoolean(PreparedStatement stmnt, int columnIndex, boolean val) throws SQLException {
            stmnt.setString(columnIndex, getRepresentation(val));
        }

        @Override
        public boolean getBoolean(ResultSet rs, int columnIndex) throws SQLException {
            return "somehowtrue".equals(rs.getString(columnIndex));
        }

        @Override
        public String getRepresentation(boolean bool) {
            return bool ? "somehowtrue" : "somehowfalse";
        }
    }
}
