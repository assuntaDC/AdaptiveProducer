package dynamiClientFramework.test;
import java.util.Formatter;

public class Metric {

	private int id;
	private int congestionCount = 0;
	private long congestionTime = 0;
	private long duration = 0;
	private long longestCongestionPeriod = 0;
	private boolean tracking = false;
	private int messageQueueMean = 0;
	private int pollingCount = 0;

	public Metric(int id) {this.id = id;}

	public void startCongestionTimer() {
		if(!tracking) {
			duration = System.currentTimeMillis();
			congestionCount++;
			tracking = true;
		}
	}	

	public void trackQueueCount(int messages) {
		messageQueueMean += messages;
		pollingCount++;
	}

	public void stopCongestionTimer() {
		if(tracking) {
			duration = System.currentTimeMillis() - duration;
			congestionTime += duration;
			if(duration > longestCongestionPeriod) longestCongestionPeriod = duration;
			tracking=false;
		}
	}

	public void print() {		
		Formatter formatter = new Formatter();
		System.out.println(formatter.format("%20s %30s %30s %30s %30s", "Metric Id", "CongestionCount", "TotalCongestionTime", "LongestCongestionTime", "Mean Queue Messages"));
		System.out.printf("%20s %30s ", id, congestionCount);
		System.out.printf("%30.4f %30.4f ", congestionTime/(double)1000, longestCongestionPeriod/(double)1000);
		System.out.printf("%30.4f\n", messageQueueMean/(double)pollingCount);

	}

}
