package test.mqtt;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.sql.Timestamp;
import java.util.UUID;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;

import dynamiClientFramework.clients.Sample;

public class MQTTConsumer{
	public long CONSUMER_PERIOD = 500;
	private String topic;
	private String address;
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
		subscriber.setCallback(new MqttCallback() {
			public void messageArrived(String topic, MqttMessage message) throws Exception {
				Thread.sleep(CONSUMER_PERIOD);
				/*
			        ByteArrayInputStream bis = new ByteArrayInputStream(message.getPayload());
			        Sample s = (Sample) new ObjectInputStream(bis).readObject(); 
			        String time = new Timestamp(System.currentTimeMillis()).toString();
			        System.out.println("\nReceived a Message!" +
			            "\n\tTime:    " + time +
			            "\n\tTopic:   " + topic +
			            "\n\tMessage: " + s.toString() + "\n");*/
			}

			public void connectionLost(Throwable cause) {
				System.out.println("Connection to Solace broker lost!" + cause.getMessage());
			}

			public void deliveryComplete(IMqttDeliveryToken token) {
			}

		});

		subscriber.subscribe(topic);
	}

	public void stopConsuming() throws MqttException{
		subscriber.unsubscribe(topic);
		subscriber.disconnect();
	}

}
