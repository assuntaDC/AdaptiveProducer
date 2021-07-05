package adaptiveProducerFramework.test;

import adaptiveProducerFramework.adaptiveProducer.Producer;

public class MQTTProducerCreatorTest implements ProducerCreatorTest {

	@Override
	public Producer createProducer(String destination, String acceptorAddress, boolean pollingServiceTest) {
		return new MQTTAdaptiveProducerTest(destination, acceptorAddress, pollingServiceTest);
	}

}
