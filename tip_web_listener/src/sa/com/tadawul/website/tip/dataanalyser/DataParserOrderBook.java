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
import sa.com.tadawul.website.tip.messages.OrderBooks;
import sa.com.tadawul.website.tip.messages.TadawulObjectCategory;
import sa.com.tadawul.website.tip.queue.MessageQueue;
import sa.com.tadawul.website.tip.shared.DBManagerMarket;
import sa.com.tadawul.website.tip.shared.TIPDataObjects;
import sa.com.tadawul.website.tip.shared.TipConstants;
import sa.com.tadawul.website.tip.shared.Utility;
import sa.com.tadawul.website.tip.vo.BestBidOffVO;
import sa.com.tadawul.website.tip.vo.TIP_BaseVO;

public class DataParserOrderBook  implements Runnable{
	
	private final static Log logger = LogFactory.getLog(DataParserOrderBook.class);
	
	private String message = "";
	String messageValue[];
	private String value [] = new String[2];
	private String messageColumnType = "";
	
	int totalOrderBookMessages;
	long sequence_no=0;
	int tmpShareUpdFlag,tmpBondUpdFlag=0;
		
	int tmpid=0;	
	String tmpSymbolId="";		
	int tmpIterId=0;
	
	String tmpTx=null,tmpOf=null,tmpLDp=null,tmpLDs=null,tmpb=null,tmpVb=null,tmpa=null,tmpVa=null,tmpYb=null,tmpYa=null ;
	
	boolean isAProccessed=false;boolean isVAProccessed=false;boolean isBProccessed=false;boolean isVBProccessed=false; boolean isLDsProccesed =false;boolean isLDbProccesed =false;
	
	
    private int flgAskLvlDel = 0;
    private int flgOrderFlush = 0;
    private int flgBidLvlDel = 0;
    int split = 0;
    long longLevel = 0;
	
	public DataParserOrderBook(){	
	}

	@Override
	public void run() {
		
		logger.info("DataParserOrderBook Thread started. " );		
								
		while(true)
				{	
					while (MessageQueue.orderBookQueue != null && MessageQueue.orderBookQueue.size() > 0) {
						//logger.info("DataParserOrderBook Queue not null. Size  :"+ orderBookQueue.size() );
						if(totalOrderBookMessages==0)
						{
							//First message processing should be delayed until other Low volume thread are completed BDIn, BDLi
							Utility.sleepAnalyser(TipConstants.SLEEPINTERVALFOROBK_OKS_INOFORFIRSTMESSAGE);
						}
						totalOrderBookMessages++;
						TIPDataObjects.totalMessagesParsed++;
						  try {
							  	message =(String) MessageQueue.orderBookQueue.pop();	
							  	tmpTx=null;tmpOf=null;tmpLDp=null;tmpLDs=null;tmpb=null;tmpVb=null;tmpa=null;tmpVa=null;tmpYb=null;tmpYa=null ;
								isAProccessed =false; isVAProccessed=false;isBProccessed=false;isVBProccessed=false;isLDsProccesed=false;isLDbProccesed=false;
							  	sequence_no =0;		
							  	
							  	if(message != null && !message.equals("")) {
							  		messageValue = message.split(TipConstants.SPLITMESSAGE);										  
									for (int i=0;i< messageValue.length ; i++) {
										value = messageValue[i].split(TipConstants.SPLITVALUE, 2);
										if (value != null && value.length > 1) {
											if (value[0] != null && value[1] != null) {
												messageColumnType = value[0]
														+ messageValue[i].charAt(value[0].length());
											} else {
												break;
											}
												
														switch(OrderBooks.getMessageValue(messageColumnType)){				
															case 1 : // IDCODE (1,"i")
																 tmpid=Integer.parseInt(value[1]);															
																break;
															
															case 2 : // SOURCEID (2,"Si")
																  tmpSymbolId=value[1];															
																 break;
															
															case 3 :  //  TIMEEXEC (3, "Tx"),
																tmpTx =value[1];
																break;
															
															case 4 ://  ORDERBOOKFLUSH (4, "Of"),
																tmpOf=value[1];	
																//logger.info( " OBK Message tmpOf :"+tmpOf);
																break;
															
															case 5 : // BIDLEVELDELETED (5, "LDb"),
																if(!isLDbProccesed)
																	tmpLDp = value[1];
																isLDbProccesed =true;
																break;	
																
															case 6 ://  ASKLEVELDELETED (6, "LDs"),	
																if(!isLDsProccesed)
																	tmpLDs=value[1];
																isLDsProccesed=true;
																break;
																
															case 7 ://   BIDPRICEATLEVEL (7, "b"),  
																if(!isBProccessed)
																	tmpb=value[1];
																isBProccessed=true;
																break;	
																
															case 8 ://    BIDVOLUMEATLEVEL (8, "Vb"),	 
																if(!isVBProccessed)	
																	tmpVb=value[1];
																isVBProccessed =true;
																break;
																
															case 9 ://   ASKPRICEATLEVEL (9, "a"),	  
																if(!isAProccessed)
																	tmpa=value[1];
																isAProccessed = true;
																break;
																
															case 10 ://    ASKVOLUMEATLEVEL (10, "Va"),;
																if(!isVAProccessed)
																	tmpVa=value[1];
																isVAProccessed = true;
																break;
														
															case 11 ://     SEQUENCENO(11,"n");
																sequence_no=Long.parseLong(value[1]);
																TIPDataObjects.last_Sequencer_Analyzer=sequence_no;
																break;
															//Added for Bonds Yield calculations projects CRQ21157	
															case 12 ://    BIDYIELDATLEVEL(12,"Yb"),
																tmpYb=value[1];
																break;	
																
															case 13 ://    ASKYIELDATLEVEL(13,"Ya");
																tmpYa=value[1];
																break;	
															default:
																break;
														}	//End of Switch
										}
									}//End of For loop													
									
									 flgOrderFlush = 0;
					                 flgBidLvlDel = 0;
					                 flgAskLvlDel = 0;
					                 split = 0;
					                 longLevel = 0;		
					                 tmpShareUpdFlag=0;
									 TadawulObjectCategory OBJ_TYPE = TadawulObjectCategory.getShareMessageCategoty(tmpSymbolId);
									 
		/*							 logger.info("OBk symbold:"+tmpSymbolId);
									 logger.info("OBk OBJ_TYPE:"+OBJ_TYPE.name());
		*/							 
									 switch(OBJ_TYPE)
										{
											case SHARE:
												//Find Index
												 if(TIPDataObjects.tipShare_Symbol_ArrayIndexMap.get(tmpSymbolId) !=null)
								            	 {
								            		 tmpIterId = (int)TIPDataObjects.tipShare_Symbol_ArrayIndexMap.get(tmpSymbolId);
								            	 } else
								            	 {
								            		 logger.info("OBk received for unkown symbol :"+tmpSymbolId);
								            		 break;
								            	 }
												 	if(null!=tmpOf)  flgOrderFlush=1;
												    if(null!=tmpLDp)  flgBidLvlDel =  Integer.parseInt(tmpLDp) ;
												    if(null!=tmpLDs)  flgAskLvlDel =  Integer.parseInt(tmpLDs) ;
												    TIPDataObjects.tipShare[tmpIterId].messageType = "";
												    if (flgOrderFlush == 1 && TIPDataObjects.tipShare[tmpIterId].market_status != TipConstants.TIP_DB_STATE_PRECLOSED
							                                && TIPDataObjects.tipShare[tmpIterId].market_status != TipConstants.TIP_DB_STATE_CLOSED) {
												    	TIPDataObjects.tipShare[tmpIterId].bid_price = 0;
												    	TIPDataObjects.tipShare[tmpIterId].bid_quantity = 0;
												    	TIPDataObjects.tipShare[tmpIterId].ask_price = 0;
												    	TIPDataObjects.tipShare[tmpIterId].ask_quantity = 0;
												    	TIPDataObjects.tipShare[tmpIterId].bestBids = TIP_BaseVO.getInitailBestBidsOffers();
												    	TIPDataObjects.tipShare[tmpIterId].bestOffers = TIP_BaseVO.getInitailBestBidsOffers();
					   	                                tmpShareUpdFlag = 1;
							                        } else {
														if (flgBidLvlDel > 0 && flgBidLvlDel <= DBManagerMarket.ORDER_BOOK_DEPTH) {
															for (int i = flgBidLvlDel-1; i < TIPDataObjects.tipShare[tmpIterId].bestBids.length-1; i++) {
																TIPDataObjects.tipShare[tmpIterId].bestBids [i] = TIPDataObjects.tipShare[tmpIterId].bestBids [i + 1];
														    }

											    			TIPDataObjects.tipShare[tmpIterId].bestBids[TIPDataObjects.tipShare[tmpIterId].bestBids.length - 1] =  new BestBidOffVO();
														    
														    
															if (flgBidLvlDel == 1) {
																TIPDataObjects.tipShare[tmpIterId].bid_price = 0;
																TIPDataObjects.tipShare[tmpIterId].bid_quantity = 0;
															}
															//TIPDataObjects.tipShare[tmpIterId].bestBids[(int) (flgBidLvlDel-1)] =  new BestBidOffVO();
															TIPDataObjects.tipShare[tmpIterId].messageType = "OBk";
															tmpShareUpdFlag = 1;
														}
														if (flgAskLvlDel > 0 && flgAskLvlDel <= DBManagerMarket.ORDER_BOOK_DEPTH) {
															for (int i = flgAskLvlDel-1; i < TIPDataObjects.tipShare[tmpIterId].bestOffers.length - 1; i++) {
																TIPDataObjects.tipShare[tmpIterId].bestOffers [i] = TIPDataObjects.tipShare[tmpIterId].bestOffers [i + 1];
																
														    }

														    // Set the last element to null after shifting
															TIPDataObjects.tipShare[tmpIterId].bestOffers[TIPDataObjects.tipShare[tmpIterId].bestOffers.length - 1] =  new BestBidOffVO();
															
								                            if (flgAskLvlDel == 1) {
								                            	TIPDataObjects.tipShare[tmpIterId].ask_price = 0;
								                            	TIPDataObjects.tipShare[tmpIterId].ask_quantity = 0;
								                            }
								                           // TIPDataObjects.tipShare[tmpIterId].bestOffers[(int) (flgAskLvlDel-1)] =  new BestBidOffVO();
								                            TIPDataObjects.tipShare[tmpIterId].messageType = "OBk";
							                                tmpShareUpdFlag = 1;
														}
							                        }
												    
												    if(null!=tmpb)
												    {
												    	split = tmpb.indexOf(':');
												    	longLevel =(long)Double.parseDouble(tmpb.substring(0, split));	
												    	if(longLevel > 0 && longLevel <= DBManagerMarket.ORDER_BOOK_DEPTH) {
												    	    double _bidPrice = Math.round(Double.parseDouble(tmpb.substring(split+1)) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
							                                if (longLevel == 1) {
							                                	TIPDataObjects.tipShare[tmpIterId].bid_price = _bidPrice;
							                                }
							                                TIPDataObjects.tipShare[tmpIterId].bestBids[(int) (longLevel-1)].price =_bidPrice;
							                                TIPDataObjects.tipShare[tmpIterId].messageType = "OBk";
							                                logger.info("OBk BBBO MAIN Added longLevel "+longLevel+" BID price "+TIPDataObjects.tipShare[tmpIterId].bestBids[(int) (longLevel- 1)].price+" actaul "+_bidPrice);

						                                	tmpShareUpdFlag = 1;
												    	}
												    }
												    
												    if(null!=tmpVb)
												    {
												    	split = tmpVb.indexOf(':');
												    	longLevel =(long)Double.parseDouble(tmpVb.substring(0, split));		
												    	long _bidQuantity = (long)Double.parseDouble(tmpVb.substring(split+1));
												    	if(longLevel > 0 && longLevel <= DBManagerMarket.ORDER_BOOK_DEPTH) {
							                                if (longLevel == 1) {
							                                	TIPDataObjects.tipShare[tmpIterId].bid_quantity =  _bidQuantity;
							                                }
							                                TIPDataObjects.tipShare[tmpIterId].bestBids[(int) (longLevel-1)].quantity =_bidQuantity;
							                                TIPDataObjects.tipShare[tmpIterId].messageType = "OBk";
							                                logger.info("OBk BBBO MAIN Added longLevel "+longLevel+" BID Quantity "+TIPDataObjects.tipShare[tmpIterId].bestBids[(int) (longLevel- 1)].quantity+" actaul "+_bidQuantity);

						                                	tmpShareUpdFlag = 1;
												    	}
												    }
												    
												    if(null!=tmpa)
												    {
												    	split = tmpa.indexOf(':');
												    	longLevel =(long)Double.parseDouble(tmpa.substring(0, split));	

							                            if(longLevel > 0 && longLevel <= DBManagerMarket.ORDER_BOOK_DEPTH) {
													    	double _askPrice = Math.round(Double.parseDouble(tmpa.substring(split+1)) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
								                            if (longLevel == 1) {
								                                TIPDataObjects.tipShare[tmpIterId].ask_price = 	_askPrice;				
								                            }
							                                TIPDataObjects.tipShare[tmpIterId].bestOffers[(int) (longLevel-1)].price =_askPrice;
							                                TIPDataObjects.tipShare[tmpIterId].messageType = "OBk";
							                                logger.info("OBk BBBO MAIN Added longLevel "+longLevel+" ASK price "+TIPDataObjects.tipShare[tmpIterId].bestOffers[(int) (longLevel- 1)].price+" actaul "+_askPrice);

							                                tmpShareUpdFlag = 1;
							                            }
												    }
												    
												    if(null!=tmpVa)
												    {
												    	split = tmpVa.indexOf(':');
												    	longLevel =(long)Double.parseDouble(tmpVa.substring(0, split));	
												    	
							                            if(longLevel > 0 && longLevel <= DBManagerMarket.ORDER_BOOK_DEPTH) {
							                            	long _askQuantity = (long)Double.parseDouble(tmpVa.substring(split+1));
								                            if (longLevel == 1) {
								                                TIPDataObjects.tipShare[tmpIterId].ask_quantity =  _askQuantity;
								                           	}
							                                TIPDataObjects.tipShare[tmpIterId].bestOffers[(int) (longLevel-1)].quantity = _askQuantity;
							                                TIPDataObjects.tipShare[tmpIterId].messageType = "OBk";
							                                logger.info("OBk BBBO MAIN Added longLevel "+longLevel+" ASK Quantity "+TIPDataObjects.tipShare[tmpIterId].bestOffers[(int) (longLevel- 1)].quantity+" actaul "+_askQuantity);

							                                tmpShareUpdFlag = 1;
							                            }
												    }				
												    try
												    {
												    if(null!=tmpTx)   TIPDataObjects.tipShare[tmpIterId].last_update_time = Timestamp.valueOf(Utility.appendTxWithToday(tmpTx));
												    }catch(Exception e)
									 				 {
														//  logger.error("Error Occured in OBK Processing", e);									 				 
									 				 }
												    
												   
												    
												    if (tmpShareUpdFlag == 1) {
							                            //tipShareQ.push(new Integer(tmpIter2));
							                            if (! TIPDataObjects.tipShare[tmpIterId].isUpdated) {
							                            	TIPDataObjects.tipShare[tmpIterId].isUpdated = true;
							                            	TIPDataObjects.totShareUpdated++;
							                            }
							                        }
												       
												    TIPDataObjects.tipShare[tmpIterId].sequence_no = sequence_no;
												   // logger.info("OBk Main  TIPDataObjects.tipShare[tmpIterId] : Shares "+tmpIterId +" Symbol: "+TIPDataObjects.tipShare[tmpIterId].fk_te_symbol +" TIPDataObjects.tipShare[tmpIterId].bid_price : "+TIPDataObjects.tipShare[tmpIterId].bid_price+" TIPDataObjects.tipShare[tmpIterId].bid_quantity  :"+TIPDataObjects.tipShare[tmpIterId].bid_quantity+" TIPDataObjects.tipShare[tmpIterId].ask_price :"+TIPDataObjects.tipShare[tmpIterId].ask_price +" TIPDataObjects.tipShare[tmpIterId].ask_quantity: "+TIPDataObjects.tipShare[tmpIterId].ask_quantity +"  TIPDataObjects.totShareUpdated:"+TIPDataObjects.totShareUpdated   );
												break;
											
											case SMESHARE:
												//Find Index
												 if(TIPDataObjects.tipSMEShare_Symbol_ArrayIndexMap.get(tmpSymbolId) !=null)
								            	 {
								            		 tmpIterId = (int)TIPDataObjects.tipSMEShare_Symbol_ArrayIndexMap.get(tmpSymbolId);
								            	 } else
								            	 {
								            		 logger.info("OBk received for unkown symbol :"+tmpSymbolId);
								            		 break;
								            	 }
												
												 	if(null!=tmpOf)  flgOrderFlush=1;
												    if(null!=tmpLDp)  flgBidLvlDel =  Integer.parseInt(tmpLDp) ;
												    if(null!=tmpLDs)  flgAskLvlDel =  Integer.parseInt(tmpLDs) ;
												    TIPDataObjects.tipSMEShare[tmpIterId].messageType = "";
												    if (flgOrderFlush == 1 && TIPDataObjects.tipSMEShare[tmpIterId].market_status != TipConstants.TIP_DB_STATE_PRECLOSED
							                                && TIPDataObjects.tipSMEShare[tmpIterId].market_status != TipConstants.TIP_DB_STATE_CLOSED) {
												    	TIPDataObjects.tipSMEShare[tmpIterId].bid_price = 0;
												    	TIPDataObjects.tipSMEShare[tmpIterId].bid_quantity = 0;
												    	TIPDataObjects.tipSMEShare[tmpIterId].ask_price = 0;
												    	TIPDataObjects.tipSMEShare[tmpIterId].ask_quantity = 0;
												    	TIPDataObjects.tipSMEShare[tmpIterId].bestBids = TIPDataObjects.tipSMEShare[tmpIterId].getInitailBestBidsOffers();
												    	TIPDataObjects.tipSMEShare[tmpIterId].bestOffers = TIPDataObjects.tipSMEShare[tmpIterId].getInitailBestBidsOffers();
												    	TIPDataObjects.tipSMEShare[tmpIterId].messageType = "OBk";
							                            tmpShareUpdFlag = 1;
							                        } else {
							                        	if (flgBidLvlDel > 0 && flgBidLvlDel <=  DBManagerMarket.ORDER_BOOK_DEPTH) {
															for (int i = flgBidLvlDel-1; i < TIPDataObjects.tipSMEShare[tmpIterId].bestBids.length - 1; i++) {
																TIPDataObjects.tipSMEShare[tmpIterId].bestBids [i] = TIPDataObjects.tipSMEShare[tmpIterId].bestBids [i + 1];
														    }

											    			TIPDataObjects.tipSMEShare[tmpIterId].bestBids[TIPDataObjects.tipSMEShare[tmpIterId].bestBids.length - 1] =  new BestBidOffVO();
								                            if (flgBidLvlDel == 1) {
								                            	TIPDataObjects.tipSMEShare[tmpIterId].bid_price = 0;
								                            	TIPDataObjects.tipSMEShare[tmpIterId].bid_quantity = 0;		
								                            }
								                          //  TIPDataObjects.tipSMEShare[tmpIterId].bestBids[(int) (flgBidLvlDel-1)] = new BestBidOffVO();
								                            TIPDataObjects.tipSMEShare[tmpIterId].messageType = "OBk";
							                                tmpShareUpdFlag = 1;
							                        	}
							                        	if(flgAskLvlDel > 0 && flgAskLvlDel <= DBManagerMarket.ORDER_BOOK_DEPTH) {
															for (int i = flgAskLvlDel-1; i < TIPDataObjects.tipSMEShare[tmpIterId].bestOffers.length - 1; i++) {
																TIPDataObjects.tipSMEShare[tmpIterId].bestOffers [i] = TIPDataObjects.tipSMEShare[tmpIterId].bestOffers [i + 1];
														    }

											    			TIPDataObjects.tipSMEShare[tmpIterId].bestOffers[TIPDataObjects.tipSMEShare[tmpIterId].bestOffers.length - 1] =  new BestBidOffVO();
								                            if (flgAskLvlDel == 1) {
								                            	TIPDataObjects.tipSMEShare[tmpIterId].ask_price = 0;
								                            	TIPDataObjects.tipSMEShare[tmpIterId].ask_quantity = 0;	
								                            }
								                           // TIPDataObjects.tipSMEShare[tmpIterId].bestOffers[(int) (flgAskLvlDel-1)] = new BestBidOffVO();
								                            TIPDataObjects.tipSMEShare[tmpIterId].messageType = "OBk";
							                                tmpShareUpdFlag = 1;
							                        	}
							                        }
												    
													if (null != tmpb) {
														split = tmpb.indexOf(':');
														longLevel = (long) Double.parseDouble(tmpb.substring(0, split));

														if (longLevel > 0 && longLevel <=  DBManagerMarket.ORDER_BOOK_DEPTH) {
												    	    double _bidPrice = Math.round(Double.parseDouble(tmpb.substring(split+1)) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;

															if (longLevel == 1) {
																TIPDataObjects.tipSMEShare[tmpIterId].bid_price = _bidPrice;
															}
															TIPDataObjects.tipSMEShare[tmpIterId].bestBids[(int) (longLevel- 1)].price = _bidPrice;
															TIPDataObjects.tipSMEShare[tmpIterId].messageType = "OBk";
							                                logger.info("OBk BBBO SME Added longLevel "+longLevel+" BID price "+TIPDataObjects.tipSMEShare[tmpIterId].bestBids[(int) (longLevel- 1)].price+" actaul "+_bidPrice);

															tmpShareUpdFlag = 1;
														}

													}
												    
												    if(null!=tmpVb)
												    {
												    	split = tmpVb.indexOf(':');
												    	longLevel =(long)Double.parseDouble(tmpVb.substring(0, split));			
												    	if (longLevel > 0 && longLevel <=  DBManagerMarket.ORDER_BOOK_DEPTH) {
												    	    long _bidQuantity = (long)Double.parseDouble(tmpVb.substring(split+1));

															if (longLevel == 1) {
																TIPDataObjects.tipSMEShare[tmpIterId].bid_quantity = _bidQuantity;
															}
															TIPDataObjects.tipSMEShare[tmpIterId].bestBids[(int) (longLevel- 1)].quantity = _bidQuantity;
															TIPDataObjects.tipSMEShare[tmpIterId].messageType = "OBk";
							                                logger.info("OBk BBBO SME Added longLevel "+longLevel+" BID Quantity "+TIPDataObjects.tipSMEShare[tmpIterId].bestBids[(int) (longLevel- 1)].quantity+" actaul "+_bidQuantity);

															tmpShareUpdFlag = 1;
														}

												    }
												    
												    if(null!=tmpa)
												    {
												    	split = tmpa.indexOf(':');
												    	longLevel =(long)Double.parseDouble(tmpa.substring(0, split));				
												    	if (longLevel > 0 && longLevel <=  DBManagerMarket.ORDER_BOOK_DEPTH) {
												    	    double _askPrice = Math.round(Double.parseDouble(tmpa.substring(split+1)) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;

															if (longLevel == 1) {
																TIPDataObjects.tipSMEShare[tmpIterId].ask_price = _askPrice;
															}
															TIPDataObjects.tipSMEShare[tmpIterId].bestOffers[(int) (longLevel- 1)].price = _askPrice;
															TIPDataObjects.tipSMEShare[tmpIterId].messageType = "OBk";
							                                logger.info("OBk BBBO SME Added longLevel "+longLevel+" ASK price "+TIPDataObjects.tipSMEShare[tmpIterId].bestOffers[(int) (longLevel- 1)].price+" actaul "+_askPrice);

															tmpShareUpdFlag = 1;
														} 

												    }
												    
												    if(null!=tmpVa)
												    {
												    	split = tmpVa.indexOf(':');
												    	longLevel =(long)Double.parseDouble(tmpVa.substring(0, split));		
												    	if (longLevel > 0 && longLevel <=  DBManagerMarket.ORDER_BOOK_DEPTH) {
												    	    long _askQuantity = (long)Double.parseDouble(tmpVa.substring(split+1));

															if (longLevel == 1) {
																TIPDataObjects.tipSMEShare[tmpIterId].ask_quantity = _askQuantity;
															}
															TIPDataObjects.tipSMEShare[tmpIterId].bestOffers[(int) (longLevel- 1)].quantity = _askQuantity;
															TIPDataObjects.tipSMEShare[tmpIterId].messageType = "OBk";
							                                logger.info("OBk BBBO SME Added longLevel "+longLevel+" ASK Qunatity "+TIPDataObjects.tipSMEShare[tmpIterId].bestOffers[(int) (longLevel- 1)].quantity+" actaul "+_askQuantity);

															tmpShareUpdFlag = 1;
														} 
												    }
													try {
														if (null != tmpTx)
															TIPDataObjects.tipSMEShare[tmpIterId].last_update_time = Timestamp
																	.valueOf(Utility.appendTxWithToday(tmpTx));
													} catch (Exception e) {
														// logger.error("Error Occured in OBK Processing", e);
													}

													if (tmpShareUpdFlag == 1) {
														if (!TIPDataObjects.tipSMEShare[tmpIterId].isUpdated) {
															TIPDataObjects.tipSMEShare[tmpIterId].isUpdated = true;
															TIPDataObjects.totSMEShareUpdated++;
														}
													}
												
												    TIPDataObjects.tipSMEShare[tmpIterId].sequence_no = sequence_no;
												   // logger.info("OBk SME  TIPDataObjects.tipSMEShare[tmpIterId] : Shares "+tmpIterId +" Symbol: "+TIPDataObjects.tipSMEShare[tmpIterId].fk_te_symbol +" TIPDataObjects.tipSMEShare[tmpIterId].bid_price : "+TIPDataObjects.tipSMEShare[tmpIterId].bid_price+" TIPDataObjects.tipSMEShare[tmpIterId].bid_quantity  :"+TIPDataObjects.tipSMEShare[tmpIterId].bid_quantity+" TIPDataObjects.tipSMEShare[tmpIterId].ask_price :"+TIPDataObjects.tipSMEShare[tmpIterId].ask_price +" TIPDataObjects.tipSMEShare[tmpIterId].ask_quantity: "+TIPDataObjects.tipSMEShare[tmpIterId].ask_quantity +"  TIPDataObjects.totShareUpdated:"+TIPDataObjects.totSMEShareUpdated   );
												break;
											
											case BOND:
												//Find Index
												 if(TIPDataObjects.tipBond_Symbol_ArrayIndexMap.get(tmpSymbolId) !=null)
								            	 {
								            		 tmpIterId = (int)TIPDataObjects.tipBond_Symbol_ArrayIndexMap.get(tmpSymbolId);
								            	 } else
								            	 {
								            		 logger.info("OBk received for unkown symbol :"+tmpSymbolId);
								            		 break;
								            	 }
												
												    if(null!=tmpOf)  flgOrderFlush=1;
												    if(null!=tmpLDp)  flgBidLvlDel =  Integer.parseInt(tmpLDp) ;
												    if(null!=tmpLDs)  flgAskLvlDel =  Integer.parseInt(tmpLDs) ;
												    TIPDataObjects.tipBond[tmpIterId].messageType = "";
												    if (flgOrderFlush == 1 && TIPDataObjects.tipBond[tmpIterId].market_status != TipConstants.TIP_DB_STATE_PRECLOSED
							                                && TIPDataObjects.tipBond[tmpIterId].market_status != TipConstants.TIP_DB_STATE_CLOSED) {
												    	TIPDataObjects.tipBond[tmpIterId].bid_price = 0;
												    	TIPDataObjects.tipBond[tmpIterId].bid_quantity = 0;
												    	TIPDataObjects.tipBond[tmpIterId].ask_price = 0;
												    	TIPDataObjects.tipBond[tmpIterId].ask_quantity = 0;
												    	TIPDataObjects.tipBond[tmpIterId].bestBids = TIPDataObjects.tipBond[tmpIterId].getInitailBestBidsOffers();
												    	TIPDataObjects.tipBond[tmpIterId].bestOffers = TIPDataObjects.tipBond[tmpIterId].getInitailBestBidsOffers();
												    	TIPDataObjects.tipBond[tmpIterId].messageType = "OBk";
							                            tmpShareUpdFlag = 1;
							                        } else {
							                        	if(flgBidLvlDel >0 && flgBidLvlDel <= DBManagerMarket.ORDER_BOOK_DEPTH) {
							                        		for (int i = flgBidLvlDel-1; i < TIPDataObjects.tipBond[tmpIterId].bestBids.length - 1; i++) {
																TIPDataObjects.tipBond[tmpIterId].bestBids [i] = TIPDataObjects.tipBond[tmpIterId].bestBids [i + 1];
														    }

											    			TIPDataObjects.tipBond[tmpIterId].bestBids[TIPDataObjects.tipBond[tmpIterId].bestBids.length - 1] =  new BestBidOffVO();
											    			
								                            if (flgBidLvlDel == 1) {
								                            	TIPDataObjects.tipBond[tmpIterId].bid_price = 0;
								                            	TIPDataObjects.tipBond[tmpIterId].bid_quantity = 0;								                              
								                            }
								                           // TIPDataObjects.tipBond[tmpIterId].bestBids[flgBidLvlDel-1] = new BestBidOffVO();
								                            TIPDataObjects.tipBond[tmpIterId].messageType = "OBk";
								                            tmpShareUpdFlag = 1;
							                        	}
							                        	if(flgAskLvlDel >0 && flgAskLvlDel <= DBManagerMarket.ORDER_BOOK_DEPTH) {
							                        		for (int i = flgAskLvlDel-1; i < TIPDataObjects.tipBond[tmpIterId].bestOffers.length - 1; i++) {
																TIPDataObjects.tipBond[tmpIterId].bestOffers [i] = TIPDataObjects.tipBond[tmpIterId].bestOffers [i + 1];
														    }

											    			TIPDataObjects.tipBond[tmpIterId].bestOffers[TIPDataObjects.tipBond[tmpIterId].bestOffers.length - 1] =  new BestBidOffVO();
								                            if (flgAskLvlDel == 1) {
								                            	TIPDataObjects.tipBond[tmpIterId].ask_price = 0;
								                            	TIPDataObjects.tipBond[tmpIterId].ask_quantity = 0;								                              
								                            }							                            
								                           // TIPDataObjects.tipBond[tmpIterId].bestOffers[flgAskLvlDel-1] = new BestBidOffVO();
								                            TIPDataObjects.tipBond[tmpIterId].messageType = "OBk";
								                            // tmpShareUpdFlag = 1;  Why this commented?
							                        	}
							                        }
												    
												    if(null!=tmpb)
												    {
												    	split = tmpb.indexOf(':');
												    	longLevel =(long)Double.parseDouble(tmpb.substring(0, split));	
												    	if (longLevel > 0 && longLevel <=  DBManagerMarket.ORDER_BOOK_DEPTH) {
												    	    double _bidPrice = Math.round(Double.parseDouble(tmpb.substring(split+1)) * TipConstants.threeDecimalRoundOff) / TipConstants.threeDecimalRoundOff;

															if (longLevel == 1) {
																TIPDataObjects.tipBond[tmpIterId].bid_price = _bidPrice;
															}
															TIPDataObjects.tipBond[tmpIterId].bestBids[(int) (longLevel- 1)].price = _bidPrice;
															TIPDataObjects.tipBond[tmpIterId].messageType = "OBk";
							                                logger.info("OBk BBBO BOND Added longLevel "+longLevel+" BID  price "+TIPDataObjects.tipBond[tmpIterId].bestBids[(int) (longLevel- 1)].price+" actaul "+_bidPrice);

															tmpShareUpdFlag = 1;
														} 
												    }
												    
												    if(null!=tmpVb)
												    {
												    	split = tmpVb.indexOf(':');
												    	longLevel =(long)Double.parseDouble(tmpVb.substring(0, split));
												    	if (longLevel > 0 && longLevel <=  DBManagerMarket.ORDER_BOOK_DEPTH) {
												    		long _bidQuantity = (long)Double.parseDouble(tmpVb.substring(split+1));
							                                if (longLevel == 1) {
							                                	TIPDataObjects.tipBond[tmpIterId].bid_quantity =  _bidQuantity;
							                                }
							                                TIPDataObjects.tipBond[tmpIterId].bestBids[(int) (longLevel- 1)].quantity = _bidQuantity;
							                                TIPDataObjects.tipBond[tmpIterId].messageType = "OBk";
							                                logger.info("OBk BBBO BOND Added longLevel "+longLevel+"  BID Quantity "+TIPDataObjects.tipBond[tmpIterId].bestBids[(int) (longLevel- 1)].quantity+" actaul "+_bidQuantity);
						                                    tmpShareUpdFlag = 1;
												    	}
												    }
												    
												    if(null!=tmpa)
												    {
												    	split = tmpa.indexOf(':');
												    	longLevel =(long)Double.parseDouble(tmpa.substring(0, split));	
												    	if (longLevel > 0 && longLevel <=  DBManagerMarket.ORDER_BOOK_DEPTH) {
												    		double _askPrice = Math.round(Double.parseDouble(tmpa.substring(split+1)) * TipConstants.threeDecimalRoundOff) / TipConstants.threeDecimalRoundOff;
							                                if (longLevel == 1) {
							                                	TIPDataObjects.tipBond[tmpIterId].ask_price = _askPrice;
							                                }
							                                TIPDataObjects.tipBond[tmpIterId].bestOffers[(int) (longLevel- 1)].price = _askPrice;
							                                TIPDataObjects.tipBond[tmpIterId].messageType = "OBk";
							                                logger.info("OBk BBBO BOND Added longLevel "+longLevel+" ASK price "+TIPDataObjects.tipBond[tmpIterId].bestOffers[(int) (longLevel- 1)].price+" actaul "+_askPrice);

						                                    tmpShareUpdFlag = 1;
												    	}
												    }
												    
												    if(null!=tmpVa)
												    {
												    	split = tmpVa.indexOf(':');
												    	longLevel =(long)Double.parseDouble(tmpVa.substring(0, split));		
												    	if (longLevel > 0 && longLevel <=  DBManagerMarket.ORDER_BOOK_DEPTH) {
												    		long _askQuantity = (long)Double.parseDouble(tmpVa.substring(split+1));
							                                if (longLevel == 1) {
							                                	TIPDataObjects.tipBond[tmpIterId].ask_quantity = _askQuantity;
							                                }
							                                TIPDataObjects.tipBond[tmpIterId].bestOffers[(int) (longLevel- 1)].quantity = _askQuantity;
							                                TIPDataObjects.tipBond[tmpIterId].messageType = "OBk";
							                                logger.info("OBk BBBO BOND Added longLevel "+longLevel+" ASK Quantity "+TIPDataObjects.tipBond[tmpIterId].bestOffers[(int) (longLevel- 1)].quantity+" actaul "+_askQuantity);

						                                    tmpShareUpdFlag = 1;
												    	}
												    }		
													try {
														if (null != tmpTx)
															TIPDataObjects.tipBond[tmpIterId].last_update_time = Timestamp
																	.valueOf(Utility.appendTxWithToday(tmpTx));
													} catch (Exception e) {
														// logger.error("Error Occured in OBK Processing", e);
													}

												  //Added for Bonds Yield calculations projects CRQ21157	
												    if(null!=tmpYb)
												    {
												    	split = tmpYb.indexOf(':');
												    	 longLevel =(long)Double.parseDouble(tmpYb.substring(0, split));								                         
							                                if (longLevel == 1) {
							                                	TIPDataObjects.tipBond[tmpIterId].bestBid_yield = Math.round(Double.parseDouble(tmpYb.substring(split+1)) * TipConstants.fourDecimalRoundOff) / TipConstants.fourDecimalRoundOff;
							                                	tmpShareUpdFlag = 1;
							                                }
												    }
												    
												    
												    if(null!=tmpYa)
												    {
												    	split = tmpYa.indexOf(':');
												    	 longLevel =(long)Double.parseDouble(tmpYa.substring(0, split));								                         
							                                if (longLevel == 1) {
							                                	TIPDataObjects.tipBond[tmpIterId].bestOffer_yield = Math.round(Double.parseDouble(tmpYa.substring(split+1)) * TipConstants.fourDecimalRoundOff) / TipConstants.fourDecimalRoundOff;
							                                	tmpShareUpdFlag = 1;
							                                }
												    }
												    
												    if (tmpShareUpdFlag == 1) {							                           
							                            if (! TIPDataObjects.tipBond[tmpIterId].isUpdated) {
							                            	TIPDataObjects.tipBond[tmpIterId].isUpdated = true;
							                            	TIPDataObjects.totBondUpdated++;
							                            }
							                        }
												    
												    TIPDataObjects.tipBond[tmpIterId].sequence_no = sequence_no;
												   // logger.info("OBk Bond  TIPDataObjects.tipBond[tmpIterId] : Shares "+tmpIterId +" Symbol: "+TIPDataObjects.tipBond[tmpIterId].fk_fi_symbol+" TIPDataObjects.tipBond[tmpIterId].bid_price : "+TIPDataObjects.tipBond[tmpIterId].bid_price+" TIPDataObjects.tipBond[tmpIterId].bid_quantity  :"+TIPDataObjects.tipBond[tmpIterId].bid_quantity+" TIPDataObjects.tipBond[tmpIterId].ask_price :"+TIPDataObjects.tipSMEShare[tmpIterId].ask_price +" TIPDataObjects.tipBond[tmpIterId].ask_quantity: "+TIPDataObjects.tipBond[tmpIterId].ask_quantity +"  TIPDataObjects.totShareUpdated:"+TIPDataObjects.totBondUpdated   );
												break;
									
											case OTCBOND:
												//Find Index
												 if(TIPDataObjects.tipOTCBond_Symbol_ArrayIndexMap.get(tmpSymbolId) !=null)
								            	 {
								            		 tmpIterId = (int)TIPDataObjects.tipOTCBond_Symbol_ArrayIndexMap.get(tmpSymbolId);
								            	 } else
								            	 {
								            		 logger.info("OBk received for unkown symbol :"+tmpSymbolId);
								            		 break;
								            	 }
												
												    if(null!=tmpOf)  flgOrderFlush=1;
												    if(null!=tmpLDp && Integer.parseInt(tmpLDp)==1) flgBidLvlDel =  Integer.parseInt(tmpLDp) ;
												    if(null!=tmpLDs && Integer.parseInt(tmpLDs)==1) flgAskLvlDel =  Integer.parseInt(tmpLDs) ;
												    
												    if (flgOrderFlush == 1 && TIPDataObjects.tipOTCBond[tmpIterId].market_status != TipConstants.TIP_DB_STATE_PRECLOSED
							                                && TIPDataObjects.tipOTCBond[tmpIterId].market_status != TipConstants.TIP_DB_STATE_CLOSED) {
												    	TIPDataObjects.tipOTCBond[tmpIterId].bid_price = 0;
												    	TIPDataObjects.tipOTCBond[tmpIterId].bid_quantity = 0;
												    	TIPDataObjects.tipOTCBond[tmpIterId].ask_price = 0;
												    	TIPDataObjects.tipOTCBond[tmpIterId].ask_quantity = 0;
							                            tmpShareUpdFlag = 1;
							                        } else {
							                            if (flgBidLvlDel == 1) {
							                            	TIPDataObjects.tipOTCBond[tmpIterId].bid_price = 0;
							                            	TIPDataObjects.tipOTCBond[tmpIterId].bid_quantity = 0;
							                                tmpShareUpdFlag = 1;
							                            }
							                            if (flgAskLvlDel == 1) {
							                            	TIPDataObjects.tipOTCBond[tmpIterId].ask_price = 0;
							                            	TIPDataObjects.tipOTCBond[tmpIterId].ask_quantity = 0;
							                               // tmpShareUpdFlag = 1;
							                            }
							                        }
												    
												    if(null!=tmpb)
												    {
												    	split = tmpb.indexOf(':');
												    	 longLevel =(long)Double.parseDouble(tmpb.substring(0, split));								                         
							                                if (longLevel == 1) {
							                                	TIPDataObjects.tipOTCBond[tmpIterId].bid_price = Math.round(Double.parseDouble(tmpb.substring(split+1)) * TipConstants.threeDecimalRoundOff) / TipConstants.threeDecimalRoundOff;
							                                    tmpShareUpdFlag = 1;
							                                }
												    }
												    
												    if(null!=tmpVb)
												    {
												    	split = tmpVb.indexOf(':');
												    	 longLevel =(long)Double.parseDouble(tmpVb.substring(0, split));								                         
							                                if (longLevel == 1) {
							                                	TIPDataObjects.tipOTCBond[tmpIterId].bid_quantity =  (long)Double.parseDouble(tmpVb.substring(split+1));
							                                    tmpShareUpdFlag = 1;
							                                }
												    }
												    
												    if(null!=tmpa)
												    {
												    	split = tmpa.indexOf(':');
												    	 longLevel =(long)Double.parseDouble(tmpa.substring(0, split));								                         
							                                if (longLevel == 1) {
							                                	TIPDataObjects.tipOTCBond[tmpIterId].ask_price = Math.round(Double.parseDouble(tmpa.substring(split+1)) * TipConstants.threeDecimalRoundOff) / TipConstants.threeDecimalRoundOff;
							                                    tmpShareUpdFlag = 1;
							                                }
												    }
												    
												    if(null!=tmpVa)
												    {
												    	split = tmpVa.indexOf(':');
												    	 longLevel =(long)Double.parseDouble(tmpVa.substring(0, split));								                         
							                                if (longLevel == 1) {
							                                	TIPDataObjects.tipOTCBond[tmpIterId].ask_quantity =  (long)Double.parseDouble(tmpVa.substring(split+1));
							                                    tmpShareUpdFlag = 1;
							                                }
												    }		
												    try
												    {
												    if(null!=tmpTx)   TIPDataObjects.tipOTCBond[tmpIterId].last_update_time = Timestamp.valueOf(Utility.appendTxWithToday(tmpTx));
															}catch(Exception e)
											 				 {
																//  logger.error("Error Occured in OBK Processing", e);									 				 
														 }
												   
												  //Added for Bonds Yield calculations projects CRQ21157	
												    if(null!=tmpYb)
												    {
												    	split = tmpYb.indexOf(':');
												    	 longLevel =(long)Double.parseDouble(tmpYb.substring(0, split));								                         
							                                if (longLevel == 1) {
							                                	TIPDataObjects.tipOTCBond[tmpIterId].bestBid_yield = Math.round(Double.parseDouble(tmpYb.substring(split+1)) * TipConstants.fourDecimalRoundOff) / TipConstants.fourDecimalRoundOff;
							                                	tmpShareUpdFlag = 1;
							                                }
												    }
												    
												    
												    if(null!=tmpYa)
												    {
												    	split = tmpYa.indexOf(':');
												    	 longLevel =(long)Double.parseDouble(tmpYa.substring(0, split));								                         
							                                if (longLevel == 1) {
							                                	TIPDataObjects.tipOTCBond[tmpIterId].bestOffer_yield = Math.round(Double.parseDouble(tmpYa.substring(split+1)) * TipConstants.fourDecimalRoundOff) / TipConstants.fourDecimalRoundOff;
							                                	tmpShareUpdFlag = 1;
							                                }
												    }
												    
												    if (tmpShareUpdFlag == 1) {							                           
							                            if (! TIPDataObjects.tipOTCBond[tmpIterId].isUpdated) {
							                            	TIPDataObjects.tipOTCBond[tmpIterId].isUpdated = true;
							                            	TIPDataObjects.totOTCBondUpdated++;
							                            }
							                        }
												    
												    TIPDataObjects.tipOTCBond[tmpIterId].sequence_no = sequence_no;
												   // logger.info("OBk Bond  TIPDataObjects.tipBond[tmpIterId] : Shares "+tmpIterId +" Symbol: "+TIPDataObjects.tipBond[tmpIterId].fk_fi_symbol+" TIPDataObjects.tipBond[tmpIterId].bid_price : "+TIPDataObjects.tipBond[tmpIterId].bid_price+" TIPDataObjects.tipBond[tmpIterId].bid_quantity  :"+TIPDataObjects.tipBond[tmpIterId].bid_quantity+" TIPDataObjects.tipBond[tmpIterId].ask_price :"+TIPDataObjects.tipSMEShare[tmpIterId].ask_price +" TIPDataObjects.tipBond[tmpIterId].ask_quantity: "+TIPDataObjects.tipBond[tmpIterId].ask_quantity +"  TIPDataObjects.totShareUpdated:"+TIPDataObjects.totBondUpdated   );
												break;
												
												
												default:
													break;
										}
									 
							  	}
					
						  }//End of Try
						  catch(Exception e)
						  {
							  logger.error("Error Occured in OBk Processing", e);
						  }
						//logger.info("DataParserOrderBook  Statistics : Total  No of Order Book Messages Parsed : "+totalOrderBookMessages);
					}//End of Inner While
					Utility.sleepAnalyser(TipConstants.DATA_ANALYZER_SLEEP_INTERVAL_OBK);
				}////End of InnerWhile
		}//Function run End
		
		
	
	
	
}
