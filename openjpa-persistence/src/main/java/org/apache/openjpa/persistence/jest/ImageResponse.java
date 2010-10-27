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
import java.io.RandomAccessFile;

import javax.imageio.ImageIO;

/**
 * Sends an image as response.
 * 
 * @author Pinaki Poddar
 *
 */
@SuppressWarnings("serial")
public class ImageResponse extends AbstractResponse {
    private final InputStream _in;
    private final String _mimeType;
    
    public ImageResponse(Request request, ServerContext ctx, InputStream resource, String mimeType,
        OutputStream out) throws Exception {
        super(request, ctx, out);
        _in = resource;
        _mimeType = mimeType;
    }

    public void writeOut() throws Exception {
        print(_request.getProtocol()); println("200 OK");
        printHeader("Connection",  "close");
        printHeader("Content-Type", _mimeType);
        println();
        byte[] b = new byte[1024];
        int i = 0;
        for (int l = 0; (l = _in.read(b)) != -1;) {
            write(b, 0, l);
            i += l;
        }
        printHeader("Content-Length", ""+i);
        _in.close();
        close();
    }
}
