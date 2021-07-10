package implementare;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;

import org.json.simple.parser.ParseException;

public class ApiHandler {
	
	
	private static boolean Prelim = false;
	ApiParser parser;
	
	//legacy - uses api_urls.txt as input - only for testing
	public void handleApiFromFileLegacy() throws IOException, URISyntaxException, ParseException {
		File fileIn = new File(getClass().getClassLoader().getResource("api_urls.txt").toURI());
		BufferedReader br = new BufferedReader(new FileReader(fileIn));
		String url;
		FileWriter fileOut = new FileWriter("./results.txt",false);
		
		while ((url = br.readLine()) != null) {
			Client client = ClientBuilder.newClient();
			WebTarget webTarget = client.target(url);
			Builder invocationBuilder = webTarget.request(MediaType.APPLICATION_JSON);
			Response res = invocationBuilder.get();
			String resultString = res.readEntity(String.class);
			if(Prelim) {
				List<String> result = preliminareApiTest(resultString);
				fileOut.append(url + result.stream().collect(Collectors.joining("\t", "\t", "\n")));
			}
			else {
				handleInitilTests(resultString);
			}
		}
		
		fileOut.flush();
		fileOut.close();
	}
	
	public void handleApiFromUrl(String url) throws ParseException {
		Client client = ClientBuilder.newClient();
		WebTarget webTarget = client.target(url);
		Builder invocationBuilder = webTarget.request(MediaType.APPLICATION_JSON);
		Response res = invocationBuilder.get();
		String resultString = res.readEntity(String.class);
		handleInitilTests(resultString);
	}
	
	public void handleApiFromFile(Path path) throws IOException, ParseException {
		String resultString = Files.lines(path).collect(Collectors.joining("\n"));
		handleInitilTests(resultString);
	}
	

	private void handleInitilTests(String apiDefinition) throws ParseException {

		parser = new ApiParser(apiDefinition);
		List<Endpoint> endpoints = parser.parsePaths();
		if(Config.DEBUG_INFO_ENABELED || Config.LOG_ENABELED) {
			Logger.log("Parsed endpoints");
			for(Endpoint endpoint: endpoints) {
				StringBuilder sb = new StringBuilder();
				sb.append("URL: ").append(endpoint.baseUrl).append(endpoint.path).append("\n")
				.append("RequestType: ").append(endpoint.type).append("\n")
				.append("Parameters:\n");
				for(Entry<String, Parameter> entry:endpoint.parameters.entrySet()) {
					sb.append(entry.getKey()).append(":\n");
					Parameter P = entry.getValue();
					sb.append("\ttype: ").append(P.type).append("\n")
					.append("\tlocation: ").append(P.location).append("\n")
					.append("\trequired: ").append(P.required).append("\n")
					.append("\tdescription: ").append(P.description).append("\n")
					.append("\tmedia-type: ").append(P.mediaType).append("\n")
					.append("\texample: ").append(P.example).append("\n")
					.append("\titemType(exclusive for arrays): ").append(P.arrayItmestype ).append("\n")
					.append("\tenum values (if is enum): ").append(P.enumOptions.stream().map(e->String.valueOf(e)).collect(Collectors.joining(","))).append("\n");
				}
				if (endpoint.requestBodyWithParams!=null) {
					sb.append("Has request body:\n");
					for(Entry<String, Parameter> entry:((Map<String, Parameter>)endpoint.requestBodyWithParams).entrySet()) {
						sb.append(entry.getKey()).append(":\n");
						Parameter P = entry.getValue();
						sb.append("\ttype: ").append(P.type).append("\n")
						.append("\tlocation: ").append(P.location).append("\n")
						.append("\trequired: ").append(P.required).append("\n")
						.append("\tdescription: ").append(P.description).append("\n")
						.append("\tmedia-type: ").append(P.mediaType).append("\n")
						.append("\texample: ").append(P.example).append("\n")
						.append("\titemType(exclusive for arrays): ").append(P.arrayItmestype).append("\n")
						.append("\tenum values (if is enum): ").append(P.enumOptions.stream().map(e->String.valueOf(e)).collect(Collectors.joining(","))).append("\n");
					}
				}
				Logger.log(sb.toString());
			}
		}
		generateTests(endpoints);
	}

	private void generateTests(List<Endpoint> endpoints) {
		List<TestCase> testCases = new ArrayList<>();
		for (Endpoint e : endpoints) {
			testCases.add(new TestCase(e));
		}
		for(TestCase test: testCases) {
			Logger.log("Running a testcase...");
			test.run();
			
		}
		Statistics.getInstance().generateStatistics();
	}

	private List<String> preliminareApiTest(String apiDefinition) throws ParseException {
		List<String> result = new ArrayList<>();
		ApiParser parser;
		try {
			parser = new ApiParser(apiDefinition);
		}catch (Exception e) {
			result.add("Error at parsing api definition");
			return result;
		}
		if(ApiParser.hasSecuritySchema(apiDefinition)) {
			result.add("Has security");
			return result;
		}
		List<Endpoint> endpoints = parser.parsePaths();
		int complexity=3;
		String possibleGets="";
		boolean multipleServers = false;
		for(Endpoint e: endpoints) {
			int c = e.getComplexity();
			if(c==0) {
				complexity=c;
				if(tryBasicRequest(e)) {
					result.add("Succesful GET request for: "+e.getURL());
					if(e.hasMultipleServers) {
						result.add("Has multiple servers");
					}
					return result;
				} else {
					possibleGets += e.getURL();
				}
			}
			multipleServers=e.hasMultipleServers;
			complexity=Math.min(complexity, c);
		}
		
		if(complexity==3) {
			result.add("Error at rating complexity");
		}
		if(complexity==2) {
			result.add("Only more complex type of requests then GET");
		}
		if(complexity==1) {
			result.add("Has GET request but needs params");
		}
		if(complexity==0) {
			result.add("Has simple GET but could not auto-validate");
			if(possibleGets.length()>7) {
				result.add("Possible GET reqests: " + possibleGets);
			}
		}
		if(multipleServers) {
			result.add("Has multiple servers");
		}
		return result;
	}

	private boolean tryBasicRequest(Endpoint e) {
		try {
		Client client = ClientBuilder.newClient();
		WebTarget webTarget = client.target(e.baseUrl).path(e.path);
		Builder invocationBuilder = webTarget.request(MediaType.WILDCARD);
		Response res = invocationBuilder.get();
		return res.getStatusInfo().getFamily() == Family.SUCCESSFUL;
		}catch (Exception ex) {
			return false;
		}
	}



	
	
}
