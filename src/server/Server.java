package server;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import org.apache.http.*;
import org.apache.http.HttpResponse;
import org.apache.http.client.*;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.*;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import db.Database;
import loggs.CollectorLogger;
import metrics.Metric;

public class Server {
	private String addr;
	private String token;
	public Boolean askedForCredentials = false;
	private static Server instance = null;

	private Server() {
		this.addr = Database.getDB().getAddr();
		this.token = Database.getDB().getToken(this.addr);
		if (addr == null || token == null) {
			getCredentials("Please, login to the logger");
		}

	}

	public static Server getInstance() {
		if (instance == null) {
			instance = new Server();
		}
		return instance;
	}

	public Boolean getCredentials(String messageInfo) {
		if (askedForCredentials) {
			return false;
		}
		askedForCredentials = true;
		JFrame frame = new JFrame("Eclipse Logger");
		frame.setSize(500, 500);

		JTextField addr = new JTextField();
		JTextField username = new JTextField();
		JTextField password = new JPasswordField();

		while (true) {
			Object[] message = { messageInfo, "Server address", addr, "Email:", username, "Password:", password };
			int option = JOptionPane.showConfirmDialog(frame, message, null, JOptionPane.OK_CANCEL_OPTION);

			if (option == JOptionPane.OK_OPTION) {
				this.addr = addr.getText();
				Credentials credentials = new Credentials(addr.getText(), username.getText(), password.getText());
				String token = authenticate(credentials);

				if (token == null || token.isEmpty()) {
					messageInfo = "Credentials are wrong. Please, try again";
				} else {
					return true;
				}
			} else {
				return false;
			}
		}
	}

	public String authenticate(Credentials credentials) {
		String addr = credentials.addr;
		String email = credentials.email;
		String password = credentials.pass;

		if (addr == null || addr.isEmpty() || email == null || email.isEmpty() || password == null
				|| password.isEmpty()) {
			return null;
		}

		HttpClient httpclient = HttpClients.createDefault();
		HttpPost httppost = new HttpPost(addr + "/login");

		ArrayList<NameValuePair> params = new ArrayList<NameValuePair>(2);
		params.add(new BasicNameValuePair("email", email));
		params.add(new BasicNameValuePair("password", password));
		try {
			httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			CollectorLogger.getLogger().warning(e.getMessage());
		}

		HttpResponse response;
		String token = null;
		try {
			response = httpclient.execute(httppost);
			HttpEntity entity = response.getEntity();

			if (entity != null) {
				String response_string = EntityUtils.toString(entity);
				JSONObject result = new JSONObject(response_string);

				token = result.getString("token");
				this.token = token;
				Database.getDB().saveToken(token, this.addr);
			}
		} catch (ClientProtocolException e) {
			CollectorLogger.getLogger().warning(e.getMessage());
		} catch (IOException e) {
			CollectorLogger.getLogger().severe(e.getMessage());
		} catch (JSONException e) {
			CollectorLogger.getLogger().severe(e.getMessage());
		}

		return token;
	}

	public boolean sendMetrics(List<Metric> metrics) {
		if (token == null) {
			Boolean authenticated = getCredentials("Please, login to the logger");
			if (!authenticated) {
				return false;
			}
		}

		try {
			HttpClient httpclient = HttpClients.createDefault();
			HttpPost httppost = new HttpPost(this.addr + "/activity");

			JSONArray activities = new JSONArray();
			for (Metric metric : metrics) {
				JSONObject json_metric = new JSONObject();
				json_metric.put("executable_name", metric.tabName);
				json_metric.put("start_time", metric.startDate);
				json_metric.put("end_time", metric.endDate);
				json_metric.put("ip_address", metric.session.ipAddr);
				json_metric.put("mac_address", metric.session.macAddr);
				json_metric.put("activity_type", metric.activity_type);
				json_metric.put("value", metric.value);
				activities.put(json_metric);
			}
			JSONObject data = new JSONObject();
			data.put("activities", activities);

			StringWriter out = new StringWriter();
			data.write(out);

			String jsonText = out.toString();

			ArrayList<NameValuePair> params = new ArrayList<NameValuePair>(2);
			params.add(new BasicNameValuePair("activity", jsonText));
			httppost.addHeader("Authorization", String.format("Token %s", this.token));
			try {
				httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				CollectorLogger.getLogger().severe(e.getMessage());
			}

			HttpResponse response;

			response = httpclient.execute(httppost);
			HttpEntity entity = response.getEntity();
			Integer responseCode = response.getStatusLine().getStatusCode();

			if (responseCode == 201) {
				return true;
			} else if (responseCode == 401) {
				Boolean authenticated = getCredentials("Please, relogin to the logger");
				if (authenticated) {
					return sendMetrics(metrics);
				}
				return false;
			}
		} catch (ConnectException e) {
			this.token = null;
			sendMetrics(metrics);
		} catch (ClientProtocolException e) {
			CollectorLogger.getLogger().warning(e.getMessage());
		} catch (IOException e) {
			CollectorLogger.getLogger().severe(e.getMessage());
		} catch (JSONException e) {
			CollectorLogger.getLogger().severe(e.getMessage());
		}

		return false;
	}
}
