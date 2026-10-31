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
 * Name          :     FileName : MessageList.java
 *
 * Version             Date                     Author                  Change Description
 *----------------------------------------------------------------------------------------------
 * 1.0                 15 - May 2016           Mohanram                  Initial Version
 *----------------------------------------------------------------------------------------------
 */
package sa.com.tadawul.website.tip.queue;


import com.objectspace.jgl.Queue;

/**
*  This class is used keep different queue of messages.
*  The queue of messages is passed from DataReader to corresponding data Writer 
*  with the help of object of this class.
*  
* 
*/

public class MessageQueue {

public  volatile static Queue turnoverListUpdateQueue = new Queue();
public  volatile static Queue indexUpdateQueue = new Queue();
public  volatile static Queue tradeQueue = new Queue();
public  volatile static Queue tradeStatisticsQueue = new Queue();
public  volatile static Queue tRdAndTStQueue = new Queue();
public  volatile static Queue orderBookQueue = new Queue();
public  volatile static Queue orderBookStateQueue = new Queue();
public  volatile static Queue indicativeOpeningQueue = new Queue();
public  volatile static Queue lowVlmMsgQueue = new Queue();
public boolean isEndOfFile = false;

public volatile static AnalyserInBuffer g_oBuffer= new AnalyserInBuffer();



}
