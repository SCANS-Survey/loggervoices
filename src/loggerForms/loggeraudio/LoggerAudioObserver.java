package loggerForms.loggeraudio;

public interface LoggerAudioObserver {

	/**
	 * A platform added or removed. 
	 * @param platformAudio may be null (if platforms removed)
	 */
	public void newPlatform(PlatformAudio platformAudio);
	
	public void platformUpdate(PlatformAudio platformAudio);
	
	public void configurationUpdate();
	
	/**
	 * broadcast levels from DR voice input
	 * @param level
	 */
	public void drRecordLevel(double level);
	
}
