/*
 * This file is part of VLCJ.
 *
 * VLCJ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * VLCJ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with VLCJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2015 Caprica Software Limited.
 */

package uk.co.caprica.vlcjplayer.view.main;

import static uk.co.caprica.vlcjplayer.Application.application;
import static uk.co.caprica.vlcjplayer.view.action.Resource.resource;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;
import uk.co.caprica.vlcj.component.EmbeddedMediaPlayerComponent;
import uk.co.caprica.vlcj.player.MediaPlayer;
import uk.co.caprica.vlcj.player.MediaPlayerEventAdapter;
import uk.co.caprica.vlcjplayer.event.AfterExitFullScreenEvent;
import uk.co.caprica.vlcjplayer.event.BeforeEnterFullScreenEvent;
import uk.co.caprica.vlcjplayer.event.PausedEvent;
import uk.co.caprica.vlcjplayer.event.PlayingEvent;
import uk.co.caprica.vlcjplayer.event.SnapshotImageEvent;
import uk.co.caprica.vlcjplayer.event.StoppedEvent;
import uk.co.caprica.vlcjplayer.view.BaseFrame;
import uk.co.caprica.vlcjplayer.view.MouseMovementDetector;
import uk.co.caprica.vlcjplayer.view.action.StandardAction;
import uk.co.caprica.vlcjplayer.view.action.mediaplayer.MediaPlayerActions;
import uk.co.caprica.vlcjplayer.view.snapshot.SnapshotView;

import com.google.common.eventbus.Subscribe;

@SuppressWarnings("serial")
public final class MainFrame extends BaseFrame {

	private static final String ACTION_EXIT_FULLSCREEN = "exit-fullscreen";

	private static final KeyStroke KEYSTROKE_ESCAPE = KeyStroke.getKeyStroke("ESCAPE");

	private static final KeyStroke KEYSTROKE_TOGGLE_FULLSCREEN = KeyStroke.getKeyStroke("F11");

	private final EmbeddedMediaPlayerComponent mediaPlayerComponent;

	private final StandardAction videoFullscreenAction;
	private final StandardAction videoAlwaysOnTopAction;

	private final PositionPane positionPane;

	private final ControlsPane controlsPane;

	private final VideoContentPane videoContentPane;

	private final JPanel bottomPane;

	private final JTabbedPane tabbedPane;

	private final MouseMovementDetector mouseMovementDetector;

	public MainFrame() {
		super("Botan TV [Kurdistan]");

		this.mediaPlayerComponent = application().mediaPlayerComponent();

		MediaPlayerActions mediaPlayerActions = application().mediaPlayerActions();

		JPanel topPanel = new JPanel();
		topPanel.setLayout(new BorderLayout());

		videoContentPane = new VideoContentPane();

		JPanel contentPane = new JPanel();
		contentPane.setLayout(new BorderLayout());
		contentPane.add(videoContentPane, BorderLayout.CENTER);

		setContentPane(topPanel);

		bottomPane = new JPanel();

		bottomPane.setLayout(new BorderLayout());

		JPanel bottomControlsPane = new JPanel();
		bottomControlsPane.setLayout(new MigLayout("fill, insets 0 n n n", "[grow]", "[]0[]"));

		positionPane = new PositionPane(mediaPlayerComponent.getMediaPlayer());
		bottomControlsPane.add(positionPane, "grow, wrap");

		controlsPane = new ControlsPane(mediaPlayerActions);
		bottomPane.add(bottomControlsPane, BorderLayout.CENTER);
		bottomControlsPane.add(controlsPane, "grow");

		contentPane.add(bottomPane, BorderLayout.SOUTH);
		tabbedPane = new JTabbedPane();
		tabbedPane.addTab("Video", contentPane);
		try {
			tabbedPane.addTab("Canal", this.getCanalPanel());
		} catch (Exception e) {
			e.printStackTrace();
		}
		topPanel.add(tabbedPane, BorderLayout.CENTER);

		videoFullscreenAction = new StandardAction(resource("menu.video.item.fullscreen")) {
			@Override
			public void actionPerformed(ActionEvent e) {
				mediaPlayerComponent.getMediaPlayer().toggleFullScreen();
			}
		};

		videoAlwaysOnTopAction = new StandardAction(resource("menu.video.item.alwaysOnTop")) {
			@Override
			public void actionPerformed(ActionEvent e) {
				boolean onTop;
				Object source = e.getSource();
				if (source instanceof JCheckBoxMenuItem) {
					JCheckBoxMenuItem menuItem = (JCheckBoxMenuItem) source;
					onTop = menuItem.isSelected();
				} else {
					throw new IllegalStateException("Don't know about source " + source);
				}
				setAlwaysOnTop(onTop);
			}
		};

		mediaPlayerComponent.getMediaPlayer().addMediaPlayerEventListener(new MediaPlayerEventAdapter() {

			@Override
			public void playing(MediaPlayer mediaPlayer) {
				videoContentPane.showVideo();
				mouseMovementDetector.start();
				application().post(PlayingEvent.INSTANCE);
			}

			@Override
			public void paused(MediaPlayer mediaPlayer) {
				mouseMovementDetector.stop();
				application().post(PausedEvent.INSTANCE);
			}

			@Override
			public void stopped(MediaPlayer mediaPlayer) {
				mouseMovementDetector.stop();
				videoContentPane.showDefault();
				application().post(StoppedEvent.INSTANCE);
			}

			@Override
			public void finished(MediaPlayer mediaPlayer) {
				videoContentPane.showDefault();
				mouseMovementDetector.stop();
				application().post(StoppedEvent.INSTANCE);
			}

			@Override
			public void error(MediaPlayer mediaPlayer) {
				videoContentPane.showDefault();
				mouseMovementDetector.stop();
				application().post(StoppedEvent.INSTANCE);
			}

			@Override
			public void mediaDurationChanged(MediaPlayer mediaPlayer, long newDuration) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						positionPane.setDuration(newDuration);
					}
				});
			}

			@Override
			public void timeChanged(MediaPlayer mediaPlayer, long newTime) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						positionPane.setTime(newTime);
					}
				});
			}
		});

		getActionMap().put(ACTION_EXIT_FULLSCREEN, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				mediaPlayerComponent.getMediaPlayer().toggleFullScreen();
				videoFullscreenAction.select(false);
			}
		});

		applyPreferences();

		
		MainFrame.this.mediaPlayerComponent.getMediaPlayer().setAspectRatio("185:100");
		MainFrame.this.mediaPlayerComponent.getMediaPlayer().setAdjustVideo(true);
		mouseMovementDetector = new VideoMouseMovementDetector(mediaPlayerComponent.getVideoSurface(), 500,
				mediaPlayerComponent);

		setMinimumSize(new Dimension(370 * 3, 240 * 3));
	}

	private JButton get(String image,String live) {
		JButton button = new JButton(
				new ImageIcon(((new ImageIcon(getClass().getResource("/canal/" + image + ".png"))).getImage())
						.getScaledInstance(170, 80, java.awt.Image.SCALE_SMOOTH)));
		button.setMargin(new Insets(1, 1, 1, 1));
		button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				MainFrame.this.tabbedPane.setSelectedIndex(0);
				MainFrame.this.mediaPlayerComponent.getMediaPlayer().playMedia(live);
				setTitle("Botan TV [" + String.valueOf(image.charAt(0)).toUpperCase() + image.substring(1) + "]");
			}
			
		});
		return button;
	}

	private JPanel getCanalPanel() throws Exception {
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout());
		panel.add(get("kurdistan-tv","rtmp://84.244.187.12/live/livestream"));
		panel.add(get("zagros-tv","http://198.100.158.231:1935/kanal10/_definst_/livestream/playlist.m3u8"));
		panel.add(get("kurdsat-tv","http://198.100.158.231:1935/kanal11/_definst_/livestream/playlist.m3u8"));
		panel.add(get("kurdsat-news","http://wpc.C1A9.edgecastcdn.net/hls-live/20C1A9/kurdsat/ls_satlink/b_528.m3u8"));
		panel.add(get("rudaw-tv","https://svs.itworkscdn.net/rudawlive/rudawlive.smil/playlist.m3u8"));
		panel.add(get("ronahi-tv","http://198.100.158.231:1935/kanal16/_definst_/livestream/list.m3u8"));
		panel.add(get("vin-tv","http://95.170.203.140:1213/hls/vinlive.m3u8"));
		panel.add(get("nalia-tv","http://prxy-wza-02.iptv-playoutcenter.de/nrt1/nrt1.stream_1/chunklist_w76649285.m3u8"));
		panel.add(get("nalia-2-tv","http://prxy-wza-02.iptv-playoutcenter.de/nrt2/nrt2.stream_1/chunklist_w1757825996.m3u8"));
		panel.add(get("knn-tv","http://51.254.209.160:1935/live/_definst_/livestream/playlist.m3u8"));
		panel.add(get("waar-tv","http://198.100.158.231:1935/kanal2/_definst_/livestream/playlist.m3u8"));
		panel.add(get("kanal4","http://198.100.158.231:1935/kanal12/_definst_/livestream/playlist.m3u8"));
		panel.add(get("gem-kurd-tv","http://63.237.48.28/ios/GEM_KURD/GEM_KURD.m3u8"));
		panel.add(get("kurdmax-tv","rtmp://live.kurdstream.net:1935/liveTrans/_definst_/mp4:myStream_720p"));
		panel.add(get("kurdistan24-tv","http://198.100.158.231:1935/kanal30/_definst_/livestream/playlist.m3u8"));
		panel.add(get("speda-tv","http://kurd-live.com:1935/live/speda/playlist.m3u8"));
		panel.add(get("rega-tv","http://162.244.81.103:1935/RegaTV/myStream/playlist.m3u8"));
		panel.add(get("cira-tv","http://62.210.100.139:1935/ciratv/smil:cira.smil/playlist.m3u8"));
		panel.add(get("med-nuce-tv","http://63.237.48.23/ios/GEM_KURD/GEM_KURD.m3u8"));
		panel.add(get("sterk-tv","http://198.100.158.231:1935/kanal6/_definst_/livestream/playlist.m3u8"));
		panel.add(get("newroz-tv","http://198.100.158.231:1935/kanal3/_definst_/livestream/playlist.m3u8"));
		panel.add(get("cihan-tv","rtmp://kurd-live.com/live/cihan"));
		panel.add(get("badinan-sat-tv","http://cofafrw181.glwiz.com:7777/Badinan.m3u8"));
		panel.add(get("korek-tv","http://sat.010e.net:8000/live/milad/milad/1232.m3u8"));
		panel.add(get("komala-tv","http://198.100.158.231:1935/kanal8/_definst_/livestream/playlist.m3u8"));
		panel.add(get("azadi-tv","http://38.99.146.181:7777/AzadiTV.m3u8"));
		panel.add(get("trt-tv","http://trtcanlitv-lh.akamaihd.net/i/TRT6_1@181944/master.m3u8"));
		panel.add(get("al-jazeera","rtmp://aljazeeraflashlivefs.fplive.net/aljazeeraflashlive-live/aljazeera_ara_high"));
		panel.add(get("russia-today","http://38.99.146.36:7777/RussiyaAlYaum_HD.m3u8"));
		return panel;
	}

	private void applyPreferences() {
		Preferences prefs = Preferences.userNodeForPackage(MainFrame.class);
		setBounds(prefs.getInt("frameX", 100), prefs.getInt("frameY", 100), prefs.getInt("frameWidth", 800),
				prefs.getInt("frameHeight", 600));
		boolean alwaysOnTop = prefs.getBoolean("alwaysOnTop", false);
		setAlwaysOnTop(alwaysOnTop);
		videoAlwaysOnTopAction.select(alwaysOnTop);
		String recentMedia = prefs.get("recentMedia", "");
		if (recentMedia.length() > 0) {
			List<String> mrls = Arrays.asList(prefs.get("recentMedia", "").split("\\|"));
			Collections.reverse(mrls);
			for (String mrl : mrls) {
				application().addRecentMedia(mrl);
			}
		}
	}

	@Override
	protected void onShutdown() {
		if (wasShown()) {
			Preferences prefs = Preferences.userNodeForPackage(MainFrame.class);
			prefs.putInt("frameX", getX());
			prefs.putInt("frameY", getY());
			prefs.putInt("frameWidth", getWidth());
			prefs.putInt("frameHeight", getHeight());
			prefs.putBoolean("alwaysOnTop", isAlwaysOnTop());

			String recentMedia;
			List<String> mrls = application().recentMedia();
			if (!mrls.isEmpty()) {
				StringBuilder sb = new StringBuilder();
				for (String mrl : mrls) {
					if (sb.length() > 0) {
						sb.append('|');
					}
					sb.append(mrl);
				}
				recentMedia = sb.toString();
			} else {
				recentMedia = "";
			}
			prefs.put("recentMedia", recentMedia);
		}
	}

	@Subscribe
	public void onBeforeEnterFullScreen(BeforeEnterFullScreenEvent event) {
		bottomPane.setVisible(false);
		registerEscapeBinding();
	}

	@Subscribe
	public void onAfterExitFullScreen(AfterExitFullScreenEvent event) {
		deregisterEscapeBinding();
		bottomPane.setVisible(true);
	}

	@Subscribe
	public void onSnapshotImage(SnapshotImageEvent event) {
		new SnapshotView(event.image());
	}

	private void registerEscapeBinding() {
		getInputMap().put(KEYSTROKE_ESCAPE, ACTION_EXIT_FULLSCREEN);
		getInputMap().put(KEYSTROKE_TOGGLE_FULLSCREEN, ACTION_EXIT_FULLSCREEN);
	}

	private void deregisterEscapeBinding() {
		getInputMap().remove(KEYSTROKE_ESCAPE);
		getInputMap().remove(KEYSTROKE_TOGGLE_FULLSCREEN);
	}

	private InputMap getInputMap() {
		JComponent c = (JComponent) getContentPane();
		return c.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
	}

	private ActionMap getActionMap() {
		JComponent c = (JComponent) getContentPane();
		return c.getActionMap();
	}
}
