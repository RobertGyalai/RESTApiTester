package implementare;

import java.util.HashMap;
import java.util.Map;

public class ParameterExampleCache {
	private static Map<String, Object> CACHE = new HashMap<>();
	
	public static Object get(String key) {
		return CACHE.get(key);
	}
	
	public static void put(String key, Object value) {
		CACHE.put(key, value);
	}
	
	public static int getSize() {
		return CACHE.size();
	}

	public static void clear() {
		CACHE.clear();
	}
}
