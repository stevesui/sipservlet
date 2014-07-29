package org.mobicents.servlet.sip.example.utils;

import java.util.NoSuchElementException;

import org.apache.commons.pool.ObjectPool;
import org.apache.log4j.Logger;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.MessageProperties;


public class PublishWorker implements Runnable {

	private static Logger logger = Logger.getLogger(PublishWorker.class);

	protected String  message = "";
	protected ObjectPool<Channel> pool;
	public PublishWorker(String message, ObjectPool<Channel> pool) {
		// TODO Auto-generated constructor stub
		this.message = message;
		this.pool = pool;
	}
	public void run(){
		Channel channel = null;
		try {

			//request a channel from connection pool.
		 channel = pool.borrowObject();  

			channel.queueDeclare("task_queue", true, false, false, null);
			channel.basicPublish( "", "task_queue", 
					MessageProperties.PERSISTENT_TEXT_PLAIN,
					message.getBytes());

		} catch (NoSuchElementException e) {
			logger.error("PublishWorker NoSuchElementException caught: "+e.getMessage());
			e.printStackTrace();
		} catch (IllegalStateException e) {
			logger.error("PublishWorker IllegalStateException caught: "+e.getMessage());
		} catch (Exception e) {
			logger.error("PublishWorker Exception caught: "+e.getMessage());
		} 
		finally {
			//return the channel to connection pool
			if(channel !=  null) {
				try {
					pool.returnObject(channel);
				} catch (Exception e) {
					logger.error("PublishWorker encounters Exception when returning channel object to pool: "+e.getMessage());
				}
			}
			
		}
	}






}
