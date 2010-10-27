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
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.openjpa.kernel.OpenJPAStateManager;
import org.apache.openjpa.persistence.JPAFacadeHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Response of a JEST Request.
 * 
 * @author Pinaki Poddar
 *
 */
@SuppressWarnings("serial")
public class JESTResponse extends AbstractResponse {
    private final OpenJPAStateManager _sm;
    
    public JESTResponse(Request request, ServerContext ctx, OpenJPAStateManager sm, OutputStream out) throws Exception {
        super(request, ctx, out);
        _sm = sm;
    }
    
    public String getContentType() {
        return "text/html";
    }
    
    public void writeOut() throws Exception {
        print(_request.getProtocol()); println("200 OK");
        printHeader("Connection",  "close");
        printHeader("Content-Type", "text/html", "charset=UTF-8");
        println();
        println("<html><body><pre>");
        XMLEncoder encoder = new XMLEncoder(_ctx.getPersistenceUnit().getMetamodel());
        Document doc = encoder.encode(_sm);
        encoder.writeDoc(doc, this);
        println("Response from JEST");
        
        println("</pre></body></html>");
        close();
    }
    
}


