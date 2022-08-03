package org.toasthub.trade.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.toasthub.core.general.model.RestRequest;
import org.toasthub.core.general.model.RestResponse;
import org.toasthub.trade.custom_technical_indicator.CustomTechnicalIndicatorDao;

@Configuration("TARequestValidation")
public class RequestValidation {

    @Autowired
    @Qualifier("TACustomTechnicalIndicatorDao")
    private CustomTechnicalIndicatorDao customTechnicalIndicatorDao;

    public static void validateDollars(final RestRequest request, final RestResponse response) {
        BigDecimal num = BigDecimal.ZERO;

        if (request.getParam("currencyAmount") instanceof Integer) {
            num = new BigDecimal((Integer) request.getParam("currencyAmount"));
        }
        if (request.getParam("currencyAmount") instanceof String) {
            num = new BigDecimal((String) request.getParam("currencyAmount"));
        }

        if (request.getParam("currencyAmount") instanceof Double) {
            num = BigDecimal.valueOf((Double) request.getParam("currencyAmount"));
        }

        if (num.compareTo(BigDecimal.ZERO) <= 0) {
            response.setStatus("Dollar amount must be greater than zero");
            return;
        }

        num = num.setScale(2, RoundingMode.HALF_UP);
        request.addParam("CURRENCY_AMOUNT", num);
    }

    public static void validateShares(final RestRequest request, final RestResponse response) {
        BigDecimal num = BigDecimal.ZERO;

        if (request.getParam("currencyAmount") instanceof Integer) {
            num = new BigDecimal((Integer) request.getParam("currencyAmount"));
        }
        if (request.getParam("currencyAmount") instanceof String) {
            num = new BigDecimal((String) request.getParam("standardDeviations"));
        }

        if (request.getParam("currencyAmount") instanceof Double) {
            num = BigDecimal.valueOf((Double) request.getParam("currencyAmount"));
        }

        if (num.compareTo(BigDecimal.ZERO) <= 0) {
            response.setStatus("Share amount must be greater than zero");
            return;
        }

        num = num.setScale(4, RoundingMode.HALF_UP);
        request.addParam("CURRENCY_AMOUNT", num);
    }

    public static void validateTrailingStopAmount(final RestRequest request, final RestResponse response) {
        BigDecimal num = BigDecimal.ZERO;

        if (request.getParam("trailingStopAmount") instanceof Integer) {
            num = new BigDecimal((Integer) request.getParam("trailingStopAmount"));
        }
        if (request.getParam("trailingStopAmount") instanceof String) {
            num = new BigDecimal((String) request.getParam("trailingStopAmount"));
        }

        if (request.getParam("trailingStopAmount") instanceof Double) {
            num = BigDecimal.valueOf((Double) request.getParam("trailingStopAmount"));
        }

        if (num.compareTo(BigDecimal.ZERO) <= 0) {
            response.setStatus("Trailing stop amount must be greater than zero");
            return;
        }

        num = num.setScale(2, RoundingMode.HALF_UP);
        request.addParam("TRAILING_STOP_AMOUNT", num);
    }

    public static void validateProfitLimitAmount(final RestRequest request, final RestResponse response) {
        BigDecimal num = BigDecimal.ZERO;

        if (request.getParam("profitLimitAmount") instanceof Integer) {
            num = new BigDecimal((Integer) request.getParam("profitLimitAmount"));
        }
        if (request.getParam("profitLimitAmount") instanceof String) {
            num = new BigDecimal((String) request.getParam("profitLimitAmount"));
        }

        if (request.getParam("profitLimitAmount") instanceof Double) {
            num = BigDecimal.valueOf((Double) request.getParam("profitLimitAmount"));
        }

        if (num.compareTo(BigDecimal.ZERO) <= 0) {
            response.setStatus("Profit Limit must be greater than 0");
            return;
        }

        num = num.setScale(2, RoundingMode.HALF_UP);
        request.addParam("PROFIT_LIMIT_AMOUNT", num);
    }

    public static void validateBudget(final RestRequest request, final RestResponse response) {
        BigDecimal num = BigDecimal.ZERO;

        if (request.getParam("budget") instanceof Integer) {
            num = new BigDecimal((Integer) request.getParam("budget"));
        }
        if (request.getParam("budget") instanceof String) {
            num = new BigDecimal((String) request.getParam("budget"));
        }

        if (request.getParam("budget") instanceof Double) {
            num = BigDecimal.valueOf((Double) request.getParam("budget"));
        }

        if (num.compareTo(BigDecimal.ZERO) <= 0) {
            response.setStatus("Budget must be greater than 0");
            return;
        }

        num = num.setScale(2, RoundingMode.HALF_UP);

        request.addParam("BUDGET", num);
    }

    public String validateName(final Object name) throws Exception {

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

        return String.class.cast(technicalIndicatorType);
    }

    public int validateShortSMAEvaluationDuration(final Object shortSMAEvaluationDuration) throws Exception {
        if (shortSMAEvaluationDuration == null
                || !(shortSMAEvaluationDuration instanceof Integer)) {
            throw new Exception("Short SMA Evaluation Duration is null or not an instance of an integer");
        }

        if ((int) shortSMAEvaluationDuration < 1 || (int) shortSMAEvaluationDuration > 999) {
            throw new Exception("Short SMA Evaluation Duration must be between 1 and 999");
        }

        return (int) shortSMAEvaluationDuration;
    }

    public int validateLongSMAEvaluationDuration(final Object longSMAEvaluationDuration) throws Exception {
        if (longSMAEvaluationDuration == null
                || !(longSMAEvaluationDuration instanceof Integer)) {
            throw new Exception("Long SMA Evaluation Duration is null or not an instance of an integer");
        }

        if ((int) longSMAEvaluationDuration < 1 || (int) longSMAEvaluationDuration > 999) {
            throw new Exception("Long SMA Evaluation Duration must be between 1 and 999");
        }

        return (int) longSMAEvaluationDuration;
    }

    public int validateLBBvaluationDuration(final Object lbbEvaluationDuration) throws Exception {
        if (lbbEvaluationDuration == null
                || !(lbbEvaluationDuration instanceof Integer)) {
            throw new Exception("Long SMA Evaluation Duration is null or not an instance of an integer");
        }

        if ((int) lbbEvaluationDuration < 1 || (int) lbbEvaluationDuration > 999) {
            throw new Exception("Long SMA Evaluation Duration must be between 1 and 999");
        }

        return (int) lbbEvaluationDuration;
    }

    public int validateUBBvaluationDuration(final Object ubbEvaluationDuration) throws Exception {
        if (ubbEvaluationDuration == null
                || !(ubbEvaluationDuration instanceof Integer)) {
            throw new Exception("Long SMA Evaluation Duration is null or not an instance of an integer");
        }

        if ((int) ubbEvaluationDuration < 1 || (int) ubbEvaluationDuration > 999) {
            throw new Exception("Long SMA Evaluation Duration must be between 1 and 999");
        }

        return (int) ubbEvaluationDuration;
    }

    public BigDecimal validateStandardDeviations(final Object standardDeviations) throws Exception {
        if (standardDeviations == null || !(standardDeviations instanceof String)) {
            throw new Exception("Standard Deviations are null or not a String");
        }

        final BigDecimal tempSD = new BigDecimal(String.class.cast(standardDeviations));

        if (tempSD.compareTo(BigDecimal.ZERO) <= 0 || tempSD.compareTo(new BigDecimal("3")) > 0) {
            throw new Exception("Standard Deviations must be between 3 and 0 ");
        }

        return tempSD.setScale(1, RoundingMode.HALF_UP);
    }

    public CustomTechnicalIndicator validateID(final Object id) throws Exception {

        if (id == null) {
            return new CustomTechnicalIndicator();
        }

        if (id instanceof Integer) {
            return customTechnicalIndicatorDao.find(Long.valueOf((Integer) id));
        }

        if (id instanceof Long) {
            return customTechnicalIndicatorDao.find(Long.class.cast(id));
        }

        throw new Exception("ID is not an integer or long");
    }
}
