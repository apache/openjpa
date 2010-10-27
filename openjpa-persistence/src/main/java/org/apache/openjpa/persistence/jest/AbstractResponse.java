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

import java.io.OutputStream;
import java.io.PrintStream;

/**
 * Abstract implementation of a stream-based response.
 * Every response is result of a {@linkplain Request} and operates within a {@linkplain ServerContext}.
 * This fact is enforced by the constructor argument of an abstract response.
 * <p>
 * Besides, this implementation provides common utility to write HTTP response or extract useful information
 * from the request.
 *  
 * @author Pinaki Poddar
 *
 */
@SuppressWarnings("serial")
public abstract class AbstractResponse extends PrintStream implements Response {
    protected final Request _request;
    protected final ServerContext _ctx;
    
    /**
     * Construct a response for the given request and server context.
     * 
     * @param request the request for this response. Can be null if the response is for server error.
     * @param ctx the processing context 
     * @param out the output stream where the response is targeted.
     */
    protected AbstractResponse(Request request, ServerContext ctx, OutputStream out) {
        super(out);
        _request = request;
        _ctx = ctx;
    }
    
    /**
     * Write a HTTP header to the stream.
     * <br> 
     * The format of HTTP header is <code>key: [value]* NEWLINE</code>
     * 
     * @param key the key of the header
     * @param values one of more value of the header fields.
     */
    protected void printHeader(String key, String...values) {
        if (key == null)
            return;
        print(key);
        print(" :");
        if (values == null || values.length == 0)
            return;
        int n = values.length-1;
        for (int i = 0; i < n-1; i++) {
            print(values[i]);
            print(";");
        }
        println(values[n]);
    }
    
}
