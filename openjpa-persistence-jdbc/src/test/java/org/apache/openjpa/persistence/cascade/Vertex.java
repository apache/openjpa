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
package org.apache.openjpa.persistence.cascade;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;


@Entity
@NamedQueries({
        @NamedQuery(name = "Vertex.findByName",
                    query = "SELECT n FROM Vertex n where n.type.name=?1"),
        @NamedQuery(name = "Vertex.findAll", query = "SELECT n FROM Vertex n") })
public class Vertex {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long oid;

    @OneToMany(mappedBy = "source", cascade = CascadeType.ALL)
    private List<Edge> outgoing;

    @OneToMany(mappedBy = "target", cascade = CascadeType.ALL)
    private List<Edge> incoming;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "TYPE_OID")
    private VertexType type;

    protected Vertex() {
        this.incoming = new ArrayList<>();
        this.outgoing = new ArrayList<>();
    }

    public Vertex( VertexType type ) {
        this();
        this.type = type;
        type.instances.add( this );
    }

    public Edge newEdge( Vertex target ) {
        Edge t = new Edge( this );
        outgoing.add( t );
        t.setTarget( target );
        return t;
    }

	public long getOid() {
		return oid;
	}
}
