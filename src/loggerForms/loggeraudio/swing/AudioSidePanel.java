package loggerForms.loggeraudio.swing;

import java.awt.BorderLayout;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import PamView.PamSidePanel;
import PamView.panel.PamPanel;
import loggerForms.loggeraudio.LoggerAudioControl;
import loggerForms.loggeraudio.swing.LoggerAudioPanel.PANELSIZE;

public class AudioSidePanel implements PamSidePanel{
	
	private LoggerAudioPanel audioPanel;
	
	private JPanel outer;

	public AudioSidePanel(LoggerAudioControl loggerAudioControl) {
		outer = new PamPanel(new BorderLayout());
		outer.setBorder(new TitledBorder(loggerAudioControl.getUnitName()));
		audioPanel = new LoggerAudioPanel(loggerAudioControl, PANELSIZE.TINY);
		outer.add(BorderLayout.CENTER, audioPanel.getComponent());
	}

	@Override
	public JComponent getPanel() {
		return outer;
	}

	@Override
	public void rename(String newName) {
		// TODO Auto-generated method stub
		
	}

}
