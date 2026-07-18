package loggerForms.loggeraudio.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import PamView.PamSymbol;
import PamView.PamSymbolType;
import PamView.panel.PamPanel;
import loggerForms.loggeraudio.LoggerAudioControl;
import loggerForms.loggeraudio.LoggerAudioObserver;
import loggerForms.loggeraudio.LoggerAudioProcess;
import loggerForms.loggeraudio.LoggerAudioSettings;
import loggerForms.loggeraudio.PlatformAudio;
import loggerForms.loggeraudio.PlatformSettings;

/**
 * Panel for displaying and controlling logger audio. 
 */
public class LoggerAudioPanel implements LoggerAudioObserver {
	
	public enum PANELSIZE {TINY, BIG};

	private JPanel mainPanel, channelPanelContainer;

	private LoggerAudioControl loggerAudioControl;
	private LoggerAudioProcess loggerAudioProcess;

	private PANELSIZE panelSize;
	
	public LoggerAudioPanel(LoggerAudioControl loggerAudioControl, PANELSIZE size) {
		this.loggerAudioControl = loggerAudioControl;
		this.loggerAudioProcess = loggerAudioControl.getLoggerAudioProcess();
		this.panelSize = size;
		mainPanel = new PamPanel(new BorderLayout());
		channelPanelContainer = new PamPanel();
		channelPanelContainer.setLayout(new BoxLayout(channelPanelContainer, panelSize == PANELSIZE.BIG ? BoxLayout.X_AXIS : BoxLayout.Y_AXIS));
		mainPanel.add(BorderLayout.CENTER, channelPanelContainer);
		
		createChannelPanels();
		
		loggerAudioControl.addAudioObserver(this);
	}
	
	private void createChannelPanels() {
		LoggerAudioSettings settings = loggerAudioControl.getLoggerAudioSettings();
		Set<String> platforms = settings.getPlatformNames();
		for (String platform : platforms) {
			if (findChannelPanel(platform) == null) {
				channelPanelContainer.add(new ChannelPanel(platform));
			}
		}
	}

	/**
	 * Find existing channel panel or return null;
	 * @return
	 */
	private ChannelPanel findChannelPanel(String platformName) {
		Component[] components = channelPanelContainer.getComponents();
		for (int i = 0; i < components.length; i++) {
			Component component = components[i];
			if (component instanceof ChannelPanel == false) {
				continue;
			}
			ChannelPanel cp = (ChannelPanel) component;
			if (cp.platformName.equals(platformName)) {
				return cp;
			}
		}
		return null;
	}
	
	public JComponent getComponent() {
		return mainPanel;
	}
	
	/**
	 * Panel for a single audio channel. 
	 */
	private class ChannelPanel extends PamPanel {
		
		private String platformName;

		private JProgressBar progressBar;
		
		private StateButton stateButton;
		
		private JTextField fileEnd;

		/**
		 * @param platformAudio
		 */
		public ChannelPanel(String platformName) {
			super(new BorderLayout());
			this.platformName = platformName;
			if (panelSize == PANELSIZE.BIG) {
				setBorder(new TitledBorder(platformName));
			}
			else {
				add(new JLabel(platformName), BorderLayout.NORTH);
			}
			JPanel inner = new PamPanel(new BorderLayout());
			this.add(inner, BorderLayout.CENTER);
			progressBar = new JProgressBar(panelSize == PANELSIZE.BIG ? SwingConstants.VERTICAL : SwingConstants.HORIZONTAL, -80, 00);
			progressBar.setStringPainted(true);
			stateButton = new StateButton(platformName, panelSize == PANELSIZE.BIG ? 24 : 16);
			fileEnd = new JTextField(3);
			fileEnd.setEditable(false);
			inner.add(progressBar, BorderLayout.CENTER);
			inner.add(stateButton, panelSize == PANELSIZE.BIG ? BorderLayout.SOUTH : BorderLayout.WEST);
			inner.add(fileEnd, panelSize == PANELSIZE.BIG ? BorderLayout.NORTH : BorderLayout.EAST);

			progressBar.setToolTipText("Right click for gain adjustment control");
			fileEnd.setToolTipText("Seconds remaining before recording ends");
//			stateButton.setToolTipText("Recording state");
			
			progressBar.addMouseListener(new MouseAdapter() {

				@Override
				public void mousePressed(MouseEvent e) {
					if (e.isPopupTrigger()) {
						showMenu(e);
					}
				}

				@Override
				public void mouseReleased(MouseEvent e) {
					if (e.isPopupTrigger()) {
						showMenu(e);
					}
				}

				private void showMenu(MouseEvent e) {
					JMenuItem menuItem = new JMenuItem("Gain control...");
					menuItem.addActionListener(new ActionListener() {
						
						@Override
						public void actionPerformed(ActionEvent e) {
							gainMenuItem();
						}

						private void gainMenuItem() {
							PlatformSettings ps = loggerAudioControl.getLoggerAudioSettings().getStreamSettings(platformName);
							EditVolume.showVolumeSlider(null, getLocationOnScreen(), ps);
						}
					});
					JPopupMenu pop = new JPopupMenu();
					pop.add(menuItem);
					pop.show(e.getComponent(), e.getX(), e.getY());
				}
				
			});
			
		}
		
		
		
		public void update(PlatformAudio platformAudio) {
			stateButton.setRecordng(platformAudio.isRecording());
			double lev = platformAudio.getLevel();
			if (lev <= 0) {
				progressBar.setValue(-90);
			}
			else {
				int lDB = (int) (20.*Math.log10(lev));
				progressBar.setValue(lDB);
				progressBar.setString(String.format("%d dB", lDB));
			}
			if (platformAudio.isRecording()) {
				String unit = panelSize == PANELSIZE.BIG ? " seconds remaining" : "";
				int remain = (int) ((platformAudio.getFileEndTime()-System.currentTimeMillis()+500)/1000);
				fileEnd.setText(String.format("%d%s", remain, unit));
			}
			else {
				fileEnd.setText("Off");
			}
		}

		
	}
	
	/**
	 * Simple icon for record or stopped. 
	 */
	private class StateButton extends JPanel {
		
		private int size;
		
		private PamSymbol record, stop;
		
		private boolean recording = false;

		private String platformName;

		public StateButton(String platformName, int size) {
			this.platformName = platformName;
			this.size = size;
			record = new PamSymbol(PamSymbolType.SYMBOL_CIRCLE, size, size, true, Color.RED, Color.RED);
			stop = new PamSymbol(PamSymbolType.SYMBOL_SQUARE, size-2, size-2, true, Color.BLACK, Color.BLACK);
			addMouseListener(new StateMouse(platformName));
			setToolTip();
		}

		private void setToolTip() {
			setToolTipText(makeToolTip());
		}

		public void setRecordng(boolean recording) {
			if (this.recording != recording) {
				this.recording = recording;
				setToolTip();
				repaint();
			}
		}
		
		private String makeToolTip() {
			int recDur = loggerAudioControl.getLoggerAudioSettings().recordSeconds;
			if (recording == false) {
				return String.format("Click to record for %d seconds", recDur);
			}
			else {
				return String.format("Click to extend recording to %d seconds", recDur);
			}
		}

		@Override
		public Dimension getPreferredSize() {
			return new Dimension(size, size);
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			int h = getHeight();
			int w = getWidth();
			int y = h/2;
			int x = w/2;
			PamSymbol s = recording ? record : stop;
			int border = recording ? 0 : 1;
			int sz = Math.min(h, w)-border*2;
			s.setHeight(sz);
			s.setWidth(sz);
			s.draw(g, new Point(x,y));
		}
		
	}

	private class StateMouse extends MouseAdapter{

		private String platformName;

		public StateMouse(String platformName) {
			this.platformName = platformName;
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			// record for predetermined time !
			PlatformAudio pa = loggerAudioProcess.getPlatformAudio(platformName);
			if (pa != null) {
				pa.makeRecording();
			}
		}
		
	}


	@Override
	public void newPlatform(PlatformAudio platformAudio) {
		createChannelPanels();
	}

	@Override
	public void platformUpdate(PlatformAudio platformAudio) {
		ChannelPanel panel = findChannelPanel(platformAudio.getPlatform());
		if (panel == null) {
			return;
		}
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				panel.update(platformAudio);
			}
		});
	}
}
