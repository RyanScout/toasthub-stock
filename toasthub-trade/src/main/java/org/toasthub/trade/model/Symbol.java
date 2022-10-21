package org.toasthub.trade.model;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.toasthub.core.general.api.View;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;

@Entity
@Table(name = "ta_symbol")
public class Symbol extends TradeBaseEntity {

    private static final long serialVersionUID = 1L;

    public static final String SPY = "SPY";
    public static final String AAPL = "AAPL";
    public static final String TSLA = "TSLA";
    public static final String WBA = "WBA";

    public static final String BTCUSD = "BTCUSD";
    public static final String LTCUSD = "LTCUSD";
    public static final String ETHUSD = "ETHUSD";

    public static final List<String> CRYPTO_SYMBOLS = Arrays.asList(BTCUSD);

    public static final List<String> STOCK_SYMBOLS = Arrays.asList(AAPL);

    public static final List<String> SYMBOLS = Stream.concat(CRYPTO_SYMBOLS.stream(), STOCK_SYMBOLS.stream()).toList();

    private String symbol;
    private CustomTechnicalIndicator customTechnicalIndicator;

    // Setter/Getter
    @JsonIgnore
    @ManyToOne(targetEntity = CustomTechnicalIndicator.class, fetch = FetchType.LAZY)
    @JoinColumn(name = "custom_technical_indicator_id")
    public CustomTechnicalIndicator getCustomTechnicalIndicator() {
        return customTechnicalIndicator;
    }

    public void setCustomTechnicalIndicator(final CustomTechnicalIndicator customTechnicalIndicator) {
        this.customTechnicalIndicator = customTechnicalIndicator;
    }

    @JsonView({ View.Member.class })
    @Column(name = "symbol")
    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(final String symbol) {
        this.symbol = symbol;
    }

}
