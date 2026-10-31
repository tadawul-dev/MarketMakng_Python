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
 * Name          :     FileName : TIP_ShareVO.java
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import sa.com.tadawul.website.tip.shared.DBManagerMarket;
import sa.com.tadawul.website.tip.shared.TIPDataObjects;
import sa.com.tadawul.website.tip.shared.TipConstants;
import sa.com.tadawul.website.tip.vo.TIP_BaseVO;

/**
 * @description This class contains attributes to get and set Share related information
 * @version 1.0
 */

public class TIP_ShareVO extends TIP_BaseVO{
    /**Inputmessage*/
    public volatile long Id;
    /**Inputmessage*/
    public volatile String fk_pk_exchange_code;
    /**Inputmessage*/
    public volatile String fk_te_symbol;
    /**Market Status - If the symbol is suspended, this status will show
     * suspended but current state field will reflect the state properly*/
    public volatile int market_status;
    /**Current State*/
    public volatile int current_state;
    /**Inputmessage*/
    public volatile Timestamp transaction_date;
    /**Inputmessage*/
    public volatile double last_trade_price;
    /**Inputmessage*/
    public volatile int last_trade_quantity;
    /**Inputmessage*/
    public volatile Timestamp last_update_time;
    /**Inputmessage*/
    public volatile BestBidOffVO[] bestBids;
    /**Inputmessage*/
    public volatile BestBidOffVO[] bestOffers;
    /**Inputmessage*/
    public volatile String messageType;
    /**Inputmessage*/
    public volatile double ask_price;
    /**Inputmessage*/
    public volatile long ask_quantity;
    /**Inputmessage*/
    public volatile double bid_price;
    /**Inputmessage*/
    public volatile long bid_quantity;
    /**Inputmessage*/
    public volatile double net_change;
    /**Inputmessage*/
    public volatile double high_price;
    /**Inputmessage*/
    public volatile double high_52_weeks_price;
    /**Inputmessage*/
    public volatile double low_price;
    /**Inputmessage*/
    public volatile double low_52_weeks_price;
    /**Inputmessage*/
    public volatile double market_cap;
    /**Inputmessage*/
    public volatile long no_of_trades;
    /**Pbr*/
    public volatile double Pbr;
    /**percentage*/
    public volatile double Per;
    /**Percent Change*/
    public volatile double percent_change;
    /**previous close Price*/
    public volatile double Previous_close_price;
    /**Todays close price*/
    public volatile double todays_close_price;
    /**Todays Open*/
    public volatile double Todays_open;
    /**Turn over*/
    public volatile double turnover;
    /**Volume Traded*/
    public volatile long volume_traded;
    /**yield*/
    public volatile double yield;
    /**High52 Weeks Date*/
    public volatile Date high_52_weeks_date;
    /**Low52 Weeks Date*/
    public volatile Date low_52_weeks_date;
    
    //Added for Rights Issue Reference Price Upper and Reference Price Lower Limits
    /**Reference price Upper Limit*/
    public volatile int reference_price_upperlimit;
    /**Reference price Lower Limit*/
    public volatile int reference_price_lowerlimit;
    
    //Added for Theoritical price & volume
    /** Indicative Open Price*/
    public volatile double indicative_Open_price;
    /** Indicative Open Volume*/
    public volatile long indicative_Open_volume;
    
   // public long last_SpecialTradeSequenceNo;
    
    @SuppressWarnings("rawtypes")
	public volatile List specialTradesSequenceNos;
    
    /**Trans Time*/
    //public Timestamp trans_time;
    /**Name*/
    //public String Name;
    /**Market*/
    //public long Market;
    /**TotalIssued*/
    //public long TotalIssued;
    /**SharesListed*/
    //public long SharesListed;
    /**FaceValue*/
    //public long FaceValue;
    /**ShareCapital*/
    //public long ShareCapital;
    /**ShortName*/
    //public String ShortName;
    /**isNew*/
    public volatile boolean isNew;
    /**last Normal Trade Time*/
    public volatile Timestamp lastNormalTradeTime;
    
    /** Added for Share RI */
    public volatile boolean isShareRI;
    
    /** Added for Share ETF */
    public volatile boolean isShareETF;    

    public volatile boolean isSpecialShare;
    
  //LISTING_DATE
    public volatile Date listingDate;
    
    
    
    
    /**
     * This constructor is called to initialize all Share related attributes
     */
    @SuppressWarnings("rawtypes")
	public TIP_ShareVO(){
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
        
        Id = 0;
        fk_pk_exchange_code = "";
        fk_te_symbol = "";
        market_status = TipConstants.TIP_DB_STATE_CLOSED;
        current_state = TipConstants.TIP_DB_STATE_CLOSED;
        transaction_date = new Timestamp(System.currentTimeMillis());
        last_trade_price = 0;
        last_trade_quantity = 0;
        last_update_time = new Timestamp(System.currentTimeMillis());
        ask_price = 0;
        ask_quantity = 0;
        bid_price = 0;
        bid_quantity = 0;
        net_change = 0;
        high_price = 0;
        high_52_weeks_price = 0;
        low_price = 0;
        low_52_weeks_price = 0;
        market_cap = 0;
        no_of_trades = 0;
        Pbr = 0;
        Per = 0;
        percent_change = 0;
        Previous_close_price = 0;
        todays_close_price = 0;
        Todays_open = 0;
        turnover = 0;
        volume_traded = 0;
        yield = 0;
        high_52_weeks_date = new Date(System.currentTimeMillis());
        low_52_weeks_date = new Date(System.currentTimeMillis());
        reference_price_upperlimit = 0;
        reference_price_lowerlimit =0;
        
      //Added for Theoritical price & volume
        indicative_Open_price=0;
        indicative_Open_volume=0;
        
        //last_SpecialTradeSequenceNo=0;
        
        specialTradesSequenceNos = new ArrayList();
        //trans_time = new Timestamp(System.currentTimeMillis());;
        /*Name="";
        Market = 0;
        TotalIssued = 0;
        SharesListed = 0;
        FaceValue = 0;
        ShareCapital = 0;
        ShortName = "";
        */
        isNew = false;
        lastNormalTradeTime = new Timestamp(System.currentTimeMillis());
        
        //Added for Share RI
        isShareRI = false;
        
        //Added for ETF
        isShareETF = false;        
       
        /*For handling the Put through*/
        isSpecialShare =false;
        listingDate = new Date(System.currentTimeMillis());
        
        
        /*For handling Best Bid and Best Offer of DBManagerMarket.ORDER_BOOK_DEPTH */
        bestBids = getInitailBestBidsOffers();
        bestOffers = getInitailBestBidsOffers();
        messageType ="";
    }
    
    
    /**
     * This constructor is called to set all Share related values into attributes.
     * @param tmpShareVO contains Share related attributes
     */
    public TIP_ShareVO(TIP_ShareVO tmpShareVO){
        frameNo = tmpShareVO.frameNo;
        analyserStartTime = tmpShareVO.analyserStartTime;
        analyserEndTime = tmpShareVO.analyserEndTime;
        readerStartTime = tmpShareVO.readerStartTime;
        readerEndTime = tmpShareVO.readerEndTime;
        sequence_no = tmpShareVO.sequence_no;
        isBOD = tmpShareVO.isBOD;
        isEOD = tmpShareVO.isEOD;
        isUpdated = tmpShareVO.isUpdated;
        isEndOfBatch = tmpShareVO.isEndOfBatch;
        
        Id = tmpShareVO.Id;
        fk_pk_exchange_code = tmpShareVO.fk_pk_exchange_code;
        fk_te_symbol = tmpShareVO.fk_te_symbol;
        market_status = tmpShareVO.market_status;
        current_state = tmpShareVO.current_state;
        transaction_date = tmpShareVO.transaction_date;
        last_trade_price = tmpShareVO.last_trade_price;
        last_trade_quantity = tmpShareVO.last_trade_quantity;
        last_update_time = tmpShareVO.last_update_time;
        ask_price = tmpShareVO.ask_price;
        ask_quantity = tmpShareVO.ask_quantity;
        bid_price = tmpShareVO.bid_price;
        bid_quantity = tmpShareVO.bid_quantity;
        net_change = tmpShareVO.net_change;
        high_price = tmpShareVO.high_price;
        high_52_weeks_price = tmpShareVO.high_52_weeks_price;
        low_price = tmpShareVO.low_price;
        low_52_weeks_price = tmpShareVO.low_52_weeks_price;
        market_cap = tmpShareVO.market_cap;
        no_of_trades = tmpShareVO.no_of_trades;
        Pbr = tmpShareVO.Pbr;
        Per = tmpShareVO.Per;
        percent_change = tmpShareVO.percent_change;
        Previous_close_price = tmpShareVO.Previous_close_price;
        todays_close_price = tmpShareVO.todays_close_price;
        Todays_open = tmpShareVO.Todays_open;
        turnover = tmpShareVO.turnover;
        volume_traded = tmpShareVO.volume_traded;
        yield = tmpShareVO.yield;
        high_52_weeks_date = tmpShareVO.high_52_weeks_date;
        low_52_weeks_date = tmpShareVO.low_52_weeks_date;
        
        //Added for Rights Issue changes for referencePrice Upper Limit and referenceprice Lower limit
        reference_price_upperlimit = tmpShareVO.reference_price_upperlimit;
        reference_price_lowerlimit = tmpShareVO.reference_price_lowerlimit;
        
      //Added for Theoritical price & volume
        indicative_Open_price = tmpShareVO.indicative_Open_price;
        indicative_Open_volume = tmpShareVO.indicative_Open_volume;
        
       // last_SpecialTradeSequenceNo=tmpShareVO.last_SpecialTradeSequenceNo;
        specialTradesSequenceNos=tmpShareVO.specialTradesSequenceNos;

        
        //trans_time = tmpShareVO.trans_time;
        /*Name = tmpShareVO.Name;
        Market = tmpShareVO.Market;
        TotalIssued = tmpShareVO.TotalIssued;
        SharesListed = tmpShareVO.SharesListed;
        FaceValue = tmpShareVO.FaceValue;
        ShareCapital = tmpShareVO.ShareCapital;
        ShortName = tmpShareVO.ShortName;
        */
        isNew = tmpShareVO.isNew;
        lastNormalTradeTime = tmpShareVO.lastNormalTradeTime;
        
        //Added for Share RI
        isShareRI = tmpShareVO.isShareRI;
        
        //Added for ETF
        isShareETF = tmpShareVO.isShareETF;
        
        //Added For Special Share
        isSpecialShare =tmpShareVO.isSpecialShare;
        
        listingDate = tmpShareVO.listingDate;
        
        //Added for BestBid abd BestOffer of 5 entries.
        bestBids = new BestBidOffVO[DBManagerMarket.ORDER_BOOK_DEPTH];
        bestOffers = new BestBidOffVO[DBManagerMarket.ORDER_BOOK_DEPTH];
        for (int i = 0; i < DBManagerMarket.ORDER_BOOK_DEPTH; i++) {
            bestBids[i] = new BestBidOffVO(tmpShareVO.bestBids[i].level, tmpShareVO.bestBids[i].price, tmpShareVO.bestBids[i].quantity,tmpShareVO.bestBids[i].orders);
            bestOffers[i] = new BestBidOffVO(tmpShareVO.bestOffers[i].level,tmpShareVO.bestOffers[i].price, tmpShareVO.bestOffers[i].quantity, tmpShareVO.bestOffers[i].orders);
        }

        messageType = tmpShareVO.messageType;
        
    }
    
     class  SortbyId implements Comparator<TIP_ShareVO>
    {

		@Override
		public int compare(TIP_ShareVO o1, TIP_ShareVO o2) {
			
			//return (int) o1.Id - (int) o2.Id;
			return Long.compare(o1.Id, o2.Id);
		}
    	
    }
    
    public  void sortArray()
    {
    	Arrays.sort(TIPDataObjects.tipShare, new SortbyId());
    }
}
