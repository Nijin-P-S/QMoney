package com.nijin.QMoney.quotes;



import com.nijin.QMoney.dto.Candle;
import com.nijin.QMoney.exception.StockQuoteServiceException;

import java.time.LocalDate;
import java.util.List;

public interface StockQuotesService {

  List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to)
          throws StockQuoteServiceException;

}
