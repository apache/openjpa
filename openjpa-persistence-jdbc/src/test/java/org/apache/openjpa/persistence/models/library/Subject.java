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
package org.apache.openjpa.persistence.models.library;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Entity
@Table(name="LIBSUBJECT")
public class Subject {
    @Id
    @GeneratedValue
    private int oid;

    // persistent fields
    @Basic
    private String name;

    @ManyToMany
    private Set<Book> books;

    protected Subject() {
        // used only by OpenJPA
    }

    public Subject(String name) {
        if (name != null)
            name = name.trim();

        if ((name == null) || (name.length() <= 0))
            throw new IllegalArgumentException("name cannot be empty or null");

        this.name = name;
        books = new HashSet<>();
    }

    public String getName() {
        return name;
    }

    public List<Book> getBooks() {
        return new ArrayList<>(books);
    }

    boolean addBook(Book book) {
        if (book != null)
            return books.add(book);

        return false;
    }

    boolean removeBook(Book book) {
        if (book != null)
            return books.remove(book);

        return false;
    }

    @Override
    public String toString() {
        return "category [" + oid + "] \"" + name + "\"";
    }

    @Override
    public int hashCode() {
        return oid;
    }

    /**
     * Uses the object's persistent identity value to determine equivalence.
     */
    @Override
    public boolean equals(Object other) {
        // standard fare
        if (other == this)
            return true;

        // if the oid is 0, then this object is not persistent.
        // in that case, it cannot be considered equivalent to
        // other managed or unmanaged objects
        if (oid == 0)
            return false;

        if (other instanceof Subject) {
            Subject oc = (Subject) other;
            return oid == oc.oid;
        }

        return false;
    }
}
