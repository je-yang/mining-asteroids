package cec2018;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import java.util.Collections;
import org.json.JSONObject;
import org.json.JSONArray;


import javax.net.ssl.HttpsURLConnection;

public class Starter {
	
	public static void main(String[] args) throws Exception{	
		
		// Start up
		sendGet("startup");

		// Parameters
		String parameters = sendGet("parameters");
		JSONObject json = new JSONObject(parameters).getJSONObject("parameters");		
		
		Integer lifetime = json.getInt("lifetime");
		Integer ms_per_week = json.getInt("ms_per_week");
		
		Integer HubBuildTime = json.getJSONObject("costs").getJSONObject("hub").getInt("weeks") + 2; 
		Integer DeployBuildTime = json.getJSONObject("costs").getJSONObject("deploy").getInt("weeks") + 2; 
		Integer ShipBuildTime = json.getJSONObject("costs").getJSONObject("ship").getInt("weeks") + 2; 
		Integer MoveBuildTime = json.getJSONObject("costs").getJSONObject("move").getInt("weeks") + 2; 
		
		long HubBuildCost = json.getJSONObject("costs").getJSONObject("hub").getLong("rate"); 
		long DeployBuildCost = json.getJSONObject("costs").getJSONObject("deploy").getLong("rate"); 
		long ShipBuildCost = json.getJSONObject("costs").getJSONObject("ship").getLong("rate"); 
		long MoveBuildCost = json.getJSONObject("costs").getJSONObject("move").getLong("rate"); 
		
		int BuildHubCounter = HubBuildTime + 1;
		int DeployHubCounter = 0;
		int ShipHubCounter = 0;		
		int MoveHubCounter = 0;
		
		boolean BuildNow = true;
		boolean DeployNow = false;
		boolean ShipNow = false;
		boolean MoveNow = false;

		// Report
		String status_report = sendGet("status_report");
		JSONObject status = new JSONObject(status_report).getJSONObject("status_report");
		System.out.println(status);
		long initial_credit = status.getJSONObject("team").getLong("balance");
		System.out.println(initial_credit);
		
		sendGet("build_hubs", "&hubs=[hub]");	
		BuildNow = false;		
		BuildHubCounter = HubBuildTime + 1;
		DeployNow = true;
		
		//Trial
		String prospects = sendGet("prospect_report");
		JSONObject json_prospects = new JSONObject(prospects).getJSONObject("prospect_report");	
		JSONArray prospectsReport = json_prospects.getJSONArray("report");
	    HashMap<Integer,Integer> prospect = new HashMap<Integer, Integer>();
	      
		
		for (int i = 0; i < prospectsReport.length(); i++) {
			JSONArray currentSector_json = prospectsReport.getJSONArray(i);
			String currentSector_string = currentSector_json.toString();
			prospect.put((int)currentSector_json.get(0), (int)currentSector_json.get(2));
		}
		
//		lifetime = 0;
		
		System.out.println(lifetime);
		
		while(lifetime > 0){
		
			if(BuildNow && !DeployNow && !ShipNow && !MoveNow && (ShipHubCounter == 0)){
				sendGet("build_hubs", "&hubs=[hub]");
				BuildHubCounter = HubBuildTime + 1;
				System.out.println("Building");
				BuildNow = false;				
				DeployNow = true;
				sendGet("status_report");
				System.out.println("Weeks Left : " + lifetime);
			}
			
			if(!BuildNow && DeployNow && !ShipNow && !MoveNow && (BuildHubCounter == 0) ){
				
			   int sector = 0;
		       int maxValueInMap=(int) (Collections.max(prospect.values()));  // This will return max value in the Hashmap
		        for (Entry<Integer, Integer> entry : prospect.entrySet()) {  // Itrate through hashmap
		            if (entry.getValue()== maxValueInMap) {
		                System.out.println("Sector: " + entry.getKey());     // Print the key with max value
				        sector = entry.getKey();				        
		            }
		        }
		        prospect.remove(sector);
				sendGet("deploy_hubs", "&hubs=[hub]&sector_ids=["+sector+"]");
				DeployHubCounter = DeployBuildTime + 1;
				System.out.println("Deploying");
				DeployNow = false;
				ShipNow = true;
				sendGet("status_report");
				System.out.println("Weeks Left : " + lifetime);
			}

			if(!BuildNow && !DeployNow && !ShipNow && MoveNow){
				sendGet("build_hubs", "&hubs=[hub]");	
				MoveHubCounter = MoveBuildTime + 1;
			}

			if(!BuildNow && !DeployNow && ShipNow && !MoveNow && (DeployHubCounter == 0)){
				sendGet("ship_ore", "&hubs=[hub]");
				ShipHubCounter = ShipBuildTime + 1;
				System.out.println("Shipping");
				ShipNow = false;
				BuildNow = true;
				sendGet("status_report");
				System.out.println("Weeks Left : " + lifetime);
			}
			
			if(BuildHubCounter > 0){
				TimeUnit.MILLISECONDS.sleep(ms_per_week);				
				lifetime--;		
				BuildHubCounter--;
				continue;
			}
						
			if(DeployHubCounter > 0){
				TimeUnit.MILLISECONDS.sleep(ms_per_week);				
				lifetime--;		
				DeployHubCounter--;
				continue;				
			}

			if(ShipHubCounter > 0){
				TimeUnit.MILLISECONDS.sleep(ms_per_week);				
				lifetime--;		
				ShipHubCounter--;
				continue;								
			}

			if(MoveHubCounter > 0){
				TimeUnit.MILLISECONDS.sleep(ms_per_week);				
				lifetime--;		
				MoveHubCounter--;
				continue;								
			}
		}			
	}
	
	public static long updateCredit() throws Exception {
		
		long sum;
		String status_report = sendGet("status_report");
		JSONObject status = new JSONObject(status_report);		
		return status.getJSONObject("status_report").getJSONObject("team").getLong("balance");
	}
	
	// HTTP GET request
	public static String sendGet(String api_call) throws Exception {

		String url = " http://www.cec-2018.ca/mcp/" + api_call + "?token=184d00ac0bfb7c92a8fc37dcac5e26b8";
						
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		// optional default is GET
		con.setRequestMethod("GET");

		BufferedReader in = new BufferedReader(
		        new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();

		if(api_call == "status_report"){
			System.out.println(response.toString());
		}

		return response.toString();
	}
	
	// HTTP GET request
	public static String sendGet(String api_call, String parameters) throws Exception {

		String url = " http://www.cec-2018.ca/mcp/" + api_call + "?token=184d00ac0bfb7c92a8fc37dcac5e26b8" + parameters;
						
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		// optional default is GET
		con.setRequestMethod("GET");

		BufferedReader in = new BufferedReader(
		        new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();

		System.out.println(response.toString());
		//print result
		return response.toString();
	}

}