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
package org.apache.openjpa.persistence.callbacks;

import jakarta.persistence.PostLoad;

/**
 * Entity listener that has @PostLoad annotation on the postLoad method.
 * The corresponding orm.xml also declares &lt;post-load method-name="postLoad"/&gt;.
 * Per JPA spec, the XML should override the annotation, resulting in only
 * one invocation, not two.
 */
public class XmlCallbackListener {

    @PostLoad
    public void postLoad(Object entity) {
        if (entity instanceof XmlCallbackEntity) {
            ((XmlCallbackEntity) entity).addPostLoadCall("XmlCallbackListener");
        }
    }
}
