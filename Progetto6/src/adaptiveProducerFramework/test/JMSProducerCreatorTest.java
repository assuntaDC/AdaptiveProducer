package adaptiveProducerFramework.test;

import adaptiveProducerFramework.producers.Producer;

public class JMSProducerCreatorTest implements ProducerCreatorTest {
	@Override
	public Producer createProducer(String destination, String acceptorAddress, boolean pollingServiceTest) {
		return new JMSAdaptiveProducerTest(destination, acceptorAddress, pollingServiceTest);
	}
}
