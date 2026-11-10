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
package org.apache.openjpa.persistence.dynamicschema.names;


import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

/**
 * An entity named after a word that most databases reserve, with attributes
 * named after words that one or another database reserves, explicit names in
 * several forms, and the relations, collections and constraints whose
 * default names are built from such names.
 */
@Entity
@Table(indexes = {@Index(columnList = "rank, window"), @Index(name = "Order_Level", columnList = "level")},
    uniqueConstraints = @UniqueConstraint(columnNames = {"key", "value"}))
public class Order {

    @Id
    private int id;

    @Version
    private int version;

    @ManyToOne
    private User user;

    @ManyToOne
    @JoinColumn(name = "Owner", foreignKey = @ForeignKey(name = "Order_Owner"))
    private User owner;

    private String group;
    private String key;
    private int value;
    private int year;
    private int month;
    private String comment;
    private int level;
    private int size;
    private String number;
    private String file;
    private String plan;
    private String session;
    private int limit;
    private int interval;
    private int position;
    private int row;
    private int rows;
    private String language;
    private String range;
    private String uid;
    private String date;
    private String time;
    private String timestamp;
    private String text;
    private int index;
    private String desc;
    private String check;
    private String primary;
    private String current;
    private String function;
    private String domain;
    private String release;
    private String mode;
    private int rank;
    private String window;
    private String system;
    private String lead;
    private int offset;
    private String returning;
    private String mixedCaseName;
    private String lower_case_name;
    private String UPPER_CASE_NAME;
    private String anAttributeWhoseNameIsLongerThanThirtyCharacters;
    private String anAttributeWhoseNameIsLongerThanSixtyFourCharactersWhichMostDatabasesCannotStore;

    @Column(name = "MixedCaseColumn")
    private String explicitMixedCase;

    @Column(name = "\"QuotedColumn\"")
    private String explicitQuoted;

    @Column(name = "\"rank\"")
    private String explicitQuotedRank;

    @Column(name = "Rows")
    private String explicitRows;

    @Column(name = "Type")
    private String explicitType;

    @Column(name = "Select")
    private String explicitSelect;

    @ElementCollection
    private Set<String> tags;

    @ElementCollection
    @CollectionTable(name = "Order_Values")
    @Column(name = "Value")
    private List<String> values;

    @ElementCollection
    private Map<String, String> attributes;

    @ManyToMany
    private Set<Group> groups;

    @OneToMany(mappedBy = "order")
    @OrderColumn
    private List<Item> items;

    @Embedded
    private Period period;
}
