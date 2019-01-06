-- Licensed to the Apache Software Foundation (ASF) under one
-- or more contributor license agreements.  See the NOTICE file
-- distributed with this work for additional information
-- regarding copyright ownership.  The ASF licenses this file
-- to you under the Apache License, Version 2.0 (the
-- "License"); you may not use this file except in compliance
-- with the License.  You may obtain a copy of the License at
--
-- http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing,
-- software distributed under the License is distributed on an
-- "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
-- KIND, either express or implied.  See the License for the
-- specific language governing permissions and limitations
-- under the License.

-- ***********************************************************
-- Init script for creating a work user schema for running
-- tests against our oracle Docker image.
-- See the 'test-oracle-docker' Maven Profile
-- ***********************************************************


CREATE USER openjpatst IDENTIFIED BY openjpatst;
GRANT CONNECT, RESOURCE, DBA TO openjpatst;
GRANT CREATE SESSION GRANT ANY PRIVILEGE TO openjpatst;
GRANT UNLIMITED TABLESPACE TO  openjpatst;