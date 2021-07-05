package test.jms;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import adaptiveProducerFramework.producers.JMSProducerCreator;
import adaptiveProducerFramework.producers.Producer;
import adaptiveProducerFramework.producers.Sample;
import adaptiveProducerFramework.producers.exceptions.InvalidSampleTTLException;
import adaptiveProducerFramework.test.JMSProducerCreatorTest;

public class NodeDriver{

	private Producer producer;
	public long SENDER_PERIOD = 500;
	private int nodeId;
	public int sent = 0;

	double max = 30;
	double min = 20;
	double range = max - min + 1;
	private ScheduledExecutorService executor;
	private ScheduledFuture<?> future;

	public NodeDriver(String queueName, String acceptorAddress, int id){
		nodeId = id;
		if(id!=1) producer = new JMSProducerCreator().createProducer(queueName, acceptorAddress);
		else producer = new JMSProducerCreatorTest().createProducer(queueName, acceptorAddress, true);
	}

	public void startSending() {
		producer.startProducer();
		executor = Executors.newSingleThreadScheduledExecutor();
		future = executor.scheduleWithFixedDelay(new SendThread(), 0, SENDER_PERIOD, TimeUnit.MILLISECONDS);			
	}

	public void stopSending() {
		producer.stopProducer();
		future.cancel(false);
		executor.shutdown();
	}

	private class SendThread implements Runnable{
		private int id = 1;
		public void run() {
			int temperature = (int) ((int)(Math.random() * range) + min);
			try {
				Sample sample = new Sample(temperature, 500);
				producer.trySending(sample);
				//System.out.println(java.time.LocalTime.now() + " Node n." + nodeId + " - Produced mex n." + id + " : " + temperature + " C");
				id++;
				sent++;
			}catch(InvalidSampleTTLException e) {
				e.printStackTrace();
			}
		}
	}
}
