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
package org.apache.openjpa.jdbc.schema;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.lib.conf.Configurable;
import org.apache.openjpa.lib.conf.Configuration;
import org.apache.openjpa.lib.meta.MetaDataSerializer;
import org.apache.openjpa.lib.util.Files;
import org.apache.openjpa.util.GeneralException;

/**
 * Factory that uses an XML schema file to construct the system schema.
 *
 * @author Abe White
 */
public class FileSchemaFactory
    implements SchemaFactory, Configurable {

    private JDBCConfiguration _conf = null;
    private String _fileName = "package.schema";

    /**
     * Return the XML resource defining this schema. Defaults to
     * <code>package.schema</code>.
     */
    public String getFile() {
        return _fileName;
    }

    /**
     * Set the XML resource defining this schema. Defaults to
     * <code>package.schema</code>.
     */
    public void setFile(String fileName) {
        _fileName = fileName;
    }

    /**
     * @deprecated Use {@link #setFile}. Retained for
     * backwards-compatible auto-configuration.
     */
    public void setFileName(String name) {
        setFile(name);
    }

    public void setConfiguration(Configuration conf) {
        _conf = (JDBCConfiguration) conf;
    }

    public void startConfiguration() {
    }

    public void endConfiguration() {
    }

    public SchemaGroup readSchema() {
        URL url = _conf.getClassLoader().getResource(_fileName); 
        if (url == null)
            return new SchemaGroup();

        XMLSchemaParser parser = new XMLSchemaParser(_conf);
        try {
            parser.parse(url);
        } catch (IOException ioe) {
            throw new GeneralException(ioe);
        }
        return parser.getSchemaGroup();
    }

    public void storeSchema(SchemaGroup schema) {
        File file = Files.getFile(_fileName, _conf.getClassLoader());
        XMLSchemaSerializer ser = new XMLSchemaSerializer(_conf);
        ser.addAll(schema);
        try {
            ser.serialize(file, MetaDataSerializer.PRETTY);
        } catch (IOException ioe) {
            throw new GeneralException(ioe);
        }
    }
}
