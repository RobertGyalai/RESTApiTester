package implementare;

import javax.swing.JTextArea;

public class Logger {

	public static JTextArea logPanel;
	
	public static void log(String msg) {
		if(Config.DEBUG_INFO_ENABELED) {
			System.out.println(msg);
		}
		if(Config.LOG_ENABELED && logPanel!= null) {
			logPanel.append(msg + "\n");
		}
	}
	
	public static void logDebug(String msg) {
		if(Config.DEBUG_INFO_ENABELED) {
			System.out.println(msg);
		}
	}
}
