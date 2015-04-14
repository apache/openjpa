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

import java.lang.Integer;
import java.lang.Long;
import java.lang.String;
import java.util.Date;
import javax.persistence.metamodel.SingularAttribute;

@javax.persistence.metamodel.StaticMetamodel
(value=DimDay.class)
@javax.annotation.Generated
(value="org.apache.openjpa.persistence.meta.AnnotationProcessor6",date="Mon Feb 23 16:53:34 MST 2015")
public class DimDay_ {
    public static volatile SingularAttribute<DimDay,Long> currentMonthInd;
    public static volatile SingularAttribute<DimDay,Long> currentMonthVsPrevYearInd;
    public static volatile SingularAttribute<DimDay,Long> currentQtrInd;
    public static volatile SingularAttribute<DimDay,Long> currentQtrVsPrevYearInd;
    public static volatile SingularAttribute<DimDay,Long> currentYearInd;
    public static volatile SingularAttribute<DimDay,Date> date;
    public static volatile SingularAttribute<DimDay,String> dayFullName;
    public static volatile SingularAttribute<DimDay,Long> dayInMonth;
    public static volatile SingularAttribute<DimDay,Long> dayInQuarter;
    public static volatile SingularAttribute<DimDay,Long> dayInWeek;
    public static volatile SingularAttribute<DimDay,Long> dayInYear;
    public static volatile SingularAttribute<DimDay,Long> eomInd;
    public static volatile SingularAttribute<DimDay,Long> eoqInd;
    public static volatile SingularAttribute<DimDay,Long> eoyInd;
    public static volatile SingularAttribute<DimDay,Long> key;
    public static volatile SingularAttribute<DimDay,String> monthCode;
    public static volatile SingularAttribute<DimDay,Long> monthEndDate;
    public static volatile SingularAttribute<DimDay,String> monthFullName;
    public static volatile SingularAttribute<DimDay,Long> monthInYearNumber;
    public static volatile SingularAttribute<DimDay,String> monthName;
    public static volatile SingularAttribute<DimDay,Long> monthStrtDate;
    public static volatile SingularAttribute<DimDay,Long> prevMonthInd;
    public static volatile SingularAttribute<DimDay,Long> prevQtrInd;
    public static volatile SingularAttribute<DimDay,Long> prevYearInd;
    public static volatile SingularAttribute<DimDay,String> qtrFullName;
    public static volatile SingularAttribute<DimDay,String> quarterCode;
    public static volatile SingularAttribute<DimDay,Long> quarterEndDate;
    public static volatile SingularAttribute<DimDay,Integer> quarterInYearNumber;
    public static volatile SingularAttribute<DimDay,Long> quarterStrtDate;
    public static volatile SingularAttribute<DimDay,Long> roll13MonthInd;
    public static volatile SingularAttribute<DimDay,Long> roll4YearsInd;
    public static volatile SingularAttribute<DimDay,Long> roll5QuartersInd;
    public static volatile SingularAttribute<DimDay,Long> weekInYear;
    public static volatile SingularAttribute<DimDay,String> weekInYearFullNm;
    public static volatile SingularAttribute<DimDay,String> year;
    public static volatile SingularAttribute<DimDay,String> yearCode;
    public static volatile SingularAttribute<DimDay,Long> yearEndDate;
    public static volatile SingularAttribute<DimDay,Long> yearStrtDate;
}
