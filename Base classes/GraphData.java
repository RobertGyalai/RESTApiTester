package implementare;

import java.awt.Color;

public class GraphData {

	public double value;
	public String text;
	public Color color;
	
	public static final Color DEFAULT_COLOR = Color.BLUE;
	
	public GraphData(double value, String text, Color color) {
		this.value = value;
		this.text = text;
		this.color = color;
	}
	public GraphData(double value, String text) {
		this.value = value;
		this.text = text;
		this.color = DEFAULT_COLOR;
	}
}
