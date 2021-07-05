package adaptiveProducerFramework.producers.exceptions;

public class InvalidSampleTTLException extends Exception{

	private static final long serialVersionUID = 1L;

	public InvalidSampleTTLException(String message) {
		super(message);
	}

	public InvalidSampleTTLException() {
		super();
	}

}
