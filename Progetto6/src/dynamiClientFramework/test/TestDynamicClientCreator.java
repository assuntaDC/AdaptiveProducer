package dynamiClientFramework.test;

import dynamiClientFramework.clients.Client;

public interface TestDynamicClientCreator {
	public Client createDynamicClient(String destination, String acceptorAddress, boolean pollingServiceTest);
}
