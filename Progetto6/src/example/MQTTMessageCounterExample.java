package example;

import java.sql.Timestamp;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MQTTMessageCounterExample {

	public static void main(String[] args) throws Exception {
		String publisherId = UUID.randomUUID().toString();
		IMqttClient publisher = new MqttClient("tcp://localhost:1883", publisherId);
		
		MqttConnectOptions options = new MqttConnectOptions();
		options.setAutomaticReconnect(true);
		options.setCleanSession(true);
		options.setConnectionTimeout(10);
		publisher.connect(options);
		
		double temp =  20;        
        byte[] payload = String.format("T:%04.2f",temp).getBytes();        
        MqttMessage msg = new MqttMessage(payload); 
        
        msg.setQos(0);
        msg.setRetained(true);
        String topic = "topic.test";
        publisher.publish(topic, msg); 
        
        CountDownLatch latch = new CountDownLatch(1000);
        String subID = UUID.randomUUID().toString();
		IMqttClient subscriber = new MqttClient("tcp://localhost:1883", subID);
		subscriber.connect();
		
		subscriber.setCallback(new MqttCallback() {
		    public void messageArrived(String topic, MqttMessage message) throws Exception {
		        String time = new Timestamp(System.currentTimeMillis()).toString();
		        System.out.println("\nReceived a Message!" +
		            "\n\tTime:    " + time +
		            "\n\tTopic:   " + topic +
		            "\n\tMessage: " + new String(message.getPayload()) +
		            "\n\tQoS:     " + message.getQos() + "\n");
		        latch.countDown(); // unblock main thread
		    }

		    public void connectionLost(Throwable cause) {
		        System.out.println("Connection to Solace broker lost!" + cause.getMessage());
		        latch.countDown();
		    }

		    public void deliveryComplete(IMqttDeliveryToken token) {
		    }

		});
        subscriber.subscribe(topic);     
        try {
            latch.await(); // block here until message received, and latch will flip
        } catch (InterruptedException e) {
            System.out.println("I was awoken while waiting");
        }
   
	    publisher.disconnect();
        publisher.close();
		
	}
	

}
