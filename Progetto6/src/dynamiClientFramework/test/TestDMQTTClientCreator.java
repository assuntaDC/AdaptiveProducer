package dynamiClientFramework.test;

import dynamiClientFramework.clients.Client;

public class TestDMQTTClientCreator implements TestDynamicClientCreator {

	@Override
	public Client createDynamicClient(String destination, String acceptorAddress, boolean pollingServiceTest) {
		return new DynamicMQTTClientTest(destination, acceptorAddress, pollingServiceTest);
	}

}
