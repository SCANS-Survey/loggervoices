package loggerForms.loggeraudio.swing;

import java.awt.Component;

import loggerForms.loggeraudio.LoggerAudioControl;
import loggerForms.loggeraudio.swing.LoggerAudioPanel.PANELSIZE;
import userDisplay.UserDisplayComponent;
import userDisplay.UserDisplayControl;
import userDisplay.UserDisplayProvider;

public class AudioDisplayProvider  implements UserDisplayProvider {
	
	private LoggerAudioControl loggerAudioControl;
	
	public AudioDisplayProvider(LoggerAudioControl loggerAudioControl) {
		this.loggerAudioControl = loggerAudioControl;
	}

	@Override
	public String getName() {
		return "Logger Audio Display";
	}

	@Override
	public UserDisplayComponent getComponent(UserDisplayControl userDisplayControl, String uniqueDisplayName) {
		return new LoggerAudioDisplay();
	}

	@Override
	public Class getComponentClass() {
		return LoggerAudioDisplay.class;
	}

	@Override
	public int getMaxDisplays() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean canCreate() {
		return true;
	}

	@Override
	public void removeDisplay(UserDisplayComponent component) {
		
	}
	
	private class LoggerAudioDisplay implements UserDisplayComponent {

		private String uniqueName;
		
		private LoggerAudioPanel loggerAudioPanel;
		
		public LoggerAudioDisplay() {
			loggerAudioPanel = new LoggerAudioPanel(loggerAudioControl, PANELSIZE.BIG);
		}

		@Override
		public Component getComponent() {
			return loggerAudioPanel.getComponent();
		}

		@Override
		public void openComponent() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void closeComponent() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void notifyModelChanged(int changeType) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public String getUniqueName() {
			return uniqueName;
		}

		@Override
		public void setUniqueName(String uniqueName) {
			this.uniqueName = uniqueName;
			
		}

		@Override
		public String getFrameTitle() {
			return "Logger app audio";
		}
		
	}

}
