package org.toasthub.stock.custom_technical_indicator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.toasthub.core.model.CustomTechnicalIndicator;
import org.toasthub.core.model.RequestValidation;
import org.toasthub.core.model.Symbol;
import org.toasthub.core.model.TechnicalIndicator;
import org.toasthub.stock.cache.CacheSvc;
import org.toasthub.utils.GlobalConstant;
import org.toasthub.utils.Request;
import org.toasthub.utils.Response;

@Service("CustomTechnicalIndicatorSvc")
public class CustomTechnicalIndicatorSvcImpl implements CustomTechnicalIndicatorSvc {

    @Autowired
    private CustomTechnicalIndicatorDao customTechnicalIndicatorDao;

    @Autowired
    private CacheSvc cacheSvc;

    @Override
    public void process(Request request, Response response) {
        String action = (String) request.getParams().get("action");
        switch (action) {
            case "ITEM":
                item(request, response);
                break;
            case "LIST":
                items(request, response);
                break;
            case "SAVE":
                save(request, response);
                break;
            case "DELETE":
                delete(request, response);
                break;
        }
    }

    @Override
    public void save(Request request, Response response) {
        response.setStatus("Starting !");

        if ((!request.containsParam(GlobalConstant.ITEM)) || (request.getParam(GlobalConstant.ITEM) == null)) {
            response.setStatus(Response.ERROR);
            return;
        }

        Map<?, ?> m = Map.class.cast(request.getParam(GlobalConstant.ITEM));

        Map<String, Object> tempMap = new HashMap<String, Object>();

        for (Object o : m.keySet()) {
            tempMap.put(String.class.cast(o), m.get(String.class.cast(o)));
        }

        request.setParams(tempMap);

        request.addParam("SYMBOLS", request.getParam("symbols"));
        request.addParam(GlobalConstant.ITEMID, request.getParam("id"));

        if (request.getParam("technicalIndicatorType") == null) {
            response.setStatus("Technical indicator type cannot be null");
            return;
        }

        if (request.getParam("evaluationPeriod") == null) {
            response.setStatus("Evaluation period cannot be null");
            return;
        }

        if (!Arrays.asList(TechnicalIndicator.TECHNICALINDICATORTYPES)
                .contains((String) request.getParam("technicalIndicatorType"))) {
            response.setStatus("Technical indicator type is not valid");
            return;
        }

        if (request.getParam("name") == null) {
            response.setStatus("Name cannot be empty");
            return;
        }

       
        request.addParam("TECHNICAL_INDICATOR_TYPE", request.getParam("technicalIndicatorType"));
        request.addParam("EVALUATION_PERIOD", request.getParam("evaluationPeriod"));

        RequestValidation.validateName(request, response);

        if (request.getParam("shortSMAType") != null
                && ((String) request.getParam("TECHNICAL_INDICATOR_TYPE")).equals(TechnicalIndicator.GOLDENCROSS)) {
            RequestValidation.validateShortSMAType(request, response);
        }

        if (request.getParam("longSMAType") != null
                && ((String) request.getParam("TECHNICAL_INDICATOR_TYPE")).equals(TechnicalIndicator.GOLDENCROSS)) {
            RequestValidation.validateLongSMAType(request, response);
        }

        if (request.getParam("lbbType") != null && ((String) request.getParam("TECHNICAL_INDICATOR_TYPE"))
                .equals(TechnicalIndicator.LOWERBOLLINGERBAND)) {
            RequestValidation.validateLBBType(request, response);
        }

        if (request.getParam("ubbType") != null && ((String) request.getParam("TECHNICAL_INDICATOR_TYPE"))
                .equals(TechnicalIndicator.UPPERBOLLINGERBAND)) {
            RequestValidation.validateUBBType(request, response);
        }

        if (request.getParam("standardDeviations") != null && (((String) request.getParam("TECHNICAL_INDICATOR_TYPE"))
                .equals(TechnicalIndicator.UPPERBOLLINGERBAND)
                || ((String) request.getParam("TECHNICAL_INDICATOR_TYPE"))
                        .equals(TechnicalIndicator.LOWERBOLLINGERBAND))) {
            RequestValidation.validateStandardDeviations(request, response);
        }

        if (!response.getStatus().equals("Starting !")) {
            return;
        }

        CustomTechnicalIndicator temp = new CustomTechnicalIndicator();

        if (request.containsParam(GlobalConstant.ITEMID) && request.getParam(GlobalConstant.ITEMID) != null) {
            try {
                customTechnicalIndicatorDao.item(request, response);
            } catch (Exception e) {
                e.printStackTrace();
            }
            temp = CustomTechnicalIndicator.class.cast(response.getParam(GlobalConstant.ITEM));
        } else {

            try {
                customTechnicalIndicatorDao.itemCount(request, response);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if ((long) response.getParam(GlobalConstant.ITEMCOUNT) > 0) {
                response.setStatus(Response.ERROR);
                return;
            }
        }

        CustomTechnicalIndicator x = temp;

        x.setName((String) request.getParam("NAME"));
        x.setEvaluationPeriod((String) request.getParam("EVALUATION_PERIOD"));
        x.setTechnicalIndicatorType((String) request.getParam("TECHNICAL_INDICATOR_TYPE"));

        String technicalIndicatorKey = "";

        if (request.getParam("SHORT_SMA_TYPE") != null) {
            x.setShortSMAType((String) request.getParam("SHORT_SMA_TYPE"));
            technicalIndicatorKey += (String) request.getParam("SHORT_SMA_TYPE") + ":";
        }

        if (request.getParam("LONG_SMA_TYPE") != null) {
            x.setLongSMAType((String) request.getParam("LONG_SMA_TYPE"));
            technicalIndicatorKey += (String) request.getParam("LONG_SMA_TYPE") + ":";
        }

        if (request.getParam("LBB_TYPE") != null) {
            x.setLBBType((String) request.getParam("LBB_TYPE"));
            technicalIndicatorKey += (String) request.getParam("LBB_TYPE") + ":";
        }

        if (request.getParam("UBB_TYPE") != null) {
            x.setUBBType((String) request.getParam("UBB_TYPE"));
            technicalIndicatorKey += (String) request.getParam("UBB_TYPE") + ":";
        }

        if (request.getParam("STANDARD_DEVIATIONS") != null) {
            x.setStandardDeviations((BigDecimal) request.getParam("STANDARD_DEVIATIONS"));
            technicalIndicatorKey += ((BigDecimal) request.getParam("STANDARD_DEVIATIONS")).toString()
                    + ":";
        }

        if (!technicalIndicatorKey.equals("")) {
            technicalIndicatorKey = technicalIndicatorKey.substring(0, technicalIndicatorKey.length() - 1);
        }

        x.setTechnicalIndicatorKey(technicalIndicatorKey);

        List<String> symbols = new ArrayList<String>();

        for (Object o : ArrayList.class.cast(request.getParam("SYMBOLS"))) {
            symbols.add((String.class.cast(o)));
        }

        x.getSymbols().clear();

        symbols.stream()
                .distinct()
                .filter(symbol -> Arrays.asList(Symbol.SYMBOLS).contains(symbol))
                .forEach(symbol -> {
                    Symbol s = new Symbol();
                    s.setSymbol(symbol);
                    s.setCustomTechnicalIndicator(x);
                    x.getSymbols().add(s);
                });

        request.addParam(GlobalConstant.ITEM, x);

        try {
            customTechnicalIndicatorDao.save(request, response);
        } catch (Exception e) {
            e.printStackTrace();
        }

        request.addParam("SYMBOLS", symbols);
        request.addParam("TECHNICAL_INDICATOR_KEY", technicalIndicatorKey);

        cacheSvc.save(request, response);

        response.setStatus(Response.SUCCESS);
    }

    @Override
    public void delete(Request request, Response response) {
        try {
			customTechnicalIndicatorDao.delete(request, response);
			response.setStatus(Response.SUCCESS);
		} catch (Exception e) {
			response.setStatus(Response.ACTIONFAILED);
			e.printStackTrace();
		}


    }

    @Override
    public void item(Request request, Response response) {

    }

    @Override
    public void items(Request request, Response response) {
    }

}
