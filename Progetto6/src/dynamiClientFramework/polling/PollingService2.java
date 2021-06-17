package dynamiClientFramework.polling;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueRequestor;
import javax.jms.QueueSession;
import javax.jms.Session;

import org.apache.activemq.artemis.api.core.management.ResourceNames;
import org.apache.activemq.artemis.api.jms.management.JMSManagementHelper;
import org.apache.activemq.artemis.jms.client.ActiveMQJMSConnectionFactory;

import dynamiClientFramework.clients.DynamicClient;
import dynamiClientFramework.clients.PollingService;

public class PollingService2 extends PollingService{

	private long pollingPeriod;
	private String resourceName;
	private boolean topic;
	private DynamicClient client;
	private QueueConnectionFactory factory;
	private QueueSession session;
	private QueueConnection connection;
	private QueueRequestor requestor;
	private ScheduledFuture<?> future;
	private ScheduledExecutorService executor;

	public PollingService2(DynamicClient client, long pollingPeriod, boolean topic){
		super(client, pollingPeriod, topic);
		this.client=client;
		this.pollingPeriod=pollingPeriod;
		this.topic=topic;
	}

	public void startPolling(){
		try {
			factory = new ActiveMQJMSConnectionFactory(client.getAddress());	
			connection = factory.createQueueConnection();
			connection.start();
			session = connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
			Queue managementQueue = session.createQueue("activemq.management");
			requestor = new QueueRequestor(session, managementQueue);
			if(topic) resourceName = ResourceNames.ADDRESS;
			else resourceName = ResourceNames.QUEUE;
			executor = Executors.newSingleThreadScheduledExecutor();
			future = executor.scheduleWithFixedDelay(new PollingServiceThread(), 0, pollingPeriod, TimeUnit.MILLISECONDS);					
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}

	public void stopPolling(){
		try {
			future.cancel(false);
			executor.shutdown();
			requestor.close();
			session.close();
			connection.close();
		} catch (JMSException e) {
			e.printStackTrace();
		}		
	}


	public void polling() {
		Message request;
		try {
			request = session.createMessage();
			JMSManagementHelper.putAttribute(request, resourceName + client.getDestination(), "MessageCount");				   
			Message reply = requestor.request(request);
			int messageCount = (Integer) JMSManagementHelper.getResult(reply, Integer.class);		      		   
			System.out.println("\n" + client.getDestination() + " contains " + messageCount + " messages.");
			client.updateQueueStatus(messageCount);

			System.out.println("Congestion Status: " + client.getStatus().toString());
			System.out.println("Strategy: " + client.getStrategy().toString());
			System.out.println("SendBuffer Size: " + client.getBUFFER_DIM());
			System.out.println("Aggressive: " + client.doesAggressiveStrategy());
			System.out.println("SendBuffer messages: " + client.getSendBuffer().size());
			System.out.println("Aggregate mex: " + client.getAggregateCount());
			System.out.println("Aggregate Sent: " + client.getAggregateSent());
			System.out.println("Invalid mex: " + client.getInvalidCount()+"\n");
			//System.out.println("Drop count: " + sup + "\n "); 

		} catch (JMSException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private class PollingServiceThread implements Runnable{
		public void run() {		
			try {
				if(client.isAlive()) polling();
			}catch(Exception e) {
				System.err.println(e);
			}
		}
	}

}