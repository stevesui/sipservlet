package com.att.cspd.utils;

import java.util.concurrent.Callable;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;

public class RabbitMQCallable implements Callable<String> {

	private long waitTime;
	 private static final String EXCHANGE_NAME = "encore_topic";
	
	public RabbitMQCallable(int timeInMillis){
	 this.waitTime=timeInMillis;
    }
	
	
	@Override
	public String call() throws Exception {
		/*
		Thread.sleep(waitTime);
		//return the thread name executing this callable task
		return Thread.currentThread().getName();
		*/
		
		String result = "failure";
		
		Connection connection = null;
	    Channel channel = null;
	    try {
	      ConnectionFactory factory = new ConnectionFactory();
	      
	      factory.setHost("asgldlloadtest03");
	      factory.setUsername("sip");
	      factory.setPassword("sip");
	  
	      connection = factory.newConnection();
	      channel = connection.createChannel();
	      
	      System.out.println("Already created connection to RabbitMQ  ");

	      //channel.exchangeDeclare(EXCHANGE_NAME, "topic");
	      
	      //String queueName = channel.queueDeclare().getQueue();
	      
	      String queueName = "sip-monitor-bucket";
	      channel.queueDeclare(queueName, true, false, false, null);
	    
	      System.out.println("Already decalred the queue to RabbitMQ  ");
	      System.out.println("queueName = "+queueName);
	 /*
	      if (argv.length < 1){
	        System.err.println("Usage: ReceiveLogsTopic [binding_key]...");
	        System.exit(1);
	      }
	    
	      for(String bindingKey : argv){    
	        channel.queueBind(queueName, EXCHANGE_NAME, bindingKey);
	      }
	    
	      System.out.println(" [*] Waiting for messages. To exit press CTRL+C");
*/
	      QueueingConsumer consumer = new QueueingConsumer(channel);
	      System.out.println("created consumer = "+consumer);
	      channel.basicConsume(queueName, true, consumer);
	      System.out.println("created channel = "+channel);

	      //while (true) {
	        QueueingConsumer.Delivery delivery = consumer.nextDelivery();
	        System.out.println("created delivery = "+delivery);
	        String message = new String(delivery.getBody());
	        String routingKey = delivery.getEnvelope().getRoutingKey();

	        System.out.println(" [x] Received '" + routingKey + "':'" + message + "'");   
	        result = "Success";;
	        
	  //    }
	    }
	    catch  (Exception e) {
	      e.printStackTrace();
	      
	    }
	    finally {
	      if (connection != null) {
	        try {
	          connection.close();
	          System.out.println("rabbitmq connection closed");
	          
	        }
	        catch (Exception ignore) {}
	      }
	    }
	    System.out.println("result value = "+result+" before returning..... ");
	    return result;
	  }
	

}
