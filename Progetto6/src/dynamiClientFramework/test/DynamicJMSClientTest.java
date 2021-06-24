package dynamiClientFramework.test;

import javax.jms.JMSException;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import org.apache.activemq.artemis.jms.client.ActiveMQJMSConnectionFactory;
import dynamiClientFramework.clients.Sample;

public class DynamicJMSClientTest extends DynamicClientTest {
		
	private Queue destination;
	private QueueSender sender;
	private QueueConnection connection;
	private QueueSession session;
	private QueueConnectionFactory factory;
	private boolean connected;
	

	public DynamicJMSClientTest(String destination, String acceptorAddress, boolean pollingServiceTest) {
		super(destination, acceptorAddress, pollingServiceTest);
	}
	
	@Override
	protected PollingServiceTest createPollingService(long pollingPeriod, boolean pollingServiceTest) {
		return new PollingServiceTest(this, pollingPeriod, pollingServiceTest);
	}

				
	/**
	 * Starts connection with the destination queue.
	 */
	@Override
	public void startConnection() {
		factory = new ActiveMQJMSConnectionFactory(super.getAddress());
		try {
			connection = factory.createQueueConnection();
			session = connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
			destination = session.createQueue(super.getDestination());
			sender = session.createSender(destination);	
			connection.start();
			connected=true;
			} catch (JMSException e) {
			System.err.println("Cannot start connection to " + super.getDestination() + " queue\n");
			e.printStackTrace();
		}
	}
	
	/**
	 * Closes connection with the destination queue.
	 */
	@Override
	public void closeConnection() {
		try {
			connected=false;
			sender.close();
			session.close();
			connection.close();
		} catch (JMSException e) {
			System.err.println("Cannot close connection to " + super.getDestination() + " queue\n");
			e.printStackTrace();
		}
	}
	

	@Override
	protected void sendMessage(Sample sample) {
		try {
			ObjectMessage message = session.createObjectMessage(sample);
			sender.send(message);
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean isAlive() {
		return connected;
	}
}
