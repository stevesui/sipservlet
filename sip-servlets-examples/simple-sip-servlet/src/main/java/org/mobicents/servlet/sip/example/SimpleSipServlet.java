/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.mobicents.servlet.sip.example;

import java.io.IOException;
import java.util.NoSuchElementException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.sip.ServletTimer;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipServlet;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.SipURI;
import javax.servlet.sip.TimerListener;
import javax.servlet.sip.TimerService;
import javax.servlet.sip.SipSession.State;


import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.log4j.Logger;
import org.mobicents.servlet.sip.example.utils.ConnectionPoolableObjectFactory;

import com.rabbitmq.client.Channel;

public class SimpleSipServlet extends SipServlet implements TimerListener {
	private static Logger logger = Logger.getLogger(SimpleSipServlet.class);
	private static final long serialVersionUID = 1L;
	private static final String CALLEE_SEND_BYE = "YouSendBye";
	//60 sec
	private static final int DEFAULT_BYE_DELAY = 60000;
	
	private int byeDelay = DEFAULT_BYE_DELAY;
	private static GenericObjectPool<Channel> pool;
	private static int counter = 0;

	private String EXCHANGE_NAME;
	private String RABBITMQ_CONN_URL;
	private int POOL_MAX_ACTIVE;
	private int POOL_MAX_IDLE;
	private int POOL_MIN_IDLE;
	private int TIME_BETWEEN_EVICTION;
	private int IDL_TIME_BEFORE_EVICTION;

	/** Creates a new instance of SimpleProxyServlet */
	public SimpleSipServlet() {
	}

	
	@Override
	public void init(ServletConfig servletConfig) throws ServletException {
		logger.info("the simple sip servlet has been started");
		super.init(servletConfig);

		RABBITMQ_CONN_URL = servletConfig.getInitParameter("rabbitmq_conn_url");
		EXCHANGE_NAME = servletConfig.getInitParameter("exchange_name");
		String pool_max_active = servletConfig.getInitParameter("rabbitmq_pool_max_active");
		String pool_max_idle = servletConfig.getInitParameter("rabbitmq_pool_max_idle");
		String pool_min_idle = servletConfig.getInitParameter("rabbitmq_pool_min_idle");
		String time_between_evication = servletConfig.getInitParameter("rabbitmq_pool_time_between_eviction");
		String idle_time_before_eviction = servletConfig.getInitParameter("rabbitmq_pool_idle_time_before_eviction");


		logger.info("INIT PARAM:  rabbitmq_conn_url = "+RABBITMQ_CONN_URL);
		logger.info("INIT PARAM : exchange name  = "+EXCHANGE_NAME);
		logger.info("INIT PARAM : pool max active = "+pool_max_active);
		logger.info("INIT PARAM : pool max idle  = "+pool_max_idle);
		logger.info("INIT PARAM : pool min idle = "+pool_min_idle);
		logger.info("INIT PARAM : time_between_evication  = "+time_between_evication);
		logger.info("INIT PARAM : idle_time_before_eviction  = "+idle_time_before_eviction);


		try{
			POOL_MAX_ACTIVE = Integer.parseInt(pool_max_active);
		} catch (NumberFormatException e) {
			logger.error("Impossible to parse the pool max active : " + pool_max_active, e);
		}

		try{
			POOL_MAX_IDLE = Integer.parseInt(pool_max_idle);
		} catch (NumberFormatException e) {
			logger.error("Impossible to parse the pool max idle : " + pool_max_idle, e);
		}

		try{
			POOL_MIN_IDLE = Integer.parseInt(pool_min_idle);
		} catch (NumberFormatException e) {
			logger.error("Impossible to parse the pool min idle  : " + pool_min_idle, e);
		}

		try{
			TIME_BETWEEN_EVICTION = Integer.parseInt(time_between_evication);
		} catch (NumberFormatException e) {
			logger.error("Impossible to parse the time between eviction : " + time_between_evication, e);
		}
		try{
			IDL_TIME_BEFORE_EVICTION = Integer.parseInt(idle_time_before_eviction);
		} catch (NumberFormatException e) {
			logger.error("Impossible to parse idle time before eviction : " + idle_time_before_eviction, e);
		}

		/**
		 * create static instance of rabbitmq connection pool
		 */
		try {
			
			GenericObjectPool.Config config = new GenericObjectPool.Config();
			config.maxActive=POOL_MAX_ACTIVE;
			config.maxIdle = POOL_MAX_IDLE;
			config.minIdle = POOL_MIN_IDLE;
			config.timeBetweenEvictionRunsMillis=TIME_BETWEEN_EVICTION;
			config.minEvictableIdleTimeMillis=IDL_TIME_BEFORE_EVICTION;
			config.testOnBorrow=false;
			config.testOnReturn=false;
			config.lifo=GenericObjectPool.DEFAULT_LIFO;
			config.whenExhaustedAction=GenericObjectPool.WHEN_EXHAUSTED_FAIL;
			
			pool = new GenericObjectPool<Channel>(new ConnectionPoolableObjectFactory(RABBITMQ_CONN_URL),config);
			
			//create an initial pool instances.
			/*
			int initSize = 25;
			for (int i =0; i < initSize; i++) {
				try {
					pool.addObject();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			*/
			
			/*
			pool.setMaxActive(POOL_MAX_ACTIVE);   //maximum connection allowed in the pool (100). If reached, worker thread needs to be blocked to wait.
			pool.setMinIdle(POOL_MIN_IDLE);       //keep minimum idle connection (set to 3) in pool in all time.
			pool.setMaxIdle(POOL_MAX_IDLE);      //No minimum to create new connection when needed (set to -1).
			pool.setTimeBetweenEvictionRunsMillis(TIME_BETWEEN_EVICTION);   //wait up eviction thread in 10 second
			pool.setMinEvictableIdleTimeMillis(IDL_TIME_BEFORE_EVICTION);  //kill the idle connection that is 5 second old
			pool.setTestOnBorrow(true);   //sanity checking when getting connection from pool.
			pool.setTestOnReturn(true);   //sanity check when returning connection to pool.
			*/

		}
		catch (IOException ex) {

			logger.error("RabbitMQ Pool failed to create. Error = "+ex.getMessage());
			throw new ServletException(ex);
		}

		//logger.info("HELLO... FINISHED LOADING THE RABBITMQ CONNECTION/CHANNEL POOL");

	}

	@Override
	/**
	 * invoked when servlet is shut down. Clean up connections within the pool.
	 */
	public void destroy() {
		pool.clear();
		try {
			pool.close();
		} catch (Exception e) {

			e.printStackTrace();
			logger.error("SIP servlet cannot close the pool properly. Error : "+e.getMessage());
		}
		logger.info("Cleared and closed the pool");

	}
	
	/**
	 * Make the static counter variable thread-safe, assuming notify is executed by different threads.
	 * 
	 * @return
	 */
	private synchronized String getBindingKey() {

		String routingKey = "";
		if(counter % 2 == 0)
			routingKey = "federation-destination";
		else
			routingKey = "upstream-destination";
		counter++;
		return routingKey;

	}

	/**
	 * {@inheritDoc}
	 */
	protected void doNotify(SipServletRequest request) throws ServletException,
	IOException {

		Channel channel= null;
		String routingKey = "";

		//a trick to change routingKey value.
		//routingKey = getBindingKey();

		try {

			channel = pool.borrowObject();
			String message = request.getCallId();
		

			channel.exchangeDeclare(EXCHANGE_NAME, "topic",true);
			channel.basicPublish(EXCHANGE_NAME, routingKey, null, message.getBytes());
			logger.info("PUBLISH A MESSAGE : "+message);

			logger.info("*************************************");
			logger.info("**"+ "Number active : " +pool.getNumActive()+" ***");
			logger.info("**"+ "Number idle  : " +pool.getNumIdle()+" ***");
			logger.info("*************************************");
			
			

		} catch (NoSuchElementException e) {

			logger.error(e.getMessage());
			throw new ServletException(e);

		} catch (IllegalStateException e) {

			logger.error(e.getMessage());
			throw new ServletException(e);
		} catch (Exception e) {

			logger.error(e.getMessage());
			throw new ServletException(e);
		} 
		finally {
			if (channel != null) {
				try {
					pool.returnObject(channel);
				} catch (Exception e) {
					e.printStackTrace();
					logger.error("Failed to return channel back to pool. Exception message: "+e.getMessage());
				}
				//logger.info("RETURN CHANNEL TO THE POOL");
			}

		}
		SipServletResponse sipServletResponse = request.createResponse(SipServletResponse.SC_OK);
		sipServletResponse.send();
		
	}

	
	/**
	 * {@inheritDoc}
	 */
	protected void doInvite(SipServletRequest request) throws ServletException,
			IOException {
		
		if(logger.isInfoEnabled()) {
			logger.info("Simple Servlet: Got request:\n"
				+ request.getMethod());
		}
		SipServletResponse sipServletResponse = request.createResponse(SipServletResponse.SC_RINGING);
		sipServletResponse.send();
		sipServletResponse = request.createResponse(SipServletResponse.SC_OK);
		sipServletResponse.send();
		if(CALLEE_SEND_BYE.equalsIgnoreCase(((SipURI)request.getTo().getURI()).getUser())) {
			TimerService timer = (TimerService) getServletContext().getAttribute(TIMER_SERVICE);			
			timer.createTimer(request.getApplicationSession(), byeDelay, false, request.getSession().getId());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	protected void doBye(SipServletRequest request) throws ServletException,
			IOException {
		if(logger.isInfoEnabled()) {
			logger.info("SimpleProxyServlet: Got BYE request:\n" + request);
		}
		SipServletResponse sipServletResponse = request.createResponse(200);
		sipServletResponse.send();
		SipApplicationSession sipApplicationSession = sipServletResponse.getApplicationSession(false);
		if(sipApplicationSession != null && sipApplicationSession.isValid()) {
			sipApplicationSession.invalidate();
		}		
	}

	/**
	 * {@inheritDoc}
	 */
	protected void doResponse(SipServletResponse response)
			throws ServletException, IOException {
		if(logger.isInfoEnabled()) {
			logger.info("SimpleProxyServlet: Got response:\n" + response);
		}
		if(SipServletResponse.SC_OK == response.getStatus() && "BYE".equalsIgnoreCase(response.getMethod())) {
			SipApplicationSession sipApplicationSession = response.getApplicationSession(false);
			if(sipApplicationSession != null && sipApplicationSession.isValid()) {
				sipApplicationSession.invalidate();
			}			
		}
	}

	/*
	 * (non-Javadoc)
	 * @see javax.servlet.sip.TimerListener#timeout(javax.servlet.sip.ServletTimer)
	 */
	public void timeout(ServletTimer servletTimer) {
		SipSession sipSession = servletTimer.getApplicationSession().getSipSession((String)servletTimer.getInfo());
		if(!State.TERMINATED.equals(sipSession.getState())) {
			try {
				sipSession.createRequest("BYE").send();
			} catch (IOException e) {
				logger.error("An unexpected exception occured while sending the BYE", e);
			}				
		}
	}
}