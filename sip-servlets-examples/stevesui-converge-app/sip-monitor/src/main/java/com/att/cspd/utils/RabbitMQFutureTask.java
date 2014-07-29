package com.att.cspd.utils;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class RabbitMQFutureTask {

	ExecutorService executor;

	public RabbitMQFutureTask() {
		executor = Executors.newFixedThreadPool(2);
	}

	public String queryRabbitMQ(int sleepTime) throws Exception {

		RabbitMQCallable callable = new RabbitMQCallable(sleepTime); //parameter is sleep time in millis

		FutureTask<String> futureTask = new FutureTask<String>(callable);
		executor.execute(futureTask);
		//@SuppressWarnings("unchecked")
		//Future<String> future = (Future<String>) executor.submit(futureTask);
/*
		try {
	        //String result = future.get(500, TimeUnit.MILLISECONDS);
	        String result = future.get();
	        System.out.println("FutureTask output=>>"+result);
	        return result;
	    } catch (Exception e){
	        e.printStackTrace();
	        future.cancel(true); //this method will stop the running underlying task
	        throw new Exception("future failed");
	    }
*/
		
		while (true) {
			try {
				if(futureTask.isDone()){
					System.out.println("Future Task Done");

					String result = futureTask.get();
					System.out.println("FutureTask output=>"+result);
					return result;
				}

				if(!futureTask.isDone()){
					//wait indefinitely for future task to complete
					System.out.println("FutureTask output=>>"+futureTask.get());
				}


			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
	}

}
