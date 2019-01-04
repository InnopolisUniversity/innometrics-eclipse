package metrics;

import java.time.Instant;

public class Metric {
	public int id;
	public String activity_type = "eclipse_tab_name";
	public String tabName;
	public Instant startDate;
	public Instant endDate;
	public Session session;
	public String value = "";
	
	public Metric(String fileName) {
		this.tabName = fileName;
		this.startDate = Instant.now();
		this.session = Session.getCurrentSession();
	}
	
	public Metric(int id, String fileName, Instant startDate, Instant endDate,
			String activity_type, String value,
			Session session) {
		this.id = id;
		this.tabName = fileName;
		this.startDate = startDate;
		this.endDate = endDate;
		this.session = session;
		this.activity_type = activity_type;
		this.value = value;
	}
	
	public void finish() {
		this.endDate = Instant.now();
	}
	
	public void print() {
		System.out.println(this.tabName);
		System.out.println(this.startDate);
		System.out.println(this.endDate);
		System.out.println(this.session.ipAddr);
		System.out.println(this.activity_type);
		System.out.println(this.value);
	}
}
