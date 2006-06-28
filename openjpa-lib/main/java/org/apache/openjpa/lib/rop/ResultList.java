/*
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.lib.rop;

import org.apache.openjpa.lib.util.*;
import org.apache.openjpa.lib.util.Closeable;

import java.io.*;

import java.util.*;
import java.util.NoSuchElementException; // for javadoc; bug #4330419


/**
 *  <p>List interface that represents a potentially lazy ResultList
 *  instantiation.</p>
 *
 *  <p>A ResultList will typically be instantiated from a factory, and
 *  will use a ResultObjectProvider for obtaining individual object
 *  data representations.</p>
 *
 *  <p>Depending on the support for scrolling inputs,
 *  the list that is returned may use lazy instantiation of the
 *  objects, and thus allow very large result sets to be obtained and
 *  manipulated.</p>
 *
 *  <p>Note that wrapping a ResultList in another Collection will
 *  always instantiate the entire set of elements contained in the
 *  ResultList. This may not always be desireable, since the list may
 *  be very large.</p>
 *
 *  @author Marc Prud'hommeaux
 */
public interface ResultList extends List, Serializable, Closeable {
    /**
     *  Returns true if the provider backing this list is open.
     */
    public boolean isProviderOpen();

    /**
     *  Close the list.
     */
    public void close();

    /**
     *  Returns true if the list has been closed.
     */
    public boolean isClosed();
}
