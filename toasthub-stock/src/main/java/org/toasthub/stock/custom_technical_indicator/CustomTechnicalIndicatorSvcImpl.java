package org.toasthub.stock.custom_technical_indicator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.toasthub.model.CustomTechnicalIndicator;
import org.toasthub.model.Symbol;
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
        if ((!request.containsParam(GlobalConstant.ITEM)) || (request.getParam(GlobalConstant.ITEM) == null)) {
            return;
        }

        Map<?, ?> m = Map.class.cast(request.getParam(GlobalConstant.ITEM));
        Map<String, Object> tempMap = new HashMap<String, Object>();
        for (Object o : m.keySet()) {
            tempMap.put(String.class.cast(o), m.get(String.class.cast(o)));
        }

        String string = "";
        String substring = "";
        int i = 0;

        request.setParams(tempMap);
        request.addParam("SYMBOLS", request.getParam("symbols"));
        request.addParam(GlobalConstant.ITEMID, request.getParam("id"));
        request.addParam("TECHNICAL_INDICATOR_TYPE", request.getParam("technicalIndicatorType"));
        request.addParam("NAME", request.getParam("name"));

        if (request.getParam("evaluationPeriod") == null) {
            response.setStatus(Response.EMPTY);
            return;
        }

        request.addParam("EVALUATION_PERIOD", request.getParam("evaluationPeriod"));

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

        if (i <= 0 || i > 999) {
            return;
        }

        request.addParam("SHORT_SMA_TYPE", request.getParam("shortSMAType"));

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

        if (i <= 0 || i > 999) {
            response.setStatus(Response.ERROR);
            return;
        }

        request.addParam("LONG_SMA_TYPE", request.getParam("longSMAType"));

        CustomTechnicalIndicator temp = new CustomTechnicalIndicator();

        if (request.containsParam(GlobalConstant.ITEMID) && request.getParam(GlobalConstant.ITEMID) != null) {
            try {
                customTechnicalIndicatorDao.item(request, response);
            } catch (Exception e) {
                e.printStackTrace();
            }
            temp = CustomTechnicalIndicator.class.cast(response.getParam(GlobalConstant.ITEM));
        }

        CustomTechnicalIndicator x = temp;

        x.setName((String) request.getParam("NAME"));
        x.setEvaluationPeriod((String) request.getParam("EVALUATION_PERIOD"));
        x.setTechnicalIndicatorType((String) request.getParam("TECHNICAL_INDICATOR_TYPE"));

        String technicalIndicatorKey = "";

        if (request.getParam("SHORT_SMA_TYPE") != null) {
            technicalIndicatorKey += (String) request.getParam("SHORT_SMA_TYPE") + ":";
        }

        if (request.getParam("LONG_SMA_TYPE") != null) {
            technicalIndicatorKey += (String) request.getParam("LONG_SMA_TYPE") + ":";
        }

        if (!technicalIndicatorKey.equals("")) {
            technicalIndicatorKey = technicalIndicatorKey.substring(0, technicalIndicatorKey.length() - 1);
        }

        x.setTechnicalIndicatorKey(technicalIndicatorKey);

        List<String> symbols = new ArrayList<String>();

        for (Object o : ArrayList.class.cast(request.getParam("SYMBOLS"))) {
            symbols.add((String.class.cast(o)));
        }

        symbols.stream()
                .distinct()
                .filter(symbol -> Arrays.asList(Symbol.SYMBOLS).contains(symbol))
                .filter(symbol -> !x.getSymbols().stream()
                        .anyMatch(tempSymbol -> tempSymbol.getSymbol().equals(symbol)))
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
        // TODO Auto-generated method stub

    }

    @Override
    public void item(Request request, Response response) {
        // TODO Auto-generated method stub

    }

    @Override
    public void items(Request request, Response response) {
    }

}
