
package com.nijin.QMoney.portfolio;



import com.nijin.QMoney.dto.AnnualizedReturn;
import com.nijin.QMoney.dto.PortfolioTrade;
import com.nijin.QMoney.exception.StockQuoteServiceException;

import java.time.LocalDate;
import java.util.List;

public interface PortfolioManager {

  List<AnnualizedReturn> calculateAnnualizedReturnParallel(
          List<PortfolioTrade> portfolioTrades, LocalDate endDate, int numThreads)
          throws InterruptedException, StockQuoteServiceException;

  //CHECKSTYLE:OFF

  List<AnnualizedReturn> calculateAnnualizedReturn(List<PortfolioTrade> portfolioTrades,
                                                   LocalDate endDate)
          throws StockQuoteServiceException
          ;
}

