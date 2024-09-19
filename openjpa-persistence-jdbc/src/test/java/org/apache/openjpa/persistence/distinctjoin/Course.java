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
package org.apache.openjpa.persistence.distinctjoin;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * This is a test entity to demonstrate OPENJPA-2179
 */
@Entity
@Table(name="ORACOURSE")
public class Course implements Serializable
{

    
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    private Long id;

    @Version
    private Integer optlock;

    @Column(name = "courseNumber", length = 10, nullable = false)
    private String courseNumber;


    private String normalAttribute;


    @Embedded
    @AttributeOverrides({@AttributeOverride(name = "textDe", column = @Column(name = "objectiveDe")),
                         @AttributeOverride(name = "textEn", column = @Column(name = "objectiveEn"))})
    private LocalizedText objective;

    @Embedded
    @AttributeOverrides({@AttributeOverride(name = "textDe", column = @Column(name = "titleDe")),
            @AttributeOverride(name = "textEn", column = @Column(name = "titleEn"))})
    private LocalizedText title;


    @Lob
    private String lobColumn;

    @OneToMany(mappedBy = "course",
            cascade = {CascadeType.PERSIST, CascadeType.REMOVE, CascadeType.MERGE},
            orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderColumn(name = "POSITION")
    private List<Lecturer> lecturers;


    public Long getId() {
        return id;
    }

    public String getCourseNumber() {
        return courseNumber;
    }

    public void setCourseNumber(String courseNumber) {
        this.courseNumber = courseNumber;
    }

    public LocalizedText getObjective() {
        return objective;
    }

    public void setObjective(LocalizedText objective) {
        this.objective = objective;
    }

    public LocalizedText getTitle() {
        return title;
    }

    public void setTitle(LocalizedText title) {
        this.title = title;
    }

    public String getLobColumn() {
        return lobColumn;
    }

    public void setLobColumn(String lobColumn) {
        this.lobColumn = lobColumn;
    }

    public Integer getOptlock() {
        return optlock;
    }

    public void setOptlock(Integer optlock) {
        this.optlock = optlock;
    }

    public List<Lecturer> getLecturers() {
        return lecturers;
    }

    public void setLecturers(List<Lecturer> lecturers) {
        this.lecturers = lecturers;
    }

    /**
     * @param lecturer the lecturer to add
     */
    void addLecturer(Lecturer lecturer) {
        if (lecturers == null) {
            lecturers = new ArrayList<>();
        }
        lecturers.add(lecturer);
    }


    public String getNormalAttribute() {
        return normalAttribute;
    }

    public void setNormalAttribute(String normalAttribute) {
        this.normalAttribute = normalAttribute;
    }
}
