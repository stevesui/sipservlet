/**
 * 
 */
package org.mobicents.servlet.sip.arquillian.testsuite.simple;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.text.ParseException;
import java.util.concurrent.Semaphore;
import java.util.logging.Logger;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import javax.sip.message.Response;

import org.cafesip.sipunit.SipCall;
import org.cafesip.sipunit.SipPhone;
import org.cafesip.sipunit.SipStack;
import org.jboss.arquillian.container.mss.extension.SipStackTool;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:gvagenas@gmail.com">George Vagenas</a>
 * 
 */
@RunWith(Arquillian.class)
public class MultiHomeUASTest 
{

	private final Logger logger = Logger.getLogger(MultiHomeUASTest.class.getName());

	@ArquillianResource
	private Deployer deployer;

	private SipStack receiver;
	private SipCall sipCallReceiver;
	private SipPhone sipPhoneReceiver;
	private String receiverPort = "5058";
	private String receiverURI = "sip:receiver@sip-servlets.com";

	private SipStack sender;
	private SipCall sipCallSender;
	private SipPhone sipPhoneSender;
	private String senderPort = "5080";
	private String senderURI = "sip:sender@sip-servlets.com";

	private int proxyPort = 5070;

	private static final int TIMEOUT = 10000;	

	public final static String[] ALLOW_HEADERS = new String[] {"INVITE","ACK","CANCEL","OPTIONS","BYE","SUBSCRIBE","NOTIFY","REFER"};

	private static SipStackTool receiverSipStackTool;
	private static SipStackTool senderSipStackTool;
	private String testArchive = "simple";
	private Boolean isDeployed = false;

	long cseqInt = 0;
	Semaphore semaphore = new Semaphore(1);

	@BeforeClass
	public static void beforeClass(){
		receiverSipStackTool = new SipStackTool("MultiHomeUASTest_receiver");
		senderSipStackTool = new SipStackTool("MultiHomeUASTest_sender");
	}

	@Before
	public void setUp() throws Exception
	{
		//Create the sipCall and start listening for messages
		receiver = receiverSipStackTool.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", receiverPort, "127.0.0.1:5070");
		sipPhoneReceiver = receiver.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, proxyPort, receiverURI);
		sipCallReceiver = sipPhoneReceiver.createSipCall();
		sipCallReceiver.listenForIncomingCall();

		sender = senderSipStackTool.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", senderPort, "127.0.0.1:5070");
		sipPhoneSender = sender.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, proxyPort, senderURI);
		sipCallSender = sipPhoneSender.createSipCall();
		sipCallSender.listenForIncomingCall();
	}

	@After
	public void tearDown() throws Exception
	{
		//Log last Error, Exception, and returnCode
		logger.info("Error Message: "+sipCallSender.getErrorMessage());
		logger.info("Exception Message: "+sipCallSender.getException());
		logger.info("Return Code: "+sipCallSender.getReturnCode());

		logger.info("About to un-deploy the application");
		if (isDeployed)
			deployer.undeploy(testArchive);
		if(sipCallSender != null)	sipCallSender.disposeNoBye();
		if(sipPhoneSender != null) sipPhoneSender.dispose();
		if(sender != null) sender.dispose();

		if(sipCallReceiver != null) sipCallReceiver.disposeNoBye();
		if(sipPhoneReceiver != null) sipPhoneReceiver.dispose();
		if(receiver != null) receiver.dispose();

	}

	@Deployment(name="simple", managed=false, testable=false)
	public static WebArchive createTestArchive()
	{
		WebArchive webArchive = ShrinkWrap.create(WebArchive.class, "shootmesipservlet.war");
		webArchive.addClasses(SimpleSipServlet.class);
		webArchive.addAsWebInfResource("in-container-sip.xml", "sip.xml");

		return webArchive;
	}

	// -------------------------------------------------------------------------------------||
	// -------------------------------------------------------------------------------------||
	// 											Tests                                       ||
	// -------------------------------------------------------------------------------------||
	// -------------------------------------------------------------------------------------||

	@Test
	public void testShootme() throws InterruptedException, SipException, ParseException, InvalidArgumentException {

		logger.info("About to deploy the application");
		deployer.deploy(testArchive);
		isDeployed = true;

		sipCallSender.initiateOutgoingCall(receiverURI, null);

		assertTrue(sipCallSender.waitForAnswer(TIMEOUT));
		assertEquals(Response.OK, sipCallSender.getLastReceivedResponse().getStatusCode());
		assertTrue(sipCallSender.sendInviteOkAck());
		assertTrue(sipCallSender.disconnect());	
		Thread.sleep(1000);
		assertTrue(sipCallSender.getLastReceivedResponse().getStatusCode() == Response.OK);
	}
}
