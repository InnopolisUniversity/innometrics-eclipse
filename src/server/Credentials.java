package server;

public class Credentials {
	public String addr = null;
	public String email = null;
	public String pass = null;
	
	public Credentials() {};
	
	public Credentials(String addr, String email, String pass) {
		this.addr = addr;
		this.email = email;
		this.pass = pass;
	}
}
