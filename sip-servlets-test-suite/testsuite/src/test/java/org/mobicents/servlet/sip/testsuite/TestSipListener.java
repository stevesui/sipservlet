/*
 * TeleStax, Open Source Cloud Communications  Copyright 2012. 
 * and individual contributors
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

package org.mobicents.servlet.sip.testsuite;

import gov.nist.javax.sip.DialogExt;
import gov.nist.javax.sip.SipStackImpl;
import gov.nist.javax.sip.address.SipUri;
import gov.nist.javax.sip.header.HeaderExt;
import gov.nist.javax.sip.header.HeaderFactoryExt;
import gov.nist.javax.sip.header.ParameterNames;
import gov.nist.javax.sip.header.SIPETag;
import gov.nist.javax.sip.header.SIPHeaderNames;
import gov.nist.javax.sip.header.WWWAuthenticate;
import gov.nist.javax.sip.header.extensions.JoinHeader;
import gov.nist.javax.sip.header.extensions.ReplacesHeader;
import gov.nist.javax.sip.header.ims.PathHeader;
import gov.nist.javax.sip.message.MessageExt;
import gov.nist.javax.sip.stack.SIPDialog;
import gov.nist.javax.sip.stack.SIPTransactionStack;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sip.ClientTransaction;
import javax.sip.Dialog;
import javax.sip.DialogTerminatedEvent;
import javax.sip.IOExceptionEvent;
import javax.sip.InvalidArgumentException;
import javax.sip.ListeningPoint;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.ServerTransaction;
import javax.sip.SipException;
import javax.sip.SipListener;
import javax.sip.SipProvider;
import javax.sip.TransactionAlreadyExistsException;
import javax.sip.TransactionDoesNotExistException;
import javax.sip.TransactionTerminatedEvent;
import javax.sip.TransactionUnavailableException;
import javax.sip.address.Address;
import javax.sip.address.SipURI;
import javax.sip.address.TelURL;
import javax.sip.address.URI;
import javax.sip.header.AllowHeader;
import javax.sip.header.AuthenticationInfoHeader;
import javax.sip.header.AuthorizationHeader;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.ContentLengthHeader;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.EventHeader;
import javax.sip.header.ExpiresHeader;
import javax.sip.header.ExtensionHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.Header;
import javax.sip.header.MaxForwardsHeader;
import javax.sip.header.ProxyAuthenticateHeader;
import javax.sip.header.ProxyAuthorizationHeader;
import javax.sip.header.RecordRouteHeader;
import javax.sip.header.ReferToHeader;
import javax.sip.header.RequireHeader;
import javax.sip.header.RouteHeader;
import javax.sip.header.SIPETagHeader;
import javax.sip.header.SIPIfMatchHeader;
import javax.sip.header.SubscriptionStateHeader;
import javax.sip.header.SupportedHeader;
import javax.sip.header.ToHeader;
import javax.sip.header.ViaHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.apache.log4j.Logger;

/**
 * This class is a UAC template. Shootist is the guy that shoots and shootme is
 * the guy that gets shot.
 * 
 * @author M. Ranganathan
 */

public class TestSipListener implements SipListener {
	private String toTag = Integer.toString(new Random().nextInt(10000000));
	
	public List<Request> allRequests = new LinkedList<Request>();
	public List<Response> allResponses = new LinkedList<Response>();

	private static final String PLAIN_UTF8_CONTENT_SUBTYPE = "plain;charset=UTF-8";

	private static final String TEXT_CONTENT_TYPE = "text";
	
	private static final String PIDF_XML_SUBTYPE = "pidf+xml";

	private static final String APPLICATION_CONTENT_TYPE = "application";

	private boolean sendBye;
	
	public int bindings;
	
	public boolean extraRoute = false;
	
	private boolean sendJoinMessage;
	
	private boolean sendReplacesMessage;
	
	private boolean sendByeBeforeTerminatingNotify;
	
	private boolean sendByeAfterTerminatingNotify;
	
	private SipProvider sipProvider;

	public ProtocolObjects protocolObjects;

	private ContactHeader contactHeader;

	private ListeningPoint listeningPoint;

	private ClientTransaction inviteClientTid;
	
	private ServerTransaction inviteServerTid;

	private Dialog dialog;
	
	private Dialog joinDialog;
	
	private Dialog replacesDialog;

	public int myPort;

	private int peerPort;

	private String peerHostPort;

	private int dialogTerminatedCount;

	private int transctionTerminatedCount;

	private int transactionCount;

	private int dialogCount;

	private boolean cancelReceived;
	
	private boolean cancelOkReceived;
	
	private boolean ackSent;
	
	private boolean ackReceived;
	
	private boolean requestTerminatedReceived;
	
	private boolean byeReceived;

	private boolean redirectReceived;
			
	private boolean joinRequestReceived;
	
	private boolean replacesRequestReceived;

	private boolean okToByeReceived;
	
	private boolean authenticationErrorReceived;
	
	private URI requestURI;
	
	private Request inviteRequest;
	
	private Request messageRequest;
	
	private Response finalResponse;
	
	private Response informationalResponse;
	
	private boolean cancelSent;

	private boolean waitForCancel;
	
	private String lastMessageContent;
	
	private List<String> allMessagesContent;
	
	private List<String> allSubscriptionStates;

	private boolean finalResponseReceived;
	
	private int finalResponseToSend = -1;
	
	public int ackCount = 0;
	
	public int notifyCount = 0;
	
	public int numberOf491s = 0;
	
	private List<Integer> provisionalResponsesToSend;

	private boolean useToURIasRequestUri;

	private boolean sendUpdateOn180;

	private String publishEvent = "reg";
	
	private String sipETag;

	private String publishContentMessage;

	private boolean referReceived;

	private boolean challengeRequests;
	private boolean multipleChallengeInResponse;
	
	private static Logger logger = Logger.getLogger(TestSipListener.class);
	
	DigestServerAuthenticationMethod dsam;

	private long timeToWaitBetweenProvisionnalResponse = 1000;
	
	private long timeToWaitBetweenSubsNotify = 1000;

	private boolean byeSent;
	
	private boolean serverErrorReceived;
	
	private long lastInfoResponseTime = -1;
	
	private Integer respondWithError = null;
	
	private long lastRegisterCSeqNumber = -1;

	private int finalResponseStatus;

	private boolean inviteReceived;

	private boolean sendReinvite;
	
	private boolean reinviteSent;
	
	private boolean abortProcessing;
	
	private boolean recordRoutingProxyTesting;
	
	public boolean b2buamessagereceived;
	
	public boolean txTimeoutReceived;
	
	private boolean sendSubsequentRequestsThroughSipProvider;
	
	private boolean testAckViaParam;

	private Request byeRequestReceived;
	
	private Request registerReceived;

	private long timeToWaitBeforeBye = 1000;
	private long timeToWaitBeforeAck = 0;

	private boolean sendAck = true;

	private boolean prackSent;
	
	private boolean prackReceived;
	
	private Request prackRequestReceived;

	private boolean okToPrackReceived;
	
	private AtomicInteger rseqNumber = new AtomicInteger(1);	
	
	public boolean sendByeInNewThread = false;

	private boolean useDefaultRoute = true;

	private boolean setTransport=true;

	private Response serviceUnavailableResponse = null;

	private int referResponseToSend = 202;

	private boolean sendNotifyForRefer = true;
	
	private Response inviteOkResponse;

	private boolean sendNotify = true;
	
	private boolean countRetrans = false;	
	private int nbRetrans = 0;	
	
	public Request firstRequest;
	public Request lastInvite;

	private boolean disableSequenceNumberValidation = false;
	
	private boolean sendCancelOn1xx = false;

	private boolean testNextNonce =false;
	private boolean testNC =false;
	
	private String nextNonce = null;
	private int nc;
	
	private boolean dropRequest = false;

	private long waitBeforeFinalResponse = 0;

	private boolean sendUpdate;

	private boolean updateReceived;

	private boolean sendUpdateAfterUpdate = false;
	
	private boolean sendUpdateAfterPrack = false;

	private boolean sendUpdateAfterProvisionalResponses;

	private Request ackRequest;

	private boolean handleAuthorization = true;

	private Request updateRequest;
	
	private Request infoRequest;
	
	private Request optionsRequest;
	
	private boolean addRecordRouteForResponses;

	private RFC5626UseCase rfc5626UseCase;

	private String securityUser = "user";
	private String securityPwd = "pass";

	private boolean sendProvisionalResponseBeforeChallenge = false;
	
	class MyEventSource implements Runnable {
		private TestSipListener notifier;
		private EventHeader eventHeader;

		public MyEventSource(TestSipListener notifier, EventHeader eventHeader ) {
			this.notifier = notifier;
			this.eventHeader = eventHeader;
		}

		public void run() {
			try {
				for (int i = 0; i < 1; i++) {

					Thread.sleep(timeToWaitBetweenSubsNotify);
					Request request = this.notifier.dialog.createRequest(Request.NOTIFY);
					SubscriptionStateHeader subscriptionState = protocolObjects.headerFactory
							.createSubscriptionStateHeader(SubscriptionStateHeader.ACTIVE);
					request.addHeader(subscriptionState);
					request.addHeader(eventHeader);
										
					allSubscriptionStates.add(subscriptionState.getState().toLowerCase());
					// Lets mark our Contact
//					((SipURI)dialog.getLocalParty().getURI()).setParameter("id","not2");
					
					ClientTransaction ct = sipProvider.getNewClientTransaction(request);
					logger.info("NOTIFY Branch ID " +
						((ViaHeader)request.getHeader(ViaHeader.NAME)).getParameter("branch"));
					this.notifier.dialog.sendRequest(ct);
					logger.info("Dialog " + dialog);
					logger.info("Dialog state after active NOTIFY: " + dialog.getState());
					if(sendByeBeforeTerminatingNotify && !byeSent) {
						sendBye();
					}
				}
			} catch (Throwable ex) {
				logger.info(ex.getMessage(), ex);
			}
		}
	}

	public void processRequest(RequestEvent requestReceivedEvent) {
		if(abortProcessing) {
			logger.error("Processing aborted");
			return ;
		}
		
		
		Request request = requestReceivedEvent.getRequest();
		allRequests.add(request);
		if(firstRequest == null) firstRequest = request;
		ServerTransaction serverTransactionId = requestReceivedEvent
				.getServerTransaction();

		logger.info("\n\nRequest " + request.getMethod()
				+ " received at " + protocolObjects.sipStack.getStackName()
				+ " with server transaction id " + serverTransactionId
				+ " dialog " + requestReceivedEvent.getDialog());
		
		if (request.getMethod().equals(Request.INVITE)) {
			processInvite(requestReceivedEvent, serverTransactionId);
		}
		
		if (request.getMethod().equals(Request.BYE)) {
			processBye(request, serverTransactionId);
		}
		
		if (request.getMethod().equals(Request.ACK)) {
			processAck(request, serverTransactionId);
		}
		
		if (request.getMethod().equals(Request.PRACK)) {
			processPrack(request, serverTransactionId);
		}

		if (request.getMethod().equals(Request.CANCEL)) {
			processCancel(requestReceivedEvent, serverTransactionId);
		}
		
		if (request.getMethod().equals(Request.MESSAGE)) {
			processMessage(request, serverTransactionId);
		}

		if (request.getMethod().equals(Request.REGISTER)) {
			processRegister(request, serverTransactionId);
		}
		
		if (request.getMethod().equals(Request.NOTIFY)) {
			processNotify(requestReceivedEvent, serverTransactionId);
		}
		
		if (request.getMethod().equals(Request.SUBSCRIBE)) {
			processSubscribe(requestReceivedEvent, serverTransactionId);
		}
		
		if (request.getMethod().equals(Request.UPDATE)) {
			processUpdate(request, serverTransactionId);
		}
		
		if (request.getMethod().equals(Request.OPTIONS)) {
			processOptions(request, serverTransactionId);
		}
		
		if (request.getMethod().equals(Request.INFO)) {
			processInfo(request, serverTransactionId);
		}
		
		if (request.getMethod().equals(Request.PUBLISH)) {
			processPublish(requestReceivedEvent, serverTransactionId);
		}
		
		if (request.getMethod().equals(Request.REFER)) {
			processRefer(requestReceivedEvent, serverTransactionId);
		}
	}
	
	private void processPrack(Request request,
			ServerTransaction serverTransactionId) {
				
		try {
			prackReceived = true;
			prackRequestReceived = request;
			ServerTransaction st = serverTransactionId;			
			if (st == null) {
				try {
					st = sipProvider.getNewServerTransaction(request);
				} catch ( TransactionAlreadyExistsException taex ) {
					// This is a retransmission so just return.
					return;				
				} 
			}						
			Response response = protocolObjects.messageFactory.createResponse(
					200, request);
			Address address = protocolObjects.addressFactory
			.createAddress("Shootme <sip:" + System.getProperty("org.mobicents.testsuite.testhostaddr") + ":" + myPort
					+";transport="+protocolObjects.transport
					+ ">");
			contactHeader = protocolObjects.headerFactory.createContactHeader(address);
			response.addHeader(contactHeader);
			st.sendResponse(response);
			
			Thread.sleep(200);
			
			RequireHeader requireHeader = (RequireHeader) request.getHeader(RequireHeader.NAME);				
			if(provisionalResponsesToSend.size() > 0) {
				logger.info("shootme: Creating provisional response with status code " + provisionalResponsesToSend.get(0));
				response = protocolObjects.messageFactory.createResponse(
						provisionalResponsesToSend.get(0), inviteRequest);
				requireHeader = protocolObjects.headerFactory.createRequireHeader("100rel");
				response.addHeader(requireHeader);
				Header rseqHeader = protocolObjects.headerFactory.createRSeqHeader(rseqNumber.getAndIncrement());
				response.addHeader(rseqHeader);
				((MessageExt)response).getToHeader().setTag(((MessageExt) request).getToHeader().getTag());
				address = protocolObjects.addressFactory
				.createAddress("Shootme <sip:" + System.getProperty("org.mobicents.testsuite.testhostaddr") + ":" + myPort
						+";transport="+protocolObjects.transport
						+ ">");
				contactHeader = protocolObjects.headerFactory.createContactHeader(address);
				response.addHeader(contactHeader);
				dialog.sendReliableProvisionalResponse(response);
				provisionalResponsesToSend.remove(0);
				return;
			} 
			
			if(!sendUpdateAfterUpdate)
				inviteServerTid.sendResponse(getFinalResponse());
		} catch(Exception e) {
			logger.error("Unexpected exception while trying to send the 200 to PRACK " + request, e);
		}
	}

	public void processRefer(RequestEvent requestEvent,
			ServerTransaction serverTransactionId) {
		try {
			SipProvider sipProvider = (SipProvider) requestEvent.getSource();
			Request request = requestEvent.getRequest();
			
			logger.info("shootist:  got a refer . ServerTxId = " + serverTransactionId);
			ServerTransaction st = requestEvent.getServerTransaction();
			if (st == null) {
				st = sipProvider.getNewServerTransaction(request);
			}
			inviteServerTid = st;
			Dialog dialog = st.getDialog();
			
			this.dialogCount ++;
			this.dialog = dialog;
			
			logger.info("Shootme: dialog = " + dialog);
			
			Response response = protocolObjects.messageFactory.createResponse(
						referResponseToSend, request);
			sipETag = Integer.toString(new Random().nextInt(10000000));
			st.sendResponse(response);
			this.transactionCount++;
			logger.info("shootist:  Sending " + referResponseToSend);			
			
			List<Header> headers = new ArrayList<Header>();
			EventHeader eventHeader = (EventHeader) 
				protocolObjects.headerFactory.createHeader(EventHeader.NAME, "Refer");
			headers.add(eventHeader);
			
			if(sendNotifyForRefer) {
				if(!referReceived) {
					referReceived = true;
									
					SubscriptionStateHeader subscriptionStateHeader = (SubscriptionStateHeader) 
						protocolObjects.headerFactory.createHeader(SubscriptionStateHeader.NAME, "active;expires=3600");
					headers.add(subscriptionStateHeader);
					allMessagesContent.add("SIP/2.0 100 Trying");
					sendInDialogSipRequest(Request.NOTIFY, "SIP/2.0 100 Trying", "message", "sipfrag;version=2.0", headers, null);
					Thread.sleep(1000);
					headers.remove(subscriptionStateHeader);
					subscriptionStateHeader = (SubscriptionStateHeader) 
						protocolObjects.headerFactory.createHeader(SubscriptionStateHeader.NAME, "terminated;reason=noresource");
					headers.add(subscriptionStateHeader);
					if(inviteRequest == null) {
						ExtensionHeader extensionHeader = (ExtensionHeader) protocolObjects.headerFactory.createHeader("Out-Of-Dialog", "true");
						headers.add(extensionHeader);
					}
					allMessagesContent.add("SIP/2.0 200 OK");
					sendInDialogSipRequest(Request.NOTIFY, "SIP/2.0 200 OK", "message", "sipfrag;version=2.0", headers, null);
				} else {
					SubscriptionStateHeader subscriptionStateHeader = (SubscriptionStateHeader) 
						protocolObjects.headerFactory.createHeader(SubscriptionStateHeader.NAME, "active;expires=3600");
					headers.add(subscriptionStateHeader);
					sendInDialogSipRequest(Request.NOTIFY, "SIP/2.0 100 Subsequent", "message", "sipfrag;version=2.0", headers, null);
				}
			}
			
		} catch (Exception ex) {
			ex.printStackTrace();			
		}		
	}

	private void processPublish(RequestEvent requestEvent,
			ServerTransaction serverTransactionId) {
		try {
			SipProvider sipProvider = (SipProvider) requestEvent.getSource();
			Request request = requestEvent.getRequest();
			
			logger.info("shootist:  got a publish . ServerTxId = " + serverTransactionId);
			ServerTransaction st = requestEvent.getServerTransaction();
			if (st == null) {
				st = sipProvider.getNewServerTransaction(request);
			}
			inviteServerTid = st;
			Dialog dialog = st.getDialog();
			
			this.dialogCount ++;
			this.dialog = dialog;
			
			logger.info("Shootme: dialog = " + dialog);
			
			if(request.getRawContent() != null) {
				this.lastMessageContent = new String(request.getRawContent());
				allMessagesContent.add(new String(lastMessageContent));
			}
			SIPIfMatchHeader sipIfMatchHeader = (SIPIfMatchHeader) request.getHeader(SIPIfMatchHeader.NAME);
			boolean sipIfMatchFound = true;
			if(sipIfMatchHeader!= null && sipIfMatchHeader.getETag() != null && !sipIfMatchHeader.getETag().equals(sipETag)) {
				sipIfMatchFound = false;
			}
			if(sipIfMatchFound) {
			
				Response response = protocolObjects.messageFactory.createResponse(
						200, request);
				sipETag = Integer.toString(new Random().nextInt(10000000));
				SIPETagHeader sipTagHeader = protocolObjects.headerFactory.createSIPETagHeader(sipETag);
				response.addHeader(sipTagHeader);
				response.addHeader(request.getHeader(ExpiresHeader.NAME));
				st.sendResponse(response);
				this.transactionCount++;
				logger.info("shootist:  Sending OK.");
			} else {
				Response response = protocolObjects.messageFactory.createResponse(
						500, request);
				serverTransactionId.sendResponse(response);
				this.transactionCount++;
			}
		
		} catch (Exception ex) {
			ex.printStackTrace();
		}		
	}

	
	public void processUpdate(Request request,
			ServerTransaction serverTransactionId) {
		try {
			logger.info("shootist:  got a update. ServerTxId = " + serverTransactionId);
			if (serverTransactionId == null) {
				logger.info("shootist:  null TID.");
				return;
			}
			
			Dialog dialog = serverTransactionId.getDialog();			
			logger.info("Dialog State = " + dialog.getState());
			Response response = protocolObjects.messageFactory.createResponse(
					200, request);
			serverTransactionId.sendResponse(response);
			this.transactionCount++;
			logger.info("shootist:  Sending OK.");
			logger.info("Dialog State = " + dialog.getState());
			updateReceived = true;
			updateRequest = request;
			if (sendUpdateAfterUpdate) {
				Request updateRequest = dialog.createRequest(Request.UPDATE);

				if (recordRoutingProxyTesting){				
					ContactHeader contHdr = (ContactHeader) request.getHeader(ContactHeader.NAME);
					SipURI sipUri = (SipURI)contHdr.getAddress().getURI();
					updateRequest.setRequestURI(sipUri);
				}

				long seqNo = (((CSeqHeader) request.getHeader(CSeqHeader.NAME)).getSeqNumber());
				CSeqHeader cseqNew = protocolObjects.headerFactory.createCSeqHeader(++seqNo, "UPDATE"); 
				updateRequest.setHeader(cseqNew);
				
				ClientTransaction ct = sipProvider
					.getNewClientTransaction(updateRequest);
				ct.sendRequest();
				sendUpdate = true;
				if(!prackReceived) {
					sendUpdateAfterUpdate = false;
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public void processOptions(Request request,
			ServerTransaction serverTransactionId) {
		try {
			logger.info("shootist:  got a OPTIONS. ServerTxId = " + serverTransactionId);
						
			if (serverTransactionId == null) {
				serverTransactionId = sipProvider.getNewServerTransaction(request);
			}
			if (serverTransactionId == null) {
				logger.info("shootist:  null TID.");
				return;
			}
			
			Dialog dialog = serverTransactionId.getDialog();
			if(dialog != null) {
				logger.info("Dialog State = " + dialog.getState());
			}
			Response response = protocolObjects.messageFactory.createResponse(
					200, request);
			serverTransactionId.sendResponse(response);
			this.transactionCount++;
			logger.info("shootist:  Sending OK.");
			if(dialog != null) {
				logger.info("Dialog State = " + dialog.getState());
			}
			this.optionsRequest = request;		
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public void processInfo(Request request,
			ServerTransaction serverTransactionId) {
		try {
			logger.info("shootist:  got a info. ServerTxId = " + serverTransactionId);
			if (serverTransactionId == null) {
				logger.info("shootist:  null TID.");
				return;
			}
			
			Dialog dialog = serverTransactionId.getDialog();			
			logger.info("Dialog State = " + dialog.getState());
			Response response = protocolObjects.messageFactory.createResponse(
					200, request);
			serverTransactionId.sendResponse(response);
			this.transactionCount++;
			logger.info("shootist:  Sending OK.");
			logger.info("Dialog State = " + dialog.getState());
			infoRequest = request;		
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * Process the invite request.
	 */
	public void processSubscribe(RequestEvent requestEvent,
			ServerTransaction serverTransaction) {
		SipProvider sipProvider = (SipProvider) requestEvent.getSource();
		Request request = requestEvent.getRequest();
		try {
			logger.info("notifier: got an Subscribe sending OK");
			logger.info("notifier:  " + request);
			logger.info("notifier : dialog = " + requestEvent.getDialog());
			EventHeader eventHeader = (EventHeader) request.getHeader(EventHeader.NAME);
//			this.gotSubscribeRequest = true;
			
			// Always create a ServerTransaction, best as early as possible in the code
			Response response = null;
			ServerTransaction st = requestEvent.getServerTransaction();			
			if (st == null) {
				st = sipProvider.getNewServerTransaction(request);
			}
			
			// Check if it is an initial SUBSCRIBE or a refresh / unsubscribe
			boolean isInitial = requestEvent.getDialog() == null;
			if ( isInitial ) {
				// JvB: need random tags to test forking
				String toTag = Integer.toHexString( (int) (Math.random() * Integer.MAX_VALUE) );
				response = protocolObjects.messageFactory.createResponse(202, request);
				ToHeader toHeader = (ToHeader) response.getHeader(ToHeader.NAME);
				
				// Sanity check: to header should not ahve a tag. Else the dialog 
				// should have matched
				toHeader.setTag(toTag); // Application is supposed to set.
				
				this.dialog = st.getDialog();
				// subscribe dialogs do not terminate on bye.
				this.dialog.terminateOnBye(false);
			} else {
				response = protocolObjects.messageFactory.createResponse(202, request);
				this.dialog = st.getDialog();
				// subscribe dialogs do not terminate on bye.
				this.dialog.terminateOnBye(false);
			}

			// Both 2xx response to SUBSCRIBE and NOTIFY need a Contact
			Address address = protocolObjects.addressFactory.createAddress("Notifier <sip:" + System.getProperty("org.mobicents.testsuite.testhostaddr") + ">");
			((SipURI)address.getURI()).setPort( sipProvider.getListeningPoint(ListeningPoint.UDP).getPort() );				
			ContactHeader contactHeader = protocolObjects.headerFactory.createContactHeader(address);			
			response.addHeader(contactHeader);
			
			// Expires header is mandatory in 2xx responses to SUBSCRIBE
			ExpiresHeader expires = (ExpiresHeader) request.getHeader( ExpiresHeader.NAME );
			if (expires==null) {
				expires = protocolObjects.headerFactory.createExpiresHeader(30);	// rather short
			}
			response.addHeader( expires );
			
			/*
			 * JvB: The SUBSCRIBE MUST be answered first. See RFC3265 3.1.6.2: 
			 * "[...] a NOTIFY message is always sent immediately after any 200-
			 * class response to a SUBSCRIBE request"
			 * 
			 *  Do this before creating the NOTIFY request below
			 */
			st.sendResponse(response);
			//Thread.sleep(1000); // Be kind to implementations
						
			/*
			 * NOTIFY requests MUST contain a "Subscription-State" header with a
			 * value of "active", "pending", or "terminated". The "active" value
			 * indicates that the subscription has been accepted and has been
			 * authorized (in most cases; see section 5.2.). The "pending" value
			 * indicates that the subscription has been received, but that
			 * policy information is insufficient to accept or deny the
			 * subscription at this time. The "terminated" value indicates that
			 * the subscription is not active.
			 */
			if(sendNotify ) {
				Request notifyRequest = dialog.createRequest( "NOTIFY" );
				
				
				// Mark the contact header, to check that the remote contact is updated
	//			((SipURI)contactHeader.getAddress().getURI()).setParameter("id","not");
				
				// Initial state is pending, second time we assume terminated (Expires==0)		
				SubscriptionStateHeader sstate = protocolObjects.headerFactory.createSubscriptionStateHeader(
						expires.getExpires() != 0 ? SubscriptionStateHeader.PENDING : SubscriptionStateHeader.TERMINATED );
				allSubscriptionStates.add(sstate.getState().toLowerCase());
				
				
				// Need a reason for terminated
				if ( sstate.getState().equalsIgnoreCase("terminated") ) {
					sstate.setReasonCode( "deactivated" );
				}
				
				notifyRequest.addHeader(sstate);
				notifyRequest.setHeader(eventHeader);
				notifyRequest.setHeader(contactHeader);
				// notifyRequest.setHeader(routeHeader);
				ClientTransaction ct = sipProvider.getNewClientTransaction(notifyRequest);
	
				if(sstate.getState().equals(SubscriptionStateHeader.TERMINATED)) {
					Thread.sleep(timeToWaitBetweenSubsNotify);
				}
				// Let the other side know that the tx is pending acceptance
				//
				dialog.sendRequest(ct);
				logger.info("NOTIFY Branch ID " +
					((ViaHeader)request.getHeader(ViaHeader.NAME)).getParameter("branch"));
				logger.info("Dialog " + dialog);
				logger.info("Dialog state after pending NOTIFY: " + dialog.getState());
				
				if (expires.getExpires() != 0) {
					Thread myEventSource = new Thread(new MyEventSource(this,eventHeader));
					myEventSource.start();
				}
			}
		} catch (Throwable ex) {
			logger.info(ex.getMessage(), ex);
		}
	}
	
	public void processNotify(RequestEvent requestEvent,
			ServerTransaction serverTransactionId) {
		SipProvider provider = (SipProvider) requestEvent.getSource();
		Request notify = requestEvent.getRequest();
		try {
			logger.info("subscriber:  got a notify count  " + this.notifyCount++ );
			if (serverTransactionId == null) {
				logger.info("subscriber:  null TID.");
				serverTransactionId = provider.getNewServerTransaction(notify);
			}
			Dialog dialog = serverTransactionId.getDialog();
//			if ( dialog != subscriberDialog ) {
//				if (forkedDialog == null) {
//					forkedDialog = dialog;
//				} else  {
//					AbstractSubsnotifyTestCase.assertTrue("Dialog should be either the subscriber dialog ", 
//							forkedDialog  == dialog);
//				}
//			}
//			
//			this.dialogs.add(dialog);
			if(dialog != null) {
				logger.info("Dialog State = " + dialog.getState());
			}
			
			Response response = protocolObjects.messageFactory.createResponse(finalResponseToSend, notify);
			// SHOULD add a Contact
			ContactHeader contact = null;
			if(contactHeader != null) {
				contact = (ContactHeader) contactHeader.clone();
			} else {
				SipURI contactURI = protocolObjects.addressFactory.createSipURI(null, "" + System.getProperty("org.mobicents.testsuite.testhostaddr") + "");
				contactURI.setPort(listeningPoint.getPort());
				Address contactAddress = protocolObjects.addressFactory.createAddress(contactURI);
				contact = protocolObjects.headerFactory.createContactHeader(contactAddress);
			}
			response.addHeader( contact );
			logger.info("Transaction State = " + serverTransactionId.getState());
			serverTransactionId.sendResponse(response);
			if(dialog != null) {
				logger.info("Dialog State = " + dialog.getState());
			}
			SubscriptionStateHeader subscriptionState = (SubscriptionStateHeader) notify
					.getHeader(SubscriptionStateHeader.NAME);

			// Subscription is terminated?
			String state = subscriptionState.getState();
			allSubscriptionStates.add(state.toLowerCase());
			if(notify.getRawContent() != null) {
				this.lastMessageContent = new String(notify.getRawContent());
				allMessagesContent.add(new String(lastMessageContent));
			}
			if (state.equalsIgnoreCase(SubscriptionStateHeader.TERMINATED)) {
				if(subscriptionState.getReasonCode() == null) {
					dialog.delete();
				}
			} else if (state.equalsIgnoreCase(SubscriptionStateHeader.ACTIVE)) {				
				if("reg".equalsIgnoreCase(((EventHeader)notify.getHeader(EventHeader.NAME)).getEventType())) {
					if(sendByeBeforeTerminatingNotify) {
						dialog.terminateOnBye(false);
						sendBye();
						Thread.sleep(1000);
					}
					logger.info("Subscriber: sending unSUBSCRIBE");
					
					// Else we end it ourselves
					Request unsubscribe = dialog.createRequest(Request.SUBSCRIBE);
					
					logger.info( "dialog created:" + unsubscribe );
					// SHOULD add a Contact (done by dialog), lets mark it to test updates
	//				((SipURI) dialog.getLocalParty().getURI()).setParameter( "id", "unsub" );
					ExpiresHeader expires = protocolObjects.headerFactory.createExpiresHeader(0);
					unsubscribe.addHeader(expires);
					// JvB note : stack should do this!
					unsubscribe.addHeader(notify.getHeader(EventHeader.NAME)); // copy
												// event
												// header
					logger.info("Sending Unsubscribe : " + unsubscribe);
					logger.info("unsubscribe dialog  " + dialog);
					ClientTransaction ct = sipProvider.getNewClientTransaction(unsubscribe);
					dialog.sendRequest(ct);
					if(sendByeAfterTerminatingNotify) {
						Thread.sleep(1000);
						sendBye();
					}
				} else if(sendByeBeforeTerminatingNotify) {
					sendBye();
				}
			} else {
				logger.info("Subscriber: state now " + state);// pending
			}

		} catch (Exception ex) {
			logger.error("Unexpected exception",ex);
		}
	}
	
	private void processMessage(Request request,
			ServerTransaction serverTransactionId) {
		if(request.toString().contains("408 received")) {
			txTimeoutReceived = true;
		}
		ServerTransaction serverTransaction = null;

		messageRequest = request;
        try {

            serverTransaction = 
            	(serverTransactionId == null? 
            			sipProvider.getNewServerTransaction(request): 
            				serverTransactionId);
        } catch (javax.sip.TransactionAlreadyExistsException ex) {
            ex.printStackTrace();
            return;
        } catch (javax.sip.TransactionUnavailableException ex1) {
            ex1.printStackTrace();
            return;
        }
		
		ContentTypeHeader contentTypeHeader = (ContentTypeHeader) 
		request.getHeader(ContentTypeHeader.NAME);
		boolean sendInvitewithJoin = false;
		boolean sendInvitewithReplaces = false;
		if(contentTypeHeader != null) {
			if(TEXT_CONTENT_TYPE.equals(contentTypeHeader.getContentType())) {
				this.lastMessageContent = new String(request.getRawContent());
				allMessagesContent.add(new String(lastMessageContent));
				if(lastMessageContent.indexOf("Join : ") != -1) {
					this.lastMessageContent = lastMessageContent.substring("Join : ".length());
					sendInvitewithJoin = true;
				}
				if(lastMessageContent.indexOf("Replaces : ") != -1) {
					this.lastMessageContent = lastMessageContent.substring("Replaces : ".length());
					sendInvitewithReplaces = true;
				}
			}
		} else {
			if(request.getHeader("From").toString().contains("b2bua@sip-servlets"))
					b2buamessagereceived = true;
		}
		try {
			Response okResponse = protocolObjects.messageFactory.createResponse(
					Response.OK, request);			
			ToHeader toHeader = (ToHeader) okResponse.getHeader(ToHeader.NAME);
			if (toHeader.getTag() == null) {
				toHeader.setTag(Integer.toString(new Random().nextInt(10000000)));
			}
//			okResponse.addHeader(contactHeader);
			serverTransaction.sendResponse(okResponse);
		} catch (Exception ex) {
			ex.printStackTrace();
			logger.error("error sending OK response to message", ex);
		}			
		if(sendInvitewithJoin) {
			try {
				String fromUser = "receiver";
				String fromHost = "sip-servlets.com";
				SipURI fromAddress = protocolObjects.addressFactory.createSipURI(
						fromUser, fromHost);
				
				String toUser = "join-receiver";
				String toHost = "sip-servlets.com";
				SipURI toAddress = protocolObjects.addressFactory.createSipURI(
						toUser, toHost);
				String[] headerNames = new String[] {"Join"};
				String[] headerContents = new String[] {lastMessageContent};
				sendSipRequest("INVITE", fromAddress, toAddress, null, null, false, headerNames, headerContents, true);
			} catch (Exception ex) {
				ex.printStackTrace();
				logger.error("error sending INVITE with Join", ex);
			}
		}
		if(sendInvitewithReplaces) {
			try {
				String fromUser = "receiver";
				String fromHost = "sip-servlets.com";
				SipURI fromAddress = protocolObjects.addressFactory.createSipURI(
						fromUser, fromHost);
				
				String toUser = "replaces-receiver";
				String toHost = "sip-servlets.com";
				SipURI toAddress = protocolObjects.addressFactory.createSipURI(
						toUser, toHost);
				String[] headerNames = new String[] {"Replaces"};
				String[] headerContents = new String[] {lastMessageContent};
				sendSipRequest("INVITE", fromAddress, toAddress, null, null, false, headerNames, headerContents, true);
			} catch (Exception ex) {
				ex.printStackTrace();
				logger.error("error sending INVITE with Join", ex);
			}
		}
	}
	
	private void processRegister(Request request,
			ServerTransaction serverTransactionId) {				

        try {
        	registerReceived = request;
        	ServerTransaction serverTransaction = serverTransactionId == null? sipProvider.getNewServerTransaction(request) : serverTransactionId;
        	
        	System.out.println("challenge Requests ? " +  challengeRequests);
        	lastRegisterCSeqNumber = ((CSeqHeader)request.getHeader("CSeq")).getSeqNumber();
        	if(challengeRequests) {
				// Verify AUTHORIZATION !!!!!!!!!!!!!!!!
		        dsam = new DigestServerAuthenticationMethod();
		        dsam.initialize(); // it should read values from file, now all static
		        if ( !checkProxyAuthorization(request) ) {
		            Response responseauth = protocolObjects.messageFactory.createResponse(Response.PROXY_AUTHENTICATION_REQUIRED,request);
		
		            ProxyAuthenticateHeader proxyAuthenticate = 
		            	protocolObjects.headerFactory.createProxyAuthenticateHeader(dsam.getScheme());
		            proxyAuthenticate.setParameter("realm",dsam.getRealm(null));
		            proxyAuthenticate.setParameter("nonce",dsam.generateNonce());
		            //proxyAuthenticateImpl.setParameter("domain",authenticationMethod.getDomain());
		            proxyAuthenticate.setParameter("opaque","");
		            
		            proxyAuthenticate.setParameter("algorithm",dsam.getAlgorithm());
		            if(testNC) {
		            	proxyAuthenticate.setQop("auth");
		            }
		            responseauth.setHeader(proxyAuthenticate);
		            // alexander kozlov : Adding the to tag to 407 as well 
		            ToHeader toHeader = (ToHeader) responseauth.getHeader(ToHeader.NAME);
					if (toHeader.getTag() == null) {
						toHeader.setTag(Integer.toString(new Random().nextInt(10000000)));
					}
					
		            if (serverTransaction!=null)
		                serverTransaction.sendResponse(responseauth);
		            else 
		                sipProvider.sendResponse(responseauth);
		
		            System.out.println("RequestValidation: 407 PROXY_AUTHENTICATION_REQUIRED replied:\n"+responseauth.toString());
		            return;
		        } else if(multipleChallengeInResponse && lastRegisterCSeqNumber > 2 && lastRegisterCSeqNumber % 2 == 1) {
		        	Response responseauth = protocolObjects.messageFactory.createResponse(Response.PROXY_AUTHENTICATION_REQUIRED,request);
		     		
//		        	ListIterator<Header> proxyAuthHeaders = request.getHeaders(ProxyAuthorizationHeader.NAME);
//		        	while (proxyAuthHeaders.hasNext()) {
//		        		ProxyAuthorizationHeader header = (ProxyAuthorizationHeader) proxyAuthHeaders.next();
//		        		ProxyAuthenticateHeader proxyAuthenticate = 
//			            	protocolObjects.headerFactory.createProxyAuthenticateHeader(dsam.getScheme());
//		        		proxyAuthenticate.setAlgorithm(header.getAlgorithm());		        		
//		        		proxyAuthenticate.setNonce(header.getNonce());
//		        		proxyAuthenticate.setRealm(header.getRealm());
//		        		proxyAuthenticate.setParameter("username", header.getParameter("username"));
//		        		proxyAuthenticate.setParameter("uri", header.getParameter("uri"));
//		        		proxyAuthenticate.setParameter("response", header.getParameter("response"));
//		        		proxyAuthenticate.setStale(false);
//						logger.debug("Adding auth header to challenge response " + proxyAuthenticate); 
//						responseauth.addLast(proxyAuthenticate);
//					}
//		        	
		            ProxyAuthenticateHeader proxyAuthenticate = 
		            	protocolObjects.headerFactory.createProxyAuthenticateHeader(dsam.getScheme());
		            proxyAuthenticate.setParameter("realm",dsam.getRealm(null));
		            proxyAuthenticate.setParameter("nonce",dsam.generateNonce());
		            //proxyAuthenticateImpl.setParameter("domain",authenticationMethod.getDomain());
		            proxyAuthenticate.setParameter("opaque","");
		            proxyAuthenticate.setStale(true);
		            
		            proxyAuthenticate.setParameter("algorithm",dsam.getAlgorithm());
		            responseauth.addHeader(proxyAuthenticate);
		            // alexander kozlov : Adding the to tag to 407 as well 
		            ToHeader toHeader = (ToHeader) responseauth.getHeader(ToHeader.NAME);
					if (toHeader.getTag() == null) {
						toHeader.setTag(Integer.toString(new Random().nextInt(10000000)));
					}
					
		            if (serverTransaction!=null)
		                serverTransaction.sendResponse(responseauth);
		            else 
		                sipProvider.sendResponse(responseauth);
		
		            System.out.println("RequestValidation: 407 PROXY_AUTHENTICATION_REQUIRED replied:\n"+responseauth.toString());
	        	}
			}        	        	
            
			Response okResponse = protocolObjects.messageFactory.createResponse(
					Response.OK, request);			
			if(testNextNonce) {
				AuthenticationInfoHeader authenticationInfoHeader = protocolObjects.headerFactory.createAuthenticationInfoHeader("");
				nextNonce = dsam.generateNonce();
				authenticationInfoHeader.setNextNonce(nextNonce);
				authenticationInfoHeader.removeParameter(ParameterNames.RESPONSE_AUTH);
				okResponse.addHeader(authenticationInfoHeader);
			}
			ToHeader toHeader = (ToHeader) okResponse.getHeader(ToHeader.NAME);
			if (toHeader.getTag() == null) {
				toHeader.setTag(Integer.toString(new Random().nextInt(10000000)));
			}
//			okResponse.addHeader(contactHeader);
			SupportedHeader supportedHeader = (SupportedHeader) request.getHeader(SupportedHeader.NAME);
			PathHeader pathHeader = (PathHeader) request.getHeader(PathHeader.NAME);
			contactHeader = (ContactHeader) request.getHeader(ContactHeader.NAME);
			if(supportedHeader != null) {
				okResponse.addHeader(supportedHeader);
				if(((HeaderExt)supportedHeader).getValue().contains("outbound")) {					
					okResponse.addHeader(protocolObjects.headerFactory.createRequireHeader("outbound"));
					contactHeader.setExpires(3600);
					okResponse.addHeader(contactHeader);
					if(pathHeader != null) {
						okResponse.addHeader(pathHeader);
					}
				}
			}
			serverTransaction.sendResponse(okResponse);
		} catch (Exception ex) {
			ex.printStackTrace();
			logger.error("error sending OK response to message", ex);
		}		
	}

	private void processCancel(RequestEvent requestEvent,
			ServerTransaction serverTransactionId) {
		try {
			cancelReceived = true;
			SipProvider sipProvider = (SipProvider) requestEvent.getSource();
			Request request = requestEvent.getRequest();
			Response response = protocolObjects.messageFactory.createResponse(
					Response.OK, request);
			ServerTransaction st = requestEvent.getServerTransaction();
	
			if (st == null) {
				st = sipProvider.getNewServerTransaction(request);
			}
			Dialog dialog = st.getDialog();		
			logger.info("Shootme: dialog = " + dialog);		
			st.sendResponse(response);
			
			response = protocolObjects.messageFactory.createResponse(
					Response.REQUEST_TERMINATED, inviteRequest);
			inviteServerTid.sendResponse(response);
		} catch (Exception ex) {
			ex.printStackTrace();
			logger.error("error sending CANCEL responses", ex);
		}
	}

	/**
	 * Process the invite request.
	 */
	public void processInvite(RequestEvent requestEvent,
			ServerTransaction serverTransaction) {
		inviteReceived = true; 
		SipProvider sipProvider = (SipProvider) requestEvent.getSource();
		Request request = requestEvent.getRequest();
		inviteRequest = request;
		logger.info("shootme: got an Invite " + request);
		if(dropRequest) {
			logger.warn("dropping " + request);
			return;
		}
		try {
			ServerTransaction st = requestEvent.getServerTransaction();			
			if (st == null) {
				st = sipProvider.getNewServerTransaction(request);
			}
			inviteServerTid = st;
			Dialog dialog = st.getDialog();
			if(request.getHeader(JoinHeader.NAME) != null) {
				setJoinRequestReceived(true);
				this.joinDialog = dialog;
			} else if (request.getHeader(ReplacesHeader.NAME) != null) {
				setReplacesRequestReceived(true);
				this.replacesDialog = dialog;
			} else {
				this.dialogCount ++;
				this.dialog = dialog;
			}						
			
			logger.info("Shootme: dialog = " + dialog);
			if(dialog != null) {
				logger.info("Shootme: dialog state = " + dialog.getState());
			}
			if(dialog != null && disableSequenceNumberValidation) {
				((DialogExt)dialog).disableSequenceNumberValidation();
			}
			this.inviteRequest = request;
			
			boolean sendReliably = false;
			RequireHeader requireHeader = (RequireHeader) request.getHeader(RequireHeader.NAME);				
			if(requireHeader != null && "100rel".equalsIgnoreCase(requireHeader.getOptionTag().trim())) {
				sendReliably = true;
			}
			
			if(sendProvisionalResponseBeforeChallenge) {
				sendProvisionalResponses(request, st, dialog, sendReliably);				
			}
			if(challengeRequests) {
				// Verify AUTHORIZATION !!!!!!!!!!!!!!!!
		        dsam = new DigestServerAuthenticationMethod();
		        dsam.initialize(); // it should read values from file, now all static
		        if ( !checkProxyAuthorization(request) ) {
		        	 Thread.sleep(600);
		        	if(!sendProvisionalResponseBeforeChallenge) {
			        	Response response100 = protocolObjects.messageFactory.createResponse(100,request);
			        	if (st!=null)
			        		st.sendResponse(response100);
				        else 
				        	sipProvider.sendResponse(response100);
			        	 
			        	Thread.sleep(600);
		        	}
		    		
		            Response responseauth = protocolObjects.messageFactory.createResponse(Response.PROXY_AUTHENTICATION_REQUIRED,request);
		
		            ProxyAuthenticateHeader proxyAuthenticate = 
		            	protocolObjects.headerFactory.createProxyAuthenticateHeader(dsam.getScheme());
		            proxyAuthenticate.setParameter("realm",dsam.getRealm(null));
		            proxyAuthenticate.setParameter("nonce",dsam.generateNonce());
		            //proxyAuthenticateImpl.setParameter("domain",authenticationMethod.getDomain());
		            proxyAuthenticate.setParameter("opaque","");
		            proxyAuthenticate.setParameter("stale","FALSE");
		            proxyAuthenticate.setParameter("algorithm",dsam.getAlgorithm());		           
		            responseauth.setHeader(proxyAuthenticate);		            
		            
		            if(multipleChallengeInResponse) {
		            	proxyAuthenticate = 
			            	protocolObjects.headerFactory.createProxyAuthenticateHeader(dsam.getScheme());
			            proxyAuthenticate.setParameter("realm",dsam.getRealm(null));
			            proxyAuthenticate.setParameter("nonce",dsam.generateNonce());
			            //proxyAuthenticateImpl.setParameter("domain",authenticationMethod.getDomain());
			            proxyAuthenticate.setParameter("opaque","");
			            proxyAuthenticate.setParameter("stale","true");
			            proxyAuthenticate.setParameter("algorithm",dsam.getAlgorithm());			            
			            responseauth.addHeader(proxyAuthenticate);
		            }
		
		            ToHeader toHeader = (ToHeader) responseauth.getHeader(ToHeader.NAME);
		            toHeader.setTag(toTag); // Application is supposed to set.
		            
		            if(sendProvisionalResponseBeforeChallenge) {
						provisionalResponsesToSend.clear();
						toTag = Integer.toString(new Random().nextInt(10000000));
						System.out.println("Resetted toTag to "+toTag);
//						this.dialog.delete();
//						((SIPTransactionStack)this.protocolObjects.sipStack).removeDialog((SIPDialog)dialog);
////						Thread.sleep(10000);
//						this.dialog = null;
					}
		            
		            if (st!=null)
		                st.sendResponse(responseauth);
		            else 
		                sipProvider.sendResponse(responseauth);
		
					
		            System.out.println("RequestValidation: 407 PROXY_AUTHENTICATION_REQUIRED replied:\n"+responseauth.toString());
		            return;
		        }
		        System.out.println("shootme: got an Invite with Authorization, sending Trying");
			}
			if(!sendProvisionalResponseBeforeChallenge) {
				sendProvisionalResponses(request, st, dialog, sendReliably);
			}
			
			if (sendUpdateAfterProvisionalResponses) {
				Request updateRequest = dialog.createRequest(Request.UPDATE);

				if (recordRoutingProxyTesting){				
					ContactHeader contHdr = (ContactHeader) request.getHeader(ContactHeader.NAME);
					SipURI sipUri = (SipURI)contHdr.getAddress().getURI();
					updateRequest.setRequestURI(sipUri);
				}

				long seqNo = (((CSeqHeader) request.getHeader(CSeqHeader.NAME)).getSeqNumber());
				CSeqHeader cseqNew = protocolObjects.headerFactory.createCSeqHeader(++seqNo, "UPDATE"); 
				updateRequest.setHeader(cseqNew);
				
				sipProvider.sendRequest(updateRequest);
				sendUpdate = true;
			}
			
			if(respondWithError != null && !sendReliably) {
				Response response = protocolObjects.messageFactory.createResponse(
						respondWithError, request);
				st.sendResponse(response);
				return;
			}
		
			
			ContactHeader contactHeader = (ContactHeader)request.getHeader(ContactHeader.NAME);
			if(contactHeader != null) {
				Iterator it = request.getHeaders(ContactHeader.NAME);
				int c=0;
				while(it.hasNext()) {c++;it.next();}
				bindings = c;
			}
			if(contactHeader != null && "0.0.0.0".equals(((SipURI)contactHeader.getAddress().getURI()).getHost())) {
				abortProcessing = true;
				throw new IllegalArgumentException("we received a contact header with 0.0.0.0 in an INVITE !");
			}
			
			if(!waitForCancel) {
				Address address = protocolObjects.addressFactory
					.createAddress("Shootme <sip:" + System.getProperty("org.mobicents.testsuite.testhostaddr") + ":" + myPort
						+";transport="+protocolObjects.transport
						+ ">");
				contactHeader = protocolObjects.headerFactory.createContactHeader(address);						
				setFinalResponse(protocolObjects.messageFactory
						.createResponse(finalResponseToSend, request));
				Response response = getFinalResponse();
				logger.debug("created final response " + response);
				if(testAckViaParam) {
					ViaHeader viaHeader = (ViaHeader)response.getHeader(ViaHeader.NAME);
					viaHeader.setParameter("testAckViaParam", "true");
				}
				if(testNextNonce) {
					AuthenticationInfoHeader authenticationInfoHeader = protocolObjects.headerFactory.createAuthenticationInfoHeader("");
					nextNonce = dsam.generateNonce();
					authenticationInfoHeader.setNextNonce(nextNonce);					
					authenticationInfoHeader.removeParameter(ParameterNames.RESPONSE_AUTH);
					response.addHeader(authenticationInfoHeader);
				}
				ToHeader toHeader = (ToHeader) response.getHeader(ToHeader.NAME);
				if(sendProvisionalResponseBeforeChallenge) {
					toHeader.removeParameter("tag");
				}
				if(toHeader.getTag() == null) {
					logger.debug("setting to Tag " + toTag);
					toHeader.setTag(toTag); // Application is supposed to set.
				}
				getFinalResponse().addHeader(contactHeader);
				if(isAddRecordRouteForResponses()) {
					address = protocolObjects.addressFactory
					.createAddress("sip:127.0.0.1:" + myPort +";transport="+protocolObjects.transport);
					RecordRouteHeader recordRouteHeader = protocolObjects.headerFactory.createRecordRouteHeader(address);
					response.addHeader(recordRouteHeader);
				}
				if(!sendReliably) {					
					Thread.sleep(waitBeforeFinalResponse);
					logger.debug("sending back response " + response);
					st.sendResponse(response);
				}
				
				if(sendUpdateAfterPrack){
					Thread.sleep(waitBeforeFinalResponse);
//					st.sendResponse(getFinalResponse());
				}
			} else {
				logger.info("Waiting for CANCEL, stopping the INVITE processing ");
				return ;
			}
			
			if(request.getRequestURI() instanceof SipURI && ("join").equalsIgnoreCase(((SipUri)request.getRequestURI()).getUser())) {
				sendJoinMessage = true;
			}
			if(request.getRequestURI() instanceof SipURI && ("replaces").equalsIgnoreCase(((SipUri)request.getRequestURI()).getUser())) {
				sendReplacesMessage = true;
			}
		} catch (Exception ex) {
			logger.error("unexpected exception", ex);
		}
	}

	private void sendProvisionalResponses(Request request,
			ServerTransaction st, Dialog dialog, boolean sendReliably)
			throws InterruptedException, ParseException,
			InvalidArgumentException, SipException {
		RequireHeader requireHeader;
		Iterator<Integer> provisionalResponseIt = provisionalResponsesToSend.iterator();
		while (provisionalResponseIt.hasNext()) {
			int provisionalResponseToSend = provisionalResponseIt.next();
			Thread.sleep(getTimeToWaitBetweenProvisionnalResponse());
			logger.info("shootme: Creating provisional response with status code " + provisionalResponseToSend);
			Response response = protocolObjects.messageFactory.createResponse(
					provisionalResponseToSend, request);
			if(response.getStatusCode() == 183) {
				response.setReasonPhrase("different" + System.nanoTime());
			}				
			if(provisionalResponseToSend >= Response.TRYING && provisionalResponseToSend < Response.OK) {
				ToHeader toHeader = (ToHeader) response.getHeader(ToHeader.NAME);
				if(provisionalResponseToSend != Response.TRYING && toHeader.getTag() == null) {
					toHeader.setTag(toTag); // Application is supposed to set.
				}
				if(sendReliably && provisionalResponseToSend != Response.TRYING) {
					provisionalResponsesToSend.remove(0);
					provisionalResponseIt = provisionalResponsesToSend.iterator();
					requireHeader = protocolObjects.headerFactory.createRequireHeader("100rel");
					response.addHeader(requireHeader);
					Header rseqHeader = protocolObjects.headerFactory.createRSeqHeader(rseqNumber.getAndIncrement());
					response.addHeader(rseqHeader);
					Address address = protocolObjects.addressFactory
					.createAddress("Shootme <sip:" + System.getProperty("org.mobicents.testsuite.testhostaddr") + ":" + myPort
							+";transport="+protocolObjects.transport
							+ ">");
					contactHeader = protocolObjects.headerFactory.createContactHeader(address);
					response.addHeader(contactHeader);
					if(isAddRecordRouteForResponses()) {
						address = protocolObjects.addressFactory
						.createAddress("sip:127.0.0.1:" + myPort +";transport="+protocolObjects.transport);
						RecordRouteHeader recordRouteHeader = protocolObjects.headerFactory.createRecordRouteHeader(address);
						response.addHeader(recordRouteHeader);
					}
					dialog.sendReliableProvisionalResponse(response);							
					break;
				}  else {
					if(provisionalResponseToSend == Response.TRYING) {
						provisionalResponsesToSend.remove(0);
						provisionalResponseIt = provisionalResponsesToSend.iterator();
					}
					Address address = protocolObjects.addressFactory
					.createAddress("Shootme <sip:" + System.getProperty("org.mobicents.testsuite.testhostaddr") + ":" + myPort
							+";transport="+protocolObjects.transport
							+ ">");
					contactHeader = protocolObjects.headerFactory.createContactHeader(address);						
					response.addHeader(contactHeader);
					if(isAddRecordRouteForResponses()) {
						address = protocolObjects.addressFactory
						.createAddress("sip:127.0.0.1:" + myPort +";transport="+protocolObjects.transport);
						RecordRouteHeader recordRouteHeader = protocolObjects.headerFactory.createRecordRouteHeader(address);
						response.addHeader(recordRouteHeader);
					}
					
					st.sendResponse(response);
				}					
			}
		}
	}
	
	public boolean checkProxyAuthorization(Request request) {
        // Let Acks go through unchallenged.
        ProxyAuthorizationHeader proxyAuthorization=
                (ProxyAuthorizationHeader)request.getHeader(ProxyAuthorizationHeader.NAME);

       if (proxyAuthorization==null) {
    	   logger.error("Authentication failed: ProxyAuthorization header missing!");     
           return false;
       }else{
    	   
    	   if(nextNonce != null && !proxyAuthorization.getNonce().equals(nextNonce)) {
    		   throw new IllegalArgumentException("Authentication failed: ProxyAuthorization nonce " + proxyAuthorization.getNonce() + " is different from the nextnonce  previously generated " + nextNonce);         		   
    	   }
    	   
    	   if(nc > 0 && proxyAuthorization.getNonceCount() != nc) {
    		   throw new IllegalArgumentException("Authentication failed: ProxyAuthorization nonceCount " + proxyAuthorization.getNonceCount() + " is different from the nextnonce  previously generated " + nc);
    	   }
    	   
           String username=proxyAuthorization.getParameter("username");
           //String password=proxyAuthorization.getParameter("password");
   
           try{
                boolean res=dsam.doAuthenticate(username,proxyAuthorization,request);
                if (res) logger.info("Authentication passed for user: "+username);
                else logger.error("Authentication failed for user: "+username); 
                return res;
           }
           catch(Exception e) {
                e.printStackTrace();
                return false;
           }     
       } 
    }


	public void processBye(Request request,
			ServerTransaction serverTransactionId) {
		try {
			logger.info("shootist:  got a bye . ServerTxId = " + serverTransactionId);
			if(abortProcessing) {
				logger.error("Processing Aborted!");
				return ;
			}
			this.byeReceived  = true;
			byeRequestReceived = request;
			if (serverTransactionId == null) {
				logger.info("shootist:  null TID.");
				return;
			}
			
			Dialog dialog = serverTransactionId.getDialog();			
			logger.info("Dialog State = " + dialog.getState());
			Response response = protocolObjects.messageFactory.createResponse(
					200, request);
			serverTransactionId.sendResponse(response);
			this.transactionCount++;
			logger.info("shootist:  Sending OK.");
			logger.info("Dialog State = " + dialog.getState());
		
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public void processAck(Request request,
			ServerTransaction serverTransactionId) {
		try {
			logger.info("shootist:  got a " + request);
			logger.info("shootist:  got an ACK. ServerTxId = " + serverTransactionId);
			ackReceived = true;
			ackRequest = request;
			
			//we don't count retransmissions
			if(serverTransactionId != null) {
				ackCount ++;
			}
			if(testAckViaParam) {
				ViaHeader viaHeader = (ViaHeader)request.getHeader(ViaHeader.NAME);
				String param = viaHeader.getParameter("testAckViaParam");
				if(param != null) {
					abortProcessing = true;
					logger.error("the Via Param set in the response shouldn't be present in the ACK");
					return;
				}
			}
			if(sendBye) {											
				Thread.sleep(timeToWaitBeforeBye );
				if(serverTransactionId != null && serverTransactionId.getDialog() != null) {
					Request byeRequest = serverTransactionId.getDialog().createRequest(Request.BYE);					
					logger.info("BYE created : " + byeRequest);
					ClientTransaction ct = sipProvider.getNewClientTransaction(byeRequest);
					logger.info("Sending BYE : " + byeRequest);
					serverTransactionId.getDialog().sendRequest(ct);
					logger.info("Dialog State = " + serverTransactionId.getDialog().getState());
				}
			}	
			if(!joinRequestReceived && sendJoinMessage) {
				String fromUser = "join";
				String fromHost = "sip-servlets.com";
				SipURI fromAddress = protocolObjects.addressFactory.createSipURI(
						fromUser, fromHost);
				
				String toUser = "join-receiver";
				String toHost = "sip-servlets.com";
				SipURI toAddress = protocolObjects.addressFactory.createSipURI(
						toUser, toHost);
				
				CallIdHeader callIdHeader = (CallIdHeader) request.getHeader(CallIdHeader.NAME);
				FromHeader fromHeader = (FromHeader) request.getHeader(FromHeader.NAME);
				ToHeader toHeader = (ToHeader) request.getHeader(ToHeader.NAME);
				JoinHeader joinHeader = (JoinHeader) ((HeaderFactoryExt)protocolObjects.headerFactory).createJoinHeader(callIdHeader.getCallId(), toHeader.getTag(), fromHeader.getTag());
				
				sendSipRequest("MESSAGE", fromAddress, toAddress, joinHeader.toString(), null, false);
			}
			if(!isReplacesRequestReceived() && sendReplacesMessage) {
				String fromUser = "replaces";
				String fromHost = "sip-servlets.com";
				SipURI fromAddress = protocolObjects.addressFactory.createSipURI(
						fromUser, fromHost);
				
				String toUser = "replaces-receiver";
				String toHost = "sip-servlets.com";
				SipURI toAddress = protocolObjects.addressFactory.createSipURI(
						toUser, toHost);
				
				CallIdHeader callIdHeader = (CallIdHeader) request.getHeader(CallIdHeader.NAME);
				FromHeader fromHeader = (FromHeader) request.getHeader(FromHeader.NAME);
				ToHeader toHeader = (ToHeader) request.getHeader(ToHeader.NAME);
				ReplacesHeader replacesHeader = (ReplacesHeader) ((HeaderFactoryExt)protocolObjects.headerFactory).createReplacesHeader(callIdHeader.getCallId(), toHeader.getTag(), fromHeader.getTag());
				
				sendSipRequest("MESSAGE", fromAddress, toAddress, replacesHeader.toString(), null, false);
			}
			if(joinRequestReceived) {
				sendBye();
				sendBye(joinDialog);
			}
			if(isReplacesRequestReceived()) {
				sendBye();
				sendBye(replacesDialog);
			}
			if(sendReinvite && !reinviteSent) {
				prackSent = false;
				List<Header> headers = new ArrayList<Header>();
				Header reinviteHeader = protocolObjects.headerFactory.createHeader("ReInvite", "true");
				headers.add(reinviteHeader);
				String transport = null;
				if(ackRequest.getRequestURI() instanceof SipURI) {
					transport = ((SipURI)ackRequest.getRequestURI()).getTransportParam();
				}
				sendInDialogSipRequest("INVITE", null, null, null, headers, transport);
				reinviteSent = true;
				return;
			}
		} catch (Exception ex) {
			ex.printStackTrace();			
		}
	}
	public void processResponse(ResponseEvent responseReceivedEvent) {
		if(abortProcessing) {
			logger.error("Processing aborted");
			return ;
		}		
		Response response = (Response) responseReceivedEvent.getResponse();
		allResponses.add(response);
		if(response.getStatusCode() == 491) numberOf491s++;
		RecordRouteHeader recordRouteHeader = (RecordRouteHeader)response.getHeader(RecordRouteHeader.NAME);
		if(!recordRoutingProxyTesting && recordRouteHeader != null) {
			abortProcessing = true;
			throw new IllegalArgumentException("we received a record route header in a response !");			
		}
		ContactHeader contactHeader = (ContactHeader)response.getHeader(ContactHeader.NAME);
		if(contactHeader != null && "0.0.0.0".equals(((SipURI)contactHeader.getAddress().getURI()).getHost())) {
			abortProcessing = true;
			throw new IllegalArgumentException("we received a contact header with 0.0.0.0 in a response !");
		}
		
		if(response.getStatusCode() >= 400 && response.getStatusCode() < 999) {
			this.serverErrorReceived = true;
		}		
		if(response.getStatusCode() == 503) {
			this.serviceUnavailableResponse = response;
		}
		if(response.toString().toLowerCase().contains("info")) {
			lastInfoResponseTime = System.currentTimeMillis();
		}
		ClientTransaction tid = responseReceivedEvent.getClientTransaction();
		Dialog responseDialog = null;
		if(tid != null) {
			responseDialog = tid.getDialog();
		} else {
			responseDialog = responseReceivedEvent.getDialog();
		}
		CSeqHeader cseq = (CSeqHeader) response.getHeader(CSeqHeader.NAME);

		logger.info("Response received : Status Code = "
				+ response.getStatusCode() + " " + cseq);
		if (tid == null && countRetrans) {
			nbRetrans++;
		}
		// not dropping in PRACK case on REINVITE the ClientTx can be null it seems		
		if (tid == null && responseDialog == null && !prackSent) {
			logger.info("Stray response -- dropping ");
			return;
		}		
		logger.info("Dialog = " + responseDialog);
		if(responseDialog != null) {
			logger.info("Dialog State is " + responseDialog.getState());
		}
		if(tid !=null) {
			logger.info("transaction state is " + tid.getState());						
		}

		try {			
			if(response.getStatusCode() > 100 && response.getStatusCode() < 200) {
				informationalResponse = response;
				if(sendCancelOn1xx) {
					sendCancel();
					return;
				}
			}
			if(response.getStatusCode() >= 200 && response.getStatusCode() < 700) {
				logger.info("final response received : status code " + response.getStatusCode());
				finalResponseReceived = true;
				setFinalResponseStatus(response.getStatusCode());
				setFinalResponse(response);
			}
			if (response.getStatusCode() == Response.OK) {
				logger.info("response = " + response);
				if (cseq.getMethod().equals(Request.INVITE) && sendAck) {
					inviteOkResponse = response;
					Request ackRequest = responseDialog.createAck(cseq.getSeqNumber());
					if (useToURIasRequestUri) {
						ackRequest.setRequestURI(requestURI);	
					}			
					if(timeToWaitBeforeAck > 0) {
						Thread.sleep(timeToWaitBeforeAck);
					}
					logger.info("Sending ACK " + ackRequest);	
					if(extraRoute) {
						Header h = protocolObjects.headerFactory.createHeader("Route", ackRequest.getRequestURI().toString());
						ackRequest.addLast(h);
					}
					if(!sendSubsequentRequestsThroughSipProvider) {
						logger.info("Sending ACK through dialog " + ackRequest);	
						responseDialog.sendAck(ackRequest);
						this.dialog = responseDialog;
					} else {
						logger.info("Sending ACK through provider " + ackRequest);	
						sipProvider.sendRequest(ackRequest);
					}
					ackSent = true;					
					Thread.sleep(1000);
					// If the caller is supposed to send the bye
					if(sendReinvite && !reinviteSent) {
						List<Header> headers = new ArrayList<Header>();
						Header reinviteHeader = protocolObjects.headerFactory.createHeader("ReInvite", "true");
						headers.add(reinviteHeader);
						if(prackSent) {							
							headers.add(protocolObjects.headerFactory.createHeader(RequireHeader.NAME, "100rel"));
						} 					
						prackSent = false;
						sendInDialogSipRequest("INVITE", null, null, null, headers, null);
						
						reinviteSent = true;
						return;
					}
					if(sendBye) {
//						Thread.sleep(30000);
						sendBye();
					}
					if(sendByeAfterTerminatingNotify) {
						responseDialog.terminateOnBye(false);
					}
				} else if(cseq.getMethod().equals(Request.BYE)) {
					okToByeReceived = true;
				} else if (cseq.getMethod().equals(Request.CANCEL)) {
					this.cancelOkReceived = true;
//					if (tid.getDialog().getState() == DialogState.CONFIRMED) {
//						// oops cancel went in too late. Need to hang up the
//						// dialog.
//						logger.info("Sending BYE -- cancel went in too late !!");
//						Request byeRequest = dialog.createRequest(Request.BYE);
//						ClientTransaction ct = sipProvider
//								.getNewClientTransaction(byeRequest);
//						tid.getDialog().sendRequest(ct);
//					} 
				} else if (cseq.getMethod().equals(Request.PUBLISH)) {
					SIPETagHeader sipTagHeader = (SIPETagHeader)response.getHeader(SIPETag.NAME);
					sipETag = sipTagHeader.getETag();
				} else if (cseq.getMethod().equals(Request.PRACK)) {
					okToPrackReceived = true;
					if (sendUpdateAfterPrack){
						Request updateRequest = dialog.createRequest(Request.UPDATE);
						
						if (recordRoutingProxyTesting){				
							ContactHeader contHdr = (ContactHeader) response.getHeader(ContactHeader.NAME);
							SipURI sipUri = (SipURI)contHdr.getAddress().getURI();
							updateRequest.setRequestURI(sipUri);
							}
						ClientTransaction ct = sipProvider
								.getNewClientTransaction(updateRequest);
						dialog.sendRequest(ct);
						sendUpdate = true;
					}
				} else if(cseq.getMethod().equals(Request.UPDATE) && sendUpdateAfterUpdate && prackReceived) {
					Address address = protocolObjects.addressFactory
					.createAddress("Shootme <sip:" + System.getProperty("org.mobicents.testsuite.testhostaddr") + ":" + myPort
							+";transport="+protocolObjects.transport
							+ ">");
					contactHeader = protocolObjects.headerFactory.createContactHeader(address);						
					setFinalResponse(protocolObjects.messageFactory
							.createResponse(finalResponseToSend, inviteRequest));					
					ToHeader toHeader = (ToHeader) getFinalResponse().getHeader(ToHeader.NAME);
					if(toHeader.getTag() == null) {
						toHeader.setTag(toTag); // Application is supposed to set.
					}
					getFinalResponse().addHeader(contactHeader);
					inviteServerTid.sendResponse(getFinalResponse());
				}
			} else if  (response.getStatusCode() == Response.MOVED_TEMPORARILY) {
				// Dialog dies as soon as you get an error response.
				this.redirectReceived = true;
				if (cseq.getMethod().equals(Request.INVITE)) {
					// lookup the contact header
					ContactHeader contHdr = (ContactHeader) response
							.getHeader(ContactHeader.NAME);
					// we can re-use the from header
					FromHeader from = ((FromHeader) response
							.getHeader(FromHeader.NAME));
					// we use the to-address, but without the tag
					ToHeader to = (ToHeader) (response.getHeader(ToHeader.NAME)).clone();
					to.removeParameter("tag");
					// the call-id can be re-used
					CallIdHeader callID = ((CallIdHeader) response
							.getHeader(CallIdHeader.NAME));
					// we take the next cseq
					long seqNo = (((CSeqHeader) response
							.getHeader(CSeqHeader.NAME)).getSeqNumber());
					logger.info("seqNo = " + seqNo);
					CSeqHeader cseqNew = protocolObjects.headerFactory
							.createCSeqHeader(++seqNo, "INVITE");
					// Create ViaHeaders (either use tcp or udp)
					ArrayList viaHeaders = new ArrayList();
					ViaHeader viaHeader = protocolObjects.headerFactory
							.createViaHeader("" + System.getProperty("org.mobicents.testsuite.testhostaddr") + "", sipProvider
									.getListeningPoint(protocolObjects.transport).getPort(), 
									protocolObjects.transport,
									null);
					// add via headers
					viaHeaders.add(viaHeader);
					// create max forwards
					MaxForwardsHeader maxForwardsHeader = protocolObjects.headerFactory
							.createMaxForwardsHeader(10);
					// create invite Request
					SipURI newUri = (SipURI)this.requestURI.clone();					
					newUri.setParameter("redirection", "true");
					requestURI = newUri;
					Request invRequest = protocolObjects.messageFactory
							.createRequest(newUri,
									"INVITE", callID, cseqNew, from, to,
									viaHeaders, maxForwardsHeader);
					// we set the Request URI to the address given
					SipURI contactURI = 
					protocolObjects.addressFactory.createSipURI(null, this.listeningPoint.getIPAddress());
					
					contactURI.setPort(this.listeningPoint.getPort());
					contactURI.setTransportParam(protocolObjects.transport);
					
					Address address = protocolObjects.addressFactory.createAddress(contactURI);
					ContactHeader contact = protocolObjects.headerFactory.createContactHeader(address);
					invRequest.addHeader(contact);
					
					// the contacat header in the response contains where to redirect
					// the request to -- which in this case happens to be back to the
					// same location.
					ContactHeader chdr = (ContactHeader)response.getHeader(ContactHeader.NAME);
					
					SipURI sipUri = (SipURI)chdr.getAddress().getURI();
//					sipUri.setLrParam();
					RouteHeader routeHeader = 
						protocolObjects.headerFactory.createRouteHeader(chdr.getAddress());
					invRequest.addHeader(routeHeader);
					invRequest.setRequestURI(sipUri);
					
					logger.info("Sending INVITE to "
							+ contHdr.getAddress().getURI().toString());
					inviteClientTid = sipProvider.getNewClientTransaction(invRequest);
					this.transactionCount++;
					
					logger.info("New TID = " + inviteClientTid);
					inviteClientTid.sendRequest();
					logger.info("sendReqeust succeeded " + inviteClientTid);
					Dialog dialog = inviteClientTid.getDialog();
					this.dialogCount ++;
					this.dialog = dialog;
				
		
				}
			} else if (response.getStatusCode() == Response.REQUEST_TERMINATED) {
				if(cseq.getMethod().equals(Request.INVITE)){
					this.requestTerminatedReceived = true;
				}
			} else if(response.getStatusCode() == Response.RINGING && sendUpdateOn180) {
				Request updateRequest = dialog.createRequest(Request.UPDATE);
								
				if (recordRoutingProxyTesting){				
					ContactHeader contHdr = (ContactHeader) response.getHeader(ContactHeader.NAME);
					SipURI sipUri = (SipURI)contHdr.getAddress().getURI();
					updateRequest.setRequestURI(sipUri);
					}
				
				
				ClientTransaction ct = sipProvider
						.getNewClientTransaction(updateRequest);
				dialog.sendRequest(ct);
				sendUpdate = true;

			} else if ((response.getStatusCode() == Response.PROXY_AUTHENTICATION_REQUIRED
					|| response.getStatusCode() == Response.UNAUTHORIZED) && handleAuthorization ) {
				URI uriReq = tid.getRequest().getRequestURI();
				Request authrequest = this.processResponseAuthorization(
						response, uriReq);
				
				if(authrequest == null) {
					System.out
						.println("INVITE AUTHORIZATION null, stopping there");
					return;
				}
				inviteClientTid.sendRequest();
				System.out
						.println("INVITE AUTHORIZATION sent:\n" + authrequest);
			} else if (response.getStatusCode() > Response.TRYING && response.getStatusCode() < Response.OK) {
				RequireHeader requireHeader = (RequireHeader) response.getHeader(RequireHeader.NAME);
				logger.debug("prackSent ? " + prackSent);
				if(requireHeader != null && "100rel".equalsIgnoreCase(requireHeader.getOptionTag().trim()) && !prackSent) {
					prackSent = true;
					Request prack = responseDialog.createPrack(response);
					ClientTransaction ct = sipProvider
						.getNewClientTransaction(prack);
					responseDialog.sendRequest(ct);					
				}
			}
			/**
			 * end of modified code
			 */
		} catch (Exception ex) {
			logger.error("An unexpected exception occured while processing the response" , ex);
		}

	}

	public long getLastInfoResponseTime() {
		return lastInfoResponseTime;
	}

	public Request processResponseAuthorization(Response response, URI uriReq) {
		RecordRouteHeader recordRouteHeader = (RecordRouteHeader)response.getHeader(RecordRouteHeader.NAME);
		if(!recordRoutingProxyTesting && recordRouteHeader != null) {
			abortProcessing = true;
			throw new IllegalArgumentException("we received a record route header in a  response !");			
		}
		Request requestauth = null;
		this.authenticationErrorReceived = true;
		try {
			System.out.println("processResponseAuthorization()");
			
			requestauth = (Request) inviteClientTid.getRequest().clone();
			
			CSeqHeader cSeq = (CSeqHeader) requestauth.getHeader((CSeqHeader.NAME));
			try {
				cSeq.setSeqNumber(cSeq.getSeqNumber() + 1l);
			} catch (InvalidArgumentException e) {
				logger.error("Cannot increment the Cseq header to the new INVITE request",e);
				throw new IllegalArgumentException("Cannot create the INVITE request",e);				
			}
			// non regression test for http://code.google.com/p/sipservlets/issues/detail?id=88
			if((securityUser.equalsIgnoreCase("badUser") || securityPwd.equalsIgnoreCase("badpwd")) && cSeq.getSeqNumber() > 2) {
				return null;
			}
			requestauth.setHeader(cSeq);
			requestauth.removeHeader(ViaHeader.NAME);
			// Create ViaHeaders
			ViaHeader viaHeader = protocolObjects.headerFactory
					.createViaHeader("" + System.getProperty("org.mobicents.testsuite.testhostaddr") + "", sipProvider
							.getListeningPoint(protocolObjects.transport).getPort(), protocolObjects.transport,
							null);
			// add via headers
			requestauth.addHeader(viaHeader);
			
			try {
				ClientTransaction retryTran = sipProvider
					.getNewClientTransaction(requestauth);
				inviteClientTid = retryTran;		
				
				
				dialog = retryTran.getDialog();
				if (dialog == null) {					
					dialog = sipProvider.getNewDialog(retryTran);
				}
			} catch (TransactionUnavailableException e) {
				logger.error("Cannot get a new transaction for the request " + requestauth,e);
				throw new IllegalArgumentException("Cannot get a new transaction for the request " + requestauth,e);
			}
			AuthorizationHeader authorization = new org.mobicents.servlet.sip.catalina.security.authentication.DigestAuthenticator(protocolObjects.headerFactory).getAuthorizationHeader(
					((CSeqHeader) response
							.getHeader(CSeqHeader.NAME)).getMethod(),
							uriReq.toString(),
					"", // TODO: What is this entity-body?
					((WWWAuthenticate) (response
							.getHeader(SIPHeaderNames.WWW_AUTHENTICATE))),
					securityUser,
					securityPwd,
					((WWWAuthenticate) (response
							.getHeader(SIPHeaderNames.WWW_AUTHENTICATE))).getNonce(),
					1);
			
			requestauth.addHeader(authorization);
		} catch (ParseException pa) {
			System.out
					.println("processResponseAuthorization() ParseException:");
			System.out.println(pa.getMessage());
			pa.printStackTrace();
		} catch (Exception ex) {
			System.out.println("processResponseAuthorization() Exception:");
			System.out.println(ex.getMessage());
			ex.printStackTrace();
		}
		return requestauth;
	}
	
	public Request createInvite(String callId, long cseq) throws ParseException,
			InvalidArgumentException {
		String fromName = "BigGuy";
		String fromSipAddress = "here.com";
		String fromDisplayName = "The Master Blaster";
		
		String toSipAddress = "there.com";
		String toUser = "LittleGuy";
		String toDisplayName = "The Little Blister";
		
		// create >From Header
		SipURI fromAddress = protocolObjects.addressFactory.createSipURI(fromName,
				fromSipAddress);
		
		Address fromNameAddress = protocolObjects.addressFactory.createAddress(fromAddress);
		fromNameAddress.setDisplayName(fromDisplayName);
		FromHeader fromHeader = protocolObjects.headerFactory.createFromHeader(fromNameAddress,
				"12345");
		
		// create To Header
		SipURI toAddress = protocolObjects.addressFactory.createSipURI(toUser, toSipAddress);
		Address toNameAddress = protocolObjects.addressFactory.createAddress(toAddress);
		toNameAddress.setDisplayName(toDisplayName);
		ToHeader toHeader = protocolObjects.headerFactory.createToHeader(toNameAddress, null);
		
		// create Request URI
		SipURI requestURI = protocolObjects.addressFactory.createSipURI(toUser, peerHostPort);
		
		// Create ViaHeaders
		ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
		ViaHeader viaHeader = protocolObjects.headerFactory.createViaHeader("" + System.getProperty("org.mobicents.testsuite.testhostaddr") + "",
				listeningPoint.getPort(), listeningPoint.getTransport(),
				null);
		// add via headers
		viaHeaders.add(viaHeader);
		
		// Create ContentTypeHeader
		ContentTypeHeader contentTypeHeader = protocolObjects.headerFactory
				.createContentTypeHeader("application", "sdp");
		
		// Create a new CallId header
		CallIdHeader callIdHeader;
		callIdHeader = sipProvider.getNewCallId();
		if (callId.trim().length() > 0)
			callIdHeader.setCallId(callId);
		
		// Create a new Cseq header
		CSeqHeader cSeqHeader = protocolObjects.headerFactory.createCSeqHeader(cseq,
				Request.INVITE);
		
		// Create a new MaxForwardsHeader
		MaxForwardsHeader maxForwards = protocolObjects.headerFactory
				.createMaxForwardsHeader(70);
		
		// Create the request.
		Request request = protocolObjects.messageFactory.createRequest(requestURI,
				Request.INVITE, callIdHeader, cSeqHeader, fromHeader, toHeader,
				viaHeaders, maxForwards);
		// Create contact headers
		String host = "" + System.getProperty("org.mobicents.testsuite.testhostaddr") + "";
		
		SipURI contactUrl = protocolObjects.addressFactory.createSipURI(fromName, host);
		contactUrl.setPort(listeningPoint.getPort());
		
		// Create the contact name address.
		SipURI contactURI = protocolObjects.addressFactory.createSipURI(fromName, host);
		contactURI.setPort(listeningPoint.getPort());
		
		Address contactAddress = protocolObjects.addressFactory.createAddress(contactURI);
		
		// Add the contact address.
		contactAddress.setDisplayName(fromName);
		
		contactHeader = protocolObjects.headerFactory.createContactHeader(contactAddress);
		request.addHeader(contactHeader);
		
		String sdpData = "v=0\r\n"
				+ "o=4855 13760799956958020 13760799956958020"
				+ " IN IP4  129.6.55.78\r\n" + "s=mysession session\r\n"
				+ "p=+46 8 52018010\r\n" + "c=IN IP4  129.6.55.78\r\n"
				+ "t=0 0\r\n" + "m=audio 6022 RTP/AVP 0 4 18\r\n"
				+ "a=rtpmap:0 PCMU/8000\r\n" + "a=rtpmap:4 G723/8000\r\n"
				+ "a=rtpmap:18 G729A/8000\r\n" + "a=ptime:20\r\n";
		byte[] contents = sdpData.getBytes();
		
		request.setContent(contents, contentTypeHeader);
		
		Header callInfoHeader = protocolObjects.headerFactory.createHeader("Call-Info",
				"<http://www.antd.nist.gov>");
		request.addHeader(callInfoHeader);
		
		return request;
	}
	
	/**
	 * @throws SipException
	 * @throws TransactionUnavailableException
	 * @throws TransactionDoesNotExistException
	 * @throws InterruptedException 
	 */
	public void sendBye() throws SipException,
			TransactionUnavailableException, TransactionDoesNotExistException, InterruptedException {
		if(timeToWaitBeforeBye > 0) {
			Thread.sleep(timeToWaitBeforeBye);
		}
		sendBye(this.dialog);
	}		
	
	/**
	 * @throws SipException
	 * @throws TransactionUnavailableException
	 * @throws TransactionDoesNotExistException
	 */
	public void sendBye(final Dialog dialog) throws SipException,
			TransactionUnavailableException, TransactionDoesNotExistException {
		Thread th = new Thread(){
			public void run() {
				try {
					if(sendByeInNewThread) Thread.sleep(600);
					Request byeRequest = dialog.createRequest(Request.BYE);
					URI uri = ((FromHeader)byeRequest.getHeader(FromHeader.NAME)).getAddress().getURI();
					if(uri.isSipURI()) {
						((SipURI)uri).removeParameter("fromParam"); 
					}
					ClientTransaction ct = sipProvider.getNewClientTransaction(byeRequest);
					logger.info("Sending BYE " + byeRequest);
					if(!sendSubsequentRequestsThroughSipProvider) {
						dialog.sendRequest(ct);
					} else {
						sipProvider.sendRequest(byeRequest);
					}
					byeSent = true;	
				} catch(Exception e)  {e.printStackTrace();}
			}
		};
		if(sendByeInNewThread) {
			th.start();
		} else {
			th.run();
		}

	}

	public void processTimeout(javax.sip.TimeoutEvent timeoutEvent) {

		logger.info("Transaction Time out");
	}

	

	public SipProvider createProvider() throws Exception {
		logger.info("Shootist: createProvider()");
		listeningPoint = protocolObjects.sipStack.createListeningPoint(
				"" + System.getProperty("org.mobicents.testsuite.testhostaddr") + "", myPort, protocolObjects.transport);
		this.sipProvider = protocolObjects.sipStack
				.createSipProvider(listeningPoint);
		return sipProvider;

	}
	
	public void addListeningPoint(String ipAddress, int port, String transport) throws Exception {
		logger.info("Shootist: addListeningPoint()");
		ListeningPoint listeningPoint = protocolObjects.sipStack.createListeningPoint(
				"" + System.getProperty("org.mobicents.testsuite.testhostaddr") + "", port, transport);
		sipProvider.addListeningPoint(listeningPoint);
	}

	public Request sendSipRequest(String method, URI fromURI, URI toURI, String messageContent, SipURI route, boolean useToURIasRequestUri) throws SipException, ParseException, InvalidArgumentException {
		return sendSipRequest(method, fromURI, toURI, messageContent, route, useToURIasRequestUri, null, null, true);
	}

	public Request sendSipRequest(String method, URI fromURI, URI toURI, String messageContent, SipURI route, boolean useToURIasRequestUri, String[] headerNames, String[] headerContents, boolean setHeader) throws SipException, ParseException, InvalidArgumentException {
		this.useToURIasRequestUri = useToURIasRequestUri;
		// create >From Header
		Address fromNameAddress = protocolObjects.addressFactory
				.createAddress(fromURI);			
		FromHeader fromHeader = protocolObjects.headerFactory
				.createFromHeader(fromNameAddress, Integer.toString(new Random().nextInt(10000000)));

		// create To Header			
		Address toNameAddress = protocolObjects.addressFactory
				.createAddress(toURI);			
		ToHeader toHeader = protocolObjects.headerFactory.createToHeader(
				toNameAddress, null);

		if(toURI instanceof SipURI) {
			SipURI toSipUri = (SipURI) toURI;
			// create Request URI
			this.requestURI = protocolObjects.addressFactory.createSipURI(
					toSipUri.getUser(), peerHostPort);
			((SipURI)this.requestURI).setPort(peerPort);
			if(setTransport) {
				((SipURI)this.requestURI).setTransportParam(listeningPoint.getTransport());
			}
		}
		if(useToURIasRequestUri || toURI instanceof TelURL) {
			this.requestURI = toURI;
		}
		
		// Create ViaHeaders

		List<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
		ViaHeader viaHeader = protocolObjects.headerFactory
				.createViaHeader("" + System.getProperty("org.mobicents.testsuite.testhostaddr") + "", sipProvider
						.getListeningPoint(protocolObjects.transport).getPort(), listeningPoint.getTransport(),
						null);
		if(rfc5626UseCase != null && (rfc5626UseCase == RFC5626UseCase.B2BUA || rfc5626UseCase == RFC5626UseCase.Proxy)) {
			//try to bind to non existing IP
			 viaHeader = protocolObjects.headerFactory
				.createViaHeader("192.192.192.192", sipProvider
						.getListeningPoint(protocolObjects.transport).getPort(), listeningPoint.getTransport(),
						null);
		}
		// add via headers
		viaHeaders.add(viaHeader);

		// Create ContentTypeHeader
//			ContentTypeHeader contentTypeHeader = protocolObjects.headerFactory
//					.createContentTypeHeader("application", "sdp");

		// Create a new CallId header
		CallIdHeader callIdHeader = sipProvider.getNewCallId();

		// Create a new Cseq header
		CSeqHeader cSeqHeader = protocolObjects.headerFactory
				.createCSeqHeader(1L, method);

		// Create a new MaxForwardsHeader
		MaxForwardsHeader maxForwards = protocolObjects.headerFactory
				.createMaxForwardsHeader(70);

		// Create the request.
		Request request = protocolObjects.messageFactory.createRequest(
				requestURI, method, callIdHeader, cSeqHeader,
				fromHeader, toHeader, viaHeaders, maxForwards);
		// Create contact headers
		String host = "" + System.getProperty("org.mobicents.testsuite.testhostaddr") + "";
		if(rfc5626UseCase != null && (rfc5626UseCase == RFC5626UseCase.B2BUA || rfc5626UseCase == RFC5626UseCase.Proxy)) {
			//try to bind to non existing IP
			 host = "192.192.192.192";
		}
		request.setHeader(protocolObjects.headerFactory.createHeader("REM", "RRRREM"));
		URI contactUrl = null;
		if(fromURI instanceof SipURI) {
			contactUrl = protocolObjects.addressFactory.createSipURI(
				((SipURI)fromURI).getUser(), host);
			/**
			 * either use tcp or udp
			 */
			((SipURI)contactUrl).setPort(listeningPoint.getPort());
			if(setTransport) {
				((SipURI)contactUrl).setTransportParam(listeningPoint.getTransport());		
				if(rfc5626UseCase == null) {
					((SipURI)contactUrl).setLrParam();
				}
			}
			if(rfc5626UseCase != null && (rfc5626UseCase == RFC5626UseCase.B2BUA || rfc5626UseCase == RFC5626UseCase.Proxy)) {
				((SipURI)contactUrl).setParameter("ob", null);
			}
		} else {
			contactUrl = fromURI;
		}
		
		// Create the contact name address.	
		Address contactAddress = protocolObjects.addressFactory
				.createAddress(contactUrl);

		// Add the contact address.
//			contactAddress.setDisplayName(fromName);

		contactHeader = protocolObjects.headerFactory
				.createContactHeader(contactAddress);
		if (rfc5626UseCase != null && rfc5626UseCase == RFC5626UseCase.Proxy) {
			contactHeader.setParameter("reg-id", "1");
			contactHeader.setParameter("+sip.instance", "\"<urn:uuid:00000000-0000-1000-8000-AABBCCDDEEFF>\"");
		}
		request.addHeader(contactHeader);
		
		SipURI uri = protocolObjects.addressFactory.createSipURI(null, "" + System.getProperty("org.mobicents.testsuite.testhostaddr") + "");
		
		uri.setLrParam();
		uri.setTransportParam(protocolObjects.transport);
		uri.setPort(this.peerPort);
		
		if(route != null) {
			Address address = protocolObjects.addressFactory.createAddress(route);
			RouteHeader routeHeader = protocolObjects.headerFactory.createRouteHeader(address);
			request.addHeader(routeHeader);
		} else if(useDefaultRoute ) {
			Address address = protocolObjects.addressFactory.createAddress(uri);
			RouteHeader routeHeader = protocolObjects.headerFactory.createRouteHeader(address);
			request.addHeader(routeHeader);
		}

		// set the message content
		if(messageContent != null) {
			ContentLengthHeader contentLengthHeader = 
				protocolObjects.headerFactory.createContentLengthHeader(messageContent.length());
			ContentTypeHeader contentTypeHeader = 
				protocolObjects.headerFactory.createContentTypeHeader(TEXT_CONTENT_TYPE,PLAIN_UTF8_CONTENT_SUBTYPE);
			byte[] contents = messageContent.getBytes();
			request.setContent(contents, contentTypeHeader);
			request.setContentLength(contentLengthHeader);
		}
		
		if(headerNames != null) {
			for(int q=0; q<headerNames.length; q++) {
				Header h = null;
				if(headerNames[q].equalsIgnoreCase(AllowHeader.NAME)) {
					h = protocolObjects.headerFactory.createAllowHeader(headerContents[q]);
				} else {
					h = protocolObjects.headerFactory.createHeader(headerNames[q], headerContents[q]);
				}
				if(setHeader) {
					request.setHeader(h);
				} else {
					request.addHeader(h);
				}
			}
		}
		addSpecificHeaders(method, request);
		// Create the client transaction.
		inviteClientTid = sipProvider.getNewClientTransaction(request);
		// send the request out.
		inviteClientTid.sendRequest();

		this.transactionCount ++;
		
		logger.info("client tx = " + inviteClientTid);
		if(!Request.MESSAGE.equalsIgnoreCase(method)) {
			dialog = inviteClientTid.getDialog();
		}
		if(Request.INVITE.equalsIgnoreCase(method)) {
			inviteRequest = request;
		}
		this.dialogCount++;
		return inviteRequest;
	}
	
	public Request sendSipRequest(String method, URI fromURI, URI toURI, String messageContent, SipURI route, boolean useToURIasRequestUri, String[] headerNames, String[] headerContents, SipURI requestUri) throws SipException, ParseException, InvalidArgumentException {
		this.useToURIasRequestUri = useToURIasRequestUri;
		// create >From Header
		Address fromNameAddress = protocolObjects.addressFactory
				.createAddress(fromURI);			
		FromHeader fromHeader = protocolObjects.headerFactory
				.createFromHeader(fromNameAddress, Integer.toString(new Random().nextInt(10000000)));

		// create To Header			
		Address toNameAddress = protocolObjects.addressFactory
				.createAddress(toURI);			
		ToHeader toHeader = protocolObjects.headerFactory.createToHeader(
				toNameAddress, null);

		this.requestURI = requestUri;
		
		// Create ViaHeaders

		List<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
		ViaHeader viaHeader = protocolObjects.headerFactory
				.createViaHeader("" + System.getProperty("org.mobicents.testsuite.testhostaddr") + "", sipProvider
						.getListeningPoint(protocolObjects.transport).getPort(), listeningPoint.getTransport(),
						null);

		// add via headers
		viaHeaders.add(viaHeader);

		// Create ContentTypeHeader
//			ContentTypeHeader contentTypeHeader = protocolObjects.headerFactory
//					.createContentTypeHeader("application", "sdp");

		// Create a new CallId header
		CallIdHeader callIdHeader = sipProvider.getNewCallId();

		// Create a new Cseq header
		CSeqHeader cSeqHeader = protocolObjects.headerFactory
				.createCSeqHeader(1L, method);

		// Create a new MaxForwardsHeader
		MaxForwardsHeader maxForwards = protocolObjects.headerFactory
				.createMaxForwardsHeader(70);

		// Create the request.
		Request request = protocolObjects.messageFactory.createRequest(
				requestURI, method, callIdHeader, cSeqHeader,
				fromHeader, toHeader, viaHeaders, maxForwards);
		// Create contact headers
		String host = "" + System.getProperty("org.mobicents.testsuite.testhostaddr") + "";

		URI contactUrl = null;
		if(fromURI instanceof SipURI) {
			contactUrl = protocolObjects.addressFactory.createSipURI(
				((SipURI)fromURI).getUser(), host);
			/**
			 * either use tcp or udp
			 */
			((SipURI)contactUrl).setPort(listeningPoint.getPort());
			((SipURI)contactUrl).setTransportParam(listeningPoint.getTransport());		
			((SipURI)contactUrl).setLrParam();
		} else {
			contactUrl = fromURI;
		}
		
		// Create the contact name address.	
		Address contactAddress = protocolObjects.addressFactory
				.createAddress(contactUrl);

		// Add the contact address.
//			contactAddress.setDisplayName(fromName);

		contactHeader = protocolObjects.headerFactory
				.createContactHeader(contactAddress);
		request.addHeader(contactHeader);
		
		SipURI uri = protocolObjects.addressFactory.createSipURI(null, "" + System.getProperty("org.mobicents.testsuite.testhostaddr") + "");
		
		uri.setLrParam();
		uri.setTransportParam(protocolObjects.transport);
		uri.setPort(this.peerPort);
		
		if(route != null) {
			Address address = protocolObjects.addressFactory.createAddress(route);
			RouteHeader routeHeader = protocolObjects.headerFactory.createRouteHeader(address);
			request.addHeader(routeHeader);
		} else {
			Address address = protocolObjects.addressFactory.createAddress(uri);
			RouteHeader routeHeader = protocolObjects.headerFactory.createRouteHeader(address);
			request.addHeader(routeHeader);
		}

		// set the message content
		if(messageContent != null) {
			ContentLengthHeader contentLengthHeader = 
				protocolObjects.headerFactory.createContentLengthHeader(messageContent.length());
			ContentTypeHeader contentTypeHeader = 
				protocolObjects.headerFactory.createContentTypeHeader(TEXT_CONTENT_TYPE,PLAIN_UTF8_CONTENT_SUBTYPE);
			byte[] contents = messageContent.getBytes();
			request.setContent(contents, contentTypeHeader);
			request.setContentLength(contentLengthHeader);
		}
		
		if(headerNames != null) {
			for(int q=0; q<headerNames.length; q++) {
				Header h = protocolObjects.headerFactory.createHeader(headerNames[q], headerContents[q]);
				request.addLast(h);
			}
		}
		addSpecificHeaders(method, request);
		// Create the client transaction.
		inviteClientTid = sipProvider.getNewClientTransaction(request);
		// send the request out.
		inviteClientTid.sendRequest();

		this.transactionCount ++;
		
		logger.info("client tx = " + inviteClientTid);
		if(!Request.MESSAGE.equalsIgnoreCase(method)) {
			dialog = inviteClientTid.getDialog();
		}
		this.dialogCount++;
		return request;
	}
	
	public Request createSipRequest(String method, URI fromURI, URI toURI, String messageContent, SipURI route, boolean useToURIasRequestUri, String[] headerNames, String[] headerContents, SipURI requestUri) throws SipException, ParseException, InvalidArgumentException {
		this.useToURIasRequestUri = useToURIasRequestUri;
		// create >From Header
		Address fromNameAddress = protocolObjects.addressFactory
				.createAddress(fromURI);			
		FromHeader fromHeader = protocolObjects.headerFactory
				.createFromHeader(fromNameAddress, Integer.toString(new Random().nextInt(10000000)));

		// create To Header			
		Address toNameAddress = protocolObjects.addressFactory
				.createAddress(toURI);			
		ToHeader toHeader = protocolObjects.headerFactory.createToHeader(
				toNameAddress, null);

		this.requestURI = requestUri;
		
		// Create ViaHeaders

		List<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
		ViaHeader viaHeader = protocolObjects.headerFactory
				.createViaHeader("" + System.getProperty("org.mobicents.testsuite.testhostaddr") + "", sipProvider
						.getListeningPoint(protocolObjects.transport).getPort(), listeningPoint.getTransport(),
						null);

		// add via headers
		viaHeaders.add(viaHeader);

		// Create ContentTypeHeader
//			ContentTypeHeader contentTypeHeader = protocolObjects.headerFactory
//					.createContentTypeHeader("application", "sdp");

		// Create a new CallId header
		CallIdHeader callIdHeader = sipProvider.getNewCallId();

		// Create a new Cseq header
		CSeqHeader cSeqHeader = protocolObjects.headerFactory
				.createCSeqHeader(1L, method);

		// Create a new MaxForwardsHeader
		MaxForwardsHeader maxForwards = protocolObjects.headerFactory
				.createMaxForwardsHeader(70);

		// Create the request.
		Request request = protocolObjects.messageFactory.createRequest(
				requestURI, method, callIdHeader, cSeqHeader,
				fromHeader, toHeader, viaHeaders, maxForwards);
		// Create contact headers
		String host = "" + System.getProperty("org.mobicents.testsuite.testhostaddr") + "";

		URI contactUrl = null;
		if(fromURI instanceof SipURI) {
			contactUrl = protocolObjects.addressFactory.createSipURI(
				((SipURI)fromURI).getUser(), host);
			/**
			 * either use tcp or udp
			 */
			((SipURI)contactUrl).setPort(listeningPoint.getPort());
			((SipURI)contactUrl).setTransportParam(listeningPoint.getTransport());		
			((SipURI)contactUrl).setLrParam();
		} else {
			contactUrl = fromURI;
		}
		
		// Create the contact name address.	
		Address contactAddress = protocolObjects.addressFactory
				.createAddress(contactUrl);

		// Add the contact address.
//			contactAddress.setDisplayName(fromName);

		contactHeader = protocolObjects.headerFactory
				.createContactHeader(contactAddress);
		request.addHeader(contactHeader);
		
		SipURI uri = protocolObjects.addressFactory.createSipURI(null, "" + System.getProperty("org.mobicents.testsuite.testhostaddr") + "");
		
		uri.setLrParam();
		uri.setTransportParam(protocolObjects.transport);
		uri.setPort(this.peerPort);
		
		if(route != null) {
			Address address = protocolObjects.addressFactory.createAddress(route);
			RouteHeader routeHeader = protocolObjects.headerFactory.createRouteHeader(address);
			request.addHeader(routeHeader);
		} else {
			Address address = protocolObjects.addressFactory.createAddress(uri);
			RouteHeader routeHeader = protocolObjects.headerFactory.createRouteHeader(address);
			request.addHeader(routeHeader);
		}

		// set the message content
		if(messageContent != null) {
			ContentLengthHeader contentLengthHeader = 
				protocolObjects.headerFactory.createContentLengthHeader(messageContent.length());
			ContentTypeHeader contentTypeHeader = 
				protocolObjects.headerFactory.createContentTypeHeader(TEXT_CONTENT_TYPE,PLAIN_UTF8_CONTENT_SUBTYPE);
			byte[] contents = messageContent.getBytes();
			request.setContent(contents, contentTypeHeader);
			request.setContentLength(contentLengthHeader);
		}
		
		if(headerNames != null) {
			for(int q=0; q<headerNames.length; q++) {
				Header h = protocolObjects.headerFactory.createHeader(headerNames[q], headerContents[q]);
				request.addLast(h);
			}
		}
		addSpecificHeaders(method, request);
		return request;
	}

	public Request sendRequet(Request request) {
		try {
			// Create the client transaction.
			inviteClientTid = sipProvider.getNewClientTransaction(request);
			// send the request out.
			inviteClientTid.sendRequest();

			this.transactionCount ++;

			this.dialogCount++;

		} catch (Exception e) {

		}
		return request;
	}

	private void addSpecificHeaders(String method, Request request)
			throws ParseException, InvalidArgumentException {
		if(Request.SUBSCRIBE.equals(method) || Request.PUBLISH.equals(method)) {
			// Create an event header for the subscription.
			EventHeader eventHeader = protocolObjects.headerFactory.createEventHeader(publishEvent);				
			request.addHeader(eventHeader);
			ExpiresHeader expires = protocolObjects.headerFactory.createExpiresHeader(200);
			request.addHeader(expires);			
		}
		if(Request.PUBLISH.equals(method)) {
			if(sipETag != null) {
				SIPIfMatchHeader sipIfMatchHeader = protocolObjects.headerFactory.createSIPIfMatchHeader(sipETag);
				request.addHeader(sipIfMatchHeader);
			}
			if(publishContentMessage != null) {
				ContentLengthHeader contentLengthHeader = 
					protocolObjects.headerFactory.createContentLengthHeader(publishContentMessage.length());
				ContentTypeHeader contentTypeHeader = 
					protocolObjects.headerFactory.createContentTypeHeader(APPLICATION_CONTENT_TYPE,PIDF_XML_SUBTYPE);
				request.setContentLength(contentLengthHeader);
				request.setContent(publishContentMessage, contentTypeHeader);
			}
		}
		if(Request.REFER.equals(method)) {
			ReferToHeader referToHeader = (ReferToHeader) protocolObjects.headerFactory.createHeader(ReferToHeader.NAME, "sip:refer-to@nist.gov");
			request.addHeader(referToHeader);
		}
	}
	
	public TestSipListener (int myPort, int peerPort, ProtocolObjects protocolObjects, boolean callerSendBye) {
		this.protocolObjects = protocolObjects;		
		this.myPort = myPort;
		if(peerPort > 0) {
			this.peerPort = peerPort;
			this.peerHostPort = "" + System.getProperty("org.mobicents.testsuite.testhostaddr") + ":"+ peerPort;
		}
		this.sendBye = callerSendBye;
		allMessagesContent = new ArrayList<String>();
		allSubscriptionStates = new ArrayList<String>();
		finalResponseToSend = Response.OK;
		provisionalResponsesToSend = new ArrayList<Integer>();
		provisionalResponsesToSend.add(Response.TRYING);
		provisionalResponsesToSend.add(Response.RINGING);
	}

	public void processIOException(IOExceptionEvent exceptionEvent) {
		logger.info("IOException happened for "
				+ exceptionEvent.getHost() + " port = "
				+ exceptionEvent.getPort());

	}

	public void processTransactionTerminated(
			TransactionTerminatedEvent transactionTerminatedEvent) {
		logger.info("Transaction terminated event recieved for " + 
				transactionTerminatedEvent.getClientTransaction());
		this.transctionTerminatedCount++;
	}

	public void processDialogTerminated(
			DialogTerminatedEvent dialogTerminatedEvent) {
		this.dialogTerminatedCount++;

	}

	public boolean getOkToByeReceived() {
		return okToByeReceived;
	}
	
	public boolean getByeReceived() {
		return byeReceived;
	}

	public void sendCancel() {
		try {
			logger.info("Sending cancel");
			
			Request cancelRequest = inviteClientTid.createCancel();
			ClientTransaction cancelTid = sipProvider
					.getNewClientTransaction(cancelRequest);
			cancelTid.sendRequest();
			cancelSent = true;
		} catch (Exception ex) {
			ex.printStackTrace();						
		}
	}

	/**
	 * @return the cancelReceived
	 */
	public boolean isCancelReceived() {
		return cancelReceived;
	}

	/**
	 */
	public void setCancelReceived(boolean cancelReceived) {
		this.cancelReceived = cancelReceived;
	}
	
	/**
	 * @return the cancelOkReceived
	 */
	public boolean isCancelOkReceived() {
		return cancelOkReceived;
	}
	
	/**
	 */
	public void setCancelOkReceived(boolean cancelOkReceived) {
		this.cancelOkReceived = cancelOkReceived;
	}

	/**
	 * @return the requestTerminatedReceived
	 */
	public boolean isRequestTerminatedReceived() {
		return requestTerminatedReceived;
	}
	
	/**
	 * @return the requestTerminatedReceived
	 */
	public void setRequestTerminatedReceived(boolean requestTerminatedReceived) {
		this.requestTerminatedReceived = requestTerminatedReceived;
	}

	/**
	 * @return the waitForCancel
	 */
	public boolean isWaitForCancel() {
		return waitForCancel;
	}

	public long getLastRegisterCSeqNumber() {
		return lastRegisterCSeqNumber;
	}

	/**
	 * @param waitForCancel the waitForCancel to set
	 */
	public void setWaitForCancel(boolean waitForCancel) {
		this.waitForCancel = waitForCancel;
	}

	/**
	 * @return the ackSent
	 */
	public boolean isAckSent() {
		return ackSent;
	}

	public void setAckSent(boolean ackSent) {
		this.ackSent = ackSent;
	}

	/**
	 * @return the ackReceived
	 */
	public boolean isAckReceived() {
		return ackReceived;
	}

	public void setAckReceived(boolean ackReceived) {
		this.ackReceived = ackReceived;
	}

	/**
	 * 
	 * @param transport TODO
	 * @param messageToSend
	 * @throws SipException
	 * @throws InvalidArgumentException
	 * @throws ParseException
	 */ 
	public void sendInDialogSipRequest(String method, String content, String contentType, String subContentType, List<Header> headers, String transport) throws SipException, InvalidArgumentException, ParseException {
		
		Request message = dialog.createRequest(method);
		if(transport !=null) {
			((SipURI)message.getRequestURI()).setTransportParam(transport);
		}

		if(content != null) {
			ContentLengthHeader contentLengthHeader = 
				protocolObjects.headerFactory.createContentLengthHeader(content.length());
			ContentTypeHeader contentTypeHeader = 
				protocolObjects.headerFactory.createContentTypeHeader(contentType,subContentType);
			message.setContentLength(contentLengthHeader);
			message.setContent(content, contentTypeHeader);
		}
		
		if(headers != null) {
			for (Header header : headers) {
				message.addHeader(header);
			}
		}
		
		addSpecificHeaders(method, message);
		message.removeHeader(ViaHeader.NAME);
		logger.info("in dialog message = " + message);
		
		ClientTransaction clientTransaction = sipProvider.getNewClientTransaction(message);
		if(method.equals("INVITE")) {
			inviteClientTid = clientTransaction;
		}
		dialog.sendRequest(clientTransaction);
	}
	
	/**
	 * 
	 * @param messageToSend
	 * @throws SipException
	 * @throws InvalidArgumentException
	 * @throws ParseException
	 */ 
	public void sendMessageInDialog(String messageToSend) throws SipException, InvalidArgumentException, ParseException {		
		Request message = dialog.createRequest(Request.MESSAGE);
		ContentLengthHeader contentLengthHeader = 
			protocolObjects.headerFactory.createContentLengthHeader(messageToSend.length());
		ContentTypeHeader contentTypeHeader = 
			protocolObjects.headerFactory.createContentTypeHeader(TEXT_CONTENT_TYPE,PLAIN_UTF8_CONTENT_SUBTYPE);
		message.setContentLength(contentLengthHeader);
		message.setContent(messageToSend, contentTypeHeader);
		ClientTransaction clientTransaction = sipProvider.getNewClientTransaction(message);
		dialog.sendRequest(clientTransaction);
	}
	
	/**
	 * 
	 * @param messageToSend
	 * @throws SipException
	 * @throws InvalidArgumentException
	 * @throws ParseException
	 */ 
	public void sendMessageNoDialog(String messageToSend) throws SipException, InvalidArgumentException, ParseException {		
		Request message = dialog.createRequest(Request.MESSAGE);
		ContentLengthHeader contentLengthHeader = 
			protocolObjects.headerFactory.createContentLengthHeader(messageToSend.length());
		ContentTypeHeader contentTypeHeader = 
			protocolObjects.headerFactory.createContentTypeHeader(TEXT_CONTENT_TYPE,PLAIN_UTF8_CONTENT_SUBTYPE);
		message.setContentLength(contentLengthHeader);
		message.setContent(messageToSend, contentTypeHeader);
		ClientTransaction clientTransaction = sipProvider.getNewClientTransaction(message);
		dialog.sendRequest(clientTransaction);
	}

	/**
	 * @return the lastMessageContent
	 */
	public String getLastMessageContent() {
		return lastMessageContent;
	}

	/**
	 * @return the allMessagesContent
	 */
	public List<String> getAllMessagesContent() {
		return allMessagesContent;
	}

	/**
	 * @return the finalResponseReceived
	 */
	public boolean isFinalResponseReceived() {
		return finalResponseReceived;
	}

	public boolean isServerErrorReceived() {
		return serverErrorReceived;
	}

	public void setServerErrorReceived(boolean serverErrorReceived) {
		this.serverErrorReceived = serverErrorReceived;
	}

	/**
	 * @return the finalResponseToSend
	 */
	public int getFinalResponseToSend() {
		return finalResponseToSend;
	}

	/**
	 * @param finalResponseToSend the finalResponseToSend to set
	 */
	public void setFinalResponseToSend(int finalResponseToSend) {
		this.finalResponseToSend = finalResponseToSend;
	}

	/**
	 * @return the provisionalResponsesToSend
	 */
	public List<Integer> getProvisionalResponsesToSend() {
		return provisionalResponsesToSend;
	}

	/**
	 * @param provisionalResponsesToSend the provisionalResponsesToSend to set
	 */
	public void setProvisionalResponsesToSend(
			List<Integer> provisionalResponsesToSend) {
		this.provisionalResponsesToSend = provisionalResponsesToSend;
	}

	/**
	 * @return the allSubscriptionState
	 */
	public List<String> getAllSubscriptionState() {
		return allSubscriptionStates;
	}

	/**
	 * @param byeReceived the byeReceived to set
	 */
	public void setByeReceived(boolean byeReceived) {
		this.byeReceived = byeReceived;
	}

	/**
	 * @param okToByeReceived the okToByeReceived to set
	 */
	public void setOkToByeReceived(boolean okToByeReceived) {
		this.okToByeReceived = okToByeReceived;
	}

	public void setSendUpdateOn180(boolean sendUpdateOn180) {
		this.sendUpdateOn180 = sendUpdateOn180;
	}

	public void setPublishEvent(String publishEvent) {
		this.publishEvent  = publishEvent;
	}

	public void setPublishContentMessage(String publishContentMessage) {
		this.publishContentMessage = publishContentMessage;		
	}

	public void setChallengeRequests(boolean challengeRequests) {
		this.challengeRequests = challengeRequests; 
	}

	/**
	 * @param timeToWaitBetweenProvisionnalResponse the timeToWaitBetweenProvisionnalResponse to set
	 */
	public void setTimeToWaitBetweenProvisionnalResponse(
			long timeToWaitBetweenProvisionnalResponse) {
		this.timeToWaitBetweenProvisionnalResponse = timeToWaitBetweenProvisionnalResponse;
	}
	
	public Response getInviteOkResponse() {
		return inviteOkResponse;
	}

	/**
	 * @return the timeToWaitBetweenProvisionnalResponse
	 */
	public long getTimeToWaitBetweenProvisionnalResponse() {
		return timeToWaitBetweenProvisionnalResponse;
	}

	public long getTimeToWaitBetweenSubsNotify() {
		return timeToWaitBetweenSubsNotify;
	}

	public void setTimeToWaitBetweenSubsNotify(long timeToWaitBetweenSubsNotify) {
		this.timeToWaitBetweenSubsNotify = timeToWaitBetweenSubsNotify;
	}

	/**
	 * @return the sendBye
	 */
	public boolean isSendBye() {
		return sendBye;
	}

	/**
	 * @param sendBye the sendBye to set
	 */
	public void setSendBye(boolean sendBye) {
		this.sendBye = sendBye;
	}

	/**
	 * @param sendByeBeforeTerminatingNotify the sendByeBeforeTerminatingNotify to set
	 */
	public void setSendByeBeforeTerminatingNotify(
			boolean sendByeBeforeTerminatingNotify) {
		this.sendByeBeforeTerminatingNotify = sendByeBeforeTerminatingNotify;
	}

	/**
	 * @return the sendByeBeforeTerminatingNotify
	 */
	public boolean isSendByeBeforeTerminatingNotify() {
		return sendByeBeforeTerminatingNotify;
	}

	/**
	 * @param sendByeAfterTerminatingNotify the sendByeAfterTerminatingNotify to set
	 */
	public void setSendByeAfterTerminatingNotify(
			boolean sendByeAfterTerminatingNotify) {
		this.sendByeAfterTerminatingNotify = sendByeAfterTerminatingNotify;
	}

	/**
	 * @return the sendByeAfterTerminatingNotify
	 */
	public boolean isSendByeAfterTerminatingNotify() {
		return sendByeAfterTerminatingNotify;
	}	
	
	public boolean isAuthenticationErrorReceived() {
		return authenticationErrorReceived;
	}
	
	public void setRespondWithError(int errorCode) {
		this.respondWithError = errorCode;
	}

	/**
	 * @param finalResponseStatus the finalResponseStatus to set
	 */
	public void setFinalResponseStatus(int finalResponseStatus) {
		this.finalResponseStatus = finalResponseStatus;
	}

	/**
	 * @return the finalResponseStatus
	 */
	public int getFinalResponseStatus() {
		return finalResponseStatus;
	}

	/**
	 * @param joinRequestReceived the joinRequestReceived to set
	 */
	public void setJoinRequestReceived(boolean joinRequestReceived) {
		this.joinRequestReceived = joinRequestReceived;
	}

	/**
	 * @return the joinRequestReceived
	 */
	public boolean isJoinRequestReceived() {
		return joinRequestReceived;
	}

	/**
	 * @param replacesRequestReceived the replacesRequestReceived to set
	 */
	public void setReplacesRequestReceived(boolean replacesRequestReceived) {
		this.replacesRequestReceived = replacesRequestReceived;
	}

	/**
	 * @return the replacesRequestReceived
	 */
	public boolean isReplacesRequestReceived() {
		return replacesRequestReceived;
	}
	
	/**
	 * @return the inviteReceived
	 */
	public boolean isInviteReceived() {
		return inviteReceived;
	}

	/**
	 * @return the inviteReceived
	 */
	public void setInviteReceived(boolean inviteReceived) {
		this.inviteReceived = inviteReceived;
	}

	
	public void setSendReinvite(boolean b) {
		sendReinvite = b;
	}

	public Request getInviteRequest() {
		return inviteRequest;
	}

	/**
	 * @param recordRoutingProxyTesting the recordRoutingProxyTesting to set
	 */
	public void setRecordRoutingProxyTesting(boolean recordRoutingProxyTesting) {
		this.recordRoutingProxyTesting = recordRoutingProxyTesting;
	}

	/**
	 * @return the recordRoutingProxyTesting
	 */
	public boolean isRecordRoutingProxyTesting() {
		return recordRoutingProxyTesting;
	}

	/**
	 * @param sendSubsequentRequestsThroughSipProvider the sendSubsequentRequestsThroughSipProvider to set
	 */
	public void setSendSubsequentRequestsThroughSipProvider(
			boolean sendSubsequentRequestsThroughSipProvider) {
		this.sendSubsequentRequestsThroughSipProvider = sendSubsequentRequestsThroughSipProvider;
	}

	/**
	 * @return the sendSubsequentRequestsThroughSipProvider
	 */
	public boolean isSendSubsequentRequestsThroughSipProvider() {
		return sendSubsequentRequestsThroughSipProvider;
	}

	/**
	 * @param testAckViaParam the testAckViaParam to set
	 */
	public void setTestAckViaParam(boolean testAckViaParam) {
		this.testAckViaParam = testAckViaParam;
	}

	/**
	 * @return the testAckViaParam
	 */
	public boolean isTestAckViaParam() {
		return testAckViaParam;
	}

	/**
	 * @return the byeRequestReceived
	 */
	public Request getByeRequestReceived() {
		return byeRequestReceived;
	}

	/**
	 * @return the registerReceived
	 */
	public Request getRegisterReceived() {
		return registerReceived;
	}

	/**
	 * @param timeToWaitBeforeBye the timeToWaitBeforeBye to set
	 */
	public void setTimeToWaitBeforeBye(long timeToWaitBeforeBye) {
		this.timeToWaitBeforeBye = timeToWaitBeforeBye;
	}

	/**
	 * @return the timeToWaitBeforeBye
	 */
	public long getTimeToWaitBeforeBye() {
		return timeToWaitBeforeBye;
	}

	/**
	 * @param sendAck the sendAck to set
	 */
	public void setSendAck(boolean sendAck) {
		this.sendAck = sendAck;
	}

	/**
	 * @return the sendAck
	 */
	public boolean isSendAck() {
		return sendAck;
	}

	/**
	 * @param prackSent the prackSent to set
	 */
	public void setPrackSent(boolean prackSent) {
		this.prackSent = prackSent;
	}

	/**
	 * @return the prackSent
	 */
	public boolean isPrackSent() {
		return prackSent;
	}

	/**
	 * @param okToPrackReceived the okToPrackReceived to set
	 */
	public void setOkToPrackReceived(boolean okToPrackReceived) {
		this.okToPrackReceived = okToPrackReceived;
	}

	/**
	 * @return the okToPrackReceived
	 */
	public boolean isOkToPrackReceived() {
		return okToPrackReceived;
	}

	/**
	 * @param prackReceived the prackReceived to set
	 */
	public void setPrackReceived(boolean prackReceived) {
		this.prackReceived = prackReceived;
	}

	/**
	 * @return the prackReceived
	 */
	public boolean isPrackReceived() {
		return prackReceived;
	}

	/**
	 * @param useDefaultRoute the useDefaultRoute to set
	 */
	public void setUseDefaultRoute(boolean useDefaultRoute) {
		this.useDefaultRoute = useDefaultRoute;
	}

	/**
	 * @return the useDefaultRoute
	 */
	public boolean isUseDefaultRoute() {
		return useDefaultRoute;
	}

	/**
	 * @param messageRequest the messageRequest to set
	 */
	public void setMessageRequest(Request messageRequest) {
		this.messageRequest = messageRequest;
	}

	/**
	 * @return the messageRequest
	 */
	public Request getMessageRequest() {
		return messageRequest;
	}

	public void setTransport(boolean b) {
		setTransport = b;
	}

	/**
	 * @param finalResponse the finalResponse to set
	 */
	public void setFinalResponse(Response finalResponse) {
		this.finalResponse = finalResponse;
	}

	/**
	 * @return the finalResponse
	 */
	public Response getFinalResponse() {
		return finalResponse;
	}

	/**
	 * @param timeToWaitBeforeAck the timeToWaitBeforeAck to set
	 */
	public void setTimeToWaitBeforeAck(long timeToWaitBeforeAck) {
		this.timeToWaitBeforeAck = timeToWaitBeforeAck;
	}

	/**
	 * @return the timeToWaitBeforeAck
	 */
	public long getTimeToWaitBeforeAck() {
		return timeToWaitBeforeAck;
	}
	
	/**
	 * @return the serviceUnavailableReceived
	 */
	public Response getServiceUnavailableResponse() {
		return serviceUnavailableResponse;
	}

	public void setReferResponseToSend(int referResponseToSend) {
		this.referResponseToSend  = referResponseToSend;
	}

	/**
	 * @param sendNotifyForRefer the sendNotifyForRefer to set
	 */
	public void setSendNotifyForRefer(boolean sendNotifyForRefer) {
		this.sendNotifyForRefer = sendNotifyForRefer;
	}

	/**
	 * @return the sendNotifyForRefer
	 */
	public boolean isSendNotifyForRefer() {
		return sendNotifyForRefer;
	}

	/**
	 * @param sendNotify the sendNotify to set
	 */
	public void setSendNotify(boolean sendNotify) {
		this.sendNotify = sendNotify;
	}

	/**
	 * @return the sendNotify
	 */
	public boolean isSendNotify() {
		return sendNotify;
	}

	/**
	 * @param informationalResponse the informationalResponse to set
	 */
	public void setInformationalResponse(Response informationalResponse) {
		this.informationalResponse = informationalResponse;
	}

	/**
	 * @return the informationalResponse
	 */
	public Response getInformationalResponse() {
		return informationalResponse;
	}

	/**
	 * @param countRetrans the countRetrans to set
	 */
	public void setCountRetrans(boolean countRetrans) {
		this.countRetrans = countRetrans;
	}

	/**
	 * @return the countRetrans
	 */
	public boolean isCountRetrans() {
		return countRetrans;
	}

	/**
	 * @param nbRetrans the nbRetrans to set
	 */
	public void setNbRetrans(int nbRetrans) {
		this.nbRetrans = nbRetrans;
	}

	/**
	 * @return the nbRetrans
	 */
	public int getNbRetrans() {
		return nbRetrans;
	}

	/**
	 * @param prackRequestReceived the prackRequestReceived to set
	 */
	public void setPrackRequestReceived(Request prackRequestReceived) {
		this.prackRequestReceived = prackRequestReceived;
	}

	/**
	 * @return the prackRequestReceived
	 */
	public Request getPrackRequestReceived() {
		return prackRequestReceived;
	}

	public void setMultipleChallengeInResponse(boolean multipleChallengeInResponse) {
		this.multipleChallengeInResponse = multipleChallengeInResponse;
	}

	/**
	 * @param disableSequenceNumberValidation the disableSequenceNumberValidation to set
	 */
	public void setDisableSequenceNumberValidation(
			boolean disableSequenceNumberValidation) {
		this.disableSequenceNumberValidation = disableSequenceNumberValidation;
	}

	/**
	 * @return the disableSequenceNumberValidation
	 */
	public boolean isDisableSequenceNumberValidation() {
		return disableSequenceNumberValidation;
	}

	/**
	 * @param sendCancelOn1xx the sendCancelOn1xx to set
	 */
	public void setSendCancelOn1xx(boolean sendCancelOn1xx) {
		this.sendCancelOn1xx = sendCancelOn1xx;
	}

	/**
	 * @return the sendCancelOn1xx
	 */
	public boolean isSendCancelOn1xx() {
		return sendCancelOn1xx;
	}

	/**
	 * @param useToURIasRequestUri the useToURIasRequestUri to set
	 */
	public void setUseToURIasRequestUri(boolean useToURIasRequestUri) {
		this.useToURIasRequestUri = useToURIasRequestUri;
	}

	/**
	 * @return the useToURIasRequestUri
	 */
	public boolean isUseToURIasRequestUri() {
		return useToURIasRequestUri;
	}

	public void setTestNextNonce(boolean b) {
		this.testNextNonce  = b;
	}
	
	public void setTestNC(boolean b) {
		this.testNC  = b;
	}

	/**
	 * @param dropRequest the dropRequest to set
	 */
	public void setDropRequest(boolean dropRequest) {
		this.dropRequest = dropRequest;
	}

	/**
	 * @return the dropRequest
	 */
	public boolean isDropRequest() {
		return dropRequest;
	}

	public void setWaitBeforeFinalResponse(long waitBeforeFinalResponse) {
		this.waitBeforeFinalResponse = waitBeforeFinalResponse;
	}

	public boolean isSendUpdate() {
		return sendUpdate;
	}

	public boolean isUpdateReceived() {
		return updateReceived;
	}

	public void setSendUpdateAfterUpdate(boolean sendUpdateAfterUpdate) {
		this.sendUpdateAfterUpdate = sendUpdateAfterUpdate;
	}

	public boolean isSendUpdateAfterUpdate() {
		return sendUpdateAfterUpdate;
	}

	public void setSendUpdateAfterPrack(boolean sendUpdateAfterPrack) {
		this.sendUpdateAfterPrack = sendUpdateAfterPrack;
	}

	public void setSendUpdateAfterProvisionalResponses(boolean sendUpdateAfterProvisonalResponses) {
		this.sendUpdateAfterProvisionalResponses = sendUpdateAfterProvisonalResponses;
	}

	/**
	 * @return the ackRequest
	 */
	public Request getAckRequest() {
		return ackRequest;
	}

	public void setHandleAuthorization(boolean handleAuthorization) {
		this.handleAuthorization = handleAuthorization;
	}

	public boolean isHandleAuthorization() {
		return handleAuthorization;
	}

	public void setUpdateRequest(Request updateRequest) {
		this.updateRequest = updateRequest;
	}

	public Request getUpdateRequest() {
		return updateRequest;
	}

	public void setInfoRequest(Request infoRequest) {
		this.infoRequest = infoRequest;
	}

	public Request getInfoRequest() {
		return infoRequest;
	}
	
	public void setOptionsRequest(Request optionsRequest) {
		this.optionsRequest = optionsRequest;
	}

	public Request getOptionsRequest() {
		return this.optionsRequest;
	}

	/**
	 * @param addRecordRouteForResponses the addRecordRouteForResponses to set
	 */
	public void setAddRecordRouteForResponses(boolean addRecordRouteForResponses) {
		this.addRecordRouteForResponses = addRecordRouteForResponses;
	}

	/**
	 * @return the addRecordRouteForResponses
	 */
	public boolean isAddRecordRouteForResponses() {
		return addRecordRouteForResponses;
	}

	public void setRFC5626UseCase(RFC5626UseCase rfc5626UseCase) {
		this.rfc5626UseCase = rfc5626UseCase;
	}

	public void setSecurityUser(String securityUser) {
		this.securityUser  = securityUser;
	}

	public void setSecurityPwd(String securityPwd) {
		this.securityPwd  = securityPwd;
	}

	public void sendProvisionalResponseBeforeChallenge(boolean b) {
		sendProvisionalResponseBeforeChallenge  = b;
	}
}
