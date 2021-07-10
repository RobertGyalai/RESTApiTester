package implementare;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import implementare.Statistics.OUTCOMES;

import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.JPanel;

public class Statistics {

	public enum STATISTICS_TYPE{operationCoverage,statusCodeClassCoverage,responseBodyPropertiesCoverage,contentTypeCoverage,responseTime};
	public enum OUTCOMES{missingExample,Succes,Failiure,Got,InSpec}
	
	int totalWidth=0;
	List<GraphData> graphDataList = new ArrayList<>();
	private List<List<GraphData>> gloablStatistic = new ArrayList<>();
	public static JPanel displayPanel;
	
	private Map<String,	//path
	Map<String,	//operation
	Map<STATISTICS_TYPE,List<Object>>>> statisticsMap = new  HashMap<>();
	private Map<String, Boolean> checkedParams = new HashMap<>();
	private Set<String> allParams = new HashSet<>();
	public Map<String,Set<String>> allPaths = new HashMap<>();
	private Integer totalResponseCodes = 0;
	private Integer totalContentTypes = 0;
	private Integer totalResponseProperties = 0;
	private static final List<List<Legend>> LEGEND = List.of(List.of(new Legend("Paths covered"), new Legend("Paths covered but without full examples", Color.RED)),
			List.of(new Legend("Operations covered"), new Legend("Operations covered but without full examples", Color.RED)),
			List.of(new Legend("Paths tested with succes"), new Legend("Paths tested with failiures", Color.RED)),
			List.of(new Legend("Operations tested with succes"), new Legend("Operations tested with failiures", Color.RED)),
			List.of(new Legend("Parameters coverd in succesfull tests"), new Legend("Parameters coverd in unsuccesfull tests", Color.RED)),
			List.of(new Legend("Status codes recived from the ones defined in the spec")),
			List.of(new Legend("Content-types recived from the ones defined in the spec")),
			List.of(new Legend("Response body properties coverage"))
			);	
	
	private static Statistics INSTANCE = new Statistics();
	
	public static Statistics getInstance() {
		return INSTANCE;
	}
	
	public Statistics() {
		
	}
	public static void resetBaseStatistics() {
		resetBaseStatistics(false);
	}
	
	public static void resetBaseStatistics(boolean resetGlobal) {
		displayPanel.removeAll();
		INSTANCE.statisticsMap.clear();
		INSTANCE.checkedParams.clear();
		INSTANCE.allParams.clear();
		INSTANCE.allPaths.clear();
		INSTANCE.totalResponseCodes = 0;
		INSTANCE.totalContentTypes = 0;
		INSTANCE.totalResponseProperties = 0;
		if (resetGlobal) {
			INSTANCE.gloablStatistic.clear();
			for (int i=0;i< LEGEND.size();i++) {
				INSTANCE.gloablStatistic.add(new ArrayList<>());
			}
			Config.CONFIG_COUNT = 0;
		}
	}

	public void addStatistic(String path, String operation, STATISTICS_TYPE type, Object resultValue) {
		statisticsMap.computeIfAbsent(path, p -> new HashMap<>())
		.computeIfAbsent(operation, o -> new HashMap<>())
		.computeIfAbsent(type, t -> new ArrayList<>())
		.add(resultValue);
	}
	
	public void addBaseParams(Set<String> params) {
		for( String param: params) {
			checkedParams.putIfAbsent(param, false);
		}
	}
	
	public void addSuccesfulParams(Set<String> params) {
		for( String param: params) {
			checkedParams.put(param, true);
		}
	}
	
	public void initTotals(Map<String,Set<String>> paths,Set<String> params) {
		this.allParams = params;
		this.allPaths = paths.entrySet().stream().filter(m -> !m.getValue().isEmpty()).collect(Collectors.toMap(e->e.getKey(), e -> e.getValue()));
	}
	
	public void generateStatistics() {
		int totalPaths =  (int) allPaths.entrySet().stream().filter(m -> !m.getValue().isEmpty()).count();
		int totalOperations = allPaths.entrySet().stream().map( m -> m.getValue().size()).reduce(0, Integer::sum);
		int operationsCovered = 0;
		int operationsCoveredwithMissingExamples = 0;
		int operationsTestSucces = 0;
		int operationsTestFail = 0;
		int pathsCovered = 0;
		int pathsCoveredwithMissingExamples = 0;
		int pathsTestSucces = 0;
		int pathsTestFail = 0;
		int responseCodesCovered = 0;
		int contentTypesCovered = 0; 
		Long totalTime = 0L;
		int totalRequests = 0;
		int responseBodyPropertiesCovered = 0;
		
		
		for( Entry<String, Map<String, Map<STATISTICS_TYPE, List<Object>>>> path : statisticsMap.entrySet()) {
			boolean pathCoveredwithMissingExamples = false;
			boolean pathTestSucces = true;
			boolean hasOperation = false;
			for ( Entry<String, Map<STATISTICS_TYPE, List<Object>>> operation : path.getValue().entrySet()) {
				responseCodesCovered += handleResponseCodeCoverage(operation.getValue().get(STATISTICS_TYPE.statusCodeClassCoverage));
				contentTypesCovered += handleContentTypeCoverage(operation.getValue().get(STATISTICS_TYPE.contentTypeCoverage));
				responseBodyPropertiesCovered += handleResponseBodyPropertiesCoverage(operation.getValue().get(STATISTICS_TYPE.responseBodyPropertiesCoverage));
				
				if(!operation.getValue().get(STATISTICS_TYPE.operationCoverage).contains(OUTCOMES.Succes) && ! operation.getValue().get(STATISTICS_TYPE.operationCoverage).contains(OUTCOMES.Failiure)) {
					continue;
				}
				hasOperation = true;
				if (operation.getValue().get(STATISTICS_TYPE.operationCoverage).contains(OUTCOMES.missingExample)) {
					operationsCoveredwithMissingExamples ++;
					pathCoveredwithMissingExamples = true;
				}
				else {
					operationsCovered ++;
				}
				if (operation.getValue().get(STATISTICS_TYPE.operationCoverage).contains(OUTCOMES.Succes)) {
					operationsTestSucces ++;
				}
				else if (operation.getValue().get(STATISTICS_TYPE.operationCoverage).contains(OUTCOMES.Failiure)) {
					operationsTestFail ++;
					pathTestSucces = false;
				}
				totalTime += ((Entry<Integer, Long>) operation.getValue().get(STATISTICS_TYPE.responseTime).get(0)).getValue().longValue();
				totalRequests += ((Entry<Integer, Long>) operation.getValue().get(STATISTICS_TYPE.responseTime).get(0)).getKey();
			}
			if(hasOperation) {
				if( pathCoveredwithMissingExamples) {
					pathsCoveredwithMissingExamples++;
				}
				else {
					pathsCovered++;
				}
				if( pathTestSucces) {
					pathsTestSucces++;
				}
				else {
					pathsTestFail++;
				}
			}
		}
		
		int totalParam = allParams.size();
		int paramsCovered = 0;
		int paramsCoveredSuccesfully = 0;
		
		paramsCoveredSuccesfully = (int) checkedParams.entrySet().stream().filter(p -> p.getValue() == true).count();
		paramsCovered = checkedParams.size() - paramsCoveredSuccesfully;
		
		
		StringBuilder sb = new StringBuilder("\n\n======STATISTICS======\n");
		
		sb.append("Path coverage:\n")
		.append("Paths covered: ").append((float)pathsCovered*100/totalPaths).append("% (").append(pathsCovered).append("/").append(totalPaths).append(")\n")
		.append("Paths covered but without full examples: ").append((float)pathsCoveredwithMissingExamples*100/totalPaths).append("% (").append(pathsCoveredwithMissingExamples).append("/").append(totalPaths).append(")\n")
		.append("Operations coverage:\n")
		.append("Operations covered: ").append((float)operationsCovered*100/totalOperations).append("% (").append(operationsCovered).append("/").append(totalOperations).append(")\n")
		.append("Operations covered but without full examples: ").append((float)operationsCoveredwithMissingExamples*100/totalOperations).append("% (").append(operationsCoveredwithMissingExamples).append("/").append(totalOperations).append(")\n")
		.append("Path tests:\n")
		.append("Paths tested with succes: ").append((float)pathsTestSucces*100/totalPaths).append("% (").append(pathsTestSucces).append("/").append(totalPaths).append(")\n")
		.append("Paths tested with failiures: ").append((float)pathsTestFail*100/totalPaths).append("% (").append(pathsTestFail).append("/").append(totalPaths).append(")\n")
		.append("Operations tests:\n")
		.append("Operations tested with succes: ").append((float)operationsTestSucces*100/totalOperations).append("% (").append(operationsTestSucces).append("/").append(totalOperations).append(")\n")
		.append("Operations tested with failiures: ").append((float)operationsTestFail*100/totalOperations).append("% (").append(operationsTestFail).append("/").append(totalOperations).append(")\n")
		.append("Parameter coverage:\n")
		.append("Parameters coverd in succesfull tests: ").append((float)paramsCoveredSuccesfully*100/totalParam).append("% (").append(paramsCoveredSuccesfully).append("/").append(totalParam).append(")\n")
		.append("Parameters covered in unsuccesfull tests: ").append((float)paramsCovered*100/totalParam).append("% (").append(paramsCovered).append("/").append(totalParam).append(")\n")
		.append("Status code class coverage:\n")
		.append("Status codes recived from the ones defined in the spec: ");
		if(totalResponseCodes>0) {
			sb.append((float)responseCodesCovered*100/totalResponseCodes).append("% (").append(responseCodesCovered).append("/").append(totalResponseCodes).append(")\n");
		}
		else{
			sb.append("No response codes found in the spec\n");
		}
		sb.append("Content-type coverage:\n")
		.append("Content-types recived from the ones defined in the spec: ");
		if(totalContentTypes>0) {
			sb.append((float)contentTypesCovered*100/totalContentTypes).append("% (").append(contentTypesCovered).append("/").append(totalContentTypes).append(")\n");
		}
		else{
			sb.append("No content-types found in the spec\n");
		}
		sb.append("Response body properties coverage:\n")
		.append("Response body properties recived from the ones defined in the spec: ");
		if(totalResponseProperties>0) {
			sb.append((float)responseBodyPropertiesCovered*100/totalResponseProperties).append("% (").append(responseBodyPropertiesCovered).append("/").append(totalResponseProperties).append(")\n");
		}
		else{
			sb.append("No response body properties found in the spec\n");
		}
		
		sb.append("Performance metric:\n")
		.append("Average response time for a request in ms: ").append(totalTime/totalRequests).append("ms (").append(totalTime).append("ms / ").append(totalRequests).append(" requests)\n");;
		
		sb.append("Param cache size: ").append(ParameterExampleCache.getSize()).append("\n");
		
		Logger.log(sb.toString());

		if(Config.RUN_ALL_CONFIGS == false) {
			
			graphDataList.add(new GraphData((float)pathsCovered*100/totalPaths, "Paths covered: " + (float)pathsCovered*100/totalPaths + "% ("+pathsCovered+"/" + totalPaths + ")"));
			graphDataList.add(new GraphData((float)pathsCoveredwithMissingExamples*100/totalPaths, "Paths covered but without full examples: " + (float)pathsCoveredwithMissingExamples*100/totalPaths + "% (" + pathsCoveredwithMissingExamples + "/" + totalPaths + ")", Color.RED));
			addGraph();
			
			graphDataList.add(new GraphData((float)operationsCovered*100/totalOperations, "Operations covered: " + (float)operationsCovered*100/totalOperations + "% (" + operationsCovered + "/" + totalOperations + ")"));
			graphDataList.add(new GraphData((float)operationsCoveredwithMissingExamples*100/totalOperations, "Operations covered but without full examples: " + (float)operationsCoveredwithMissingExamples*100/totalOperations + "% (" + operationsCoveredwithMissingExamples + "/" + totalOperations + ")", Color.RED));
			addGraph();
			
			graphDataList.add(new GraphData((float)pathsTestSucces*100/totalPaths, "Paths tested with succes: " + (float)pathsTestSucces*100/totalPaths + "% (" + pathsTestSucces + "/" + totalPaths + ")"));
			graphDataList.add(new GraphData((float)pathsTestFail*100/totalPaths, "Paths tested with failiures: " + (float)pathsTestFail*100/totalPaths + "% (" + pathsTestFail + "/" + totalPaths + ")", Color.RED));
			addGraph();
			
			graphDataList.add(new GraphData((float)operationsTestSucces*100/totalOperations, "Operations tested with succes: " + (float)operationsTestSucces*100/totalOperations + "% (" + operationsTestSucces + "/" + totalOperations + ")"));
			graphDataList.add(new GraphData((float)operationsTestFail*100/totalOperations, "Operations tested with failiures: " + (float)operationsTestFail*100/totalOperations + "% (" + operationsTestFail + "/" + totalOperations + ")", Color.RED));
			addGraph();
			
			graphDataList.add(new GraphData((float)paramsCoveredSuccesfully*100/totalParam, "Parameters coverd in succesfull tests: " + (float)paramsCoveredSuccesfully*100/totalParam + "% (" + paramsCoveredSuccesfully + "/" + totalParam + ")"));
			graphDataList.add(new GraphData((float)paramsCovered*100/totalParam, "Parameters covered in unsuccesfull tests: " + (float)paramsCovered*100/totalParam + "% (" + paramsCovered + "/" + totalParam + ")", Color.RED));
			addGraph();
			
			
			if(totalResponseCodes>0) {
				graphDataList.add(new GraphData((float)responseCodesCovered*100/totalResponseCodes, "Status codes recived from the ones defined in the spec: " + (float)responseCodesCovered*100/totalResponseCodes + "% (" + responseCodesCovered + "/" + totalResponseCodes + ")"));
				addGraph();
			}
			
			if(totalContentTypes>0) {
				graphDataList.add(new GraphData((float)contentTypesCovered*100/totalContentTypes, "Content-types recived from the ones defined in the spec: " + (float)contentTypesCovered*100/totalContentTypes + "% (" + contentTypesCovered + "/" + totalContentTypes + ")"));
				addGraph();
			}
			
			if(totalResponseProperties>0) {
				graphDataList.add(new GraphData((float)responseBodyPropertiesCovered*100/totalResponseProperties, "Response body properties recived from the ones defined in the spec: " + (float)responseBodyPropertiesCovered*100/totalResponseProperties + "% (" + responseBodyPropertiesCovered + "/" + totalResponseProperties + ")"));
				addGraph();
			}
		}
		else {
			
			gloablStatistic.get(0).add(new GraphData((float)pathsCovered*100/totalPaths, ""));
			gloablStatistic.get(0).add(new GraphData((float)pathsCoveredwithMissingExamples*100/totalPaths, Config.CURRENT_CONFIG, Color.RED));
			
			gloablStatistic.get(1).add(new GraphData((float)operationsCovered*100/totalOperations, ""));
			gloablStatistic.get(1).add(new GraphData((float)operationsCoveredwithMissingExamples*100/totalOperations, Config.CURRENT_CONFIG, Color.RED));
			
			gloablStatistic.get(2).add(new GraphData((float)pathsTestSucces*100/totalPaths, ""));
			gloablStatistic.get(2).add(new GraphData((float)pathsTestFail*100/totalPaths,  Config.CURRENT_CONFIG, Color.RED));
			
			gloablStatistic.get(3).add(new GraphData((float)operationsTestSucces*100/totalOperations, ""));
			gloablStatistic.get(3).add(new GraphData((float)operationsTestFail*100/totalOperations,  Config.CURRENT_CONFIG, Color.RED));
			
			gloablStatistic.get(4).add(new GraphData((float)paramsCoveredSuccesfully*100/totalParam, ""));
			gloablStatistic.get(4).add(new GraphData((float)paramsCovered*100/totalParam,  Config.CURRENT_CONFIG, Color.RED));
			
			
			if(totalResponseCodes>0) {
				gloablStatistic.get(5).add(new GraphData((float)responseCodesCovered*100/totalResponseCodes,  Config.CURRENT_CONFIG));
			}
			if(totalContentTypes>0) {
				gloablStatistic.get(6).add(new GraphData((float)contentTypesCovered*100/totalContentTypes,  Config.CURRENT_CONFIG));
			}
			if(totalResponseProperties>0) {
				gloablStatistic.get(7).add(new GraphData((float)responseBodyPropertiesCovered*100/totalResponseProperties,  Config.CURRENT_CONFIG));
			}
			Config.CONFIG_COUNT ++;
			if (Config.CONFIG_COUNT == Config.CONFIG_TOTAL) {
				addGlobalGraphs();
			}
		}
		
	}

	private int handleResponseBodyPropertiesCoverage(List<Object> list) {
		if(list == null || list.isEmpty()) {
			return 0;
		}
		List<String> inSpec = list.stream().filter(p -> ((Entry<OUTCOMES, String>)p).getKey() == OUTCOMES.InSpec).map( p -> ((Entry<OUTCOMES, String>)p).getValue()).collect(Collectors.toList());
		List<String> received = list.stream().filter(p -> ((Entry<OUTCOMES, String>)p).getKey() == OUTCOMES.Got).map(p -> ((Entry<OUTCOMES, String>) p).getValue()).collect(Collectors.toList());
		int correctResponses = 0;
		for (String r : inSpec) {
			if (received.stream().anyMatch(p -> p.toLowerCase().contains("\""+r.toLowerCase()+"\""))) {
				correctResponses++;
			}
		}
		totalResponseProperties += inSpec.size();
		return correctResponses;
	}

	private int handleContentTypeCoverage(List<Object> list) {
		if(list == null || list.isEmpty()) {
			return 0;
		}
		List<String> inSpec = list.stream().filter(p -> ((Entry<OUTCOMES, String>)p).getKey() == OUTCOMES.InSpec).map( p -> ((Entry<OUTCOMES, String>)p).getValue()).collect(Collectors.toList());
		List<String> received = list.stream().filter(p -> ((Entry<OUTCOMES, String>)p).getKey() == OUTCOMES.Got).map(p -> ((Entry<OUTCOMES, String>) p).getValue()).collect(Collectors.toList());
		int correctResponses = 0;
		for (String r : inSpec) {
			int index = r.indexOf(';');
			if (inSpec.contains("*/*") || received.stream().anyMatch(p -> p.toLowerCase().contains(index!= -1 ? r.toLowerCase().substring(0, index) : r.toLowerCase()))) {
				correctResponses++;
			}
		}
		totalContentTypes += inSpec.size();
		return correctResponses;
	}

	private int handleResponseCodeCoverage(List<Object> list) {
		if(list == null || list.isEmpty()) {
			return 0;
		}
		List<String> inSpec = list.stream().filter(p -> ((Entry<OUTCOMES, String>)p).getKey() == OUTCOMES.InSpec).map( p -> ((Entry<OUTCOMES, String>)p).getValue()).collect(Collectors.toList());
		List<String> received = list.stream().filter(p -> ((Entry<OUTCOMES, String>)p).getKey() == OUTCOMES.Got).map( p -> ((Entry<OUTCOMES, String>)p).getValue()).collect(Collectors.toList());
		int correctResponses = 0;
		for(String r : inSpec) {
			if(r.contains("xx")) {
				if( received.stream().anyMatch(p -> p.startsWith(r.substring(0, 1)))) {
					correctResponses ++;
				}
			}
			else {
				if(received.contains(r)) {
					correctResponses ++;
				}
			}
		}
		totalResponseCodes += inSpec.size();
		return correctResponses;
	}
	
	public static void initialize() {
		displayPanel.setLayout(new FlowLayout());
	}
	
	private void addGraph() {

		Graph G = new Graph(List.copyOf(graphDataList), null);
		graphDataList.clear();
		displayPanel.add(G);
		totalWidth += G.getWidth();
	}

	private void addGlobalGraphs() {
		for (int i=0;i<LEGEND.size(); i++) {
			Graph G = new Graph(List.copyOf(gloablStatistic.get(i)), LEGEND.get(i));
			displayPanel.add(G);
		}
		
	}


}
