package loggerForms.loggeraudio;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.sound.sampled.Mixer;
import javax.sound.sampled.Mixer.Info;

import Acquisition.SoundCardSystem;

public class LoggerAudioSettings implements Cloneable, Serializable {

	public static final long serialVersionUID = 1L;
	
	private Map<String, PlatformSettings> platformAudioSettings = new TreeMap<String, PlatformSettings>();
	
	private Map<String, Boolean> talkGroupSelection = new HashMap<String, Boolean>();
	
	private int outputDeviceIndex = 0;
	public String outputDeviceName = null;
	
	private int inputDeviceIndex = 0;
	public String inputDeviceName = null;
	
	public int bufferSeconds = 10; // time before button press. 
	public int recordSeconds = 60; // time after button press
	
	public String outputFolder = null;
	public boolean outputSubFolders = true;
	
	public static final String NOTALK = "No talkback";
	
	public PlatformSettings getStreamSettings(String senderName) {
		if (senderName == null) {
			return null;
		}

		senderName = senderName.strip();
		PlatformSettings ss = platformAudioSettings.get(senderName);
		if (ss == null) {
			ss = new PlatformSettings(senderName);
			platformAudioSettings.put(senderName, ss);
		}
		
		return ss;
	}
	
	/**
	 * Find a mixer, ideally based on it's name, if not on the last index. 
	 * @return try really hard to return something !
	 */
	public Info findOutputMixer() {
		ArrayList<Info> mixers = SoundCardSystem.getOutputMixerList();
		if (mixers == null || mixers.size() == 0) {
			return null;
		}
		int ind = Math.min(outputDeviceIndex, mixers.size()-1);
		if (outputDeviceName == null) {
			return mixers.get(ind);
		}
		int i = 0;
		for (Info mi : mixers) {
			if (mi.getName().equals(outputDeviceName)) {
				outputDeviceIndex = i;
				return mi;
			}
			i++;
		}
		return mixers.get(ind);
	}
	
	/**
	 * Find a mixer, ideally based on it's name, if not on the last index. 
	 * @return try really hard to return something !
	 */
	public Info findInputMixer() {
		ArrayList<Info> mixers = SoundCardSystem.getInputMixerList();
		if (mixers == null || mixers.size() == 0) {
			return null;
		}
		int ind = Math.min(inputDeviceIndex, mixers.size()-1);
		if (inputDeviceName == null) {
			return mixers.get(ind);
		}
		int i = 0;
		for (Info mi : mixers) {
			if (mi.getName().equals(inputDeviceName)) {
				inputDeviceIndex = i;
				return mi;
			}
			i++;
		}
		return mixers.get(ind);
	}
	
	/**
	 * Get all the current platform names. 
	 * @return set of current platform names. 
	 */
	public Set<String> getPlatformNames() {
		return platformAudioSettings.keySet();
	}

	public void clearDevices() {
		platformAudioSettings.clear();
		
	}
	
	/**
	 * Get a list of unique non null talk groups - groups that the DR might talk back to
	 * @return
	 */
	public Set<String> getTalkGroups() {
		Set<String> names = getPlatformNames();
		Set<String> groups = new HashSet<String>();
		for (String name : names) {
			PlatformSettings platSet = getStreamSettings(name);
			String tg = platSet.talkGroup;
			if (tg != null) {
				groups.add(tg);
			}
		}
		return groups;
	}
	
	/**
	 * See if a talk group is selected. 
	 * @param groupName talk group name
	 * @return
	 */
	public boolean isTalkGroup(String groupName) {
		if (groupName == null) {
			return false;
		}
		if (talkGroupSelection == null) {
			talkGroupSelection = new HashMap<String, Boolean>();
		}
		Boolean sel = talkGroupSelection.get(groupName);
		return sel != null ? sel : false;
	}
	
	/**
	 * Set if a talk group is selected. 
	 * @param groupName
	 * @param isTalk
	 */
	public void setTalkGroup(String groupName, boolean isTalk) {
		if (talkGroupSelection == null) {
			talkGroupSelection = new HashMap<String, Boolean>();
		}
		talkGroupSelection.put(groupName, isTalk);
	}
	
	/**
	 * See if a platform should be talked back to
	 * @param platformSettings
	 * @return
	 */
	public boolean isTalkPlatform(PlatformSettings platformSettings) {
		if (platformSettings == null) {
			return false;
		}
		return isTalkGroup(platformSettings.talkGroup);
	}
	
	/**
	 * See if named platform should be talked back to
	 * @param platformName
	 * @return
	 */
	public boolean isTalkPlatform(String platformName) {
		return isTalkPlatform(getStreamSettings(platformName));
	}

}
