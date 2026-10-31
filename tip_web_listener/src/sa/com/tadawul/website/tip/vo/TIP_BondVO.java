/**
 * #---------------------------------------------------------------------------------------------
 * #   This source is part of the Tadawul Web Listener, Saudi Stock Exchange
 * #   Copyright (c) 2009 Saudi Stock Exchange
 * #
 * #
 * #   All rights reserved.  No part of this source or any copyrights subsisting or arising out of these materials may be reproduced, distributed, amended, modified, adapted, stored in a
 * #   retrieval system, adopted or transmitted in any form or by any means,
 * #   electronic, mechanical, photographic, graphic, optic recording or otherwise,
 * #   translated in any language or computer language, without the prior written
 * #   permission of Saudi Stock Exchange
 * #
 * # ---------------------------------------------------------------------------------------------
 * ---------------------------------------------------------------------------------------------
 * Name          :     FileName : TIP_BondVO.java
 *
 * Version             Date                     Author                  Change Description
 *----------------------------------------------------------------------------------------------
 * 1.0                 10/April/2009            Oracle                   Initial Version
 * 1.1				   14/November/2009         Mohanram,Oracle          Added functionalities to process the Equity Rights Share
 * 1.2				   02/February/2010         Mohanram,Oracle          Added functionalities to process the Exchange Traded Funds.	
 *----------------------------------------------------------------------------------------------
 */
package sa.com.tadawul.website.tip.vo;

import java.sql.Date;
import java.sql.Timestamp;

import sa.com.tadawul.website.tip.shared.DBManagerMarket;
import sa.com.tadawul.website.tip.shared.TipConstants;
import sa.com.tadawul.website.tip.vo.TIP_BaseVO;

/**
 * @description This class contains attributes to get and set Bond related information.
 * @ version 1.0
 */

public class TIP_BondVO extends TIP_BaseVO{
    
    /**Bond Id*/
    public volatile long BondId;
    /**Exchange code*/
    public volatile String fk_pk_exchange_code;
    /**Symbol*/
    public volatile String fk_fi_symbol;
    /**Transaction date*/
    public volatile Timestamp transaction_date;
    /**Last Traded price*/
    public volatile double last_trade_price;
    /**Last Traded Quantity*/
    public volatile int last_trade_quantity;
    /**Last Updated Time*/
    public volatile Timestamp last_update_time;
    /**Market Status - If the symbol is suspended, this status will show
     * suspended but current state field will reflect the state properly*/
    public volatile int market_status;
    /**Current State*/
    public volatile int current_state;
    /**Ask price 5 level Values*/
    public volatile BestBidOffVO[] bestBids;
    /**Bid price 5 level Values*/
    public volatile BestBidOffVO[] bestOffers;
    /**Inputmessage*/
    public volatile String messageType;
    /**Ask price*/
    public volatile double ask_price;
    /**Ask Quantity*/
    public volatile long ask_quantity;
    /**Bid price*/
    public volatile double bid_price;
    /**Bid Quantity*/
    public volatile long bid_quantity;
    /**Net change*/
    public volatile double net_change;
    /**High Price*/
    public volatile double high_price;
    /**High52weeks price*/
    public volatile double high_52_weeks_price;
    /**Low Price*/
    public volatile double low_price;
    /**Low52weeks price*/
    public volatile double low_52_weeks_price;
    /**No of Trades*/
    public volatile long no_of_trades;
    /**Percent change*/
    public volatile double percent_change;
    /**Previous close price*/
    public volatile double Previous_close_price;
    /**Previous close price*/
    public volatile double todays_close_price;
    /**Todays open price*/
    public volatile double Todays_open_price;
    /**Turn over*/
    public volatile double turnover;
    /**Volume Traded*/
    public volatile long volume_traded;
    /**High52weeks Date*/
    public volatile Date high_52_weeks_date;
    /**Low52weeks Date*/
    public volatile Date low_52_weeks_date;
    /**isNew*/
    public volatile boolean isNew;
    
    //Added for Yield calculations projects CRQ21157
    //Traded Yield
    public volatile double traded_yield;
    
    //Best Bid Yield
    public volatile double bestBid_yield;
    
    //Best Offer Yield
    public volatile double bestOffer_yield;
    
    //ISIN
    public volatile String isin;
    
    //Added for Debt Market EnhancementsCRQ21357
    //OUTSTANDING_AMOUNT
    public volatile double outstandingAmount;
    
    //ISSUE_CURRENCY
    public volatile String issueCurrency;
    
    //PAR_VALUE
    public volatile double parValue;
    
    //COUPON_FREQUENCY
    public volatile double couponFrequency;
    
    //DAYCOUNT_METHOD
    public volatile int dayCountMethod;
    
    //COUPON_RATE
    public volatile double couponRate;
    
    //MATURITY_DATE
    public volatile Date maturityDate;
    
    //RATE_CALCTYPE
    public volatile int rateCalcType;
    
    //LISTING_DATE
    public volatile Date listingDate;
    
    //ISSUE_DATE
    public volatile Date issueDate;
    
    //SETTLEMENT_DATE
    public volatile Date settlementDate;
    
    /**Turn over*/
    public volatile double issueAmount;
    
    //Added for Sukuk & Bonds Negotiated deals
    public volatile boolean isSpecialShare;
    
    public volatile int updateCode;
    
    /**
     * This constructor is called to initialize all Bond related attributes
     */
    public  TIP_BondVO(){
        frameNo = 0;
        analyserStartTime = 0;
        analyserEndTime = 0;
        readerStartTime = 0;
        readerEndTime = 0;
        sequence_no = 0;
        isBOD = false;
        isEOD = false;
        isUpdated = false;
        isEndOfBatch = false;
        
        BondId =0;
        fk_pk_exchange_code="";
        fk_fi_symbol="";
        transaction_date=new Timestamp(System.currentTimeMillis());
        last_trade_price=0;
        last_trade_quantity=0;
        last_update_time=new Timestamp(System.currentTimeMillis());
        market_status = TipConstants.TIP_DB_STATE_CLOSED;
        current_state = TipConstants.TIP_DB_STATE_CLOSED;
        ask_price=0;
        ask_quantity=0;
        bid_price=0;
        bid_quantity=0;
        net_change=0;
        high_price=0;
        high_52_weeks_price=0;
        low_price=0;
        low_52_weeks_price=0;
        no_of_trades=0;
        percent_change=0;
        Previous_close_price=0;
        todays_close_price=0;
        Todays_open_price=0;
        turnover=0;
        volume_traded=0;
        high_52_weeks_date=new Date(System.currentTimeMillis());
        low_52_weeks_date=new Date(System.currentTimeMillis());
        isNew = false;
        //Added for Yield calculations projects CRQ21157
        traded_yield =0;
        bestBid_yield=0;
        bestOffer_yield=0;
        isin="";
        
        //Added for Debt Market EnhancementsCRQ21357
        outstandingAmount=0;
        issueCurrency="";
        parValue=0;
        couponFrequency=0;
        dayCountMethod=0;
        couponRate=0;
        maturityDate = new Date(System.currentTimeMillis());
        rateCalcType=0;
        
        listingDate = new Date(System.currentTimeMillis());
        issueDate = new Date(System.currentTimeMillis());
        settlementDate = new Date(System.currentTimeMillis());
        issueAmount =0;
        
        //Added for Sukuk & Bonds Negotiated deals
        isSpecialShare = false;
        updateCode =TipConstants.UPDATE_CODE_UNCHANGED;
        
        /*For handling Best Bid and Best Offer of DBManagerMarket.ORDER_BOOK_DEPTH */
        bestBids = getInitailBestBidsOffers();
        bestOffers = getInitailBestBidsOffers();
        messageType="";

    }
    /**
     * This constructor is called to set all  Bond related values into attributes.
     * @param tmpTipBond contains Bond related attributes
     */
    public  TIP_BondVO(TIP_BondVO tmpTipBond){
        frameNo = tmpTipBond.frameNo;
        analyserStartTime = tmpTipBond.analyserStartTime;
        analyserEndTime = tmpTipBond.analyserEndTime;
        readerStartTime = tmpTipBond.readerStartTime;
        readerEndTime = tmpTipBond.readerEndTime;
        sequence_no = tmpTipBond.sequence_no;
        isBOD = tmpTipBond.isBOD;
        isEOD = tmpTipBond.isEOD;
        isUpdated = tmpTipBond.isUpdated;
        isEndOfBatch = tmpTipBond.isEndOfBatch;
        
        BondId =tmpTipBond.BondId;
        fk_pk_exchange_code=tmpTipBond.fk_pk_exchange_code;
        fk_fi_symbol=tmpTipBond.fk_fi_symbol;
        transaction_date=tmpTipBond.transaction_date;
        last_trade_price=tmpTipBond.last_trade_price;
        last_trade_quantity=tmpTipBond.last_trade_quantity;
        last_update_time=tmpTipBond.last_update_time;
        market_status = tmpTipBond.market_status;
        current_state = tmpTipBond.current_state;
        ask_price=tmpTipBond.ask_price;
        ask_quantity=tmpTipBond.ask_quantity;
        bid_price=tmpTipBond.bid_price;
        bid_quantity=tmpTipBond.bid_quantity;
        net_change=tmpTipBond.net_change;
        high_price=tmpTipBond.high_price;
        high_52_weeks_price=tmpTipBond.high_52_weeks_price;
        low_price=tmpTipBond.low_price;
        low_52_weeks_price=tmpTipBond.low_52_weeks_price;
        no_of_trades=tmpTipBond.no_of_trades;
        percent_change=tmpTipBond.percent_change;
        Previous_close_price=tmpTipBond.Previous_close_price;
        todays_close_price=tmpTipBond.todays_close_price;
        Todays_open_price=tmpTipBond.Todays_open_price;
        turnover=tmpTipBond.turnover;
        volume_traded=tmpTipBond.volume_traded;
        high_52_weeks_date=tmpTipBond.high_52_weeks_date;
        low_52_weeks_date=tmpTipBond.low_52_weeks_date;
        isNew = tmpTipBond.isNew;
        //Added for Yield calculations projects CRQ21157
        traded_yield =tmpTipBond.traded_yield;
        bestBid_yield=tmpTipBond.bestBid_yield;
        bestOffer_yield=tmpTipBond.bestOffer_yield;
        isin=tmpTipBond.isin;
       
      //Added for Debt Market EnhancementsCRQ21357
        outstandingAmount=tmpTipBond.outstandingAmount;
        issueCurrency=tmpTipBond.issueCurrency;
        parValue=tmpTipBond.parValue;
        couponFrequency=tmpTipBond.couponFrequency;
        dayCountMethod=tmpTipBond.dayCountMethod;
        couponRate=tmpTipBond.couponRate;
        maturityDate = tmpTipBond.maturityDate;
        rateCalcType=tmpTipBond.rateCalcType;
        
        listingDate = tmpTipBond.listingDate;
        issueDate = tmpTipBond.issueDate;
        settlementDate = tmpTipBond.settlementDate;
        issueAmount =tmpTipBond.issueAmount;
        
        //Added for Sukuk & Bonds Negotiated deals
        isSpecialShare = tmpTipBond.isSpecialShare;
        
        updateCode=tmpTipBond.updateCode;
        
        //Added for BestBid abd BestOffer of 5 entries.
        bestBids = new BestBidOffVO[DBManagerMarket.ORDER_BOOK_DEPTH];
        bestOffers = new BestBidOffVO[DBManagerMarket.ORDER_BOOK_DEPTH];
        for (int i = 0; i < DBManagerMarket.ORDER_BOOK_DEPTH; i++) {
            bestBids[i] = new BestBidOffVO(tmpTipBond.bestBids[i].level,tmpTipBond.bestBids[i].price, tmpTipBond.bestBids[i].quantity, tmpTipBond.bestBids[i].orders);
            bestOffers[i] = new BestBidOffVO(tmpTipBond.bestOffers[i].level,tmpTipBond.bestOffers[i].price, tmpTipBond.bestOffers[i].quantity, tmpTipBond.bestOffers[i].orders);
        }

        messageType = tmpTipBond.messageType;
    }
}
