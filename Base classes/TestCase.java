package implementare;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import javax.net.ssl.SSLContext;
import javax.swing.JOptionPane;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;

import org.glassfish.jersey.SslConfigurator;
import org.json.simple.JSONValue;

import implementare.Endpoint.RequestType;
import implementare.Parameter.PARAM_LOCATION;
import implementare.Parameter.PARAM_TYPE;
import implementare.Statistics.OUTCOMES;
import implementare.Statistics.STATISTICS_TYPE;

public class TestCase {

	private static final List<String> TEST_STRINGS = List.of("Just","Some","Random","String","From","Which","To","Choose","From");
	SSLContext sslContext = SslConfigurator.newInstance()
            .securityProtocol("TLSv1.2")
            .createSSLContext();
	Client defaultClient = ClientBuilder.newClient();
	Endpoint endpoint;
	Set<Map<String, Map.Entry<Object, Boolean>>> paramValueSets = new HashSet<>();
	private boolean expectedValue;
	private boolean missingExample;
	Map<String, List<Map.Entry<Object, Boolean>>> individualValues = new LinkedHashMap<>();
	public TestCase(Endpoint e) {
		this.endpoint = e;
		generateParamCombinations();
	}

	private void generateParamCombinations() {
		
		missingExample = false;
		for( Entry<String, Parameter> entry : this.endpoint.parameters.entrySet()) {
			individualValues.put(entry.getKey(), resolveParameterValues(entry.getValue()));
		}
		if(endpoint.requestBodyWithParams!= null) {
			addParamsFromRequestBody(individualValues, endpoint.requestBodyWithParams);
		}
		if(Config.DEBUG_INFO_ENABELED || Config.LOG_ENABELED) {
			StringBuilder sb = new StringBuilder("IndividualValues for "+endpoint.path+":\n");
			for(Entry<String, List<Entry<Object, Boolean>>> entry: individualValues.entrySet()) {
				sb.append(entry.getKey()).append(": ").append(entry.getValue().stream().map(e->""+e.getKey()+"("+e.getValue()+")").collect(Collectors.joining(",","","\n")));
			}
			Logger.log(sb.toString());
		}
		
		Statistics.getInstance().addStatistic(endpoint.path, endpoint.type.name(), STATISTICS_TYPE.operationCoverage, missingExample ? OUTCOMES.missingExample : null);
		Statistics.getInstance().addBaseParams(individualValues.entrySet().stream().filter(e -> !e.getValue().isEmpty()).map(e -> e.getKey()).collect(Collectors.toSet()));
		
		paramValueSets = new HashSet<>();
		individualValues = individualValues.entrySet().stream().filter(e -> !e.getValue().isEmpty()).collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
		long combinations = individualValues.entrySet().stream().map(e -> Long.valueOf(e.getValue().size())).filter(e -> e > 1).reduce(1L, (a, b) -> a * b);
		if (combinations < 17) {
			recursivlyCombine(individualValues, 0, new HashMap<String, Map.Entry<Object, Boolean>>());
		}
		else {
			combineSimple(individualValues);
		}
		
		if(Config.DEBUG_INFO_ENABELED || Config.LOG_ENABELED) {
			StringBuilder sb = new StringBuilder("Combined Test Values for "+endpoint.path+":\n");
			int i=0;
			for( Map<String, Map.Entry<Object, Boolean>> set : paramValueSets) {
				sb.append("Set ").append(++i).append("\n");
				boolean testValue = true;
				for( Entry<String, Entry<Object, Boolean>> entry : set.entrySet()) {
					sb.append(entry.getKey()).append("=\"").append(entry.getValue().getKey()).append("\"(").append(entry.getValue().getValue()).append("), ");
					testValue &= entry.getValue().getValue();
				}
				sb.append("\t testValue=").append(testValue).append("\n");
			}
			Logger.log(sb.toString());
		}
		
	}


	private void addParamsFromRequestBody(Map<String, List<Entry<Object, Boolean>>> individualValues, Object value) {
			if(value!=null) {
				for( Entry<String, Object> entry : ((Map<String,Object>)value).entrySet()) {
					if(((Parameter)entry.getValue()).type == PARAM_TYPE.Object ) {
						addParamsFromRequestBody(individualValues, ((Parameter)entry.getValue()).childObject);
					}
					else if(((Parameter)entry.getValue()).type == PARAM_TYPE.Array && ((Parameter)entry.getValue()).arrayItmestype == PARAM_TYPE.Object) {
						addParamsFromRequestBody(individualValues, ((Parameter)entry.getValue()).arrayChildObject);
					}
					else {
						individualValues.putIfAbsent(entry.getKey(), resolveParameterValues((Parameter) entry.getValue()));
					}
				}
			}
		
	}

	private void recursivlyCombine(Map<String, List<Entry<Object, Boolean>>> individualValues, int depth,
			HashMap<String, Entry<Object, Boolean>> hashMap) {
		if(depth == individualValues.size()) {
			if(depth>0) {	
				paramValueSets.add(new HashMap<String, Map.Entry<Object,Boolean>>(hashMap));
			}
			return;
		}
		int i=0;
		for(Entry<String, List<Entry<Object, Boolean>>> entry:individualValues.entrySet()) {
			if(i==depth){
				for(Entry<Object, Boolean> value:entry.getValue()) {
					hashMap.put(entry.getKey(), value);
					recursivlyCombine(individualValues, depth+1, hashMap);
					hashMap.remove(entry.getKey());
				}
			}
			i++;
		}
		
	}


	private void combineSimple(Map<String, List<Entry<Object, Boolean>>> individualValues) {
		List<String> keys = individualValues.keySet().stream().collect(Collectors.toList());
		for(String key: keys) {
			List<Entry<Object, Boolean>> list = individualValues.get(key);
			for(Entry<Object, Boolean> listentry: list) {
				Map<String, Map.Entry<Object,Boolean>> map = new HashMap<String, Map.Entry<Object,Boolean>>();
				for(Entry<String, List<Entry<Object, Boolean>>> entry: individualValues.entrySet()) {
					if(entry.getKey() == key) {
						map.put(key, listentry);
					}
					else {
						map.put(entry.getKey(), entry.getValue().get(0));
					}
				}
				paramValueSets.add(map);
			}
		}
	}
	
	private List<Map.Entry<Object, Boolean>> resolveParameterValues(Parameter parameter) {
		List<Map.Entry<Object, Boolean>> possibleValues = new ArrayList<>();
		if(parameter.required == false && Config.IGNORE_NON_REQUIRED_PARAMS) {
			return possibleValues;
		}
		if(parameter.example == null && Config.GENERATE_PARTIAL_TESTS) {
			Object value = ParameterExampleCache.get(parameter.name);
			if(value!=null) {
				parameter.example = value;
			}
			else {
				String input = JOptionPane.showInputDialog(GUI.mainWindow, "Need exaple for parameter \""+parameter.name+"\" (type: "+parameter.type+(parameter.type==PARAM_TYPE.Array? ("/" + parameter.arrayItmestype) : "") +" description: "+parameter.description + (parameter.min!=null? (" ;min=" + parameter.min) : "") + (parameter.max!=null? (" ;max=" + parameter.max) : "") + ")");
				try {
					if(Config.DEBUG_INFO_ENABELED || Config.LOG_ENABELED) {
						Logger.log("Got input value for \""+parameter.name+"\": " + input);
					}
					if(!input.isEmpty()) {
						switch (parameter.type) {
						case String:
							parameter.example = input;
							break;
						case Integer:
						case Double:
						case Float:
						case Number: 
						case Long:
							parameter.example = convertNumber(input, parameter.type);
							break;
						case Boolean:
							parameter.example = Boolean.valueOf(input);
							break;
						}	
					}
					ParameterExampleCache.put(parameter.name, parameter.example);
				} catch (Exception e) {
					e.printStackTrace();
				} 
			}
		}
		if(parameter.example == null) {
			missingExample = true;
		}
		Random R = new Random();
		switch(parameter.type) {
		case String:
			possibleValues.add(new AbstractMap.SimpleEntry<Object, Boolean>(parameter.example!= null ? parameter.example :
						TEST_STRINGS.get(R.nextInt(TEST_STRINGS.size())),true));
			if (parameter.location == PARAM_LOCATION.requestBody) {
					possibleValues.add(new AbstractMap.SimpleEntry<Object, Boolean>(7, false ));
			}
			break;
		case Enum:
			for(Object o : parameter.enumOptions) {
				possibleValues.add(new AbstractMap.SimpleEntry<Object, Boolean>(o, true));
			}
			possibleValues.add(new AbstractMap.SimpleEntry<Object, Boolean>("random words",false));
			break;
		case Integer:
		case Number:
		case Float:
		case Double:
		case Long:
			if(parameter.example != null) {
				possibleValues.add(new AbstractMap.SimpleEntry<Object, Boolean>(parameter.example, true));
			}
			else {
				possibleValues.add(new AbstractMap.SimpleEntry<Object, Boolean>(7, isSevenInRange(parameter)));
			}
			if(parameter.location!=PARAM_LOCATION.path && parameter.min!=null && parameter.max!=null) {
				addDomainTestingValues2(possibleValues, parameter.type, convertNumber(parameter.min, parameter.type), convertNumber(parameter.max, parameter.type));
			}
			possibleValues.add(new AbstractMap.SimpleEntry<Object, Boolean>("Random String", false));
			break;
		case Boolean:
			possibleValues.add(new AbstractMap.SimpleEntry<Object, Boolean>(true,true));
			possibleValues.add(new AbstractMap.SimpleEntry<Object, Boolean>(false,true));
			possibleValues.add(new AbstractMap.SimpleEntry<Object, Boolean>("quantum", false));
			break;
		case Array:
			PARAM_TYPE type = parameter.arrayItmestype != null ? parameter.arrayItmestype : PARAM_TYPE.String;
			switch (type) {
			case String:
				possibleValues.add(new AbstractMap.SimpleEntry<Object, Boolean>(parameter.example!= null ? parameter.example :
					TEST_STRINGS,true));
				possibleValues.add(new AbstractMap.SimpleEntry<Object, Boolean>(List.of(7,7), false));
				break;
			case Integer:
			case Number:
			case Float:
			case Double:
				possibleValues.add(new AbstractMap.SimpleEntry<Object, Boolean>(List.of(1,2,7), true));
				possibleValues.add(new AbstractMap.SimpleEntry<Object, Boolean>(List.of("Random "," String"), false));
				break;
			}
			break;
		}
		
		
		return possibleValues;
	}

	private Boolean isSevenInRange(Parameter P) {
		if(P.min != null ) {
			switch (P.type) {
			case Integer:
				if (Integer.parseInt(P.min) > 7) {
					return false;
				}
				break;
			case Double:
				if (Double.parseDouble(P.min)> 7) {
					return false;
				}
				break;
			case Float:
			case Number:
				if (Float.parseFloat(P.min)> 7) {
					return false;
				}
				break;
			case Long:
				if (Long.parseLong(P.min)> 7) {
					return false;
				}
				break;
			}
		}
		if(P.max != null ) {
			switch (P.type) {
			case Integer:
				if (Integer.parseInt(P.max) < 7) {
					return false;
				}
				break;
			case Double:
				if (Double.parseDouble(P.max) < 7) {
					return false;
				}
				break;
			case Float:
			case Number: 
				if (Float.parseFloat(P.max) < 7) {
					return false;
				}
				break;
			case Long:
				if (Long.parseLong(P.max) < 7) {
					return false;
				}
				break;
			}
		}
		return true;
	}

	private void addDomainTestingValues2(List<Entry<Object, Boolean>> possibleValues, PARAM_TYPE type, Object min,
			Object max) {
		switch (type) {
			case Integer:
				possibleValues.add(new AbstractMap.SimpleEntry<Object, Boolean>((Integer) min - 1, false));
				possibleValues.add(new AbstractMap.SimpleEntry<Object, Boolean>((Integer) min, true));
				possibleValues.add(new AbstractMap.SimpleEntry<Object, Boolean>((Integer)min + ((Integer)max-(Integer)min)/2, true));
				possibleValues.add(new AbstractMap.SimpleEntry<Object, Boolean>((Integer) max, true));
				possibleValues.add(new AbstractMap.SimpleEntry<Object, Boolean>((Integer) max +1, false));
				break;
			case Float:
			case Number:
				possibleValues.add(new AbstractMap.SimpleEntry<Object, Boolean>((Float) min - 1, false));
				possibleValues.add(new AbstractMap.SimpleEntry<Object, Boolean>((Float) min, true));
				possibleValues.add(new AbstractMap.SimpleEntry<Object, Boolean>((Float)min + ((Float)max-(Float)min)/2, true));
				possibleValues.add(new AbstractMap.SimpleEntry<Object, Boolean>((Float) max, true));
				possibleValues.add(new AbstractMap.SimpleEntry<Object, Boolean>((Float) max +1, false));
				break;
			case Long:
				possibleValues.add(new AbstractMap.SimpleEntry<Object, Boolean>((Long) min - 1, false));
				possibleValues.add(new AbstractMap.SimpleEntry<Object, Boolean>((Long) min, true));
				possibleValues.add(new AbstractMap.SimpleEntry<Object, Boolean>((Long)min + ((Long)max-(Long)min)/2, true));
				possibleValues.add(new AbstractMap.SimpleEntry<Object, Boolean>((Long) max, true));
				possibleValues.add(new AbstractMap.SimpleEntry<Object, Boolean>((Long) max +1, false));
				break;
			case Double:
				possibleValues.add(new AbstractMap.SimpleEntry<Object, Boolean>((Double) min - 1, false));
				possibleValues.add(new AbstractMap.SimpleEntry<Object, Boolean>((Double) min, true));
				possibleValues.add(new AbstractMap.SimpleEntry<Object, Boolean>((Double)min + ((Double)max-(Double)min)/2, true));
				possibleValues.add(new AbstractMap.SimpleEntry<Object, Boolean>((Double) max, true));
				possibleValues.add(new AbstractMap.SimpleEntry<Object, Boolean>((Double) max +1, false));
				break;
			}
		
	}

	public void run() {
		Long start = System.currentTimeMillis();
		int runs=0;
		boolean finalResult = true;
		if(paramValueSets.isEmpty() && this.endpoint.type == RequestType.GET) {
			runs = 1;
			finalResult = doGetOrPostRequest(Collections.emptyMap());
		}
		if(paramValueSets.isEmpty() && (this.endpoint.type == RequestType.POST || this.endpoint.type == RequestType.PUT) && this.endpoint.requestBodyWithParams == null) {
			runs = 1;
			finalResult = doGetOrPostRequest(Collections.emptyMap());
		}
		switch(this.endpoint.type) {
		case GET:
		case POST:
		case PUT:
			for( Map<String, Entry<Object, Boolean>> map : paramValueSets) {
				finalResult &= doGetOrPostRequest(map);
				runs++;
			}
			break;
		}
		if(runs>0) {
			Statistics.getInstance().addStatistic(endpoint.path, endpoint.type.name(), STATISTICS_TYPE.operationCoverage, finalResult ? OUTCOMES.Succes : OUTCOMES.Failiure);
			if(finalResult && !paramValueSets.isEmpty()) {
				Statistics.getInstance().addSuccesfulParams(individualValues.keySet());
			}
			Statistics.getInstance().addStatistic(endpoint.path, endpoint.type.name(), STATISTICS_TYPE.responseTime, new AbstractMap.SimpleEntry<Integer, Long>(runs, System.currentTimeMillis()- start));
		}
	}

	private boolean doGetOrPostRequest(Map<String, Entry<Object, Boolean>> params) {
		String url = this.endpoint.baseUrl;
		String path = this.endpoint.path;
		expectedValue = true;
		for( Entry<String, Entry<Object, Boolean>> param: params.entrySet()) {
			Parameter P = this.endpoint.parameters.get(param.getKey());
			if(P!=null && P.location == PARAM_LOCATION.path) {
				url = replaceParamInPath(url, param.getKey(), param.getValue().getKey());
				path= replaceParamInPath(path, param.getKey(), param.getValue().getKey());
				expectedValue &= param.getValue().getValue();
			}
			
		}
		WebTarget webTarget = defaultClient.target(url).path(path);
		for( Entry<String, Entry<Object, Boolean>> param: params.entrySet()) {
			Parameter P = this.endpoint.parameters.get(param.getKey());
			if(P!=null && P.location == PARAM_LOCATION.query) {
				webTarget = webTarget.queryParam(param.getKey(), param.getValue().getKey());
				expectedValue &= param.getValue().getValue();
			}
		}
		String finalRequestBody=null;
		if(this.endpoint.type == RequestType.POST || this.endpoint.type == RequestType.PUT) {
			finalRequestBody = decodeRequestBody(params);
		}
		
		Builder invocationBuilder = webTarget.request(MediaType.WILDCARD);	
		Logger.log("Executing (" + endpoint.type + ") request for " + webTarget.getUri().toString());
		if(finalRequestBody != null) {
			Logger.log("With requestBody:\n" + finalRequestBody);
		}
		Response res = null;
		if(endpoint.type == RequestType.GET) {
			res = invocationBuilder.get();
		}
		else if(endpoint.type == RequestType.POST) {
			if(finalRequestBody != null) {
				if(finalRequestBody.isBlank() || endpoint.requestBodyType.contains("json")) {
					res = invocationBuilder.post(Entity.entity(finalRequestBody, MediaType.APPLICATION_JSON));
				}
			}
			else {
				res = invocationBuilder.post(Entity.entity("", MediaType.APPLICATION_JSON));
			}
		}
		else if(endpoint.type == RequestType.PUT) {
			if(finalRequestBody != null) {
				if(finalRequestBody.isBlank() || endpoint.requestBodyType.contains("json")) {
					res = invocationBuilder.put(Entity.entity(finalRequestBody, MediaType.APPLICATION_JSON));
				}
			}
			else {
				res = invocationBuilder.put(Entity.entity("", MediaType.APPLICATION_JSON));
			}
		}
		if(res!=null && res.getStatus() == 301 && url.contains("http://")) {	
			url = url.replace("http://", "https://");
			WebTarget webTarget2 = defaultClient.target(url).path(path);
			for( Entry<String, Entry<Object, Boolean>> param: params.entrySet()) {
				Parameter P = this.endpoint.parameters.get(param.getKey());
				if(P!=null && P.location == PARAM_LOCATION.query) {
					webTarget2 = webTarget.queryParam(param.getKey(), param.getValue().getKey());
				}
			}
			invocationBuilder = webTarget2.request(MediaType.WILDCARD);	
			if(endpoint.type == RequestType.GET) {
				res = invocationBuilder.get();
			}
			else if(endpoint.type == RequestType.POST) {
				if(finalRequestBody != null) {
					if(finalRequestBody.isBlank() || endpoint.requestBodyType.contains("json")) {
						res = invocationBuilder.post(Entity.entity(finalRequestBody, MediaType.APPLICATION_JSON));
					}
				}
				else {
					res = invocationBuilder.post(Entity.entity("", MediaType.APPLICATION_JSON));
				}
			}
			else if(endpoint.type == RequestType.PUT) {
				if(finalRequestBody != null) {
					if(finalRequestBody.isBlank() || endpoint.requestBodyType.contains("json")) {
						res = invocationBuilder.put(Entity.entity(finalRequestBody, MediaType.APPLICATION_JSON));
					}
				}
				else {
					res = invocationBuilder.put(Entity.entity("", MediaType.APPLICATION_JSON));
				}
			}
		}
		
		String resultString = null; 
		if(res!=null) {
			Statistics.getInstance().addStatistic(endpoint.path, endpoint.type.name(), STATISTICS_TYPE.statusCodeClassCoverage, new AbstractMap.SimpleEntry<OUTCOMES, String>(OUTCOMES.Got, String.valueOf(res.getStatus())));
			Statistics.getInstance().addStatistic(endpoint.path, endpoint.type.name(), STATISTICS_TYPE.contentTypeCoverage, new AbstractMap.SimpleEntry<OUTCOMES, String>(OUTCOMES.Got, res.getMediaType() != null ? res.getMediaType().toString() : ""));
			resultString = res.readEntity(String.class);
			Statistics.getInstance().addStatistic(endpoint.path, endpoint.type.name(), STATISTICS_TYPE.responseBodyPropertiesCoverage, new AbstractMap.SimpleEntry<OUTCOMES, String>(OUTCOMES.Got, resultString));
		}
		boolean result = res!=null && res.getStatusInfo().getFamily() == Family.SUCCESSFUL;
		if(res!=null && (Config.DEBUG_INFO_ENABELED || Config.LOG_ENABELED)) {
			Logger.log("Status code received: " + res.getStatus());
			if (resultString.length() < 200) {
				Logger.log("Receved data: " + "\n" + resultString);
			}
			else {
				Logger.log("Receved data: " + "\n" + resultString.substring(0, 200) + "...");
				Logger.logDebug("Receved data (FULL): " + "\n" + resultString);
			}
		}
		Logger.log("Test ran and got result: "+ (result == expectedValue) +"(expected: " + expectedValue + "; got: "+result+")\n");
		return (result == expectedValue);
	}

	private String decodeRequestBody(Map<String, Entry<Object, Boolean>> params) {
		if (endpoint.requestBodyType != null) {
			if (endpoint.requestBodyType.contains("json")) {
				Object body = resolveParamsForBody(endpoint.requestBodyWithParams, params);
				String json = JSONValue.toJSONString(body);
				return json;
			}

			else {
				Logger.log("Curently not suporting other type then json for posts (type:"
						+ endpoint.requestBodyType + ")");
				return null;
			}
		} else {
			return "";
		}
			
	}

	private Object resolveParamsForBody(Object body ,Map<String, Entry<Object, Boolean>> params) {
		Map<String, Object> result = new HashMap<>();
		if (body != null) {
			for (Entry<String, Object> entry : ((Map<String, Object>) body).entrySet()) {
				if (((Parameter) entry.getValue()).type == PARAM_TYPE.Object) {
					result.put(entry.getKey(),
							resolveParamsForBody(((Parameter) entry.getValue()).childObject, params));
				} else if (((Parameter) entry.getValue()).type == PARAM_TYPE.Array
						&& ((Parameter) entry.getValue()).arrayItmestype == PARAM_TYPE.Object) {
					result.put(entry.getKey(),
							resolveParamsForBody(((Parameter) entry.getValue()).arrayChildObject, params));
				} else {
					if(params.containsKey(entry.getKey())) {
						result.put(entry.getKey(), params.get(entry.getKey()).getKey());
						expectedValue &= params.get(entry.getKey()).getValue();
					}
				}
			}
		}
		return result;
	}

	private String replaceParamInPath(String S, String name, Object value) {
		return S.replace("{"+name+"}", String.valueOf(value));	
	}
	
	private static Object convertNumber(String input, PARAM_TYPE type) {
		switch (type) {
		case Integer:
			return Integer.parseInt(input); 
		case Double:
			return Double.parseDouble(input);
		case Float:
		case Number: 
			return Float.parseFloat(input);
		case Long:
			return Long.parseLong(input);
		}
		return null;
	}
}
