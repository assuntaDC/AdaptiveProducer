package test.mqtt;

import java.sql.Timestamp;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;

public class MQTTConsumer{
	public long CONSUMER_PERIOD = 5000;
	private String topic;
	private String address;
	public int consumed;
	private ScheduledExecutorService executor;
	private ScheduledFuture<?> future;
	private IMqttClient subscriber;

	private int consumerID;
	
	public MQTTConsumer(String topic, String acceptorAddress, int consumerID){
		this.topic = topic;
		this.address = acceptorAddress;	
		this.consumerID = consumerID;
	}
	
	public void startConsuming() throws MqttSecurityException, MqttException{
        String subID = UUID.randomUUID().toString();
		subscriber = new MqttClient(address, subID);
		subscriber.connect();
		executor = Executors.newSingleThreadScheduledExecutor();
		future = executor.scheduleWithFixedDelay(new ConsumeThread(), 0, CONSUMER_PERIOD, TimeUnit.MILLISECONDS);
		subscriber.subscribe(topic);
	}
	
	public void stopConsuming() throws MqttException{
		future.cancel(false);
		executor.shutdown();
		subscriber.unsubscribe(topic);
		subscriber.disconnect();
	}
	
	private class ConsumeThread implements Runnable{
		CountDownLatch latch = new CountDownLatch(1000);
		public void run() {	
			subscriber.setCallback(new MqttCallback() {
			    public void messageArrived(String topic, MqttMessage message) throws Exception {
					consumed++;
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
	        try {
			    latch.await(); // block here until message received, and latch will flip
			} catch (InterruptedException e) {
			    System.out.println("I was awoken while waiting");
			}
		}
	}
	
}
