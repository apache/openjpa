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

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.Query;

import org.apache.openjpa.kernel.OpenJPAStateManager;
import org.apache.openjpa.kernel.StoreContext;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.persistence.JPAFacadeHelper;
import org.apache.openjpa.util.ApplicationIds;
import org.apache.openjpa.util.ObjectNotFoundException;

/**
 * @author Pinaki Poddar
 *
 */
@SuppressWarnings("serial")
public class GETRequest extends JESTRequest {
    public Response process(ServerContext server, OutputStream out) throws Exception {
        String action = getAction();
        try {
            if ("find".equals(action)) {
                return find(server, out);
            } else {
                return resource(server, out);
            }
        } catch (Exception e) {
            return new ErrorResponse(this, server, new RuntimeException("bad action " + action), 
                HttpURLConnection.HTTP_BAD_REQUEST, out);
        }
    }
    
    Response find(ServerContext server, OutputStream out)  throws Exception {
        EntityManager em = server.getPersistenceUnit().createEntityManager();
        Map<String, String> qualifiers = getQualifiers();
        Map<String, String> parameters = getParameters();
        if (parameters.size() < 2)
            throw new IllegalArgumentException("find must have at least two parameters");
        Object[] pks = new Object[parameters.size()-1];
        Iterator<Map.Entry<String,String>> params = parameters.entrySet().iterator();
        String alias = null;
        for (int i = 0; i < parameters.size(); i++) {
            if (i == 0) {
                alias = params.next().getKey();
            } else {
                pks[i-1] = params.next().getKey();
            }
        }
        ClassMetaData meta = server.resolve(alias);
        Object oid = ApplicationIds.fromPKValues(pks, meta);
        Object pc = em.find(meta.getDescribedType(), oid); 
        if (pc != null) {
            OpenJPAStateManager sm = ((StoreContext)JPAFacadeHelper.toBroker(em)).getStateManager(pc);
            return new JESTResponse(this, server, sm, out);
        } else {
            return new ErrorResponse(this, server, new EntityNotFoundException("not found!"), 
                HttpURLConnection.HTTP_NOT_FOUND, out);
        }
    }
    
    Response query(ServerContext server, OutputStream out)  throws Exception {
        EntityManager em = server.getPersistenceUnit().createEntityManager();
        Map<String, String> qualifiers = getQualifiers();
        boolean named = isBooleanQualifier("named");
        boolean single = isBooleanQualifier("single");
        Map<String, String> parameters = getParameters();
        if (parameters.size() < 1)
            throw new IllegalArgumentException("find must have at least one parameter");
        Iterator<Map.Entry<String,String>> params = parameters.entrySet().iterator();
        Query query = null;
        int i = 0;
        for (Map.Entry<String, String> param : parameters.entrySet()) {
            if (i == 0) {
                query = named ? em.createQuery(param.getKey()) : em.createNamedQuery(param.getKey());
            } else {
                query.setParameter(param.getKey(), param.getValue());
            }
        }
        if (single) {
            Object result = query.getSingleResult();
            OpenJPAStateManager sm = ((StoreContext)JPAFacadeHelper.toBroker(em)).getStateManager(result);
            return new JESTResponse(this, server, sm, out);
        } else {
            List<Object> result = query.getResultList();
            return new ErrorResponse(this, server, new EntityNotFoundException("not found!"), 404, out);
        }
    }
    
    Response resource(ServerContext server, OutputStream out)  throws Exception {
        String resource = getAction();
        if (resource.length() == 0)
            resource = "index.html";
        String mimeType = getMimeType(resource);
//        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
        InputStream in = getClass().getResourceAsStream(resource);
        if (in == null) {
            return new ErrorResponse(this, server, new ObjectNotFoundException(resource), 404, out);
        }
        if (server.getLog().isTraceEnabled())
            server.getLog().trace("Found resource " + resource);
        return mimeType.startsWith("image") 
          ? new ImageResponse(this, server, in, mimeType, out)
          : new ResourceResponse(this, server, in, mimeType, out);
    }
    
    boolean isBooleanQualifier(String key) {
        String q = getQualifier(key);
        return hasQualifier(key) && (q == null || "true".equals(q));
    }
    
    String getMimeType(String resource) {
        int index = resource.lastIndexOf('.');
        String ext = (index != -1) ? resource.substring(index+1) : ""; 
        if (ext.equalsIgnoreCase("html") || ext.equalsIgnoreCase(".html")) return "text/html";
        if (ext.equalsIgnoreCase(".png") || ext.equalsIgnoreCase(".ico") || ext.equalsIgnoreCase("jpeg")) 
            return "image/"+ext;
        return "text/html";
    }
    
}
