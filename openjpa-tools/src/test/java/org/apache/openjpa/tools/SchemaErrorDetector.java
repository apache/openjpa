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
package org.apache.openjpa.tools;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A SAX Default Handler to catch any errors during parsing.
 * 
 * @author Pinaki Poddar
 *
 */
public class SchemaErrorDetector extends DefaultHandler {
    private boolean _expectingError;
    private List<String> errors = new ArrayList<String>();
    private Locator _locator;
    private String _source;
    
    
    SchemaErrorDetector(String source, boolean expectingError) {
        _source = source;
        _expectingError = expectingError;
    }
    
    @Override
    public void setDocumentLocator(Locator locator) {
        _locator = locator;
    }
    
    @Override
    public void error(SAXParseException exception) throws SAXException {
        handleError(exception);
    }

    @Override
    public void fatalError(SAXParseException exception) throws SAXException {
        handleError(exception);
    }

    @Override
    public void warning(SAXParseException exception) throws SAXException {
        handleError(exception);
    }
    
    private void handleError(SAXParseException exception) {
        String msg = _source + ":" + _locator.getLineNumber() + ":" + exception.getMessage();
        if (_expectingError) {
            System.err.println("The following is an expected error:");
        }
        System.err.println(msg);
        
        errors.add(msg);
        if (!_expectingError)
            exception.printStackTrace();
    
    }
    
    boolean hasErrors() {
        return !errors.isEmpty();
    }
    
    void print() {
        for (String s : errors) {
            System.err.println(s);
        }
    }
    public String toString() {
        StringBuilder buf = new StringBuilder();
        for (String s : errors) {
            buf.append(s).append("\r\n");
        }
        return buf.toString();
    }
}

