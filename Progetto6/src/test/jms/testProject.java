package test.jms;

import java.util.ArrayList;
import javax.jms.JMSException;

public class testProject {
	// ARTEMIS BROKER PATH: cd C:\Lib\apache-artemis-2.17.0\brokerProject\bin
	public static void main(String[] args) throws InterruptedException, JMSException{
		
		String acceptorAddress = "tcp://localhost:61616";
		String queueName = "testQueue";
		int NODES = 10, CONSUMERS = 3;
		
		long max = 3000;
	    long min = 500;
	    long range = max - min + 1;
		
		//set up node driver
		System.out.println("SENDERS:" + NODES);
		ArrayList<NodeDriver> nodes = new ArrayList<NodeDriver>();
		for(int i=1; i<=NODES; i++) {
			NodeDriver node = new NodeDriver(queueName, acceptorAddress, i);
			long period = 0;
			while(period==0) period = ((long)(Math.random() * range) + min);
			node.SENDER_PERIOD = period;
			//System.out.println("Node n." + i + " - sending rate: "+ Math.round(1.0/((double)node.SENDER_PERIOD/1000.0)) + " packets/s");
			System.out.println("Node n." + i + " - sending period: "+ node.SENDER_PERIOD + " s");
			node.startSending();
			nodes.add(node);
		}
				
		//set up consumer
		System.out.println("CONSUMERS:" + CONSUMERS);
		ArrayList<Consumer> consumers = new ArrayList<Consumer>();
		for(int i=1; i<=CONSUMERS; i++) {
			Consumer consumer = new Consumer(queueName, acceptorAddress, i);
			long period = 0;
			while(period==0) period = ((long)(Math.random() * range) + min);
			consumer.CONSUMER_PERIOD = period;
			//System.out.println("Consumer n." + i + " - consuming rate: "+ Math.round(1.0/((double)consumer.CONSUMER_PERIOD/1000.0))/1000 + " packets/s");
			System.out.println("Consumer n." + i + " - consuming period: "+ consumer.CONSUMER_PERIOD + " s");
			consumer.startConsuming();
			consumers.add(consumer);
		}
		
		System.out.println("\n----SIMULATION SEEN FROM NODE N.1----\n");

		//Let simulation run
		Thread.sleep(60*1000);
				
		//stop and clean all
		for(NodeDriver node: nodes) node.stopSending();
		for(Consumer consumer: consumers) consumer.stopConsuming();
		System.out.println("----SIMULATION ENDEND----\n");		
	}
}
