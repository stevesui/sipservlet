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

package org.mobicents.servlet.sip.proxy;

import gov.nist.javax.sip.header.Via;
import gov.nist.javax.sip.message.MessageExt;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletException;
import javax.servlet.sip.ProxyBranch;
import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.SipURI;
import javax.servlet.sip.URI;
import javax.sip.SipException;
import javax.sip.SipProvider;
import javax.sip.Transaction;
import javax.sip.TransactionState;
import javax.sip.header.ContactHeader;
import javax.sip.header.Header;
import javax.sip.header.RecordRouteHeader;
import javax.sip.header.RouteHeader;
import javax.sip.header.ViaHeader;
import javax.sip.message.Message;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.apache.log4j.Logger;
import org.mobicents.ha.javax.sip.ReplicationStrategy;
import org.mobicents.servlet.sip.JainSipUtils;
import org.mobicents.servlet.sip.address.AddressImpl.ModifiableRule;
import org.mobicents.servlet.sip.address.SipURIImpl;
import org.mobicents.servlet.sip.address.TelURLImpl;
import org.mobicents.servlet.sip.core.DispatcherException;
import org.mobicents.servlet.sip.core.MobicentsSipFactory;
import org.mobicents.servlet.sip.core.dispatchers.MessageDispatcher;
import org.mobicents.servlet.sip.core.message.MobicentsSipServletRequest;
import org.mobicents.servlet.sip.core.message.MobicentsSipServletResponse;
import org.mobicents.servlet.sip.core.proxy.MobicentsProxy;
import org.mobicents.servlet.sip.core.proxy.MobicentsProxyBranch;
import org.mobicents.servlet.sip.core.session.MobicentsSipApplicationSession;
import org.mobicents.servlet.sip.core.session.MobicentsSipSession;
import org.mobicents.servlet.sip.core.timers.ProxyTimerService;
import org.mobicents.servlet.sip.message.SipFactoryImpl;
import org.mobicents.servlet.sip.message.SipServletRequestImpl;
import org.mobicents.servlet.sip.message.SipServletResponseImpl;
import org.mobicents.servlet.sip.message.TransactionApplicationData;
import org.mobicents.servlet.sip.proxy.ProxyBranchImpl.TransactionRequest;
import org.mobicents.servlet.sip.startup.StaticServiceHolder;

/**
 * @author root
 *
 */
public class ProxyImpl implements MobicentsProxy, Externalizable {

	private static final long serialVersionUID = 1L;

	private static final Logger logger = Logger.getLogger(ProxyImpl.class);
	
	private SipServletRequestImpl originalRequest;
	private transient SipServletResponseImpl bestResponse;
	private transient ProxyBranchImpl bestBranch;
	private boolean recurse = true;
	private int proxyTimeout;
	private int proxy1xxTimeout;
	private int seqSearchTimeout;
	private boolean supervised = true; 
	private boolean recordRoutingEnabled;
	private boolean parallel = true;
	private boolean addToPath;
	private boolean sipOutboundSupport;
	private int bestResponseSent = -1;
	protected transient SipURIImpl pathURI;
	protected transient SipURI recordRouteURI;
	private transient SipURI outboundInterface;
	private transient SipFactoryImpl sipFactoryImpl;
	private boolean isNoCancel;
	
	private final transient Map<String, TransactionApplicationData> transactionMap = new ConcurrentHashMap<String, TransactionApplicationData>();
	
	private transient Map<URI, ProxyBranchImpl> proxyBranches;
	private boolean started; 
	private boolean ackReceived = false;
	private boolean tryingSent = false;
	// This branch is the final branch (set when the final response has been sent upstream by the proxy) 
	// that will be used for proxying subsequent requests
	private ProxyBranchImpl finalBranchForSubsequentRequests;
	
	// Keep the URI of the previous SIP entity that sent the original request to us (either another proxy or UA)
	private SipURI previousNode;
	
	// The From-header of the initiator of the request. Used to determine the direction of the request.
	// Caller -> Callee or Caller <- Callee
	private String callerFromTag;
	// Issue 1791 : using a timer service created outside the application loader to avoid leaks on startup/shutdown
	private transient ProxyTimerService proxyTimerService;

	// Information required to implement 3GPP TS 24.229 section 5.2.8.1.2. ie Termination of Session originating from the proxy.
	private long callerCSeq; 	 // Last CSeq seen from caller
	private long calleeCSeq = 0; // Last CSeq seen from callee (We may never see one if the callee never sends a request)
	private boolean storeTerminationInfo = false; // Enables storage of termination information.
	private ProxyTerminationInfo terminationInfo; // Object to store termination information	

	// empty constructor used only for Externalizable interface
	public ProxyImpl() {}
	
	public ProxyImpl(SipServletRequestImpl request, SipFactoryImpl sipFactoryImpl)
	{
		this.proxyTimerService = ((MobicentsSipApplicationSession)request.getApplicationSession(false)).getSipContext().getProxyTimerService();
		this.originalRequest = request;
		this.sipFactoryImpl = sipFactoryImpl;
		this.proxyBranches = new LinkedHashMap<URI, ProxyBranchImpl> ();		
		this.proxyTimeout = 180; // 180 secs default
		this.proxy1xxTimeout = -1; // not activated by default
		String outboundInterfaceStringified = ((MobicentsSipSession)request.getSession()).getOutboundInterface();
		if(outboundInterfaceStringified != null) {
			try {
				outboundInterface = (SipURI) sipFactoryImpl.createURI(outboundInterfaceStringified);
			} catch (ServletParseException e) {
				throw new IllegalArgumentException("couldn't parse the outbound interface " + outboundInterface, e);
			}
		}
		this.callerFromTag = ((MessageExt)request.getMessage()).getFromHeader().getTag();
		this.previousNode = extractPreviousNodeFromRequest(request);
		this.callerCSeq = ((MessageExt)request.getMessage()).getCSeqHeader().getSeqNumber();
		putTransaction(originalRequest);
    }
    // https://code.google.com/p/sipservlets/issues/detail?id=238
    public void putTransaction(SipServletRequestImpl request) {
            final String txId = ((ViaHeader) request.getMessage().getHeader(ViaHeader.NAME)).getBranch();
            final TransactionApplicationData txData = request.getTransactionApplicationData();
            if(txData != null && this.transactionMap.put(txId, txData) == null) {
                    if(logger.isDebugEnabled()) {
                            logger.debug("Transaction "+txId+" added to proxy.");
                    }
            }                               
    }
    // https://code.google.com/p/sipservlets/issues/detail?id=238
    public void removeTransaction(String txId) {
            if(this.transactionMap.remove(txId) != null) {
                    if(logger.isDebugEnabled()) {
                            logger.debug("Transaction "+txId+" removed from proxy.");
                    }
            }                                       
    }
	
	/*
	 * This method will find the address of the machine that is the previous dialog path node.
	 * If there are proxies before the current one that are adding Record-Route we should visit them,
	 * otherwise just send to the client directly. And we don't want to visit proxies that are not
	 * Record-Routing, because they are not in the dialog path.
	 */
	private SipURI extractPreviousNodeFromRequest(SipServletRequestImpl request) {
		SipURI uri = null;
		try {
			// First check for record route
			RecordRouteHeader rrh = (RecordRouteHeader) request.getMessage().getHeader(RecordRouteHeader.NAME);
			if(rrh != null) {
				javax.sip.address.SipURI sipUri = (javax.sip.address.SipURI) rrh.getAddress().getURI();
				uri = new SipURIImpl(sipUri, ModifiableRule.NotModifiable);
			} else { 
				// If no record route is found then use the last via (the originating endpoint)
				ListIterator<ViaHeader> viaHeaders = request.getMessage().getHeaders(ViaHeader.NAME);
				ViaHeader lastVia = null;
				while(viaHeaders.hasNext()) {
					lastVia = viaHeaders.next();
				} 
				String uriString = ((Via)lastVia).getSentBy().toString();
				uri = sipFactoryImpl.createSipURI(null, uriString);
				if(lastVia.getTransport() != null) {
					uri.setTransportParam(lastVia.getTransport());
				} else {
					uri.setTransportParam("udp");
				}
			}
		} catch (Exception e) {
			// We shouldn't completely fail in this case because it is rare to visit this code
			logger.error("Failed parsing previous address ", e);
		}
		return uri;

	}
	
	/* (non-Javadoc)
	 * @see javax.servlet.sip.Proxy#cancel()
	 */
	public void cancel() {
		if(ackReceived) 
			throw new IllegalStateException("There has been an ACK received. Can not cancel more brnaches, the INVITE tx has finished.");
		cancelAllExcept(null, null, null, null, true);
	}
	
	/*
	 * (non-Javadoc)
	 * @see javax.servlet.sip.Proxy#cancel(java.lang.String[], int[], java.lang.String[])
	 */
	public void cancel(String[] protocol, int[] reasonCode, String[] reasonText) {
		if(ackReceived) 
			throw new IllegalStateException("There has been an ACK received. Can not cancel more brnaches, the INVITE tx has finished.");
		cancelAllExcept(null, protocol, reasonCode, reasonText, true);
	}

	public void cancelAllExcept(ProxyBranch except, String[] protocol, int[] reasonCode, String[] reasonText, boolean throwExceptionIfCannotCancel) {
		if(logger.isDebugEnabled()) {
			if(except == null) {
				logger.debug("Cancelling all Branches");
			} else {
				logger.debug("Cancelling all Branches except " + except + " with outgoing resquest " + except.getRequest());
			}
		}
		for(ProxyBranch proxyBranch : proxyBranches.values()) {		
			if(!proxyBranch.equals(except)) {
				// Do not make this check in the beginning of the method, because in case of reINVITE etc, we already have a single brnch nd this method
				// would have no actual effect, no need to fail it just because we've already seen ACK. Only throw exception if there are other branches.
				try {
					proxyBranch.cancel(protocol, reasonCode, reasonText);
				} catch (IllegalStateException e) {
					// TODO: Instead of catching excpetions here just determine if the branch is cancellable
					if(throwExceptionIfCannotCancel) throw e;
				}
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see javax.servlet.sip.Proxy#createProxyBranches(java.util.List)
	 */
	public List<ProxyBranch> createProxyBranches(List<? extends URI> targets) {
		ArrayList<ProxyBranch> list = new ArrayList<ProxyBranch>();
		for(URI target: targets)
		{
			if(target == null) {
				throw new NullPointerException("URI can't be null");
			}
			if(!JainSipUtils.checkScheme(target.toString())) {
				throw new IllegalArgumentException("Scheme " + target.getScheme() + " is not supported");
			}
			ProxyBranchImpl branch = new ProxyBranchImpl(target, this);
			branch.setRecordRoute(recordRoutingEnabled);
			branch.setRecurse(recurse);
			list.add(branch);
			this.proxyBranches.put(target, branch);
		}
		return list;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.sip.Proxy#getAddToPath()
	 */
	public boolean getAddToPath() {
		return addToPath;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.sip.Proxy#getOriginalRequest()
	 */
	public SipServletRequest getOriginalRequest() {
		return originalRequest;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.sip.Proxy#getParallel()
	 */
	public boolean getParallel() {
		return this.parallel;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.sip.Proxy#getPathURI()
	 */
	public SipURI getPathURI() {
		if(!this.addToPath) throw new IllegalStateException("You must setAddToPath(true) before getting URI");
		return this.pathURI;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.sip.Proxy#getProxyBranch(javax.servlet.sip.URI)
	 */
	public ProxyBranch getProxyBranch(URI uri) {
		return this.proxyBranches.get(uri);
	}

	/* (non-Javadoc)
	 * @see javax.servlet.sip.Proxy#getProxyBranches()
	 */
	public List<ProxyBranch> getProxyBranches() {
		return new ArrayList<ProxyBranch>(this.proxyBranches.values());
	}
		
	public Map<URI, ProxyBranchImpl> getProxyBranchesMap() {
		return this.proxyBranches;
	}

	/**
	 * @return the finalBranchForSubsequentRequest
	 */
	public MobicentsProxyBranch getFinalBranchForSubsequentRequests() {
		return finalBranchForSubsequentRequests;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.sip.Proxy#getProxyTimeout()
	 */
	public int getProxyTimeout() {
		return this.proxyTimeout;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.sip.Proxy#getRecordRoute()
	 */
	public boolean getRecordRoute() {
		return this.recordRoutingEnabled;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.sip.Proxy#getRecordRouteURI()
	 */
	public SipURI getRecordRouteURI() {
		if(!this.recordRoutingEnabled) throw new IllegalStateException("You must setRecordRoute(true) before getting URI");
		return this.recordRouteURI;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.sip.Proxy#getRecurse()
	 */
	public boolean getRecurse() {
		return this.recurse;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.sip.Proxy#getSequentialSearchTimeout()
	 */
	public int getSequentialSearchTimeout() {
		return this.seqSearchTimeout;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.sip.Proxy#getStateful()
	 */
	public boolean getStateful() {
		return true;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.sip.Proxy#getSupervised()
	 */
	public boolean getSupervised() {
		return this.supervised;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.sip.Proxy#proxyTo(java.util.List)
	 */
	public void proxyTo(final List<? extends URI> uris) {
		for (URI uri : uris)
		{
			if(uri == null) {
				throw new NullPointerException("URI can't be null");
			}
			if(!JainSipUtils.checkScheme(uri.toString())) {
				throw new IllegalArgumentException("Scheme " + uri.getScheme() + " is not supported");
			}
			final ProxyBranchImpl branch = new ProxyBranchImpl((URI) uri, this);
			branch.setRecordRoute(recordRoutingEnabled);
			branch.setRecurse(recurse);
			this.proxyBranches.put(uri, branch);
		}
		startProxy();
	}

	/* (non-Javadoc)
	 * @see javax.servlet.sip.Proxy#proxyTo(javax.servlet.sip.URI)
	 */
	public void proxyTo(final URI uri) {
		if(uri == null) {
			throw new NullPointerException("URI can't be null");
		}
		if(!JainSipUtils.checkScheme(uri.toString())) {
			// Fix for Issue http://code.google.com/p/mobicents/issues/detail?id=2327, checking the route header
			RouteHeader routeHeader = (RouteHeader) originalRequest.getMessage().getHeader(RouteHeader.NAME);
			if(routeHeader == null || (routeHeader != null && !JainSipUtils.checkScheme(routeHeader.getAddress().getURI().toString()))) {
				throw new IllegalArgumentException("Scheme " + uri.getScheme() + " is not supported");
			}			
		}
		final ProxyBranchImpl branch = new ProxyBranchImpl(uri, this);
		branch.setRecordRoute(recordRoutingEnabled);
		branch.setRecurse(recurse);
		this.proxyBranches.put(uri, branch);
		startProxy();

	}

	/* (non-Javadoc)
	 * @see javax.servlet.sip.Proxy#setAddToPath(boolean)
	 */
	public void setAddToPath(boolean p) {
		if(started) {
			throw new IllegalStateException("Cannot set a record route on an already started proxy");
		}
		if(this.pathURI == null) {
			this.pathURI = new SipURIImpl ( JainSipUtils.createRecordRouteURI( sipFactoryImpl.getSipNetworkInterfaceManager(), null), ModifiableRule.Modifiable);			
			pathURI.setLrParam(true);
			pathURI.setIsModifiable(ModifiableRule.NotModifiable);
		}		
		addToPath = p;

	}

	/* (non-Javadoc)
	 * @see javax.servlet.sip.Proxy#setParallel(boolean)
	 */
	public void setParallel(boolean parallel) {
		this.parallel = parallel;

	}

	/* (non-Javadoc)
	 * @see javax.servlet.sip.Proxy#setProxyTimeout(int)
	 */
	public void setProxyTimeout(int seconds) {
		if(seconds<=0) throw new IllegalArgumentException("Negative or zero timeout not allowed");
		
		proxyTimeout = seconds;
		for(ProxyBranchImpl proxyBranch : proxyBranches.values()) {	
			final boolean inactive = proxyBranch.isCanceled() || proxyBranch.isTimedOut();
			
			if(!inactive) {
				proxyBranch.setProxyBranchTimeout(seconds);
			}
		}

	}

	/* (non-Javadoc)
	 * @see javax.servlet.sip.Proxy#setRecordRoute(boolean)
	 */
	public void setRecordRoute(boolean rr) {
		if(started) {
			throw new IllegalStateException("Cannot set a record route on an already started proxy");
		}
		if(rr) {
			Message message = null;
			if(originalRequest != null) {
				message = originalRequest.getMessage();
			}
			// record route should be based on the original received message
			javax.sip.address.SipURI flowUri= originalRequest.getSipSession().getFlow();
			if(flowUri != null) {				
				this.recordRouteURI = new SipURIImpl (flowUri, ModifiableRule.ProxyRecordRouteNotModifiable);
				if(logger.isDebugEnabled())
					logger.debug("Using Session Flow URI as record route URI " + recordRouteURI);
			} else {
				this.recordRouteURI = new SipURIImpl ( JainSipUtils.createRecordRouteURI( sipFactoryImpl.getSipNetworkInterfaceManager(), message), ModifiableRule.ProxyRecordRouteNotModifiable);
			}
			if(logger.isDebugEnabled()) {
				logger.debug("Record routing enabled for proxy, Record Route used will be : " + recordRouteURI.toString());
			}
		}		
		this.recordRoutingEnabled = rr;

	}

	/* (non-Javadoc)
	 * @see javax.servlet.sip.Proxy#setRecurse(boolean)
	 */
	public void setRecurse(boolean recurse) {
		this.recurse = recurse;

	}

	/* (non-Javadoc)
	 * @see javax.servlet.sip.Proxy#setSequentialSearchTimeout(int)
	 */
	public void setSequentialSearchTimeout(int seconds) {
		seqSearchTimeout = seconds;

	}

	/* (non-Javadoc)
	 * @see javax.servlet.sip.Proxy#setStateful(boolean)
	 */
	public void setStateful(boolean stateful) {
		//NOTHING

	}

	/* (non-Javadoc)
	 * @see javax.servlet.sip.Proxy#setSupervised(boolean)
	 */
	public void setSupervised(boolean supervised) {
		this.supervised = supervised;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.sip.Proxy#startProxy()
	 */
	public void startProxy() {
		if(this.ackReceived) 
			throw new IllegalStateException("Can't start. ACK has been received.");
		if(!this.originalRequest.isInitial())
			throw new IllegalStateException("Applications should not attempt to " +
					"proxy subsequent requests. Proxying the initial request is " +
					"sufficient to carry all subsequent requests through the same" +
					" path.");

		// Only send TRYING when the request is INVITE, needed by testProxyGen2xx form TCK (it sends MESSAGE)
		if(this.originalRequest.getMethod().equals(Request.INVITE) && !tryingSent) {
			// Send provisional TRYING. Chapter 10.2
			// We must send only one TRYING no matter how many branches we spawn later.
			// This is needed for tests like testProxyBranchRecurse
			tryingSent = true;
			TransactionState transactionState = null;
			if(originalRequest.getTransaction() != null) {
				transactionState = originalRequest.getTransaction().getState();
			}
			// Fix for Issue http://code.google.com/p/mobicents/issues/detail?id=2417
			// Two 100 Trying responses sent if Proxy decision is delayed.
			if (transactionState == null || transactionState == TransactionState.TRYING) {
				if(originalRequest.getTransaction().getState() == null )
				logger.info("Sending 100 Trying to the source");
				SipServletResponse trying =
					originalRequest.createResponse(100);			
				try {
					trying.send();
				} catch (IOException e) { 
					logger.error("Cannot send the 100 Trying",e);
				}
			}
		}
		
		
		started = true;
		if(this.parallel) {
			for (final MobicentsProxyBranch pb : this.proxyBranches.values()) {
				if(!pb.isStarted()) {
					pb.start();
				}
			}
		} else {
			startNextUntriedBranch();
		}		
	}
	
	public SipURI getOutboundInterface() {
		return outboundInterface;
	}
	
	public void onFinalResponse(ProxyBranchImpl branch) throws DispatcherException {
		//Get the final response
		final SipServletResponseImpl response = (SipServletResponseImpl) branch.getResponse();
		final int status = response.getStatus(); 
		
		// Cancel all others if 2xx or 6xx 10.2.4 and it's not a retransmission
		if(!isNoCancel && response.getTransaction() != null) {
			if(this.getParallel()) {
				if( (status >= 200 && status < 300) 
					|| (status >= 600 && status < 700) ) { 
					if(logger.isDebugEnabled())
						logger.debug("Cancelling all other broanches in this proxy");
					cancelAllExcept(branch, null, null, null, false);
				}
			}
		}
		// Recurse if allowed
		if(status >= 300 && status < 400
				&& recurse)
		{
			// We may want to store these for "moved permanently" and others
			ListIterator<Header> headers = 
				response.getMessage().getHeaders(ContactHeader.NAME);
			while(headers.hasNext()) {
				final ContactHeader contactHeader = (ContactHeader) headers.next();
				final javax.sip.address.URI addressURI = contactHeader.getAddress().getURI();
				URI contactURI = null;
				if (addressURI instanceof javax.sip.address.SipURI) {
					contactURI = new SipURIImpl(
							(javax.sip.address.SipURI) addressURI, ModifiableRule.NotModifiable);
				} else if (addressURI instanceof javax.sip.address.TelURL) {
					contactURI = new TelURLImpl(
							(javax.sip.address.TelURL) addressURI);

				}
				final ProxyBranchImpl recurseBranch = new ProxyBranchImpl(contactURI, this);
				recurseBranch.setRecordRoute(recordRoutingEnabled);
				recurseBranch.setRecurse(recurse);
				this.proxyBranches.put(contactURI, recurseBranch);
				branch.addRecursedBranch(branch);
				if(parallel) {
					recurseBranch.start();
				}
				// if not parallel, just adding it to the list is enough
			}
		}
		
		// Sort best do far				
		if(bestResponse == null || bestResponse.getStatus() > status)
		{
			//Assume 600 and 400 are equally bad, the better one is the one that came first (TCK doBranchBranchTest)
			if(bestResponse != null) {
				if(status < 400) {
					bestResponse = response;
					bestBranch = branch;
				}
			} else {
				bestResponse = response;
				bestBranch = branch;
			}
		}
		
		if(logger.isDebugEnabled())
					logger.debug("Best response so far is " + bestResponse);
		
		// Check if we are waiting for more response
		if(parallel && allResponsesHaveArrived()) {
			finalBranchForSubsequentRequests = bestBranch;			
			if(logger.isDebugEnabled())
				logger.debug("All responses have arrived, sending final response for parallel proxy" );
			sendFinalResponse(bestResponse, bestBranch);
		} else if (!parallel) {
			final int bestResponseStatus = bestResponse.getStatus();
			if(bestResponseStatus >= 200 && bestResponseStatus < 300) {
				finalBranchForSubsequentRequests = bestBranch;
				if(logger.isDebugEnabled())
					logger.debug("Sending final response for sequential proxy" );
				sendFinalResponse(bestResponse, bestBranch);
			} else {
				if(allResponsesHaveArrived()) {
					if(logger.isDebugEnabled())
						logger.debug("All responses have arrived for sequential proxy and we are sending the best one");
					sendFinalResponse(bestResponse, bestBranch);
				} else {
					if(logger.isDebugEnabled())
						logger.debug("Trying new branch in proxy" );
					startNextUntriedBranch();
					branch.onBranchTerminated();
				}
			}
		}

	}
	
	public void onBranchTimeOut(ProxyBranchImpl branch) throws DispatcherException
	{
		if(this.bestBranch == null) this.bestBranch = branch;
		if(allResponsesHaveArrived())
		{
			sendFinalResponse(bestResponse, bestBranch);
		}
		else
		{
			if(!parallel)
			{
				branch.cancel();
				startNextUntriedBranch();
				branch.onBranchTerminated();
			}
		}
	}
	
	// In sequential proxying get some untried branch and start it, then wait for response and repeat
	public void startNextUntriedBranch()
	{
		if(this.parallel) 
			throw new IllegalStateException("This method is only for sequantial proxying");
		
		for(final MobicentsProxyBranch pbi: this.proxyBranches.values())
		{			
			// Issue http://code.google.com/p/mobicents/issues/detail?id=2461
			// don't start the branch is it has been cancelled already
			if(!pbi.isStarted() && !pbi.isCanceled())
			{
				pbi.start();
				return;
			}
		}
	}
	
	public boolean allResponsesHaveArrived()
	{
		for(final MobicentsProxyBranch pbi: this.proxyBranches.values())
		{
			final SipServletResponse response = pbi.getResponse();
			
			// The unstarted branches still haven't got a chance to get response
			// Issue http://code.google.com/p/mobicents/issues/detail?id=2461 adding !isCancelled
			if(!pbi.isStarted() && !pbi.isCanceled()) { 
				return false;
			}
			
			if(pbi.isStarted() && !pbi.isTimedOut() && !pbi.isCanceled())
			{
				if(response == null || 						// if there is no response yet
					response.getStatus() < Response.OK) {	// or if the response if not final
					return false;							// then we should wait more
				}
			}
		}
		return true;
	}
	
	public void sendFinalResponse(MobicentsSipServletResponse response,
			ProxyBranchImpl proxyBranch) throws DispatcherException {		
		
		// If we didn't get any response and only a timeout just return a timeout
		if(proxyBranch.isTimedOut()) {
			try {
				MobicentsSipServletResponse timeoutResponse = (MobicentsSipServletResponse) originalRequest.createResponse(Response.REQUEST_TIMEOUT);
				if(logger.isDebugEnabled())
					logger.debug("Proxy branch has timed out");
				// Issue 2474 & 2475
				if(logger.isDebugEnabled())
					logger.debug("All responses have arrived, sending final response for parallel proxy" );
				try {
					MessageDispatcher.callServlet(timeoutResponse);
				} catch (ServletException e) {				
					throw new DispatcherException("Unexpected servlet exception while processing the response : " + response, e);					
				} catch (IOException e) {				
					throw new DispatcherException("Unexpected io exception while processing the response : " + response, e);
				} catch (Throwable e) {				
					throw new DispatcherException("Unexpected exception while processing response : " + response, e);
				}
				timeoutResponse.send();
				bestResponseSent = Response.REQUEST_TIMEOUT;
				return;
			} catch (IOException e) {
				throw new IllegalStateException("Failed to send a timeout response", e);
			}
		}
			
		if(logger.isDebugEnabled())
					logger.debug("Proxy branch has NOT timed out");

		// Issue 2474 & 2475
		if(supervised) {
			try {
				response.setBranchResponse(false);
				MessageDispatcher.callServlet(response);
			} catch (ServletException e) {				
				throw new DispatcherException("Unexpected servlet exception while processing the response : " + response, e);					
			} catch (IOException e) {				
				throw new DispatcherException("Unexpected io exception while processing the response : " + response, e);
			} catch (Throwable e) {				
				throw new DispatcherException("Unexpected exception while processing response : " + response, e);
			}
		}
		
		if(parallel) {
			if(!allResponsesHaveArrived()) {
				if(logger.isDebugEnabled())
					logger.debug("The application has started new branches so we are waiting for responses on those" );
				return;
			}
		} else {
			final int bestResponseStatus = response.getStatus();
			if((bestResponseStatus < 200 || bestResponseStatus >= 300) && !allResponsesHaveArrived()) {
				if(logger.isDebugEnabled())
					logger.debug("The application has started new branches so we are waiting for responses on those" );
				return;
			}
		}
		
		if(logger.isDebugEnabled())
			logger.debug("All responses have arrived, sending final response for parallel proxy" );
		//Otherwise proceed with proxying the response
		final SipServletResponseImpl proxiedResponse = 
			ProxyUtils.createProxiedResponse(response, proxyBranch);
		
		if(proxiedResponse == null || proxiedResponse.getMessage() == null) {
			if(logger.isDebugEnabled())
				logger.debug("Response was dropped because getProxyUtils().createProxiedResponse(response, proxyBranch) returned null");
			return;// drop out of order message
		}

		try {
			String branch = ((Via)proxiedResponse.getMessage().getHeader(Via.NAME)).getBranch();
			Transaction transaction = null;
			synchronized(proxyBranch.ongoingTransactions) {
				for(TransactionRequest tr : proxyBranch.ongoingTransactions) {

					if(tr.branchId.equals(branch)) {
						transaction = tr.request.getTransaction();
						((SipServletResponseImpl)proxiedResponse).setTransaction(transaction);
						((SipServletResponseImpl)proxiedResponse).setOriginalRequest(tr.request);
						break;
					}
				}
			}
			if(transaction != null // If the server transaction has been responded to before then UDP sets it to COMPLETED while TCP sets it to TERMINATED so we got to watch out for these
					&& !transaction.getState().equals(TransactionState.COMPLETED)
					&& !transaction.getState().equals(TransactionState.TERMINATED)) {	
				// non retransmission case
				try {
					if(logger.isDebugEnabled())
						logger.debug("Sending out proxied final response with existing transaction " + proxiedResponse);
					
					proxiedResponse.send();	
					bestResponseSent = proxiedResponse.getStatus();
					proxyBranches.clear();
					originalRequest = null;
					// not needed cleanup in the finally clause will do it
//					bestBranch = null;
//					bestResponse = null;
					if (storeTerminationInfo && proxiedResponse.getRequest().isInitial() && getRecordRouteURI() != null) {
						if(logger.isDebugEnabled()) {
							logger.debug("storing termination Info for request " + proxiedResponse.getRequest());
						}
						terminationInfo = new ProxyTerminationInfo(proxiedResponse, getRecordRouteURI(), this);
					}
				} catch (Exception e) {
					logger.error("A problem occured while proxying the final response", e);
				}
			} else {
				// retransmission case, RFC3261 specifies that the retrans should be proxied statelessly
				final Message message = proxiedResponse.getMessage();
				String transport = JainSipUtils.findTransport(message);
				SipProvider sipProvider = getSipFactoryImpl().getSipNetworkInterfaceManager().findMatchingListeningPoint(
						transport, false).getSipProvider();
				try {
					if(logger.isDebugEnabled())
						logger.debug("Sending out proxied final response retransmission " + proxiedResponse);
					sipProvider.sendResponse((Response)message);
				} catch (SipException e) {
					logger.error("A problem occured while proxying the final response retransmission", e);
				}
			}
		} finally {
			// This cleanup will eliminate this issues where a retrans leaves unclean branch http://code.google.com/p/mobicents/issues/detail?id=1986
			bestBranch = null;
			bestResponse = null;
		}
	}		

	/**
	 * @return the bestResponse
	 */
	public SipServletResponseImpl getBestResponse() {
		return bestResponse;
	}
	
	public void setOriginalRequest(MobicentsSipServletRequest originalRequest) {
		// Determine the direction of the request. Either it's from the dialog initiator (the caller)
		// or from the callee
		if(((MessageExt)originalRequest.getMessage()).getFromHeader().getTag().equals(callerFromTag)) {
			callerCSeq = (((MessageExt)originalRequest.getMessage()).getCSeqHeader().getSeqNumber());
		} else {
			// If it's from the callee we should send it in the other direction
		    calleeCSeq = (((MessageExt)originalRequest.getMessage()).getCSeqHeader().getSeqNumber());
		}
		this.originalRequest = (SipServletRequestImpl) originalRequest;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean getNoCancel() {
		return isNoCancel;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setNoCancel(boolean isNoCancel) {
		this.isNoCancel = isNoCancel;
	}

	/**
	 * @return the sipFactoryImpl
	 */
	public SipFactoryImpl getSipFactoryImpl() {
		return sipFactoryImpl;
	}

	/**
	 * @param sipFactoryImpl the sipFactoryImpl to set
	 */
	public void setMobicentsSipFactory(MobicentsSipFactory sipFactoryImpl) {
		this.sipFactoryImpl = (SipFactoryImpl) sipFactoryImpl;
	}

	/*
	 * (non-Javadoc)
	 * @see javax.servlet.sip.Proxy#setOutboundInterface(java.net.InetAddress)
	 */
	public void setOutboundInterface(InetAddress inetAddress) {
		if(inetAddress == null) {
			throw new NullPointerException("outbound Interface param shouldn't be null");
		}
		String address = inetAddress.getHostAddress();
		List<SipURI> list = this.sipFactoryImpl.getSipNetworkInterfaceManager().getOutboundInterfaces();
		SipURI networkInterface = null;
		for(SipURI networkInterfaceURI : list) {
			if(networkInterfaceURI.toString().contains(address)) {
				networkInterface = networkInterfaceURI;
				break;
			}
		}
		
		if(networkInterface == null) throw new IllegalArgumentException("Network interface for " +
				inetAddress.getHostAddress() + " not found");
		
		outboundInterface = networkInterface;
	}

	/*
	 * (non-Javadoc)
	 * @see javax.servlet.sip.Proxy#setOutboundInterface(java.net.InetSocketAddress)
	 */
	public void setOutboundInterface(InetSocketAddress inetSocketAddress) {
		if(inetSocketAddress == null) {
			throw new NullPointerException("outbound Interface param shouldn't be null");
		}
		String address = inetSocketAddress.getAddress().getHostAddress()
			+ ":" + inetSocketAddress.getPort();
		List<SipURI> list = this.sipFactoryImpl.getSipNetworkInterfaceManager().getOutboundInterfaces();
		SipURI networkInterface = null;
		for(SipURI networkInterfaceURI : list) {
			if(networkInterfaceURI.toString().contains(address)) {
				networkInterface = networkInterfaceURI;
				break;
			}
		}
		
		if(networkInterface == null) throw new IllegalArgumentException("Network interface for " +
				address + " not found");		
		
		outboundInterface = networkInterface;
	}	
	
	/*
	 * (non-Javadoc)
	 * @see org.mobicents.javax.servlet.sip.ProxyExt#setOutboundInterface(javax.servlet.sip.SipURI)
	 */
	public void setOutboundInterface(SipURI outboundInterface) {
		if(outboundInterface == null) {
			throw new NullPointerException("outbound Interface param shouldn't be null");
		}
		List<SipURI> list = this.sipFactoryImpl.getSipNetworkInterfaceManager().getOutboundInterfaces();
		SipURI networkInterface = null;
		for(SipURI networkInterfaceURI : list) {
			if(networkInterfaceURI.equals(outboundInterface)) {
				networkInterface = networkInterfaceURI;
				break;
			}
		}
		
		if(networkInterface == null) throw new IllegalArgumentException("Network interface for " +
				outboundInterface + " not found");		
		
		this.outboundInterface = networkInterface;
	}
	
	public void setAckReceived(boolean received) {
		this.ackReceived = received;
	}
	
	public boolean getAckReceived() {
		return this.ackReceived;
	}
	
	public SipURI getPreviousNode() {
		return previousNode;
	}

	public String getCallerFromTag() {
		return callerFromTag ;
	}

	public void setCallerFromTag(String initiatorFromTag) {
		this.callerFromTag = initiatorFromTag;
	}

	public Map<String, TransactionApplicationData> getTransactionMap() {
		return transactionMap;
	}

	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		if(ReplicationStrategy.EarlyDialog == StaticServiceHolder.sipStandardService.getReplicationStrategy()) {
			// Issue 2587 : read only if not null.
			if(in.readBoolean()) {
				originalRequest = (SipServletRequestImpl) in.readObject();
			}
		}
		recurse = in.readBoolean();
		proxyTimeout = in.readInt();
		seqSearchTimeout = in.readInt();
		supervised = in.readBoolean();
		recordRoutingEnabled = in.readBoolean();
		parallel = in.readBoolean();
		addToPath = in.readBoolean();
		isNoCancel = in.readBoolean();
		started = in.readBoolean();
		ackReceived = in.readBoolean();
		tryingSent = in.readBoolean();
		finalBranchForSubsequentRequests = (ProxyBranchImpl) in.readObject();
		if(finalBranchForSubsequentRequests != null) {
			finalBranchForSubsequentRequests.setProxy(this);
		}
		previousNode = (SipURI) in.readObject();
		callerFromTag = in.readUTF();
		calleeCSeq = in.readLong();
		callerCSeq = in.readLong();
		storeTerminationInfo = in.readBoolean();
		if (storeTerminationInfo) {
			terminationInfo = (ProxyTerminationInfo) in.readObject();
			terminationInfo.setProxy(this);
		}
		this.proxyBranches = new LinkedHashMap<URI, ProxyBranchImpl> ();
	}

	public void writeExternal(ObjectOutput out) throws IOException {
		if(ReplicationStrategy.EarlyDialog == StaticServiceHolder.sipStandardService.getReplicationStrategy()) {
			// Issue 2587 : replicating original request is only useful for early dialog failover
			if(originalRequest != null && originalRequest.getMethod().equalsIgnoreCase(Request.INVITE)) {
				out.writeBoolean(true);
				out.writeObject(originalRequest);
			} else {
				out.writeBoolean(false);
			}
		}
		out.writeBoolean(recurse);
		out.writeInt(proxyTimeout);
		out.writeInt(seqSearchTimeout);
		out.writeBoolean(supervised);
		out.writeBoolean(recordRoutingEnabled);
		out.writeBoolean(parallel);
		out.writeBoolean(addToPath);
		out.writeBoolean(isNoCancel);
		out.writeBoolean(started);
		out.writeBoolean(ackReceived);
		out.writeBoolean(tryingSent);
		out.writeObject(finalBranchForSubsequentRequests);
		out.writeObject(previousNode);
		out.writeUTF(callerFromTag);
		out.writeLong(calleeCSeq);
		out.writeLong(callerCSeq);
		out.writeBoolean(storeTerminationInfo);
		if (storeTerminationInfo) {
			out.writeObject(terminationInfo);
		}
	}
	/*
	 * (non-Javadoc)
	 * @see org.mobicents.javax.servlet.sip.ProxyExt#getProxy1xxTimeout()
	 */
	public int getProxy1xxTimeout() {
		return proxy1xxTimeout;
	}

	/*
	 * (non-Javadoc)
	 * @see org.mobicents.javax.servlet.sip.ProxyExt#setProxy1xxTimeout(int)
	 */
	public void setProxy1xxTimeout(int timeout) {
		proxy1xxTimeout = timeout;
		
	}
	/**
	 * @return the proxyTimerService
	 */
	public ProxyTimerService getProxyTimerService() {
		return proxyTimerService;
	}

	public void addProxyBranch(ProxyBranchImpl proxyBranchImpl) {
		if(proxyBranches == null) {
			this.proxyBranches = new LinkedHashMap<URI, ProxyBranchImpl> ();
		}
		this.proxyBranches.put(proxyBranchImpl.getTargetURI(), proxyBranchImpl);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.mobicents.javax.servlet.sip.ProxyExt#storeTerminationInformation(boolean)
	 */
	public void storeTerminationInformation(final boolean store) throws IllegalStateException {
		if(null != finalBranchForSubsequentRequests) 
			throw new IllegalStateException("Proxy has been established.");
		storeTerminationInfo = store;
	}

	/*
	 * (non-Javadoc)
	 * @see org.mobicents.javax.servlet.sip.ProxyExt#terminateSession(javax.servlet.sip.SipSession, int, java.lang.String, int, java.lang.String)
	 */
    public void terminateSession(final SipSession session,
    							 final int calleeResponseCode, final String calleeResponseText,
    							 final int callerResponseCode, final String callerResponseText)
    							 throws IllegalStateException, IOException {
    	if(null == finalBranchForSubsequentRequests) {
			throw new IllegalStateException("Proxy has not yet been established. Before final response use cancel.");
    	}
    	if (!storeTerminationInfo || terminationInfo == null) {
    		logger.error("storeTerminationInfo = " + storeTerminationInfo);
    		if (terminationInfo == null) {
    			logger.error("terminationInfo = null");
    		}
    		throw new IllegalStateException("No termination information stored.Call storeTerminationInformation before final response arrives.");
    	}
    	terminationInfo.terminate(session, callerCSeq, calleeCSeq, calleeResponseCode, calleeResponseText, callerResponseCode, callerResponseText);
    }	

	/**
	 * @return the terminationInfo
	 */
	public boolean isTerminationSent() {
		if(terminationInfo == null) {
			return false;
		}
		return terminationInfo.isTerminationSent();
	}

	/**
	 * @return the bestResponseSent
	 */
	public int getBestResponseSent() {
		return bestResponseSent;
	}

	@Override
	public boolean getSipOutboundSupport() {		
		return sipOutboundSupport;
	}

	@Override
	public void setSipOutboundSupport(boolean sipOutboundSupport) {
		this.sipOutboundSupport = sipOutboundSupport;
	}


}
