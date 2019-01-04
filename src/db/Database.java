package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Platform;

import loggs.CollectorLogger;
import metrics.Metric;
import metrics.Session;

public class Database {
	private String dbUrl;
	private static Database instance;

	private Database() {
		String fileName = Platform.getInstallLocation().getURL().getPath();
		this.dbUrl = "jdbc:sqlite:" + fileName + "innometrics.db";

		try (Connection conn = DriverManager.getConnection(this.dbUrl)) {
			if (conn != null) {
				createTables();
			}
		} catch (SQLException e) {
			CollectorLogger.getLogger().severe(e.getMessage());
		}
	}
	
	public static Database getDB() {
		if (instance == null) {
			instance = new Database();
		}
		return instance;
	}
	
	private Statement getNewStatement() {
		try {
			Connection conn = DriverManager.getConnection(this.dbUrl);
			Statement stmt = conn.createStatement();
			return stmt;
		} catch (SQLException e) {
			CollectorLogger.getLogger().severe(e.getMessage());
			return null;
		}
		
	}

	private void createTables() throws SQLException {
		Statement stmt = getNewStatement();
		String sql = 
				"DROP TABLE METRIC; DROP TABLE TOKEN; CREATE TABLE METRIC " +
				"(ID INTEGER PRIMARY KEY," +
				" name           TEXT    NOT NULL, " + 
				" start_date     varchar(50)     NOT NULL, " + 
				" end_date       varchar(50)	NOT NULL, " + 
				" ip_addr		 varchar(50), "
				+ " activity_type		 varchar(50) NOT NULL, "
				+ " value		 TEXT, "
				+ "mac_addr 	 varchar(50));"
				+ "CREATE TABLE TOKEN (ID INTEGER PRIMARY KEY, "
				+ "value varchar(100) NOT NULL, "
				+ "addr varchar(100) NOT NULL);";
		try {
			stmt.executeUpdate(sql);
		} catch (SQLException e){
			System.out.println(e);
			// Tables were already created
		}
	}

	public void insertNewMetric(Metric metric) {
		Statement stmt = getNewStatement();
		String sql = String.format("INSERT INTO METRIC "
				+ "(name, start_date, end_date, ip_addr, mac_addr,"
				+ "activity_type, value) " +
				"VALUES ('%s', '%s', '%s',"
				+ " '%s', '%s', '%s', '%s');", metric.tabName,
				metric.startDate.toString(), metric.endDate.toString(), metric.session.ipAddr,
				metric.session.macAddr, metric.activity_type, metric.value);
		System.out.println(sql);
		try {
			stmt.executeUpdate(sql);
			stmt.close();
		} catch (SQLException e) {
			CollectorLogger.getLogger().severe(e.getMessage());
		}
		
	}

	public List<Metric> getMetrics() {
		ArrayList<Metric> metrics = new ArrayList<Metric>();
		try {
			Statement stmt = getNewStatement();
			ResultSet rs = stmt.executeQuery( "SELECT * FROM METRIC;" );
			
			while (rs.next() ) {
				Integer id = rs.getInt("id");
				String name = rs.getString("name");
				String startDate  = rs.getString("start_date");
				String endDate  = rs.getString("end_date");
				String ipAddr = rs.getString("ip_addr");
				String macAddr = rs.getString("mac_addr");
				String activityType = rs.getString("activity_type");
				String value = rs.getString("value");
				
				metrics.add(new Metric(id, name, Instant.parse(startDate),
						Instant.parse(endDate), activityType, value,
						new Session(ipAddr, macAddr)));
			}
			rs.close();
			stmt.close();
		} catch (SQLException e) {
			CollectorLogger.getLogger().severe(e.getMessage());
		}
		
		return metrics;	
	}
	
	public void deleteMetrics(List<Metric> metrics) {
		if (metrics.isEmpty()) {
			return;
		}
		
		List<Integer> ids = new ArrayList<Integer>();
		for (Metric metric: metrics) {
			ids.add(metric.id);
		}
		String sql = String.format("DELETE FROM METRIC WHERE id in %s;",
				ids.toString().replace('[', '(').replace(']', ')'));

		Statement stmt = getNewStatement();
		try {
			stmt.executeUpdate(sql);
			stmt.close();
		} catch (SQLException e) {
			CollectorLogger.getLogger().severe(e.getMessage());
		}
	}
	
	public void saveToken(String token, String addr) {
		String sql = String.format("DELETE FROM TOKEN;"
				+ "INSERT INTO TOKEN (value, addr) VALUES('%s', '%s');",
				token, addr);
		System.out.println(sql);
		Statement stmt = getNewStatement();
		try {
			stmt.executeUpdate(sql);
			stmt.close();
		} catch (SQLException e) {
			CollectorLogger.getLogger().severe(e.getMessage());
		}
	}
	
	public String getToken(String addr) {
		String sql = String.format("SELECT VALUE FROM TOKEN "
				+ "WHERE addr='%s' ORDER"
				+ " BY ID DESC LIMIT 1;", addr);

		Statement stmt = getNewStatement();
		String token = null;
		try {
			ResultSet rs = stmt.executeQuery(sql);
			if (rs.next()) {
				token = rs.getString("value");
			}
			
			stmt.close();
		} catch (SQLException e) {
			CollectorLogger.getLogger().severe(e.getMessage());
		}
		return token;
	}
	
	public String getAddr() {
		String sql = String.format("SELECT ADDR FROM TOKEN "
				+ "ORDER"
				+ " BY ID DESC LIMIT 1;");

		Statement stmt = getNewStatement();
		String addr = null;
		try {
			ResultSet rs = stmt.executeQuery(sql);
			if (rs.next()) {
				addr = rs.getString("addr");
			}
			
			stmt.close();
		} catch (SQLException e) {
			CollectorLogger.getLogger().severe(e.getMessage());
		}
		return addr;
	}
}
