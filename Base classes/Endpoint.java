package implementare;

import java.util.HashMap;
import java.util.Map;

public class Endpoint {

	String baseUrl;
	String path;
	public enum RequestType{GET, PUT, POST, DELETE, OPTIONS, HEAD, PATH, TRACE;
		public static RequestType getFromString(String value) {
			for(RequestType R:RequestType.values()) {
				if(R.name().equalsIgnoreCase(value)) {
					return R;
				}
			}
			return null;
		}	
	}
	RequestType type;
	Map<String, Parameter> parameters = new HashMap<>();
	boolean hasMultipleServers;
	Object requestBodyWithParams;
	String requestBodyType;
	
	public Endpoint(String baseUrl,String path, RequestType type, Map<String, Parameter> parameters) {
		if(Config.MANUAL_BASE_URL!= null && !Config.MANUAL_BASE_URL.isEmpty()) {
			this.baseUrl = Config.MANUAL_BASE_URL;
		}
		else {
			this.baseUrl = baseUrl;
		}
		this.path = path;
		this.type = type;
		this.parameters = parameters;
	}



	public int getComplexity() {
		if(type == RequestType.GET) {
			if(path.contains("{") || path.contains("}")) {
				return 1;
			}
			
			if (parameters == null && parameters.isEmpty()) {
				return 0;
			}
			for(Map.Entry<String, Parameter> parameter : parameters.entrySet()) {
				if(parameter.getValue().required) {
					return 1;
				}
			}
			return 0;
		}
		else {
			return 2;
		}
	}

	public String getURL() {
		return baseUrl + path;
	}

	public void addRequestBody(String type, Object requestBody) {
		this.requestBodyType=type;
		this.requestBodyWithParams = requestBody;
	}
	
	
	
}
