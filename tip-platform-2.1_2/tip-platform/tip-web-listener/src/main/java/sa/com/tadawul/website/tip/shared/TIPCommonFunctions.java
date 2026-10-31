package sa.com.tadawul.website.tip.shared;

public class TIPCommonFunctions {
	
	public static synchronized int setState(int prev_state, int state) {
        switch(state) {
            case TipConstants.TIP_STATE_CLOSED:
                return TipConstants.TIP_DB_STATE_CLOSED;
            case TipConstants.TIP_STATE_CALL_INTERACTION:
                return TipConstants.TIP_DB_STATE_PREOPENED;
            case TipConstants.TIP_STATE_CONTINUOUS_TRADING:
                return TipConstants.TIP_DB_STATE_OPENED;
            case TipConstants.TIP_STATE_POST_TRADING:
                return TipConstants.TIP_DB_STATE_PRECLOSED;
            case TipConstants.TIP_STATE_CLOSING_AUCTION:
                return TipConstants.TIP_DB_STATE_CLOSING_AUCTION;
              //Added for SR3
            case TipConstants.TIP_STATE_TRADEATLAST:
                return TipConstants.TIP_DB_STATE_TRADEATLAST;
            //Added for SR2 NEGDEALS ONLY
            case TipConstants.TIP_STATE_NEGDEALS_ONLY:           
                return TipConstants.TIP_DB_STATE_NEGDEALSONLY;
                
              //Added for SR2 NEGDEALS ONLY
            case TipConstants.TIP_STATE_NEGDEALS_NOTPLUS0:           
                return TipConstants.TIP_DB_STATE_NEGDEALSONLY;
                
            case TipConstants.TIP_DB_STATE_CLOSED:
        }
        return prev_state;
    }
	
	
	public static synchronized int setStateBySTaMessage(int prev_state, int state) {
        switch(state) {
            case TipConstants.TIP_MARKET_STATE_CLOSED:
                return TipConstants.TIP_DB_STATE_CLOSED;
            case TipConstants.TIP_MARKET_STATE_PRE_TRADING:
                return TipConstants.TIP_DB_STATE_PREOPENED;
            case TipConstants.TIP_MARKET_STATE_TRADING:
                return TipConstants.TIP_DB_STATE_OPENED;
            case TipConstants.TIP_MARKET_STATE_POST_TRADING:
                return TipConstants.TIP_DB_STATE_PRECLOSED;
            case TipConstants.TIP_MARKET_STATE_CLOSING_AUCTION:
                return TipConstants.TIP_DB_STATE_CLOSING_AUCTION;
            //Added for SR3
            case TipConstants.TIP_MARKET_STATE_TRADEATLAST:
                return TipConstants.TIP_DB_STATE_TRADEATLAST;
           //Added for SR2 NEGDEALS ONLY
            case TipConstants.TIP_MARKET_STATE_NEGDEALS_ONLY:           
                return TipConstants.TIP_DB_STATE_NEGDEALSONLY;
            case TipConstants.TIP_MARKET_STATE_NEGDEALS_NOTPLUS0:
            	return TipConstants.TIP_DB_STATE_NEGDEALSONLY;
        }
        return prev_state;
    }
	
	

}
