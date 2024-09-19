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
package org.apache.openjpa.persistence.detach;

import java.io.Serializable;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

@Entity
@Table(name="Entity20_detach")
public class Entity20 implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private Integer id;

    @Column(name = "sqldate" )
    @Temporal(TemporalType.DATE)
    private Date sqlDate;

    @Column(name = "sqltime")
    @Temporal(TemporalType.TIME)
    private Time sqlTime;

    @Column(name = "sqltimestamp")
    @Temporal(TemporalType.TIMESTAMP)
    private Timestamp sqlTimestamp;

    private String name;

    public Entity20() {
    }

    public Entity20(int id) {
        this.id = id;
        this.name = this.id.toString();
        Long time = System.currentTimeMillis();
        this.sqlTime = new Time(time);
        this.sqlDate = new Date(time);
        this.sqlTimestamp = new Timestamp(time);
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setDate(Date d) {
        sqlDate = d;
    }

    public Date getDate() {
        return sqlDate;
    }

    public void setTime(Time t) {
        sqlTime = t;
    }

    public Time getTime() {
        return sqlTime;
    }

    public void setTimestamp(Timestamp t) {
        sqlTimestamp = t;
    }

    public Timestamp getTimestamp() {
        return sqlTimestamp;
    }
}
