package com.nijin.QMoney.quotes;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nijin.QMoney.dto.AlphavantageCandle;
import com.nijin.QMoney.dto.AlphavantageDailyResponse;
import com.nijin.QMoney.dto.Candle;
import com.nijin.QMoney.exception.StockQuoteServiceException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AlphavantageService implements StockQuotesService {

    protected RestTemplate restTemplate;

    public AlphavantageService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // TODO: CRIO_TASK_MODULE_ADDITIONAL_REFACTOR
    //  Implement the StockQuoteService interface as per the contracts. Call Alphavantage service
    //  to fetch daily adjusted data for last 20 years.
    //  Refer to documentation here: https://www.alphavantage.co/documentation/
    //  --
    //  The implementation of this functions will be doing following tasks:
    //    1. Build the appropriate url to communicate with third-party.
    //       The url should consider startDate and endDate if it is supported by the provider.
    //    2. Perform third-party communication with the url prepared in step#1
    //    3. Map the response and convert the same to List<Candle>
    //    4. If the provider does not support startDate and endDate, then the implementation
    //       should also filter the dates based on startDate and endDate. Make sure that
    //       result contains the records for for startDate and endDate after filtering.
    //    5. Return a sorted List<Candle> sorted ascending based on Candle#getDate
    //  IMP: Do remember to write readable and maintainable code, There will be few functions like
    //    Checking if given date falls within provided date range, etc.
    //    Make sure that you write Unit tests for all such functions.
    //  Note:
    //  1. Make sure you use {RestTemplate#getForObject(URI, String)} else the test will fail.
    //  2. Run the tests using command below and make sure it passes:
    //    ./gradlew test --tests AlphavantageServiceTest
    //CHECKSTYLE:OFF

    @Override
    public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to)
            throws StockQuoteServiceException {
        String responseString = restTemplate.getForObject(buildUri(symbol), String.class);

        AlphavantageDailyResponse alphavantageDailyResponse;
        try {
            alphavantageDailyResponse =
                    getObjectMapper().readValue(responseString, AlphavantageDailyResponse.class);
            if (alphavantageDailyResponse.getCandles() == null || responseString == null)
                throw new StockQuoteServiceException("Alphavantage Response Invalid Return");
        } catch (JsonProcessingException e) {
            throw new StockQuoteServiceException(e.getMessage());
        }
        List<Candle> alphavantageCandles = new ArrayList<>();
        Map<LocalDate, AlphavantageCandle> mapOFDateAndAlphavantageCandle =
                alphavantageDailyResponse.getCandles();
        for (LocalDate localDate : mapOFDateAndAlphavantageCandle.keySet()) {
            if (localDate.isAfter(from.minusDays(1)) && localDate.isBefore(to.plusDays(1))) {
                AlphavantageCandle alphavantageCandle =
                        alphavantageDailyResponse.getCandles().get(localDate);
                alphavantageCandle.setDate(localDate);
                alphavantageCandles.add(alphavantageCandle);
            }
        }
        return alphavantageCandles.stream().sorted(Comparator.comparing(Candle::getDate))
                .collect(Collectors.toList());
    }
    //CHECKSTYLE:ON
    // TODO: CRIO_TASK_MODULE_ADDITIONAL_REFACTOR
    //  1. Write a method to create appropriate url to call Alphavantage service. The method should
    //     be using configurations provided in the {@link @application.properties}.
    //  2. Use this method in #getStockQuote.

    //https://www.alphavantage.co/query?function=TIME_SERIES_DAILY_ADJUSTED&symbol=GOOG&outputsize=full&apikey=

    protected String buildUri(String symbol) {
        String uriTemplate = "https://www.alphavantage.co/query?function=TIME_SERIES_DAILY_ADJUSTED&symbol="
                + symbol + "&outputsize=full&apikey=" + getToken();
        return uriTemplate;
    }

    protected String getToken() {
        return "LJHAA16M4WMSCWSQ";
    }

    private static ObjectMapper getObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper;
    }

    // TODO: CRIO_TASK_MODULE_EXCEPTIONS
    //   1. Update the method signature to match the signature change in the interface.
    //   2. Start throwing new StockQuoteServiceException when you get some invalid response from
    //      Alphavantage, or you encounter a runtime exception during Json parsing.
    //   3. Make sure that the exception propagates all the way from PortfolioManager, so that the
    //      external user's of our API are able to explicitly handle this exception upfront.
    //CHECKSTYLE:OFF

}
