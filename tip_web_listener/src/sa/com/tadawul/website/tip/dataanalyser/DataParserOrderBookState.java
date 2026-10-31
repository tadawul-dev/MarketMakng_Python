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

package sa.com.tadawul.website.tip.dataanalyser;

import java.sql.Timestamp;

import sa.com.tadawul.website.tip.log.Log;
import sa.com.tadawul.website.tip.log.LogFactory;
import sa.com.tadawul.website.tip.messages.OrderBookState;
import sa.com.tadawul.website.tip.messages.TadawulObjectCategory;
import sa.com.tadawul.website.tip.queue.MessageQueue;
import sa.com.tadawul.website.tip.shared.TIPCommonFunctions;
import sa.com.tadawul.website.tip.shared.TIPDataObjects;
import sa.com.tadawul.website.tip.shared.TipConstants;
import sa.com.tadawul.website.tip.shared.Utility;

public class DataParserOrderBookState  implements Runnable{
	
	private final static Log logger = LogFactory.getLog(DataParserOrderBookState.class);
	
	private String message = "";
	String messageValue[];
	private String value [] = new String[2];
	private String messageColumnType = "";
	
	int totalOrderBookStateMessages;
	long sequence_no=0;
	
	int tmpid=0;	
	String tmpSymbolId="";		
	int tmpIterId=0;
	String tmpTx=null,tmpOSt=null ;
		
	public DataParserOrderBookState(){
	}

	@Override
	public void run() {
		
		logger.info("DataParserOrderBookState Thread started. " );
					
		while(true)
		{	
			while (MessageQueue.orderBookStateQueue != null && MessageQueue.orderBookStateQueue.size() > 0) {
				//logger.info("DataParserOrderBook Queue not null. Size  :"+ orderBookStateQueue.size() );				
				if(totalOrderBookStateMessages==0)
				{
					//First message processing should be delayed until other Low volume thread are completed BDIn, BDLi
					Utility.sleepAnalyser(TipConstants.SLEEPINTERVALFOROBK_OKS_INOFORFIRSTMESSAGE);
				}
				totalOrderBookStateMessages++;
				TIPDataObjects.totalMessagesParsed++;
				  try {
					  	message =(String) MessageQueue.orderBookStateQueue.pop();		
					  	tmpTx=null;tmpOSt=null ;
					  	sequence_no=0;
					  	// logger.info("DataParserOrderBookState message  :"+ message);	
					  	if(message != null && !message.equals("")) {
					  		messageValue = message.split(TipConstants.SPLITMESSAGE);										  
							for (int i=0;i< messageValue.length ; i++) {
								value = messageValue[i].split(TipConstants.SPLITVALUE, 2);
								if (value != null && value.length > 1) {
									if (value[0] != null && value[1] != null) {
										messageColumnType = value[0] + messageValue[i].charAt(value[0].length());
									}
									else {
										break;
									}
										
												switch(OrderBookState.getMessageValue(messageColumnType)){				
													case 1 : // IDCODE (1,"i")
														 tmpid=Integer.parseInt(value[1]);
														break;
													
													case 2 : // SOURCEID (2,"Si")
														  tmpSymbolId=value[1];
														 break;
													
													case 3 :  //  TIMEEXEC (3, "Tx"),
														tmpTx =value[1];
														break;
													
													case 4 ://  ORDERBOOKSTATECODE (4, "OSt");
														tmpOSt=value[1];
														break;
													
													case 5 ://  	    SEQUENCENO(5,"n");
														sequence_no= Long.parseLong(value[1]);
														TIPDataObjects.last_Sequencer_Analyzer=sequence_no;
														break;
													default:
														break;
												}	//End of Switch
								}
							}//End of For loop													
							
							 TadawulObjectCategory OBJ_TYPE = TadawulObjectCategory.getShareMessageCategoty(tmpSymbolId);		
	/*						 logger.info("OKs symbold:"+tmpSymbolId);
							 logger.info("OKs OBJ_TYPE:"+OBJ_TYPE.name());
	*/						 
							 switch(OBJ_TYPE)
								{
									case SHARE:
										//Find Index
										 if(TIPDataObjects.tipShare_Symbol_ArrayIndexMap.get(tmpSymbolId) !=null)
						            	 {
						            		 tmpIterId = (int)TIPDataObjects.tipShare_Symbol_ArrayIndexMap.get(tmpSymbolId);
						            	 }else
						            	 {
						            		 logger.info("OKS received for unkown symbol :"+tmpSymbolId);
						            		 break;
						            	 }
										 
										 if (null!=tmpOSt) { // OSt
											 TIPDataObjects.tipShare[tmpIterId].current_state = TIPCommonFunctions.setState(TIPDataObjects.tipShare[tmpIterId].market_status, Integer.parseInt(tmpOSt));
				                                if (TIPDataObjects.tipShare[tmpIterId].market_status != TipConstants.TIP_DB_STATE_SUSPENDED)
				                                	TIPDataObjects.tipShare[tmpIterId].market_status = TIPDataObjects.tipShare[tmpIterId].current_state;
				                                
				                                if (! TIPDataObjects.tipShare[tmpIterId].isUpdated) {
				                                	TIPDataObjects.tipShare[tmpIterId].isUpdated = true;
				                                	TIPDataObjects.totShareUpdated++;
				                                }
				                                
										 }
										 try
										 {
										 if(null!=tmpTx)	 TIPDataObjects.tipShare[tmpIterId].last_update_time = Timestamp.valueOf(Utility.appendTxWithToday(tmpTx));
										 }catch(Exception e)
										 {
											// logger.info("Errors occured in OKS Processing :"+e);
										 }
										 
										 //The below are commented to take from STa messages
										 /*if (TIPDataObjects.tipShare[tmpIterId].fk_te_symbol.equals(TipConstants.SHARE_MARKET_SYMBOL_REFERENCE)) {
					                            if (TIPDataObjects.tipMarket[0].current_state != TIPDataObjects.tipShare[tmpIterId].current_state) {
					                                logger.debug("Share Market State Change Received, Current State: "+ TIPDataObjects.tipMarket[0].current_state
					                                        + " Current Market Status: " + TIPDataObjects.tipMarket[0].market_status
					                                        + " New State: " + TIPDataObjects.tipShare[tmpIterId].current_state);
					                                TIPDataObjects.tipMarket[0].current_state = TIPDataObjects.tipShare[tmpIterId].current_state;
					                                if (TIPDataObjects.tipMarket[0].market_status != TipConstants.TIP_DB_STATE_SUSPENDED)
					                                	TIPDataObjects.tipMarket[0].market_status = TIPDataObjects.tipMarket[0].current_state;
					                                TIPDataObjects.tipMarket[0].lasttradeddate = TIPDataObjects.tipShare[tmpIterId].last_update_time;
					                              
					                                // BOD logic for Share
					                                if (TIPDataObjects.tipMarket[0].market_status == TipConstants.TIP_DB_STATE_PREOPENED) {
					                                	TIPDataObjects.isShareBOD = true;
					                                }
					                                // EOD logic for Share
					                                if (TIPDataObjects.tipMarket[0].market_status == TipConstants.TIP_DB_STATE_PRECLOSED
					                                        || TIPDataObjects.tipMarket[0].market_status == TipConstants.TIP_DB_STATE_CLOSED) {
					                                	TIPDataObjects.isShareEOD = true;
					                                }
					                               
					                                if (! TIPDataObjects.tipMarket[0].isUpdated) {
					                                	TIPDataObjects.tipMarket[0].isUpdated = true;					                                	
					                                	TIPDataObjects.totMarketUpdated++;
					                                	logger.debug("totMarketUpdated in OBK :"+TIPDataObjects.totMarketUpdated);
					                                }
					                            }
					                            TIPDataObjects.tipMarket[0].sequence_no = sequence_no;
					                        }*/
										 TIPDataObjects.tipShare[tmpIterId].sequence_no = sequence_no ;
										// logger.info("OKs Main  TIPDataObjects.tipShare[tmpIterId] : Shares "+tmpIterId +" Symbol: "+TIPDataObjects.tipShare[tmpIterId].fk_te_symbol +" TIPDataObjects.tipShare[tmpIterId].market_status : "+TIPDataObjects.tipShare[tmpIterId].market_status+" TIPDataObjects.tipMarket[0].market_status  :"+TIPDataObjects.tipMarket[0].market_status+" TIPDataObjects.totMarketUpdated :"+TIPDataObjects.totMarketUpdated  );
										break;
									
									case SMESHARE:
										//Find Index
										 if(TIPDataObjects.tipSMEShare_Symbol_ArrayIndexMap.get(tmpSymbolId) !=null)
						            	 {
						            		 tmpIterId = (int)TIPDataObjects.tipSMEShare_Symbol_ArrayIndexMap.get(tmpSymbolId);
						            	 }else
						            	 {
						            		 logger.info("OKS received for unkown symbol :"+tmpSymbolId);
						            		 break;
						            	 }
										
										 if (null!=tmpOSt) { // OSt
											 TIPDataObjects.tipSMEShare[tmpIterId].current_state = TIPCommonFunctions.setState(TIPDataObjects.tipSMEShare[tmpIterId].market_status, Integer.parseInt(tmpOSt));
				                                if (TIPDataObjects.tipSMEShare[tmpIterId].market_status != TipConstants.TIP_DB_STATE_SUSPENDED)
				                                	TIPDataObjects.tipSMEShare[tmpIterId].market_status = TIPDataObjects.tipSMEShare[tmpIterId].current_state;
				                                
				                                if (! TIPDataObjects.tipSMEShare[tmpIterId].isUpdated) {
				                                	TIPDataObjects.tipSMEShare[tmpIterId].isUpdated = true;
				                                	TIPDataObjects.totSMEShareUpdated++;
				                                }
										 }
										 try
										 {
										 if(null!=tmpTx)	 TIPDataObjects.tipSMEShare[tmpIterId].last_update_time = Timestamp.valueOf(Utility.appendTxWithToday(tmpTx));
											}catch(Exception e)
											 {
												// logger.info("Errors occured in OKS Processing :"+e);
											 }
										 //The below are commented to take from STa messages
										/* if (TIPDataObjects.tipSMEShare[tmpIterId].fk_te_symbol.equals(TipConstants.SME_MARKET_SYMBOL_REFERENCE)) {
											        if (TIPDataObjects.tipMarket[2].current_state != TIPDataObjects.tipSMEShare[tmpIterId].current_state) {
					                                logger.debug("SME Share Market State Change Received, Current State: "+ TIPDataObjects.tipMarket[2].current_state
					                                        + " Current Market Status: " + TIPDataObjects.tipMarket[2].market_status
					                                        + " New State: " + TIPDataObjects.tipSMEShare[tmpIterId].current_state);
					                                TIPDataObjects.tipMarket[2].current_state = TIPDataObjects.tipSMEShare[tmpIterId].current_state;
					                                if (TIPDataObjects.tipMarket[2].market_status != TipConstants.TIP_DB_STATE_SUSPENDED)
					                                	TIPDataObjects.tipMarket[2].market_status = TIPDataObjects.tipMarket[2].current_state;
					                                TIPDataObjects.tipMarket[2].lasttradeddate = TIPDataObjects.tipSMEShare[tmpIterId].last_update_time;
					                              
					                                // BOD logic for Share
					                                if (TIPDataObjects.tipMarket[2].market_status == TipConstants.TIP_DB_STATE_PREOPENED) {
					                                	//logger.info("SME Share Market State Change Received :isSMEShareBOD TRUE");
					                                	TIPDataObjects.isSMEShareBOD = true;
					                                }
					                                // EOD logic for Share
					                                if (TIPDataObjects.tipMarket[2].market_status == TipConstants.TIP_DB_STATE_PRECLOSED
					                                        || TIPDataObjects.tipMarket[2].market_status == TipConstants.TIP_DB_STATE_CLOSED) {
					                                	TIPDataObjects.isSMEShareEOD = true;
					                                }
					                                
					                                if (! TIPDataObjects.tipMarket[2].isUpdated) {
					                                	TIPDataObjects.tipMarket[2].isUpdated = true;
					                                	TIPDataObjects.totMarketUpdated++;
					                                	logger.debug("totMarketUpdated in OBK :"+TIPDataObjects.totMarketUpdated);
					                                }
					                               
					                            }
					                            TIPDataObjects.tipMarket[2].sequence_no = sequence_no;
					                        }*/
										 
										 TIPDataObjects.tipSMEShare[tmpIterId].sequence_no = sequence_no ;
										// logger.info("OKs SME  TIPDataObjects.tipSMEShare[tmpIterId] : tipSMEShare "+tmpIterId +" Symbol: "+TIPDataObjects.tipSMEShare[tmpIterId].fk_te_symbol +" TIPDataObjects.tipSMEShare[tmpIterId].market_status : "+TIPDataObjects.tipSMEShare[tmpIterId].market_status+" TIPDataObjects.tipMarket[2].market_status  :"+TIPDataObjects.tipMarket[2].market_status+" TIPDataObjects.totMarketUpdated :"+TIPDataObjects.totMarketUpdated  );
										break;
									
									case BOND:
										//Find Index
										 if(TIPDataObjects.tipBond_Symbol_ArrayIndexMap.get(tmpSymbolId) !=null)
						            	 {
						            		 tmpIterId = (int)TIPDataObjects.tipBond_Symbol_ArrayIndexMap.get(tmpSymbolId);
						            	 }else
						            	 {
						            		 logger.info("OKS received for unkown symbol :"+tmpSymbolId);
						            		 break;
						            	 }
										
										 if (null!=tmpOSt) { // OSt
											 TIPDataObjects.tipBond[tmpIterId].current_state = TIPCommonFunctions.setState(TIPDataObjects.tipBond[tmpIterId].market_status, Integer.parseInt(tmpOSt));
				                                if (TIPDataObjects.tipBond[tmpIterId].market_status != TipConstants.TIP_DB_STATE_SUSPENDED)
				                                	TIPDataObjects.tipBond[tmpIterId].market_status = TIPDataObjects.tipBond[tmpIterId].current_state;
				                                
				                                if (! TIPDataObjects.tipBond[tmpIterId].isUpdated) {
				                                	TIPDataObjects.tipBond[tmpIterId].isUpdated = true;
				                                	TIPDataObjects.totBondUpdated++;
				                                }
										 }
										 try
										 {
										 if(null!=tmpTx)	 TIPDataObjects.tipBond[tmpIterId].last_update_time = Timestamp.valueOf(Utility.appendTxWithToday(tmpTx));
											}catch(Exception e)
											 {
												// logger.info("Errors occured in OKS Processing :"+e);
											 }
		/*								 logger.info("TIPDataObjects.tipBond[tmpIterId].fk_te_symbo :"+TIPDataObjects.tipBond[tmpIterId].fk_fi_symbol);
										 logger.info("TipConstants.BOND_MARKET_SYMBOL_REFERENCE:"+TipConstants.BOND_MARKET_SYMBOL_REFERENCE);
										 logger.info("TIPDataObjects.tipMarket[1].current_state:"+TIPDataObjects.tipMarket[1].current_state);
										 logger.info("TIPDataObjects.tipBond[tmpIterId].current_state:"+TIPDataObjects.tipBond[tmpIterId].current_state);
		*/		              
										 //The below are commented to take from STa messages
										/* if (TIPDataObjects.tipBond[tmpIterId].fk_fi_symbol.equals(TipConstants.BOND_MARKET_SYMBOL_REFERENCE)) {
					                            if (TIPDataObjects.tipMarket[1].current_state != TIPDataObjects.tipBond[tmpIterId].current_state) {
					                                logger.debug("Bond Market State Change Received, Current State: "+ TIPDataObjects.tipMarket[2].current_state
					                                        + " Current Market Status: " + TIPDataObjects.tipMarket[1].market_status
					                                        + " New State: " + TIPDataObjects.tipBond[tmpIterId].current_state);
					                                TIPDataObjects.tipMarket[1].current_state = TIPDataObjects.tipBond[tmpIterId].current_state;
					                                if (TIPDataObjects.tipMarket[1].market_status != TipConstants.TIP_DB_STATE_SUSPENDED)
					                                	TIPDataObjects.tipMarket[1].market_status = TIPDataObjects.tipMarket[1].current_state;
					                                TIPDataObjects.tipMarket[1].lasttradeddate = TIPDataObjects.tipBond[tmpIterId].last_update_time;
					                              
					                                // BOD logic for Share
					                                if (TIPDataObjects.tipMarket[1].market_status == TipConstants.TIP_DB_STATE_PREOPENED) {
					                                	TIPDataObjects.isBondBOD = true;
					                                }
					                                // EOD logic for Share
					                                if (TIPDataObjects.tipMarket[1].market_status == TipConstants.TIP_DB_STATE_PRECLOSED
					                                        || TIPDataObjects.tipMarket[1].market_status == TipConstants.TIP_DB_STATE_CLOSED) {
					                                	TIPDataObjects.isBondEOD = true;
					                                }
					                               
					                                if (! TIPDataObjects.tipMarket[1].isUpdated) {
					                                	TIPDataObjects.tipMarket[1].isUpdated = true;
					                                	TIPDataObjects.totMarketUpdated++;
					                                }
					                                
					                            }
					                            TIPDataObjects.tipMarket[1].sequence_no = sequence_no;
					                        }*/
										 TIPDataObjects.tipBond[tmpIterId].sequence_no = sequence_no;
										// logger.info("OKs tipBond  TIPDataObjects.tipBond[tmpIterId] : tipBond "+tmpIterId +" Symbol: "+TIPDataObjects.tipBond[tmpIterId].fk_fi_symbol +" TIPDataObjects.tipBond[tmpIterId].market_status : "+TIPDataObjects.tipBond[tmpIterId].market_status+" TIPDataObjects.tipMarket[1].market_status  :"+TIPDataObjects.tipMarket[1].market_status+" TIPDataObjects.totMarketUpdated :"+TIPDataObjects.totMarketUpdated  );
										 	break;
										 
										 	
									case OTCBOND:
										//Find Index
										 if(TIPDataObjects.tipOTCBond_Symbol_ArrayIndexMap.get(tmpSymbolId) !=null)
						            	 {
						            		 tmpIterId = (int)TIPDataObjects.tipOTCBond_Symbol_ArrayIndexMap.get(tmpSymbolId);
						            	 }else
						            	 {
						            		 logger.info("OKS received for unkown symbol :"+tmpSymbolId);
						            		 break;
						            	 }
										
										 if (null!=tmpOSt) { // OSt
											 TIPDataObjects.tipOTCBond[tmpIterId].current_state = TIPCommonFunctions.setState(TIPDataObjects.tipOTCBond[tmpIterId].market_status, Integer.parseInt(tmpOSt));
				                                if (TIPDataObjects.tipOTCBond[tmpIterId].market_status != TipConstants.TIP_DB_STATE_SUSPENDED)
				                                	TIPDataObjects.tipOTCBond[tmpIterId].market_status = TIPDataObjects.tipOTCBond[tmpIterId].current_state;
				                                
				                                if (! TIPDataObjects.tipOTCBond[tmpIterId].isUpdated) {
				                                	TIPDataObjects.tipOTCBond[tmpIterId].isUpdated = true;
				                                	TIPDataObjects.totOTCBondUpdated++;
				                                }
										 }
										 try
										 {
										 if(null!=tmpTx)	 TIPDataObjects.tipOTCBond[tmpIterId].last_update_time = Timestamp.valueOf(Utility.appendTxWithToday(tmpTx));
											}catch(Exception e)
											 {
												// logger.info("Errors occured in OKS Processing :"+e);
											 }
		
										 TIPDataObjects.tipOTCBond[tmpIterId].sequence_no = sequence_no;
										// logger.info("OKs tipBond  TIPDataObjects.tipBond[tmpIterId] : tipBond "+tmpIterId +" Symbol: "+TIPDataObjects.tipBond[tmpIterId].fk_fi_symbol +" TIPDataObjects.tipBond[tmpIterId].market_status : "+TIPDataObjects.tipBond[tmpIterId].market_status+" TIPDataObjects.tipMarket[1].market_status  :"+TIPDataObjects.tipMarket[1].market_status+" TIPDataObjects.totMarketUpdated :"+TIPDataObjects.totMarketUpdated  );
										 	break;
					
										 	
										default:
											break;
								}//End of Switch
					  	
					  	}//End of If
				  }//End of Try
				  catch(Exception e)
				  {
					  logger.error("Error Occured in OKs Processing", e);
				  }
				
				//logger.info("DataParserOrderBookState  Statistics : Total  No of Order Book State Messages Parsed : "+totalOrderBookStateMessages);
				  
				
			}//End of Inner While
			Utility.sleepAnalyser(TipConstants.DATA_ANALYZER_SLEEP_INTERVAL_OKS);
		}////End of OutterWhile
		}//Function run End
		
		
	
	
	
}
