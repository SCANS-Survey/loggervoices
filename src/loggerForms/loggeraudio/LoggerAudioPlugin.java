package loggerForms.loggeraudio;

import PamController.PamguardVersionInfo;
import PamModel.PamDependency;
import PamModel.PamPluginInterface;

public class LoggerAudioPlugin implements PamPluginInterface {

	private String jarFile;

	@Override
	public String getDefaultName() {
		return "Logger Voices";
	}

	@Override
	public String getHelpSetName() {
//		"src/loggerForms/loggeraudio/help/LoggerVoices.hs"
		return "loggerForms/loggeraudio/help/LoggerVoices.hs";
	}

	@Override
	public void setJarFile(String jarFile) {
		this.jarFile = jarFile;
	}

	@Override
	public String getJarFile() {
		return jarFile;
	}

	@Override
	public String getDeveloperName() {
		return "Doug Gillespie";
	}

	@Override
	public String getContactEmail() {
		return "pamguard@pamguard.org";
	}

	@Override
	public String getVersion() {
		return "0.0";
	}

	@Override
	public String getPamVerDevelopedOn() {
		return "2.02.19";
	}

	@Override
	public String getPamVerTestedOn() {
		return "2.02.19";
	}

	@Override
	public String getAboutText() {
		return "Capture audio from oververs using the SCANS mobile app";
	}

	@Override
	public String getClassName() {
		return LoggerAudioControl.class.getName();
	}

	@Override
	public String getDescription() {
		return "Capture audio from oververs using the SCANS mobile app";
	}

	@Override
	public String getMenuGroup() {
		return "Visual Methods";
	}

	@Override
	public String getToolTip() {
		return getDescription();
	}

	@Override
	public PamDependency getDependency() {
		return null;
	}

	@Override
	public int getMinNumber() {
		return 0;
	}

	@Override
	public int getMaxNumber() {
		return 1;
	}

	@Override
	public int getNInstances() {
		return 0;
	}

	@Override
	public boolean isItHidden() {
		return false;
	}

	@Override
	public int allowedModes() {
		return 0;
	}

}
