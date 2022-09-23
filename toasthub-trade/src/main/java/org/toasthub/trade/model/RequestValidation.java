package org.toasthub.trade.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.NoResultException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.toasthub.trade.custom_technical_indicator.CustomTechnicalIndicatorDao;
import org.toasthub.trade.trade.TradeDao;

@Configuration("TARequestValidation")
public class RequestValidation {

    @Autowired
    @Qualifier("TACustomTechnicalIndicatorDao")
    private CustomTechnicalIndicatorDao customTechnicalIndicatorDao;

    @Autowired
    @Qualifier("TATradeDao")
    private TradeDao tradeDao;

    final ExpressionParser parser = new SpelExpressionParser();

    public String validateCustomTechnicalIndicatorName(final Object name) throws Exception {

        if (name == null || !(name instanceof String)) {
            throw new Exception("Name is null or not a string");
        }

        if (String.class.cast(name).replaceAll("\\s+", "").equals("")) {
            throw new Exception("Name is null");
        }

        if (Arrays.asList("(", ")", "&", "|").contains(String.class.cast(name))) {
            throw new Exception("Name contains logical operators");
        }

        return String.class.cast(name);
    }

    public String validateEvaluationPeriod(final Object evaluationPeriod) throws Exception {
        if (evaluationPeriod == null || !(evaluationPeriod instanceof String)) {
            throw new Exception("Evaluation period is null or not a string");
        }

        if (!evaluationPeriod.equals("DAY") && !evaluationPeriod.equals("MINUTE")) {
            throw new Exception("Evaluation period does not equal DAY or MINUTE");
        }

        return String.class.cast(evaluationPeriod);
    }

    public String validateTechnicalIndicatorType(final Object technicalIndicatorType) throws Exception {
        if (technicalIndicatorType == null || !(technicalIndicatorType instanceof String)) {
            throw new Exception("Technical Indicator Type is null or not a string");
        }

        if (!Arrays.asList(TechnicalIndicator.TECHNICAL_INDICATOR_TYPES)
                .contains(String.class.cast(technicalIndicatorType))) {
            throw new Exception("Technical Indicator type not supported");
        }

        return String.class.cast(technicalIndicatorType);
    }

    public int validateShortSMAEvaluationDuration(final Object shortSMAEvaluationDuration) throws Exception {
        if (shortSMAEvaluationDuration == null) {
            throw new Exception("Short SMA Evaluation Duration is null");
        }
        if (!(shortSMAEvaluationDuration instanceof Integer) && !(shortSMAEvaluationDuration instanceof String)) {
            throw new Exception("Short SMA Evaluation Duration is not an instance of an integer or string");
        }

        if (Integer.valueOf(String.valueOf(shortSMAEvaluationDuration)) < 1
                || Integer.valueOf(String.valueOf(shortSMAEvaluationDuration)) > 999) {
            throw new Exception("Short SMA Evaluation Duration must be between 1 and 999");
        }

        return Integer.valueOf(String.valueOf(shortSMAEvaluationDuration));
    }

    public int validateLongSMAEvaluationDuration(final Object longSMAEvaluationDuration) throws Exception {
        if (longSMAEvaluationDuration == null) {
            throw new Exception("Long SMA Evaluation Duration is null");
        }
        if (!(longSMAEvaluationDuration instanceof Integer) && !(longSMAEvaluationDuration instanceof String)) {
            throw new Exception("Long SMA Evaluation Duration is not an instance of an integer or string");
        }

        if (Integer.valueOf(String.valueOf(longSMAEvaluationDuration)) < 1
                || Integer.valueOf(String.valueOf(longSMAEvaluationDuration)) > 999) {
            throw new Exception("Long SMA Evaluation Duration must be between 1 and 999");
        }

        return Integer.valueOf(String.valueOf(longSMAEvaluationDuration));
    }

    public int validateLBBEvaluationDuration(final Object lbbEvaluationDuration) throws Exception {
        if (lbbEvaluationDuration == null) {
            throw new Exception("LBB Evaluation Duration is null");
        }
        if (!(lbbEvaluationDuration instanceof Integer) && !(lbbEvaluationDuration instanceof String)) {
            throw new Exception("LBB Evaluation Duration is not an instance of an integer or string");
        }

        if (Integer.valueOf(String.valueOf(lbbEvaluationDuration)) < 1
                || Integer.valueOf(String.valueOf(lbbEvaluationDuration)) > 999) {
            throw new Exception("LBB Evaluation Duration must be between 1 and 999");
        }

        return Integer.valueOf(String.valueOf(lbbEvaluationDuration));
    }

    public int validateUBBEvaluationDuration(final Object ubbEvaluationDuration) throws Exception {
        if (ubbEvaluationDuration == null) {
            throw new Exception("UBB Evaluation Duration is null");
        }
        if (!(ubbEvaluationDuration instanceof Integer) && !(ubbEvaluationDuration instanceof String)) {
            throw new Exception("UBB Evaluation Duration is not an instance of an integer or string");
        }

        if (Integer.valueOf(String.valueOf(ubbEvaluationDuration)) < 1
                || Integer.valueOf(String.valueOf(ubbEvaluationDuration)) > 999) {
            throw new Exception("UBB Evaluation Duration must be between 1 and 999");
        }

        return Integer.valueOf(String.valueOf(ubbEvaluationDuration));
    }

    public BigDecimal validateStandardDeviations(final Object standardDeviations) throws Exception {

        if (standardDeviations == null) {
            throw new Exception("Standard Deviations amount is null");
        }

        if (!(standardDeviations instanceof Integer) && !(standardDeviations instanceof String)) {
            throw new Exception("Standard deviations amount is not an instance of integer or string");
        }

        final BigDecimal tempSD = new BigDecimal(String.valueOf(standardDeviations));

        if (tempSD.compareTo(BigDecimal.ZERO) <= 0 || tempSD.compareTo(new BigDecimal("3")) > 0) {
            throw new Exception("Standard Deviations must be between 3 and 0 ");
        }

        return tempSD.setScale(1, RoundingMode.HALF_UP);
    }

    public CustomTechnicalIndicator validateCustomTechnicalIndicatorID(final Object id) throws Exception {

        if (id == null) {
            return new CustomTechnicalIndicator();
        }

        if (id instanceof Integer) {
            return customTechnicalIndicatorDao.findById(Long.valueOf((Integer) id));
        }

        if (id instanceof Long) {
            return customTechnicalIndicatorDao.findById(Long.class.cast(id));
        }

        throw new Exception("ID is not an integer or long");
    }

    public Trade validateTradeID(final Object id) throws Exception {
        if (id == null) {
            return new Trade();
        }

        if (id instanceof Integer) {
            return tradeDao.findTradeById(Long.valueOf((Integer) id));
        }

        if (id instanceof Long) {
            return tradeDao.findTradeById(Long.class.cast(id));
        }

        throw new Exception("ID is not an integer or long");
    }

    public long validateId(final Object id) throws Exception {
        if (id == null) {
            throw new Exception("ID is null");
        }
        if (!(id instanceof String) && !(id instanceof Long) && !(id instanceof Integer)) {
            throw new Exception("Date is not an instance of String or Long or Integer");
        }

        return Long.valueOf(String.valueOf(id));

    }

    public String validateTradeName(final Object name) throws Exception {
        if (name == null || !(name instanceof String)) {
            throw new Exception("Name is null or not a string");
        }

        if (String.class.cast(name).replaceAll("\\s+", "").equals("")) {
            throw new Exception("Name is null");
        }

        return String.class.cast(name);
    }

    public String validateOrderSide(final Object orderSide) throws Exception {
        if (orderSide == null || !(orderSide instanceof String)) {
            throw new Exception("Orderside is null or not a string");
        }

        if (!Arrays.asList(Trade.SUPPORTED_ORDER_SIDES).contains(String.class.cast(orderSide).toUpperCase())) {
            throw new Exception("Orderside is not supported");
        }

        return String.class.cast(orderSide);
    }

    public String validateOrderType(final Object orderType) throws Exception {

        if (orderType == null || !(orderType instanceof String)) {
            throw new Exception("Ordertype is null or not a string");
        }

        if (!Arrays.asList(Trade.SUPPORTED_ORDER_TYPES).contains(String.class.cast(orderType).toUpperCase())) {
            throw new Exception("Ordertype is not supported");
        }

        return String.class.cast(orderType);
    }

    public String validateCurrencyType(final Object currencyType) throws Exception {
        if (currencyType == null || !(currencyType instanceof String)) {
            throw new Exception("Currency type is null or not a string");
        }

        if (!Arrays.asList("DOLLARS", "SHARES").contains(String.class.cast(currencyType).toUpperCase())) {
            throw new Exception("Currency Type is not supported");
        }

        return String.class.cast(currencyType);
    }

    public BigDecimal validateDollarAmount(final Object dollarAmount) throws Exception {

        if (dollarAmount == null) {
            throw new Exception("Dollar amount is null");
        }

        if (!(dollarAmount instanceof Integer) && !(dollarAmount instanceof String)) {
            throw new Exception("Share amount is not an instance of integer or string");
        }

        if (new BigDecimal(String.valueOf(dollarAmount)).compareTo(BigDecimal.ZERO) <= 0) {
            throw new Exception("Dollar amount must be greater than zero");
        }

        return new BigDecimal(String.valueOf(dollarAmount)).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal validateShareAmount(final Object shareAmount) throws Exception {
        if (shareAmount == null) {
            throw new Exception("Share amount is null");
        }

        if (!(shareAmount instanceof Integer) && !(shareAmount instanceof String)) {
            throw new Exception("Share amount is not an instance of integer or string");
        }

        if (new BigDecimal(String.valueOf(shareAmount)).compareTo(BigDecimal.ZERO) <= 0) {
            throw new Exception("Share amount must be greater than zero");
        }

        return new BigDecimal(String.valueOf(shareAmount)).setScale(4, RoundingMode.HALF_UP);
    }

    public BigDecimal validateTrailingStopAmount(final Object trailingStopAmount) throws Exception {

        if (trailingStopAmount == null || !(trailingStopAmount instanceof String)) {
            throw new Exception("Trailing stop amount is null or is not an instance of String");
        }

        if (new BigDecimal(String.class.cast(trailingStopAmount)).compareTo(BigDecimal.ZERO) <= 0) {
            throw new Exception("Trailing stop amount must be greater than zero");
        }

        return new BigDecimal(String.class.cast(trailingStopAmount)).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal validateProfitLimitAmount(final Object profitLimitAmount) throws Exception {
        if (profitLimitAmount == null || !(profitLimitAmount instanceof String)) {
            throw new Exception("Trailing stop amount is null or is not an instance of String");
        }

        if (new BigDecimal(String.class.cast(profitLimitAmount)).compareTo(BigDecimal.ZERO) <= 0) {
            throw new Exception("Trailing stop amount must be greater than zero");
        }

        return new BigDecimal(String.class.cast(profitLimitAmount)).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal validateBudget(final Object budget) throws Exception {

        if (budget == null) {
            throw new Exception("Budget is null");
        }

        if (!(budget instanceof Integer) && !(budget instanceof String)) {
            throw new Exception("Budget is not an instance of integer or string");
        }

        if (new BigDecimal(String.valueOf(budget)).compareTo(BigDecimal.ZERO) <= 0) {
            throw new Exception("Budget must be greater than 0");
        }

        return new BigDecimal(String.valueOf(budget)).setScale(2, RoundingMode.HALF_UP);

    }

    public String validateBuyCondition(final Trade trade, final Object buyCondition) throws Exception {

        if (buyCondition == null || !(buyCondition instanceof String)) {
            throw new Exception("Buy condition is null");
        }

        if (!(buyCondition instanceof String)) {
            throw new Exception("Buy condition is not an instance of string");
        }

        final String stretchedStr = String.class.cast(buyCondition)
                .replaceAll("\\s+", "")
                .replaceAll("[&]+", " && ")
                .replaceAll("[|]+", " || ")
                .replace("(", " ( ")
                .replace(")", " ) ");

        final List<String> validatedStrings = new ArrayList<String>();

        final List<String> testStrings = new ArrayList<String>();

        for (final String string : stretchedStr.split(" ")) {

            if (Arrays.asList("(", ")", "&&", "||", "").contains(string)) {
                testStrings.add(string);
                validatedStrings.add(string);
                continue;
            }

            try {
                final CustomTechnicalIndicator customTechnicalIndicator = customTechnicalIndicatorDao
                        .findByName(string);

                if (!trade.getEvaluationPeriod().equals(customTechnicalIndicator.getEvaluationPeriod())) {
                    throw new Exception("\"" + customTechnicalIndicator.getName()
                            + "\" does not support this trades evaluation period");
                }

                if (!customTechnicalIndicator.getSymbols()
                        .stream()
                        .anyMatch(symbol -> symbol.getSymbol().equals(trade.getSymbol()))) {
                    throw new Exception(
                            "\"" + customTechnicalIndicator.getName() + "\" does not support " + trade.getSymbol());
                }

                testStrings.add("true");
                validatedStrings.add(String.valueOf(customTechnicalIndicator.getId()));

            } catch (final NoResultException e) {
                throw new Exception("Invalid technical indicator in buy condition");
            }
        }

        final String testString = String.join(" ", testStrings);

        if (!testString.equals("")) {
            try {
                parser.parseExpression(testString).getValue(Boolean.class);
            } catch (final ParseException e) {
                throw new Exception("Invalid logic in buy condition");
            }
        }

        final String validatedBuyCondition = String.join(" ", validatedStrings).trim();

        return validatedBuyCondition;

    }

    public String validateSellCondition(final Trade trade, final Object sellCondition) throws Exception {

        if (sellCondition == null || !(sellCondition instanceof String)) {
            throw new Exception("Sell condition is null");
        }

        if (!(sellCondition instanceof String)) {
            throw new Exception("Sell condition is not an instance of string");
        }

        final String stretchedStr = String.class.cast(sellCondition)
                .replaceAll("\\s+", "")
                .replaceAll("[&]+", " && ")
                .replaceAll("[|]+", " || ")
                .replace("(", " ( ")
                .replace(")", " ) ");

        final List<String> validatedStrings = new ArrayList<String>();

        final List<String> testStrings = new ArrayList<String>();

        for (final String string : stretchedStr.split(" ")) {

            if (Arrays.asList("(", ")", "&&", "||", "").contains(string)) {
                testStrings.add(string);
                validatedStrings.add(string);
                continue;
            }

            try {
                final CustomTechnicalIndicator customTechnicalIndicator = customTechnicalIndicatorDao
                        .findByName(string);

                if (!trade.getEvaluationPeriod().equals(customTechnicalIndicator.getEvaluationPeriod())) {
                    throw new Exception("\"" + customTechnicalIndicator.getName()
                            + "\" does not support this trades evaluation period");
                }

                if (!customTechnicalIndicator.getSymbols()
                        .stream()
                        .anyMatch(symbol -> symbol.getSymbol().equals(trade.getSymbol()))) {
                    throw new Exception(
                            "\"" + customTechnicalIndicator.getName() + "\" does not support " + trade.getSymbol());
                }

                testStrings.add("true");
                validatedStrings.add(String.valueOf(customTechnicalIndicator.getId()));

            } catch (final NoResultException e) {
                throw new Exception("Invalid technical indicator in sell condition");
            }
        }

        final String testString = String.join(" ", testStrings);

        if (!testString.equals("")) {
            try {
                parser.parseExpression(testString).getValue(Boolean.class);
            } catch (final ParseException e) {
                throw new Exception("Invalid logic in sell condition");
            }
        }

        final String validatedSellCondition = String.join(" ", validatedStrings).trim();

        return validatedSellCondition;
    }

    public String validateSymbol(final Object symbol) throws Exception {

        if (symbol == null || !(symbol instanceof String)) {
            throw new Exception("Symbol is null or not an instance of string");
        }

        if (!Symbol.SYMBOLS.contains(String.class.cast(symbol))) {
            throw new Exception("Symbol is not supported");
        }

        return String.class.cast(symbol);
    }

    public String validateStatus(final Object status) throws Exception {
        if (status == null || !(status instanceof String)) {
            throw new Exception("Status is null or not a string");
        }

        return String.class.cast(status);
    }

    public long validateDate(final Object date) throws Exception {
        if (date == null) {
            throw new Exception("Date is null");
        }
        if (!(date instanceof String) && !(date instanceof Long) && !(date instanceof Integer)) {
            throw new Exception("Date is not an instance of String or Long or Integer");
        }

        return Long.valueOf(String.valueOf(date));
    }
}
