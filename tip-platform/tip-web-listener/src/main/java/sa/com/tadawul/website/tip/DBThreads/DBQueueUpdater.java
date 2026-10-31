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
 * FileName : DataParserForLowVolm.java
 *
 * Description : 
 *
 * Created Date : 23 April 2018
 *
 * Version             Date                     Author                  Change Description
 *----------------------------------------------------------------------------------------------
 * 1.0                 23 April 2018            Mohanram                  Initial Version
 *----------------------------------------------------------------------------------------------
*/

package sa.com.tadawul.website.tip.DBThreads;


import java.util.function.UnaryOperator;
import java.util.Arrays;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import sa.com.tadawul.website.tip.queue.Buffer;
import sa.com.tadawul.website.tip.queue.MessageDatabaseQueue;
import sa.com.tadawul.website.tip.queue.MessageQueue;
import sa.com.tadawul.website.tip.shared.LoadInitialData;
import sa.com.tadawul.website.tip.shared.TIPDataObjects;
import sa.com.tadawul.website.tip.shared.TipConstants;
import sa.com.tadawul.website.tip.shared.TipThreads;
import sa.com.tadawul.website.tip.shared.Utility;
import sa.com.tadawul.website.tip.vo.TIP_BaseVO;
import sa.com.tadawul.website.tip.vo.TIP_BondIndexVO;
import sa.com.tadawul.website.tip.vo.TIP_BondVO;
import sa.com.tadawul.website.tip.vo.TIP_IndexVO;
import sa.com.tadawul.website.tip.vo.TIP_MarketVO;
import sa.com.tadawul.website.tip.vo.TIP_SMEIndexVO;
import sa.com.tadawul.website.tip.vo.TIP_SMEShareVO;
import sa.com.tadawul.website.tip.vo.TIP_ShareVO;

public class DBQueueUpdater  implements Runnable{
	
	private static final Logger logger = LogManager.getLogger(DBQueueUpdater.class);
	
	long quantaStart = System.currentTimeMillis();

	int ctr;
	int tmpIter1 = 0;
    int tmpIter2 = 0;

    int tmpTotMarketUpdated=0;
    int tmpTotIndexUpdated=0;
    int tmpTotSMEIndexUpdated=0;
    int tmpTotShareUpdated=0;
    int tmpTotSMEShareUpdated=0;
    int tmpTotBondUpdated=0;
    int tmpTotBondIndexUpdated=0;	
    
    //Added for PTTP Project CRQ22398
    int tmpTotOTCShareUpdated=0;
	
    
    int totalMarketUpdated=0;
    int totalIndexUpdated=0;
    int totalSMEIndexUpdated=0;
    int totalShareUpdated=0;
    int totalSMEShareUpdated=0;
    int totalBondUpdated=0;
    int totalBondIndexUpdated=0;
    //Added for PTTP Project CRQ22398
    int totalOTCShareUpdated=0;
    int tmpTotOTCBondUpdated=0;
    int totalOTCBondUpdated=0;
    int printIntervalCount=0;
    
	public DBQueueUpdater(){
		
	}

	@Override
	public void run() {
		
		logger.info("DBQueueUpdater Thread started. " );
		
		startDataBaseUpdatedThreads();
		
		while(true)
				{			
			
			     tmpTotMarketUpdated=0;
			     tmpTotIndexUpdated=0;
			     tmpTotSMEIndexUpdated=0;
			     tmpTotShareUpdated=0;
			     tmpTotSMEShareUpdated=0;
			     tmpTotBondUpdated=0;
			     tmpTotBondIndexUpdated=0;	
			     //Added for PTTP Project CRQ22398
			     tmpTotOTCShareUpdated=0;
			     printIntervalCount++;
			
			/* if (  TIPDataObjects.isShareBOD || TIPDataObjects.isBondBOD || TIPDataObjects.isShareEOD || TIPDataObjects.isBondEOD ||TIPDataObjects.isSMEShareEOD||TIPDataObjects.isSMEShareBOD ||
		                ((System.currentTimeMillis() - quantaStart) > TipConstants.TIP_MAX_DELAY_TIME
		                || currentCtr >= TipConstants.TIP_MAX_DELAY_MSGS)) {
	*/	        	
			
			//logger.info("isShareBOD :{} isSMEShareBOD  :{} ((System.currentTimeMillis() - quantaStart) > TipConstants.TIP_MAX_DELAY_TIME :{}", TIPDataObjects.isShareBOD, TIPDataObjects.isSMEShareBOD, ((System.currentTimeMillis() - quantaStart) > TipConstants.TIP_MAX_DELAY_TIME));
			
				 if (  TIPDataObjects.isShareBODPreOpen ||  TIPDataObjects.isShareBODOpen || TIPDataObjects.isBondBODPreOpen || TIPDataObjects.isBondBODOpen || TIPDataObjects.isShareEOD || TIPDataObjects.isBondEOD || TIPDataObjects.isSMEShareEOD || TIPDataObjects.isSMEShareBODPreOpen || TIPDataObjects.isSMEShareBODOpen ||
			                ((System.currentTimeMillis() - quantaStart) > TipConstants.TIP_MAX_DELAY_TIME  )) {			 
	
					 tmpTotMarketUpdated = TIPDataObjects.totMarketUpdated.getAndSet(0); // atomic read-and-reset
					 
					 totalMarketUpdated +=tmpTotMarketUpdated;
					 
		            ctr = 0;
		            if (tmpTotMarketUpdated > 0) {
		                Arrays.stream(TIPDataObjects.tipMarket, 0, TipConstants.TIP_MAX_NO_MARKET)
		                    .filter(market -> market.isUpdated)
		                    .forEach(market -> {
		                        ctr++;
		                        TIP_MarketVO MarketToQ = new TIP_MarketVO(market);
		                        if (logger.isDebugEnabled()) logger.debug("MarketToQ . Sequence no :{}", MarketToQ.sequence_no);
		                        if (ctr == tmpTotMarketUpdated)
		                            MarketToQ.isEndOfBatch = true;
		                        //Normal TIP Updates
		                        if (!TipConstants.IS_NEGDEALS_EARLY_START) {
		                            MessageDatabaseQueue.tipmarketQ.setFrame(MarketToQ);
		                            market.isUpdated = false;
		                        }
		                        //Incase if it is Early start for NegDeals changes
		                        if (TipConstants.IS_NEGDEALS_EARLY_START && TIPDataObjects.isAfterPreOpen && Utility.shouldUpdatedShares()) {
		                            if (logger.isDebugEnabled()) logger.debug("Market messages received after Market Pre-Open. Hence this will be updated :{}", market.Pk_exchange_code);
		                            MessageDatabaseQueue.tipmarketQ.setFrame(MarketToQ);
		                            market.isUpdated = false;
		                        } else {
		                            logger.info("Market messages received before Market Pre-Open. Hence this will not be updated :{}", market.Pk_exchange_code);
		                        }
		                    });
		            }
		            
		            tmpTotIndexUpdated = TIPDataObjects.totIndexUpdated.getAndSet(0); // atomic read-and-reset
		            
		            totalIndexUpdated +=tmpTotIndexUpdated;
		            if (tmpTotIndexUpdated > 0) {
		                Arrays.stream(TIPDataObjects.tipIndex, 0, TIPDataObjects.tipIndexIter)
		                    .filter(index -> index.isUpdated)
		                    .forEach(index -> {
		                        TIP_IndexVO IndexToQ = new TIP_IndexVO(index);
		                        if (logger.isDebugEnabled()) logger.debug("TIPDataObjects.isAfterPreOpen :{}", TIPDataObjects.isAfterPreOpen);
		                        //Normal TIP Updates
		                        if (!TipConstants.IS_NEGDEALS_EARLY_START) {
		                            MessageDatabaseQueue.tipindexQ.setFrame(IndexToQ);
		                            index.isUpdated = false;
		                        }
		                        //Incase if it is Early start for NegDeals changes
		                        if (TipConstants.IS_NEGDEALS_EARLY_START && TIPDataObjects.isAfterPreOpen && Utility.shouldUpdatedShares()) {
		                            if (logger.isDebugEnabled()) logger.debug("Index messages received after Market Pre-Open. Hence this will be updated :{}", index.Fk_te_symbol);
		                            MessageDatabaseQueue.tipindexQ.setFrame(IndexToQ);
		                            index.isUpdated = false;
		                        } else {
		                            if (logger.isDebugEnabled()) logger.debug("Index messages received before Market Pre-Open. Hence this will not be updated :{}", index.Fk_te_symbol);
		                        }
		                    });
		            }
		            
		            
		            tmpTotBondIndexUpdated = TIPDataObjects.totBondIndexUpdated.getAndSet(0); // atomic read-and-reset
		             
		            totalBondIndexUpdated +=tmpTotBondIndexUpdated;
		            flushUpdated(tmpTotBondIndexUpdated, TIPDataObjects.tipBondIndex, TIPDataObjects.tipBondIndexIter, TIP_BondIndexVO::new, MessageDatabaseQueue.tipBondindexQ);
		            
		            
		            tmpTotSMEIndexUpdated = TIPDataObjects.totSMEIndexUpdated.getAndSet(0); // atomic read-and-reset
		            
		            totalSMEIndexUpdated +=tmpTotSMEIndexUpdated;		           
		            flushUpdated(tmpTotSMEIndexUpdated, TIPDataObjects.tipSMEIndex, TIPDataObjects.tipSMEIndexIter, TIP_SMEIndexVO::new, MessageDatabaseQueue.tipSMEindexQ);
		            
		           
		            
		           tmpTotShareUpdated = TIPDataObjects.totShareUpdated.getAndSet(0); // atomic read-and-reset
		           
		           totalShareUpdated +=tmpTotShareUpdated;
		            flushUpdated(tmpTotShareUpdated, TIPDataObjects.tipShare, TIPDataObjects.tipShareIter, TIP_ShareVO::new, MessageDatabaseQueue.tipshareQ);		            
		            
		            
		            tmpTotSMEShareUpdated = TIPDataObjects.totSMEShareUpdated.getAndSet(0); // atomic read-and-reset
		            
		            totalSMEShareUpdated +=tmpTotSMEShareUpdated;
		            flushUpdated(tmpTotSMEShareUpdated, TIPDataObjects.tipSMEShare, TIPDataObjects.tipShareSMEIter, TIP_SMEShareVO::new, MessageDatabaseQueue.tipSMEshareQ);
		            
		            
		            tmpTotBondUpdated = TIPDataObjects.totBondUpdated.getAndSet(0); // atomic read-and-reset
		            
		            totalBondUpdated +=tmpTotBondUpdated;
		            flushUpdated(tmpTotBondUpdated, TIPDataObjects.tipBond, TIPDataObjects.tipBondIter, TIP_BondVO::new, MessageDatabaseQueue.tipbondQ);
		            
		            //Added for PTTP Project CRQ22398
		            tmpTotOTCShareUpdated = TIPDataObjects.totOTCShareUpdated.getAndSet(0); // atomic read-and-reset
			           
			           totalOTCShareUpdated +=tmpTotOTCShareUpdated;
			            flushUpdated(tmpTotOTCShareUpdated, TIPDataObjects.tipOTCShare, TIPDataObjects.tipOTCShareIter, TIP_ShareVO::new, MessageDatabaseQueue.tipOTCShareIntraQ);
		            
			          //Added for PTTP Project CRQ22398
			            tmpTotOTCBondUpdated = TIPDataObjects.totOTCBondUpdated.getAndSet(0); // atomic read-and-reset
			            
			            totalOTCBondUpdated +=tmpTotOTCBondUpdated;
			            flushUpdated(tmpTotOTCBondUpdated, TIPDataObjects.tipOTCBond, TIPDataObjects.tipOTCBonditer, TIP_BondVO::new, MessageDatabaseQueue.tipOTCbondQ);
			            
			            
		            if ( TIPDataObjects.isShareBODPreOpen ||  TIPDataObjects.isShareBODOpen || TIPDataObjects.isBondBODPreOpen || TIPDataObjects.isBondBODOpen  || TIPDataObjects.isShareEOD || TIPDataObjects.isBondEOD || TIPDataObjects.isSMEShareBODPreOpen || TIPDataObjects.isSMEShareBODOpen  || TIPDataObjects.isSMEShareEOD) {	            	
		            	
		            	TIP_BaseVO baseObj = new TIP_BaseVO();
		                baseObj.analyserEndTime=System.currentTimeMillis();
		                baseObj.isEndOfBatch = true;
		                
		                
		                if (TIPDataObjects.isShareBODPreOpen) {
		                    logger.info("Share Market BOD PreOpen received");
		                    baseObj.isBOD = true;
		                    TIPDataObjects.isShareBODPreOpen = false;		                   
		                	
			            	TIP_ShareVO baseShareObj = new TIP_ShareVO();
			            	baseShareObj.isEndOfBatch = true;
			            	baseShareObj.isBOD = true;
			            	baseShareObj.isBODPreOpen=true;
			            	baseShareObj.market_status=TipConstants.TIP_DB_STATE_PREOPENED;
		                   
			            	 TIP_MarketVO MarketToQ = new TIP_MarketVO(TIPDataObjects.tipMarket[0]);
			            	 MarketToQ.isEndOfBatch = true;
                   		  	MarketToQ.isBODPreOpen=true;
		                      MessageDatabaseQueue.tipmarketQ.setFrame(MarketToQ);
		                      MarketToQ = null;
			            	
			            	
		                    if(!LoadInitialData.dtShareIndexLastUpdateTime.equals(TipConstants.DB_FEED_REQ_DATE.trim())) {		                    	
		                    	MessageDatabaseQueue.tipshareQ.setFrame(baseShareObj);
		                    	MessageDatabaseQueue.tipShareIntraQ.setFrame(baseShareObj);
		                    	
		                        //logger.debug("Pk_exchange_code: " + tipMarket[0].Pk_exchange_code + " Seq# " + tipMarket[0].sequence_no
		                        //        + " lasttradeddate " + tipMarket[0].lasttradeddate);
		                    }
		                    
		                }else if(TIPDataObjects.isShareBODOpen)
		                {
		                		logger.info("Share Market BOD Open received");
			                    baseObj.isBOD = true;
			                    TIPDataObjects.isShareBODOpen = false;
			                    
			                    TIP_IndexVO baseIndexObj = new TIP_IndexVO();
				            	baseIndexObj.isEndOfBatch = true;
				            	baseIndexObj.isBOD = true;
				            	baseIndexObj.isBODOpen = true;
		                	
				            	  if(!LoadInitialData.dtShareIndexLastUpdateTime.equals(TipConstants.DB_FEED_REQ_DATE.trim())) {		                    	
				                    	MessageDatabaseQueue.tipindexQ.setFrame(baseIndexObj);
				                    	MessageDatabaseQueue.tipIndexIntraQ.setFrame(baseIndexObj);
				                        
				                    	if(!TipConstants.IS_PAST_OR_FUTURE_DATE)
				                    	{
				                        TIPDataObjects.tipMarket[0].no_of_trades = 0;
				                        TIPDataObjects.tipMarket[0].turnover = 0;
				                        TIPDataObjects.tipMarket[0].volume_traded = 0;
				                        TIPDataObjects.tipMarket[0].no_of_symbols_traded = 0;
				                        TIPDataObjects. tipMarket[0].no_of_downs = 0;
				                        TIPDataObjects.tipMarket[0].no_of_no_changes = 0;
				                        TIPDataObjects.tipMarket[0].no_of_ups = 0;
				                    	}

				                        TIP_MarketVO MarketToQ = new TIP_MarketVO(TIPDataObjects.tipMarket[0]);
				                        MarketToQ.isBOD = true;
				                        MarketToQ.isEndOfBatch = true;
				                        MarketToQ.isBODOpen = true;
				                        MessageDatabaseQueue.tipmarketQ.setFrame(MarketToQ);
				                        MarketToQ = null;
				                      
				                        //To fix BOD issue, we add 10 seconds delay
				                        try{
				                       	Thread.sleep(10000);
				                       }catch(Exception e)
				                   	{
				                   		logger.error("Error Occured");
				                   	}
				                  //logger.debug("Pk_exchange_code: " + tipMarket[0].Pk_exchange_code + " Seq# " + tipMarket[0].sequence_no
				                 //        + " lasttradeddate " + tipMarket[0].lasttradeddate);
				                    }
		                }		                
		                  else if (TIPDataObjects.isBondBODPreOpen) {
		                    logger.info("Bond Market BOD PreOpen received");
		                    baseObj.isBOD = true;
		                    TIPDataObjects.isBondBODPreOpen = false;
		                    
		                  
			            	TIP_BondVO baseBondObj = new TIP_BondVO();
			            	baseBondObj.isEndOfBatch = true;
			            	baseBondObj.isBOD = true;
			            	baseBondObj.isBODPreOpen=true;
			            	baseBondObj.market_status=TipConstants.TIP_DB_STATE_PREOPENED;
			            	
			            	 TIP_MarketVO MarketToQ = new TIP_MarketVO(TIPDataObjects.tipMarket[1]);
			            	 MarketToQ.isEndOfBatch = true;
			            	 MarketToQ.isBODPreOpen=true;
		                      MessageDatabaseQueue.tipmarketQ.setFrame(MarketToQ);
		                      MarketToQ = null;
		                      
			            	
		                    if(!LoadInitialData.dtBondLastUpdateTime.equals(TipConstants.DB_FEED_REQ_DATE.trim())) {	                    	
		                    	MessageDatabaseQueue.tipbondQ.setFrame(baseBondObj);
		                    	MessageDatabaseQueue.tipBondIntraQ.setFrame(baseBondObj);  
		                    	//Added for PTTP Project 
		                    	MessageDatabaseQueue.tipOTCbondQ.setFrame(baseBondObj);
		                    	MessageDatabaseQueue.tipOTCBondIntraQ.setFrame(baseBondObj);       
		                    		                        
		                  
		                    }		                    
		                } else if (TIPDataObjects.isBondBODOpen) {
		                    logger.info("Bond Market Open BOD received");
		                    baseObj.isBOD = true;
		                    TIPDataObjects.isBondBODOpen = false;
		                    
		                    TIP_BondIndexVO baseIndexObj = new TIP_BondIndexVO();
			            	baseIndexObj.isEndOfBatch = true;
			            	baseIndexObj.isBOD = true;
			            	baseIndexObj.isBODOpen = true;
			            	
			            	if(!LoadInitialData.dtBondLastUpdateTime.equals(TipConstants.DB_FEED_REQ_DATE.trim())) {

		                    	MessageDatabaseQueue.tipBondindexQ.setFrame(baseIndexObj);
		                    	MessageDatabaseQueue.tipBondIndexIntraQ.setFrame(baseIndexObj);              
		                    	
		                    	if(!TipConstants.IS_PAST_OR_FUTURE_DATE)
		                    	{
		                        TIPDataObjects.tipMarket[1].no_of_trades = 0;
		                        TIPDataObjects.tipMarket[1].turnover = 0;
		                        TIPDataObjects.tipMarket[1].volume_traded = 0;
		                        TIPDataObjects.tipMarket[1].no_of_symbols_traded = 0;
		                        TIPDataObjects.tipMarket[1].no_of_downs = 0;
		                        TIPDataObjects.tipMarket[1].no_of_no_changes = 0;
		                        TIPDataObjects. tipMarket[1].no_of_ups = 0;
		                    	}
		                        TIP_MarketVO MarketToQ = new TIP_MarketVO(TIPDataObjects.tipMarket[1]);
		                        MarketToQ.isBOD = true;
		                        MarketToQ.isEndOfBatch = true;
		                        MarketToQ.isBODOpen = true;
		                        MessageDatabaseQueue.tipmarketQ.setFrame(MarketToQ);
		                        MarketToQ = null;
		                        //To fix BOD issue, we add 10 seconds delay
		                        try{
		                       	 Thread.sleep(10000);
		                       }catch(Exception e)
		                   	{
		                   		logger.error("Error Occured");
		                   	}
		                    }		                    
		                }   else if (TIPDataObjects.isSMEShareBODPreOpen) {
		                    logger.info("SME Share Market PreOpen BOD received");
		                    baseObj.isBOD = true;
		                    TIPDataObjects. isSMEShareBODPreOpen = false;
		                    
		                    TIP_SMEShareVO baseSMEShareObj = new TIP_SMEShareVO();
			            	baseSMEShareObj.isEndOfBatch = true;
			            	baseSMEShareObj.isBOD = true;
			            	baseSMEShareObj.isBODPreOpen=true;
			            	baseSMEShareObj.market_status=TipConstants.TIP_DB_STATE_PREOPENED;
			            	
			            	TIP_MarketVO MarketToQ = new TIP_MarketVO(TIPDataObjects.tipMarket[2]);
			            	MarketToQ.isEndOfBatch = true;
			            	MarketToQ.isBODPreOpen=true;
		                      MessageDatabaseQueue.tipmarketQ.setFrame(MarketToQ);
		                      MarketToQ = null;
			            	
		                    if(!LoadInitialData.dtSMEShareIndexLastUpdateTime.equals(TipConstants.DB_FEED_REQ_DATE.trim())) {
		                    	MessageDatabaseQueue.tipSMEshareQ.setFrame(baseSMEShareObj);
		                    	MessageDatabaseQueue.tipSMEShareIntraQ.setFrame(baseSMEShareObj);
		                    	
		                                           
		                    
		                        //logger.debug("Pk_exchange_code: " + tipMarket[0].Pk_exchange_code + " Seq# " + tipMarket[0].sequence_no
		                        //        + " lasttradeddate " + tipMarket[0].lasttradeddate);
		                    }
		                    
		                } else if (TIPDataObjects.isSMEShareBODOpen) {
		                    logger.info("SME Share Market Open BOD received");
		                    baseObj.isBOD = true;
		                    TIPDataObjects. isSMEShareBODOpen = false;
		                    
		                    TIP_SMEIndexVO baseIndexObj = new TIP_SMEIndexVO();
			            	baseIndexObj.isEndOfBatch = true;
			            	baseIndexObj.isBOD = true;
			            	baseIndexObj.isBODOpen = true;
			            	
			            	
		                    if(!LoadInitialData.dtSMEShareIndexLastUpdateTime.equals(TipConstants.DB_FEED_REQ_DATE.trim())) {
		                    	MessageDatabaseQueue.tipSMEindexQ.setFrame(baseIndexObj);
		                    	MessageDatabaseQueue.tipSMEIndexIntraQ.setFrame(baseIndexObj);
		                        
		                    	if(!TipConstants.IS_PAST_OR_FUTURE_DATE)
		                    	{
		                        TIPDataObjects.tipMarket[2].no_of_trades = 0;
		                        TIPDataObjects.tipMarket[2].turnover = 0;
		                        TIPDataObjects.tipMarket[2].volume_traded = 0;
		                        TIPDataObjects.tipMarket[2].no_of_symbols_traded = 0;
		                        TIPDataObjects.tipMarket[2].no_of_downs = 0;
		                        TIPDataObjects.tipMarket[2].no_of_no_changes = 0;
		                        TIPDataObjects.tipMarket[2].no_of_ups = 0;
		                    	}
		                        
		                        TIP_MarketVO MarketToQ = new TIP_MarketVO(TIPDataObjects.tipMarket[2]);
		                        MarketToQ.isBOD = true;
		                        MarketToQ.isEndOfBatch = true;
		                        MarketToQ.isBODOpen = true;
		                        MessageDatabaseQueue.tipmarketQ.setFrame(MarketToQ);
		                        MarketToQ = null;
		                        //To fix BOD issue, we add 10 seconds delay
		                        try{
		                       	 Thread.sleep(10000);
		                       }catch(Exception e)
		                   	{
		                   		logger.error("Error Occured");
		                   	}
		                        //logger.debug("Pk_exchange_code: " + tipMarket[0].Pk_exchange_code + " Seq# " + tipMarket[0].sequence_no
		                        //        + " lasttradeddate " + tipMarket[0].lasttradeddate);
		                    }
		                    
		                }else if (TIPDataObjects.isShareEOD) {
		                	
		                	    TIP_IndexVO baseIndexObj = new TIP_IndexVO();
				            	baseIndexObj.isEndOfBatch = true;
				            	baseIndexObj.isEOD = true;		
				            	
				            	TIP_ShareVO baseShareObj = new TIP_ShareVO();
				            	baseShareObj.isEndOfBatch = true;
				            	baseShareObj.isEOD = true;
		                	
		                    logger.info("Share Market EOD received");
		                    baseObj.isEOD = true;
		                    TIPDataObjects.isShareEOD = false;
		                    MessageDatabaseQueue.tipshareQ.setFrame(baseShareObj);
		                    //messagedatabaseQueue.tipindexQ.setFrame(baseObj);
		                    MessageDatabaseQueue.tipindexQ.setFrame(baseIndexObj);
		                    
		                } else if (TIPDataObjects.isBondEOD) {

		                    TIP_BondIndexVO baseBondIndexObj = new TIP_BondIndexVO();
		                    baseBondIndexObj.isEndOfBatch = true;
		                    baseBondIndexObj.isEOD = true;			 
			            	
			            	TIP_BondVO baseBondObj = new TIP_BondVO();
			            	baseBondObj.isEndOfBatch = true;
			            	baseBondObj.isEOD = true;
			            	
		                    logger.info("Bond Market EOD received");
		                    baseObj.isEOD = true;
		                    TIPDataObjects. isBondEOD = false;
		                    
		                    MessageDatabaseQueue.tipbondQ.setFrame(baseBondObj);
		                    MessageDatabaseQueue.tipBondindexQ.setFrame(baseBondIndexObj);
		                    
		                    //Added for PTTP Project
		                    MessageDatabaseQueue.tipOTCbondQ.setFrame(baseBondObj);
		                   
		                   
		                }else if (TIPDataObjects.isSMEShareEOD) {
		                	
		                	  TIP_SMEIndexVO baseIndexObj = new TIP_SMEIndexVO();
				            	baseIndexObj.isEndOfBatch = true;
				            	baseIndexObj.isEOD = true;
				            	
				            	TIP_SMEShareVO baseSMEShareObj = new TIP_SMEShareVO();
				            	baseSMEShareObj.isEndOfBatch = true;
				            	baseSMEShareObj.isEOD = true;
				            	
		                    logger.info("SME Share Market EOD received");
		                    baseObj.isEOD = true;
		                    TIPDataObjects.isSMEShareEOD = false;
		                    MessageDatabaseQueue.tipSMEshareQ.setFrame(baseSMEShareObj);
		                    MessageDatabaseQueue.tipSMEindexQ.setFrame(baseIndexObj);
		                    
		                }
		                baseObj = null;
		            }
		           		          
	/*	            logger.info("DBQueue Updater Statistics :  currentMarketUpdated " + tmpTotMarketUpdated + " currentIndexUpdated " + tmpTotIndexUpdated
		                    + " currentShareUpdated " + tmpTotShareUpdated + " currentBondUpdated " + tmpTotBondUpdated +" currentBondIndexUpdated" +tmpTotBondIndexUpdated
		                    + " currentSMEIndexUpdate "+tmpTotSMEIndexUpdated+ " currentSMEShareUpdates "+tmpTotSMEShareUpdated );
		  */          
		           //Print only for once in ten
		            if(printIntervalCount  == 120)
		            {
		            	printIntervalCount=0;
		            logger.info("DBQueue Updater Statistics  totalMarketUpdated " + totalMarketUpdated + " totalIndexUpdated " + totalIndexUpdated
		                    + " totalShareUpdated " + totalShareUpdated + "  totalBondUpdated " + totalBondUpdated +" totalBondIndexUpdated" +totalBondIndexUpdated
		                    + " totalSMEIndexUpdated "+totalSMEIndexUpdated+ " totalSMEShareUpdated "+totalSMEShareUpdated );
		            
		            logger.info("Queue Size : Analyzer queue :{} tipMarketQ :{} tipindexQ : {}  tipSMEIndexQueue : {}  tipBondindexQ : {}", MessageQueue.g_oBuffer.size(), MessageDatabaseQueue.tipmarketQ.size(), MessageDatabaseQueue.tipindexQ.size(), MessageDatabaseQueue.tipSMEindexQ.size(), MessageDatabaseQueue.tipBondindexQ.size());
		           logger.info("Queue Size : tipshareQ : {}   tipOTCShareIntraQ : {}  tipSMEshareQ : {}  tipbondQ : {}", MessageDatabaseQueue.tipshareQ.size(), MessageDatabaseQueue.tipOTCShareIntraQ.size(), MessageDatabaseQueue.tipSMEshareQ.size(), MessageDatabaseQueue.tipbondQ.size());
		           logger.info("Queue Size : tipIndexIntraQ :{}  tipSMEIndexIntraQ : {}   tipBondindexIntraQ : {}", MessageDatabaseQueue.tipIndexIntraQ.size(), MessageDatabaseQueue.tipSMEIndexIntraQ.size(), MessageDatabaseQueue.tipBondIndexIntraQ.size());
		           logger.info("Queue Size : tipShareIntraQ : {} tipSMEShareIntraQ : {} tipBondIntraQ : {}", MessageDatabaseQueue.tipShareIntraQ.size(), MessageDatabaseQueue.tipSMEShareIntraQ.size(), MessageDatabaseQueue.tipBondIntraQ.size());
		           logger.info("Queue Size : tipShareSpecialIntraQ : {} tipSMEShareSpecialIntraQ : {} tipBondSpecialIntraQ : {}", MessageDatabaseQueue.tipShareSpecialIntraQ.size(), MessageDatabaseQueue.tipSMEShareSpecialIntraQ.size(), MessageDatabaseQueue.tipBondSpecialIntraQ.size());
		           logger.info("Queue Size :  tipOTCbondQ : {}  tipOTCBondIntraQ : {}", MessageDatabaseQueue.tipOTCbondQ.size(), MessageDatabaseQueue.tipOTCBondIntraQ.size());
			          
		            }
	        
		        }						
				Utility.sleepAnalyser(TipConstants.DATA_PARSER_SLEEP_INTERVAL_DBQUEUE_UPDATER);
				}//Outer while
		}//Function run End
		
		
	
	
	private void startDataBaseUpdatedThreads()
    {
    	// Create pool of threads   
		
	//	ExecutorService executor = Executors.newFixedThreadPool(17);
		 //Added for PTTP Project CRQ22398
		// Each worker now runs on its own named platform thread (was: an unnamed, never-shut-down
		// fixed pool of 19, so logs and thread dumps showed "pool-N-thread-M").
		
    	try
    	{
   
    	   Runnable dbUpdateMarket = new DBUpdateMarket("DBUpdaterMarket");
           TipThreads.startWorker("DBUpdaterMarket", dbUpdateMarket);
           
          Runnable dbUpdateIndex = new DBUpdateIndex("DBUpdateIndex");
           TipThreads.startWorker("DBUpdateIndex", dbUpdateIndex);
           
           Runnable dbUpdateSMEIndex = new DBUpdateSMEIndex("DBUpdateSMEIndex");
           TipThreads.startWorker("DBUpdateSMEIndex", dbUpdateSMEIndex);
          
           Runnable dbUpdateBondIndex = new DBUpdateBondIndex("DBUpdateBondIndex");
           TipThreads.startWorker("DBUpdateBondIndex", dbUpdateBondIndex);
           
           Runnable dbUpdateShare = new DBUpdateShare("DBUpdateShare");
           TipThreads.startWorker("DBUpdateShare", dbUpdateShare);
           
           Runnable dbUpdateSMEShare = new DBUpdateSMEShare("DBUpdateSMEShare");
           TipThreads.startWorker("DBUpdateSMEShare", dbUpdateSMEShare);
           
           Runnable dbUpdateBond = new DBUpdateBond("DBUpdateBond");
           TipThreads.startWorker("DBUpdateBond", dbUpdateBond);
           
           Runnable dbWriterBondIndexIntra = new DBWriterBondIndexIntra("DBWriterBondIndexIntra");
           TipThreads.startWorker("DBWriterBondIndexIntra", dbWriterBondIndexIntra);
           
           Runnable dbWriterIndexIntra = new DBWriterIndexIntra("DBWriterIndexIntra");
           TipThreads.startWorker("DBWriterIndexIntra", dbWriterIndexIntra);
           
          Runnable dbWriterSMEIndexIntra = new DBWriterSMEIndexIntra("DBWriterSMEIndexIntra");
           TipThreads.startWorker("DBWriterSMEIndexIntra", dbWriterSMEIndexIntra);
           
           Runnable dbWriterShareIntra = new DBWriterShareIntra("DBWriterShareIntra");
           TipThreads.startWorker("DBWriterShareIntra", dbWriterShareIntra);
           
           Runnable dbWriterShareSpecialIntra = new DBWriterShareSpecialIntra("DBWriterShareSpecialIntra");
           TipThreads.startWorker("DBWriterShareSpecialIntra", dbWriterShareSpecialIntra);
           
           Runnable dbWriterSMEShareIntra = new DBWriterSMEShareIntra("DBWriterSMEShareIntra");
           TipThreads.startWorker("DBWriterSMEShareIntra", dbWriterSMEShareIntra);
           
           Runnable dbWriterSMEShareSpecialIntra = new DBWriterSMEShareSpecialIntra("DBWriterSMEShareSpecialIntra");
           TipThreads.startWorker("DBWriterSMEShareSpecialIntra", dbWriterSMEShareSpecialIntra);
           
           Runnable dbWriterBondIntra = new DBWriterBondIntra("DBWriterBondIntra");
           TipThreads.startWorker("DBWriterBondIntra", dbWriterBondIntra);
           
		   Runnable dbWriterBondSpecialIntra = new DBWriterBondSpecialIntra("DBWriterBondSpecialIntra");
           TipThreads.startWorker("DBWriterBondSpecialIntra", dbWriterBondSpecialIntra);
		   
           //Added for PTTP Project CRQ22398
           Runnable dbWriterOTCShareIntra = new DBWriterOTCShareIntra("dbWriterOTCShareIntra");
           TipThreads.startWorker("dbWriterOTCShareIntra", dbWriterOTCShareIntra);
           
           //Added for PTTP Project CRQ22398
           Runnable dbUpdateOTCBond = new DBUpdateOTCBond("DBUpdateOTCBond");
           TipThreads.startWorker("DBUpdateOTCBond", dbUpdateOTCBond);
           
           //Added for PTTP Project CRQ22398
           Runnable dbWriterOTCBondIntra = new DBWriterOTCBondIntra("DBWriterOTCBondIntra");
           TipThreads.startWorker("DBWriterOTCBondIntra", dbWriterOTCBondIntra);
		   
           
    	}catch(Exception e)
    	{
    		logger.error("Error in startDataBaseUpdatedThreads :"+e);
    	}          
    
    }
	
	
	public static boolean isDBQueueAllEmpty()
	{
		boolean isEmpty = true;
		
		if( MessageDatabaseQueue.tipmarketQ.size() >0 || MessageDatabaseQueue.tipindexQ.size() >0 || MessageDatabaseQueue.tipSMEindexQ.size() > 0 || MessageDatabaseQueue.tipBondindexQ.size() > 0
		//	 || messagedatabaseQueue.tipBondIndexIntraQ.size() > 0 || messagedatabaseQueue.tipIndexIntraQ.size() >0 || messagedatabaseQueue.tipSMEIndexIntraQ.size() > 0  
			|| MessageDatabaseQueue.tipshareQ.size() > 0 || MessageDatabaseQueue.tipSMEshareQ.size() > 0  ||  MessageDatabaseQueue.tipOTCShareIntraQ.size() > 0)
			//|| messagedatabaseQueue.tipbondQ.size() > 0 ||	messagedatabaseQueue.tipshareQ.size() > 0 
			{
				isEmpty = false;
			}
		
			return isEmpty;
	}

	/**
	 * Queues a copy of every VO in {@code source[0..count)} whose {@code isUpdated} flag is set and
	 * clears the flag. Replaces seven copy-pasted index loops.
	 */
	private static <V extends TIP_BaseVO> void flushUpdated(int totalUpdated, V[] source, int count,
			UnaryOperator<V> copy, Buffer target) {
		if (totalUpdated <= 0) {
			return;
		}
		Arrays.stream(source, 0, Math.min(count, source.length))
			.filter(vo -> vo.isUpdated)
			.forEach(vo -> {
				target.setFrame(copy.apply(vo));
				vo.isUpdated = false;
			});
	}
}
