package com.nijin.QMoney.portfolio;

import com.nijin.QMoney.quotes.StockQuoteServiceFactory;
import org.springframework.web.client.RestTemplate;

public class PortfolioManagerFactory {

    public static PortfolioManager getPortfolioManager(RestTemplate restTemplate) {
        return getPortfolioManager("",restTemplate);
    }

    public static PortfolioManager getPortfolioManager(String provider, RestTemplate restTemplate) {
        return new PortfolioManagerImpl(StockQuoteServiceFactory.INSTANCE.getService(provider, restTemplate));
    }


}
