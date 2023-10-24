package com.nijin.QMoney.portfolio;


import com.nijin.QMoney.dto.AnnualizedReturn;
import com.nijin.QMoney.dto.Candle;
import com.nijin.QMoney.dto.PortfolioTrade;
import com.nijin.QMoney.exception.StockQuoteServiceException;
import com.nijin.QMoney.quotes.StockQuotesService;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static java.time.temporal.ChronoUnit.DAYS;

public class PortfolioManagerImpl implements PortfolioManager {

  protected RestTemplate restTemplate;
  protected StockQuotesService stockQuotesService;



  // Caution: Do not delete or modify the constructor, or else your build will break!
  // This is absolutely necessary for backward compatibility

  @Deprecated
  protected PortfolioManagerImpl(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  public PortfolioManagerImpl(StockQuotesService stockQuotesService) {
    this.stockQuotesService = stockQuotesService;
  }

  private Comparator<AnnualizedReturn> getComparator() {
    return Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed();
  }

  //CHECKSTYLE:OFF

  // TODO: CRIO_TASK_MODULE_REFACTOR
  //  Extract the logic to call Tiingo third-party APIs to a separate function.
  //  Remember to fill out the buildUri function and use that.


  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to)
          throws StockQuoteServiceException {
    // String tiingoRestURL = buildUri(symbol, from, to);
    // TiingoCandle[] tiingoCandleArray = restTemplate.getForObject(tiingoRestURL, TiingoCandle[].class);
    // if(tiingoCandleArray == null){
    //   return new ArrayList<Candle>();
    // }else {
    //   return Arrays.stream(tiingoCandleArray).collect(Collectors.toList());
    // }
    return stockQuotesService.getStockQuote(symbol, from, to);
  }

  // public static String getToken() {
  //   //e3066a87e006a0e44b49c3fd79ba797366c01178 289464e8faf5cf34aba42001442fb59b3c854b6c
  //   return "e3066a87e006a0e44b49c3fd79ba797366c01178";
  // }

  // protected String buildUri(String symbol, LocalDate startDate, LocalDate endDate) {
  //   return "https://api.tiingo.com/tiingo/daily/" + symbol + "/prices?startDate=" + startDate
  //   + "&endDate=" + endDate + "&token=" + getToken();
  // }


  private Double getOpeningPriceOnStartDate(List<Candle> candles) {
    return candles.get(0).getOpen();
  }

  private Double getClosingPriceOnEndDate(List<Candle> candles) {
    return candles.get(candles.size() - 1).getClose();
  }

  private AnnualizedReturn calculateAnnualizedReturns(LocalDate endDate, PortfolioTrade trade,
                                                      Double buyPrice, Double sellPrice) {
    double total_num_years = DAYS.between(trade.getPurchaseDate(), endDate) / 365.2422;
    double totalReturns = (sellPrice - buyPrice) / buyPrice;
    double annualized_returns = Math.pow((1.0 + totalReturns), (1.0 / total_num_years)) - 1;
    return new AnnualizedReturn(trade.getSymbol(), annualized_returns, totalReturns);
  }

  //TODO: CRIO_TASK_MODULE_REFACTOR
  // 1. Now we want to convert our code into a module, so we will not call it from main anymore.
  //    Copy your code from Module#3 PortfolioManagerApplication#calculateAnnualizedReturn
  //    into #calculateAnnualizedReturn function here and ensure it follows the method signature.
  // 2. Logic to read Json file and convert them into Objects will not be required further as our
  //    clients will take care of it, going forward.

  // Note:
  // Make sure to exercise the tests inside PortfolioManagerTest using command below:
  // ./gradlew test --tests PortfolioManagerTest

  //CHECKSTYLE:OFF

  @Override
  public List<AnnualizedReturn> calculateAnnualizedReturn(List<PortfolioTrade> portfolioTrades, LocalDate endDate)
          throws StockQuoteServiceException {
    List<AnnualizedReturn> annualizedReturns = new ArrayList<>();
    for (PortfolioTrade portfolioTrade : portfolioTrades) {
      List<Candle> candles = getStockQuote(portfolioTrade.getSymbol(), portfolioTrade.getPurchaseDate(), endDate);
      AnnualizedReturn annualizedReturn = calculateAnnualizedReturns(endDate, portfolioTrade,
              getOpeningPriceOnStartDate(candles), getClosingPriceOnEndDate(candles));
      annualizedReturns.add(annualizedReturn);
    }
    return annualizedReturns.stream().sorted(getComparator()).collect(Collectors.toList());
  }


  @Override
  public List<AnnualizedReturn> calculateAnnualizedReturnParallel(
          List<PortfolioTrade> portfolioTrades, LocalDate endDate, int numThreads)
          throws StockQuoteServiceException {

    ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
    List<AnnualizedReturn> annualizedReturns = new ArrayList<>();
    List<AnnualizedReturnTask> annualizedReturnTaskList = new ArrayList<>();
    List<Future<AnnualizedReturn>> annualizedReturnFutureList = null;
    for (PortfolioTrade portfolioTrade : portfolioTrades)
      annualizedReturnTaskList.add(new AnnualizedReturnTask(portfolioTrade, stockQuotesService, endDate));
    try {
      annualizedReturnFutureList = executorService.invokeAll(annualizedReturnTaskList);
    } catch (InterruptedException e) {
      throw new StockQuoteServiceException(e.getMessage());
    }
    for (Future<AnnualizedReturn> annualizedReturnFuture : annualizedReturnFutureList) {
      try {
        annualizedReturns.add(annualizedReturnFuture.get());
      } catch (InterruptedException | ExecutionException e) {
        throw new StockQuoteServiceException(e.getMessage());
      }
    }
    executorService.shutdown();
    return annualizedReturns.stream().sorted(getComparator()).collect(Collectors.toList());
  }

}
