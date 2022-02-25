package org.toasthub.stock.analysis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.toasthub.stock.model.Trade;

import net.jacobpeterson.alpaca.AlpacaAPI;
import net.jacobpeterson.alpaca.model.endpoint.orders.enums.OrderClass;
import net.jacobpeterson.alpaca.model.endpoint.orders.enums.OrderSide;
import net.jacobpeterson.alpaca.model.endpoint.orders.enums.OrderTimeInForce;
import net.jacobpeterson.alpaca.model.endpoint.orders.enums.OrderType;

@Service("CurrentTestingSvc")
public class CurrentTestingSvcImpl{

    @Autowired
    protected AlpacaAPI alpacaAPI;

    @Autowired
    protected CurrentBuySignals currentBuySignals;

    // Constructors
    public CurrentTestingSvcImpl() {
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

    public void currentTest(Trade trade) {
            if (currentBuySignals.process(trade.getAlgorithum(), trade.getStock())){
                try{
                alpacaAPI.orders().requestOrder(trade.getStock(), null, trade.getBuyAmount().doubleValue(), 
                OrderSide.BUY, OrderType.TRAILING_STOP, OrderTimeInForce.DAY, null, null, null,
                trade.getTrailingStopPercent().doubleValue(), false, null, 
                OrderClass.ONE_TRIGGERS_OTHER, null, null,null);
                }catch(Exception e){
                    System.out.println("Not Executed!");
                    System.out.println(e.getMessage());
                }
            }
    }
}
