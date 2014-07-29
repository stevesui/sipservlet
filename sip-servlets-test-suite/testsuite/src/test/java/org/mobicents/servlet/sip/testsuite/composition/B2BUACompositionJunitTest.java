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

package org.mobicents.servlet.sip.testsuite.composition;

import javax.sip.ListeningPoint;
import javax.sip.SipProvider;
import javax.sip.address.SipURI;

import org.apache.log4j.Logger;
import org.mobicents.servlet.sip.SipServletTestCase;
import org.mobicents.servlet.sip.testsuite.ProtocolObjects;
import org.mobicents.servlet.sip.testsuite.TestSipListener;

public class B2BUACompositionJunitTest extends SipServletTestCase {
	
	private static final String TO_NAME = "forward-receiver";
	private static final String FROM_NAME = "composition";
	
	private static final String FROM_DOMAIN = "sip-servlets.com";
	private String TO_DOMAIN;	

	private static transient Logger logger = Logger.getLogger(B2BUACompositionJunitTest.class);

	private static final String TRANSPORT = "udp";
	private static final boolean AUTODIALOG = true;
	private static final int TIMEOUT = 10000;	
//	private static final int TIMEOUT = 100000000;
	
	TestSipListener sender;
	TestSipListener receiver;
	ProtocolObjects senderProtocolObjects;
	ProtocolObjects	receiverProtocolObjects;

	public B2BUACompositionJunitTest(String name) {
		super(name);
		autoDeployOnStartup = false;
		startTomcatOnStartup = false;
	}

	@Override
	public void deployApplication() {
		deployB2BUA();
		deployCallForwarding();
	}

	private void deployB2BUA() {
		assertTrue(tomcat.deployContext(
				projectHome + "/sip-servlets-test-suite/applications/b2bua-sip-servlet/src/main/sipapp",
				"b2bua-context", 
				"b2bua"));
	}
	
	private void deployCallForwarding() {
		assertTrue(tomcat.deployContext(
				projectHome + "/sip-servlets-test-suite/applications/call-forwarding-b2bua-servlet/src/main/sipapp",
				"call-forwarding-b2bua-context", 
				"call-forwarding-b2bua"));
	}

	@Override
	protected String getDarConfigurationFile() {
		return "file:///"
				+ projectHome
				+ "/sip-servlets-test-suite/testsuite/src/test/resources/"
				+ "org/mobicents/servlet/sip/testsuite/composition/b2bua-composition-dar.properties";
	}
	
	@Override
	protected void setUp() throws Exception {		
		super.setUp();

		TO_DOMAIN = "" + System.getProperty("org.mobicents.testsuite.testhostaddr") + ":5090";
		tomcat.addSipConnector(serverName, sipIpAddress, 5070, ListeningPoint.TCP);
		tomcat.startTomcat();		
		
		senderProtocolObjects = new ProtocolObjects(FROM_NAME,
				"gov.nist", TRANSPORT, AUTODIALOG, null, null, null);
		receiverProtocolObjects = new ProtocolObjects(TO_NAME,
				"gov.nist", TRANSPORT, AUTODIALOG, null, null, null);
			
	}
	
	public void testCallForwardingCallerSendBye() throws Exception {
		deployB2BUA();
		deployCallForwarding();
		sender = new TestSipListener(5080, 5070, senderProtocolObjects, true);		
		SipProvider senderProvider = sender.createProvider();

		receiver = new TestSipListener(5059, 5070, receiverProtocolObjects, false);	
		SipProvider receiverProvider = receiver.createProvider();

		receiverProvider.addSipListener(receiver);
		senderProvider.addSipListener(sender);

		senderProtocolObjects.start();
		receiverProtocolObjects.start();

		String fromName = FROM_NAME;
		String fromSipAddress = FROM_DOMAIN;
		SipURI fromAddress = senderProtocolObjects.addressFactory.createSipURI(
				fromName, fromSipAddress);
		
		String toSipAddress = TO_DOMAIN;
		String toUser = TO_NAME;
		SipURI toAddress = senderProtocolObjects.addressFactory.createSipURI(
				toUser, toSipAddress);
		
		sender.sendSipRequest("INVITE", fromAddress, toAddress, null, null, true);		
		Thread.sleep(TIMEOUT);
		//checking numbers of ACK received see http://forums.java.net/jive/thread.jspa?messageID=277840
		assertEquals(1,receiver.ackCount);
		assertTrue(sender.getOkToByeReceived());
		assertTrue(receiver.getByeReceived());
	}

	public void testCallForwardingCalleeSendBye() throws Exception {
		deployB2BUA();
		deployCallForwarding();
		sender = new TestSipListener(5080, 5070, senderProtocolObjects, false);
		SipProvider senderProvider = sender.createProvider();

		receiver = new TestSipListener(5059, 5070, receiverProtocolObjects, true);
		SipProvider receiverProvider = receiver.createProvider();

		receiverProvider.addSipListener(receiver);
		senderProvider.addSipListener(sender);

		senderProtocolObjects.start();
		receiverProtocolObjects.start();

		String fromName = FROM_NAME;
		String fromSipAddress = TO_DOMAIN;
		SipURI fromAddress = senderProtocolObjects.addressFactory.createSipURI(
				fromName, fromSipAddress);
		
		String toSipAddress = FROM_DOMAIN;
		String toUser = TO_NAME;
		SipURI toAddress = senderProtocolObjects.addressFactory.createSipURI(
				toUser, toSipAddress);
		
		sender.sendSipRequest("INVITE", fromAddress, toAddress, null, null, false);		
		Thread.sleep(TIMEOUT);
		assertTrue(receiver.getOkToByeReceived());
		assertTrue(sender.getByeReceived());		
	}

	public void testCancelCallForwarding() throws Exception {
		deployB2BUA();
		deployCallForwarding();
		sender = new TestSipListener(5080, 5070, senderProtocolObjects, false);
		SipProvider senderProvider = sender.createProvider();

		receiver = new TestSipListener(5059, 5070, receiverProtocolObjects, true);
		SipProvider receiverProvider = receiver.createProvider();

		receiverProvider.addSipListener(receiver);
		senderProvider.addSipListener(sender);

		senderProtocolObjects.start();
		receiverProtocolObjects.start();

		String fromName = FROM_NAME;
		String fromSipAddress = FROM_DOMAIN;
		SipURI fromAddress = senderProtocolObjects.addressFactory.createSipURI(
				fromName, fromSipAddress);
		
		String toSipAddress = TO_DOMAIN;
		String toUser = TO_NAME;
		SipURI toAddress = senderProtocolObjects.addressFactory.createSipURI(
				toUser, toSipAddress);
		
		sender.sendSipRequest("INVITE", fromAddress, toAddress, null, null, true);
		Thread.sleep(500);
		sender.sendCancel();
		Thread.sleep(TIMEOUT);
		assertTrue(sender.isCancelOkReceived());
		assertTrue(sender.isRequestTerminatedReceived());
		assertTrue(receiver.isCancelReceived());
	}
	
	@Override
	protected void tearDown() throws Exception {	
		senderProtocolObjects.destroy();
		receiverProtocolObjects.destroy();			
		logger.info("Test completed");
		super.tearDown();
	}


}
