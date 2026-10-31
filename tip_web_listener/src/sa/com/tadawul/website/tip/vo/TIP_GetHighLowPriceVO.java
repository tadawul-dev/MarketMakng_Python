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
 * Name          :     FileName : TIP_GetHighLowPriceVO.java
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


/**
 * @description This class contains attributes to get and set Bond related information.
 * @ version 1.0
 */

public class TIP_GetHighLowPriceVO {
	
	/**High52weeks price*/
	public double high_52_weeks_price;
	/**Low52weeks price*/
	public double low_52_weeks_price;
	/**High52weeks Date*/
	public Date high_52_weeks_date;
	/**Low52weeks Date*/
	public Date low_52_weeks_date;
	
	/**
	 * This constructor is called to initialize all Bond related attributes
	 */
	public  TIP_GetHighLowPriceVO(){
		high_52_weeks_price=0;
		low_52_weeks_price=0;
		high_52_weeks_date=new Date(System.currentTimeMillis());
		low_52_weeks_date=new Date(System.currentTimeMillis());
	}
}
