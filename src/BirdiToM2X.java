import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import com.att.m2x.client.M2XClient;
import com.att.m2x.client.M2XDevice;
import com.att.m2x.client.M2XStream;

@SuppressWarnings("deprecation")
public class BirdiToM2X {
	private static final String BIRDI_URL = "http://api-m2x.att.com/v2/devices/e0664612675c3f92e43ab42c1c433979/streams";
	private static final String POLLEN_URL = BIRDI_URL + "/pollen";
	private static final String USER_AGENT = "Mozilla/5.0";
	private static final String USER_AGENT_HEADER = "User-Agent";
	private static final String M2X_HEADER = "H-M2X-KEY";
	private static final String API_KEY = "b4eab554dc00784b372646e93c7bd0b4";
	private static M2XClient client = new M2XClient(
			"6192f89a79f3c9f2d006b78eda4df0cc");
	private static M2XDevice device = client
			.device("15af65895ed19be59193b8b303b76a8f");

	private static final String[] STREAM_NAMES = new String[] { "co-indoor",
			"co2-indoor", "pollen", "smoke", "temperature-indoor",
			"air-quality-index", "humidity-indoor", "particulates-indoor" };

	private static final String TYPE = "type";
	private static final String UNIT = "unit";
	private static final String NAME = "name";
	private static final String VALUE = "value";
	private static final String STREAMS = "streams";
	private static HashSet<String> NAME_SET = new HashSet<String>();

	public static void main(String[] args) throws Exception {
		init();
		postToM2X();
	}

	private static void init() {
		for (int i = 0; i < STREAM_NAMES.length; i++) {
			NAME_SET.add(STREAM_NAMES[i]);
		}
	}

	private static void postToM2X() throws Exception {
		int count = 0;
		while (count < 1) {
			String body = getBirdiData();
			JSONObject json = new JSONObject(body);
			JSONArray streams = json.getJSONArray(STREAMS);
			for (int i = 0; i < streams.length(); i++) {
				JSONObject jobj = streams.getJSONObject(i);
				String name = jobj.getString(NAME);
				if (NAME_SET.contains(name)) {
					final Double pd = jobj.getDouble(VALUE);
					M2XStream stream = device.stream(name);
					System.out.println("Will post to stream: " + name + " with value: " + pd);
					stream.updateValue(M2XClient
							.jsonSerialize(new HashMap<String, Object>() {
								{
									put("value", pd);
								}
							}));
				}
			}
			Thread.sleep(10000);
			count++;
		}
	}

	private static double getValue(String value) {
		Pattern pattern = Pattern.compile("\\w+");
		Matcher matcher = null;

		matcher = pattern.matcher(value);
		if (matcher.find()) {
			return 0.0;
		} else
			return Double.parseDouble(value);
	}
	
	private static void createStream() throws Exception {
		String body = getBirdiData();
		JSONObject json = new JSONObject(body);
		JSONArray streams = json.getJSONArray(STREAMS);

		for (int i = 0; i < streams.length(); i++) {
			JSONObject jobj = streams.getJSONObject(i);
			String name = jobj.getString(NAME);
			if (NAME_SET.contains(name)) {
				StringBuilder sb = new StringBuilder();
				sb.append("{\"");
				sb.append(TYPE);
				sb.append("\":\"");
				sb.append(jobj.get(TYPE));
				sb.append("\",\"");
				sb.append(UNIT);
				sb.append("\":");
				sb.append(jobj.get(UNIT));
				sb.append("}");
				// System.out.println(sb.toString());
				M2XStream stream = device.stream(name);
				stream.createOrUpdate(sb.toString());
			}
		}
		// M2XStream stream = device.stream("pollen");
		// stream.createOrUpdate("{\"type\":\"numeric\",\"unit\":{\"label\":\"Level\",\"symbol\":\"pt\"}}");

	}

	private static String getBirdiData() {
		String retValue = null;

		try {
			HttpClient client = new DefaultHttpClient();
			HttpGet request = new HttpGet(BIRDI_URL);

			// add request header
			request.addHeader(USER_AGENT_HEADER, USER_AGENT);
			request.addHeader(M2X_HEADER, API_KEY);

			HttpResponse response = client.execute(request);

			System.out
					.println("\nSending 'GET' request to URL : " + POLLEN_URL);
			System.out.println("Response Code : "
					+ response.getStatusLine().getStatusCode());

			BufferedReader rd = new BufferedReader(new InputStreamReader(
					response.getEntity().getContent()));

			StringBuilder sb = new StringBuilder();
			String line = "";
			while ((line = rd.readLine()) != null) {
				sb.append(line);
			}

			retValue = sb.toString();
		} catch (Exception ex) {
			for (StackTraceElement elem : ex.getStackTrace()) {
				System.out.println(elem.toString());
			}
			System.out.println("found exception: " + ex.getMessage());

		}
		return retValue;
	}

	private static String findPollenData() {
		String retValue = null;

		try {
			HttpClient client = new DefaultHttpClient();
			HttpGet request = new HttpGet(POLLEN_URL);

			// add request header
			request.addHeader(USER_AGENT_HEADER, USER_AGENT);
			request.addHeader(M2X_HEADER, API_KEY);

			HttpResponse response = client.execute(request);

			System.out
					.println("\nSending 'GET' request to URL : " + POLLEN_URL);
			System.out.println("Response Code : "
					+ response.getStatusLine().getStatusCode());

			BufferedReader rd = new BufferedReader(new InputStreamReader(
					response.getEntity().getContent()));

			String line = "";
			while ((line = rd.readLine()) != null) {
				int ind = line.indexOf("\"value\"");
				int ind2 = line.indexOf(',', ind);
				retValue = line.substring(ind + 8, ind2);
				System.out.println(retValue);
			}

		} catch (Exception ex) {
			for (StackTraceElement elem : ex.getStackTrace()) {
				System.out.println(elem.toString());
			}
			System.out.println("found exception: " + ex.getMessage());

		}
		return retValue;
	}
}
