package test.mqtt;

import java.util.ArrayList;

import javax.jms.JMSException;

import org.eclipse.paho.client.mqttv3.MqttException;

public class MQTTtestProject {
	// ARTEMIS BROKER PATH: cd C:\Lib\apache-artemis-2.17.0\brokerProject\bin
	public static void main(String[] args) throws InterruptedException, JMSException, MqttException{

		String acceptorAddress = "tcp://0.0.0.0:61616";
		String topic = "topic.test";
		int NODES = 1, CONSUMERS = 5;

		long max = 3000;
		long min = 500;
		long range = max - min + 1;

		//set up consumer
		System.out.println("CONSUMERS:" + CONSUMERS);
		ArrayList<MQTTConsumer> consumers = new ArrayList<MQTTConsumer>();
		for(int i=1; i<=CONSUMERS; i++) {
			MQTTConsumer consumer = new MQTTConsumer(topic, acceptorAddress, i);
			consumer.CONSUMER_PERIOD = (long)((long)(Math.random() * range) + min);
			System.out.println("Consumer n." + i + " - consuming period: "+ consumer.CONSUMER_PERIOD + " s");
			consumer.startConsuming();
			consumers.add(consumer);
		}

		//set up node driver
		System.out.println("SENDERS:" + NODES);
		ArrayList<MQTTNodeDriver> nodes = new ArrayList<MQTTNodeDriver>();
		for(int i=1; i<=NODES; i++) {
			MQTTNodeDriver node = new MQTTNodeDriver(topic, acceptorAddress, i);
			node.SENDER_PERIOD = (long)((long)(Math.random() * range) + min);
			System.out.println("Node n." + i + " - sending period: "+ node.SENDER_PERIOD + " s");
			node.startSending();
			nodes.add(node);
		}

		System.out.println("\n----SIMULATION SEEN FROM NODE N.1----\n");

		//Let simulation run
		Thread.sleep(3*1000);

		//stop and clean all
		for(MQTTNodeDriver node: nodes) node.stopSending();
		for(MQTTConsumer consumer: consumers) consumer.stopConsuming();
		System.out.println("----SIMULATION ENDEND----\n");
	}
}
