package adaptiveProducerFramework.test;

import adaptiveProducerFramework.producers.Producer;

public interface ProducerCreatorTest {
	public Producer createProducer(String destination, String acceptorAddress, boolean pollingServiceTest);
}
