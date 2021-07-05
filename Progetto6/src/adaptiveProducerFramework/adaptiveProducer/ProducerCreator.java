package adaptiveProducerFramework.adaptiveProducer;

public interface ProducerCreator {
	/**
	 * Hide Dynamic Client creation.
	 * @param destination queue/topic to connect to-
	 * @param acceptorAddress Artemis valid acceptor address.
	 * @return a Client instance.
	 */
	public Producer createProducer(String destination, String acceptorAddress);
}
