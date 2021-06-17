package test.jms;

import java.util.ArrayList;

import javax.jms.JMSException;

public class testProject {
	// ARTEMIS BROKER PATH: cd C:\Lib\apache-artemis-2.17.0\brokerProject\bin
	public static void main(String[] args) throws InterruptedException, JMSException{
		
		String acceptorAddress = "tcp://localhost:61616";
		String brokerAddress = "tcp://0.0.0.0:61616";
		String queueName = "testQueue";
		int NODES = 10, CONSUMERS = 5;
		
		//set up node driver
		System.out.println("SENDERS:" + NODES);
		ArrayList<NodeDriver> nodes = new ArrayList<NodeDriver>();
		for(int i=1; i<=NODES; i++) {
			NodeDriver node = new NodeDriver(queueName, acceptorAddress, i);
			if(i%2==0) node.SENDER_PERIOD *= 2;
			System.out.println("Node n." + i + " - sending rate: "+Math.round(1.0/((double)node.SENDER_PERIOD/1000.0)) + " packets/s");
			node.startSending();
			nodes.add(node);
		}
				
		//set up consumer
		System.out.println("CONSUMERS:" + CONSUMERS);
		ArrayList<Consumer> consumers = new ArrayList<Consumer>();
		for(int i=1; i<=CONSUMERS; i++) {
			Consumer consumer = new Consumer(queueName, acceptorAddress, i);
			//if(i==1 || i==3) consumer.CONSUMER_PERIOD = 500;
			System.out.println("Consumer n." + i + " - consuming rate: "+Math.round(1.0/((double)consumer.CONSUMER_PERIOD/1000.0)) + " packets/s");
			consumer.startConsuming();
			consumers.add(consumer);
		}
		
		System.out.println("\n----SIMULATION SEEN FROM NODE N.1----\n");

		//Let simulation run
		Thread.sleep(30000);
		
		//stop and clean all
		int sent = 0, consumed = 0;
		for(NodeDriver node: nodes) {
			node.stopSending();
			sent += node.sent;
		}
		for(Consumer consumer: consumers) {
			consumer.stopConsuming();
			consumed += consumer.consumed;
		}
		
		System.out.println("----SIMULATION ENDEND----\n");
		System.out.println("Total sent: " + sent);
		System.out.println("Total sent by n.1: " + nodes.get(0).sent);
		System.out.println("Total received: " + consumed);
		
	}
}
