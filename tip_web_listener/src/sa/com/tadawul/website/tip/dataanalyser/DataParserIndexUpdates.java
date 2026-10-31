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
import sa.com.tadawul.website.tip.messages.IndexUpdates;
import sa.com.tadawul.website.tip.messages.TadawulObjectCategory;
import sa.com.tadawul.website.tip.queue.MessageDatabaseQueue;
import sa.com.tadawul.website.tip.queue.MessageQueue;
import sa.com.tadawul.website.tip.shared.TIPDataObjects;
import sa.com.tadawul.website.tip.shared.TipConstants;
import sa.com.tadawul.website.tip.shared.Utility;
import sa.com.tadawul.website.tip.vo.TIP_BondIndexVO;
import sa.com.tadawul.website.tip.vo.TIP_IndexVO;
import sa.com.tadawul.website.tip.vo.TIP_SMEIndexVO;

public class DataParserIndexUpdates  implements Runnable{
	
	private final static Log logger = LogFactory.getLog(DataParserIndexUpdates.class);
	
	private String message = "";
	String messageValue[];
	private String value [] = new String[2];
	private String messageColumnType = "";
	
	int totalINdMessages;
	boolean finished = false;
	
	int tmpid=0;	
	String tmpTime=null,tmpVc=null,tmpVh=null,tmpVl=null,tmpDd=null,tmpDDn=null;
	String tmpSymbolId="";		
	int tmpIndexIter=0;
	double tipIndex_index_price;
	 int tmpIndexUpdFlag,tmpSMEIndexUpdFlag,tmpBndIndexUpdFlag= 0;
	TadawulObjectCategory OBJ_TYPE=null;
	long sequence_no=0;
	
	public DataParserIndexUpdates(){	
	}

	@Override
	public void run() {
		
		logger.info("DataParserIndexUpdates Thread started. " );
		
		while(true)
				{
					
					//logger.info("DataParserIndexUpdates indexUpdatesQueue Befor Null. " + (indexUpdatesQueue != null) +" Size " + indexUpdatesQueue.size() );
							while (MessageQueue.indexUpdateQueue != null && MessageQueue.indexUpdateQueue .size() > 0) {
									//logger.info("DataParserIndexUpdates Queue not null. " );
									finished = false;
									 tmpIndexUpdFlag =0;
									totalINdMessages++;
									TIPDataObjects.totalMessagesParsed++;
									  try {
										  	message =(String) MessageQueue.indexUpdateQueue.pop();
										  	 tmpTime=null;tmpVc=null;tmpVh=null;tmpVl=null;tmpDd=null;tmpDDn=null;
										  	sequence_no = 0;										
										  	if(message != null && !message.equals("")) {
										  		messageValue = message.split(TipConstants.SPLITMESSAGE);
										 
												for (int i=0;i< messageValue.length && !finished; i++) {
													value = messageValue[i].split(TipConstants.SPLITVALUE, 2);
													if (value != null && value.length > 1) {
														if (value[0] != null && value[1] != null) {
															messageColumnType = value[0] + messageValue[i].charAt(value[0].length());
														}
														else {
															break;
														}													
																	switch(IndexUpdates.getMessageValue(messageColumnType)){				
																		case 1 : // IDCODE (1,"i")
																			 tmpid=Integer.parseInt(value[1]);	
																			break;
																		
																		case 2 : // SOURCEID (2,"Si")
																			  tmpSymbolId=value[1];	
																			 break;
																		
																		case 3 :  //INDEXTIMESTAMP(3,"Tx"),
																			tmpTime =value[1];											 
																			break;
																		
																		case 4 ://  CURRENTVALUE(4,"Vc"),
																			tmpVc=value[1];		
																			break;
																		
																		case 5 : // HIGHVALUE(5,"Vh"),,
																			tmpVh = value[1];																				
																			break;	
																			
																		case 6 ://   LOWVALUE(6,"Vl"),,
																			tmpVl=value[1];		
																			break;
																			
																		case 7 ://  DIFFDAYPER(7,"Dd"),
																			tmpDd=value[1];		
																			break;
																			
																		case 8 ://  DIFFDAYNOM(8,"DDn");
																			tmpDDn=value[1];		
																			finished =true;
																			break;
																			
																		case 9 :// SEQUENCENO(9,"n");
																			sequence_no= Long.parseLong(value[1]);
																			TIPDataObjects.last_Sequencer_Analyzer=sequence_no;
																			break;
																			
																		default:
																			break;
																	}	//End of Switch
													}
												}//End of For loop	
												
												 OBJ_TYPE = TadawulObjectCategory.getIndexMessageCategoty(tmpSymbolId);
												 logger.info("DataParserIndexUpdates tmpSymbolId :"+tmpSymbolId);
												
												 switch(OBJ_TYPE)
													{												 
												
													case SHAREINDEX:									            	 
										            	 //To check already available data
										            	 if(TIPDataObjects.tipIndex_Symbol_ArrayIndexMap.get(tmpSymbolId) !=null)
										            	 {
										            		 tmpIndexIter = (int)TIPDataObjects.tipIndex_Symbol_ArrayIndexMap.get(tmpSymbolId);
										            	 }else
										            	 {
										                	//Logger Error
										            		 logger.error("Index Update Msg received for an unknown Index Id " + tmpSymbolId);
										            	 }
										            	 try
										            	 {										            	
										            	  if(null != tmpTime)	TIPDataObjects.tipIndex[tmpIndexIter].trans_time= Timestamp.valueOf(Utility.appendTxWithToday(tmpTime));
										            	 }catch(Exception e)
														 {
																 logger.info("Errors occured in IND Processing :"+e);
															 }
										            	  if(null!=tmpVc)	
										            	  {
										            	 	 tipIndex_index_price = Math.round(Double.parseDouble(tmpVc) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
									                            if (TIPDataObjects.tipIndex[tmpIndexIter].index_price != tipIndex_index_price) {
									                                tmpIndexUpdFlag = 1;
									                                TIPDataObjects.tipIndex[tmpIndexIter].index_price = tipIndex_index_price;
									                            }
									                           /* if (TIPDataObjects.tipIndex[tmpIndexIter].open_price <= 0.0) { // Fix for UAT bug - 21-Apr-2009
									                            	TIPDataObjects.tipIndex[tmpIndexIter].open_price = TIPDataObjects.tipIndex[tmpIndexIter].previous_index_price;
									                                logger.debug("Index Update: " + TIPDataObjects.tipIndex[tmpIndexIter].Fk_te_symbol + " Open Value: " + TIPDataObjects.tipIndex[tmpIndexIter].open_price
									                                        + " Cur Value: " + TIPDataObjects.tipIndex[tmpIndexIter].index_price);
									                                tmpIndexUpdFlag = 1;
									                            }*/
									                            
									                            //Added to update the first index update for Index Open Price
									                            if(!TIPDataObjects.tipIndex[tmpIndexIter].isOpenIndexPriceAlreadyUpdated)
									                            {
									                            	TIPDataObjects.tipIndex[tmpIndexIter].open_price = tipIndex_index_price;
									                            	TIPDataObjects.tipIndex[tmpIndexIter].isOpenIndexPriceAlreadyUpdated = true;
									                            	 tmpIndexUpdFlag = 1;
									                            	logger.debug("Index Open Price Updated for Index :"+TIPDataObjects.tipIndex[tmpIndexIter].Fk_te_symbol +"  Open Index Value :"+ TIPDataObjects.tipIndex[tmpIndexIter].open_price);
									                            	
									                            }
										            	  }
										            	 	
										            	  if(null!=tmpVh)
										            	  {
										            		  tmpIndexUpdFlag =1;
										            		  TIPDataObjects.tipIndex[tmpIndexIter].high_price=Math.round(Double.parseDouble(tmpVh) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
										            	  }
										            	  if(null!=tmpVl)  
										            		  {
										            		  tmpIndexUpdFlag =1;
										            		  TIPDataObjects.tipIndex[tmpIndexIter].low_price=Math.round(Double.parseDouble(tmpVl) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;;										            		  
										            		  }
										            	  if(null!=tmpDd)   
										            		  {
										            		  tmpIndexUpdFlag =1;
										            		  TIPDataObjects.tipIndex[tmpIndexIter].percent_change=Math.round(Double.parseDouble(tmpDd) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;;
										            		  }
										            	  if(null!=tmpDDn) 
										            		  {
										            		  tmpIndexUpdFlag =1;
										            		   TIPDataObjects.tipIndex[tmpIndexIter].net_change=Math.round(Double.parseDouble(tmpDDn) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;;
										            		  }
									                      
										            	  TIPDataObjects.tipIndex[tmpIndexIter].sequence_no = sequence_no;
										            	  
										            	  if (tmpIndexUpdFlag == 1) {										                     
										                        if (! TIPDataObjects.tipIndex[tmpIndexIter].isUpdated) {
										                        	TIPDataObjects.tipIndex[tmpIndexIter].isUpdated = true;
										                        	TIPDataObjects.totIndexUpdated++;
										                                 }
										                        
										                        TIP_IndexVO indexToQ = new TIP_IndexVO(TIPDataObjects.tipIndex[tmpIndexIter]);
										                        MessageDatabaseQueue.tipIndexIntraQ.setFrame(indexToQ);
										            	  }
										 					
										            	  
										 					//logger.info("TIPDataObjects.tipIndex[tipIndexIter] : Index "+tmpIndexIter +" Symbol: "+TIPDataObjects.tipIndex[tmpIndexIter].Fk_te_symbol +" ID: "+TIPDataObjects.tipIndex[tmpIndexIter].ListId+" Index Price :"+TIPDataObjects.tipIndex[tmpIndexIter].index_price +" High Price :"+TIPDataObjects.tipIndex[tmpIndexIter].high_price+" Low Price :"+TIPDataObjects.tipIndex[tmpIndexIter].low_price+" Percent Change :"+TIPDataObjects.tipIndex[tmpIndexIter].percent_change +" Net Change :"+TIPDataObjects.tipIndex[tmpIndexIter].net_change);     					
    		
														break;
														
													case SMESHAEINDEX:	
										            	//To check already available data
										            	 if( TIPDataObjects.tipSMEIndex_Symbol_ArrayIndexMap.get(tmpSymbolId) !=null)
										            	 {
										            		 tmpIndexIter = (int)TIPDataObjects.tipSMEIndex_Symbol_ArrayIndexMap.get(tmpSymbolId);
										            	 }else
										            	 {
										            		//logger Error
										            		 logger.error("Index Update Msg received for an unknown Index Id " + tmpSymbolId);
										            	 }	            
										            	 try
										            	 {
										            	 if(null != tmpTime)TIPDataObjects.tipSMEIndex[tmpIndexIter].trans_time= Timestamp.valueOf(Utility.appendTxWithToday(tmpTime));		
										            	 } catch(Exception e)
														 {
																// logger.info("Errors occured in IND Processing :"+e);
															 }
										            	 if(null!=tmpVc)	
										            	  {
										            	 	 tipIndex_index_price = Math.round(Double.parseDouble(tmpVc) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
									                            if (TIPDataObjects.tipSMEIndex[tmpIndexIter].index_price != tipIndex_index_price) {
									                                tmpIndexUpdFlag = 1;
									                                TIPDataObjects.tipSMEIndex[tmpIndexIter].index_price = tipIndex_index_price;
									                            }
									                           /* if (TIPDataObjects.tipSMEIndex[tmpIndexIter].open_price <= 0.0) { // Fix for UAT bug - 21-Apr-2009
									                            	TIPDataObjects.tipSMEIndex[tmpIndexIter].open_price = TIPDataObjects.tipSMEIndex[tmpIndexIter].previous_index_price;
									                                logger.debug("Index Update: " + TIPDataObjects.tipSMEIndex[tmpIndexIter].Fk_te_symbol + " Open Value: " + TIPDataObjects.tipSMEIndex[tmpIndexIter].open_price
									                                        + " Cur Value: " + TIPDataObjects.tipSMEIndex[tmpIndexIter].index_price);
									                                tmpIndexUpdFlag = 1;
									                            }*/
									                            
									                            //Added to update the first index update for Index Open Price
									                            if(!TIPDataObjects.tipSMEIndex[tmpIndexIter].isOpenIndexPriceAlreadyUpdated)
									                            {
									                            	TIPDataObjects.tipSMEIndex[tmpIndexIter].open_price = tipIndex_index_price;
									                            	TIPDataObjects.tipSMEIndex[tmpIndexIter].isOpenIndexPriceAlreadyUpdated = true;
									                            	tmpIndexUpdFlag = 1;
									                            	logger.debug("Index Open Price Updated for SMEIndex :"+TIPDataObjects.tipSMEIndex[tmpIndexIter].Fk_te_symbol +"  Open Index Value :"+ TIPDataObjects.tipSMEIndex[tmpIndexIter].open_price);
									                            }
										            	  }
								                        if(null!=tmpVh) 
								                        	{
								                        	TIPDataObjects.tipSMEIndex[tmpIndexIter].high_price=Math.round(Double.parseDouble(tmpVh) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
								                        	 tmpIndexUpdFlag = 1;
								                        	}
								                        if(null!=tmpVl) 
								                        	{
								                        	TIPDataObjects.tipSMEIndex[tmpIndexIter].low_price=Math.round(Double.parseDouble(tmpVl) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;;
								                        	 tmpIndexUpdFlag = 1;
								                        	}
								                        if(null!=tmpDd) 
								                        	{
								                        	TIPDataObjects.tipSMEIndex[tmpIndexIter].percent_change=Math.round(Double.parseDouble(tmpDd) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;;
								                        	 tmpIndexUpdFlag = 1;
								                        	}
								                        if(null!=tmpDDn) 
								                        	{
								                        	TIPDataObjects.tipSMEIndex[tmpIndexIter].net_change=Math.round(Double.parseDouble(tmpDDn) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;;
								                        	 tmpIndexUpdFlag = 1;
								                        	}
								                       
								                        TIPDataObjects.tipSMEIndex[tmpIndexIter].sequence_no = sequence_no;
								                        
								                        if (tmpIndexUpdFlag == 1) {										                     
									                        if (! TIPDataObjects.tipSMEIndex[tmpIndexIter].isUpdated) {
									                        	TIPDataObjects.tipSMEIndex[tmpIndexIter].isUpdated = true;
									                        	TIPDataObjects.totSMEIndexUpdated++;
									                                 }
									                        TIP_SMEIndexVO indexToQ = new TIP_SMEIndexVO(TIPDataObjects.tipSMEIndex[tmpIndexIter]);
						  			                         MessageDatabaseQueue.tipSMEIndexIntraQ.setFrame(indexToQ);		
									            	     }		
								                        
								                        
								                      //  logger.info("TIPDataObjects.tipSMEIndex[tipIndexIter] : Index "+tmpIndexIter +" Symbol: "+TIPDataObjects.tipSMEIndex[tmpIndexIter].Fk_te_symbol +" ID: "+TIPDataObjects.tipSMEIndex[tmpIndexIter].ListId+" Index Price :"+TIPDataObjects.tipSMEIndex[tmpIndexIter].index_price +" High Price :"+TIPDataObjects.tipSMEIndex[tmpIndexIter].high_price+" Low Price :"+TIPDataObjects.tipSMEIndex[tmpIndexIter].low_price+" Percent Change :"+TIPDataObjects.tipSMEIndex[tmpIndexIter].percent_change +" Net Change :"+TIPDataObjects.tipSMEIndex[tmpIndexIter].net_change);
														break;
														
													case BONDINDEX:
										            	//To check already available data
										            	 if( TIPDataObjects.tipBondIndex_Symbol_ArrayIndexMap.get(tmpSymbolId) !=null)
										            	 {
										            		 tmpIndexIter = (int)TIPDataObjects.tipBondIndex_Symbol_ArrayIndexMap.get(tmpSymbolId);
										            		 logger.info("BONDINDEX tipBondIndex_Symbol_ArrayIndexMap found :"+tmpIndexIter);
										            	 }else
										            	 {
										            		//logger Error
										            		 logger.error("Index Update Msg received for an unknown Index Id " + tmpSymbolId);
										            	 }	 
										            	try
										            	{
										            	 if(null != tmpTime) TIPDataObjects.tipBondIndex[tmpIndexIter].trans_time= Timestamp.valueOf(Utility.appendTxWithToday(tmpTime));
										            	}catch(Exception e)
														 {
															 logger.info("Errors occured in BONDINDEX IND Processing :"+e);
															 }
										            	 if(null!=tmpVc)	
										            	  {	
											            	 	 tipIndex_index_price = Math.round(Double.parseDouble(tmpVc) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
										                            if (TIPDataObjects.tipBondIndex[tmpIndexIter].index_price != tipIndex_index_price) {
										                                tmpIndexUpdFlag = 1;
										                                TIPDataObjects.tipBondIndex[tmpIndexIter].index_price = tipIndex_index_price;
										                            }
										                            if (TIPDataObjects.tipBondIndex[tmpIndexIter].open_price <= 0.0) { // Fix for UAT bug - 21-Apr-2009
										                            	TIPDataObjects.tipBondIndex[tmpIndexIter].open_price = TIPDataObjects.tipBondIndex[tmpIndexIter].previous_index_price;
										                                logger.debug("Index Update: " + TIPDataObjects.tipBondIndex[tmpIndexIter].Fk_te_symbol + " Open Value: " + TIPDataObjects.tipBondIndex[tmpIndexIter].open_price
										                                        + " Cur Value: " + TIPDataObjects.tipBondIndex[tmpIndexIter].index_price);
										                                tmpIndexUpdFlag = 1;
										                            }
										            	  }
								                        if(null!=tmpVh)
								                        	{
								                        	TIPDataObjects.tipBondIndex[tmpIndexIter].high_price=Math.round(Double.parseDouble(tmpVh) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
								                        	 tmpIndexUpdFlag = 1;
								                        	}
								                        if(null!=tmpVl)  
								                        	{
								                        	TIPDataObjects.tipBondIndex[tmpIndexIter].low_price=Math.round(Double.parseDouble(tmpVl) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;;
								                        	 tmpIndexUpdFlag = 1;
								                        	}
								                        if(null!=tmpDd) 
								                        	{
								                        	TIPDataObjects.tipBondIndex[tmpIndexIter].percent_change=Math.round(Double.parseDouble(tmpDd) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;;
								                        	 tmpIndexUpdFlag = 1;
								                        	}
								                        if(null!=tmpDDn) 
								                        	{
								                        	TIPDataObjects.tipBondIndex[tmpIndexIter].net_change=Math.round(Double.parseDouble(tmpDDn) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;;
								                        	 tmpIndexUpdFlag = 1;
								                        	}
														
								                        TIPDataObjects.tipBondIndex[tmpIndexIter].sequence_no = sequence_no; 
								                        
								                        if (tmpIndexUpdFlag == 1) {										                     
									                        if (! TIPDataObjects.tipBondIndex[tmpIndexIter].isUpdated) {
									                        	TIPDataObjects.tipBondIndex[tmpIndexIter].isUpdated = true;
									                        	TIPDataObjects.totBondIndexUpdated++;
									                                 }
									                        TIP_BondIndexVO indexToQ = new TIP_BondIndexVO(TIPDataObjects.tipBondIndex[tmpIndexIter]);
						  			                         MessageDatabaseQueue.tipBondIndexIntraQ.setFrame(indexToQ);
									            	     }
								                        
								                       
								                        logger.info("TIPDataObjects.tipBondIndex[tipIndexIter] : Index "+tmpIndexIter +" Symbol: "+TIPDataObjects.tipBondIndex[tmpIndexIter].Fk_te_symbol +" ID: "+TIPDataObjects.tipBondIndex[tmpIndexIter].ListId+" Index Price :"+TIPDataObjects.tipBondIndex[tmpIndexIter].index_price +" High Price :"+TIPDataObjects.tipBondIndex[tmpIndexIter].high_price+" Low Price :"+TIPDataObjects.tipBondIndex[tmpIndexIter].low_price+" Percent Change :"+TIPDataObjects.tipBondIndex[tmpIndexIter].percent_change +" Net Change :"+TIPDataObjects.tipBondIndex[tmpIndexIter].net_change);
									            	 
														break;
												default:
													break;
									             }	
										  	}
								
									  }//End of Try
									  catch(Exception e)
									  {
										  logger.error("Error Occured in INd Processing", e);
										 e.printStackTrace();
										 logger.error("Error Occured in INd Processing", e);
									  }
									  
									  
									//logger.info("DataParserIndexUpdates  Statistics : Total  No of INd Messages Parsed : "+totalINdMessages);
									  
									
								}//End of InnerWhile
						
							 Utility.sleepAnalyser(TipConstants.DATA_ANALYZER_SLEEP_INTERVAL_IND);
				}//Outer while
		}//Function run End
		
		
	
	
	
}
