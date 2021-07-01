package dynamiClientFramework.test;

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

public class PollingServiceTest{

	private long pollingPeriod;
	private DynamicClientTest client;
	private QueueConnectionFactory factory;
	private QueueSession session;
	private QueueConnection connection;
	private QueueRequestor requestor;
	private ScheduledFuture<?> future;
	private ScheduledExecutorService executor;
	private boolean test;
	private Metric m;

	public PollingServiceTest(DynamicClientTest client, long pollingPeriod, boolean test){
		this.client=client;
		this.pollingPeriod=pollingPeriod;
		this.test = test;
		
	}

	public void startPolling(){
		try {
			factory = new ActiveMQJMSConnectionFactory(client.getAddress());	
			connection = factory.createQueueConnection();
			connection.start();
			session = connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
			Queue managementQueue = session.createQueue("activemq.management");
			requestor = new QueueRequestor(session, managementQueue);
			m =  new Metric(1);
			if(client.getStatus()==DynamicClientTest.State.CONGESTED) m.startCongestionTimer();
			executor = Executors.newSingleThreadScheduledExecutor();
			future = executor.scheduleWithFixedDelay(new PollingServiceThread(), 0, pollingPeriod, TimeUnit.MILLISECONDS);					
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}

	public void stopPolling(){
		try {
			m.stopCongestionTimer();
			m.print();
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
			JMSManagementHelper.putAttribute(request, ResourceNames.ADDRESS + client.getDestination(), "MessageCount");				   
			Message reply = requestor.request(request);
			int messageCount = (Integer) JMSManagementHelper.getResult(reply, Integer.class);		      		   
			System.out.println("\n" + client.getDestination() + " contains " + messageCount + " messages.");
			client.updateQueueStatus(messageCount);

			if(test) {
				m.trackQueueCount(messageCount);
				System.out.println("Congestion Status: " + client.getStatus().toString());
				System.out.println("Strategy: " + client.getStrategy().toString());
				System.out.println("SendBuffer Size: " + client.getBUFFER_DIM());
				System.out.println("Aggressive: " + client.doesAggressiveStrategy());
				System.out.println("SendBuffer messages: " + client.getSendBuffer().size());
				System.out.println("Aggregate mex: " + client.getAggregateCount());
				System.out.println("Aggregate Sent: " + client.getAggregateSent());
				System.out.println("Invalid mex: " + client.getInvalidCount()+"\n");
				if(client.getStatus()==DynamicClientTest.State.CONGESTED) m.startCongestionTimer();
				else m.stopCongestionTimer();
			}
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
				polling();
			}catch(Exception e) {
				System.err.println(e);
			}
		}
	}

}
