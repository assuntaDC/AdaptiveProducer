package adaptiveProducerFramework.adaptiveProducer;

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

public class PollingService{
	private long pollingPeriod;
	private AdaptiveProducer producer;
	private QueueConnectionFactory factory;
	private QueueSession session;
	private QueueConnection connection;
	private QueueRequestor requestor;
	private ScheduledFuture<?> future;
	private ScheduledExecutorService executor;

	/**
	 * 
	 * @param producer than requires queue/topic monitoring
	 * @param pollingPeriod expressed in milliseconds
	 * @param topic set to true if object has to monitor a topic, false otherwise.
	 */
	public PollingService(AdaptiveProducer producer, long pollingPeriod){
		this.producer=producer;
		this.pollingPeriod=pollingPeriod;
	}

	/**
	 * Start queue/topic monitoring, setting up JMS connection to destination.
	 */
	public void startPolling(){
		try {
			factory = new ActiveMQJMSConnectionFactory(producer.getAddress());	
			connection = factory.createQueueConnection();
			connection.start();
			session = connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
			Queue managementQueue = session.createQueue("activemq.management");
			requestor = new QueueRequestor(session, managementQueue);
			executor = Executors.newSingleThreadScheduledExecutor();
			future = executor.scheduleWithFixedDelay(new PollingServiceThread(), 0,
					pollingPeriod, TimeUnit.MILLISECONDS);					
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Stop queue/topic monitoring, closing JMS connection to destination.
	 */
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

	/**
	 * Checks message count into destination queue and updates producer congestion status.
	 */
	public void polling() {
		Message request;
		try {
			request = session.createMessage();
			JMSManagementHelper.putAttribute(request, ResourceNames.ADDRESS + producer.getDestination(), "MessageCount");				   
			Message reply = requestor.request(request);
			int messageCount = (Integer) JMSManagementHelper.getResult(reply, Integer.class);
			producer.updateQueueStatus(messageCount);
		} catch (JMSException e) {
			e.printStackTrace();
		}
		/*catch(IllegalStateException e) {
			System.out.println("Consumer already closed.");
		}*/
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Executes polling if producer is alive and connection is up.
	 * @author Assunta De Caro, Pietro Vitagliano
	 */
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
