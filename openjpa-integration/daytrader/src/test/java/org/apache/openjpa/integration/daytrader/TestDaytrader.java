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

import org.apache.openjpa.persistence.OpenJPAEntityManagerFactorySPI;
import org.apache.openjpa.persistence.OpenJPAPersistence;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Uses Daytrader to stress test OpenJPA.
 *
 * @version $Rev$ $Date$
 */
public class TestDaytrader extends SingleEMFTestCase {

    OpenJPAEntityManagerFactorySPI emf = null;
    //private TradeJPADirect trade = null;
    
    @Override
    public void setUp() {
        super.setUp(DROP_TABLES, AccountDataBean.class,
            AccountProfileDataBean.class, HoldingDataBean.class,
            OrderDataBean.class, QuoteDataBean.class);
        if (emf == null) {
            emf = (OpenJPAEntityManagerFactorySPI)OpenJPAPersistence.createEntityManagerFactory(
                "daytrader", "persistence.xml");
            assertNotNull(emf);
        }
        //trade = new TradeJPADirect((EntityManagerFactory)emf);
    }
    
    /**
     * Scenario being tested:
     * @throws Exception 
     *   
     */
    public void newtestTradeBuildDB() throws Exception {
        getLog().info("testTradeBuildDB() started");
        assertNotNull(emf);
        TradeBuildDB tradeDB = new TradeBuildDB(getLog(), emf);
        getLog().info("testTradeBuildDB() completed");
    }

    public void testTradeBuildDB() {
        getLog().info("testTradeBuildDB() started");
        getLog().info("testTradeBuildDB() createQuotes(" + TradeConfig.getMAX_QUOTES() + ")");
        createQuotes(TradeConfig.getMAX_QUOTES());
        getLog().info("testTradeBuildDB() createAccounts(" + TradeConfig.getMAX_USERS() + ")");
        createAccounts(TradeConfig.getMAX_USERS()); // with 10 holdings each
        getLog().info("testTradeBuildDB() completed");
    }

    // from TradeBuildDB.TradeBuildDB()
    private void createQuotes(int num) {
        int errorCount = 0;
        String symbol, companyName;
        TradeConfig.setMAX_QUOTES(num);
        for (int i = 0; i < num; i++) {
            symbol = "s:" + i;
            companyName = "S" + i + " Incorporated";
            try {
                QuoteDataBean quoteData = createQuote(symbol, companyName,
                    new java.math.BigDecimal(TradeConfig.rndPrice()));
            } catch (Exception e) {
                if (errorCount++ >= 10) {
                    getLog().error("createQuotes - aborting after 10 create quote errors", e);
                    throw new RuntimeException(e);
                }
            }
        }

    }
    
    // from TradeJPADirect.createQuote()
    private QuoteDataBean createQuote(String symbol, String companyName, BigDecimal price) {
        EntityManager entityManager = emf.createEntityManager();
        QuoteDataBean quote = new QuoteDataBean(symbol, companyName, 0, price, price, price, price, 0);
        try {
            entityManager.getTransaction().begin();
            entityManager.persist(quote);
            entityManager.getTransaction().commit();
        }
        catch (Exception e) {
            getLog().error("createQuote - rollback - exception creating Quote", e);
            entityManager.getTransaction().rollback();
            throw new RuntimeException(e);
        } finally {
            entityManager.close();
        }
        if (getLog().isTraceEnabled())
            getLog().trace("createQuote-->" + quote);
        return quote;
    }

    // from TradeBuildDB.TradeBuildDB()
    private void createAccounts(int num) {
        TradeConfig.setMAX_USERS(num);
        for (int i = 0; i < num; i++) {
            String userID = "uid:" + i;
            String fullname = TradeConfig.rndFullName();
            String email = TradeConfig.rndEmail(userID);
            String address = TradeConfig.rndAddress();
            String creditcard = TradeConfig.rndCreditCard();
            double initialBalance = (double) (TradeConfig.rndInt(100000)) + 200000;
            if (i == 0) {
                initialBalance = 1000000; // uid:0 starts with a cool million.
            }
            
            AccountDataBean accountData = register(userID, "xxx", fullname, address,
                email, creditcard, new BigDecimal(initialBalance));

            String results, symbol;
            if (accountData != null) {
                // 0-MAX_HOLDING (inclusive), avg holdings per user = (MAX-0)/2
                // int holdings = TradeConfig.rndInt(TradeConfig.getMAX_HOLDINGS() + 1);
                int holdings = TradeConfig.getMAX_HOLDINGS();
                double quantity = 0;
                OrderDataBean orderData;
                for (int j = 0; j < holdings; j++) {
                    symbol = TradeConfig.rndSymbol();
                    quantity = TradeConfig.rndQuantity();
                    orderData = buy(userID, symbol, quantity, TradeConfig.orderProcessingMode);
                }
                if (getLog().isTraceEnabled())
                    getLog().trace("createAccounts - created " + holdings + " for userID=" + userID);
            } else {
                throw new RuntimeException("createAccounts - userID=" + userID + " already registered.");
            }
        }
    }
    
    // from TradeJPADirect.register()
    private AccountDataBean register(String userID, String password, String fullname, 
        String address, String email, String creditcard, BigDecimal openBalance) {
        AccountDataBean account = null;
        AccountProfileDataBean profile = null;
        EntityManager entityManager = emf.createEntityManager();

        // assume that profile with the desired userID doeesn't already exist
        profile = new AccountProfileDataBean(userID, password, fullname, address, email, creditcard);
        account = new AccountDataBean(0, 0, null, new Timestamp(System.currentTimeMillis()), openBalance, openBalance, userID);
        profile.setAccount(account);
        account.setProfile(profile);
        try {
            entityManager.getTransaction().begin();
            entityManager.persist(profile);
            entityManager.persist(account);
            entityManager.getTransaction().commit();
        } catch (Exception e) {
            getLog().error("createQuote - rollback - exception creating Quote", e);
            entityManager.getTransaction().rollback();
            throw new RuntimeException(e);
        } finally {
            entityManager.close();
        }
        if (getLog().isTraceEnabled())
            getLog().trace("register-->" + account);
        return account;
    }

    private OrderDataBean buy(String userID, String symbol, double quantity, int orderProcessingMode) {
        OrderDataBean order = null;
        BigDecimal total;
        EntityManager entityManager = emf.createEntityManager();
        assertNotNull(entityManager);

        try {
            if (getLog().isTraceEnabled())
                getLog().trace("buy-->userID=" + userID);

            entityManager.getTransaction().begin();

            AccountProfileDataBean profile = entityManager.find(AccountProfileDataBean.class, userID);
            assertNotNull(profile);
            AccountDataBean account = profile.getAccount();
            assertNotNull(account);

            QuoteDataBean quote = entityManager.find(QuoteDataBean.class, symbol);
            assertNotNull(quote);

            HoldingDataBean holding = null; // The holding will be created by this buy order

            order = createOrder(account, quote, holding, "buy", quantity, entityManager);
            assertNotNull(order);

            // order = createOrder(account, quote, holding, "buy", quantity);
            // UPDATE - account should be credited during completeOrder

            BigDecimal price = quote.getPrice();
            BigDecimal orderFee = order.getOrderFee();
            BigDecimal balance = account.getBalance();
            total = (new BigDecimal(quantity).multiply(price)).add(orderFee);
            account.setBalance(balance.subtract(total));

            // commit the transaction before calling completeOrder
            entityManager.getTransaction().commit();

            // if (orderProcessingMode == TradeConfig.SYNCH)
            completeOrder(order.getOrderID(), false);
        } catch (Exception e) {
            getLog().error("buy(" + userID + "," + symbol + "," + quantity + ") --> failed", e);
            // On exception - cancel the order
            // TODO figure out how to do this with JPA
            if (order != null)
                order.cancel();

            entityManager.getTransaction().rollback();
            entityManager.close();
            entityManager = null;

            // throw new EJBException(e);
            throw new RuntimeException(e);
        }
        if (entityManager != null) {
            entityManager.close();
            entityManager = null;
        }

        // after the purchase or sell of a stock, update the stocks volume and price
        updateQuotePriceVolume(symbol, TradeConfig.getRandomPriceChangeFactor(), quantity);

        return order;
    }

    private OrderDataBean createOrder(AccountDataBean account,
            QuoteDataBean quote, HoldingDataBean holding, String orderType,
            double quantity, EntityManager entityManager) {
        OrderDataBean order;
        if (getLog().isTraceEnabled())
            getLog().trace("createOrder(orderID=" + " account="
                + ((account == null) ? null : account.getAccountID())
                + " quote=" + ((quote == null) ? null : quote.getSymbol())
                + " orderType=" + orderType + " quantity=" + quantity);
        try {
            order = new OrderDataBean(orderType, 
                "open", 
                new Timestamp(System.currentTimeMillis()), 
                null, 
                quantity, 
                // quote.getPrice().setScale(FinancialUtils.SCALE, FinancialUtils.ROUND),
                quote.getPrice().setScale(2, BigDecimal.ROUND_HALF_UP),
                TradeConfig.getOrderFee(orderType), 
                account, 
                quote, 
                holding);
            entityManager.persist(order);
        } catch (Exception e) {
            getLog().error("createOrder - failed to create Order", e);
            throw new RuntimeException("createOrder - failed to create Order", e);
        }
        return order;
    }

    private OrderDataBean completeOrder(Integer orderID, boolean twoPhase) throws Exception {
        EntityManager entityManager = emf.createEntityManager();
        OrderDataBean order = null;

        if (getLog().isTraceEnabled())
            getLog().trace("completeOrder - orderID=" + orderID + " twoPhase=" + twoPhase);

        order = entityManager.find(OrderDataBean.class, orderID);
        assertNotNull(order);
        order.getQuote();
        if (order == null) {
            getLog().error("completeOrder - Unable to find Order " + orderID + " FBPK returned " + order);
            return null;
        }
        if (order.isCompleted()) {
            throw new RuntimeException("completeOrder - attempt to complete Order that is already completed\n" + order);
        }
        AccountDataBean account = order.getAccount();
        assertNotNull(account);
        QuoteDataBean quote = order.getQuote();
        assertNotNull(quote);
        HoldingDataBean holding = order.getHolding();
        BigDecimal price = order.getPrice();
        double quantity = order.getQuantity();

        String userID = account.getProfile().getUserID();
        assertNotNull(userID);

        if (getLog().isTraceEnabled())
            getLog().trace("completeOrder--> Completing Order "
                      + order.getOrderID() + "\n\t Order info: " + order
                      + "\n\t Account info: " + account + "\n\t Quote info: "
                      + quote + "\n\t Holding info: " + holding);

        HoldingDataBean newHolding = null;
        if (order.isBuy()) {
            newHolding = createHolding(account, quote, quantity, price, entityManager);
            assertNotNull(newHolding);
        }
        try {
            entityManager.getTransaction().begin();
            if (newHolding != null) {
                order.setHolding(newHolding);
            }
            if (order.isSell()) {
                if (holding == null) {
                    getLog().error("completeOrder - error " + order.getOrderID() + " holding already sold");
                    order.cancel();
                    entityManager.getTransaction().commit();
                    return order;
                }
                else {
                    entityManager.remove(holding);
                    order.setHolding(null);
                }
            }
            order.setOrderStatus("closed");
            order.setCompletionDate(new java.sql.Timestamp(System.currentTimeMillis()));
            if (getLog().isTraceEnabled())
                getLog().trace("completeOrder--> Completed Order "
                          + order.getOrderID() + "\n\t Order info: " + order
                          + "\n\t Account info: " + account + "\n\t Quote info: "
                          + quote + "\n\t Holding info: " + holding);
            entityManager.getTransaction().commit();
        }
        catch (Exception e) {
            getLog().error(e);
            entityManager.getTransaction().rollback();
            entityManager.close();
            entityManager = null;
        }
        if (entityManager != null) {
            entityManager.close();
            entityManager = null;
        }
        return order;
    }

    private HoldingDataBean createHolding(AccountDataBean account,
            QuoteDataBean quote, double quantity, BigDecimal purchasePrice,
            EntityManager entityManager) throws Exception {

        HoldingDataBean newHolding = new HoldingDataBean(quantity,
            purchasePrice, new Timestamp(System.currentTimeMillis()),
            account, quote);
        assertNotNull(newHolding);
        try {
            entityManager.getTransaction().begin();
            entityManager.persist(newHolding);
            entityManager.getTransaction().commit();
        } catch (Exception e) {
            entityManager.getTransaction().rollback();
        }
        return newHolding;
    }

    public QuoteDataBean updateQuotePriceVolume(String symbol, BigDecimal changeFactor, double sharesTraded) {
        if (!TradeConfig.getUpdateQuotePrices())
            return new QuoteDataBean();
        if (getLog().isTraceEnabled())
            getLog().trace("updateQuote - symbol=" + symbol + " changeFactor=" + changeFactor);

        EntityManager entityManager = emf.createEntityManager();
        QuoteDataBean quote = null;
        if (TradeConfig.jpaLayer == TradeConfig.HIBERNATE) {
            quote = entityManager.find(QuoteDataBean.class, symbol);
        } else if (TradeConfig.jpaLayer == TradeConfig.OPENJPA) {
            Query q = entityManager.createNamedQuery("quoteejb.quoteForUpdate");
            q.setParameter(1, symbol);
            quote = (QuoteDataBean) q.getSingleResult();
        }
        BigDecimal oldPrice = quote.getPrice();
        if (quote.getPrice().equals(TradeConfig.PENNY_STOCK_PRICE)) {
            changeFactor = TradeConfig.PENNY_STOCK_RECOVERY_MIRACLE_MULTIPLIER;
        }
        BigDecimal newPrice = changeFactor.multiply(oldPrice).setScale(2, BigDecimal.ROUND_HALF_UP);

        try {
            quote.setPrice(newPrice);
            quote.setVolume(quote.getVolume() + sharesTraded);
            quote.setChange((newPrice.subtract(quote.getOpen()).doubleValue()));

            entityManager.getTransaction().begin();
            entityManager.merge(quote);
            entityManager.getTransaction().commit();
        } catch (Exception e) {
            entityManager.getTransaction().rollback();
        } finally {
            if (entityManager != null) {
                entityManager.close();
                entityManager = null;
            }
        }
        this.publishQuotePriceChange(quote, oldPrice, changeFactor, sharesTraded);
        return quote;
    }
    
    private void publishQuotePriceChange(QuoteDataBean quote, BigDecimal oldPrice, 
            BigDecimal changeFactor, double sharesTraded) {
        if (!TradeConfig.getPublishQuotePriceChange())
            return;
        getLog().error("publishQuotePriceChange - is not implemented for this runtime mode");
        throw new UnsupportedOperationException("publishQuotePriceChange - is not implemented for this runtime mode");
    }

}
