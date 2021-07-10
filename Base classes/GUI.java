package implementare;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.file.Path;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.filechooser.FileSystemView;
import javax.swing.text.DefaultCaret;

import org.json.simple.parser.ParseException;


public class GUI {

	public static JFrame mainWindow;
	private JPanel settingsPanel;
	private JPanel resultsPanel;
	private JPanel logPanel;
	private JPanel settingsPane;
	private JScrollPane resultsPane;
	private JScrollPane logPane;
	private JTabbedPane tabPane;

	private JLabel label1;
	private JRadioButton radioButtonUrl;
	private JTextField urlField; 
	private JRadioButton radioButtonFile;
	private JTextField fileField;
	private JButton browseButton;
	private JFileChooser jfc = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
	
	private JLabel label2;
	private JComboBox comboBoxConfig;
	private JTextArea configDescription;
	
	private JCheckBox checkBoxCustomUrl;
	private JTextField baseUrlField;
	
	private JButton startButton;
	private JTextArea logArea;
	
	private enum CONFIGS{
		CONFIG1("Standard", "Standard", false, true, "Using a minimum number of parameters (only those required) and with no generation of partial tests"),
		CONFIG2("Max Parameter Coverage", "Max|Parameter|Coverage", false, false, "All parameters were used, either required or not required, and with no generation of partial tests"),
		CONFIG3("Partial Test Generation", "Partial|Test|Generation", true, true, "Using a minimum number of parameters (only those required) and with generation of partial tests"),
		CONFIG4("Max Parameter Coverage and Partial Tests", "Max Parameter|Coverage and|Partial Tests", true, false, "Using all parameters (required or not required) and with generation of partial tests"),
		CONFIG5("Run ALL", "", false, true, "Run all the previous configurations in order");
		
		public String config;
		public String configSeparated;
		public String details;
		boolean partailFlag;
		boolean paramFlag;
		
		
		CONFIGS(String config, String configSeparated,boolean partailFlag, boolean paramFlag, String details) {
			this.config = config;
			this.configSeparated = configSeparated;
			this.partailFlag = partailFlag;
			this.paramFlag = paramFlag;
			this.details = details;
		}
		
		public static String getDescriptionAndSetFlags(String config) {
			if(CONFIGS.CONFIG5.config.equalsIgnoreCase(config)) {
				Config.RUN_ALL_CONFIGS = true;
				return CONFIGS.CONFIG5.details;
			}
			for (CONFIGS C : CONFIGS.values()) {
				if (C.config.equalsIgnoreCase(config)) {
					Config.GENERATE_PARTIAL_TESTS = C.partailFlag;
					Config.IGNORE_NON_REQUIRED_PARAMS = C.paramFlag;
					return C.details;
				}
			}
			return "";
		}
	}
	
	public GUI() {
		initialize();
	}
	
	private void initialize() {		
		mainWindow = new JFrame();
		mainWindow.setBounds(100, 100, 500, 500);
		mainWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		mainWindow.getContentPane().setLayout(new BorderLayout());
		mainWindow.setTitle("REST API Tester");
		ImageIcon img = new ImageIcon("C:\\Users\\Robert\\Desktop\\BLANK_ICON.png");
		mainWindow.setIconImage(img.getImage());
		
		settingsPane = new JPanel();
		
		
		settingsPanel = new JPanel();
		settingsPanel.setPreferredSize(new Dimension(400, 450));
		settingsPane.add(settingsPanel);
		resultsPanel = new JPanel();
		resultsPanel.setMaximumSize(new Dimension(700, 7000));
		resultsPanel.setPreferredSize(new Dimension(450, 1500));
		resultsPane = new JScrollPane(resultsPanel);
		logPanel = new JPanel();
		logPanel.setLayout(new BorderLayout());
		logPane = new JScrollPane(logPanel);		
		tabPane = new JTabbedPane();

		tabPane.add("Settings", settingsPane);
		tabPane.add("Results", resultsPane);
		tabPane.add("Log", logPane);

		SpringLayout springLayout1 = new SpringLayout();
		settingsPanel.setLayout(springLayout1);
		label1 = new JLabel("Select an API Specificatin (OpenAPI 3.X)");
		radioButtonUrl = new JRadioButton("From URL:");
		urlField = new JTextField(18);
		radioButtonFile = new JRadioButton("From local file:");
		fileField = new JTextField(15);
		browseButton = new JButton("Browse");
		
		ButtonGroup group = new ButtonGroup();
	    group.add(radioButtonUrl);
	    group.add(radioButtonFile);
	    
	    ActionListener sourceSelectAction = new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				if (e.getSource() == radioButtonUrl) {
					urlField.setEnabled(true);
					fileField.setEnabled(false);
					browseButton.setEnabled(false);
				}
				else if (e.getSource() == radioButtonFile) {
					urlField.setEnabled(false);
					fileField.setEnabled(true);
					browseButton.setEnabled(true);
				}
				
			}
		};

	    radioButtonUrl.addActionListener(sourceSelectAction);
	    radioButtonFile.addActionListener(sourceSelectAction);
	    
		radioButtonUrl.setSelected(true);
		sourceSelectAction.actionPerformed(new ActionEvent(radioButtonUrl, 0, null));
	    
		browseButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
		        

		        int returnValue = jfc.showOpenDialog(null);

		        if (returnValue == JFileChooser.APPROVE_OPTION) {
		        	fileField.setText(jfc.getSelectedFile().getAbsolutePath());
		        }
			}
		});
		
		
	    settingsPanel.add(label1);
		settingsPanel.add(radioButtonUrl);
		settingsPanel.add(urlField);
		settingsPanel.add(radioButtonFile);
		settingsPanel.add(fileField);
		settingsPanel.add(browseButton);
		
		springLayout1.putConstraint(SpringLayout.WEST, label1, 5, SpringLayout.WEST, settingsPanel);
		springLayout1.putConstraint(SpringLayout.NORTH, label1, 10, SpringLayout.NORTH, settingsPanel);
		springLayout1.putConstraint(SpringLayout.WEST, radioButtonUrl, 5, SpringLayout.WEST, settingsPanel);
		springLayout1.putConstraint(SpringLayout.NORTH, radioButtonUrl, 10, SpringLayout.SOUTH, label1);

		springLayout1.putConstraint(SpringLayout.WEST, urlField, 10, SpringLayout.EAST, radioButtonUrl);
		springLayout1.putConstraint(SpringLayout.NORTH, urlField, 2, SpringLayout.NORTH, radioButtonUrl);

		springLayout1.putConstraint(SpringLayout.WEST, radioButtonFile, 5, SpringLayout.WEST, settingsPanel);
		springLayout1.putConstraint(SpringLayout.NORTH, radioButtonFile, 10, SpringLayout.SOUTH, radioButtonUrl);

		springLayout1.putConstraint(SpringLayout.WEST, fileField, 5, SpringLayout.EAST, radioButtonFile);
		springLayout1.putConstraint(SpringLayout.NORTH, fileField, 2, SpringLayout.NORTH, radioButtonFile);

		springLayout1.putConstraint(SpringLayout.WEST, browseButton, 5, SpringLayout.EAST, fileField);
		springLayout1.putConstraint(SpringLayout.NORTH, browseButton, 0, SpringLayout.NORTH, radioButtonFile);
				
		label2 = new JLabel("Select configuration:");
		comboBoxConfig = new JComboBox<String>();
		comboBoxConfig.addItem(CONFIGS.CONFIG1.config);
		comboBoxConfig.addItem(CONFIGS.CONFIG2.config);
		comboBoxConfig.addItem(CONFIGS.CONFIG3.config);
		comboBoxConfig.addItem(CONFIGS.CONFIG4.config);
		comboBoxConfig.addItem(CONFIGS.CONFIG5.config);
		configDescription = new JTextArea();
		configDescription.setLineWrap(true);
		configDescription.setWrapStyleWord(true);
		configDescription.setColumns(30);
		
		
		ActionListener configListener = new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				Config.RUN_ALL_CONFIGS = false;
				configDescription.setText(CONFIGS.getDescriptionAndSetFlags((String) comboBoxConfig.getSelectedItem()));
			}
		};
		comboBoxConfig.addActionListener(configListener);
		configListener.actionPerformed(new ActionEvent(comboBoxConfig, 0, null));
		
		settingsPanel.add(label2);
		settingsPanel.add(comboBoxConfig);
		settingsPanel.add(configDescription);
		
		springLayout1.putConstraint(SpringLayout.WEST, label2, 5 , SpringLayout.WEST, settingsPanel);
		springLayout1.putConstraint(SpringLayout.NORTH, label2, 25 , SpringLayout.SOUTH, radioButtonFile);
		
		springLayout1.putConstraint(SpringLayout.WEST, comboBoxConfig, 5 , SpringLayout.WEST, settingsPanel);
		springLayout1.putConstraint(SpringLayout.NORTH, comboBoxConfig, 5 , SpringLayout.SOUTH, label2);
		springLayout1.putConstraint(SpringLayout.WEST, configDescription, 5 , SpringLayout.WEST, settingsPanel);
		springLayout1.putConstraint(SpringLayout.NORTH, configDescription, 5 , SpringLayout.SOUTH, comboBoxConfig);
				
		checkBoxCustomUrl = new JCheckBox("Use custom base url for API requests");
		baseUrlField = new JTextField(25);
		
		ActionListener customUrlAction = new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				baseUrlField.setEnabled(checkBoxCustomUrl.isSelected());
			}
		};

		checkBoxCustomUrl.addActionListener(customUrlAction);
		customUrlAction.actionPerformed(new ActionEvent(checkBoxCustomUrl, 0, null));
		
		settingsPanel.add(checkBoxCustomUrl);
		settingsPanel.add(baseUrlField);
		
		springLayout1.putConstraint(SpringLayout.WEST, checkBoxCustomUrl, 5 , SpringLayout.WEST, settingsPanel);
		springLayout1.putConstraint(SpringLayout.NORTH, checkBoxCustomUrl, 25 , SpringLayout.SOUTH, configDescription);
		
		springLayout1.putConstraint(SpringLayout.WEST, baseUrlField, 5 , SpringLayout.WEST, settingsPanel);
		springLayout1.putConstraint(SpringLayout.NORTH, baseUrlField, 5 , SpringLayout.SOUTH, checkBoxCustomUrl);
				
		startButton = new JButton("Start");
		
		startButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				ParameterExampleCache.clear();
				if (Config.RUN_ALL_CONFIGS == true) {
					executeAllConfigs();
				} else {
					executeConfig();
				}
			}
		});
		
		settingsPanel.add(startButton);
		springLayout1.putConstraint(SpringLayout.WEST, startButton, 25 , SpringLayout.WEST, settingsPanel);
		springLayout1.putConstraint(SpringLayout.NORTH, startButton, 25 , SpringLayout.SOUTH, baseUrlField);
				
		logArea = new JTextArea();
		logArea.setMaximumSize(new Dimension(350, 1000));
		logArea.setLineWrap(true);
		logArea.setWrapStyleWord(true);
		DefaultCaret caret = (DefaultCaret)logArea.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		
		logPanel.add(logArea, BorderLayout.CENTER);
				
		mainWindow.add(tabPane, BorderLayout.CENTER);
		mainWindow.setVisible(true);
		Logger.logPanel = logArea;
		Statistics.displayPanel = resultsPanel;
		Statistics.initialize();
		Config.LOG_ENABELED = true;
	}
	
	private void executeConfig() {
		if (checkBoxCustomUrl.isSelected() && !baseUrlField.getText().isBlank()) {
			Config.MANUAL_BASE_URL = baseUrlField.getText();
		}
		logArea.setText("");
		Statistics.resetBaseStatistics();
		try {
			Thread thread = null;
			if (radioButtonUrl.isSelected()) {
				 thread = new Thread(new Runnable() {
					
					@Override
					public void run() {
						try {
							new ApiHandler().handleApiFromUrl(urlField.getText());
						} catch (ParseException e) {
							e.printStackTrace();
						}
					}
				});
			}
			else if (radioButtonFile.isSelected()) {
				thread = new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							new ApiHandler().handleApiFromFile(jfc.getSelectedFile().toPath());
						} catch (IOException | ParseException e) {
							e.printStackTrace();
						}
					}
				});
				
			}
			tabPane.setSelectedIndex(2);
			thread.start();
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}
	
	private void executeAllConfigs() {
		if (checkBoxCustomUrl.isSelected() && !baseUrlField.getText().isBlank()) {
			Config.MANUAL_BASE_URL = baseUrlField.getText();
		}
		logArea.setText("");
		Statistics.resetBaseStatistics(true);
		try {
			Thread thread = null;
			if (radioButtonUrl.isSelected()) {
				 thread = new Thread(new Runnable() {
					
					@Override
					public void run() {
						try {
							String url = urlField.getText();
							logArea.append("\n====== Running CONFIG1: "+ CONFIGS.CONFIG1.config +" ======\n\n");
							CONFIGS.getDescriptionAndSetFlags(CONFIGS.CONFIG1.config);
							Config.CURRENT_CONFIG = CONFIGS.CONFIG1.configSeparated;
							new ApiHandler().handleApiFromUrl(url);
							Statistics.resetBaseStatistics();
							logArea.append("\n====== Running CONFIG2: "+ CONFIGS.CONFIG2.config +" ======\n\n");
							CONFIGS.getDescriptionAndSetFlags(CONFIGS.CONFIG2.config);
							Config.CURRENT_CONFIG = CONFIGS.CONFIG2.configSeparated;
							new ApiHandler().handleApiFromUrl(url);
							Statistics.resetBaseStatistics();
							logArea.append("\n====== Running CONFIG3: "+ CONFIGS.CONFIG3.config +" ======\n\n");
							CONFIGS.getDescriptionAndSetFlags(CONFIGS.CONFIG3.config);
							Config.CURRENT_CONFIG = CONFIGS.CONFIG3.configSeparated;
							new ApiHandler().handleApiFromUrl(url);
							Statistics.resetBaseStatistics();
							logArea.append("\n====== Running CONFIG4: "+ CONFIGS.CONFIG4.config +" ======\n\n");
							CONFIGS.getDescriptionAndSetFlags(CONFIGS.CONFIG4.config);
							Config.CURRENT_CONFIG = CONFIGS.CONFIG4.configSeparated;
							new ApiHandler().handleApiFromUrl(url);
						} catch (ParseException e) {
							e.printStackTrace();
						}
					}
				});
			}
			else if (radioButtonFile.isSelected()) {
				thread = new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							Path path = jfc.getSelectedFile().toPath();
							logArea.append("\n====== Running CONFIG1: "+ CONFIGS.CONFIG1.config +" ======\n\n");
							CONFIGS.getDescriptionAndSetFlags(CONFIGS.CONFIG1.config);
							Config.CURRENT_CONFIG = CONFIGS.CONFIG1.configSeparated;
							new ApiHandler().handleApiFromFile(path);
							Statistics.resetBaseStatistics();
							logArea.append("\n====== Running CONFIG2: "+ CONFIGS.CONFIG2.config +" ======\n\n");
							CONFIGS.getDescriptionAndSetFlags(CONFIGS.CONFIG2.config);
							Config.CURRENT_CONFIG = CONFIGS.CONFIG2.configSeparated;
							new ApiHandler().handleApiFromFile(path);
							Statistics.resetBaseStatistics();
							logArea.append("\n====== Running CONFIG3: "+ CONFIGS.CONFIG3.config +" ======\n\n");
							CONFIGS.getDescriptionAndSetFlags(CONFIGS.CONFIG3.config);
							Config.CURRENT_CONFIG = CONFIGS.CONFIG3.configSeparated;
							new ApiHandler().handleApiFromFile(path);
							Statistics.resetBaseStatistics();
							logArea.append("\n====== Running CONFIG4: "+ CONFIGS.CONFIG4.config +" ======\n\n");
							CONFIGS.getDescriptionAndSetFlags(CONFIGS.CONFIG4.config);
							Config.CURRENT_CONFIG = CONFIGS.CONFIG4.configSeparated;
							new ApiHandler().handleApiFromFile(path);
						} catch (IOException | ParseException e) {
							e.printStackTrace();
						}
					}
				});
				
			}
			tabPane.setSelectedIndex(2);
			thread.start();
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}
	
}
