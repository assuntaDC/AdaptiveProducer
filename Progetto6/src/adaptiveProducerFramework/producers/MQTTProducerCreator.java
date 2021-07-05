package adaptiveProducerFramework.producers;

public class MQTTProducerCreator implements ProducerCreator {

	@Override
	public Producer createProducer(String destination, String acceptorAddress) {
		return new MQTTAdaptiveProducer(destination, acceptorAddress);
	}

}
