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
package org.apache.openjpa.persistence.jpql.joins;

import javax.persistence.metamodel.ListAttribute;
import javax.persistence.metamodel.SingularAttribute;

@javax.persistence.metamodel.StaticMetamodel
(value=org.apache.openjpa.persistence.jpql.joins.Department.class)
@javax.annotation.Generated
(value="org.apache.openjpa.persistence.meta.AnnotationProcessor6",date="Tue Jun 03 09:13:08 CDT 2014")
public class Department_ {
    public static volatile SingularAttribute<Department,Integer> deptno;
    public static volatile ListAttribute<Department,Employee> employee2s;
    public static volatile ListAttribute<Department,Employee> employees;
    public static volatile SingularAttribute<Department,String> name;
    public static volatile SingularAttribute<Department,Integer> version;
}
