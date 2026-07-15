package loggerForms.loggeraudio.swing;

import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.Window;
import java.util.Hashtable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import PamView.PamSlider;
import PamView.dialog.PamDialog;
import PamView.panel.PamPanel;
import loggerForms.loggeraudio.PlatformSettings;

/**
 * Simple popup control to edit the volume of one of the logger audio channels. 
 */
public class EditVolume extends PamDialog {
	
	
	private PlatformSettings platformSettings;
	private Point pt;
	
	private JSlider slider;
	
	private JTextField value;

	private EditVolume(Window parent, Point pt, PlatformSettings platformSettings) {
		super(parent, "Gain dB", false);
		this.pt = pt;
		this.platformSettings = platformSettings;
		getCancelButton().setVisible(false);
		slider = new PamSlider(SwingConstants.VERTICAL);
		slider.setMinimum(-30);
		slider.setMaximum(30);
		slider.setValue(platformSettings.gainDB);
		slider.setPaintLabels(true);
		slider.setPaintTicks(true);
		slider.setPaintTrack(true);
		Hashtable<Integer, JComponent> lables = slider.createStandardLabels(10);
		slider.setLabelTable(lables);
		JPanel mainPanel = new PamPanel(new BorderLayout());
		mainPanel.add(BorderLayout.CENTER, slider);
		value = new JTextField(6);
		value.setEditable(false);
		value.setHorizontalAlignment(JTextField.CENTER);
		mainPanel.add(BorderLayout.SOUTH, value);
		
		slider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				platformSettings.gainDB = slider.getValue();
				setValue();
			}
		});

		setValue();
		setDialogComponent(mainPanel);
	}
	
	protected void setValue() {
		value.setText(String.format("%+d dB", slider.getValue()));
	}

	public static void showVolumeSlider(Window parent, Point pt, PlatformSettings platformSettings) {
		EditVolume ev = new EditVolume(parent, pt, platformSettings);
		ev.setLocation(pt);
		ev.setVisible(true);
	}

	@Override
	public boolean getParams() {
		return true;
	}

	@Override
	public void cancelButtonPressed() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void restoreDefaultSettings() {
		// TODO Auto-generated method stub
		
	}

}
