package test.mqtt;

import java.util.ArrayList;

import javax.jms.JMSException;

import org.eclipse.paho.client.mqttv3.MqttException;

public class MQTTtestProject {
	// ARTEMIS BROKER PATH: cd C:\Lib\apache-artemis-2.17.0\brokerProject\bin
	public static void main(String[] args) throws InterruptedException, JMSException, MqttException{
		
		String acceptorAddress = "tcp://0.0.0.0:61616";
		String topic = "topic.test";
		int NODES = 3, CONSUMERS = 1;
		
		
		//set up consumer
		System.out.println("CONSUMERS:" + CONSUMERS);
		ArrayList<MQTTConsumer> consumers = new ArrayList<MQTTConsumer>();
		for(int i=1; i<=CONSUMERS; i++) {
			MQTTConsumer consumer = new MQTTConsumer(topic, acceptorAddress, i);
			//if(i==1 || i==3) consumer.CONSUMER_PERIOD = 500;
			System.out.println("Consumer n." + i + " - consuming rate: "+Math.round(1.0/((double)consumer.CONSUMER_PERIOD/1000.0)) + " packets/s");
			consumer.startConsuming();
			consumers.add(consumer);
		}
		
		//set up node driver
		System.out.println("SENDERS:" + NODES);
		ArrayList<MQTTNodeDriver> nodes = new ArrayList<MQTTNodeDriver>();
		for(int i=1; i<=NODES; i++) {
			MQTTNodeDriver node = new MQTTNodeDriver(topic, acceptorAddress, i);
			if(i%2==0) node.SENDER_PERIOD *= 2;
			System.out.println("Node n." + i + " - sending rate: "+Math.round(1.0/((double)node.SENDER_PERIOD/1000.0)) + " packets/s");
			node.startSending();
			nodes.add(node);
		}
		
		System.out.println("\n----SIMULATION SEEN FROM NODE N.1----\n");

		//Let simulation run
		Thread.sleep(10000);
		
		//stop and clean all
		int sent = 0, consumed = 0;
		for(MQTTNodeDriver node: nodes) {
			node.stopSending();
			sent += node.sent;
		}
		for(MQTTConsumer consumer: consumers) {
			consumer.stopConsuming();
			consumed += consumer.consumed;
		}
		
		System.out.println("----SIMULATION ENDEND----\n");/*
		System.out.println("Total sent: " + sent);
		System.out.println("Total sent by n.1: " + nodes.get(0).sent);
		System.out.println("Total received: " + consumed);
		*/
	}
}
