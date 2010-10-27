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

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * A request from a remote client to a server to do something.
 * The request arrives as stream of bytes from a remote location
 * and if the server can interpret the protocol from the stream, 
 * then  make a concrete request object.
 * 
 * @author Pinaki Poddar
 *
 */
public interface Request extends Serializable {
    /**
     * Get the HTTP verb such as GET, POST
     * 
     * @return uppercase string
     */
    String getMethod();
    
    /**
     * Get the first path segment as intended persistence action such as <code>find</code> or <code>query</code>
     *   
     * @return lowercase action name. Can be empty.
     */
    String getAction();
    
    /**
     * Get the protocol such as HTTP/1.1
     * 
     * @return upper-case string
     */
    String getProtocol();
    
    /**
     * Get the body, if any.
     * 
     * @return body of the request. null if no body.
     */
    String getBody();
    
    /**
     * Get the headers indexed by the keys.
     * Header values are list of Strings.
     * 
     * @return empty map if there is no header
     */
    Map<String, List<String>> getHeaders();
    
    /**
     * Get the header values for the given key.
     * @param key a key
     * @return null if no header value for the given key
     */
    List<String> getHeader(String key);
    
    /**
     * Affirm if the the given qualifier is available in this request.
     *  
     * @param key case-sensitive qualifier
     * @return true if the key is present.
     */
    boolean hasQualifier(String key);
    
    /**
     * Gets the value for the given qualifier key.
     * 
     * @param key case-sensitive qualifier
     * @return value of the qualifier. null if the key is absent.
     */
    String getQualifier(String key);
    
    /**
     * Get all the qualifiers available in this request.
     * 
     * @return key-value pairs of the qualifiers. Empty map if no qualifier is present.
     */
    Map<String,String> getQualifiers();
    
    
    /**
     * Affirm if the the given parameter is available in this request.
     *  
     * @param key case-sensitive parameter
     * @return true if the key is present.
     */
    boolean hasParameter(String key);
    
    
    /**
     * Gets the value for the given parameter key.
     * 
     * @param key case-sensitive parameter
     * @return value of the parameter. null if the key is absent.
     */
    String getParameter(String key);
    
    /**
     * Get all the parameters available in this request.
     * 
     * @return key-value pairs of the parameters. Empty map if no parameter is present.
     */
    Map<String,String> getParameters();
    
    /**
     * Parse the request represented as a list of strings.
     *
     * @param lines each line of the request.
     * @throws IOException
     */
    void read(List<String> lines) throws IOException;
    
    Response process(ServerContext server, OutputStream stream) throws Exception;
}
