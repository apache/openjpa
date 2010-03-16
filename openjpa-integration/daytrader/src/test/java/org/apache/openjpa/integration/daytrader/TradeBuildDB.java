/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.openjpa.integration.daytrader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;

import javax.persistence.EntityManagerFactory;

import org.apache.openjpa.lib.log.Log;

// import org.apache.geronimo.samples.daytrader.core.*;
// import org.apache.geronimo.samples.daytrader.core.direct.*;
// import org.apache.geronimo.samples.daytrader.beans.*;
// import org.apache.geronimo.samples.daytrader.util.*;

/**
 * TradeBuildDB uses operations provided by the TradeApplication to 
 *   (a) create the Database tables 
 *   (b) populate a DayTrader database without creating the tables. 
 * Specifically, a new DayTrader User population is created using
 * UserIDs of the form "uid:xxx" where xxx is a sequential number 
 * (e.g. uid:0, uid:1, etc.). New stocks are also created of the form "s:xxx",
 * again where xxx represents sequential numbers (e.g. s:1, s:2, etc.)
 */
public class TradeBuildDB {

    private TradeConfig tCfg = new TradeConfig();
    private TradeJPADirect trade = null;
    private Log log = null;

    /**
     * Re-create the DayTrader db tables and populate them OR just populate a 
     * DayTrader DB, logging to the provided output stream
     */
    public TradeBuildDB(Log log, EntityManagerFactory emf) throws Exception {
        this.log = log;
        // update config
        tCfg.setRunTimeMode(TradeConfig.JPA);
        tCfg.setLog(log);
        
        // always use TradeJPADirect mode
        trade = new TradeJPADirect(log, emf);

        // removed - createDBTables

        // removed - Attempt to delete all of the Trade users and Trade Quotes first
        
        // create MAX_QUOTES
        createQuotes();

        // create MAX_USERS
        createAccounts();
    }

    private void createQuotes() {
        int errorCount = 0;
        String symbol, companyName;
        log.info("TradeBuildDB.createQuotes(" + TradeConfig.getMAX_QUOTES() + ")");
        for (int i = 0; i < TradeConfig.getMAX_QUOTES(); i++) {
            symbol = "s:" + i;
            companyName = "S" + i + " Incorporated";
            try {
                QuoteDataBean quoteData = trade.createQuote(symbol, companyName,
                    new java.math.BigDecimal(TradeConfig.rndPrice()));
            } catch (Exception e) {
                if (errorCount++ >= 10) {
                    log.error("createQuotes - aborting after 10 create quote errors", e);
                    throw new RuntimeException(e);
                }
            }
        }

    }
    
    private void createAccounts() {
        log.info("TradeBuildDB.createAccounts(" + TradeConfig.getMAX_USERS() + ")");
        for (int i = 0; i < TradeConfig.getMAX_USERS(); i++) {
            String userID = "uid:" + i;
            String fullname = TradeConfig.rndFullName();
            String email = TradeConfig.rndEmail(userID);
            String address = TradeConfig.rndAddress();
            String creditcard = TradeConfig.rndCreditCard();
            double initialBalance = (double) (TradeConfig.rndInt(100000)) + 200000;
            if (i == 0) {
                initialBalance = 1000000; // uid:0 starts with a cool million.
            }
            
            AccountDataBean accountData = trade.register(userID, "xxx", fullname, address,
                email, creditcard, new BigDecimal(initialBalance));

            String symbol;
            if (accountData != null) {
                // 0-MAX_HOLDING (inclusive), avg holdings per user = (MAX-0)/2
                // int holdings = TradeConfig.rndInt(TradeConfig.getMAX_HOLDINGS() + 1);
                int holdings = TradeConfig.getMAX_HOLDINGS();
                double quantity = 0;
                OrderDataBean orderData = null;
                for (int j = 0; j < holdings; j++) {
                    symbol = TradeConfig.rndSymbol();
                    quantity = TradeConfig.rndQuantity();
                    orderData = trade.buy(userID, symbol, quantity, TradeConfig.orderProcessingMode);
                }
                if (log.isTraceEnabled()) {
                    log.trace("createAccounts - created " + holdings + " for userID=" + userID + " order=" + orderData);
                }
            } else {
                throw new RuntimeException("createAccounts - userID=" + userID + " already registered.");
            }
        }
    }

}
