package com.nijin.QMoney;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nijin.QMoney.dto.*;
import com.nijin.QMoney.log.UncaughtExceptionHandler;
import com.nijin.QMoney.portfolio.PortfolioManager;
import com.nijin.QMoney.portfolio.PortfolioManagerFactory;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;


public class PortfolioManagerApplication {

    public static RestTemplate restTemplate = new RestTemplate();
    public static PortfolioManager portfolioManager = PortfolioManagerFactory.getPortfolioManager(restTemplate);

    // TODO: CRIO_TASK_MODULE_REFACTOR
    //  Once you are done with the implementation inside PortfolioManagerImpl and
    //  PortfolioManagerFactory, create PortfolioManager using PortfolioManagerFactory.
    //  Refer to the code from previous modules to get the List<PortfolioTrades> and endDate, and
    //  call the newly implemented method in PortfolioManager to calculate the annualized returns.

    // Note:
    // Remember to confirm that you are getting same results for annualized returns as in Module 3.

    public static List<AnnualizedReturn> mainCalculateReturnsAfterRefactor(String[] args) throws Exception {
        String file = args[0];
        LocalDate endDate = LocalDate.parse(args[1]);
        String contents = readFileAsString(file);
        ObjectMapper objectMapper = getObjectMapper();
        PortfolioTrade[] portfolioTrades = objectMapper.readValue(contents, PortfolioTrade[].class);
        return portfolioManager.calculateAnnualizedReturn(Arrays.asList(portfolioTrades), endDate);
    }


    private static String readFileAsString(String fileName) throws IOException, URISyntaxException {
        return new String(Files.readAllBytes(resolveFileFromResources(fileName).toPath()), "UTF-8");
    }

    static Double getOpeningPriceOnStartDate(List<Candle> candles) {
        return candles.get(0).getOpen();
    }

    public static Double getClosingPriceOnEndDate(List<Candle> candles) {
        return candles.get(candles.size() - 1).getClose();
    }

    public static List<Candle> fetchCandles(PortfolioTrade trade, LocalDate endDate, String token) {
        RestTemplate restTemplate = new RestTemplate();
        String tiingoRestURL = prepareUrl(trade, endDate, token);
        TiingoCandle[] tiingoCandleResults = restTemplate.getForObject(tiingoRestURL, TiingoCandle[].class);
        return Arrays.stream(tiingoCandleResults).collect(Collectors.toList());
    }

    public static List<AnnualizedReturn> mainCalculateSingleReturn(String[] args) throws IOException, URISyntaxException {
        List<PortfolioTrade> portfolioTrades = readTradesFromJson(args[0]);
        List<AnnualizedReturn> annualizedReturns = new ArrayList<>();
        LocalDate localDate = LocalDate.parse(args[1]);
        for (PortfolioTrade portfolioTrade : portfolioTrades) {
            List<Candle> candles = fetchCandles(portfolioTrade, localDate, getToken());
            AnnualizedReturn annualizedReturn = calculateAnnualizedReturns(localDate, portfolioTrade,
                    getOpeningPriceOnStartDate(candles), getClosingPriceOnEndDate(candles));
            annualizedReturns.add(annualizedReturn);
        }
        return annualizedReturns.stream()
                .sorted((a1, a2) -> Double.compare(a2.getAnnualizedReturn(), a1.getAnnualizedReturn())) //descending order
                .collect(Collectors.toList());
    }

    public static AnnualizedReturn calculateAnnualizedReturns(LocalDate endDate, PortfolioTrade trade, Double buyPrice, Double sellPrice) {
        double totalNumberYears = ChronoUnit.DAYS.between(trade.getPurchaseDate(), endDate) / 365.2422; //period
        double totalReturns = (sellPrice - buyPrice) / buyPrice; //absoluteReturn
        double annualizedReturns = Math.pow((1.0 + totalReturns), (1.0 / totalNumberYears)) - 1;
        return new AnnualizedReturn(trade.getSymbol(), annualizedReturns, totalReturns);
    }

    public static String getToken() {
        return "289464e8faf5cf34aba42001442fb59b3c854b6c";
    }

    public static List<String> mainReadQuotes(String[] args) throws IOException, URISyntaxException {
        RestTemplate restTemplate = new RestTemplate();
        List<TotalReturnsDto> totalReturnsDto = new ArrayList<TotalReturnsDto>();
        List<PortfolioTrade> portfolioTrade = readTradesFromJson(args[0]);
        for(PortfolioTrade t : portfolioTrade){
            LocalDate endDate = LocalDate.parse(args[1]);
            String url = prepareUrl(t, endDate, getToken());
            System.out.println(url);
            TiingoCandle[] results = restTemplate.getForObject(url, TiingoCandle[].class);
            if(results != null){
                totalReturnsDto.add(new TotalReturnsDto(t.getSymbol(), results[results.length - 1].getClose()));
            }
        }

        Collections.sort(totalReturnsDto, new Comparator<TotalReturnsDto>(){

            @Override
            public int compare(TotalReturnsDto o1, TotalReturnsDto o2){
                return (int) (o1.getClosingPrice().compareTo(o2.getClosingPrice()));
            }

        });

        List<String> listAnswer = new ArrayList<>();
        for(int i = 0; i < totalReturnsDto.size(); i++){
            listAnswer.add(totalReturnsDto.get(i).getSymbol());
        }
        return listAnswer;
    }

    public static List<PortfolioTrade> readTradesFromJson(String filename) throws IOException, URISyntaxException {
        File file = resolveFileFromResources(filename);
        ObjectMapper objectmapper = getObjectMapper();
        PortfolioTrade[] portfolioTrade = objectmapper.readValue(file, PortfolioTrade[].class);
        List<PortfolioTrade> listPortfolioTrade = Arrays.asList(portfolioTrade);
        return listPortfolioTrade;
    }

    //  Build the Url using given parameters and use this function in your code to cann the API.
    public static String prepareUrl(PortfolioTrade trade, LocalDate endDate, String token) {
        return  "https://api.tiingo.com/tiingo/daily/"+ trade.getSymbol() +"/prices?startDate="+
                trade.getPurchaseDate() +"&endDate="+ endDate +"&token=" + token;
    }


    public static List<String> mainReadFile(String[] args) throws IOException, URISyntaxException {
        File file = resolveFileFromResources(args[0]);
        ObjectMapper objectMapper = getObjectMapper();
        PortfolioTrade[] trades = objectMapper.readValue(file, PortfolioTrade[].class);
        List<String> symbols = new ArrayList<String>();
        for(PortfolioTrade stock : trades){
            symbols.add(stock.getSymbol());
        }
        return symbols;
    }

//    private static File resolveFileFromResources(String filename) throws URISyntaxException {
//        return Paths.get(Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResource(filename)).toURI()).toFile();
//    }

    private static File resolveFileFromResources(String filename) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL resource = classLoader.getResource(filename);

        if (resource == null) {
            throw new IllegalArgumentException("Resource not found: " + filename);
        }

        try {
            return new File(resource.toURI());
        } catch (URISyntaxException e) {
            // Handle the URISyntaxException (e.g., log the error, rethrow it, or return a default file)
            e.printStackTrace(); // You should replace this with proper error handling
            return null; // Or return a default file if appropriate
        }
    }

    private static ObjectMapper getObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper;
    }

    public static List<String> debugOutputs() {
        String valueOfArgument0 = "trades.json";
        String resultOfResolveFilePathArgs0 = "/home/crio-user/workspace/prashant-criodo-ME_QMONEY_V2/qmoney/bin/main/trades.json";
        String toStringOfObjectMapper = "com.fasterxml.jackson.databind.ObjectMapper@5542c4ed";
        String functionNameFromTestFileInStackTrace = "PortfolioManagerApplicationTest.mainReadFile()";
        String lineNumberFromTestFileInStackTrace = "29:1";

        return Arrays.asList(new String[]{valueOfArgument0, resultOfResolveFilePathArgs0,
                toStringOfObjectMapper, functionNameFromTestFileInStackTrace,
                lineNumberFromTestFileInStackTrace});
    }

    private static void printJsonObject(Object object) throws IOException {
        Logger logger = Logger.getLogger(PortfolioManagerApplication.class.getCanonicalName());
        ObjectMapper mapper = new ObjectMapper();
        logger.info(mapper.writeValueAsString(object));
    }


    public static void main(String[] args) throws Exception {
        ThreadContext.put("runId", UUID.randomUUID().toString());

        printJsonObject(mainCalculateReturnsAfterRefactor(args));
    }
}

