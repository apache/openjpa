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
package org.apache.openjpa.tools.util;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

import org.xml.sax.Locator;

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
/**
 * Formats log records by augmenting location information from a SAX Parser, if available.
 * 
 * @author Pinaki Poddar
 *
 */
public class LogRecordFormatter extends Formatter {
    private Locator _locator;
    private String  _source;
    
    @Override
    public String format(LogRecord record) {
        return getLocation() + record.getMessage() + "\r\n";
    }

    public void setLocator(Locator locator) {
        _locator = locator;
    }

    public void setSource(String source) {
        _source = source;
    }
    
    String getLocation() {
        return (_source == null ? "" : _source + ":") +  
               (_locator == null ? "" : _locator.getLineNumber() + "::");
    }
    


}
