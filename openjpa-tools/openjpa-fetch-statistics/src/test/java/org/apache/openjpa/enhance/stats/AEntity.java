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
package org.apache.openjpa.enhance.stats;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class AEntity {

    @Id
    private int id;
    private String name;
    private String desc;
    private boolean checked;
    private EEntity extraInfo;

    @ManyToOne
    private BEntity referredBEntity;


    public AEntity(int id, String name, String desc, BEntity bEntity) {
        super();
        this.id = id;
        this.name = name;
        this.desc = desc;
        referredBEntity = bEntity;
        extraInfo = new EEntity("extra " + desc, "E" + id);
    }

    public String getFullInfo(){
        return name + desc;
    }


    public boolean isChecked(){
        return checked;
    }

    public String getReferredBEntityName(){
        return referredBEntity.getName();
    }

    public String getExtraInfo(){
        return extraInfo.getExtraDesc();
    }

}
