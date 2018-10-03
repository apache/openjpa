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
package org.apache.openjpa.kernel.exps;

/**
 * Interface for any literal value.
 *
 * @author Abe White
 */
public interface Literal
    extends Value, Constant {

    int TYPE_UNKNOWN = 0;
    int TYPE_NUMBER = 1;
    int TYPE_BOOLEAN = 2;
    int TYPE_STRING = 3;
    int TYPE_SQ_STRING = 4; // single-quoted string
    int TYPE_CLASS = 5;
    int TYPE_ENUM = 6;
    int TYPE_COLLECTION = 7;
    int TYPE_DATE = 8;
    int TYPE_TIME = 9;
    int TYPE_TIMESTAMP = 10;

    /**
     * The value of this literal.
     */
    Object getValue();

    /**
     * The value of this literal.
     */
    void setValue(Object val);

    /**
     * The type the literal was parsed as.
     */
    int getParseType();
}

