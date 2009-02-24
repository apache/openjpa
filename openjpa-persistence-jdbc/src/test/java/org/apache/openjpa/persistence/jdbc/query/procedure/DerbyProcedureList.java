
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
package org.apache.openjpa.persistence.jdbc.query.procedure;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

/*
 * holds the stored procedures that will be used by test cases
 */
public class DerbyProcedureList extends ProcedureList {

    public List<String> getCreateProcedureList () {
        ArrayList<String> retList = new ArrayList<String>();

        retList.add ("create procedure ADD_X_TO_CHARLIE () " +
                     "PARAMETER STYLE JAVA LANGUAGE JAVA MODIFIES SQL DATA " +
                     "EXTERNAL NAME 'org.apache.openjpa.persistence.jdbc." + 
                     "query.procedure.DerbyProcedureList.addXToCharlie'");
        

        return retList;
    }

    public List<String> getDropProcedureList () {
        ArrayList<String> retList = new ArrayList<String>();

        retList.add ("drop procedure ADD_X_TO_CHARLIE");

        return retList;
    }

    public String callAddXToCharlie () {
        return "{ call ADD_X_TO_CHARLIE () }";
    }

    public static void addXToCharlie () throws Exception {
        Connection conn = DriverManager.getConnection(
            "jdbc:default:connection");
        PreparedStatement ps1 = conn.prepareStatement("update APPLICANT set " + 
            "name = 'Charliex' where name = 'Charlie'");
        ps1.executeUpdate();

        conn.close();
    }
}
