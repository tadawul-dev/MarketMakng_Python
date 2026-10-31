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
    public long BondId;
    /**Exchange code*/
    public String fk_pk_exchange_code;
    /**Symbol*/
    public String fk_fi_symbol;
    /**Transaction date*/
    public Timestamp transaction_date;
    /**Last Traded price*/
    public double last_trade_price;
    /**Last Traded Quantity*/
    public int last_trade_quantity;
    /**Last Updated Time*/
    public Timestamp last_update_time;
    /**Market Status - If the symbol is suspended, this status will show
     * suspended but current state field will reflect the state properly*/
    public int market_status;
    /**Current State*/
    public volatile int current_state;
    /**Ask price 5 level Values*/
    public BestBidOffVO[] bestBids;
    /**Bid price 5 level Values*/
    public BestBidOffVO[] bestOffers;
    /**Inputmessage*/
    public String messageType;
    /**Ask price*/
    public double ask_price;
    /**Ask Quantity*/
    public long ask_quantity;
    /**Bid price*/
    public double bid_price;
    /**Bid Quantity*/
    public long bid_quantity;
    /**Net change*/
    public double net_change;
    /**High Price*/
    public double high_price;
    /**High52weeks price*/
    public double high_52_weeks_price;
    /**Low Price*/
    public double low_price;
    /**Low52weeks price*/
    public double low_52_weeks_price;
    /**No of Trades*/
    public long no_of_trades;
    /**Percent change*/
    public double percent_change;
    /**Previous close price*/
    public double Previous_close_price;
    /**Previous close price*/
    public double todays_close_price;
    /**Todays open price*/
    public double Todays_open_price;
    /**Turn over*/
    public double turnover;
    /**Volume Traded*/
    public long volume_traded;
    /**High52weeks Date*/
    public Date high_52_weeks_date;
    /**Low52weeks Date*/
    public Date low_52_weeks_date;
    /**isNew*/
    public boolean isNew;
    
    //Added for Yield calculations projects CRQ21157
    //Traded Yield
    public double traded_yield;
    
    //Best Bid Yield
    public double bestBid_yield;
    
    //Best Offer Yield
    public double bestOffer_yield;
    
    //ISIN
    public String isin;
    
    //Added for Debt Market EnhancementsCRQ21357
    //OUTSTANDING_AMOUNT
    public double outstandingAmount;
    
    //ISSUE_CURRENCY
    public String issueCurrency;
    
    //PAR_VALUE
    public double parValue;
    
    //COUPON_FREQUENCY
    public double couponFrequency;
    
    //DAYCOUNT_METHOD
    public int dayCountMethod;
    
    //COUPON_RATE
    public double couponRate;
    
    //MATURITY_DATE
    public Date maturityDate;
    
    //RATE_CALCTYPE
    public int rateCalcType;
    
    //LISTING_DATE
    public Date listingDate;
    
    //ISSUE_DATE
    public Date issueDate;
    
    //SETTLEMENT_DATE
    public Date settlementDate;
    
    /**Turn over*/
    public double issueAmount;
    
    //Added for Sukuk & Bonds Negotiated deals
    public boolean isSpecialShare;
    
    public int updateCode;
    
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
        bestBids = BestBidOffVO.copyOf(tmpTipBond.bestBids, DBManagerMarket.ORDER_BOOK_DEPTH);
        bestOffers = BestBidOffVO.copyOf(tmpTipBond.bestOffers, DBManagerMarket.ORDER_BOOK_DEPTH);

        messageType = tmpTipBond.messageType;
    }
}
