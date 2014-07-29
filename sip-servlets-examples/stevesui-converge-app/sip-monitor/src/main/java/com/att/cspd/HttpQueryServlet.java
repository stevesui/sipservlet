package com.att.cspd;


import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.sip.ConvergedHttpSession;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.URI;

import org.apache.log4j.Logger;

//import com.att.cspd.utils.RabbitMQCallable;
//import com.att.cspd.utils.RabbitMQFutureTask;





import com.att.cspd.utils.RabbitMQCallable;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Properties;

public class HttpQueryServlet extends HttpServlet implements Servlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	//RabbitMQFutureTask futureTask = null;

	public HttpQueryServlet() {}

	private static Logger logger = Logger.getLogger(HttpQueryServlet.class);
	private SipFactory sipFactory;	

	@Override
	public void init(ServletConfig config) throws ServletException {		
		super.init(config);
		logger.info("the SimpleWebServlet has been started");
		try { 			
			// Getting the Sip factory from the JNDI Context
			Properties jndiProps = new Properties();			
			Context initCtx = new InitialContext(jndiProps);
			Context envCtx = (Context) initCtx.lookup("java:comp/env");
			sipFactory = (SipFactory) envCtx.lookup("sip/com.att.cspd.SimpleSipServlet/SipFactory");
			logger.info("Sip Factory ref from JNDI : " + sipFactory);
		} catch (NamingException e) {
			throw new ServletException("Uh oh -- JNDI problem !", e);
		}

		//init RabbitMQFutureTask threadpool
		//futureTask = new RabbitMQFutureTask();

	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException
			{
		// Create app session and request
		SipApplicationSession appSession = 
				((ConvergedHttpSession)request.getSession()).getApplicationSession();

		javax.servlet.sip.Address to = sipFactory.createAddress("sip:sip-monitor@localhost:5060");
		javax.servlet.sip.Address from = sipFactory.createAddress("sip:sip-monitor@localhost:5060");
		javax.servlet.sip.SipServletRequest notifyReq = sipFactory.createRequest(appSession, "NOTIFY", from, to);
		//javax.servlet.sip.SipSession sess = invite.getSession(true);
		//sess.setHandler(ÒsipClickToDial");
		//invite.setContent(content, contentType);
		notifyReq.setHeader("Subscription-State", "terminated");
		notifyReq.setHeader("Event", "message-summary");
		notifyReq.send();

		String result = null;
		String checkRabbitMQ = null;
		checkRabbitMQ = request.getParameter("rabbitmq");
		if(checkRabbitMQ== null) {

			response.setStatus(HttpServletResponse.SC_OK);
			PrintWriter out = response.getWriter();
			out.println("<html>");
			out.println("<head><title>SIP Monitor</title></head>");
			out.println("<body>SIP Monitor only send out NOTIFY and did not check RabbitMQ</body>");
			out.println("</html>");
			out.close();
			return;
		}
		else
		{
			/*
			try {
				Thread.sleep(500);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			*/

			//String result = null;

			RabbitMQCallable callable = new RabbitMQCallable(100);
			try {
				result = callable.call();
			}catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		/*
    	//query RabbitMQ status
    	try {
			result = futureTask.queryRabbitMQ(1000);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		 */


		if (result.equalsIgnoreCase("success"))
		{
			response.setStatus(HttpServletResponse.SC_OK);
			PrintWriter out = response.getWriter();
			out.println("<html>");
			out.println("<head><title>SIP Monitor</title></head>");
			out.println("<body>SIP Monitor checked the RabbitMQ returns " +result+"</body>");
			out.println("</html>");
			out.close();
		}
		else
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			}
}