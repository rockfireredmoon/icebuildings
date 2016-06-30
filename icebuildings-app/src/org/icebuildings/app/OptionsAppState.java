package org.icebuildings.app;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import org.icebuildings.BuildingsConfig;
import org.icelib.XDesktop;
import org.icescene.DisplaySettingsWindow;
import org.icescene.IcesceneApp;
import org.icescene.SceneConfig;
import org.iceui.XTabPanelContent;
import org.iceui.controls.FancyButton;
import org.iceui.controls.TabPanelContent;
import org.iceui.controls.UIUtil;

import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.math.Vector2f;

import icetone.controls.text.TextField;
import icetone.core.Container;
import icetone.core.layout.mig.MigLayout;

public class OptionsAppState extends org.icescene.options.OptionsAppState {

	private static final Logger LOG = Logger.getLogger(OptionsAppState.class.getName());

	private TextField workspaceDir;

	public OptionsAppState(Preferences prefs) {
		super(prefs);
	}

	@Override
	protected void addAdditionalTabs() {
		super.addAdditionalTabs();
		appTab();
	}

	protected void setVideoDefaults() {
		bloom.setIsChecked(SceneConfig.SCENE_BLOOM_DEFAULT);
		ambientOcclusion.setIsChecked(SceneConfig.SCENE_SSAO_DEFAULT);
	}

	protected void videoTab() {
		TabPanelContent el = new XTabPanelContent(screen);
		el.setIsResizable(false);
		el.setIsMovable(false);
		el.setLayoutManager(new MigLayout(screen, "fill", "[150:150:150][grow]", "[][][]push"));

		// Checkbox container 1
		Container c2 = new Container(screen);
		c2.setLayoutManager(new MigLayout(screen, "wrap 1", "[]", "[][]"));

		c2.addChild(bloom = createCheckbox(SceneConfig.SCENE_BLOOM, "Bloom", SceneConfig.SCENE_BLOOM_DEFAULT));
		c2.addChild(ambientOcclusion = createCheckbox(SceneConfig.SCENE_SSAO, "Ambient Occlusion", SceneConfig.SCENE_SSAO_DEFAULT));

		el.addChild(c2, "span 2, wrap");

		// Dislay
		FancyButton display = new FancyButton(screen) {
			@Override
			public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
				DisplaySettingsWindow w = new DisplaySettingsWindow(screen, Vector2f.ZERO,
						((IcesceneApp) app).getAppSettingsName()) {
				};
				UIUtil.center(screen, w);
				w.showWithEffect();
			}
		};
		display.setText("Display");
		display.setText("Display settings");
		el.addChild(display, "ax 50%, span 2, wrap");

		optionTabs.addTab("Video");
		optionTabs.addTabChild(0, el);
	}

	private void appTab() {
		TabPanelContent el = new TabPanelContent(screen);
		el.setIsResizable(false);
		el.setIsMovable(false);
		el.setLayoutManager(new MigLayout(screen, "wrap 3", "[shrink 0][grow, fill][]", "[]push"));
		el.addChild(createLabel("Workspace Directory"));
		workspaceDir = new TextField(screen) {

			@Override
			public void controlKeyPressHook(KeyInputEvent evt, String text) {
				prefs.put(BuildingsConfig.APP_WORKSPACE_DIR, text.replace("~", System.getProperty("user.home")));
			}

		};
		workspaceDir.setText(prefs.get(BuildingsConfig.APP_WORKSPACE_DIR, BuildingsConfig.APP_WORKSPACE_DIR_DEFAULT)
				.replace(System.getProperty("user.home"), "~"));
		el.addChild(workspaceDir);
		FancyButton open = new FancyButton(screen) {

			@Override
			public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
				final File folder = new File(
						prefs.get(BuildingsConfig.APP_WORKSPACE_DIR, BuildingsConfig.APP_WORKSPACE_DIR_DEFAULT));
				try {
					if (!folder.exists() && !folder.mkdirs()) {
						throw new IOException("Failed to create folder.");
					}
					XDesktop.getDesktop().open(folder);
				} catch (Exception ex) {
					LOG.log(Level.SEVERE, String.format("Failed to open folder %s", folder), ex);
					error(String.format("Failed to open folder %s", folder), ex);
				}
			}

		};
		open.setText("Open");
		open.setToolTipText("Open the workspace directory.");
		el.addChild(open);

		optionTabs.addTab("Application");
		optionTabs.addTabChild(3, el);
	}

	protected void setAdditionalDefaults() {
		workspaceDir.setText(BuildingsConfig.APP_WORKSPACE_DIR_DEFAULT.replace(System.getProperty("user.home"), "~"));
	}
}
