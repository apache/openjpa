#!/bin/bash
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#

# Init script of the 'test-oracle-docker' Maven profile, run once by the entrypoint of the
# gvenzl/oracle-free image when the database is created, before it reports DATABASE IS READY TO USE.
# The test suite opens more connections than the image allows by default (processes=200) and then
# fails with ORA-12516/ORA-12519, so processes and sessions are raised and the database is restarted.
sqlplus -s / as sysdba <<SQL
ALTER SYSTEM SET processes=1000 SCOPE=SPFILE;
ALTER SYSTEM SET sessions=1600 SCOPE=SPFILE;
SHUTDOWN IMMEDIATE;
STARTUP;
ALTER PLUGGABLE DATABASE ALL OPEN;
exit;
SQL
