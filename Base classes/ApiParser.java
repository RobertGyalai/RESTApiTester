package implementare;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.Map.Entry;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import implementare.Endpoint.RequestType;
import implementare.Parameter.PARAM_LOCATION;
import implementare.Parameter.PARAM_TYPE;
import implementare.Statistics.OUTCOMES;
import implementare.Statistics.STATISTICS_TYPE;

public class ApiParser {
	private static final String SECURITY_SCHEMA_KEYWORD ="securitySchemes";
	public enum PARTS{parameters,respnse}

	JSONObject api;
	Map<String,Object> components = new HashMap<>();
	
	public ApiParser(String apiDefinition) throws ParseException {
		api = (JSONObject) new JSONParser().parse(apiDefinition);
		components = (Map<String, Object>) api.get("components");
	}
	
	public Object resolveComponent(String ref) {
		String[] paths = ref.split("/");
		int i=0;
		while(!paths[i].equalsIgnoreCase("components")) {
			i++;
		}
		i++;
		Object o = components;
		while(i<paths.length) {
			o = ((Map<String,Object>)o).get(paths[i]);
			i++;
		}
		return o;
	}
	
	public List<Endpoint> parsePaths() {
		List<Endpoint> endpoints = new ArrayList<>();
		List<Object> servers = (ArrayList) this.api.get("servers");
		String baseurl = "/";
		if(servers != null && !servers.isEmpty()) {
			baseurl = (String) ((JSONObject)servers.get(0)).getOrDefault("url","/");
		}
		Map<String, Object> paths= (Map<String, Object>) this.api.get("paths");
		Set<String> allParams = new HashSet<>();	
		Map<String,Set<String>> allPaths = new HashMap<>();	
		for(Entry<String, Object> entry : paths.entrySet()) {
			String path = entry.getKey();
			allPaths.put(path, new HashSet<String>());
			Map<String, Object> requests = (Map<String, Object>) entry.getValue();
			List<Map<String, Object>> additionalParams = (List<Map<String, Object>>)requests.get("parameters");
			for(Entry<String, Object> request:requests.entrySet()) {
				RequestType operation = RequestType.getFromString(request.getKey());
				if(operation != null && (operation == RequestType.GET || operation == RequestType.POST || operation == RequestType.PUT)) {
					allPaths.get(path).add(operation.name());
				}
				if(operation!=null && (operation == RequestType.GET || operation == RequestType.POST || operation == RequestType.PUT)) {
					
					Map<String, Parameter> createdParams = createParameters((List<Map<String, Object>>) ((Map<String, Object>) request.getValue()).get(PARTS.parameters.name()));
					if(additionalParams != null && !additionalParams.isEmpty()) {
						createdParams.putAll(createParameters(additionalParams));
					}
					Endpoint endpoint = new Endpoint(baseurl, path, operation, createdParams);
					createRequestBody(endpoint, ((Map<String, Object>) request.getValue()).get("requestBody"));
					if(servers != null && servers.size()>1) {
						endpoint.hasMultipleServers=true;
					}
					allParams.addAll(endpoint.parameters.keySet());
					if(endpoint.requestBodyWithParams != null && (operation == RequestType.GET || operation == RequestType.POST || operation == RequestType.PUT)) {
						addParamsTotal(endpoint.requestBodyWithParams,allParams);
					}
					if(operation == RequestType.GET || operation == RequestType.POST || operation == RequestType.PUT) {
						Map<String, Object> responses = (Map<String, Object>) ((Map<String, Object>) request.getValue()).get("responses");
						if(responses!= null) {
							for(Entry<String, Object> response : responses.entrySet()) {
								Statistics.getInstance().addStatistic(path, operation.name(), STATISTICS_TYPE.statusCodeClassCoverage, new AbstractMap.SimpleEntry<OUTCOMES, String>(OUTCOMES.InSpec, response.getKey()));
								Map<String, Object> tmpResp = (Map<String, Object>) response.getValue();
								while (isRef(tmpResp)) {
									tmpResp=(Map<String, Object>) resolveComponent((String) tmpResp.get("$ref"));
								}
								tmpResp = resolveOneAnyOf(tmpResp);
								Map<String, Object> content = (Map<String, Object>) (tmpResp).get("content");
								if(content != null) {
									for(Entry<String, Object> contentType : content.entrySet()) {
										Statistics.getInstance().addStatistic(path, operation.name(), STATISTICS_TYPE.contentTypeCoverage, new AbstractMap.SimpleEntry<OUTCOMES, String>(OUTCOMES.InSpec, contentType.getKey()));
										if(true || contentType.getKey().contains("json")) { 
											Map<String, Object> rschema = (Map<String, Object>) ((Map<String, Object>)contentType.getValue()).get("schema");
											while (isRef(rschema)) {
												rschema=(Map<String, Object>) resolveComponent((String) rschema.get("$ref"));
											}
											rschema = resolveOneAnyOf(rschema);
											if(rschema != null) {
												String rtype = (String) rschema.get("type");
												if("object".equalsIgnoreCase(rtype)) {
													HashSet<String> rset = addResponseschemaToStatistics((Map<String, Object>)rschema.get("properties"));
													for(String name: rset) {
														Statistics.getInstance().addStatistic(path, operation.name(), STATISTICS_TYPE.responseBodyPropertiesCoverage, new AbstractMap.SimpleEntry<OUTCOMES, String>(OUTCOMES.InSpec, name));
													}
												}
												else if("array".equalsIgnoreCase(rtype)) {
													Map<String, Object> items = (Map<String, Object>) rschema.get("items");
													while (isRef(items)) {
														items=(Map<String, Object>) resolveComponent((String) items.get("$ref"));
													}
													items = resolveOneAnyOf(items);
													HashSet<String> rset = addResponseschemaToStatistics((Map<String, Object>)items.get("properties"));
													for(String name: rset) {
														Statistics.getInstance().addStatistic(path, operation.name(), STATISTICS_TYPE.responseBodyPropertiesCoverage, new AbstractMap.SimpleEntry<OUTCOMES, String>(OUTCOMES.InSpec, name));
													}
												}
											}
											else { 
												rschema = (Map<String, Object>) ((Map<String, Object>)contentType.getValue()).get("examples");
												if(rschema!= null) {
													rschema = (Map<String, Object>) rschema.get("response");
													if(rschema != null) {
														Object values =  rschema.get("value");
														if(values instanceof JSONArray) {
															rschema = (Map<String, Object>) ((JSONArray) values).get(0);
														}else if(values instanceof JSONObject){
															rschema = (Map<String, Object>) values;
														}
														else {
															rschema = null;
														}
														if(rschema!=null) {
															HashSet<String> rset = addResponseschemaToStatistics(rschema);
															for(String name: rset) {
																Statistics.getInstance().addStatistic(path, operation.name(), STATISTICS_TYPE.responseBodyPropertiesCoverage, new AbstractMap.SimpleEntry<OUTCOMES, String>(OUTCOMES.InSpec, name));
															}
														}
													}
												}
											}
										}
									}
								}
							}
						}
					}
					
					endpoints.add(endpoint);
				}
			}
		}
		Statistics.getInstance().initTotals(allPaths, allParams);
		return endpoints;
	}
	
	private HashSet<String> addResponseschemaToStatistics(Map<String, Object> object) {
		HashSet<String> set = new HashSet<>();
		if (object != null) {
			for (Entry<String, Object> entry : object.entrySet()) {
				set.add(entry.getKey());
				if( entry.getValue() instanceof JSONObject) {
					Map<String, Object> tmpSchema = (Map<String, Object>) entry.getValue();
					if (isRef(tmpSchema)) {
						tmpSchema=(Map<String, Object>) resolveComponent((String) tmpSchema.get("$ref"));
					}
					tmpSchema = resolveOneAnyOf(tmpSchema);
					String type = (String) tmpSchema.get("type");
					if ("object".equalsIgnoreCase(type)) {
						Object properties = tmpSchema.get("properties");
						if (properties != null) {
							set.addAll(addResponseschemaToStatistics((Map<String, Object>) properties));
						}
					}
					if ("array".equalsIgnoreCase(type)) {
						Map<String, Object> items = (Map<String, Object>) tmpSchema.get("items");
						if (isRef(items)) {
							items=(Map<String, Object>) resolveComponent((String) items.get("$ref"));
						}
						items = resolveOneAnyOf(items);
						Object properties = items.get("properties");
						if (properties != null) {
							set.addAll(addResponseschemaToStatistics((Map<String, Object>) properties));
						}
					}
				}
			}
		}
		return set;
	}

	private void addParamsTotal(Object body, Set<String> allParams) {
		for(Entry<String, Object> entry: ((Map<String, Object>)body).entrySet()) {
			if(((Parameter)entry.getValue()).type != PARAM_TYPE.Object) {
				allParams.add(entry.getKey());
			}
			else if(((Parameter)entry.getValue()).childObject != null){
				addParamsTotal(((Parameter)entry.getValue()).childObject, allParams);
			}
		}
	}

	private void createRequestBody(Endpoint endpoint, Object body) {
		if(body == null) {
			return;
		}
		Map<String, Object> bodyMap = (Map<String, Object>) body;
		while (isRef(bodyMap)) {
			bodyMap=(Map<String, Object>) resolveComponent((String) bodyMap.get("$ref"));
		}
		bodyMap = resolveOneAnyOf(bodyMap);
		Map<String, Object> content = (Map<String, Object>) bodyMap.get("content");
		String type =  (String) content.keySet().toArray()[0];
		Map<String, Object> media = (Map<String, Object>) content.get(type);
		Map<String, Object> schema = (Map<String, Object>)media.get("schema");
		if (isRef(schema)) {
			schema=(Map<String, Object>) resolveComponent((String) schema.get("$ref"));
		}
		schema = resolveOneAnyOf(schema);
		if(!"object".equalsIgnoreCase((String) schema.get("type"))) {
			return;
		}
		Object rBody = parseSchemaForRequestBody((Map<String, Object>) schema.get("properties"),null);
		Object example = schema.get("example");
		if(example != null && example instanceof JSONObject) {
			for(Entry<String, Object> entry:((Map<String,Object>)example).entrySet()) {
				assingExample((Map<String,Parameter>)rBody, entry);
			}
		}
		endpoint.addRequestBody(type, rBody);
		return ;
	}

	private void assingExample(Map<String, Parameter> rBody, Entry<String, Object> entry) {
		if(rBody == null) {
			return;
		}
		Parameter P = rBody.get(entry.getKey());
		if (P == null) {
			return;
		}
		if(P.type!=PARAM_TYPE.Object) {
			P.example = entry.getValue();
		}
		else {
			assingExample((Map<String,Parameter>) P.childObject, (Entry<String, Object>) entry.getValue());
		}
		
	}

	private Object parseSchemaForRequestBody(Map<String, Object> properties, List<String> requieredParams) {
		if(properties==null) {
			return null;
		}
		Map<String, Object> body = new HashMap<String, Object>();
		for (Entry<String, Object> property:properties.entrySet()) {
			Parameter P = new Parameter();
			P.name = property.getKey();
			if(requieredParams!=null && requieredParams.contains(P.name)) {
				P.required = true;
			}
			
			P.location = PARAM_LOCATION.requestBody;
			Map<String, Object> values = (Map<String, Object>) property.getValue();
			if (isRef(values)) {
				values=(Map<String, Object>) resolveComponent((String) values.get("$ref"));
			}
			values = resolveOneAnyOf(values);
			P.description = (String) values.get("description");
			P.example = values.get("example");
			P.type = PARAM_TYPE.getFromString((String) values.get("type"));
			P.format = (String) values.get("format");
			P.min = (String) values.get("minimum");
			P.min = (String) values.get("maximum");
			List<String> needed = null;
			if(P.type == PARAM_TYPE.Object) {
				needed = null;
				try {
					needed =  (List<String>) values.get("required"); 
				}catch (Exception e) {}
				P.childObject = parseSchemaForRequestBody((Map<String, Object>) values.get("properties"), needed);
			}
			else {
				P.required =(boolean) values.getOrDefault("required", false); 
			}
			if(P.type == PARAM_TYPE.Array) {
				Map<String, Object> items = (Map<String, Object>) values.get("items");
				items = resolveOneAnyOf(items);
				P.arrayItmestype = PARAM_TYPE.getFromString((String) items.get("type"));
				if(P.arrayItmestype == PARAM_TYPE.Object) {
					 needed = null;
						try {
							needed =  (List<String>) items.get("required"); 
						}catch (Exception e) {}
						P.arrayChildObject = parseSchemaForRequestBody((Map<String, Object>) items.get("properties"), needed);
				}
			}
			body.put(P.name, P);
		}
		return body;
	}

	private Map<String, Parameter> createParameters(List<Map<String, Object>> params) {
		Map<String, Parameter> parameters = new HashMap<>();
		if(params != null) {
			for(Map<String, Object> parameter : params) {
				if (isRef(parameter)) {
					parameter=(Map<String, Object>) resolveComponent((String) parameter.get("$ref"));
				}
				parameter = resolveOneAnyOf(parameter);
				Parameter P = new Parameter();
				P.name = (String) parameter.get("name");
				P.location = PARAM_LOCATION.getFromString((String) parameter.get("in"));
				P.description = (String) parameter.get("description");
				P.required =(boolean) parameter.getOrDefault("required",false);	
				Map<String,Object> schema = null;
				Map<String,Object> content = (Map<String, Object>) parameter.get("content");
				if(content!=null) {
					for(Entry<String, Object> C: content.entrySet()) {
						P.mediaType = C.getKey();
						Map<String, Object> media = (Map<String, Object>) C.getValue();
						schema = (Map<String, Object>) media.get("schema");
						parseSchema(schema, P); 
						Object o = media.get("example");
						if(o != null) {
							P.example = o;
						}

						for (Map.Entry<String,Object> entry: ((Map<String, Object>)media.getOrDefault("examples", Collections.emptyMap())).entrySet()) {
							P.example = entry.getValue();
						}
					}
				}
				else {
					schema = (Map<String, Object>) parameter.get("schema");
					parseSchema(schema, P);
				}
				Object o = parameter.get("example");
				if(o != null) {
					P.example = o;
				}

				for (Map.Entry<String,Object> entry: ((Map<String, Object>)parameter.getOrDefault("examples", Collections.emptyMap())).entrySet()) {
					P.example = entry.getValue();
				}
				parameters.put(P.name, P);
			}
		}
		return parameters;
	}

	private void parseSchema(Map<String, Object> schema, Parameter P) {
		if (isRef(schema)) {
			schema=(Map<String, Object>) resolveComponent((String) schema.get("$ref"));
		}
		schema = resolveOneAnyOf(schema);
		P.type = PARAM_TYPE.getFromString((String) schema.get("type"));
		Object o = schema.get("default");
		if(o != null) {
			P.example = o;
		}
		o = schema.get("example");
		if(o != null) {
			P.example = o;
		}
		String format = (String) schema.get("format");
		P.min = String.valueOf(schema.get("minimum"));
		P.max = String.valueOf(schema.get("maximum"));
		if(P.min.equals("null")) {
			P.min = null;
		}
		if(P.max.equals("null")) {
			P.max = null;
		}
		if(format!= null) {
			switch (format) {
			case "int32":
				P.type = PARAM_TYPE.Integer;
				break;
			case "int64":
				P.type = PARAM_TYPE.Long;
				break;
			case "float":
				P.type = PARAM_TYPE.Float;
				break;
			case "double":
				P.type = PARAM_TYPE.Double;
				break;
			}
		}
		List<Object> enumValues= (List<Object>) schema.get("enum");
		if(enumValues!= null) {
			P.type=PARAM_TYPE.Enum;
			P.enumOptions = new ArrayList<>(enumValues);
			if(P.example == null) {
				P.example = enumValues.get(0);	
			}
		}
		if(P.type == PARAM_TYPE.Array) {
			Map<String, Object> items = (Map<String, Object>) schema.get("items");
			items = resolveOneAnyOf(items);
			P.arrayItmestype = PARAM_TYPE.getFromString((String) items.get("type"));
		}
	}

	
	private Map<String, Object> resolveOneAnyOf(Map<String, Object> schema) {
		if(schema == null) {
			return null;
		}
		Object oneOf = schema.get("oneOf");
		Object anyOf = schema.get("anyOf");
		Object allOf = schema.get("allOf");
		if(oneOf == null && anyOf == null && allOf == null) {
			return schema;
		}
		else if(oneOf != null){ 
			return (Map<String, Object>) ((List<Object>)oneOf).get(0);
		}
		else if(anyOf != null){
			return (Map<String, Object>) ((List<Object>)anyOf).get(0);
		}
		else if(allOf != null){
			Map<String, Object> map = new HashMap<String, Object>();
			((List<Map<String, Object>>)allOf).forEach(m -> {
				while (isRef(m)) {
					m=(Map<String, Object>) resolveComponent((String) m.get("$ref"));
				}
				m = resolveOneAnyOf(m);
				if (map.containsKey("properties")) {
					((Map<String, Object>)map.get("properties")).putAll(((Map<String, Object>)m.get("properties")));
				}
				else {
					map.putAll(m);
				}
				});
			return map;
		}
		else {
			return null;
		}
	}

	public static boolean hasSecuritySchema(String apiDefinition) {
		return apiDefinition.contains(SECURITY_SCHEMA_KEYWORD);
	}
	
	private boolean isRef(Map<String,Object> map) {
		return map != null && map.containsKey("$ref");
	}
}
