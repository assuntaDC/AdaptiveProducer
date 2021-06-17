package dynamiClientFramework.clients;

import java.util.UUID;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;

public class DynamicMQTTClient extends DynamicClient {
	
	private IMqttClient publisher;

	public DynamicMQTTClient(String destination, String acceptorAddress) {
		super(destination, acceptorAddress);
	}

	@Override
	protected void sendMessage(Sample sample) {
        try {
        	MqttMessage msg = new MqttMessage(sample.toString().getBytes()); 
            //msg.setQos(0);
            //msg.setRetained(true);
			publisher.publish(super.getDestination(), msg);
		} catch (MqttPersistenceException e) {
			e.printStackTrace();
		} catch (MqttException e) {
			e.printStackTrace();
		} 
	}

	@Override
	protected void startConnection() {
		try {
			String publisherId = UUID.randomUUID().toString();
			publisher = new MqttClient(super.getAddress(), publisherId);
			MqttConnectOptions options = new MqttConnectOptions();
			options.setAutomaticReconnect(true);
			options.setCleanSession(true);
			options.setConnectionTimeout(10);
			publisher.connect(options);
		} catch (MqttSecurityException e) {
			e.printStackTrace();
		} catch (MqttException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void closeConnection() {
		try {
	        //publisher.close();
			publisher.disconnect();
		} catch (MqttException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected PollingService createPollingService(long pollingPeriod) {
		return new PollingService(this, pollingPeriod, true);
	}

	@Override
	public boolean isAlive() {
		return publisher.isConnected();
	}

}
