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

package org.apache.openjpa.persistence.jest;

import java.util.HashMap;
import java.util.Map;

/**
 * A factory to create a specific type of request.
 * 
 * @author Pinaki Poddar
 *
 */
public class RequestFactory {
    private final String _protocol;
    private static Map<String, RequestFactory> _registered = new HashMap<String, RequestFactory>();
    static {
        _registered.put("HTTP/1.0", new RequestFactory("HTTP/1.0"));
        _registered.put("HTTP/1.1", new RequestFactory("HTTP/1.1"));
    }
    
    public static void register(String protocol, RequestFactory factory) {
        _registered.put(protocol, factory);
    }
    
    private RequestFactory(String proto) {
        _protocol = proto;
    }
    
    public static RequestFactory getFactory(String protocol) {
        return _registered.get(protocol);
    }
    
    Request createRequest(String method) {
        JESTRequest request = null;
        if ("GET".equalsIgnoreCase(method)) {
            request = new GETRequest();
        } else {
            throw new UnsupportedOperationException();
        }
        request.setProtocol(_protocol);
        request.setMethod(method.toUpperCase());
        return request;
        
    }
}
