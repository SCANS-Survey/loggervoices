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
	
	private JComboBox<String> cardList;
	private ArrayList<Info> mixers;
	
	private SelectFolder outputFolder;
	
	private JTextField bufferSeconds;
	private JTextField recordSeconds;
	
	private LoggerAudioControl loggerAudioControl;
	
	private JPanel channelPanel;
	
	private JLabel[] platformNames;
	
	private JCheckBox[][] platformChannels;
	
	private LoggerAudioDialog(Window parentFrame, LoggerAudioControl loggerAudioControl) {
		super(parentFrame, "Logger app audio", false);
		this.loggerAudioControl = loggerAudioControl;
		cardList = new JComboBox<>();
		cardList.setToolTipText("Sound card for audio output");
		outputFolder = new SelectFolder("Output folder", 30, true);
		bufferSeconds = new JTextField(3);
		recordSeconds = new JTextField(3);
		bufferSeconds.setToolTipText("Time to record before recording is initialised");
		recordSeconds.setToolTipText("Time to record after recording is initialised");
		channelPanel = new JPanel(new GridBagLayout());
		
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		JPanel cardPanel = new JPanel(new BorderLayout());
		mainPanel.add(cardPanel);
		cardPanel.setBorder(new TitledBorder("Audio Output device"));
		cardPanel.add(BorderLayout.NORTH, cardList);
		cardPanel.add(BorderLayout.CENTER, new PamAlignmentPanel(channelPanel, BorderLayout.WEST));
		
		JPanel pp = new JPanel(new BorderLayout());
		pp.add(outputFolder.getFolderPanel(), BorderLayout.CENTER);
		mainPanel.add(pp);
		pp.setBorder(new TitledBorder("Output folder"));
		
		JPanel dataPanel = new JPanel(new GridBagLayout());
		mainPanel.add(dataPanel);
		dataPanel.setBorder(new TitledBorder("Data options"));
		GridBagConstraints c = new PamGridBagContraints();
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
		cardList.removeAllItems();
		mixers = SoundCardSystem.getOutputMixerList();
		for (int i = 0; i < mixers.size(); i++) {
			cardList.addItem(mixers.get(i).getName());
		}
	}
	
	private void setParams(LoggerAudioSettings audioSettings) {
		this.audioSettings =audioSettings;
		Info currMix = audioSettings.findMixer();
		for (int i = 0; i < mixers.size(); i++) {
			if (mixers.get(i).getName().equals(currMix.getName())) {
				cardList.setSelectedIndex(i);
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
		Set<String> platforms = audioSettings.getPlatformNames();
		int i = 0;
		platformNames = new JLabel[platforms.size()];
		platformChannels = new JCheckBox[platforms.size()][2];
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
		for (String platform : platforms) {
			PlatformSettings platSettings = audioSettings.getStreamSettings(platform);
			c.gridx = 0;
			c.gridy++;
			channelPanel.add(platformNames[i] = new JLabel(platform, JLabel.RIGHT), c);
			for (int ch = 0; ch < 2; ch++) {
				c.gridx++;
				channelPanel.add(platformChannels[i][ch] = new JCheckBox(), c);
				platformChannels[i][ch].setSelected((platSettings.outputChannel & 1<<ch) != 0);
			}
			i++;
		}
		pack();
	}

	@Override
	public boolean getParams() {
		int ind = cardList.getSelectedIndex();
		if (ind < 0) {
			return showWarning("No output sound device selected");
		}
		audioSettings.outputDeviceName = mixers.get(ind).getName();
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
