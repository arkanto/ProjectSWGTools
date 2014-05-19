package waveTools.msb;

import java.awt.Cursor;
import java.awt.EventQueue;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JMenu;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.swing.JTree;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import javax.swing.text.NumberFormatter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;

import waveTools.msb.secondaryWindows.CreatureTemplateDialog;
import waveTools.msb.secondaryWindows.Settings;
import waveTools.msb.secondaryWindows.WeaponTemplateDialog;

import javax.swing.JScrollPane;

import java.awt.Label;

import javax.swing.DefaultListModel;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JList;
import javax.swing.AbstractListModel;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.JComboBox;
import javax.swing.DefaultComboBoxModel;

import waveTools.msb.resources.Helpers;
import waveTools.msb.resources.Mobile;
import waveTools.msb.resources.Weapon;

import java.awt.Color;

import javax.swing.border.BevelBorder;

import java.awt.Font;

import javax.swing.JFormattedTextField;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.JLabel;

import waveTools.msb.resources.enums.Difficulty;
import waveTools.msb.resources.enums.WeaponType;

import javax.swing.JProgressBar;
import javax.swing.JCheckBox;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.JPopupMenu;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

public class MobileScriptBuilder {

	private JFrame frmPswgToolsMbs;
	private JTree mobilesTree;
	private JTextField tbCreatureName;
	private JTextField tbScriptLocation;
	private JFormattedTextField tbCreatureLevel;
	private DefaultMutableTreeNode rootMobileTreeNode;

	private String coreLocation = "";
	private JComboBox<Difficulty> cmbDifficulty;

	private Mobile activeMobile;
	
	private JButton btnSave;
	private JList<String> listCreatureTemps;
	private JList<String> listWeaponTemps;
	private JFormattedTextField tbAttackRange;

	private JFormattedTextField tbAttackSpeed;
	private JList<String> listAttacks;

	private boolean mobilesLoaded;
	private JComboBox<WeaponType> cmbWeaponType;
	private Properties config;
	private JProgressBar prgMobilesLoading;
	private JComboBox<String> cmbDefaultAttack;
	private JFormattedTextField tbMinLevel;

	private JFormattedTextField tbMaxLevel;

	private JCheckBox chckbxDeathblowEnabled;
	
	private Vector<Mobile> modifiedTemplates = new Vector<Mobile>();
	@SuppressWarnings("rawtypes")
	private DefaultListModel weaponTemps;
	@SuppressWarnings("rawtypes")
	private DefaultListModel creatureTemps;
	private JButton btnBuildAll;
	private JButton btnBuildCurrent;
	
	public static MobileScriptBuilder instance;
	private WeaponTemplateDialog weaponTempDialog;
	private JButton btnRemoveWeapTemp;
	private JButton btnAddNewCreatureTemp;
	private JButton btnAddNewWeapTemp;
	private JButton btnEditWeapTemp;
	private JButton btnRemoveCreatureTemp;
	private JButton btnEditCreatureTemp;
	
	
	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
		} catch (Throwable e) {
			e.printStackTrace();
		}
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					MobileScriptBuilder window = new MobileScriptBuilder();
					window.frmPswgToolsMbs.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	public MobileScriptBuilder() {
		initialize();

		// Perform these after UI setup
		try { createDependencies(); } catch (Exception e) { Helpers.showExceptionError(frmPswgToolsMbs, e.getLocalizedMessage());}

		// Populate Default Attacks combobox
		Vector<String> defaultAttacks = new Vector<String>();
		try(BufferedReader br = new BufferedReader(new FileReader("./defaultAttacks.txt"))) {
			for(String line; (line = br.readLine()) != null; ) {
				if (line.equals("") || line.equals(" "))
					continue;

				defaultAttacks.add(line);
			}
		} catch (Exception e) { Helpers.showExceptionError(frmPswgToolsMbs, e.getLocalizedMessage());}

		cmbDefaultAttack.setModel(new DefaultComboBoxModel<String>(defaultAttacks));

		// Populate Mobiles Tree
		if (coreLocation != null && coreLocation != "" && !mobilesLoaded) { populateMobilesTree(new File(coreLocation + "\\scripts\\mobiles")); }
		
		instance = this;
		
		// Load additional UI's
		weaponTempDialog = new WeaponTemplateDialog();
		
	}

	private void createDependencies() throws Exception {
		// Generate Configuration File
		config = new Properties();
		File configFile = new File("./config.cfg");

		if (!configFile.exists()) {
			Helpers.showMessageBox(frmPswgToolsMbs, "No config file detected. You must setup your settings to generate one (File -> Settings)");
		} else {
			try {
				FileInputStream configInput = new FileInputStream(configFile);
				config.load(configInput);
				configInput.close();

				coreLocation = config.getProperty("CoreLocation");
			} catch (Exception e) { Helpers.showExceptionError(frmPswgToolsMbs, e.getLocalizedMessage()); }
		}

		// Generate defaultAttacks.txt file
		File dAttacksFile = new File("./defaultAttacks.txt");
		if (!dAttacksFile.exists()) {
			dAttacksFile.createNewFile();
			PrintWriter writer = new PrintWriter(dAttacksFile, "UTF-8");
			Vector<String> dAttacks = new Vector<String>(Arrays.asList(new String[] {
					"creatureMeleeAttack", "creatureRangedAttack", "meleeHit", "rangedShot", "saberHit"
			}));

			dAttacks.stream().forEach(a -> writer.println(a));
			writer.close();
		}
	}

	public int fileCount(File folder, int count) {
		for (File file : folder.listFiles()) {
			if (file.isFile())
				count++;
			else
				count = fileCount(file, count);
		}
		return count;
	}

	public Mobile getActiveMobile() {
		return activeMobile;
	}

	public Properties getConfig() {
		return config;
	}

	public String getCoreLocation() {
		return coreLocation;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void initialize() {
		NumberFormatter basicIntFormat = new NumberFormatter();
		//basicIntFormat.setAllowsInvalid(false);
		basicIntFormat.setCommitsOnValidEdit(true);

		NumberFormatter decFormat = new NumberFormatter(new DecimalFormat());
		decFormat.setCommitsOnValidEdit(true);

		frmPswgToolsMbs = new JFrame();
		frmPswgToolsMbs.setResizable(false);
		frmPswgToolsMbs.setTitle("PSWGTools - Mobile Script Builder by Waverunner");
		frmPswgToolsMbs.setBounds(100, 100, 643, 499);
		frmPswgToolsMbs.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		JMenuBar menuBar = new JMenuBar();
		frmPswgToolsMbs.setJMenuBar(menuBar);

		JMenu mnFile = new JMenu("File");
		menuBar.add(mnFile);

		JMenuItem mntmLoadMobiles = new JMenuItem("Load Mobiles...");
		mntmLoadMobiles.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				
				JFileChooser mobilesFolderSelect = new JFileChooser();
				mobilesFolderSelect.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				mobilesFolderSelect.setDialogTitle("Select directory containing mobile scripts");
				if (coreLocation != null && !coreLocation.equals(" ") && !coreLocation.equals(""))
					mobilesFolderSelect.setCurrentDirectory(new File(coreLocation + "\\scripts"));
				
				int success = mobilesFolderSelect.showOpenDialog(frmPswgToolsMbs);

				if (success == JFileChooser.APPROVE_OPTION) {
					if (mobilesFolderSelect.getSelectedFile().getName().equals("mobiles"))
						populateMobilesTree(mobilesFolderSelect.getSelectedFile());
					else
						Helpers.showMessageBox(frmPswgToolsMbs, "Not a valid mobiles folder!");
					//mntmLoadMobiles.setEnabled(false);
				}
			}
		});
		mnFile.add(mntmLoadMobiles);

		JMenuItem mntmSettings = new JMenuItem("Settings..");
		mntmSettings.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				Settings sDialog = new Settings();
				sDialog.setVisible(true);
			}
		});
		mnFile.add(mntmSettings);

		JMenu mnHelp = new JMenu("Help");
		menuBar.add(mnHelp);

		JMenuItem mntmAbout = new JMenuItem("About...");
		mnHelp.add(mntmAbout);

		frmPswgToolsMbs.getContentPane().setLayout(null);

		JScrollPane mobilesScrollPane = new JScrollPane();
		mobilesScrollPane.setBounds(12, 6, 200, 400);
		frmPswgToolsMbs.getContentPane().add(mobilesScrollPane);

		rootMobileTreeNode = new DefaultMutableTreeNode("No mobiles loaded");

		mobilesTree = new JTree(rootMobileTreeNode);
		mobilesTree.addTreeSelectionListener(new TreeSelectionListener() {
			@Override
			public void valueChanged(TreeSelectionEvent e) {
				if (e.getNewLeadSelectionPath() == null || e.getNewLeadSelectionPath().getLastPathComponent() == null)
					return;
				
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.getNewLeadSelectionPath().getLastPathComponent();
				DefaultMutableTreeNode priorNode = null;

				if (e.getOldLeadSelectionPath() != null)
					priorNode = (DefaultMutableTreeNode) e.getOldLeadSelectionPath().getLastPathComponent();

				if (priorNode != null && priorNode.isLeaf() && priorNode.getUserObject() instanceof Mobile) {
					//System.out.println("Prior selected node was " + ((Mobile)priorNode.getUserObject()).getCreatureName());
				}

				if (node != null && node.isLeaf() && node.getUserObject() instanceof Mobile) {
					Mobile mobile = (Mobile) node.getUserObject();
					populateScriptCreator(mobile);
					activeMobile = mobile;
					
					tbAttackRange.setEnabled(true);
					tbCreatureLevel.setEnabled(true);
					tbCreatureName.setEnabled(true);
					tbAttackSpeed.setEnabled(true);
					tbMaxLevel.setEnabled(true);
					tbMinLevel.setEnabled(true);
					cmbDifficulty.setEnabled(true);
					cmbWeaponType.setEnabled(true);
					cmbDefaultAttack.setEnabled(true);
					chckbxDeathblowEnabled.setEnabled(true);
					
					btnAddNewCreatureTemp.setEnabled(true);
					btnAddNewWeapTemp.setEnabled(true);
				}
			}
		});
		mobilesTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

		mobilesScrollPane.setViewportView(mobilesTree);
		
		JPopupMenu popupMenu = new JPopupMenu();
		addPopup(mobilesTree, popupMenu);
		
		JMenuItem mntmRefreshMobiles = new JMenuItem("Refresh Mobiles");
		mntmRefreshMobiles.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (mobilesLoaded)
					populateMobilesTree(new File(coreLocation + "\\scripts\\mobiles"));
			}
		});
		popupMenu.add(mntmRefreshMobiles);

		JPanel buttonsPane = new JPanel();
		buttonsPane.setBounds(224, 400, 401, 36);
		frmPswgToolsMbs.getContentPane().add(buttonsPane);

		JButton btnAddMobile = new JButton("New Mobile");
		btnAddMobile.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				if (!mobilesLoaded) {
					Helpers.showMessageBox(frmPswgToolsMbs, "You cannot do that because you have not loaded the mobiles folder yet!");
					return;
				}
				FileFilter pyFilter = new FileNameExtensionFilter("Mobile Script File", "py");
				JFileChooser newMobileDialog = new JFileChooser();
				newMobileDialog.setFileSelectionMode(JFileChooser.FILES_ONLY);
				newMobileDialog.setDialogTitle("Select where to save the mobile script");
				newMobileDialog.setCurrentDirectory(new File(coreLocation + "\\scripts\\mobiles"));
				newMobileDialog.setDialogType(JFileChooser.SAVE_DIALOG);
				newMobileDialog.setFileFilter(pyFilter);
				int selectedIndex = newMobileDialog.showSaveDialog(frmPswgToolsMbs);
				if (selectedIndex == JFileChooser.APPROVE_OPTION) {
					File file = newMobileDialog.getSelectedFile();
					
					if (!file.getAbsolutePath().endsWith(".py")) {
						file = new File(file.getAbsolutePath() + ".py");
					}
					if (file.exists()) {
						Helpers.showMessageBox(frmPswgToolsMbs, "That script already exists! Please choose a different name!");
						return;
					}
					try {
						file.createNewFile();
						populateMobilesTree(new File(coreLocation + "\\scripts\\mobiles"));
					} catch (IOException e) {
						Helpers.showExceptionError(frmPswgToolsMbs, e.getLocalizedMessage());
					}
				}
				/*DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode) mobilesTree.getLeadSelectionPath().getLastPathComponent();
				
				if (currentNode.getUserObject() instanceof Mobile) {
					Mobile currentMobile = (Mobile) currentNode.getUserObject();

					String path = currentMobile.getScriptLocation();
					newMobile.setScriptLocation(path);
					
					DefaultMutableTreeNode newNode = new DefaultMutableTreeNode();
					newNode.setUserObject(newMobile);
					((DefaultMutableTreeNode)currentNode.getParent()).add(newNode);
					mobilesTree.updateUI();
				}*/
			}
		});
		buttonsPane.add(btnAddMobile);

		btnSave = new JButton("Save");
		btnSave.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				saveCurrentValues();
			}
		});
		btnSave.setEnabled(false);
		buttonsPane.add(btnSave);
		
		btnBuildCurrent = new JButton("Build");
		btnBuildCurrent.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode) mobilesTree.getSelectionPath().getLastPathComponent();
				if (currentNode == null || !(currentNode.getUserObject() instanceof Mobile))
					return;
				
				buildMobileScript((Mobile) currentNode.getUserObject());
				
				Helpers.showMessageBox(frmPswgToolsMbs, "Successfully Built (1) Mobile Script.");
			}
		});
		btnBuildCurrent.setEnabled(false);
		buttonsPane.add(btnBuildCurrent);
		
		btnBuildAll = new JButton("Build All");
		btnBuildAll.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				modifiedTemplates.stream().forEach(mobile -> { buildMobileScript(mobile); });
				Helpers.showMessageBox(frmPswgToolsMbs, "Successfully Built (" + modifiedTemplates.size() + ") Mobile Scripts.");
			}
		});
		btnBuildAll.setEnabled(false);
		buttonsPane.add(btnBuildAll);

		JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.setBounds(224, 6, 391, 392);
		frmPswgToolsMbs.getContentPane().add(tabbedPane);

		JPanel tpGenSettings = new JPanel();
		tpGenSettings.setBorder(new BevelBorder(BevelBorder.RAISED, null, null, null, null));
		tpGenSettings.setForeground(Color.BLACK);
		tabbedPane.addTab("General Settings", null, tpGenSettings, null);
		tpGenSettings.setLayout(null);

		Label label = new Label("Creature Name");
		label.setBounds(10, 55, 90, 23);
		tpGenSettings.add(label);

		Label label_1 = new Label("Script Location");
		label_1.setBounds(10, 18, 90, 23);
		tpGenSettings.add(label_1);

		tbCreatureName = new JTextField();
		tbCreatureName.setEnabled(false);
		tbCreatureName.setFont(new Font("SansSerif", Font.PLAIN, 12));
		tbCreatureName.setBounds(106, 53, 279, 28);
		tpGenSettings.add(tbCreatureName);
		tbCreatureName.setColumns(10);

		tbScriptLocation = new JTextField();
		tbScriptLocation.setFont(new Font("SansSerif", Font.PLAIN, 12));
		tbScriptLocation.setEnabled(false);
		tbScriptLocation.setBounds(106, 13, 279, 28);
		tpGenSettings.add(tbScriptLocation);
		tbScriptLocation.setColumns(10);

		Label label_2 = new Label("Level");
		label_2.setBounds(10, 86, 33, 23);
		tpGenSettings.add(label_2);

		Label label_3 = new Label("Difficulty");
		label_3.setBounds(10, 120, 47, 23);
		tpGenSettings.add(label_3);

		cmbDifficulty = new JComboBox();
		cmbDifficulty.setEnabled(false);
		cmbDifficulty.setModel(new DefaultComboBoxModel(Difficulty.values()));
		cmbDifficulty.setSelectedIndex(0);
		cmbDifficulty.setBounds(59, 118, 74, 26);
		tpGenSettings.add(cmbDifficulty);

		tbCreatureLevel = new JFormattedTextField(basicIntFormat);
		tbCreatureLevel.setEnabled(false);
		tbCreatureLevel.setBounds(49, 84, 33, 28);
		tpGenSettings.add(tbCreatureLevel);

		JLabel lblAttackRange = new JLabel("Attack Range");
		lblAttackRange.setBounds(10, 161, 81, 16);
		tpGenSettings.add(lblAttackRange);

		tbAttackRange = new JFormattedTextField(decFormat);
		tbAttackRange.setEnabled(false);
		tbAttackRange.setBounds(88, 155, 33, 28);
		tpGenSettings.add(tbAttackRange);

		JLabel lblAttackSpeed = new JLabel("Attack Speed");
		lblAttackSpeed.setBounds(133, 161, 74, 16);
		tpGenSettings.add(lblAttackSpeed);

		tbAttackSpeed = new JFormattedTextField(decFormat);
		tbAttackSpeed.setEnabled(false);
		tbAttackSpeed.setBounds(205, 155, 33, 28);
		tpGenSettings.add(tbAttackSpeed);

		JScrollPane scrollPane_Attacks = new JScrollPane();
		scrollPane_Attacks.setBounds(10, 211, 207, 124);
		tpGenSettings.add(scrollPane_Attacks);

		listAttacks = new JList();
		listAttacks.setEnabled(false);
		listAttacks.setModel(new DefaultListModel());
		scrollPane_Attacks.setViewportView(listAttacks);

		JLabel lblAttacks = new JLabel("Attacks:");
		lblAttacks.setEnabled(false);
		lblAttacks.setBounds(10, 189, 55, 16);
		tpGenSettings.add(lblAttacks);

		JButton btnAddAttack = new JButton("Add Attack");
		btnAddAttack.setEnabled(false);
		btnAddAttack.setBounds(220, 221, 114, 28);
		tpGenSettings.add(btnAddAttack);

		JButton btnRemoveAttack = new JButton("Remove Attack");
		btnRemoveAttack.setEnabled(false);
		btnRemoveAttack.setBounds(220, 256, 114, 28);
		tpGenSettings.add(btnRemoveAttack);

		JLabel lblDefaultAttack = new JLabel("Default Attack");
		lblDefaultAttack.setBounds(150, 122, 81, 16);
		tpGenSettings.add(lblDefaultAttack);

		JLabel lblWeaponType = new JLabel("Weapon Type");
		lblWeaponType.setBounds(160, 189, 90, 16);
		tpGenSettings.add(lblWeaponType);

		cmbWeaponType = new JComboBox();
		cmbWeaponType.setEnabled(false);
		cmbWeaponType.setModel(new DefaultComboBoxModel(WeaponType.values()));
		cmbWeaponType.setBounds(244, 184, 141, 26);
		tpGenSettings.add(cmbWeaponType);

		cmbDefaultAttack = new JComboBox();
		cmbDefaultAttack.setEnabled(false);
		cmbDefaultAttack.setEditable(true);
		cmbDefaultAttack.setBounds(230, 117, 155, 26);
		tpGenSettings.add(cmbDefaultAttack);

		tbMinLevel = new JFormattedTextField(basicIntFormat);
		tbMinLevel.setEnabled(false);
		tbMinLevel.setBounds(150, 84, 33, 28);
		tpGenSettings.add(tbMinLevel);

		tbMaxLevel = new JFormattedTextField(basicIntFormat);
		tbMaxLevel.setEnabled(false);
		tbMaxLevel.setBounds(254, 84, 33, 28);
		tpGenSettings.add(tbMaxLevel);

		JLabel lblMinLevel = new JLabel("Min. Level");
		lblMinLevel.setBounds(94, 86, 55, 23);
		tpGenSettings.add(lblMinLevel);

		JLabel lblMaxLevel = new JLabel("Max Level");
		lblMaxLevel.setBounds(195, 86, 55, 23);
		tpGenSettings.add(lblMaxLevel);

		chckbxDeathblowEnabled = new JCheckBox("Deathblow Enabled");
		chckbxDeathblowEnabled.setEnabled(false);
		chckbxDeathblowEnabled.setSelected(true);
		chckbxDeathblowEnabled.setBounds(250, 160, 135, 18);
		tpGenSettings.add(chckbxDeathblowEnabled);

		JPanel tpCreatureTemplates = new JPanel();
		tabbedPane.addTab("Creature Temps", null, tpCreatureTemplates, null);
		tpCreatureTemplates.setLayout(null);

		JScrollPane scrollPane_CreatureTemps = new JScrollPane();
		scrollPane_CreatureTemps.setBounds(10, 10, 375, 234);
		tpCreatureTemplates.add(scrollPane_CreatureTemps);

		creatureTemps = new DefaultListModel();
		listCreatureTemps = new JList();
		listCreatureTemps.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent event) {
				try {
					if (creatureTemps.get(event.getFirstIndex()) != null) {
						btnRemoveCreatureTemp.setEnabled(true);
						btnEditCreatureTemp.setEnabled(true);
					}
				} catch (ArrayIndexOutOfBoundsException e) {
					btnRemoveCreatureTemp.setEnabled(false);
					btnEditCreatureTemp.setEnabled(false);
				}
			}
		});
		listCreatureTemps.setModel(creatureTemps);
		scrollPane_CreatureTemps.setViewportView(listCreatureTemps);
		
		JPanel panelCreatureTempBtns = new JPanel();
		panelCreatureTempBtns.setBounds(6, 255, 379, 42);
		tpCreatureTemplates.add(panelCreatureTempBtns);
		
				btnAddNewCreatureTemp = new JButton("Add");
				panelCreatureTempBtns.add(btnAddNewCreatureTemp);
				btnAddNewCreatureTemp.setEnabled(false);
				
						btnRemoveCreatureTemp = new JButton("Remove");
						panelCreatureTempBtns.add(btnRemoveCreatureTemp);
						btnRemoveCreatureTemp.setEnabled(false);
						
						btnEditCreatureTemp = new JButton("Edit");
						btnEditCreatureTemp.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent arg0) {
								CreatureTemplateDialog dialog = new CreatureTemplateDialog();
								String template = listCreatureTemps.getSelectedValue();
								dialog.setTbCreatureTempNameText(template);
								
								dialog.setVisible(true);
							}
						});
						btnEditCreatureTemp.setEnabled(false);
						panelCreatureTempBtns.add(btnEditCreatureTemp);
						btnRemoveCreatureTemp.addActionListener(new ActionListener() {
							@Override
							public void actionPerformed(ActionEvent arg0) {
								int index = listCreatureTemps.getSelectedIndex();
								creatureTemps.remove(index);
							}
						});
				btnAddNewCreatureTemp.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent arg0) {
						CreatureTemplateDialog dialog = new CreatureTemplateDialog();
						dialog.setVisible(true);
					}
				});

		JPanel tpWeaponTemplates = new JPanel();
		tabbedPane.addTab("Weapon Temps", null, tpWeaponTemplates, null);
		tpWeaponTemplates.setLayout(null);

		JScrollPane scrollPane_WeaponTemps = new JScrollPane();
		scrollPane_WeaponTemps.setBounds(6, 6, 375, 234);
		tpWeaponTemplates.add(scrollPane_WeaponTemps);
		
		weaponTemps = new DefaultListModel();
		listWeaponTemps = new JList();
		listWeaponTemps.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent event) {
				try {
					if (weaponTemps.get(event.getFirstIndex()) != null) {
						btnRemoveWeapTemp.setEnabled(true);
						btnEditWeapTemp.setEnabled(true);
					}
				} catch (ArrayIndexOutOfBoundsException e) {
					btnRemoveWeapTemp.setEnabled(false);
					btnEditWeapTemp.setEnabled(false);
				}
			}
		});
		listWeaponTemps.setModel(weaponTemps);
		scrollPane_WeaponTemps.setViewportView(listWeaponTemps);
		
		JPanel wpTmpBtnsPanel = new JPanel();
		wpTmpBtnsPanel.setBounds(6, 252, 375, 43);
		tpWeaponTemplates.add(wpTmpBtnsPanel);
		
				btnAddNewWeapTemp = new JButton("Add");
				btnAddNewWeapTemp.setEnabled(false);
				wpTmpBtnsPanel.add(btnAddNewWeapTemp);
				
						btnRemoveWeapTemp = new JButton("Remove");
						btnRemoveWeapTemp.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent arg0) {
								int index = listWeaponTemps.getSelectedIndex();
								
								try {
									weaponTemps.remove(index);
								} catch (Exception e) { Helpers.showExceptionError(frmPswgToolsMbs, e.getLocalizedMessage());}
							}
						});
						btnRemoveWeapTemp.setEnabled(false);
						wpTmpBtnsPanel.add(btnRemoveWeapTemp);
						
						btnEditWeapTemp = new JButton("Edit");
						btnEditWeapTemp.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent arg0) {
								String base = listWeaponTemps.getSelectedValue();
								String[] values = base.replace(" ", "").split(",");
								
								weaponTempDialog.getTbWeaponTemp().setText(values[0]);
								weaponTempDialog.getCmbWeaponType().setSelectedIndex(Integer.valueOf(values[1]));
								weaponTempDialog.getTbAttackSpeed().setText(values[2]);
								
								weaponTempDialog.setEditMode(true);
								
								weaponTempDialog.setVisible(true);
							}
						});
						btnEditWeapTemp.setEnabled(false);
						wpTmpBtnsPanel.add(btnEditWeapTemp);
						
						JPanel tpMiscSettings = new JPanel();
						tabbedPane.addTab("Misc. Settings", null, tpMiscSettings, null);
						tabbedPane.setEnabledAt(3, false);
				btnAddNewWeapTemp.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent arg0) {
						weaponTempDialog.getTbWeaponTemp().setText("");
						weaponTempDialog.getCmbWeaponType().setSelectedIndex(0);
						weaponTempDialog.getTbAttackSpeed().setText("");
						
						weaponTempDialog.setVisible(true);
					}
				});

		prgMobilesLoading = new JProgressBar();
		prgMobilesLoading.setBounds(12, 410, 200, 26);
		frmPswgToolsMbs.getContentPane().add(prgMobilesLoading);
	}
	private void populateMobileObject(Mobile baseMobile, File script) {
		try(BufferedReader br = new BufferedReader(new FileReader(script))) {
			for(String line; (line = br.readLine()) != null; ) {
				line = line.replaceAll("\\s", "");
				if (line.equals(""))
					continue;
				else if (line.startsWith("mobileTemplate.setCreatureName")) { baseMobile.setCreatureName(line.replace("mobileTemplate.setCreatureName('", "").replace("')", "")); }
				else if (line.startsWith("mobileTemplate.setLevel")) { baseMobile.setLevel(Integer.valueOf(line.replace("mobileTemplate.setLevel(", "").replace(")", ""))); }
				else if (line.startsWith("mobileTemplate.setMinLevel")) { baseMobile.setMinLevel(Integer.valueOf(line.replace("mobileTemplate.setMinLevel(", "").replace(")", ""))); }
				else if (line.startsWith("mobileTemplate.setMaxLevel")) { baseMobile.setMinLevel(Integer.valueOf(line.replace("mobileTemplate.setMaxLevel(", "").replace(")", ""))); }
				else if (line.startsWith("mobileTemplate.setDifficulty")) { baseMobile.setDifficulty(Integer.valueOf(line.replace("mobileTemplate.setDifficulty(", "").replace(")", ""))); }
				else if (line.startsWith("mobileTemplate.setAttackRange")) { baseMobile.setAttackRange(Integer.valueOf(line.replace("mobileTemplate.setAttackRange(", "").replace(")", ""))); }
				else if (line.startsWith("mobileTemplate.setAttackSpeed")) { baseMobile.setAttackSpeed(Float.valueOf(line.replace("mobileTemplate.setAttackSpeed(", "").replace(")", ""))); }
				else if (line.startsWith("mobileTemplate.setWeaponType")) { baseMobile.setWeaponType(Integer.valueOf(line.replace("mobileTemplate.setWeaponType(", "").replace(")", ""))); }
				// mobileTemplate.setMinSpawnDistance(3)
				// mobileTemplate.setMaxSpawnDistance(5)
				else if (line.startsWith("mobileTemplate.setDeathblow")) { 
					switch(line.replace("mobileTemplate.setDeathblow(", "").replace(")", "")) {
						case "True":
							baseMobile.setDeathblowEnabled(true);
							break;
						case "False":
							baseMobile.setDeathblowEnabled(false);
							break;
					}
				}
				/*
		        mobileTemplate.setSocialGroup('dark jedi')
		        mobileTemplate.setAssistRange(12)*/
				else if (line.startsWith("templates.add")) { baseMobile.addCreatureTemplate(line.replace("templates.add('", "").replace("')", "")); }
				else if (line.startsWith("weapontemplate=WeaponTemplate")) {
					String baseTemp = line.replace("weapontemplate=WeaponTemplate('", "").replace("'", "").replace(")", "");
					String[] values = baseTemp.split(",");
					baseMobile.getWeaponTemplates().add(new Weapon(values[0], Integer.valueOf(values[1]), Float.valueOf(values[2])));
				}
				else if (line.startsWith("attacks.add")) { baseMobile.addAttack(line.replace("attacks.add('", "")); }
				else if (line.startsWith("mobileTemplate.setDefaultAttack('")) { baseMobile.setDefaultAttack(line.replace("mobileTemplate.setDefaultAttack('", "").replace("')", "")); }
			}
		} catch (IOException e) {
			Helpers.showExceptionError(frmPswgToolsMbs, e.getLocalizedMessage());
		}
	}

	// TODO: Progress bar updating
	public void populateMobilesTree(File mobilesDirectory) {
		if (mobilesDirectory.isFile())
			return;
		rootMobileTreeNode.removeAllChildren();

		SwingWorker<DefaultMutableTreeNode, Void> task = new SwingWorker<DefaultMutableTreeNode, Void>() {

			@Override
			public DefaultMutableTreeNode doInBackground() throws Exception {
				DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode();
				DefaultMutableTreeNode node = null;
				rootNode.setUserObject("Mobile Scripts");

				frmPswgToolsMbs.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

				int fileCount = fileCount(mobilesDirectory, 0);
				System.out.println("File count: " + fileCount);
				int currentFileNumber = 0;
				for (File file : mobilesDirectory.listFiles()) {

					if (file.isFile()) {
						Mobile mobile = new Mobile(file.getName().split(".py")[0], file.getPath());
						node = new DefaultMutableTreeNode(mobile);
						rootNode.add(node);
						setProgress(currentFileNumber++);
					} else if (file.isDirectory()) {
						String folderName = file.getName();
						if (folderName.equals("spawnareas") || folderName.equals("lairs") || folderName.equals("lairgroups") || folderName.equals("dynamicgroups"))
							continue;
						node = new DefaultMutableTreeNode(file.getName());
						rootNode.add(node);

						populateSubFolders(node, file);
					}
				}
				return rootNode;
			}
			@Override
			public void done() {
				frmPswgToolsMbs.setCursor(null);

				try {
					mobilesTree.setModel(new DefaultTreeModel(get()));
				} catch (Exception e) {
					e.printStackTrace();
				}
				mobilesTree.updateUI();

				mobilesLoaded = true;
			}
			private void populateSubFolders(DefaultMutableTreeNode parentNode, File directory) {

				DefaultMutableTreeNode node = null;

				for (File file : directory.listFiles()) {

					if (file.isFile()) {
						if (!file.getAbsolutePath().endsWith(".py"))
							continue;

						Mobile mobile = new Mobile(file.getName().replace(".py", ""), file.getPath());
						//System.out.println("Path for mobile is " + mobile.getScriptLocation());
						populateMobileObject(mobile, file);

						node = new DefaultMutableTreeNode(mobile);

						parentNode.add(node);
					} else if (file.isDirectory()) {
						node = new DefaultMutableTreeNode(file.getName());
						parentNode.add(node);

						populateSubFolders(node, file);
					}
				}
			}
		};
		task.execute();
	}

	@SuppressWarnings({ "unchecked", "rawtypes", "serial" })
	private void populateScriptCreator(Mobile mobileTemplate) {
		creatureTemps.clear();
		
		tbCreatureName.setText(mobileTemplate.getCreatureName().split(".py")[0]);
		tbScriptLocation.setText(mobileTemplate.getScriptLocation());
		tbCreatureLevel.setValue(mobileTemplate.getLevel());
		tbMinLevel.setValue(mobileTemplate.getMinLevel());
		tbMaxLevel.setValue(mobileTemplate.getMaxLevel());
		tbAttackRange.setValue(mobileTemplate.getAttackRange());
		tbAttackSpeed.setValue(mobileTemplate.getAttackSpeed());

		cmbDefaultAttack.setSelectedItem(mobileTemplate.getDefaultAttack());
		cmbDifficulty.setSelectedIndex(mobileTemplate.getDifficulty());
		cmbWeaponType.setSelectedIndex(mobileTemplate.getWeaponType());

		chckbxDeathblowEnabled.setSelected(mobileTemplate.isDeathblowEnabled());

		if (mobileTemplate.getCreatureTemplates().size() != 0) {
			for (String s : mobileTemplate.getCreatureTemplates()) { creatureTemps.addElement(s); }
			listCreatureTemps.setModel(creatureTemps);
		}
		else {
			listCreatureTemps.setModel(new AbstractListModel() {
				String[] values = new String[] {"No Creature Templates found."};
				public int getSize() {
					return values.length;
				}
				public Object getElementAt(int index) {
					return values[index];
				}
			});
		}
		
		updateWeaponTemplatesList(mobileTemplate);

		if (mobileTemplate.getAttacks().size() != 0)
			listAttacks.setListData(mobileTemplate.getAttacks());
		else
			listAttacks.setListData(new String[] { "Template has no attacks." });

		//btnSaveAll.setEnabled(true); // TODO: Handle checking of multiple mobiles
		btnSave.setEnabled(true); // TODO: Script compiling
	}
	
	private void buildMobileScript(Mobile mobile) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(mobile.getScriptLocation()));
			writer.write("import sys\n");
			writer.write("from services.spawn import MobileTemplate\n");
			if (mobile.getWeaponTemplates().size() > 0) 
				writer.write("from services.spawn import WeaponTemplate\n");
			writer.write("from java.util import Vector\n");
			writer.newLine();
			
			writer.write("def addTemplate(core):\n");
			writer.write("\tmobileTemplate = MobileTemplate()\n");
			writer.newLine();
			
			writer.write("\tmobileTemplate.setCreatureName('" + mobile.getCreatureName() + "')\n");
			writer.write("\tmobileTemplate.setLevel(" + mobile.getLevel() + ")\n");
			writer.write("\tmobileTemplate.setDifficulty(" + mobile.getDifficulty() + ")\n");
			writer.write("\tmobileTemplate.setAttackRange(" + mobile.getAttackRange() + ")\n");
			writer.write("\tmobileTemplate.setAttackSpeed(" + mobile.getAttackSpeed() + ")\n");
			writer.write("\tmobileTemplate.setWeaponType(" + mobile.getWeaponType() + ")\n");
			//writer.write("\tmobileTemplate.setMinSpawnDistance(" + 1 + ")\n");
			//writer.write("\tmobileTemplate.setMaxSpawnDistance(" + 1 + ")\n");
			writer.write("\tmobileTemplate.setDeathblow(" + (mobile.isDeathblowEnabled() ? "True" : "False") + ")\n");
			//writer.write("\tmobileTemplate.setScale(" + 1 + ")\n");
			//writer.write("\tmobileTemplate.setSocialGroup('" + mobile.getSocialGroup() + "')\n");
			//writer.write("\tmobileTemplate.setAssistRange(" + mobile.getAssistRange() + ")\n");
			//writer.write("\tmobileTemplate.setStalker(" + (mobile.isStalker() ? "True" : "False") + ")\n");
			//writer.write("\tmobileTemplate.setFaction('" + mobile.getFaction() + "')\n");
			//writer.write("\tmobileTemplate.setFactionStatus(" + 1 + ")\n");
			writer.newLine();
			
			writer.write("\ttemplates = new Vector()\n");
			if (mobile.getCreatureTemplates().size() > 0) {
				for (String temp : mobile.getCreatureTemplates()) {
					writer.write("\ttemplates.add('" + temp + "')\n");
				}
			}
			writer.write("\tmobileTemplate.setTemplates(templates)\n");
			writer.newLine();
			
			writer.write("\tweaponTemplates = Vector()\n");
			if (mobile.getWeaponTemplates().size() > 0) {
				for (Weapon weapon : mobile.getWeaponTemplates()) {
					writer.write("\tweaponTemplate = WeaponTemplate('" + weapon.getTemplate() + "', " + weapon.getWeaponType() + ", " 
							+ weapon.getAttackSpeed() +")\n");
					writer.write("\tweaponTemplates.add(weaponTemplate)\n");
				}
			}
			writer.write("\tmobileTemplate.setWeaponTemplateVector(weaponTemplates)\n");
			writer.newLine();
			
			writer.write("\tattacks = new Vector()\n");
			if (mobile.getAttacks().size() > 0) {
				for (String attack : mobile.getAttacks()) {
					writer.write("\tattacks.add('" + attack + "')\n");
				}
			}
			writer.write("\tmobileTemplate.setDefaultAttack('" + mobile.getDefaultAttack() + "')\n");
			writer.write("\tmobileTemplate.setAttacks(attacks)");
			writer.newLine();
			
			writer.write("\tcore.spawnService.addMobileTemplate('" + mobile.getCreatureName() + "', mobileTemplate)\n\treturn");
			
			writer.close();
		} catch (Exception e) {
			Helpers.showExceptionError(frmPswgToolsMbs, e.getLocalizedMessage());
		}
	}
	
	private void saveCurrentValues() {
		Mobile mobileTemplate = (Mobile) ((DefaultMutableTreeNode) mobilesTree.getSelectionPath().getLastPathComponent()).getUserObject();
		System.out.println("Saved values for Template: " + mobileTemplate.getCreatureName());
		
		mobileTemplate.setCreatureName(tbCreatureName.getText());
		mobileTemplate.setScriptLocation(tbScriptLocation.getText());
		
		mobileTemplate.setLevel((int) tbCreatureLevel.getValue());
		mobileTemplate.setMinLevel((int) tbMinLevel.getValue());
		mobileTemplate.setMaxLevel((int) tbMaxLevel.getValue());
		mobileTemplate.setAttackRange((int) tbAttackRange.getValue());
		mobileTemplate.setAttackSpeed((float) tbAttackSpeed.getValue());

		mobileTemplate.setDifficulty(cmbDefaultAttack.getSelectedIndex());
		mobileTemplate.setDefaultAttack((String) cmbDefaultAttack.getSelectedItem()); 
		mobileTemplate.setWeaponType(cmbWeaponType.getSelectedIndex());
		
		mobileTemplate.setDeathblowEnabled(chckbxDeathblowEnabled.isSelected());
		
		if (creatureTemps.getSize() > 0) {
			Object[] objArray = creatureTemps.toArray();
			String[] stringArray = Arrays.copyOf(objArray, objArray.length, String[].class);
			Vector<String> creatureTemplates = new Vector<String>(Arrays.asList(stringArray));
			mobileTemplate.setCreatureTemplates(creatureTemplates);
		} else {
			mobileTemplate.setCreatureTemplates(new Vector<String>());
		}

		if (weaponTemps.size() > 0) {
			Object[] objArray = weaponTemps.toArray();
			String[] stringArray = Arrays.copyOf(objArray, objArray.length, String[].class);
			Vector<Weapon> updatedWeaponTemps = new Vector<Weapon>();
			
			for (String temp : stringArray) {
				String[] values = temp.replace(" ", "").split(",");
				updatedWeaponTemps.add(new Weapon(values[0], Integer.valueOf(values[1]), Float.valueOf(values[2])));
			}
			
			mobileTemplate.setWeaponTemplates(updatedWeaponTemps);
		} else {
			mobileTemplate.setWeaponTemplates(new Vector<Weapon>());
		}

		/*
		if (mobileTemplate.getAttacks().size() != 0)
			listAttacks.setListData(mobileTemplate.getAttacks());
		else
			listAttacks.setListData(new String[] { "Template has no attacks." });*/
		
		mobileTemplate.setDirty(true);
		
		if (!btnBuildCurrent.isEnabled())
			btnBuildCurrent.setEnabled(true);
		
		modifiedTemplates.add(mobileTemplate);
		
		if (modifiedTemplates.size() >= 2 && !btnBuildAll.isEnabled())
			btnBuildAll.setEnabled(true);
		
		mobilesTree.updateUI(); // Show Asterisk 
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes", "serial" })
	public void updateWeaponTemplatesList(Mobile mobileTemplate) {
		weaponTemps.clear();
		
		if (mobileTemplate.getWeaponTemplates().size() != 0) {

			for (Weapon weapon : mobileTemplate.getWeaponTemplates())
				weaponTemps.addElement(weapon.toString());
			
			listWeaponTemps.setModel(weaponTemps);
		}else {
			listWeaponTemps.setModel(new AbstractListModel() {
				String[] values = new String[] {"No Weapon Templates found."};
				public int getSize() {
					return values.length;
				}
				public Object getElementAt(int index) {
					return values[index];
				}
			});
		}
	}
	public void setActiveMobile(Mobile activeMobile) {
		this.activeMobile = activeMobile;
	}

	public void setConfig(Properties config) {
		this.config = config;
	}

	public void setCoreLocation(String coreLocation) {
		this.coreLocation = coreLocation;
	}
	
	public static MobileScriptBuilder getInstance() {
		return instance;
	}

	@SuppressWarnings("unchecked")
	public DefaultListModel<String> getWeaponTemps() {
		return weaponTemps;
	}

	@SuppressWarnings("unchecked")
	public DefaultListModel<String> getCreatureTemps() {
		return creatureTemps;
	}
	
	public void setWeaponTemps(DefaultListModel<String> weaponTemps) {
		this.weaponTemps = weaponTemps;
	}

	public void setCreatureTemps(DefaultListModel<String> creatureTemps) {
		this.creatureTemps = creatureTemps;
	}

	public JList<String> getListWeaponTemps() {
		return listWeaponTemps;
	}

	public void setListWeaponTemps(JList<String> listWeaponTemps) {
		this.listWeaponTemps = listWeaponTemps;
	}
	
	public JList<String> getListCreatureTemps() {
		return listCreatureTemps;
	}

	public JTree getMobilesTree() {
		return mobilesTree;
	}
	private static void addPopup(Component component, final JPopupMenu popup) {
		component.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				if (e.isPopupTrigger()) {
					showMenu(e);
				}
			}
			public void mouseReleased(MouseEvent e) {
				if (e.isPopupTrigger()) {
					showMenu(e);
				}
			}
			private void showMenu(MouseEvent e) {
				popup.show(e.getComponent(), e.getX(), e.getY());
			}
		});
	}
}
