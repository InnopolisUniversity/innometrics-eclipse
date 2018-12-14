package metrics;

import utils.GetNetworkAddress;

public class Session {
	public String ipAddr;
	public String macAddr;
	private static Session instance;
	
	private Session() {
		this.ipAddr = GetNetworkAddress.GetAddress("ip");
		this.macAddr = GetNetworkAddress.GetAddress("mac");
	}
	
	public Session(String ipAddr, String macAddr) {
		this.ipAddr = ipAddr;
		this.macAddr = macAddr;
	}
	
	public static Session getCurrentSession() {
		if (instance == null) {
			instance = new Session();
		}
		return instance;
	}
}
