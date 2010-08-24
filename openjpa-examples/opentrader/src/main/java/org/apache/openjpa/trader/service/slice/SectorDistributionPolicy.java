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
package org.apache.openjpa.trader.service.slice;

import java.util.List;

import org.apache.openjpa.slice.DistributionPolicy;
import org.apache.openjpa.trader.domain.Stock;
import org.apache.openjpa.trader.domain.Tradable;
import org.apache.openjpa.trader.domain.Trader;

/**
 * Distributes each persistent domain instances of OpenTrader model into specific slice
 * based on the sector.
 * 
 * @author Pinaki Poddar
 *
 */
public class SectorDistributionPolicy implements DistributionPolicy {
    /**
     * This distribution policy determines the sector of the stock and
     * picks the slice of the given list of slices at ordinal index of the
     * enumerated Sector.
     */
    public String distribute(Object pc, List<String> slices, Object context) {
        Stock stock = null;
        if (pc instanceof Tradable) {
            stock = ((Tradable)pc).getStock();
        } else if (pc instanceof Stock) {
            stock = (Stock)pc;
        } else if (pc instanceof Trader) {
            throw new IllegalArgumentException("Trader should have been replicated");
        }
//        if (stock == null) {
//            throw new IllegalStateException(pc + "(" + pc.getClass() + ") is not associated with a Stock");
//        }
        return stock != null ? slices.get(stock.getSector().ordinal()) : null;
    }

}
