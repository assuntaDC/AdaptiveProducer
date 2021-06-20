package test.jms;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.jms.JMSException;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSession;
import javax.jms.Session;

import org.apache.activemq.artemis.jms.client.ActiveMQQueueConnectionFactory;

public class Consumer{
	public long CONSUMER_PERIOD = 1000;
	private String queueName;
	private String address;
	public int consumed;
	
	private ScheduledExecutorService executor;
	private ScheduledFuture<?> future;
	private QueueSession session;
	private QueueConnection connection;
	private QueueReceiver receiver;
	private QueueConnectionFactory connFactory;
	private int consumerID;
	
	public Consumer(String queueName, String address, int consumerID){
		this.queueName = queueName;
		this.address = address;	
		this.consumerID = consumerID;
	}
	
	public void startConsuming() throws JMSException {
		connFactory = new ActiveMQQueueConnectionFactory(address);
		connection = connFactory.createQueueConnection();
		session = connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
		Queue myQueue = session.createQueue(queueName);
		receiver = session.createReceiver(myQueue);
		connection.start();
		executor = Executors.newSingleThreadScheduledExecutor();
		future = executor.scheduleWithFixedDelay(new ConsumeThread(), 0, CONSUMER_PERIOD, TimeUnit.MILLISECONDS);			
	}
	
	public void stopConsuming() throws JMSException {
		future.cancel(false);
		executor.shutdown();
		receiver.close();
		session.close();
		connection.close();
		//System.out.println("Consumer n." + consumerID + " Total consumed: " + consumed);
	}
	
	private class ConsumeThread implements Runnable{
		public void run() {	
			ObjectMessage mex;
			try {
				mex = (ObjectMessage) receiver.receive();
				consumed++;
				//System.out.println(java.time.LocalTime.now() + "Consumer n." + consumerID + " Consumed: "+ (int) ((Sample)mex.getObject()).getValue());
			} catch (JMSException e) {
				e.printStackTrace();
			}
			
		}
	}
	
}
