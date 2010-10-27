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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;

import org.apache.openjpa.kernel.OpenJPAStateManager;
import org.apache.openjpa.kernel.StoreContext;
import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.FieldMetaData;
import org.apache.openjpa.persistence.JPAFacadeHelper;
import org.apache.openjpa.persistence.OpenJPAPersistence;
import org.apache.openjpa.util.ApplicationIds;

/**
 * Handles a request from a remote client.
 * Reads the socket data.
 * Populates the request.
 * Determines the processor based on request method and action.
 * Delegates to the processor.
 * Processor generates the response.
 * Writes the response to the stream.
 * 
 * @author Pinaki Poddar
 *
 */
public class RequestHandler implements Callable<Void> {
    private static final int SPACE = ' ';
    private final Socket _socket;
    private final ServerContext _server;
    private final Log _log;
    private static final Localizer _loc = Localizer.forPackage(RequestHandler.class);
    
    public RequestHandler(Socket socket, ServerContext server) {
        _socket = socket;
        _server = server;
        _log = _server.getLog();
    }
    
    public Void call() throws Exception {
        Request request = null;
        Response response = null;
        try {
            request = readRequest(_socket.getInputStream());
            response = request.process(_server, _socket.getOutputStream());
        } catch (Exception e) {
            response = new ErrorResponse(request, _server, e, HttpURLConnection.HTTP_INTERNAL_ERROR, 
                _socket.getOutputStream());
        }
        response.writeOut();
        return null;
    }
    


    /**
     * Reads the given CR-LF delimited stream. 
     * The first line is scanned for the method (first space-delimited String) and protocol (the last
     * space-delimited String). Accordingly a request is created and rest of the input stream content
     * is parsed by the request itself. 
     * 
     * @param input
     * @throws IOException
     */
    public Request readRequest(InputStream input) throws IOException {
        if (_log.isTraceEnabled())
            _log.trace("Reading request from the input stream ");
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        String status = reader.readLine();
        if (_log.isInfoEnabled())
            _log.info("Status Line [" + status + "]");
        int spaceFirst = status.indexOf(SPACE);
        if (spaceFirst == -1) 
            throw new IOException("HTTP Method could not be determined from [" + status + "]");
        int spaceLast = status.lastIndexOf(SPACE);
        if (spaceLast == -1) 
            throw new IOException("HTTP Protocol could not be determined from [" + status + "]");
        String method = status.substring(0, spaceFirst);
        String protocol = status.substring(spaceLast+1);
        String path = status.substring(spaceFirst+1, spaceLast);
        Request request = RequestFactory.getFactory(protocol).createRequest(method);
        List<String> lines = new ArrayList<String>();
        if (path.equals("/")) {
            lines.add(path);
        } else {
            lines = readlines(reader);
            lines.add(0, path);
        }
        if (lines.isEmpty()) {
            throw new IOException("No CR-LF delimited lines could be read from " + input);
        }
        request.read(lines);
        return request;
    }
    

    List<String> readlines(BufferedReader reader) throws IOException {
        List<String> buffers = new ArrayList<String>();
        String line;
        while ((line = reader.readLine()) != null && line.length() > 0) {
            buffers.add(line);
        }
        return buffers;
    }
    
    
    public String toString() {
        return _socket.getInetAddress()+":"+_socket.getPort();
    }    
}
