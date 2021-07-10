package implementare;

import java.util.ArrayList;
import java.util.List;

public class Parameter {

	 String name;
	public enum PARAM_TYPE {String, Integer, Float,Double,Number,Long, Enum, Boolean, Object, Array ;
		
		public static PARAM_TYPE getFromString(String value) {
		for(PARAM_TYPE P:PARAM_TYPE.values()) {
			if(P.name().equalsIgnoreCase(value)) {
				return P;
			}
		}
		return null;
	}
	};
	 PARAM_TYPE type;
	 PARAM_TYPE arrayItmestype=null;
	 String format;
	 List<Object> enumOptions = new ArrayList<>();
	 boolean required;
	public enum PARAM_LOCATION {path, query, header, cookie, requestBody; 
		
	public static PARAM_LOCATION getFromString(String value) {
		for(PARAM_LOCATION P:PARAM_LOCATION.values()) {
			if(P.name().equalsIgnoreCase(value)) {
				return P;
			}
		}
		return null;
	}
	};
	 PARAM_LOCATION location;
	 String description;
	 Object example;
	 boolean resolved;
	 String ref;
	 String mediaType;
	 Object childObject;
	 Object arrayChildObject;
	 String min;
	 String max;
	 
	public Parameter() {
	}
	
}
