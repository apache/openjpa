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

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
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
@Table(name="DI_LINE1")
@VersionColumn
public class Line1 implements Serializable {
    
    private static final long serialVersionUID = 1L;

    @EmbeddedId
    @AttributeOverride(name="lineNum", column=@Column(name="LINE_NUM"))
    private LineId1 lid;

    @MapsId("page")
    @ManyToOne
    @JoinColumns({
        @JoinColumn(name="LIBRARY_NAME", referencedColumnName="LIBRARY_NAME"),
        @JoinColumn(name="BOOK_NAME", referencedColumnName="BOOK_NAME"),
        @JoinColumn(name="PAGE_NUM", referencedColumnName="PAGE_NUM")
    })
    private Page1 page;

    public LineId1 getLid() {
        return lid;
    }

    public void setLid(LineId1 lid) {
        this.lid = lid;
    }

    public Page1 getPage() {
        return page;
    }

    public void setPage(Page1 page) {
        this.page = page;
    }
}
