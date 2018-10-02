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
package org.apache.openjpa.persistence.criteria.multiselect;

import java.lang.Boolean;
import java.lang.Double;
import java.lang.Integer;
import java.lang.Long;
import java.lang.String;
import java.util.Date;
import javax.persistence.metamodel.SingularAttribute;

@javax.persistence.metamodel.StaticMetamodel
(value=FactWorkAssignment.class)
@javax.annotation.Generated
(value="org.apache.openjpa.persistence.meta.AnnotationProcessor6",date="Mon Feb 23 16:16:50 MST 2015")
public class FactWorkAssignment_ {
    public static volatile SingularAttribute<FactWorkAssignment,String> clockNumber;
    public static volatile SingularAttribute<FactWorkAssignment,Double> compaRt;
    public static volatile SingularAttribute<FactWorkAssignment,Boolean> currentRecord;
    public static volatile SingularAttribute<FactWorkAssignment,String> dataCntrlNumber;
    public static volatile SingularAttribute<FactWorkAssignment,Date> effEndDt;
    public static volatile SingularAttribute<FactWorkAssignment,Date> effStartDt;
    public static volatile SingularAttribute<FactWorkAssignment,Long> effectiveEndDate;
    public static volatile SingularAttribute<FactWorkAssignment,Long> effectiveStartDate;
    public static volatile SingularAttribute<FactWorkAssignment,Integer> employeeCount;
    public static volatile SingularAttribute<FactWorkAssignment,Long> hrOrgKey;
    public static volatile SingularAttribute<FactWorkAssignment,Long> jobKey;
    public static volatile SingularAttribute<FactWorkAssignment,Long> managerKey;
    public static volatile SingularAttribute<FactWorkAssignment,String> orgOID;
    public static volatile SingularAttribute<FactWorkAssignment,Long> paygroupKey;
    public static volatile SingularAttribute<FactWorkAssignment,Long> payrollOrgKey;
    public static volatile SingularAttribute<FactWorkAssignment,Long> personKey;
    public static volatile SingularAttribute<FactWorkAssignment,String> personObjId;
    public static volatile SingularAttribute<FactWorkAssignment,Long> personProfileKey;
    public static volatile SingularAttribute<FactWorkAssignment,Integer> primary;
    public static volatile SingularAttribute<FactWorkAssignment,Long> salPlanKey;
    public static volatile SingularAttribute<FactWorkAssignment,String> secClrCd;
    public static volatile SingularAttribute<FactWorkAssignment,String> statusCode;
    public static volatile SingularAttribute<FactWorkAssignment,String> statusDesc;
    public static volatile SingularAttribute<FactWorkAssignment,String> workAssgnmntNbr;
    public static volatile SingularAttribute<FactWorkAssignment,Long> workLocationKey;
}
