package loggerForms.loggeraudio.swing;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Set;

import javax.sound.sampled.Mixer.Info;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import Acquisition.SoundCardSystem;
import PamUtils.SelectFolder;
import PamView.dialog.PamDialog;
import PamView.dialog.PamGridBagContraints;
import PamView.panel.PamAlignmentPanel;
import loggerForms.loggeraudio.LoggerAudioControl;
import loggerForms.loggeraudio.LoggerAudioSettings;
import loggerForms.loggeraudio.PlatformSettings;

public class LoggerAudioDialog extends PamDialog {

	private static final long serialVersionUID = 1L;
	
	private static LoggerAudioDialog singleInstance;
	private LoggerAudioSettings audioSettings;
	
	private JComboBox<String> outputCards;
	private ArrayList<Info> outputMixers;
	private JComboBox<String> inputCards;
	private ArrayList<Info> inputMixers;
	
	private SelectFolder outputFolder;
	
	private JTextField bufferSeconds;
	private JTextField recordSeconds;
	
	private LoggerAudioControl loggerAudioControl;
	
	private JPanel channelPanel;
	
	private JLabel[] platformNames;
	
	private JCheckBox[][] platformChannels;
	
	private JTextField[] talkGroup;
	
	private LoggerAudioDialog(Window parentFrame, LoggerAudioControl loggerAudioControl) {
		super(parentFrame, "Logger app audio", false);
		this.loggerAudioControl = loggerAudioControl;
		outputCards = new JComboBox<>();
		outputCards.setToolTipText("Sound card for audio output");
		inputCards = new JComboBox<String>();
		inputCards.setToolTipText("Device to capture Data recorder voice");
		outputFolder = new SelectFolder("Output folder", 30, true);
		bufferSeconds = new JTextField(3);
		recordSeconds = new JTextField(3);
		bufferSeconds.setToolTipText("Time to record before recording is initialised");
		recordSeconds.setToolTipText("Time to record after recording is initialised");
		channelPanel = new JPanel(new GridBagLayout());
		
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		JPanel cardPanel = new JPanel();
		cardPanel.setLayout(new GridBagLayout());
//		cardPanel.setLayout(new BoxLayout(cardPanel, BoxLayout.Y_AXIS));
		GridBagConstraints c = new PamGridBagContraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		cardPanel.setBorder(new TitledBorder("Audio devices"));
		cardPanel.add(new JLabel("Output device (to hear observers)", JLabel.LEFT), c);
		c.gridy++;
		cardPanel.add(outputCards, c);
		c.gridy++;
		cardPanel.add(new JLabel("Input device (to talk to observers)", JLabel.LEFT), c);
		c.gridy++;
		cardPanel.add(inputCards, c);
		mainPanel.add(new PamAlignmentPanel(cardPanel, BorderLayout.WEST, true));
		
		channelPanel.setBorder(new TitledBorder("Channels and talkback"));
		mainPanel.add(new PamAlignmentPanel(channelPanel, BorderLayout.WEST, true));
		
		JPanel pp = new JPanel(new BorderLayout());
		pp.add(outputFolder.getFolderPanel(), BorderLayout.CENTER);
		mainPanel.add(pp);
		pp.setBorder(new TitledBorder("Output folder"));
		
		JPanel dataPanel = new JPanel(new GridBagLayout());
		mainPanel.add(dataPanel);
		dataPanel.setBorder(new TitledBorder("Data options"));
		c = new PamGridBagContraints();
		dataPanel.add(new JLabel("Buffer length ", JLabel.RIGHT), c);
		c.gridx++;
		dataPanel.add(bufferSeconds, c);
		c.gridx++;
		dataPanel.add(new JLabel(" seconds ", JLabel.LEFT), c);
		c.gridx = 0;
		c.gridy++;
		dataPanel.add(new JLabel("Record duration ", JLabel.RIGHT), c);
		c.gridx++;
		dataPanel.add(recordSeconds, c);
		c.gridx++;
		dataPanel.add(new JLabel(" seconds ", JLabel.LEFT), c);
		
		fillCardList();
				
		setDialogComponent(mainPanel);
	}
	
	public static LoggerAudioSettings showDialog(Window parentFrame, LoggerAudioControl loggeraudioControl, LoggerAudioSettings audioSettings) {
//		if (singleInstance == null || singleInstance.getParent() != parentFrame) {
			singleInstance = new LoggerAudioDialog(parentFrame, loggeraudioControl);
//		}
		singleInstance.setParams(audioSettings);
		singleInstance.setVisible(true);
		return singleInstance.audioSettings;
	}
	
	private void fillCardList() {
		outputCards.removeAllItems();
		outputMixers = SoundCardSystem.getOutputMixerList();
		for (int i = 0; i < outputMixers.size(); i++) {
			outputCards.addItem(outputMixers.get(i).getName());
		}
		inputCards.removeAllItems();
		inputMixers = SoundCardSystem.getInputMixerList();
		inputCards.addItem(LoggerAudioSettings.NOTALK);
		for (int i = 0; i < inputMixers.size(); i++) {
			inputCards.addItem(inputMixers.get(i).getName());
		}
	}
	
	private void setParams(LoggerAudioSettings audioSettings) {
		this.audioSettings = audioSettings;
		Info currMix = audioSettings.findOutputMixer();
		for (int i = 0; i < outputMixers.size(); i++) {
			if (outputMixers.get(i).getName().equals(currMix.getName())) {
				outputCards.setSelectedIndex(i);
				break;
			}
		}
		Info ipMix = audioSettings.findInputMixer();
		if (LoggerAudioSettings.NOTALK.equals(audioSettings.inputDeviceName)) {
			inputCards.setSelectedIndex(0);
		}
		else for (int i = 0; i < inputMixers.size(); i++) {
			if (inputMixers.get(i).getName().equals(ipMix.getName())) {
				inputCards.setSelectedIndex(i+1); // add one to allow for notalk. 
				break;
			}
		}
		
		outputFolder.setFolderName(audioSettings.outputFolder);
		outputFolder.setIncludeSubFolders(audioSettings.outputSubFolders);
		
		bufferSeconds.setText(Integer.valueOf(audioSettings.bufferSeconds).toString());
		recordSeconds.setText(Integer.valueOf(audioSettings.recordSeconds).toString());
		
		createPlatformList();
	}

	private void createPlatformList() {
		String[] channelNames = {"Left", "Right"};
		Set<String> platforms = audioSettings.getPlatformNames();
		int i = 0;
		platformNames = new JLabel[platforms.size()];
		platformChannels = new JCheckBox[platforms.size()][2];
		talkGroup = new JTextField[platforms.size()];
		channelPanel.removeAll();
		channelPanel.setLayout(new GridBagLayout());
		GridBagConstraints c = new PamGridBagContraints();
		if (platforms.size() == 0) {
			channelPanel.add(new JLabel("No audio channels defined. Wait for SCANS app to send data"), c);
			return;
		}
		channelPanel.add(new JLabel("Platform", JLabel.RIGHT), c);
		c.gridx++;
		channelPanel.add(new JLabel(" L ", JLabel.CENTER), c);
		c.gridx++;
		channelPanel.add(new JLabel(" R ", JLabel.CENTER), c);
		c.gridx++;
		channelPanel.add(new JLabel(" Talk group", JLabel.LEFT), c);
		for (String platform : platforms) {
			PlatformSettings platSettings = audioSettings.getStreamSettings(platform);
			c.gridx = 0;
			c.gridy++;
			channelPanel.add(platformNames[i] = new JLabel(platform, JLabel.RIGHT), c);
			for (int ch = 0; ch < 2; ch++) {
				c.gridx++;
				channelPanel.add(platformChannels[i][ch] = new JCheckBox(), c);
				platformChannels[i][ch].setSelected((platSettings.outputChannel & 1<<ch) != 0);
				platformChannels[i][ch].setToolTipText(String.format("Play through %s speaker / headphone", channelNames[ch]));
			}
			c.gridx++;
			channelPanel.add(talkGroup[i] = new JTextField(6), c);
			talkGroup[i].setText(platSettings.talkGroup);
			talkGroup[i].setToolTipText("Group for talking back to observers");
			i++;
		}
		pack();
	}

	@Override
	public boolean getParams() {
		int ind = outputCards.getSelectedIndex();
		if (ind < 0) {
			return showWarning("No output sound device selected");
		}
		audioSettings.outputDeviceName = outputMixers.get(ind).getName();
		ind = inputCards.getSelectedIndex();
		if (ind < 0) {
			return showWarning("No input sound device selected");
		}
		audioSettings.inputDeviceName = (String) inputCards.getSelectedItem();
		audioSettings.outputFolder = outputFolder.getFolderName(true);
		audioSettings.outputSubFolders = outputFolder.isIncludeSubFolders();
		try {
			audioSettings.bufferSeconds = Integer.valueOf(bufferSeconds.getText());
			audioSettings.recordSeconds = Integer.valueOf(recordSeconds.getText());
		}
		catch (NumberFormatException e) {
			return showWarning("Invalid recording or buffer seconds (must be integer");
		}
		
		int nPlat = platformNames.length;
		for (int i = 0; i < nPlat; i++) {
			String name = platformNames[i].getText();
			PlatformSettings platSettings = audioSettings.getStreamSettings(name);
			int sel = 0;
			for (int ch = 0; ch < 2; ch++) {
				if (platformChannels[i][ch].isSelected()) {
					sel |= 1<<ch;
				}
			}
			platSettings.outputChannel = sel;
			platSettings.talkGroup = talkGroup[i].getText();
		}
		return true;
	}

	@Override
	public void cancelButtonPressed() {
		audioSettings = null;
	}

	@Override
	public void restoreDefaultSettings() {
		// TODO Auto-generated method stub

	}

}
