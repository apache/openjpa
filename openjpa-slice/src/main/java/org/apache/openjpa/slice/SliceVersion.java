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
package org.apache.openjpa.slice;

import java.io.InputStream;
import java.util.Properties;

public class SliceVersion  {
    public static final String VERSION;
    public static final String REVISION;
    
    static {
        Properties revisionProps = new Properties();
        try {
            InputStream in = SliceVersion.class.getResourceAsStream
                ("/META-INF/org.apache.openjpa.slice.revision.properties");
            if (in != null) {
                try {
                    revisionProps.load(in);
                } finally {
                    in.close();
                }
            }
        } catch (Exception e) {
        }

        VERSION = revisionProps.getProperty("slice.version", "0.0.0");
        REVISION = revisionProps.getProperty("revision.number");
    }
    
    public static void main(String[] args) {
        System.out.println(new SliceVersion());
    }
    
    public String toString() {
        return "Slice Version " + VERSION + " [revision "+REVISION+"]";
    }

}
