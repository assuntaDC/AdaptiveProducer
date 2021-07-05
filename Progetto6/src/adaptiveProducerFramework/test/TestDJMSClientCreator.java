package dynamiClientFramework.test;

import dynamiClientFramework.clients.Client;

public class TestDJMSClientCreator implements TestDynamicClientCreator {
	@Override
	public Client createDynamicClient(String destination, String acceptorAddress, boolean pollingServiceTest) {
		return new DynamicJMSClientTest(destination, acceptorAddress, pollingServiceTest);
	}
}
