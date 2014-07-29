package org.mobicents.servlet.sip.example.utils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.log4j.Logger;


import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

 public class ConnectionPoolableObjectFactory extends BasePoolableObjectFactory<Channel> {

	private static Logger logger = Logger.getLogger(ConnectionPoolableObjectFactory.class);
	
		// private static int counter= 0;
		 private final ConnectionFactory factory;
		 
		 public ConnectionPoolableObjectFactory(String connURL) throws IOException {
				this.factory = new ConnectionFactory();
				try {
					this.factory.setUri(connURL);
				} catch (KeyManagementException e) {
					logger.error("Failed to create RabbitMQ connection factory. Reason : "+ e.getMessage());
					throw new IOException(e);
				} catch (NoSuchAlgorithmException e) {
					logger.error("Failed to create RabbitMQ connection factory. Reason : "+ e.getMessage());
					throw new IOException(e);
				} catch (URISyntaxException e) {
					logger.error("Failed to create RabbitMQ connection factory. Reason : "+ e.getMessage());
					throw new IOException(e);
				}
			}
	    @Override
	    public Channel makeObject() throws Exception {
	    	//logger.info("makeObject is invoked");
	        Channel channel = null;
	        try{ 
	        	channel = factory.newConnection().createChannel();
	        	//logger.info("NEW RABBITMQ CHANNEL IS CREATED. CHANNEL NUMBER : "+channel.getChannelNumber());
	        }
	        catch(IOException e) {
	        	logger.error("IOEXCEPTION CAUGHT TRYING TO CREATE CONNECTION & CHANNEL. msg : "+e.getMessage() );
	        	throw e;
	        }
	        return channel;

	    }

	    @Override
	    public boolean validateObject(Channel channel) {
	    	//logger.info("validateObject is invoked");
	        return channel.isOpen();
	    }

	    @Override
	    public void destroyObject(Channel channel) throws Exception {
	    	
	    	//logger.info("destroyObject is invoked");
	    	try {
	    		channel.close();
	    	}
	    	catch(IOException e) {
	    		logger.error("IOEXCEPTION CAUGHT TRYING TO CLOSE CHANNEL. msg : "+e.getMessage() );
	    		throw e;
	    		
	    	}
	    	
	    	//get the connection object via channel API.
	    	Connection conn = channel.getConnection();
	    	try {
	    		conn.close();
	    	}
	    	catch(IOException e) {
	    		logger.error("IOEXCEPTION CAUGHT TRYING TO CLOSE CONNECTION. msg : "+e.getMessage() );
	    		throw e;
	    	}
	    	//logger.info("Close the channel object and its connection object. Channel number : " +channel.getChannelNumber());
	    	
	    }

	    @Override
	    public void passivateObject(Channel channel) throws Exception {
	    	//logger.info("passivateObject is invoked");
	    	//logger.info("Channel is sent back to queue. Channel number: "+channel.getChannelNumber());
	    }
	}
