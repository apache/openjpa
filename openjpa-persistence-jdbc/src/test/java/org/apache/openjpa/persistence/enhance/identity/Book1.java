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
package org.apache.openjpa.persistence.enhance.identity;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.apache.openjpa.persistence.jdbc.VersionColumn;

/**
 * Entity used to test compound primary keys using entity as relationship to
 * more than one level.
 *
 * Test case and domain classes were originally part of the reported issue
 * <A href="https://issues.apache.org/jira/browse/OPENJPA-207">OPENJPA-207</A>
 *
 * @author Jeffrey Blattman
 * @author Pinaki Poddar
 *
 */
@Entity
@Table(name="DI_BOOK1")
@VersionColumn
public class Book1 implements Serializable {
    
    private static final long serialVersionUID = 1L;

    @EmbeddedId
    @AttributeOverrides({
        @AttributeOverride(name="name", column=@Column(name="BOOK_NAME")),
        @AttributeOverride(name="library", column=@Column(name="LIBRARY_NAME"))
    })
    private BookId1 bid;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "book")
    private Set<Page1> pages = new HashSet<>();

    @MapsId("library")
    @ManyToOne
    @JoinColumn(name="LIBRARY_NAME", referencedColumnName="LIBRARY_NAME")
    private Library1 library;

    private String author;

	public BookId1 getBid() {
        return bid;
    }

    public void setBid(BookId1 bid) {
        this.bid = bid;
    }

    public Library1 getLibrary() {
        return library;
    }

    public void setLibrary(Library1 library) {
        this.library = library;
    }

    public Page1 getPage(PageId1 pid) {
        for (Page1 p: pages) {
            if (p.getPid().equals(pid)) {
                return p;
            }
        }
        return null;
    }

    public void addPage(Page1 p) {
        p.setBook(this);
        pages.add(p);
    }

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Book1)) {
            return false;
        }

        Book1 other = (Book1)o;

        if (!getBid().equals(other.getBid())) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return getBid().hashCode();
    }
}
