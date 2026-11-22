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
package org.apache.openjpa.persistence.simple;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Field access entity that is loaded before the EntityManagerFactory is
 * created, see {@link TestRedefineLoadedUnenhancedClass}. Named "Unenhanced*"
 * so the build-time enhancer skips it.
 */
@Entity
@Table(name = "UNENHANCED_REDEF_MOVIE")
public class UnenhancedRedefinedMovie {

    @Id
    private long id;

    private String title;

    private int year;

    public UnenhancedRedefinedMovie() {
    }

    public UnenhancedRedefinedMovie(long id, String title, int year) {
        this.id = id;
        this.title = title;
        this.year = year;
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }
}
