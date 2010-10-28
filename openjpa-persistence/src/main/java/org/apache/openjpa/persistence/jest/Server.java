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
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.persistence.EntityManagerFactory;

import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.lib.conf.Configurable;
import org.apache.openjpa.lib.conf.Configuration;
import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.MetaDataRepository;
import org.apache.openjpa.persistence.EntityManagerFactoryImpl;


/**
 * A server running on an independent thread that allows a remote, language-neutral client to access OpenJPA runtime.
 *  
 * @author Pinaki Poddar
 *
 */
public class Server implements ServerContext, Configurable, Runnable {
    private ServerSocket _listenSocket;
    protected ExecutorService _executors;
    public final static int DEFAULT_PORT = 6789;
    protected String _host = "127.0.0.1";
    protected int _port = DEFAULT_PORT;
    protected int _range = 1;
    protected String _format = "xml";
    protected Log _log;
    protected Thread _thread;
    private EntityManagerFactoryImpl _ctx;
    private static Localizer _loc = Localizer.forPackage(Server.class);
    
    public Server() {
        try {
            _host = InetAddress.getLocalHost().getHostAddress();
        } catch (Exception ex) {
            
        }

    }
    /**
     * Sets the persistence unit context in which this server will serve requests.
     * The context must be set before operation.
     * 
     * @param emf an implementation of OpenJPA Persistence Unit. Must not be null.
     */
    public void setContext(EntityManagerFactoryImpl emf) {
        if (emf == null)
            throw new NullPointerException();
        _ctx = emf;
    }
    
    /**
     * Gets the persistence unit context in which this server serves requests.
     * 
     * @param emf an implementation of OpenJPA Persistence Unit. 
     */
    public EntityManagerFactoryImpl getPersistenceUnit() {
        return _ctx;
    }
    
    public Log getLog() {
        return _log;
    }
    
    /**
     * Start the server in a daemon thread.
     * This method is idempotent.
     * 
     * @return true if the server has started by this call or already running. 
     */
    public synchronized boolean start() {
        try {
            if (_thread != null)
                return true;
            if (createServerSocket()) {
                _thread = new Thread(this);
                _thread.setDaemon(true);
                _thread.start();
                return true;
            }
            return false;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }
    
    /**
     * Stops the server.
     */
    public synchronized void stop() {
        _thread.interrupt();
        _thread = null;
        _executors.shutdownNow();
    }
    
    public String getHost() {
        return _host;
    }
    
    public URI getURI() {
        try {
            return new URI("http://"+_host+":"+_port);
        } catch (URISyntaxException e) {
            return null;
        }
    }
    
    /**
     * Sets the port in which the server will listen.
     * 
     * @param port a positive integer.
     */
    public void setPort(int port) {
        _port = port;
    }
    
    /**
     * Gets the current port.
     * 
     * @return the port number. Defaults to default HTTP port.
     */
    public int getPort() {
        return _port;
    }
    
    /**
     * Sets the range of ports the server will attempt at start.
     * 
     * @param range a positive integer.
     */
    public void setRange(int range) {
        if (range > 0) 
            _range = range;
    }
    
    public void setFormat(String format) {
        _format = format;
    }
    
    public String getFormat() {
        return _format;
    }
    
    /**
     * Sets the range of ports the server will attempt at start.
     * @return  a positive integer. Defaults to 1.
     */
    public int getRange() {
        return _range;
    }
    
    public void run() {
        _log.info(_loc.get("server-starting", this));
        
        _executors = Executors.newCachedThreadPool();
        while (!Thread.interrupted()) {
            try {
                Socket socket = _listenSocket.accept();
                if (_log.isTraceEnabled())
                    _log.trace(_loc.get("server-request", socket));
                RequestHandler request = new RequestHandler(socket, this); 
                _executors.submit(request);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private boolean createServerSocket() {
        int p = _port;
        int p2 = p + _range;
        Exception error = null;
        for (; _listenSocket == null && p < p2; ) {
            try {
                _listenSocket = new ServerSocket(p);
            } catch (IOException ex) {
                p++;
                error = ex;
            }
        }
        if (_listenSocket != null) {
            if (p != _port) {
                _port = p;
                _log.warn(_loc.get("server-reconfigured", _port));
            }
        } else {
            if (error != null) {
                _log.warn(_loc.get("server-failed", this, _port, error));
            }
        }
        return _listenSocket != null;
    }
    
    // Configurable contract
    public void setConfiguration(Configuration conf) {
        _log = conf.getLog("Remote");
    }

    public void startConfiguration() {
    }

    public void endConfiguration() {
    }
    
    
    // Server side utilities 
    
    /**
     * Resolves the given alias to a persistent class meta data.
     * 
     * @exception if no meta data available for the given alias 
     */
    public ClassMetaData resolve(String alias) {
        MetaDataRepository repos = _ctx.getConfiguration().getMetaDataRepositoryInstance();
        return repos.getMetaData(alias, Thread.currentThread().getContextClassLoader(), true);
    }
    
    public String toString() {
        if (_listenSocket == null) return "JEST Server [not strated]";
        return "JEST Server " + getURI().toString();
    }

}
