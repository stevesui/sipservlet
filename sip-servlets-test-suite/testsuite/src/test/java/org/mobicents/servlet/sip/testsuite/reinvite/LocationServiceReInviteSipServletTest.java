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

import java.text.ParseException;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import javax.sip.SipProvider;
import javax.sip.address.SipURI;

import org.apache.log4j.Logger;
import org.mobicents.servlet.sip.SipServletTestCase;
import org.mobicents.servlet.sip.testsuite.ProtocolObjects;
import org.mobicents.servlet.sip.testsuite.TestSipListener;

/**
 * Testing of RFC3665-3.7 -- Session with re-INVITE
 * 
 * @author <A HREF="mailto:jean.deruelle@gmail.com">Jean Deruelle</A> 
 *
 */
public class LocationServiceReInviteSipServletTest extends SipServletTestCase {
	
	private static transient Logger logger = Logger.getLogger(LocationServiceReInviteSipServletTest.class);

	private static final String TRANSPORT = "udp";
	private static final boolean AUTODIALOG = true;
	private static final int TIMEOUT = 20000;	
//	private static final int TIMEOUT = 100000000;
	
	TestSipListener sender;
	TestSipListener receiver;
	ProtocolObjects senderProtocolObjects;
	ProtocolObjects	receiverProtocolObjects;

	
	public LocationServiceReInviteSipServletTest(String name) {
		super(name);
//		createTomcatOnStartup = false;
	}

	@Override
	public void deployApplication() {
		assertTrue(tomcat.deployContext(
				projectHome + "/sip-servlets-test-suite/applications/location-service-servlet/src/main/sipapp",
				"location-service-context", 
				"location-service"));
	}

	@Override
	protected String getDarConfigurationFile() {
		return "file:///" + projectHome + "/sip-servlets-test-suite/testsuite/src/test/resources/" +
				"org/mobicents/servlet/sip/testsuite/reinvite/locationservice-dar.properties";
	}
	
	@Override
	protected void setUp() throws Exception {				
		super.setUp();						
		
		senderProtocolObjects =new ProtocolObjects(
				"reinvite", "gov.nist", TRANSPORT, AUTODIALOG, null, null, null);		
		receiverProtocolObjects = new ProtocolObjects("receiver",
				"gov.nist", TRANSPORT, AUTODIALOG, null, null, null);		
					
		sender = new TestSipListener(5080, 5070, senderProtocolObjects, false);
		sender.setRecordRoutingProxyTesting(true); 
		SipProvider senderProvider = sender.createProvider();					
		senderProvider.addSipListener(sender);		
		senderProtocolObjects.start();			
		
		receiver = new TestSipListener(5090, 5070, receiverProtocolObjects, false);
		receiver.setRecordRoutingProxyTesting(true);
		SipProvider receiverProvider = receiver.createProvider();
		receiverProvider.addSipListener(receiver);
		receiverProtocolObjects.start();
	}
	
	public void testReInvite() throws InterruptedException, SipException, ParseException, InvalidArgumentException {
		String fromName = "reinvite";
		String fromSipAddress = "sip-servlets.com";
		SipURI fromAddress = senderProtocolObjects.addressFactory.createSipURI(
				fromName, fromSipAddress);
				
		String toUser = "receiver-failover";
		String toSipAddress = "" + System.getProperty("org.mobicents.testsuite.testhostaddr") + ":5090";
		SipURI toAddress = senderProtocolObjects.addressFactory.createSipURI(
				toUser, toSipAddress);
		
		receiver.setSendReinvite(true);
		receiver.setWaitBeforeFinalResponse(2000);
		sender.sendSipRequest("INVITE", fromAddress, toAddress, null, null, true);		
		Thread.sleep(TIMEOUT);			
		assertTrue(sender.isInviteReceived());
		sender.sendInDialogSipRequest("BYE", null, null, null, null, null);
		Thread.sleep(TIMEOUT);
		assertTrue(receiver.getByeReceived());
		assertTrue(sender.getOkToByeReceived());
	}
	
	public void testReInviteSending() throws InterruptedException, SipException, ParseException, InvalidArgumentException {
		String fromName = "isendreinvite";
		String fromSipAddress = "sip-servlets.com";
		SipURI fromAddress = senderProtocolObjects.addressFactory.createSipURI(
				fromName, fromSipAddress);
				
		String toUser = "receiver-failover";
		String toSipAddress = "" + System.getProperty("org.mobicents.testsuite.testhostaddr") + ":5090";
		SipURI toAddress = senderProtocolObjects.addressFactory.createSipURI(
				toUser, toSipAddress);
		
		sender.setSendReinvite(true);
		sender.setSendBye(true);
		sender.sendSipRequest("INVITE", fromAddress, toAddress, null, null, true);		
		Thread.sleep(TIMEOUT);
		assertTrue(receiver.getByeReceived());
		assertTrue(sender.getOkToByeReceived());		
	}

	@Override
	protected void tearDown() throws Exception {					
		senderProtocolObjects.destroy();			
		receiverProtocolObjects.destroy();
		logger.info("Test completed");
		super.tearDown();
	}


}
