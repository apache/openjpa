package org.apache.openjpa.trader.service.slice;

import java.util.List;
import java.util.Map;

import org.apache.openjpa.slice.QueryTargetPolicy;
import org.apache.openjpa.trader.domain.Stock;
import org.apache.openjpa.trader.domain.Tradable;
import org.apache.openjpa.trader.service.TradingService;

/**
 * An example of a {@link QueryTargetPolicy query target policy} that directs the query based
 * on its parameters.
 * 
 * @author Pinaki Poddar
 *
 */
public class SectorBasedQueryTargetPolicy implements QueryTargetPolicy {

	@Override
	public String[] getTargets(String query, Map<Object, Object> params,
			String language, List<String> slices, Object context) {        
		Stock stock = null;
		if (TradingService.MATCH_ASK.equals(query)) {
			stock = ((Tradable)params.get("ask")).getStock();
		} else if (TradingService.MATCH_BID.equals(query)) {
			stock = ((Tradable)params.get("bid")).getStock();
		}
        return stock != null ? new String[]{slices.get(stock.getSector().ordinal())} : null;
	}

}
