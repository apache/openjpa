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
package org.apache.openjpa.persistence.relations;

import java.util.ArrayList;
import java.util.Collection;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Version;

import org.apache.openjpa.persistence.jdbc.ForeignKey;

@Entity
public class TblChild {

	@Id
	@Column(name = "CHILD_ID",nullable=false)
	private Integer childId;

	@Version
	@Column(name = "VRS_NBR")
	private Integer vrsNbr;

	@OneToMany(mappedBy="tblChild",fetch = FetchType.EAGER,
			cascade = {CascadeType.PERSIST,CascadeType.MERGE})
	private Collection<TblGrandChild> tblGrandChildren =
        new ArrayList<>();

	@ManyToOne(fetch = FetchType.LAZY,
			cascade = {CascadeType.PERSIST,CascadeType.MERGE })
	@JoinColumns({@JoinColumn(name =
		"PARENT_ID",referencedColumnName="PARENT_ID")})
	@ForeignKey
	private TblParent tblParent;

	public Integer getChildId() {
		return childId;
	}

	public void setChildId(Integer childId) {
		this.childId = childId;
	}

	public Integer getVrsNbr() {
		return vrsNbr;
	}

	public void setVrsNbr(Integer vrsNbr) {
		this.vrsNbr = vrsNbr;
	}

	public Collection<TblGrandChild> getTblGrandChildren() {
		return tblGrandChildren;
	}

	public void setTblGrandChildren(Collection<TblGrandChild>
        tblGrandChildren) {
		this.tblGrandChildren = tblGrandChildren;
	}

	public void addTblGrandChild(TblGrandChild tblGrandChild) {
		tblGrandChild.setTblChild(this);
		tblGrandChildren.add(tblGrandChild);
	}

	public void removeTblGrandChild(TblGrandChild tblGrandChild) {
		tblGrandChild.setTblChild(null);
		tblGrandChildren.remove(tblGrandChild);
	}

	public TblParent getTblParent() {
		return tblParent;
	}

	public void setTblParent(TblParent tblParent) {
		this.tblParent = tblParent;
	}
}
