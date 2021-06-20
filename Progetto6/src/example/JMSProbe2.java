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

public class JMSProbe2 {
	public static void main(String[] args) {
		String url = "tcp://0.0.0.0:61616";
		
		//"org.apache.activemq.artemis:broker=\"0.0.0.0\",component=addresses,address=\"MessageCountTestTopic\",subcomponent=queues,routing-type=\"multicast\",queue=\"subscriber 1.MessageCountTestTopic\""
		
		//"org.apache.activemq.artemis:broker=&quot;0.0.0.0&quot;,component=addresses,address=&quot;MessageCountTestTopic&quot;,subcomponent=queues,routing-type=&quot;multicast&quot;,queue=&quot;subscriber 1.MessageCountTestTopic&quot;"
		
		QueueConnectionFactory factory = new ActiveMQJMSConnectionFactory(url);
		try {
			QueueConnection connection = factory.createQueueConnection("brokerProject", "brokerProject");
			connection.start();
			QueueSession session = connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
			
			Queue managementQueue = session.createQueue("activemq.management");
			
			QueueRequestor requestor = new QueueRequestor(session, managementQueue);
			
			Message request = session.createMessage();
			JMSManagementHelper.putAttribute(request, ResourceNames.ADDRESS + "topic.test", "MessageCount");
			//JMSManagementHelper.putAttribute(request, "queue.topic.test", "MessageCount");
			Message reply = requestor.request(request);
			
			int messageCount = (int) JMSManagementHelper.getResult(reply, Integer.class);
	        System.out.println("topic.test contains " + messageCount + " messages.");
			
			session.close();
			connection.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
