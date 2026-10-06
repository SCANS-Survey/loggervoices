package loggerForms.loggeraudio;

import java.io.Serializable;

public class PlatformSettings implements Serializable {

	private String platform;

	private static final long serialVersionUID = 1L;

	/**
	 * Don't mix into output stream
	 */
	public boolean mute;
	
	/**
	 * output stream mixer channel map (0, 1, or 2 for no output, left or right). 
	 */
	public int outputChannel = 0;
	
	/**
	 * Group for talkback, to help control who DR is talking back to. 
	 */
	public String talkGroup;
	
	/**
	 * Gain in decibels
	 */
	public int gainDB = 0;

	public PlatformSettings(String senderName) {
		this.platform = senderName;
		// go for bitmap of channels so that they can be output on both if we want to 
		outputChannel = platform.startsWith("P") ? 1 : 2;
	}
	
}
