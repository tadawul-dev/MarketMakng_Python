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
 * File Name	: Log4jWrapper.java
 *
 * 
 * Version		Date				Author					Change Description
 * ---------------------------------------------------------------------------------------------
 * 1.0			Nov 20, 2016		Mohanram	Initial version
 * ---------------------------------------------------------------------------------------------
 */
package sa.com.tadawul.website.tip.log;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author vinoth.arulvizhi
 * 
 */
public class Log4jWrapper implements Log {

	private Logger log;

	public Log4jWrapper(final Class<?> clazz) {
		log = LogManager.getLogger(clazz);
	}

	public Log4jWrapper() {
		log = LogManager.getLogger();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see sa.com.tadawul.log.Log#serviceEntry(java.lang.String)
	 */
	@Override
	public void serviceEntry(final String serviceName) {
		serviceEntry(serviceName, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see sa.com.tadawul.log.Log#serviceEntry(java.lang.String, java.lang.Object)
	 */
	@Override
	public void serviceEntry(final String serviceName, final Object input) {
		if (log.isInfoEnabled()) {
			log.info("Service " + serviceName + " - Entry");
		}

		if (log.isDebugEnabled() && input != null) {
			log.debug("Service Input Data: ");
			log.debug(input);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see sa.com.tadawul.log.Log#serviceExit(java.lang.String)
	 */
	@Override
	public void serviceExit(final String serviceName) {
		serviceExit(serviceName, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see sa.com.tadawul.log.Log#serviceExit(java.lang.String, java.lang.Object)
	 */
	@Override
	public void serviceExit(final String serviceName, final Object output) {
		if (log.isInfoEnabled()) {
			log.info("Service " + serviceName + " - Exit");
		}

		if (log.isDebugEnabled() && output != null) {
			log.debug("Service Output Data: ");
			log.debug(output);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see sa.com.tadawul.log.Log#methodEntry(java.lang.String)
	 */
	@Override
	public void methodEntry(final String methodName) {
		methodEntry(methodName, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see sa.com.tadawul.log.Log#methodEntry(java.lang.String, java.lang.Object)
	 */
	@Override
	public void methodEntry(final String methodName, final Object input) {
		if (log.isInfoEnabled()) {
			log.info("Method " + methodName + "() - Entry");
		}

		if (log.isDebugEnabled() && input != null) {
			log.debug("Method Input Data: ");
			log.debug(input);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see sa.com.tadawul.log.Log#methodExit(java.lang.String)
	 */
	@Override
	public void methodExit(final String methodName) {
		methodExit(methodName, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see sa.com.tadawul.log.Log#methodExit(java.lang.String, java.lang.Object)
	 */
	@Override
	public void methodExit(final String methodName, final Object output) {
		if (log.isInfoEnabled()) {
			log.info("Method " + methodName + "() - Exit");
		}

		if (log.isDebugEnabled() && output != null) {
			log.debug("Method Output Data: ");
			log.debug(output);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see sa.com.tadawul.log.Log#fatal(java.lang.String)
	 */
	@Override
	public void fatal(final String message) {
		log.fatal(message);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see sa.com.tadawul.log.Log#fatal(java.lang.String, java.lang.Throwable)
	 */
	@Override
	public void fatal(final String message, final Throwable error) {
		log.fatal(message, error);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see sa.com.tadawul.log.Log#error(java.lang.String)
	 */
	@Override
	public void error(final String message) {
		log.error(message);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see sa.com.tadawul.log.Log#error(java.lang.String, java.lang.Throwable)
	 */
	@Override
	public void error(final String message, final Throwable error) {
		log.error(message, error);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see sa.com.tadawul.log.Log#info(java.lang.String)
	 */
	@Override
	public void info(final String message) {
		if (log.isInfoEnabled()) {
			log.info(message);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see sa.com.tadawul.log.Log#info(java.lang.Object)
	 */
	@Override
	public void info(final Object message) {
		if (log.isInfoEnabled()) {
			log.info(message);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see sa.com.tadawul.log.Log#debug(java.lang.String)
	 */
	@Override
	public void debug(final String message) {
		if (log.isDebugEnabled()) {
			log.debug(message);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see sa.com.tadawul.log.Log#debug(java.lang.Object)
	 */
	@Override
	public void debug(final Object message) {
		if (log.isDebugEnabled()) {
			log.debug(message);
		}
	}

}
