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

package org.mobicents.servlet.sip.testsuite.reinvite;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.sip.SipProvider;
import javax.sip.header.Header;
import javax.sip.header.RecordRouteHeader;
import javax.sip.message.Response;

import org.apache.catalina.deploy.ApplicationParameter;
import org.apache.log4j.Logger;
import org.mobicents.servlet.sip.SipServletTestCase;
import org.mobicents.servlet.sip.catalina.SipStandardManager;
import org.mobicents.servlet.sip.startup.SipContextConfig;
import org.mobicents.servlet.sip.startup.SipStandardContext;
import org.mobicents.servlet.sip.testsuite.ProtocolObjects;
import org.mobicents.servlet.sip.testsuite.TestSipListener;

public class UACReInviteSipServletTest extends SipServletTestCase {
	private static transient Logger logger = Logger.getLogger(UACReInviteSipServletTest.class);		
	private static final String TRANSPORT = "udp";
	private static final boolean AUTODIALOG = true;
	private static final int TIMEOUT = 10000;	
//	private static final int TIMEOUT = 100000000;
	
	TestSipListener receiver;
	
	ProtocolObjects receiverProtocolObjects;
	
	public UACReInviteSipServletTest(String name) {
		super(name);
		startTomcatOnStartup = false;
		autoDeployOnStartup = false;
	}

	@Override
	public void deployApplication() {
		SipStandardContext context = new SipStandardContext();
		context.setDocBase(projectHome + "/sip-servlets-test-suite/applications/shootist-sip-servlet/src/main/sipapp");
		context.setName("sip-test-context");
		context.setPath("sip-test");
		context.addLifecycleListener(new SipContextConfig());
		context.setManager(new SipStandardManager());
		ApplicationParameter applicationParameter = new ApplicationParameter();
		applicationParameter.setName("username");
		applicationParameter.setValue("reinvite");
		context.addApplicationParameter(applicationParameter);
		assertTrue(tomcat.deployContext(context));
	}
	
	public SipStandardContext deployApplication(Map<String, String> params) {
		SipStandardContext context = new SipStandardContext();
		context.setDocBase(projectHome + "/sip-servlets-test-suite/applications/shootist-sip-servlet/src/main/sipapp");
		context.setName("sip-test-context");
		context.setPath("sip-test");
		context.addLifecycleListener(new SipContextConfig());
		context.setManager(new SipStandardManager());
		for (Entry<String, String> param : params.entrySet()) {
			ApplicationParameter applicationParameter = new ApplicationParameter();
			applicationParameter.setName(param.getKey());
			applicationParameter.setValue(param.getValue());
			context.addApplicationParameter(applicationParameter);
		}
		assertTrue(tomcat.deployContext(context));
		return context;
	}

	@Override
	protected String getDarConfigurationFile() {
		return "file:///" + projectHome + "/sip-servlets-test-suite/testsuite/src/test/resources/" +
				"org/mobicents/servlet/sip/testsuite/simple/shootist-sip-servlet-dar.properties";
	}
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();												
	}
	
	public void testShootistReInvite() throws Exception {
//		receiver.sendInvite();
		receiverProtocolObjects =new ProtocolObjects(
				"sender", "gov.nist", TRANSPORT, AUTODIALOG, null, null, null);
					
		receiver = new TestSipListener(5080, 5070, receiverProtocolObjects, false);
		SipProvider senderProvider = receiver.createProvider();			
		
		senderProvider.addSipListener(receiver);
		
		receiverProtocolObjects.start();
		tomcat.startTomcat();
		deployApplication();
		Thread.sleep(TIMEOUT);
		assertTrue(receiver.getByeReceived());		
	}	
	
	/**
	 * Non Regression test for 
	 * http://code.google.com/p/mobicents/issues/detail?id=2066
	 * Miss Record-Route in Response
	 */
	public void testShootistReInviteRecordRouteHeaders() throws Exception {
//		receiver.sendInvite();
		receiverProtocolObjects =new ProtocolObjects(
				"recordrouteeinvite", "gov.nist", TRANSPORT, AUTODIALOG, null, null, null);
					
		receiver = new TestSipListener(5080, 5070, receiverProtocolObjects, false);
		receiver.setRecordRoutingProxyTesting(true);
		SipProvider senderProvider = receiver.createProvider();			
		
		senderProvider.addSipListener(receiver);
		
		receiverProtocolObjects.start();
		tomcat.startTomcat();
		Map<String, String> params = new HashMap<String, String>();
		params.put("username", "recordrouteeinvite");
		params.put("timeToWaitForBye", "15000");
		deployApplication(params);
		Thread.sleep(TIMEOUT);
		assertTrue(receiver.isAckReceived());
		
		List<Header> headers = new ArrayList<Header>();
		headers.add(receiverProtocolObjects.headerFactory.createHeader(RecordRouteHeader.NAME, "sip:" + System.getProperty("org.mobicents.testsuite.testhostaddr") + ":5080"));
		receiver.sendInDialogSipRequest("INVITE", null, null, null, headers, null);
		Thread.sleep(TIMEOUT);
		
		Response reinviteOKResponse = receiver.getInviteOkResponse();
		ListIterator<Header> recordRouteHeaders = reinviteOKResponse.getHeaders(RecordRouteHeader.NAME);
		assertTrue(recordRouteHeaders.hasNext());
		
		assertTrue(receiver.getByeReceived());		
	}
	
	/**
	 * Non Regression test for 
	 * http://code.google.com/p/mobicents/issues/detail?id=2066
	 * Miss Record-Route in Response
	 */
	public void testShootistReInviteNonRecordRouteHeaders() throws Exception {
//		receiver.sendInvite();
		receiverProtocolObjects =new ProtocolObjects(
				"recordrouteeinvite", "gov.nist", TRANSPORT, AUTODIALOG, null, null, null);
					
		receiver = new TestSipListener(5080, 5070, receiverProtocolObjects, false);
		receiver.setRecordRoutingProxyTesting(true);
		SipProvider senderProvider = receiver.createProvider();			
		
		senderProvider.addSipListener(receiver);
		
		receiverProtocolObjects.start();
		tomcat.startTomcat();
		Map<String, String> params = new HashMap<String, String>();
		params.put("username", "nonrecordrouteeinvite");
		params.put("timeToWaitForBye", "15000");
		deployApplication(params);
		Thread.sleep(TIMEOUT);
		assertTrue(receiver.isAckReceived());
		
		List<Header> headers = new ArrayList<Header>();
		headers.add(receiverProtocolObjects.headerFactory.createHeader(RecordRouteHeader.NAME, "sip:" + System.getProperty("org.mobicents.testsuite.testhostaddr") + ":5080"));
		receiver.sendInDialogSipRequest("INVITE", null, null, null, headers, null);
		Thread.sleep(TIMEOUT);
		
		Response reinviteOKResponse = receiver.getInviteOkResponse();
		ListIterator<Header> recordRouteHeaders = reinviteOKResponse.getHeaders(RecordRouteHeader.NAME);
		assertFalse(recordRouteHeaders.hasNext());
		
		assertTrue(receiver.getByeReceived());		
	}

	@Override
	protected void tearDown() throws Exception {					
		receiverProtocolObjects.destroy();			
		logger.info("Test completed");
		super.tearDown();
	}
}