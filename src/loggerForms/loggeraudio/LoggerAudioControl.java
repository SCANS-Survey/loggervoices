package loggerForms.loggeraudio;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import javax.swing.JMenuItem;

import PamController.PamControlledUnit;
import PamController.PamControlledUnitSettings;
import PamController.PamController;
import PamController.PamSettingManager;
import PamController.PamSettings;
import PamView.PamSidePanel;
import loggerForms.actions.ActionOwner;
import loggerForms.actions.LoggerActions;
import loggerForms.loggeraudio.swing.AudioDisplayProvider;
import loggerForms.loggeraudio.swing.AudioSidePanel;
import loggerForms.loggeraudio.swing.LoggerAudioDialog;
import loggerForms.network.LoggerNetworkManager;
import loggerForms.network.LoggerNetworkObserver;
import loggerForms.network.LoggerNetworkSettings;
import loggerForms.network.LoggerNetworkSystem;
import userDisplay.UserDisplayControl;

public class LoggerAudioControl extends PamControlledUnit implements LoggerNetworkObserver, PamSettings, ActionOwner, LoggerAudioObserver {
	
	public static final String unitTupe = "Logger Audio";
	
	private LoggerAudioProcess loggerAudioProcess;
	
	private LoggerAudioSettings loggerAudioSettings = new LoggerAudioSettings();
	
	private HashMap<String, LoggerAudioAction> localActionsMap = new HashMap<>();
	
	private ArrayList<LoggerAudioObserver> observers  = new ArrayList();

	private AudioSidePanel sidePanel;;

	public LoggerAudioControl(String unitName) {
		super(unitTupe, unitName);
		loggerAudioProcess = new LoggerAudioProcess(this);
		addPamProcess(loggerAudioProcess);
		
		PamSettingManager.getInstance().registerSettings(this);
		
		checkActionsMap();
		UserDisplayControl.addUserDisplayProvider(new AudioDisplayProvider(this));
	}

	/**
	 * @return the loggerAudioSettings
	 */
	public LoggerAudioSettings getLoggerAudioSettings() {
		return loggerAudioSettings;
	}

	@Override
	public JMenuItem createDetectionMenu(Frame parentFrame) {
		JMenuItem menuItem = new JMenuItem(getUnitName() + " settings ...");
		menuItem.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				showSettingsMenu(parentFrame);
			}
		});
		return menuItem;
	}

	protected void showSettingsMenu(Frame parentFrame) {
		LoggerAudioSettings newSettings = LoggerAudioDialog.showDialog(parentFrame, this, loggerAudioSettings);
		if (newSettings != null) {
			loggerAudioSettings = newSettings;
			loggerAudioProcess.notifyModelChanged(PamController.INITIALIZATION_COMPLETE);
		}
	}
	
	/**
	 *  clears the device settings list, but then immediately recreates it from
	 *  the current connections list.  
	 */	
	public void clearDeviceSettings() {
		loggerAudioSettings.clearDevices();
		Set<String> keys = loggerAudioProcess.getPlatformNames();
		for (String aKey : keys) {
			loggerAudioSettings.getStreamSettings(aKey);
			newPlatform(loggerAudioProcess.getPlatformAudio(aKey));
		}
		
	}

	@Override
	public void updateState(boolean connected, int nClient) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Serializable getSettingsReference() {
		return loggerAudioSettings;
	}

	@Override
	public long getSettingsVersion() {
		return LoggerAudioSettings.serialVersionUID;
	}

	@Override
	public boolean restoreSettings(PamControlledUnitSettings pamControlledUnitSettings) {
		loggerAudioSettings = (LoggerAudioSettings) pamControlledUnitSettings.getSettings();
		return true;
	}
	
	/**
	 * Check all the logger actions are correctly available and registered.
	 * this gets called at startup and after a new platform has been added. 
	 * A bit tricky since the action map won't build until data have been received
	 * at least once, and old actions may get left in, so could get a bit weird.  
	 */
	public void checkActionsMap() {
		Set<String> platNames = loggerAudioSettings.getPlatformNames();
		for (String name : platNames) {
			checkActionsMap(name);
		}
	}
	
	public void checkActionsMap(String name) {

		LoggerAudioAction action = localActionsMap.get(name);
		if (action == null) {
			action = new LoggerAudioAction(this, name);
			localActionsMap.put(name, action);
			LoggerActions loggerActions = LoggerActions.getInstance();
			loggerActions.registerAction(action);
		}
	}

	/**
	 * @return the loggerAudioProcess
	 */
	public LoggerAudioProcess getLoggerAudioProcess() {
		return loggerAudioProcess;
	}
	
	public void addAudioObserver(LoggerAudioObserver loggerAudioObserver) {
		synchronized (observers) {
			observers.add(loggerAudioObserver);
		}
	}

	@Override
	public void newPlatform(PlatformAudio platformAudio) {
		for (LoggerAudioObserver obs : observers) {
			obs.newPlatform(platformAudio);
		}
		
	}

	@Override
	public PamSidePanel getSidePanel() {
		if (sidePanel == null) {
			sidePanel = new AudioSidePanel(this);
		}
		return sidePanel;
	}

	@Override
	public void platformUpdate(PlatformAudio platformAudio) {
		for (LoggerAudioObserver obs : observers) {
			obs.platformUpdate(platformAudio);
		}
	}

	/**
	 * Called when a channel modifies it's recording state. 
	 * @param platformAudio audio handler. 
	 * @param recording currently recording true or false
	 * @param remaining remaining seconds. 
	 */
	public void notifyRecording(PlatformAudio platformAudio, boolean recording, int remaining) {
		String plat = platformAudio.getPlatform();
		LoggerNetworkManager netMan = LoggerNetworkSystem.getManager();
		if (netMan == null) {
			return;
		}
		String data = recording ? Integer.valueOf(remaining).toString() : "-1";
		netMan.sendData("", "LoggerRecording/"+plat, data.getBytes());
	}

	@Override
	public boolean canClose() {
		// check if anything is recording - end all recordings
		loggerAudioProcess.endAllRecordings();
		return true;
	}


}
