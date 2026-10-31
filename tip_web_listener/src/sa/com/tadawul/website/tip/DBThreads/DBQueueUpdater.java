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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import sa.com.tadawul.website.tip.log.Log;
import sa.com.tadawul.website.tip.log.LogFactory;
import sa.com.tadawul.website.tip.queue.MessageDatabaseQueue;
import sa.com.tadawul.website.tip.queue.MessageQueue;
import sa.com.tadawul.website.tip.shared.LoadInitialData;
import sa.com.tadawul.website.tip.shared.TIPDataObjects;
import sa.com.tadawul.website.tip.shared.TipConstants;
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
	
	private final static Log logger = LogFactory.getLog(DBQueueUpdater.class);
	
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
			
			//logger.info("isShareBOD :"+TIPDataObjects.isShareBOD +" isSMEShareBOD  :"+TIPDataObjects.isSMEShareBOD +" ((System.currentTimeMillis() - quantaStart) > TipConstants.TIP_MAX_DELAY_TIME :"+((System.currentTimeMillis() - quantaStart) > TipConstants.TIP_MAX_DELAY_TIME));
			
				 if (  TIPDataObjects.isShareBODPreOpen ||  TIPDataObjects.isShareBODOpen || TIPDataObjects.isBondBODPreOpen || TIPDataObjects.isBondBODOpen || TIPDataObjects.isShareEOD || TIPDataObjects.isBondEOD || TIPDataObjects.isSMEShareEOD || TIPDataObjects.isSMEShareBODPreOpen || TIPDataObjects.isSMEShareBODOpen ||
			                ((System.currentTimeMillis() - quantaStart) > TipConstants.TIP_MAX_DELAY_TIME  )) {			 
	
					 tmpTotMarketUpdated=TIPDataObjects.totMarketUpdated;
					 TIPDataObjects.totMarketUpdated=0;
					 totalMarketUpdated +=tmpTotMarketUpdated;
					 
		            for (ctr = 0, tmpIter1 = 0; (tmpTotMarketUpdated > 0 && tmpIter1 < TipConstants.TIP_MAX_NO_MARKET) ; tmpIter1++) {
		                if (TIPDataObjects.tipMarket[tmpIter1].isUpdated) {
		                    ctr++;
		                    TIP_MarketVO MarketToQ = new TIP_MarketVO(TIPDataObjects.tipMarket[tmpIter1]);
		                    logger.debug("MarketToQ . Sequence no :"+MarketToQ.sequence_no);
		                    if (ctr == tmpTotMarketUpdated)
		                        MarketToQ.isEndOfBatch = true;
		                   
		                    //Normal TIP Updates
		                	if(!TipConstants.IS_NEGDEALS_EARLY_START)
							{
		                		MessageDatabaseQueue.tipmarketQ.setFrame(MarketToQ);
		                    	TIPDataObjects.tipMarket[tmpIter1].isUpdated = false;
		                    	MarketToQ = null;
							}
		                    
		                	//Incase if it is Early start for NegDeals changes
		                    if(	TipConstants.IS_NEGDEALS_EARLY_START && TIPDataObjects.isAfterPreOpen && Utility.shouldUpdatedShares()  )
		                    {
		                    	logger.debug("Market messages received after Market Pre-Open. Hence this will be updated :"+TIPDataObjects.tipMarket[tmpIter1].Pk_exchange_code);
		                    	MessageDatabaseQueue.tipmarketQ.setFrame(MarketToQ);
		                    	TIPDataObjects.tipMarket[tmpIter1].isUpdated = false;
		                    	MarketToQ = null;
		                    }else
		                    {
		                    	logger.info("Market messages received before Market Pre-Open. Hence this will not be updated :"+TIPDataObjects.tipMarket[tmpIter1].Pk_exchange_code);
		                    }
		                  
		                }
		            }
		            
		            tmpTotIndexUpdated=TIPDataObjects.totIndexUpdated ;
		            TIPDataObjects.totIndexUpdated=0;
		            totalIndexUpdated +=tmpTotIndexUpdated;
		            for ( tmpIter1 = 0; (tmpTotIndexUpdated > 0 && tmpIter1 <TIPDataObjects. tipIndexIter) ; tmpIter1++) {
		                if (TIPDataObjects.tipIndex[tmpIter1].isUpdated) {		                   
		                    TIP_IndexVO IndexToQ = new TIP_IndexVO(TIPDataObjects.tipIndex[tmpIter1]);
		                   /* if (ctr == TIPDataObjects.totIndexUpdated)
		                        IndexToQ.isEndOfBatch = true;*/
		                    logger.debug("TIPDataObjects.isAfterPreOpen :"+TIPDataObjects.isAfterPreOpen);
		                    //Normal TIP Updates
		                	if(!TipConstants.IS_NEGDEALS_EARLY_START)
							{
		                		MessageDatabaseQueue.tipindexQ.setFrame(IndexToQ);		                    
		                    	TIPDataObjects.tipIndex[tmpIter1].isUpdated = false;
							}
		                  //Incase if it is Early start for NegDeals changes
		                    if(	TipConstants.IS_NEGDEALS_EARLY_START && TIPDataObjects.isAfterPreOpen && Utility.shouldUpdatedShares())
		                    {
		                    	logger.debug("Index messages received after Market Pre-Open. Hence this will be updated :"+TIPDataObjects.tipIndex[tmpIter1].Fk_te_symbol);
		                    	MessageDatabaseQueue.tipindexQ.setFrame(IndexToQ);		                    
		                    	TIPDataObjects.tipIndex[tmpIter1].isUpdated = false;
		                    
		                   // logger.info("MessageDatabaseQueue.tipindexQ.size "+MessageDatabaseQueue.tipindexQ.size());
		                    IndexToQ = null;
		                    }else
		                    {
		                    	logger.debug("Index messages received before Market Pre-Open. Hence this will not be updated :"+TIPDataObjects.tipIndex[tmpIter1].Fk_te_symbol);
		                    }
		                }
		            }	  		            
		            
		            
		            tmpTotBondIndexUpdated= TIPDataObjects.totBondIndexUpdated ;
		            TIPDataObjects.totBondIndexUpdated =0; 
		            totalBondIndexUpdated +=tmpTotBondIndexUpdated;
		            for ( tmpIter1 = 0; (tmpTotBondIndexUpdated> 0 && tmpIter1 <TIPDataObjects.tipBondIndexIter) ; tmpIter1++) {
		                if (TIPDataObjects.tipBondIndex[tmpIter1].isUpdated) {		                   
		                    TIP_BondIndexVO IndexToQ = new TIP_BondIndexVO(TIPDataObjects.tipBondIndex[tmpIter1]);
		                   /* if (ctr == TIPDataObjects.totBondIndexUpdated)
		                        IndexToQ.isEndOfBatch = true;*/
		                    MessageDatabaseQueue.tipBondindexQ.setFrame(IndexToQ);
		                    TIPDataObjects.tipBondIndex[tmpIter1].isUpdated = false;
		                    IndexToQ = null;
		                }
		            }
		            
		            
		            tmpTotSMEIndexUpdated=TIPDataObjects.totSMEIndexUpdated ;
		            TIPDataObjects.totSMEIndexUpdated =0;
		            totalSMEIndexUpdated +=tmpTotSMEIndexUpdated;		           
		            for ( tmpIter1 = 0; (tmpTotSMEIndexUpdated > 0 && tmpIter1 < TIPDataObjects.tipSMEIndexIter) ; tmpIter1++) {
		                if (TIPDataObjects.tipSMEIndex[tmpIter1].isUpdated) {		                		                   
		                    TIP_SMEIndexVO IndexToQ = new TIP_SMEIndexVO(TIPDataObjects.tipSMEIndex[tmpIter1]);
		                    /*if (ctr == TIPDataObjects.totSMEIndexUpdated)
		                        IndexToQ.isEndOfBatch = true;		  */              
		                    MessageDatabaseQueue.tipSMEindexQ.setFrame(IndexToQ);
		                    TIPDataObjects.tipSMEIndex[tmpIter1].isUpdated = false;		                   
		                    IndexToQ = null;
		                }
		            }
		            
		           
		            
		           tmpTotShareUpdated =TIPDataObjects.totShareUpdated ;
		           TIPDataObjects.totShareUpdated =0;
		           totalShareUpdated +=tmpTotShareUpdated;
		            for (tmpIter1 = 0; (tmpTotShareUpdated > 0 && tmpIter1 < TIPDataObjects.tipShareIter) ; tmpIter1++) {
		                if (TIPDataObjects.tipShare[tmpIter1].isUpdated) {		                   
		                    TIP_ShareVO ShareToQ = new TIP_ShareVO(TIPDataObjects.tipShare[tmpIter1]);
		                   /* if (ctr == TIPDataObjects.totShareUpdated)
		                        ShareToQ.isEndOfBatch = true;*/
		                    MessageDatabaseQueue.tipshareQ.setFrame(ShareToQ);
		                    TIPDataObjects.tipShare[tmpIter1].isUpdated = false;
		                    ShareToQ = null;
		                }
		            }		            
		            
		            
		            tmpTotSMEShareUpdated = TIPDataObjects.totSMEShareUpdated ;
		            TIPDataObjects.totSMEShareUpdated =0;
		            totalSMEShareUpdated +=tmpTotSMEShareUpdated;
		            for (tmpIter1 = 0; (tmpTotSMEShareUpdated > 0 && tmpIter1 < TIPDataObjects.tipShareSMEIter) ; tmpIter1++) {
		                if (TIPDataObjects.tipSMEShare[tmpIter1].isUpdated) {		                   
		                    TIP_SMEShareVO ShareToQ = new TIP_SMEShareVO(TIPDataObjects.tipSMEShare[tmpIter1]);
		                   /* if (ctr == TIPDataObjects.totSMEShareUpdated)
		                        ShareToQ.isEndOfBatch = true;*/
		                    MessageDatabaseQueue.tipSMEshareQ.setFrame(ShareToQ);
		                    TIPDataObjects.tipSMEShare[tmpIter1].isUpdated = false;
		                    ShareToQ = null;
		                }
		            }
		            
		            
		            tmpTotBondUpdated = TIPDataObjects.totBondUpdated ;
		            TIPDataObjects.totBondUpdated =0;
		            totalBondUpdated +=tmpTotBondUpdated;
		            for (tmpIter1 = 0; (tmpTotBondUpdated> 0 && tmpIter1 < TIPDataObjects.tipBondIter) ; tmpIter1++) {
		                if (TIPDataObjects.tipBond[tmpIter1].isUpdated) {		                   
		                    TIP_BondVO BondToQ = new TIP_BondVO(TIPDataObjects.tipBond[tmpIter1]);
		                    /*if (ctr == TIPDataObjects.totBondUpdated)
		                        BondToQ.isEndOfBatch = true;*/
		                    MessageDatabaseQueue.tipbondQ.setFrame(BondToQ);
		                    TIPDataObjects.tipBond[tmpIter1].isUpdated = false;
		                    BondToQ = null;
		                }
		            }
		            
		            //Added for PTTP Project CRQ22398
		            tmpTotOTCShareUpdated =TIPDataObjects.totOTCShareUpdated ;
			           TIPDataObjects.totOTCShareUpdated =0;
			           totalOTCShareUpdated +=tmpTotOTCShareUpdated;
			            for (tmpIter1 = 0; (tmpTotOTCShareUpdated > 0 && tmpIter1 < TIPDataObjects.tipOTCShareIter) ; tmpIter1++) {
			                if (TIPDataObjects.tipOTCShare[tmpIter1].isUpdated) {		                   
			                    TIP_ShareVO ShareToQ = new TIP_ShareVO(TIPDataObjects.tipOTCShare[tmpIter1]);
			                   /* if (ctr == TIPDataObjects.totShareUpdated)
			                        ShareToQ.isEndOfBatch = true;*/
			                    MessageDatabaseQueue.tipOTCShareIntraQ.setFrame(ShareToQ);
			                    TIPDataObjects.tipOTCShare[tmpIter1].isUpdated = false;
			                    ShareToQ = null;
			                }
			            }
		            
			          //Added for PTTP Project CRQ22398
			            tmpTotOTCBondUpdated = TIPDataObjects.totOTCBondUpdated ;
			            TIPDataObjects.totOTCBondUpdated =0;
			            totalOTCBondUpdated +=tmpTotOTCBondUpdated;
			            for (tmpIter1 = 0; (tmpTotOTCBondUpdated> 0 && tmpIter1 < TIPDataObjects.tipOTCBonditer) ; tmpIter1++) {
			                if (TIPDataObjects.tipOTCBond[tmpIter1].isUpdated) {		                   
			                    TIP_BondVO BondToQ = new TIP_BondVO(TIPDataObjects.tipOTCBond[tmpIter1]);
			                    /*if (ctr == TIPDataObjects.totBondUpdated)
			                        BondToQ.isEndOfBatch = true;*/
			                    MessageDatabaseQueue.tipOTCbondQ.setFrame(BondToQ);
			                    TIPDataObjects.tipOTCBond[tmpIter1].isUpdated = false;
			                    BondToQ = null;
			                }
			            }
			            
			            
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
		            
		            logger.info("Queue Size : Analyzer queue :"+MessageQueue.g_oBuffer.size()+" tipMarketQ :"+MessageDatabaseQueue.tipmarketQ.size()+" tipindexQ : "+MessageDatabaseQueue.tipindexQ.size()+"  tipSMEIndexQueue : "+MessageDatabaseQueue.tipSMEindexQ.size()+"  tipBondindexQ : "+MessageDatabaseQueue.tipBondindexQ.size());
		           logger.info("Queue Size : tipshareQ : "+MessageDatabaseQueue.tipshareQ.size()+"   tipOTCShareIntraQ : "+MessageDatabaseQueue.tipOTCShareIntraQ.size()+"  tipSMEshareQ : "+MessageDatabaseQueue.tipSMEshareQ.size()+"  tipbondQ : "+MessageDatabaseQueue.tipbondQ.size());
		           logger.info("Queue Size : tipIndexIntraQ :"+MessageDatabaseQueue.tipIndexIntraQ.size()+"  tipSMEIndexIntraQ : "+MessageDatabaseQueue.tipSMEIndexIntraQ.size()+"   tipBondindexIntraQ : "+MessageDatabaseQueue.tipBondIndexIntraQ.size());		         
		           logger.info("Queue Size : tipShareIntraQ : "+MessageDatabaseQueue.tipShareIntraQ.size()+" tipSMEShareIntraQ : "+MessageDatabaseQueue.tipSMEShareIntraQ.size()+" tipBondIntraQ : "+MessageDatabaseQueue.tipBondIntraQ.size());
		           logger.info("Queue Size : tipShareSpecialIntraQ : "+MessageDatabaseQueue.tipShareSpecialIntraQ.size()+" tipSMEShareSpecialIntraQ : "+MessageDatabaseQueue.tipSMEShareSpecialIntraQ.size()+" tipBondSpecialIntraQ : "+MessageDatabaseQueue.tipBondSpecialIntraQ.size());
		           logger.info("Queue Size :  tipOTCbondQ : "+MessageDatabaseQueue.tipOTCbondQ.size()+"  tipOTCBondIntraQ : "+MessageDatabaseQueue.tipOTCBondIntraQ.size());
			          
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
		ExecutorService executor = Executors.newFixedThreadPool(19);
		
    	try
    	{
   
    	   Runnable dbUpdateMarket = new DBUpdateMarket("DBUpdaterMarket");
           executor.execute(dbUpdateMarket);
           
          Runnable dbUpdateIndex = new DBUpdateIndex("DBUpdateIndex");
           executor.execute(dbUpdateIndex);
           
           Runnable dbUpdateSMEIndex = new DBUpdateSMEIndex("DBUpdateSMEIndex");
           executor.execute(dbUpdateSMEIndex);
          
           Runnable dbUpdateBondIndex = new DBUpdateBondIndex("DBUpdateBondIndex");
           executor.execute(dbUpdateBondIndex);
           
           Runnable dbUpdateShare = new DBUpdateShare("DBUpdateShare");
           executor.execute(dbUpdateShare);
           
           Runnable dbUpdateSMEShare = new DBUpdateSMEShare("DBUpdateSMEShare");
           executor.execute(dbUpdateSMEShare);
           
           Runnable dbUpdateBond = new DBUpdateBond("DBUpdateBond");
           executor.execute(dbUpdateBond);
           
           Runnable dbWriterBondIndexIntra = new DBWriterBondIndexIntra("DBWriterBondIndexIntra");
           executor.execute(dbWriterBondIndexIntra);
           
           Runnable dbWriterIndexIntra = new DBWriterIndexIntra("DBWriterIndexIntra");
           executor.execute(dbWriterIndexIntra);
           
          Runnable dbWriterSMEIndexIntra = new DBWriterSMEIndexIntra("DBWriterSMEIndexIntra");
           executor.execute(dbWriterSMEIndexIntra);
           
           Runnable dbWriterShareIntra = new DBWriterShareIntra("DBWriterShareIntra");
           executor.execute(dbWriterShareIntra);
           
           Runnable dbWriterShareSpecialIntra = new DBWriterShareSpecialIntra("DBWriterShareSpecialIntra");
           executor.execute(dbWriterShareSpecialIntra);
           
           Runnable dbWriterSMEShareIntra = new DBWriterSMEShareIntra("DBWriterSMEShareIntra");
           executor.execute(dbWriterSMEShareIntra);
           
           Runnable dbWriterSMEShareSpecialIntra = new DBWriterSMEShareSpecialIntra("DBWriterSMEShareSpecialIntra");
           executor.execute(dbWriterSMEShareSpecialIntra);
           
           Runnable dbWriterBondIntra = new DBWriterBondIntra("DBWriterBondIntra");
           executor.execute(dbWriterBondIntra);
           
		   Runnable dbWriterBondSpecialIntra = new DBWriterBondSpecialIntra("DBWriterBondSpecialIntra");
           executor.execute(dbWriterBondSpecialIntra);
		   
           //Added for PTTP Project CRQ22398
           Runnable dbWriterOTCShareIntra = new DBWriterOTCShareIntra("dbWriterOTCShareIntra");
           executor.execute(dbWriterOTCShareIntra);
           
           //Added for PTTP Project CRQ22398
           Runnable dbUpdateOTCBond = new DBUpdateOTCBond("DBUpdateOTCBond");
           executor.execute(dbUpdateOTCBond);
           
           //Added for PTTP Project CRQ22398
           Runnable dbWriterOTCBondIntra = new DBWriterOTCBondIntra("DBWriterOTCBondIntra");
           executor.execute(dbWriterOTCBondIntra);
		   
           
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
	
}
