package example;

import java.util.HashMap;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.api.core.management.ObjectNameBuilder;
import org.apache.activemq.artemis.api.core.management.QueueControl;

public class JMXProbe2 {
	public static void main(String[] args) {
		String jmx_url = "service:jmx:rmi:///jndi/rmi://localhost:1099/jmxrmi";
		
		try {
			//ObjectName on = ObjectNameBuilder.create("org.apache.activemq.artemis", "0.0.0.0").getQueueObjectName(SimpleString.toSimpleString("MessageCountTestQueue"), SimpleString.toSimpleString("MessageCountTestQueue"), RoutingType.ANYCAST);
			ObjectName on = ObjectNameBuilder.create("org.apache.activemq.artemis", "0.0.0.0").getQueueObjectName(SimpleString.toSimpleString("MQTT Example"), SimpleString.toSimpleString("subscriber 1.MQTT Example"), RoutingType.MULTICAST);
			
	        HashMap<String, String[]> env = new HashMap<String, String[]>();
	        String[] creds = {"brokerProject", "brokerProject"};
	        env.put(JMXConnector.CREDENTIALS, creds);
	        
	        JMXConnector connector = JMXConnectorFactory.connect(new JMXServiceURL(jmx_url), env);
	        MBeanServerConnection mbsc = connector.getMBeanServerConnection();
	        QueueControl queueControl = MBeanServerInvocationHandler.newProxyInstance(mbsc, on, QueueControl.class, false);
	        long messageCount = queueControl.getMessageCount();
			
			System.out.println("MessageCountTestQueue contains " + messageCount + " messages.");
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
