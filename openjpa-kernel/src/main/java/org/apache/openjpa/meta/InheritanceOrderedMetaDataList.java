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
package org.apache.openjpa.meta;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.io.Serializable;

public class InheritanceOrderedMetaDataList
    implements Serializable {

    private MetaDataInheritanceComparator _comp
        = new MetaDataInheritanceComparator();
    private LinkedList<ClassMetaData> buffer = new LinkedList<ClassMetaData>();

    public boolean add(ClassMetaData meta) {
        if (meta == null || buffer.contains(meta))
            return false;
        for (ListIterator<ClassMetaData> itr = buffer.listIterator();
            itr.hasNext();) {
            int ord = _comp.compare(meta, itr.next());
            if (ord > 0)
                continue;
            if (ord == 0)
                return false;
            itr.previous();
            itr.add(meta);
            return true;
        }
        buffer.add(meta);
        return true;
    }

    public boolean remove(ClassMetaData meta) {
        return buffer.remove(meta);
    }

    public ClassMetaData peek() {
        return buffer.peek();
    }
    
    public int size() {
        return buffer.size();
    }
    
    public Iterator<ClassMetaData> iterator() {
        return buffer.iterator();
    }
    
    public boolean isEmpty() {
        return buffer.isEmpty();
    }
    
    public void clear() {
        buffer.clear();
    }
}
