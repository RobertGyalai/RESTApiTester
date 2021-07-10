package implementare;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SpringLayout;

public class Graph extends JPanel{
	
	private static final double CONVERSION = 2.0;	//1% = x px
	private static final int MARGIN = 10;
	private static final int CULOMN_WIDTH = 60;
	private static final int MAX_COLUMN_WIDTH = 60;
	private static final int LEGEND_HEIGHT = 80;

	private static final int HEIGHT = (int) (MARGIN + 100*CONVERSION) + LEGEND_HEIGHT;
	private static final int MIN_WIDTH = HEIGHT *3/4;
	private int WIDTH;
	private List<GraphData> data;
	private List<Legend> legendData;
	private JTextArea legend;
	
	private boolean isFull = false;
	
	public Graph(List<GraphData> graphData, List<Legend> legendData) {
		super();
		this.data = graphData;
		if (legendData != null) {
			this.legendData = legendData;
			isFull = true;
		}
		WIDTH = Math.max(MIN_WIDTH, MARGIN *6 + (data.size() - (isFull ? (int) data.stream().filter(d->d.color != GraphData.DEFAULT_COLOR).count() : 0))*(CULOMN_WIDTH + 10));
		legend = new JTextArea();
		this.add(legend);
		SpringLayout springLayout1 = new SpringLayout();
		this.setLayout(springLayout1);
		springLayout1.putConstraint(SpringLayout.WEST, legend, MARGIN + 15, SpringLayout.WEST, this);
		springLayout1.putConstraint(SpringLayout.NORTH, legend, (int) (MARGIN + 100*CONVERSION) + (isFull ? 45 : 10), SpringLayout.NORTH, this);
		legend.setPreferredSize(new Dimension(WIDTH - (MARGIN +15), LEGEND_HEIGHT));
		legend.setLineWrap(true);
		legend.setWrapStyleWord(true);
		if (isFull) {
			legend.setText(" " + legendData.stream().map(d -> d.text).collect(Collectors.joining("\n ")));
		}else {
			legend.setText(" - " + data.stream().map(d -> d.text).collect(Collectors.joining("\n - ")));
		}
		this.setPreferredSize(new Dimension(WIDTH, HEIGHT + 10));
	}
	
	 @Override
     public void paintComponent(Graphics g) {
        super.paintComponent(g);     
        setBackground(Color.WHITE);

        g.setColor(Color.BLACK);
        g.drawString("100%", MARGIN, MARGIN);
        g.drawString("50%", MARGIN, (int) (MARGIN + 50*CONVERSION));
        g.drawString("0%", MARGIN, (int) (MARGIN + 100*CONVERSION));
        
        g.drawLine(MARGIN *5, MARGIN, MARGIN *5, (int) (MARGIN + 100*CONVERSION));
        g.drawLine(MARGIN *5, (int) (MARGIN + 100*CONVERSION), WIDTH, (int) (MARGIN + 100*CONVERSION));
        
        int index = 0;
        for( int i = 0; i < this.data.size() ; i++){
        	GraphData D = this.data.get(i);
        	g.setColor(D.color);
        	int culomnWidth = CULOMN_WIDTH;
        	if(WIDTH == MIN_WIDTH) {
        		culomnWidth = (WIDTH - MARGIN *5)/(data.size() - (isFull ? (int) data.stream().filter(d->d.color != GraphData.DEFAULT_COLOR).count() : 0)) - 10;
        	}
        	int x = MARGIN *5 + index * (culomnWidth + 10) + 5;
        	int y = (int) (MARGIN + 100 *CONVERSION);
        	int height = (int) (D.value * CONVERSION);
        	g.fillRect(x + Math.max((culomnWidth- MAX_COLUMN_WIDTH)/2, 0), y - height, Math.min(culomnWidth, MAX_COLUMN_WIDTH), height);
        	if (isFull && (i+1)< this.data.size() && this.data.get(i+1).color != GraphData.DEFAULT_COLOR) {
        		i++;
        		D = this.data.get(i);
        		g.setColor(D.color);
        		y -= height;
        		height = (int) (D.value * CONVERSION);
            	g.fillRect(x + Math.max((culomnWidth- MAX_COLUMN_WIDTH)/2, 0), y - height, Math.min(culomnWidth, MAX_COLUMN_WIDTH), height);
        	}
        	if(isFull) {
        		g.setColor(Color.BLACK);
        		x = MARGIN *5 + index * (culomnWidth + 10);
            	y = (int) (MARGIN + 100 *CONVERSION) + 12;
            	String[] lines = D.text.split("\\|");
            	for(String s : lines) {
            		g.drawString(s, x, y);
            		y+=12;
            	}
            	
        	}
        	index++;
        }
        if(isFull) {
	        Point point = this.legend.getLocation();
	    	int x = point.x - 15;
	    	int y = point.y;
	    	for (Legend L : this.legendData)
	    	{
	    		g.setColor(L.color);
	    		g.fillRect(x, y, 12, 12);
	    		y += 15;
	    	}
        }
     }
	 
	 public int getWidth() {
		 return (int) this.getPreferredSize().getWidth();
	 }

}
