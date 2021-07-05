package adaptiveProducerFramework.test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import adaptiveProducerFramework.adaptiveProducer.Producer;
import adaptiveProducerFramework.adaptiveProducer.Sample;
import adaptiveProducerFramework.producers.exceptions.InvalidPropertyException;
import adaptiveProducerFramework.producers.exceptions.InvalidSampleTTLException;

public abstract class AdaptiveProducerTest implements Producer{

	public enum State {NORMAL, CONGESTED};
	public enum Strategy {DROP, AGGREGABLE};
	public enum Operations {MIN, MAX, SUM, MEAN};

	private int EPSILON;
	private int BUFFER_DIM;
	private int CONSECUTIVE_DECREASES;
	private boolean aggressiveStrategy = false;
	private int MAX_BUFFER_DIM;
	private long TTL;	
	private long POLLING_PERIOD;
	private State status;
	private Strategy strategy;
	private Operations operation;
	protected List<Sample> sendBuffer;
	private	PollingServiceTest ps; 
	private int currMessageCount, decreaseCount;
	private String destination, acceptorAddress;

	private int aggregateCount = 0;	
	private int aggregateSent = 0;	
	private int invalidCount = 0;

	public AdaptiveProducerTest(String destination, String acceptorAddress, boolean pollingServiceTest) {
		this.destination=destination;
		this.acceptorAddress=acceptorAddress;
		loadProperties();
		MAX_BUFFER_DIM = BUFFER_DIM * 2;
		sendBuffer = new ArrayList<Sample>();
		status = State.NORMAL;
		currMessageCount = 0;
		decreaseCount = 0;
		ps = createPollingService(POLLING_PERIOD, pollingServiceTest);
	}

	//PUBLIC INTERFACE*****************************/
	public void trySending(Sample sample){
		switch(status) {
		case NORMAL: 
			if(!sendBuffer.isEmpty()) emptyBuffer();
			sendMessage(sample);
			break;
		case CONGESTED:
			handleStrategy(sample);
			break;
		}
	}

	public void startProducer() {
		startConnection();
		ps.startPolling();
	}

	public void stopProducer() {
		while(!sendBuffer.isEmpty()) emptyBuffer();
		ps.stopPolling();
		closeConnection();
	}

	protected abstract void sendMessage(Sample sample);

	protected abstract void startConnection();

	protected abstract void closeConnection();
	//*********************************************/

	private PollingServiceTest createPollingService(long pollingPeriod, boolean pollingServiceTest) {
		return new PollingServiceTest(this, pollingPeriod, pollingServiceTest);
	}

	private void loadProperties() {
		String filename = "config.properties";
		try (InputStream input = getClass().getClassLoader().getResourceAsStream(filename)) {
			Properties prop = new Properties();
			if (input == null) throw new FileNotFoundException();

			prop.load(input);
			EPSILON = Integer.parseInt(prop.getProperty("epsilon"));
			if(EPSILON<=0) throw new InvalidPropertyException("Epsilon must be greater than 0.");

			CONSECUTIVE_DECREASES = Integer.parseInt(prop.getProperty("consecutiveDecreases"));
			if(CONSECUTIVE_DECREASES<=0) throw new InvalidPropertyException("ConsecutiveDecreases must be greater than 0.");

			BUFFER_DIM = Integer.parseInt(prop.getProperty("bufferDim"));
			if(BUFFER_DIM<=0) throw new InvalidPropertyException("Buffer dim must be greater than 0.");

			POLLING_PERIOD = Long.parseLong(prop.getProperty("pollingPeriod"));
			if(POLLING_PERIOD<=0) throw new InvalidPropertyException("Polling period must be greater than 0.");

			TTL = Long.parseLong(prop.getProperty("TTL"));
			if(TTL<=0) throw new InvalidPropertyException("Sample TTL must be greater than 0.");

			switch(prop.getProperty("strategy")) {
			case "AGGREGABLE": strategy=Strategy.AGGREGABLE; break;
			case "DROP": strategy= Strategy.DROP; break;
			default: throw new InvalidPropertyException("Strategy malformed. Use: AGGREGABLE or DROP."); 
			}

			switch(prop.getProperty("aggregationType")) {
			case "MIN": operation=Operations.MIN; break;
			case "MAX": operation=Operations.MAX; break;
			case "SUM": operation=Operations.SUM; break;
			case "MEAN": operation=Operations.MEAN; break;
			default: throw new InvalidPropertyException("Aggregation type malformed. Use: MAX, MIN, SUM or MEAN."); 
			}  
		} catch (FileNotFoundException ex) {
			System.err.println("Sorry, unable to find " + filename);
		} catch (IOException e) {
			System.err.println("Property file malformed.");
		} catch (InvalidPropertyException e) {
			System.err.println(e.getMessage());
		}
	}


	/**
	 * Sets CONGESTED status checking messageCount
	 * @param messageCount number of current messages within the destination queue.
	 */
	public void updateQueueStatus(int messageCount) {	
		int delta = (messageCount - currMessageCount);
		currMessageCount=messageCount;
		//System.out.println("Delta: " + delta);
		switch(status) {
		case NORMAL: 
			if(messageCount>EPSILON) status=State.CONGESTED;
			decreaseCount = 0;
			break;
		case CONGESTED:
			if(delta<=0) {
				decreaseCount++;
				if(delta<=0) {
					decreaseCount++;
					if(decreaseCount >= CONSECUTIVE_DECREASES) {
						if(messageCount<=EPSILON) status=State.NORMAL;
						aggressiveStrategy=false;	
					}

				}
			}
			else if(delta>0) {
				aggressiveStrategy=true;
				decreaseCount = 0;
			}
			break;
		}

		//If strategy must be more aggressive, we need to extend buffer dimension.
		//If strategy must be less aggressive, we need to reduce buffer dimension;
		//before resizing it, we must be sure there are no more sample than buffer half dim.
		//In this case, we have to aggregate sample or empty the buffer, otherwise we can reduce its length.
		if(aggressiveStrategy && BUFFER_DIM<MAX_BUFFER_DIM) BUFFER_DIM = MAX_BUFFER_DIM;
		else if(!aggressiveStrategy && BUFFER_DIM==MAX_BUFFER_DIM) {
			if(sendBuffer.size()>=BUFFER_DIM) {
				if(strategy==Strategy.AGGREGABLE) aggregate();
				else emptyBuffer();
			}
			BUFFER_DIM /=2;
		}
	}


	private void handleStrategy(Sample sample) {
		switch(strategy) {
		case DROP: 
			drop(sample);
			break;
		case AGGREGABLE:
			sendBuffer.add(sample);
			aggregate();
			break;
		}
	}


	private void emptyBuffer() {
		int i;
		if(sendBuffer.size()<=BUFFER_DIM/2) {
			for(Sample sample: sendBuffer) {
				if(sample.isValid()) {
					aggregateCount--;
					sendMessage(sample);
				}else invalidCount++;
			}
			sendBuffer.clear();
		}
		else {
			for(i=0; i<BUFFER_DIM/2; i++){
				Sample sample = sendBuffer.get(i);
				if(sample!=null && sample.isValid()) {
					sendMessage(sample);
					aggregateCount--;
				}else if(sample!=null) invalidCount++;
			}
			for(int j = i; j>=0; j--) sendBuffer.remove(j);
		}
	} 

	private void drop(Sample sample) {
		if(sendBuffer.size() < BUFFER_DIM) sendBuffer.add(sample);
		else {
			emptyBuffer();
			sendBuffer.add(sample);
		}
	}

	private void aggregate() {
		aggregateCount++;
		if(sendBuffer.size()>=BUFFER_DIM){
			Serializable value = computeAggregation();
			if(value!=null) {
				try {
					sendMessage(new Sample(value, TTL));
				} catch (InvalidSampleTTLException e) {
					e.printStackTrace();
				}
				aggregateSent++;
			}
			sendBuffer.clear();
		}
	}

	private Serializable computeAggregation() {
		switch(operation) {
		case MIN: return computeMin();
		case MAX: return computeMax();
		case SUM: return computeSum();
		case MEAN: return computeMean();
		default: return null;
		}
	}

	private Serializable computeMin() {
		double min = (double) sendBuffer.get(0).getValue();
		int validCount = 0;
		for(int i=1; i<sendBuffer.size(); i++) {
			Sample s = sendBuffer.get(i);
			if(s.isValid()) {
				double value = (double) s.getValue();
				if(value < min) min = value;
				validCount++;
			}else invalidCount++;
		}
		if(validCount>0)//aggregate at least two samples
			return min;
		else return null;
	}

	private Serializable computeMax() {
		double max = (double) sendBuffer.get(0).getValue();
		int validCount = 0;
		for(int i=1; i<sendBuffer.size(); i++) {
			Sample s = sendBuffer.get(i);
			if(s.isValid()) {
				double value = (double) s.getValue();
				if(value > max) max = value;
				validCount++;
			}else invalidCount++;
		}
		if(validCount>0)//aggregate at least two samples
			return max;
		else return null;
	}

	private Serializable computeSum() {
		double sum = 0.0;
		int validCount = 0;
		for(Sample s: sendBuffer) {
			if(s.isValid()) {
				sum += (double) s.getValue();
				validCount++;
			}else invalidCount++;
		}
		if(validCount>0)//aggregate at least two samples
			return sum;
		else return null;
	}

	private Serializable computeMean() {
		double mean = 0.0;
		int validCount = 0;
		for(Sample s: sendBuffer) {
			if(s.isValid()) {
				mean += (Double) s.getValue();
				validCount++;
			}else invalidCount++;
		}
		if(validCount>0) {
			//aggregate at least two samples
			mean = mean / (double) validCount;
			return mean;
		}
		else return null;
	}


	public String getDestination() {
		return destination;
	}

	public String getAddress() {
		return acceptorAddress;
	}

	public int getBUFFER_DIM() {
		return BUFFER_DIM;
	}

	public boolean doesAggressiveStrategy() {
		return aggressiveStrategy;
	}

	public State getStatus() {
		return status;
	}

	public Strategy getStrategy() {
		return strategy;
	}

	public int getCurrMessageCount() {
		return currMessageCount;
	}

	public int getAggregateCount() {
		return aggregateCount;
	}

	public int getAggregateSent() {
		return aggregateSent;
	}

	public int getInvalidCount() {
		return invalidCount;
	}

	public List<Sample> getSendBuffer() {
		return sendBuffer;
	}

}
