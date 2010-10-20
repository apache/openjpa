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
package org.apache.openjpa.integration.daytrader;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Query;

// import org.apache.geronimo.samples.daytrader.beans.AccountDataBean;
// import org.apache.geronimo.samples.daytrader.beans.AccountProfileDataBean;
// import org.apache.geronimo.samples.daytrader.beans.HoldingDataBean;
// import org.apache.geronimo.samples.daytrader.beans.OrderDataBean;
// import org.apache.geronimo.samples.daytrader.beans.QuoteDataBean;
// import org.apache.geronimo.samples.daytrader.core.FinancialUtils;
// import org.apache.geronimo.samples.daytrader.util.TradeConfig;
// import org.apache.geronimo.samples.daytrader.web.TradeBuildDB;

import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactorySPI;
import org.apache.openjpa.persistence.test.AbstractPersistenceTestCase;
//import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Uses a modified version of Apache Geronimo Daytrader to stress test OpenJPA.
 *
 * @version $Rev$ $Date$
 */
public class TestDaytrader extends AbstractPersistenceTestCase {
    private static final int TEST_USERS = 50;
    OpenJPAEntityManagerFactorySPI emf = null;
    Log log = null;
    private TradeAction trade = null;
    
    @Override
    public void setUp() {
        /*
        super.setUp(DROP_TABLES, AccountDataBean.class,
            AccountProfileDataBean.class, HoldingDataBean.class,
            OrderDataBean.class, QuoteDataBean.class);
        if (emf == null) {
            emf = (OpenJPAEntityManagerFactorySPI)OpenJPAPersistence.createEntityManagerFactory(
                "daytrader", "persistence.xml");
            assertNotNull(emf);
        }
        */
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("openjpa.DynamicEnhancementAgent", "false");
        map.put("openjpa.jdbc.SynchronizeMappings", "buildSchema(ForeignKeys=true,"
            + "SchemaAction='add,deleteTableContents')");
        emf = createNamedEMF("daytrader", map);
        assertNotNull(emf);
        log = emf.getConfiguration().getLog("test");
        TradeConfig.setRunTimeMode(TradeConfig.JPA);
        TradeConfig.setLog(log);
        trade = new TradeAction(emf.getConfiguration().getLog("test"), emf);
    }
    
    @Override
    public void tearDown() throws Exception {
        trade = null;
        log = null;
        closeEMF(emf);
        emf = null;
        super.tearDown();
    }
    
    /**
     * Scenario being tested:
     *   - Creates 1000 quotes
     *   - Creates 500 user accounts w/ 10 holdings each
     *   - 
     * @throws Exception 
     *   
     */
    public void testTrade() throws Exception {
        log.info("TestDaytrader.testTrade() started");
        // setup quotes, accounts and holdings in DB
        TradeBuildDB tradeDB = new TradeBuildDB(log, trade);
        tradeDB.setup(TradeConfig.getMAX_QUOTES(), TradeConfig.getMAX_USERS());
        // perform some operations per user
        TradeScenario scenario = new TradeScenario(trade);
        log.info("TestDaytrader.testTrade() calling TradeScenario.performUserTasks(" + TEST_USERS + ")");
        for (int i = 0; i < TEST_USERS; i++) {
            String userID = "uid:" + i;
            if (scenario.performUserTasks(userID) == false) {
                fail("TestDaytrader.testTrade() call to TradeScenario.performUserTask(" + userID + ") failed");
            }
        }
        log.info("TestDaytrader.testTrade() completed");
    }

}
