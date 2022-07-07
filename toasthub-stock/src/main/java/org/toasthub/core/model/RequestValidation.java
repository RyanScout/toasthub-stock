package org.toasthub.core.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.toasthub.utils.Request;
import org.toasthub.utils.Response;

public class RequestValidation {

    public static void validateShortSMAType(Request request, Response response) {
        String string = "";
        String substring = "";
        int i = 0;

        string = (String) request.getParam("shortSMAType");

        if (!string.endsWith("-" + ((String) request.getParam("EVALUATION_PERIOD")).toLowerCase())) {
            response.setStatus(Response.ERROR);
            return;
        }

        substring = string.substring(0,
                string.length() - ((String) request.getParam("EVALUATION_PERIOD")).length() - 1);
        try {
            i = Integer.parseInt(substring);
        } catch (NumberFormatException e) {
            response.setStatus(Response.ERROR);
            return;
        }

        if (i <= 0 || i > 1000) {
            response.setStatus("Short SMA period must be between 0 and 1000");
            return;
        }

        request.addParam("SHORT_SMA_TYPE", request.getParam("shortSMAType"));
    }

    public static void validateLongSMAType(Request request, Response response) {
        String string = "";
        String substring = "";
        int i = 0;

        string = (String) request.getParam("longSMAType");

        if (!string.endsWith("-" + ((String) request.getParam("EVALUATION_PERIOD")).toLowerCase())) {
            response.setStatus(Response.ERROR);
            return;
        }
        substring = string.substring(0,
                string.length() - ((String) request.getParam("EVALUATION_PERIOD")).length() - 1);
        try {
            i = Integer.parseInt(substring);
        } catch (NumberFormatException e) {
            response.setStatus(Response.ERROR);
            return;
        }

        if (i <= 0 || i > 1000) {
            response.setStatus("Short SMA period must be between 0 and 1000");
            return;
        }

        request.addParam("LONG_SMA_TYPE", request.getParam("longSMAType"));
    }

    public static void validateLBBType(Request request, Response response) {
        String string = "";
        String substring = "";
        int i = 0;

        string = (String) request.getParam("lbbType");

        if (!string.endsWith("-" + ((String) request.getParam("EVALUATION_PERIOD")).toLowerCase())) {
            response.setStatus(Response.ERROR);
            return;
        }
        substring = string.substring(0,
                string.length() - ((String) request.getParam("EVALUATION_PERIOD")).length() - 1);
        try {
            i = Integer.parseInt(substring);
        } catch (NumberFormatException e) {
            response.setStatus(Response.ERROR);
            return;
        }

        if (i <= 0 || i > 1000) {
            response.setStatus("Lower bollinger band amount must be between 0 and 1000");
            return;
        }

        request.addParam("LBB_TYPE", (String) request.getParam("lbbType"));
    }

    public static void validateUBBType(Request request, Response response) {
        String string = "";
        String substring = "";
        int i = 0;

        string = (String) request.getParam("ubbType");

        if (!string.endsWith("-" + ((String) request.getParam("EVALUATION_PERIOD")).toLowerCase())) {
            response.setStatus(Response.ERROR);
            return;
        }
        substring = string.substring(0,
                string.length() - ((String) request.getParam("EVALUATION_PERIOD")).length() - 1);
        try {
            i = Integer.parseInt(substring);
        } catch (NumberFormatException e) {
            response.setStatus(Response.ERROR);
            return;
        }

        if (i <= 0 || i > 1000) {
            response.setStatus("Upper bollinger band period must be between 0 and 1000");
            return;
        }

        request.addParam("UBB_TYPE", (String) request.getParam("ubbType"));
    }

    public static void validateStandardDeviations(Request request, Response response) {
        BigDecimal num = BigDecimal.ZERO;

        if (request.getParam("standardDeviations") instanceof Integer) {
            num = new BigDecimal((Integer) request.getParam("standardDeviations"));
        }

        if (request.getParam("standardDeviations") instanceof String) {
            num = new BigDecimal((String) request.getParam("standardDeviations"));
        }

        if (request.getParam("standardDeviations") instanceof Double) {
            num = BigDecimal.valueOf((Double) request.getParam("standardDeviations"));
        }

        if (num.compareTo(BigDecimal.ZERO) <= 0 || num.compareTo(new BigDecimal(3)) > 0) {
            response.setStatus("Standard Deviations must be between 3 and 0 ");
            return;
        }
        
        num = num.setScale(1, RoundingMode.HALF_UP);

        request.addParam("STANDARD_DEVIATIONS", num);
    }

    public static void validateDollars(Request request, Response response) {
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

    public static void validateShares(Request request, Response response) {
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

    public static void validateTrailingStopAmount(Request request, Response response){
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

    public static void validateProfitLimitAmount(Request request, Response response){
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

    public static void validateBudget(Request request, Response response){
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

    public static void validateName(Request request, Response response){
        String name = "";

        if(request.getParam("name") instanceof String){
            name = (String)request.getParam("name");
        }

        if(name.replaceAll("\\s+", "").equals("")){
            response.setStatus("Name cannot be empty");
            return;
        }

        if(name.contains("(") || name.contains(")") || name.contains("&") || name.contains("|")){
            response.setStatus("Name cannot contain logical operators");
            return;
        }

        request.addParam(("NAME"), name);
    }
}
