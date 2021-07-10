package implementare;

public class Config {

	public static boolean DEBUG_INFO_ENABELED = true;
	public static boolean LOG_ENABELED = false;
	public static String MANUAL_BASE_URL = null;
	//ex =>
	//https://api.apis.guru/v2/specs/amentum.space/space_radiation/1.0.1/openapi.json
	//"https://spaceradiation.amentum.space"
	
	public static Integer PAUSE_BETWEEN_REQUESTS = null;
	
	
//1	
	public static boolean GENERATE_PARTIAL_TESTS = false;	
	public static boolean IGNORE_NON_REQUIRED_PARAMS = true;	//false=use all params to generate tests
	
//2	
//	public static boolean GENERATE_PARTIAL_TESTS = false;	
//	public static boolean IGNORE_NON_REQUIRED_PARAMS = false;	
	
//3	
//	public static boolean GENERATE_PARTIAL_TESTS = true;	
//	public static boolean IGNORE_NON_REQUIRED_PARAMS = true;	
	
//4	
//	public static boolean GENERATE_PARTIAL_TESTS = true;	
//	public static boolean IGNORE_NON_REQUIRED_PARAMS = false;	
	
	public static boolean RUN_ALL_CONFIGS = false;
	public static Integer CONFIG_TOTAL = 4;	// 1-indexed
	public static Integer CONFIG_COUNT = 0;
	public static String CURRENT_CONFIG = "";

}
