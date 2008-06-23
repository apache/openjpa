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
package org.apache.openjpa.jdbc.meta.strats;

import java.io.CharArrayReader;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.Reader;

import org.apache.openjpa.jdbc.kernel.JDBCStore;
import org.apache.openjpa.jdbc.meta.JavaSQLTypes;
import org.apache.openjpa.jdbc.meta.ValueMapping;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.schema.ColumnIO;
import org.apache.openjpa.jdbc.sql.Sized;
import org.apache.openjpa.util.StoreException;

/**
 * Handler for char array values.
 *
 * @nojavadoc
 */
public class CharArrayStreamValueHandler
    extends AbstractValueHandler {

    private static final CharArrayStreamValueHandler _instance =
        new CharArrayStreamValueHandler();

    /**
     * Singleton instance.
     */
    public static CharArrayStreamValueHandler getInstance() {
        return _instance;
    }

    public Column[] map(ValueMapping vm, String name, ColumnIO io,
        boolean adapt) {
        Column col = new Column();
        col.setName(name);
        col.setJavaType(JavaSQLTypes.CHAR_STREAM);
        col.setSize(-1);
        return new Column[]{ col };
    }

    public Object toDataStoreValue(ValueMapping vm, Object val,
        JDBCStore store) {
        if (val == null)
            return null;
        char[] chars = PrimitiveWrapperArrays.toCharArray(val);
        return new Sized(new CharArrayReader(chars), chars.length);
    }

    public Object toObjectValue(ValueMapping vm, Object val) {
        if (val == null)
            return null;

        Reader reader = (Reader) val;
        CharArrayWriter writer = new CharArrayWriter();
        try {
            for (int c; (c = reader.read()) != -1;)
                writer.write(c);
        } catch (IOException ioe) {
            throw new StoreException(ioe);
        }
        return PrimitiveWrapperArrays.toObjectValue(vm, writer.toCharArray());
    }
}
