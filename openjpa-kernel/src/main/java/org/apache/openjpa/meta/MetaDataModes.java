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
package org.apache.openjpa.meta;

/**
 * Mode constants used to track the initialization status of metadata.
 * These constants can be used as bit flags.
 *
 * @author Abe White
 * @since 0.4.0
 */
public interface MetaDataModes {

    int MODE_NONE = 0;
    int MODE_META = 1;
    int MODE_MAPPING = 2;
    int MODE_QUERY = 4;
    int MODE_MAPPING_INIT = 8;
    int MODE_ANN_MAPPING = 16;

    int MODE_ALL = MODE_META | MODE_MAPPING | MODE_QUERY
        | MODE_MAPPING_INIT | MODE_ANN_MAPPING;
}
