package com.nijin.QMoney.quotes;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nijin.QMoney.dto.Candle;
import com.nijin.QMoney.dto.TiingoCandle;
import com.nijin.QMoney.exception.StockQuoteServiceException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class TiingoService implements StockQuotesService {


  private RestTemplate restTemplate;

  protected TiingoService(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }


  // TODO: CRIO_TASK_MODULE_ADDITIONAL_REFACTOR
  //  Implement getStockQuote method below that was also declared in the interface.

  @Override
  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to)
          throws StockQuoteServiceException {
    String tiingoURL = buildUri(symbol, from, to);
    String responseString=null;
    try {
      responseString = restTemplate.getForObject(tiingoURL, String.class);
    } catch (HttpClientErrorException e) {
      throw new StockQuoteServiceException("TooManyRequests: 429 Unknown Status Code");
    }

    TiingoCandle[] tiingoCandleArray;
    try {
      tiingoCandleArray = getObjectMapper().readValue(responseString, TiingoCandle[].class);
      if (tiingoCandleArray == null || responseString == null)
        throw new StockQuoteServiceException("Tiingo Response Invalid Return");
    } catch (JsonProcessingException e) {
      throw new StockQuoteServiceException(e.getMessage());
    }
    return Arrays.stream(tiingoCandleArray).sorted(Comparator.comparing(Candle::getDate))
            .collect(Collectors.toList());
  }


  // Note:
  // 1. You can move the code from PortfolioManagerImpl#getStockQuote inside newly created method.
  // 2. Run the tests using command below and make sure it passes.
  //    ./gradlew test --tests TiingoServiceTest


  //CHECKSTYLE:OFF

  // TODO: CRIO_TASK_MODULE_ADDITIONAL_REFACTOR
  //  Write a method to create appropriate url to call the Tiingo API.

  public static String getToken() {
    //e3066a87e006a0e44b49c3fd79ba797366c01178 289464e8faf5cf34aba42001442fb59b3c854b6c
    return "e3066a87e006a0e44b49c3fd79ba797366c01178";
  }

  protected String buildUri(String symbol, LocalDate startDate, LocalDate endDate) {
    return "https://api.tiingo.com/tiingo/daily/" + symbol + "/prices?startDate=" + startDate
            + "&endDate=" + endDate + "&token=" + getToken();
  }

  private static ObjectMapper getObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    return objectMapper;
  }


}
