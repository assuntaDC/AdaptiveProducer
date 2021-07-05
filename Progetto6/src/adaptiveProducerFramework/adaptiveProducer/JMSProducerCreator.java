package adaptiveProducerFramework.adaptiveProducer;

public class JMSProducerCreator implements ProducerCreator {

	@Override
	public Producer createProducer(String destination, String acceptorAddress) {
		return new JMSAdaptiveProducer(destination, acceptorAddress);
	}

}
