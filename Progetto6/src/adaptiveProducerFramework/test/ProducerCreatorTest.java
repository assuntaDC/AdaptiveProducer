package adaptiveProducerFramework.test;

import adaptiveProducerFramework.adaptiveProducer.Producer;

public interface ProducerCreatorTest {
	public Producer createProducer(String destination, String acceptorAddress, boolean pollingServiceTest);
}
