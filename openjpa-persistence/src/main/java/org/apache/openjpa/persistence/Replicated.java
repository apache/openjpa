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
package org.apache.openjpa.persistence;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotation to specify the instance of the annotated entity be 
 * replicated across more than one <em>slices</em>. The actual slices where an
 * instance of the annotated entity will be replicated is determined by 
 * the return value of user-specified 
 * {@link ReplicationPolicy#replicate(Object, java.util.List, Object)}
 * method. 
 * 
 * @see ReplicationPolicy
 * 
 * @author Pinaki Poddar
 *
 */
@Target({ TYPE })
@Retention(RUNTIME)
public @interface Replicated {

}
