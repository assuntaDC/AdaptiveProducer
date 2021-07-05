package adaptiveProducerFramework.adaptiveProducer;

import javax.jms.JMSException;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;

import org.apache.activemq.artemis.jms.client.ActiveMQJMSConnectionFactory;

public class JMSAdaptiveProducer extends AdaptiveProducer {

	private Queue destination;
	private QueueSender sender;
	private QueueConnection connection;
	private QueueSession session;
	private QueueConnectionFactory factory;


	/**
	 * Creates a JMSAdaptiveProducer instance.
	 * @param destination queue to connect to.
	 * @param acceptorAddress Artemis valid acceptor to connect to.
	 */
	public JMSAdaptiveProducer(String destination, String acceptorAddress) {
		super(destination, acceptorAddress);
	}

	@Override
	public void startConnection() {
		factory = new ActiveMQJMSConnectionFactory(super.getAddress());
		try {
			connection = factory.createQueueConnection();
			session = connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
			destination = session.createQueue(super.getDestination());
			sender = session.createSender(destination);	
			connection.start();
		} catch (JMSException e) {
			System.err.println("Cannot start connection to " + super.getDestination() + " queue\n");
			e.printStackTrace();
		}
	}

	@Override
	public void closeConnection() {
		try {
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

}
