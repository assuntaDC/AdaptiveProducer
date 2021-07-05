package adaptiveProducerFramework.producers;

public interface ProducerCreator {
	/**
	 * Hide Adaptive Producer creation.
	 * @param destination queue/topic to connect to-
	 * @param acceptorAddress Artemis valid acceptor address.
	 * @return a Producer instance.
	 */
	public Producer createProducer(String destination, String acceptorAddress);
}
