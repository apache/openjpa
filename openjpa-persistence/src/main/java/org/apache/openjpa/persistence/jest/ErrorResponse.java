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
import java.net.HttpURLConnection;

/**
 * A HTTP response for something gone wrong.
 *  
 * @author Pinaki Poddar
 *
 */
@SuppressWarnings("serial")
public class ErrorResponse extends AbstractResponse {
    private final Exception _error;
    private final int _code;
    /**
     * Construct a response to describe a error.
     * 
     * @param request a request that produced this response. VCan be null to denote that the request can not
     * be created.
     * @param ctx the processing context
     * @param ex the error
     * @param code HTTP error code
     * @param out the stream where the response is written
     * 
     */
    public ErrorResponse(Request request, ServerContext ctx, Exception ex, int code, OutputStream out)  {
        super(request, ctx, out);
        _error = ex;
        _code = code;
    }

    /**
     * Writes the response. 
     * The response is always a HTTP response with the error stack trace.
     */
    public void writeOut() throws Exception {
        println("HTTP/1.1"); print(" " + _code); println("Error");
        printHeader("Connection",  "close");
        printHeader("Content-Type", "text/html", "charset=UTF-8");
        println();
        println("<html><body><pre>");
        _error.printStackTrace(this);
        println("Response from JEST");
        
        println("</pre></body></html>");
        close();
    }
}
