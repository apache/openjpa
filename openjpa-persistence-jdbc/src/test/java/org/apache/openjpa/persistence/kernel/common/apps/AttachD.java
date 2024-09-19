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
package org.apache.openjpa.persistence.kernel.common.apps;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.apache.openjpa.persistence.FetchAttribute;
import org.apache.openjpa.persistence.FetchGroup;
import org.apache.openjpa.persistence.FetchGroups;
import org.apache.openjpa.persistence.PersistentMap;
import org.apache.openjpa.persistence.jdbc.KeyColumn;

@Entity
@FetchGroups({
@FetchGroup(name = "all", attributes = {
@FetchAttribute(name = "bs", recursionDepth = 0),
@FetchAttribute(name = "pcStringMap"),
@FetchAttribute(name = "bigIntegerPCMap", recursionDepth = 0),
@FetchAttribute(name = "embeddedE", recursionDepth = 0),
@FetchAttribute(name = "embeddedA", recursionDepth = 0)
    })
    })
@Table(name="K_ATTACHD")
public class AttachD implements Serializable {

    
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "D_ID")
    private int id;

    private Object version;

    @Basic
    private String dstr;
    @Basic
    private int dint;
    @Basic
    private double ddbl;

    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    private AttachA a;

    @ManyToMany(mappedBy = "ds",
        cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    private Set<AttachB> bs = new HashSet();

    @PersistentMap(keyCascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @KeyColumn(name = "strngmap")
    private Map<AttachA, String> pcStringMap = new HashMap();

    @PersistentMap(elementCascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @KeyColumn(name = "intmap")
    private TreeMap<BigInteger, AttachF> bigIntegerPCMap =
        new TreeMap<>();

    @Embedded
    private AttachE embeddedE;

    @Embedded
    private AttachA embeddedA;

    public int getId() {
        return id;
    }

    public void setDstr(String dstr) {
        this.dstr = dstr;
    }

    public String getDstr() {
        return this.dstr;
    }

    public void setDint(int dint) {
        this.dint = dint;
    }

    public int getDint() {
        return this.dint;
    }

    public void setDdbl(double ddbl) {
        this.ddbl = ddbl;
    }

    public double getDdbl() {
        return this.ddbl;
    }

    public AttachA getA() {
        return a;
    }

    public void setA(AttachA a) {
        this.a = a;
    }

    public void setBs(Set bs) {
        this.bs = bs;
    }

    public Set getBs() {
        return this.bs;
    }

    public void setPcStringMap(Map pcStringMap) {
        this.pcStringMap = pcStringMap;
    }

    public Map getPcStringMap() {
        return this.pcStringMap;
    }

    public void setBigIntegerPCMap(TreeMap bigIntegerPCMap) {
        this.bigIntegerPCMap = bigIntegerPCMap;
    }

    public TreeMap getBigIntegerPCMap() {
        return this.bigIntegerPCMap;
    }

    public void setEmbeddedE(AttachE embeddedE) {
        this.embeddedE = embeddedE;
    }

    public AttachE getEmbeddedE() {
        return this.embeddedE;
    }

    public void setEmbeddedA(AttachA embeddedA) {
        this.embeddedA = embeddedA;
    }

    public AttachA getEmbeddedA() {
        return this.embeddedA;
    }

    public Object getVersion() {
        return this.version;
    }
}
