package example;
 
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

/**
 * An example showing how to use message counters to have information on a queue.
 */
public class MessageCounterExample {

   public static void main(final String[] args) throws Exception {
	   String url = "tcp://localhost:61616";
	   String queueName = "acks";
	   
	   QueueConnectionFactory factory = new ActiveMQJMSConnectionFactory(url);
	   try {
		   QueueConnection connection = factory.createQueueConnection();
		   connection.start();
		   
		   QueueSession session = connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
		   Queue managementQueue = session.createQueue("activemq.management");
		   QueueRequestor requestor = new QueueRequestor(session, managementQueue);
		   Message request = session.createMessage();
		   JMSManagementHelper.putAttribute(request, ResourceNames.QUEUE + queueName, "MessageCount");
		   
		   Message reply = requestor.request(request);
		   int messageCount = (Integer) JMSManagementHelper.getResult(reply, Integer.class);
		   System.out.println(queueName + " contains " + messageCount + " messages.");
		   
		   session.close();
		   connection.close();
	   }catch(Exception e) {
		   System.err.println(e);
	   }
   }
}
