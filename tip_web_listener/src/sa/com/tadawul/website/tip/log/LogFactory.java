/**
 * ---------------------------------------------------------------------------------------------
 * (c) Copyright 2016 Tadawul - All Rights Reserved
 *
 *		http://www.tadawul.com.sa
 *
 * This source is property of Tadawul, Saudi Stock Exchange.
 * No part of this source or any copyrights subsisting or arising out of these materials may be
 * reproduced, distributed, amended, modified, adapted, stored in a retrieval system, adopted
 * or transmitted in any form or by any means, electronic, mechanical, photographic, graphic,
 * optic recording or otherwise, translated in any language or computer language, without the
 * prior written permission from Tadawul, Saudi Stock Exchange
 *
 * ---------------------------------------------------------------------------------------------
 *
 * File Name	: LogFactory.java
 *
 * 
 * Version		Date				Author					Change Description
 * ---------------------------------------------------------------------------------------------
 * 1.0			Nov 20, 2016		Mohanram	Initial version
 * ---------------------------------------------------------------------------------------------
 */
package sa.com.tadawul.website.tip.log;

/**
 * @author Mohanram
 * 
 */
public final class LogFactory {

	private static final LogFactory INSTANCE = new LogFactory();

	private LogFactory() {

	}

	public static LogFactory getInstance() {
		return INSTANCE;
	}

	public static Log getLog(final Class<?> clazz) {
		return new Log4jWrapper(clazz);
	}

	
	
}
