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
 * Name          :     FileName : TIP_IndexVO.java
 *
 * Version             Date                     Author                  Change Description
 *----------------------------------------------------------------------------------------------
 * 1.0                 10/April/2009            Oracle                   Initial Version
 * 1.1				   14/November/2009         Mohanram,Oracle          Added functionalities to process the Equity Rights Share
 * 1.2				   02/February/2010         Mohanram,Oracle          Added functionalities to process the Exchange Traded Funds.	
 *----------------------------------------------------------------------------------------------
 */
package sa.com.tadawul.website.tip.vo;

import java.sql.Timestamp;

import sa.com.tadawul.website.tip.vo.TIP_BaseVO;

/**
 *@description This class contains attributes to get and set values in to Index Tables.
 * @ version 1.0
 */

public class TIP_BondIndexVO extends TIP_BaseVO{
    
    /** Index Id*/
    public long IndexId;
    /** ListId*/
    public long ListId;
    /** Description_in ENGLISH*/
    public String Description_EN;
    /** Description_in ARABIC*/
    public String Description_AR;
    /** Index Price*/
    public double index_price;
    /** Volume Traded*/
    public long volume_trade;
    /** Turn over*/
    public double turnover;
    /** No of Trades*/
    public int no_of_trades;
    /** Net Change*/
    public double net_change;
    /** Percent Change*/
    public double percent_change;
    /**High Price*/
    public double high_price;
    /** Low price*/
    public double low_price;
    /** Open price*/
    public double open_price;
    /** Previous Index Price*/
    public double previous_index_price;
    /** ISymbol*/
    public String Fk_te_symbol;
    /** ITransaction time */
    public Timestamp trans_time;
    
    /**
     * This constructor is called to initialize  Index Table related attributes
     */
    public  TIP_BondIndexVO(){
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
        
        IndexId = 0;
        ListId = 0;
        Description_EN = "";
        Description_AR = "";
        index_price = 0;
        volume_trade = 0;
        turnover = 0;
        no_of_trades = 0;
        net_change = 0;
        percent_change = 0;
        high_price = 0;
        low_price = 0;
        open_price = 0;
        previous_index_price = 0;
        Fk_te_symbol = "";
        trans_time = new Timestamp(System.currentTimeMillis());
    }
    /**
     * This constructor is called to set values related to Index Table.
     * @param tmpTipIndex contains index Table related attributes
     */
    public  TIP_BondIndexVO(TIP_BondIndexVO tmpTipIndex){
        frameNo = tmpTipIndex.frameNo;
        analyserStartTime = tmpTipIndex.analyserStartTime;
        analyserEndTime = tmpTipIndex.analyserEndTime;
        readerStartTime = tmpTipIndex.readerStartTime;
        readerEndTime = tmpTipIndex.readerEndTime;
        sequence_no = tmpTipIndex.sequence_no;
        isBOD = tmpTipIndex.isBOD;
        isEOD = tmpTipIndex.isEOD;
        isUpdated = tmpTipIndex.isUpdated;
        isEndOfBatch = tmpTipIndex.isEndOfBatch;
        
        IndexId = tmpTipIndex.IndexId;
        ListId = tmpTipIndex.ListId;
        Description_EN = tmpTipIndex.Description_EN;
        Description_AR = tmpTipIndex.Description_AR;
        index_price = tmpTipIndex.index_price;
        volume_trade = tmpTipIndex.volume_trade;
        turnover = tmpTipIndex.turnover;
        no_of_trades = tmpTipIndex.no_of_trades;
        net_change = tmpTipIndex.net_change;
        percent_change = tmpTipIndex.percent_change;
        high_price = tmpTipIndex.high_price;
        low_price = tmpTipIndex.low_price;
        open_price = tmpTipIndex.open_price;
        previous_index_price = tmpTipIndex.previous_index_price;
        Fk_te_symbol = tmpTipIndex.Fk_te_symbol;
        trans_time = tmpTipIndex.trans_time;
    }
    
}
