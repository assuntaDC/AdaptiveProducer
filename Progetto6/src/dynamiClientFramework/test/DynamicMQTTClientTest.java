package dynamiClientFramework.test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.UUID;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import dynamiClientFramework.clients.Sample;

public class DynamicMQTTClientTest extends DynamicClientTest {
	
	private IMqttClient publisher;

	public DynamicMQTTClientTest(String destination, String acceptorAddress, boolean pollingServiceTest) {
		super(destination, acceptorAddress, pollingServiceTest);
	}

	@Override
	protected void sendMessage(Sample sample) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(sample);
            oos.flush();
            byte [] data = bos.toByteArray();
        	MqttMessage msg = new MqttMessage(data); 
            msg.setQos(0);
            //msg.setRetained(true);
			publisher.publish(super.getDestination(), msg);
		} catch (MqttPersistenceException e) {
			e.printStackTrace();
		} catch (MqttException e) {
			e.printStackTrace();
		} catch (IOException e) {
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
	protected PollingServiceTest createPollingService(long pollingPeriod, boolean pollingServiceTest) {
		return new PollingServiceTest(this, pollingPeriod, pollingServiceTest);
	}

	
	@Override
	public boolean isAlive() {
		return publisher.isConnected();
	}

}
