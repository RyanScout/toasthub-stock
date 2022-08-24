package org.toasthub.trade.custom_technical_indicator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.toasthub.core.general.handler.ServiceProcessor;
import org.toasthub.core.general.model.GlobalConstant;
import org.toasthub.core.general.model.RestRequest;
import org.toasthub.core.general.model.RestResponse;
import org.toasthub.trade.cache.CacheSvc;
import org.toasthub.trade.model.CustomTechnicalIndicator;
import org.toasthub.trade.model.RequestValidation;
import org.toasthub.trade.model.Symbol;
import org.toasthub.trade.model.TechnicalIndicator;
import org.toasthub.trade.ti_snapshot.TISnapshotSvc;

@Service("TACustomTechnicalIndicatorSvc")
public class CustomTechnicalIndicatorSvcImpl implements ServiceProcessor, CustomTechnicalIndicatorSvc {

    @Autowired
    @Qualifier("TACustomTechnicalIndicatorDao")
    private CustomTechnicalIndicatorDao customTechnicalIndicatorDao;

    @Autowired
    @Qualifier("TATISnapshotSvc")
    private TISnapshotSvc tiSnapshotSvc;

    @Autowired
    @Qualifier("TACacheSvc")
    private CacheSvc cacheSvc;

    @Autowired
    @Qualifier("TARequestValidation")
    private RequestValidation validator;

    @Override
    public void process(final RestRequest request, final RestResponse response) {
        final String action = (String) request.getParams().get("action");
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
    public void save(final RestRequest request, final RestResponse response) {
        try {
            if ((!request.containsParam(GlobalConstant.ITEM))
                    || (request.getParam(GlobalConstant.ITEM) == null)
                    || !(request.getParam(GlobalConstant.ITEM) instanceof LinkedHashMap)) {
                throw new Exception("Invalid Item Object at Custom Technical Indicator Service Save");
            }

            final Map<?, ?> m = Map.class.cast(request.getParam(GlobalConstant.ITEM));

            final Map<String, Object> itemProperties = new HashMap<String, Object>();

            for (final Object o : m.keySet()) {
                itemProperties.put(String.class.cast(o), m.get(String.class.cast(o)));
            }

            final CustomTechnicalIndicator item = validator
                    .validateCustomTechnicalIndicatorID(itemProperties.get("id"));

            item.setName(validator.validateCustomTechnicalIndicatorName(itemProperties.get("name")));
            item.setEvaluationPeriod(
                    validator.validateEvaluationPeriod(itemProperties.get("evaluationPeriod")));
            item.setTechnicalIndicatorType(
                    validator.validateTechnicalIndicatorType(itemProperties.get("technicalIndicatorType")));

            switch (item.getTechnicalIndicatorType()) {
                case TechnicalIndicator.GOLDENCROSS:

                    final int shortSMAEvaluationDuration = validator
                            .validateShortSMAEvaluationDuration(itemProperties.get("shortSMAEvaluationDuration"));
                    final int longSMAEvaluationDuration = validator
                            .validateShortSMAEvaluationDuration(itemProperties.get("longSMAEvaluationDuration"));

                    if (shortSMAEvaluationDuration >= longSMAEvaluationDuration) {
                        throw new Exception(
                                "Short SMA Evaluation Duration must be less than Long SMA Evaluation Duration");
                    }

                    item.setShortSMAEvaluationDuration(shortSMAEvaluationDuration);
                    item.setLongSMAEvaluationDuration(longSMAEvaluationDuration);
                    item.setTechnicalIndicatorKey(shortSMAEvaluationDuration + ":" + longSMAEvaluationDuration);

                    break;

                case TechnicalIndicator.LOWERBOLLINGERBAND:

                    final int lbbEvaluationDuration = validator
                            .validateLBBEvaluationDuration(itemProperties.get("lbbEvaluationDuration"));
                    final BigDecimal lbbStandardDeviations = validator
                            .validateStandardDeviations(itemProperties.get("standardDeviations"));

                    item.setLbbEvaluationDuration(lbbEvaluationDuration);
                    item.setStandardDeviations(lbbStandardDeviations);

                    item.setTechnicalIndicatorKey(lbbEvaluationDuration + ":" + lbbStandardDeviations);

                    break;

                case TechnicalIndicator.UPPERBOLLINGERBAND:

                    final int ubbEvaluationDuration = validator
                            .validateUBBEvaluationDuration(itemProperties.get("ubbEvaluationDuration"));
                    final BigDecimal ubbStandardDeviations = validator
                            .validateStandardDeviations(itemProperties.get("standardDeviations"));

                    item.setUbbEvaluationDuration(ubbEvaluationDuration);
                    item.setStandardDeviations(ubbStandardDeviations);

                    item.setTechnicalIndicatorKey(ubbEvaluationDuration + ":" + ubbStandardDeviations);
                    break;

                default:
                    throw new Exception("Invalid Technical Indicator Type");
            }

            final List<String> symbols = new ArrayList<String>();

            for (final Object o : ArrayList.class.cast(itemProperties.get("effectiveSymbols"))) {
                if (o instanceof String) {
                    symbols.add((String.class.cast(o)));
                }
            }

            final Set<Symbol> symbolEntities = new HashSet<Symbol>();

            symbols.stream()
                    .distinct()
                    .filter(symbol -> Symbol.SYMBOLS.contains(symbol))
                    .forEach(symbol -> {
                        final Symbol s = new Symbol();
                        s.setSymbol(symbol);
                        s.setCustomTechnicalIndicator(item);
                        symbolEntities.add(s);
                    });

            item.setSymbols(symbolEntities);

            customTechnicalIndicatorDao.saveItem(item);

            cacheSvc.save(item);

            tiSnapshotSvc.createRelevantSnapshots(item);

        } catch (final Exception e) {
            e.printStackTrace();
            response.setStatus("Exception: " + e.getMessage());
            return;
        }

        response.setStatus(RestResponse.SUCCESS);
    }

    @Override
    public void delete(final RestRequest request, final RestResponse response) {
        try {
            customTechnicalIndicatorDao.delete(request, response);
            response.setStatus(RestResponse.SUCCESS);
        } catch (final Exception e) {
            response.setStatus(RestResponse.ACTIONFAILED);
            e.printStackTrace();
        }

    }

    @Override
    public void item(final RestRequest request, final RestResponse response) {

    }

    @Override
    public void items(final RestRequest request, final RestResponse response) {
    }

}
