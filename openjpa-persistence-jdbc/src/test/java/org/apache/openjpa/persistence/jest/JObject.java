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

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

/**
 * A persistent entity with singular and plural association used for testing encoding to XML and JSON.
 * 
 * @author Pinaki Poddar
 *
 */
@Entity
public class JObject {
    @Id
    private long ssn;
    
    private String name;
    
    private int age;
    
    @OneToOne
    private JObject spouse;
    
    @OneToMany
    private List<JObject> friends;

    public long getSsn() {
        return ssn;
    }

    public void setSsn(long ssn) {
        this.ssn = ssn;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }
    
    public List<JObject> getFriends() {
        return friends;
    }
    
    public void addFriend(JObject friend) {
        if (friends == null)
            friends = new ArrayList<JObject>();
        friends.add(friend);
    }
    
    public void setSpouse(JObject p) {
        spouse = p;
    }
    
    public JObject getSpouse() {
        return spouse;
    }
    
}
