package loggerForms.loggeraudio;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.LineEvent.Type;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import Acquisition.SoundCardSystem;
import Filters.ButterworthMethod;
import Filters.Filter;
import Filters.FilterBand;
import Filters.FilterParams;
import Filters.FilterType;
import Filters.IIRFilterMethod;

import javax.sound.sampled.Mixer.Info;

import PamController.PamController;
import PamDetection.RawDataUnit;
import PamUtils.PamCalendar;
import PamUtils.PamUtils;
import PamguardMVC.PamProcess;
import PamguardMVC.PamRawDataBlock;
import dataPlots.data.DataLineInfo;
import loggerForms.loggeraudio.logging.LoggerAudioDataBlock;
import loggerForms.loggeraudio.logging.LoggerAudioLogging;
import loggerForms.network.LoggerNetworkManager;
import loggerForms.network.LoggerNetworkMessage;
import loggerForms.network.LoggerNetworkReceiver;
import loggerForms.network.LoggerNetworkSystem;
import wavFiles.ByteConverter;

public class LoggerAudioProcess extends PamProcess {

	private LoggerAudioControl loggerAudioControl;

	/**
	 * @return the loggerAudioControl
	 */
	public LoggerAudioControl getLoggerAudioControl() {
		return loggerAudioControl;
	}

	private boolean listening = false;

	public static final int appSampleRate = 8000;
	
	public static final int appBitDepth = 16;

	static private final String listenTopic = "Logger/AudioData/#";

	private Map<String, PlatformAudio> platformAudios;

	private AudioFormat outputFormat = new AudioFormat(appSampleRate, appBitDepth, 2, true, true);

	private ByteConverter inputByteConverter = ByteConverter.createByteConverter(2, false, Encoding.PCM_SIGNED);
	private ByteConverter outputByteConverter = ByteConverter.createByteConverter(2, true, Encoding.PCM_SIGNED);
	private ByteConverter drByteConverter;

	private int lineBuffSize;

	private boolean running;

	private Mixer inputMixer; // miser for recording DR voice to send to SCANSAPPs
	private Mixer outputMixer; // mixer for playing input from SCANSAPP

	private Timer queueTimer; 

	private LoggerAudioDataBlock audioDataBlock;

	private TargetDataLine inputDataLine;
	
	

	private volatile boolean acquire;

	private Filter drVoiceFilter;


	public LoggerAudioProcess(LoggerAudioControl loggerAudioControl) {
		super(loggerAudioControl, null);
		this.loggerAudioControl = loggerAudioControl;
		platformAudios = new TreeMap<>(); 
		audioDataBlock = new LoggerAudioDataBlock(this);
		audioDataBlock.SetLogging(new LoggerAudioLogging(audioDataBlock));
		addOutputDataBlock(audioDataBlock);

		queueTimer = new Timer(50, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				queueTimerAction();
			}
		});
		if (loggerAudioControl.isViewer() == false) {
			queueTimer.start();
		}
	}


	@Override
	public void notifyModelChanged(int changeType) {
		super.notifyModelChanged(changeType);
		if (changeType == PamController.INITIALIZATION_COMPLETE) {
			prepareOutput();
			setupListener();
			prepareInput();
		}
	}


	/**
	 * Prepare input from the logger computer sound card to send out to the SCANSAPP observers. 
	 */
	private boolean prepareInput() {
		closeInput();
		if (LoggerAudioSettings.NOTALK.equals(loggerAudioControl.getLoggerAudioSettings().inputDeviceName)) {
			System.out.println("No voice input from data recorder");
			return false;
		}
		
		// prepare a filter for the data - options to follow
		FilterParams fp = new FilterParams(FilterType.BUTTERWORTH, FilterBand.HIGHPASS, 200, 200, 2);
		IIRFilterMethod filterMethod = new ButterworthMethod(appSampleRate, fp);
		drVoiceFilter = filterMethod.createFilter(0);
		
		// see https://docs.oracle.com/javase/tutorial/sound/capturing.html
		try {
			Mixer.Info inputMixerInfo = loggerAudioControl.getLoggerAudioSettings().findInputMixer();
			if (inputMixerInfo == null) {
				inputMixerInfo = SoundCardSystem.getInputMixerList().get(0);
			}
			inputMixer = AudioSystem.getMixer(inputMixerInfo);
			AudioFormat format = new AudioFormat(appSampleRate, appBitDepth, 1, true, false);
			drByteConverter = ByteConverter.createByteConverter(format);
			if (inputMixer.getTargetLineInfo().length == 0) {
				return false;
			}
			inputDataLine = (TargetDataLine) inputMixer.getLine(inputMixer.getTargetLineInfo()[0]);
			inputDataLine.open(format);
			AudioCaptureThread audioCaptureThread = new AudioCaptureThread(inputDataLine, format);
			Thread t = new Thread(audioCaptureThread);
			t.start();
			
		}
		catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		
		return inputMixer != null;
	}
	
	private void closeInput() {
		if (inputDataLine != null) {
			acquire = false;
			inputDataLine.stop();
			inputDataLine.close();
			inputDataLine = null;
		}
		if (inputMixer != null) {
			inputMixer.close();
			inputMixer = null;
		}
	}
	
	private class AudioCaptureThread implements Runnable {
		private TargetDataLine inputDataLine;
		private AudioFormat format;
		/**
		 * @param inputDataLine
		 * @param format
		 */
		public AudioCaptureThread(TargetDataLine inputDataLine, AudioFormat format) {
			super();
			this.inputDataLine = inputDataLine;
			this.format = format;
		}
		@Override
		public void run() {
			acquire = true;
			int buffSize = (int) (format.getFrameSize()*format.getFrameRate()/10);
			System.out.println("Starting voice acquire: " + format.toString());
			inputDataLine.start();
			long startTime = System.currentTimeMillis();
			long totalSamples = 0;
			while(acquire) {
				try {
					if (inputDataLine.isOpen() == false) {
						break;
					}
					if (inputDataLine.available() < buffSize) {
						Thread.sleep(5);
						continue;
					}
					byte[] data = new byte[buffSize];
					int bytesRead = inputDataLine.read(data, 0, buffSize);
					if (bytesRead < buffSize) {
//						System.out.printf("Voice acquired %d of %d bytes\n", bytesRead, buffSize);
						acquire = false;
						break;
					}
					getLevels(data, bytesRead);
					shareVoiceBuffer(data, bytesRead);
					totalSamples += bytesRead/format.getFrameSize();
//					System.out.printf(".");
				}
				catch (Exception e) {
					acquire = false;
					e.printStackTrace();
				}
			}
			long stopTime = System.currentTimeMillis();
			System.out.printf("Leave voice acquire, %d samples in %d millis = rate %3.2fkHz\n", 
					totalSamples, stopTime-startTime, (double) totalSamples / (double) (stopTime-startTime));
		}
		
	}

	/**
	 * Send voice data to app connections via MQTT. Who gets it depends 
	 * on which groups are enabled and which remote app is in each group. 
	 * @param data
	 * @param bytesRead
	 */
	private void shareVoiceBuffer(byte[] data, int bytesRead) {
		LoggerAudioSettings settings = loggerAudioControl.getLoggerAudioSettings();
		
		data = filterVoiceBuffer(data, bytesRead);
		
		Set<String> platforms = settings.getPlatformNames();
		Set<String> groups = settings.getTalkGroups();
		for (String platform : platforms) {
			PlatformSettings platSet = settings.getStreamSettings(platform);
			String platGroup = platSet.talkGroup;
			if (settings.isTalkGroup(platGroup)) {
				shareVoiceBuffer(platform, data, bytesRead);
			}
		}
	}

	// run a high pass filter on the data 
	private byte[] filterVoiceBuffer(byte[] data, int bytesRead) {
		int nSamp = bytesRead/2;
		double[][] audio = new double[1][nSamp];
		drByteConverter.bytesToDouble(data, audio, bytesRead);
		drVoiceFilter.runFilter(audio[0]);
		drByteConverter.doubleToBytes(audio, data, nSamp);
		return data;
	}


	/**
	 * Get levels from the input voice. 
	 * @param data
	 * @param bytesRead
	 */
	public void getLevels(byte[] data, int bytesRead) {
		int nSamp = bytesRead/2;
		double[][] audio = new double[1][nSamp];
		drByteConverter.bytesToDouble(data, audio, bytesRead);
		double max = 0;
		double[] chan = audio[0];
		for (int i = 0; i < chan.length; i++) {
			max = Math.max(max, Math.abs(chan[i]));
		}
		loggerAudioControl.drRecordLevel(max);
	}


	/**
	 * Share voice buffer to a platform via MQTT. If we get here, we
	 * already know that this platform group is enabled. 
	 * @param platform name of platform
	 * @param data data
	 * @param bytesRead data length in bytes
	 */
	private boolean shareVoiceBuffer(String platform, byte[] data, int dataBytes) {
		/*
		 *   need to share data very specifically to the one platform. So data topic must include
		 *   the platform name, not the group name. This may generate slightly more traffic on the local
		 *   connection to the MQTT server, but it's still the same traffic to each Android device.  
		 */
		LoggerNetworkManager netMan = LoggerNetworkSystem.getManager();
		if (netMan == null) {
			return false;
		}
		if (dataBytes < data.length) {
			data = Arrays.copyOf(data, dataBytes);
		}
		String topic = "DRVoice/"+platform;
		return netMan.sendData(platform, topic, data);
	}


	/**
	 * Setup output device for audio playback of what's coming in from the logger observerser
	 * @return
	 */
	private boolean prepareOutput() {

		closeOutput();

		ArrayList<Info> mixers = SoundCardSystem.getOutputMixerList();
		if (mixers == null || mixers.size() == 0) {
			return false;
		}
		Info mixer = loggerAudioControl.getLoggerAudioSettings().findOutputMixer();
		outputMixer = AudioSystem.getMixer(mixer);

		return true;
	}

	private void closeOutput() {
		try {
			// clear audio lines. 
			Set<String> keys = platformAudios.keySet();
			for (String key : keys) {
				PlatformAudio pfa = platformAudios.get(key);
				pfa.clearLine();
			}

			Line[] sls = outputMixer.getSourceLines();
			if (sls != null) {
				for (int i = 0; i < sls.length; i++) {
					//					sls[i].
				}
			}
			if (outputMixer != null) {
				//				currentMixer.
				outputMixer.close();
				outputMixer = null;
			}
		}
		catch (Exception e) {}
	}

	private class SCLineListener implements LineListener {

		@Override
		public void update(LineEvent event) {
			//			System.out.println(event.getClass().getName());
			Type type = event.getType();
			if (type == LineEvent.Type.START) {
				//				deviceState.setStarted(true);
			}
			else if (type == LineEvent.Type.STOP) {
				//				deviceState.setStarted(false);
			}
			else if (type == LineEvent.Type.OPEN) {

			}
		}

	}

	/**
	 * Get plaform audio. Always create if not there. 
	 * @param key
	 * @return
	 */
	public PlatformAudio getPlatformAudio(String key) {
		PlatformAudio p = platformAudios.get(key);
		if (p == null) {
			p = new PlatformAudio(this, key);
			platformAudios.put(key, p);
		}
		return p;
	}

	/**
	 * Get platform audio. Don't create, so return null if it's not there. 
	 * @param key
	 * @return
	 */
	public PlatformAudio findPlatformAudio(String key) {
		return platformAudios.get(key);
	}


	/**
	 * Setup the MQTT channels to receive data from the observers. 
	 */
	private void setupListener() {
		LoggerNetworkManager netManager = LoggerNetworkSystem.getManager();
		if (netManager != null && listening == false) {
			netManager.subsribeTopic(listenTopic, new LoggerNetworkReceiver() {
				@Override
				public boolean newMessage(LoggerNetworkMessage message) {
					try {
						audioReceived(message);
					}
					catch (Exception e) {
						e.printStackTrace();
					}
					return true;
				}
			});
			listening = true;
		}
	}

	private long lastOut = 0;

	private long startTime;

	/**
	 * Network callback. repacks the data as double and dumps it into a queue for each platform. 
	 * A different thread will read the queues and use the data so that this is never blocked
	 * by writing to the output device. 
	 * @param message
	 */
	protected void audioReceived(LoggerNetworkMessage message) {
		// get last topic id which is the sending platform
		long now = System.currentTimeMillis();
		int lastSlash = message.getTopic().lastIndexOf('/');
		if (lastSlash < 0) {
			return; // should never happen
		}
		String sender = message.getTopic().substring(lastSlash+1);
		// need to see if it's a new channel, because if it is, then we need
		// to notify a few things. 
		boolean isNew = false;
		PlatformAudio pfa = platformAudios.get(sender);
		if (pfa == null) {
			pfa = new PlatformAudio(this, sender);
			isNew = true;
			platformAudios.put(sender, pfa);
			loggerAudioControl.checkActionsMap(sender);
			loggerAudioControl.getLoggerAudioSettings().getStreamSettings(sender); // init settings so other parts know of them.
			loggerAudioControl.newPlatform(pfa);
			loggerAudioControl.platformUpdate(pfa);
		}
		if (isNew) {

		}

		//		 byteConverter = ByteConverter.createByteConverter(2, false, Encoding.PCM_SIGNED);
		// it's int16, so need half the number of samples. 
		// app is sending 1280 bytes, but we're receiving 1285. Wha'ts happening ? 
		byte[] audioBytes = Arrays.copyOfRange(message.getData(), 0, message.getData().length);
		int nBytes = audioBytes.length;
		int nSamples = nBytes/2;
		double[][] audio = new double[1][nSamples]; 
		inputByteConverter.bytesToDouble(audioBytes, audio, nBytes);
		double max = 0;
		for (int i = 0; i < 10; i++) {
			max = Math.max(Math.abs(audio[0][i]), max);
		}
		RawDataUnit rdu = new RawDataUnit(now, 1, pfa.totalSamples, audio[0].length);
		rdu.setRawData(audio[0]);

		pfa.addAudioData(rdu);
		pfa.totalSamples += audio[0].length;
		// what's in those first five bytes ? [0 8 -2 0 5]
		//		if (now - lastOut > 1000) {
		//			System.out.printf("%d(%d) bytes %d-%d received from %s - tot samples %d max level is %5.4f\n", 
		//					audioBytes.length, message.getData().length, audioBytes[0], audioBytes[1], sender, pfa.totalSamples, max);
		//			lastOut = now;
		//		}


	}

	/**
	 *  timer action to empty queues, done here so that the callback
	 *  from MQTT can always return immediately. Will also do each queue in a 
	 *  separate thread in the hope that they interleave better
	 */	
	protected void queueTimerAction() {
		Set<String> keys = platformAudios.keySet();
		Thread[] threads = new Thread[keys.size()];
		int i = 0;
		for (String key : keys) {
			PlatformAudio pfa = platformAudios.get(key);
			threads[i] = new Thread(new Runnable() {
				@Override
				public void run() {
					emptyQueue(pfa);
				}
			});
			threads[i].start();
			i++;
		}
		// wait for all to finish so that we can never have two threads writing to the same line
		for (i = 0; i < threads.length; i++) {
			try {
				threads[i].join();
			} catch (InterruptedException e) {
				//				e.printStackTrace();
			}
		}
	}


	/**
	 * Empties network data que for each platform. Data are put into 
	 * a datablock, and also sent to the output device. 
	 * Note that the data may not be correctly interleaved in the 
	 * datablock - may want to change this to a block per platform ?  
	 * @param pfa
	 */
	private void emptyQueue(PlatformAudio pfa) {

		RawDataUnit rdu;
		LoggerRawAudioDataBlock rawOutDataBlock = pfa.getRawOutDataBlock();
		rawOutDataBlock.setNaturalLifetime(loggerAudioControl.getLoggerAudioSettings().bufferSeconds);
		long now = System.currentTimeMillis();
		PlatformSettings platSettings = loggerAudioControl.getLoggerAudioSettings().getStreamSettings(pfa.getPlatform());

		while ((rdu = pfa.getUnit()) != null) {
			// amplify the data if necessary
			int gain = platSettings.gainDB;
			double[] data = rdu.getRawData();
			if (gain != 0) {
				double g = Math.pow(10., (double) gain / 20.);
				for (int i = 0; i < data.length; i++) {
					data[i] *= g;
				}
			}
			double max = 0;
			for (int i = 0; i < data.length; i++) {
				max = Math.max(max, Math.abs(data[i]));
			}

			pfa.storeDataUnit(rdu);
			pfa.setLevel(max);

			double[] interleaved = interleaveAudio(rdu.getRawData(), platSettings.outputChannel);
			double[][] out = {interleaved};
			byte[] outBytes = new byte[interleaved.length*2];
			outputByteConverter.doubleToBytes(out, outBytes, interleaved.length);
			SourceDataLine dataLine = pfa.getSourceDataLine(outputMixer, outputFormat);
			if (dataLine != null) {
				long tic = System.currentTimeMillis();
				dataLine.write(outBytes, 0, outBytes.length);
				long toc = System.currentTimeMillis();
				int bs = dataLine.getBufferSize();
				if (toc-tic >= 2) {
					pfa.clearQueue();
					dataLine.flush();
					dataLine.drain();
					int aa = dataLine.available();
					//				if (now - lastOut > 1000 && platSettings.outputChannel == 1) {
					System.out.printf("Line buffer size chan %d is %d, avail %d, write took %d millis\n", platSettings.outputChannel, bs, aa, toc-tic);
					//				System.out.printf("%d(%d) bytes %d-%d received from %s - tot samples %d max level is %5.4f\n", 
					//						audioBytes.length, message.getData().length, audioBytes[0], audioBytes[1], sender, pfa.totalSamples, max);
					//					lastOut = now;
				}
			}
			// and clear up old data. Since PAMGuard probably isn't running the clearup won't get called, so do it here. 
			synchronized(rawOutDataBlock.getSynchLock()) {
				rawOutDataBlock.clearold(System.currentTimeMillis() - loggerAudioControl.getLoggerAudioSettings().bufferSeconds*1000);
			}
			loggerAudioControl.platformUpdate(pfa);
		}
	}

	/**
	 * Interleave data with zeros so that it can be sent to any channel combination 
	 * of a stereo output line. 
	 * @param data
	 * @param channelMap = 0,1,2,3 depending on which channels selected. 
	 * @return
	 */
	private double[] interleaveAudio(double[] data, int channelMap) {
		int n = data.length;
		double[] out = new double[data.length * 2];
		int[] chan = PamUtils.getChannelArray(channelMap);
		for (int ch = 0; ch < chan.length; ch++) {
			for (int i = 0, j = chan[ch]; i < data.length; i++, j += 2) {
				out[j] = data[i];
			}
		}
		return out;
	}


	@Override
	public float getSampleRate() {
		return appSampleRate;
	}

	@Override
	public void setSampleRate(float sampleRate, boolean notify) {
		sampleRate = this.appSampleRate;
		super.setSampleRate(sampleRate, notify);
	}

	/**
	 * @return the audioDataBlock
	 */
	public LoggerAudioDataBlock getAudioDataBlock() {
		return audioDataBlock;
	}


	@Override
	public void pamStart() {
		startTime = PamCalendar.getTimeInMillis();
		running = true;
	}

	@Override
	public void pamStop() {
		running = false;
	}


	/**
	 * Called when PAMGuard closes, to make sure all recordings are
	 * nicely ended and logged to database. 
	 */
	public void endAllRecordings() {
		Set<String> keys = platformAudios.keySet();
		for (String key : keys) {
			PlatformAudio pfa = platformAudios.get(key);
			pfa.stopRecording();
		}

	}

	/**
	 * Get current platform audio names. 
	 * @return
	 */
	public Set<String> getPlatformNames() {
		return platformAudios.keySet();
	}


}
