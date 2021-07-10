package implementare;

import java.awt.Color;

public class Legend {
	public String text;
	public Color color;
	private static final Color DEFAULT_COLOR = Color.BLUE;

	
	public Legend(String text, Color color) {
		this.text = text;
		this.color = color;
	}
	
	public Legend(String text) {
		this.text = text;
		this.color = DEFAULT_COLOR;
	}
}
