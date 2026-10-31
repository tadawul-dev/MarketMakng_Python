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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import sa.com.tadawul.website.tip.messages.TadawulObjectCategory;
import sa.com.tadawul.website.tip.messages.TurnoverListUpdates;
import sa.com.tadawul.website.tip.queue.MessageQueue;
import sa.com.tadawul.website.tip.shared.LoadInitialData;
import sa.com.tadawul.website.tip.shared.TIPDataObjects;
import sa.com.tadawul.website.tip.shared.TipConstants;
import sa.com.tadawul.website.tip.shared.Utility;

public class DataParserTurnOverListUpdates  implements Runnable{
	
	private static final Logger logger = LogManager.getLogger(DataParserTurnOverListUpdates.class);
	
	TadawulObjectCategory OBJ_TYPE=null;
	
	private String message = "";
	String messageValue[];
	private String value [] = new String[2];
	private String messageColumnType = "";
	
	int totalTurnOverMessages;
	boolean finished = false;
	long sequence_no=0;
	int tmpid=0;	
	String tmpSymbolId="";		
	int tmpIterId,tmpIterIdShared=0;
	
	String tmpTx=null,tmpANt=null,tmpTq=null,tmpAt=null,tmpTOt=null,tmpPm=null,tmpPu=null,tmpPp=null ;
	 int tmpIndexUpdFlag,tmpSMEIndexUpdFlag,tmpBndIndexUpdFlag= 0;
	 double tipIndex_turnover;
	 boolean isSharedList=false;
	 String tmpSymbolId_shared="";	
	public DataParserTurnOverListUpdates(){
	
	}

	@Override
	public void run() {
		
		logger.info("DataParserTurnOverListUpdates Thread started. " );
		
		while(true)
				{
					
					while(true)
					{						
						while (MessageQueue.turnoverListUpdateQueue != null && MessageQueue.turnoverListUpdateQueue.size() > 0) {
							//logger.info("DataParserTurnOverListUpdates Queue not null. Size  :{}", turnoverListUpdateQueue.size());
							finished = false;
							totalTurnOverMessages++;
							
							TIPDataObjects.totalMessagesParsed.incrementAndGet();
							  try {
								  	message =MessageQueue.turnoverListUpdateQueue.pop();		
								  	
								  	tmpTx=null;tmpANt=null;tmpTq=null;tmpAt=null;tmpTOt=null;tmpPm=null;tmpPu=null;tmpPp=null ;
								  	tmpIterId=0;tmpIterIdShared=0;
								  	sequence_no =0;
								  	isSharedList=false;
									tmpSymbolId_shared="";
								  	if(message != null && !message.equals("")) {
								  		messageValue = message.split(TipConstants.SPLITMESSAGE);										  
										for (int i=0;i< messageValue.length && !finished; i++) {
											value = Utility.splitTagValue(messageValue[i]);
											if (value != null && value.length > 1) {
												if (value[0] != null && value[1] != null) {
													messageColumnType = value[0] + messageValue[i].charAt(value[0].length());
												}
												else {
													break;
												}
													
															
												
															switch(TurnoverListUpdates.getMessageValue(messageColumnType)){				
																case 1 : // IDCODE (1,"i")
																	 tmpid=Integer.parseInt(value[1]);
																	break;
																
																case 2 : // SOURCEID (2,"Si")
																	  tmpSymbolId=LoadInitialData.hmListToIndexMap.get(value[1]);																	  
																	 break;
																
																case 3 :  //  TIMEEXEC (3, "Tx"),																	
																	tmpTx =value[1];
																	break;
																
																case 4 ://  NUMBEROFTRADES (4, "ANt"),\logger.info(" TLU Message IDcode :{}", tmpid);
																	tmpANt=value[1];
																	break;
																	
																case 5 ://  TRADEDQUANTITY (5, "Tq"),																
																	tmpTq=value[1];		
																	break;
																	
																case 6 ://  ACCUMULATEDTURNOVER (6, "At"),																	
																	tmpAt=value[1];		
																	break;
																	
																case 7 ://   TOTALTRADED (7, "TOt"),
																	tmpTOt=value[1];		
																	break;
																	
																case 8 ://  MINUSPAID (8, "Pm"),
																	tmpPm=value[1];		
																	break;
																	
																case 9 ://   UNCHANGEDPAID (9, "Pu"),
																	tmpPu=value[1];	
																	break;
																	
																case 10 ://     PLUSPAID (10, "Pp") 
																	tmpPp=value[1];	
																	break;
																	
																case 11 ://      SEQUENCENO(11,"n");
																	sequence_no=Long.parseLong(value[1]);	
																	TIPDataObjects.last_Sequencer_Analyzer=sequence_no;
																	break;
																
																default:
																	break;
															}	//End of Switch
											}
										}//End of For loop													
										
										  OBJ_TYPE = TadawulObjectCategory.getIndexMessageCategoty(tmpSymbolId);							 
										  tmpIndexUpdFlag = 0;tmpSMEIndexUpdFlag=0;tmpBndIndexUpdFlag=0;
										  tipIndex_turnover=0D;
										  
										  if(null != TIPDataObjects.SharedList_Mapping_TLU && TIPDataObjects.SharedList_Mapping_TLU.containsKey(tmpSymbolId))
										  {
											  isSharedList = true;
											  tmpSymbolId_shared=TIPDataObjects.SharedList_Mapping_TLU.get(tmpSymbolId);
										  }
										  
										 if(null !=OBJ_TYPE)
										 switch(OBJ_TYPE)
											{
												case SHAREINDEX:
													//Find Index
													 if(TIPDataObjects.tipIndex_Symbol_ArrayIndexMap.get(tmpSymbolId) !=null)
									            	 {
									            		 tmpIterId = (int)TIPDataObjects.tipIndex_Symbol_ArrayIndexMap.get(tmpSymbolId);
									            		 if(isSharedList)
														 	{
									            			 if(TIPDataObjects.tipIndex_Symbol_ArrayIndexMap.get(tmpSymbolId_shared) !=null)
									            			 	{	
									            				 	tmpIterIdShared = (int)TIPDataObjects.tipIndex_Symbol_ArrayIndexMap.get(tmpSymbolId_shared);
									            			 	}
									            			 else
									            			 {
									            				 	logger.info("TLu received for unkown symbol as IndexNot found, inside shared list:{}", tmpSymbolId);
									            			 }
										            	
														 	}
									            	 } else
									            	 {
									            		 logger.info("TLu received for unkown symbol as index Not found :{}", tmpSymbolId);
									            		 break;
									            	 }
													 try
													 {												
													 
														 if(null!=tmpTx)
													 	{
														 	TIPDataObjects.tipIndex[tmpIterId].trans_time = Timestamp.valueOf(Utility.appendTxWithToday(tmpTx));
														 	if(isSharedList)
														 	{
														 		TIPDataObjects.tipIndex[tmpIterIdShared].trans_time = Timestamp.valueOf(Utility.appendTxWithToday(tmpTx));
														 	}
													 	}
													 }catch(Exception e)
													 {
														  logger.error("Error Occured in TLU Processing in transtime ", e);
													 }
													
													 if(null!=tmpANt && TIPDataObjects.tipIndex[tmpIterId].no_of_trades !=Integer.parseInt(tmpANt))
													 {
														 TIPDataObjects.tipIndex[tmpIterId].no_of_trades = Integer.parseInt(tmpANt);
														
														 if(isSharedList)
														 	{
														 		TIPDataObjects.tipIndex[tmpIterIdShared].no_of_trades = Integer.parseInt(tmpANt);
														 	}
														 
														 tmpIndexUpdFlag =1;
													 }
													
													 if(null!=tmpTq && TIPDataObjects.tipIndex[tmpIterId].volume_trade != Long.parseLong(tmpTq)) 
													 {
														 TIPDataObjects.tipIndex[tmpIterId].volume_trade = Long.parseLong(tmpTq);
														 if(isSharedList)
														 	{
														 		TIPDataObjects.tipIndex[tmpIterIdShared].volume_trade =  Long.parseLong(tmpTq);
														 	}
														 tmpIndexUpdFlag =1;
														 
													 }
													
													 if(null!=tmpAt)  
													 {
														 tipIndex_turnover=Math.round(Double.parseDouble(tmpAt) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
														 
														 if(TIPDataObjects.tipIndex[tmpIterId].turnover != tipIndex_turnover)
														 {
															 TIPDataObjects.tipIndex[tmpIterId].turnover = tipIndex_turnover;
															 
															 if(isSharedList)
															 	{
															 		TIPDataObjects.tipIndex[tmpIterIdShared].turnover =  tipIndex_turnover;
															 	}
															 
															 tmpIndexUpdFlag =1;
														 }
														
													 }
													 
													 if(null!=tmpTOt) TIPDataObjects.tipIndex[tmpIterId].no_of_symbols_traded = Integer.parseInt(tmpTOt);
														
													 
													 if(tmpIndexUpdFlag ==1 )
													 {
														 TIPDataObjects.tipIndex[tmpIterId].isUpdated = true;
													 	 TIPDataObjects.totIndexUpdated.incrementAndGet();
													 	 if(isSharedList)
														 	{
													 		TIPDataObjects.tipIndex[tmpIterIdShared].isUpdated = true;
														 	 TIPDataObjects.totIndexUpdated.incrementAndGet();
														 	}
													 }
													 
													 TIPDataObjects.tipIndex[tmpIterId].sequence_no = sequence_no;
													// logger.info("TLU Main Index TIPDataObjects.tipIndex[tipIndexIter] : Index {} Symbol: {} ID: {} No Of Trades :{} VOT:{} Turnover :{}", tmpIterId, TIPDataObjects.tipIndex[tmpIterId].Fk_te_symbol, TIPDataObjects.tipIndex[tmpIterId].ListId, TIPDataObjects.tipIndex[tmpIterId].no_of_trades, TIPDataObjects.tipIndex[tmpIterId].volume_trade, TIPDataObjects.tipIndex[tmpIterId].turnover);
													// logger.info("TLU Main Index TIPDataObjects.tipIndex[tipIndexIter] : Index {} Symbol: {} ID: {} No Of Trades :{} VOT:{} Turnover :{}", tmpIterId, TIPDataObjects.tipIndex[tmpIterId].Fk_te_symbol, TIPDataObjects.tipIndex[tmpIterId].ListId, TIPDataObjects.tipIndex[tmpIterId].no_of_trades, TIPDataObjects.tipIndex[tmpIterId].volume_trade, TIPDataObjects.tipIndex[tmpIterId].turnover);
													break;
												
												case SMESHAEINDEX:
													//Find Index
													 if(TIPDataObjects.tipSMEIndex_Symbol_ArrayIndexMap.get(tmpSymbolId) !=null)
									            	 {
									            		 tmpIterId = (int)TIPDataObjects.tipSMEIndex_Symbol_ArrayIndexMap.get(tmpSymbolId);
									            		
									            		 if(isSharedList)
														 	{
									            			 if(TIPDataObjects.tipSMEIndex_Symbol_ArrayIndexMap.get(tmpSymbolId_shared) !=null)
									            			 	{	
									            				 	tmpIterIdShared = (int)TIPDataObjects.tipSMEIndex_Symbol_ArrayIndexMap.get(tmpSymbolId_shared);
									            			 	}
									            			 else
									            			 {
									            				 	logger.info("TLu received for unkown symbol :{}", tmpSymbolId);
									            			 }
										            	
														 	}									            		 
									            		 
									            	 } else
									            	 {
									            		 logger.info("TLu received for unkown symbol :{}", tmpSymbolId);
									            		 break;
									            	 }
													 
																										 
													 try
													 {
													
														 if(null!=tmpTx) TIPDataObjects.tipSMEIndex[tmpIterId].trans_time = Timestamp.valueOf(Utility.appendTxWithToday(tmpTx));
															 if(isSharedList)
															 	{
															 		TIPDataObjects.tipSMEIndex[tmpIterIdShared].trans_time = Timestamp.valueOf(Utility.appendTxWithToday(tmpTx));
															 	}
													 
													 }catch(Exception e)
													 {
														 // logger.error("Error Occured in TLU Processing", e);
													 }
													 
													 if(null!=tmpANt && TIPDataObjects.tipSMEIndex[tmpIterId].no_of_trades != Integer.parseInt(tmpANt))
													 {
														 	TIPDataObjects.tipSMEIndex[tmpIterId].no_of_trades = Integer.parseInt(tmpANt);
														 	 if(isSharedList)
															 	{
															 		TIPDataObjects.tipSMEIndex[tmpIterIdShared].no_of_trades = Integer.parseInt(tmpANt);
															 	}
														 	tmpSMEIndexUpdFlag = 1;
													 }
													 if(null!=tmpTq && TIPDataObjects.tipSMEIndex[tmpIterId].volume_trade != Long.parseLong(tmpTq)) 
														 {
														 	TIPDataObjects.tipSMEIndex[tmpIterId].volume_trade = Long.parseLong(tmpTq);
														 	 if(isSharedList)
															 	{
															 		TIPDataObjects.tipSMEIndex[tmpIterIdShared].volume_trade = Long.parseLong(tmpTq);
															 	}
														 	tmpSMEIndexUpdFlag = 1;
														 }
													 if(null!=tmpAt) 
														 {
														    tipIndex_turnover = Math.round(Double.parseDouble(tmpAt) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
														    if(TIPDataObjects.tipSMEIndex[tmpIterId].turnover != tipIndex_turnover)
														    {
														 	TIPDataObjects.tipSMEIndex[tmpIterId].turnover = tipIndex_turnover;
														 	if(isSharedList)
														 	{
														 		TIPDataObjects.tipSMEIndex[tmpIterIdShared].turnover =  tipIndex_turnover;
														 	}
														 	tmpSMEIndexUpdFlag = 1;
														    }
														 }
													 
													 if(null!=tmpTOt) TIPDataObjects.tipSMEIndex[tmpIterId].no_of_symbols_traded = Integer.parseInt(tmpTOt);
														
													 
													 if(tmpSMEIndexUpdFlag ==1 )
													 {
													 TIPDataObjects.tipSMEIndex[tmpIterId].isUpdated = true;
													 TIPDataObjects.totSMEIndexUpdated.incrementAndGet();
														 if(isSharedList)
														 	{
													 		TIPDataObjects.tipSMEIndex[tmpIterIdShared].isUpdated = true;
														 	 TIPDataObjects.totSMEIndexUpdated.incrementAndGet();
														 	}
													 }
													
													 TIPDataObjects.tipSMEIndex[tmpIterId].sequence_no = sequence_no;
													 //logger.info("TLU SME Index TIPDataObjects.tipSMEIndex[tipIndexIter] : Index {} Symbol: {} ID: {} No Of Trades :{} VOT:{} Turnover :{}", tmpIterId, TIPDataObjects.tipSMEIndex[tmpIterId].Fk_te_symbol, TIPDataObjects.tipSMEIndex[tmpIterId].ListId, TIPDataObjects.tipSMEIndex[tmpIterId].no_of_trades, TIPDataObjects.tipSMEIndex[tmpIterId].volume_trade, TIPDataObjects.tipSMEIndex[tmpIterId].turnover);
													break;
												
												case BONDINDEX:												
													
													//Find Index
													 if(TIPDataObjects.tipBondIndex_Symbol_ArrayIndexMap.get(tmpSymbolId) !=null)
									            	 {
									            		 tmpIterId = (int)TIPDataObjects.tipBondIndex_Symbol_ArrayIndexMap.get(tmpSymbolId);
									            	 } else
									            	 {
									            		 logger.info("TLu received for unkown symbol :{}", tmpSymbolId);
									            		 break;
									            	 }
													
													
													 try
													 {
													 if(null!=tmpTx) TIPDataObjects.tipBondIndex[tmpIterId].trans_time = Timestamp.valueOf(Utility.appendTxWithToday(tmpTx));
													 }catch(Exception e)
													 {
														//  logger.error("Error Occured in TLU Processing", e);
													 }
													 
													 if(null!=tmpANt && TIPDataObjects.tipBondIndex[tmpIterId].no_of_trades != Integer.parseInt(tmpANt)) 
														 {
														 	TIPDataObjects.tipBondIndex[tmpIterId].no_of_trades = Integer.parseInt(tmpANt);
														 	tmpBndIndexUpdFlag=1;
														 }
													 if(null!=tmpTq && TIPDataObjects.tipBondIndex[tmpIterId].volume_trade != Long.parseLong(tmpTq)) 
														 {
														 	TIPDataObjects.tipBondIndex[tmpIterId].volume_trade = Long.parseLong(tmpTq);
														 	tmpBndIndexUpdFlag=1;
														 }
													 if(null!=tmpAt)  
														 {
														    tipIndex_turnover = Math.round(Double.parseDouble(tmpAt) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
														    if(TIPDataObjects.tipBondIndex[tmpIterId].turnover != tipIndex_turnover)
														    {
														    TIPDataObjects.tipBondIndex[tmpIterId].turnover = Math.round(Double.parseDouble(tmpAt) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
														 	tmpBndIndexUpdFlag = 1;
														    }
														 }
													 if(tmpBndIndexUpdFlag ==1 )
													 {
													 TIPDataObjects.tipBondIndex[tmpIterId].isUpdated = true;
													 TIPDataObjects.totBondIndexUpdated.incrementAndGet();
													 }
													 TIPDataObjects.tipBondIndex[tmpIterId].sequence_no = sequence_no;
													// logger.info("TLU Bond Index TIPDataObjects.tipBondIndex[tipIndexIter] : Index {} Symbol: {} ID: {} No Of Trades :{} VOT:{} Turnover :{}", tmpIterId, TIPDataObjects.tipBondIndex[tmpIterId].Fk_te_symbol, TIPDataObjects.tipBondIndex[tmpIterId].ListId, TIPDataObjects.tipBondIndex[tmpIterId].no_of_trades, TIPDataObjects.tipBondIndex[tmpIterId].volume_trade, TIPDataObjects.tipBondIndex[tmpIterId].turnover);
												default:
													break;

											}//End of Switch
										 
										// logger.info("IDCode:{}", tmpid);
										 OBJ_TYPE = TadawulObjectCategory.getShareMessageCategotyByIDCode(tmpid);	
										// logger.info("SHARE OBJ_TYPE:{}", OBJ_TYPE);
										 if(null !=OBJ_TYPE)
										 switch(OBJ_TYPE)
										 {
										 		case SHARE:
										 			
										 			 if(null!=tmpTx)  
										 				 {
										 				 try
														 {
										 				  TIPDataObjects.tipMarket[0].lasttradeddate = Timestamp.valueOf(Utility.appendTxWithToday(tmpTx));
														 }catch(Exception e)
										 				 {
															 // logger.error("Error Occured in TLU Processing", e);
										 				 }
										 				 
										 				 }
													 if(null!=tmpANt) TIPDataObjects.tipMarket[0].no_of_trades = Integer.parseInt(tmpANt);
													 if(null!=tmpTq)  TIPDataObjects.tipMarket[0].volume_traded = Long.parseLong(tmpTq);
													 if(null!=tmpAt)  TIPDataObjects.tipMarket[0].turnover = Math.round(Double.parseDouble(tmpAt) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
													 if(null!=tmpTOt) TIPDataObjects.tipMarket[0].no_of_symbols_traded = Integer.parseInt(tmpTOt);
													 if(null!=tmpPm) TIPDataObjects.tipMarket[0].no_of_downs = Integer.parseInt(tmpPm);
													 if(null!=tmpPu) TIPDataObjects.tipMarket[0].no_of_no_changes = Integer.parseInt(tmpPu);
													 if(null!=tmpPp) TIPDataObjects.tipMarket[0].no_of_ups = Integer.parseInt(tmpPp);	
													
													 //This is comments to update forcefulley
												//	 if(! TIPDataObjects.tipMarket[0].isUpdated)
													 {
														 TIPDataObjects.tipMarket[0].isUpdated = true;
														 TIPDataObjects.totMarketUpdated.incrementAndGet();
														// logger.debug("totMarketUpdated in TLu :{}", TIPDataObjects.totMarketUpdated.get());
														// logger.info("DataParserTurnOverListUpdates totMarketUpdated  After:{}", TIPDataObjects.totMarketUpdated.get());
													 }
													 TIPDataObjects.tipMarket[0].sequence_no = sequence_no;
													if (logger.isDebugEnabled()) logger.debug("TLU Main Share  no_of_trades: {} volume_traded: {} turnover :{} no_of_symbols_traded:{} no_of_ups :{}", TIPDataObjects.tipMarket[0].no_of_trades, TIPDataObjects.tipMarket[0].volume_traded, TIPDataObjects.tipMarket[0].turnover, TIPDataObjects.tipMarket[0].no_of_symbols_traded, TIPDataObjects.tipMarket[0].no_of_ups);
										 			break;
										 			
										 		case SMESHARE:
										 			 if(null!=tmpTx)  
										 			 {
										 				 try
										 				 {
										 				 TIPDataObjects.tipMarket[2].lasttradeddate = Timestamp.valueOf(Utility.appendTxWithToday(tmpTx));
												 			 }catch(Exception e)
											 				 {
																//  logger.error("Error Occured in TLU Processing", e);
											 				 }
										 			 }
													 if(null!=tmpANt) TIPDataObjects.tipMarket[2].no_of_trades = Integer.parseInt(tmpANt);
													 if(null!=tmpTq)  TIPDataObjects.tipMarket[2].volume_traded = Long.parseLong(tmpTq);
													 if(null!=tmpAt)  TIPDataObjects.tipMarket[2].turnover = Math.round(Double.parseDouble(tmpAt) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
													 if(null!=tmpTOt) TIPDataObjects.tipMarket[2].no_of_symbols_traded = Integer.parseInt(tmpTOt);
													 if(null!=tmpPm) TIPDataObjects.tipMarket[2].no_of_downs = Integer.parseInt(tmpPm);
													 if(null!=tmpPu) TIPDataObjects.tipMarket[2].no_of_no_changes = Integer.parseInt(tmpPu);
													 if(null!=tmpPp) TIPDataObjects.tipMarket[2].no_of_ups = Integer.parseInt(tmpPp);		
													
													//This is comments to update forcefulley
													// if(! TIPDataObjects.tipMarket[2].isUpdated)
													 {
														 TIPDataObjects.tipMarket[2].isUpdated = true;
														 TIPDataObjects.totMarketUpdated.incrementAndGet();
													 }
													 TIPDataObjects.tipMarket[2].sequence_no = sequence_no;
													 if (logger.isDebugEnabled()) logger.debug("TLU SME Share  no_of_trades: {} volume_traded: {} turnover :{} no_of_symbols_traded:{} no_of_ups :{}", TIPDataObjects.tipMarket[2].no_of_trades, TIPDataObjects.tipMarket[2].volume_traded, TIPDataObjects.tipMarket[2].turnover, TIPDataObjects.tipMarket[2].no_of_symbols_traded, TIPDataObjects.tipMarket[2].no_of_ups); // hot path: was INFO per message
										 			break;
										 			
										 		case BOND :
										 			 
										 			if(null!=tmpTx)
										 				{
										 				    try
										 				    {
										 				    	TIPDataObjects.tipMarket[1].lasttradeddate = Timestamp.valueOf(Utility.appendTxWithToday(tmpTx));
										 				    }catch(Exception e)
											 				 {
																//  logger.error("Error Occured in TLU Processing", e);
											 				 }
										 				}
										 			 
													 if(null!=tmpANt) TIPDataObjects.tipMarket[1].no_of_trades = Integer.parseInt(tmpANt);
													 if(null!=tmpTq)  TIPDataObjects.tipMarket[1].volume_traded = Long.parseLong(tmpTq);
													 if(null!=tmpAt)  TIPDataObjects.tipMarket[1].turnover = Math.round(Double.parseDouble(tmpAt) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
													 if(null!=tmpTOt) TIPDataObjects.tipMarket[1].no_of_symbols_traded = Integer.parseInt(tmpTOt);
													 if(null!=tmpPm) TIPDataObjects.tipMarket[1].no_of_downs = Integer.parseInt(tmpPm);
													 if(null!=tmpPu) TIPDataObjects.tipMarket[1].no_of_no_changes = Integer.parseInt(tmpPu);
													 if(null!=tmpPp) TIPDataObjects.tipMarket[1].no_of_ups = Integer.parseInt(tmpPp);
													//This is comments to update forcefulley
													// if(! TIPDataObjects.tipMarket[1].isUpdated)
													 {
														 TIPDataObjects.tipMarket[1].isUpdated = true;
														 TIPDataObjects.totMarketUpdated.incrementAndGet();
													 }
													 TIPDataObjects.tipMarket[1].sequence_no = sequence_no;
													// logger.info("TLU Bond  no_of_trades: {} volume_traded: {} turnover :{} no_of_symbols_traded:{} no_of_ups :{}", TIPDataObjects.tipMarket[1].no_of_trades, TIPDataObjects.tipMarket[1].volume_traded, TIPDataObjects.tipMarket[1].turnover, TIPDataObjects.tipMarket[1].no_of_symbols_traded, TIPDataObjects.tipMarket[1].no_of_ups);
										 		  break;
										
										 		default:
										 			break;
										 }
								  	
								  	}//End of If
							  }//End of Try
							  catch(Exception e)
							  {
								  logger.error("Error Occured in TLU Processing", e);
							  }
							if (logger.isDebugEnabled()) logger.debug("DataParserTurnOverListUpdates  Statistics : Total  No of TLu Messages Parsed : {}", totalTurnOverMessages); // hot path: was INFO per message
														
						}//End of Inner While
						
						MessageQueue.turnoverListUpdateQueue.awaitData(TipConstants.PARSER_QUEUE_IDLE_WAIT_MS); // was: sleep(DATA_ANALYZER_SLEEP_INTERVAL_TLU) polling
					}////End of InnerWhile
					
				}//Outer while
		}//Function run End
		
		
	
	
	
}
