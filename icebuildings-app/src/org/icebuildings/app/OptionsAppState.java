package org.icebuildings.app;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import org.icebuildings.BuildingsConfig;
import org.icelib.XDesktop;
import org.icescene.SceneConfig;
import org.iceui.controls.TabPanelContent;

import icetone.controls.buttons.PushButton;
import icetone.controls.text.TextField;
import icetone.core.StyledContainer;
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
		bloom.setChecked(SceneConfig.SCENE_BLOOM_DEFAULT);
		ambientOcclusion.setChecked(SceneConfig.SCENE_SSAO_DEFAULT);
	}

	protected void videoTab() {
		TabPanelContent el = new TabPanelContent(screen);
		el.setResizable(false);
		el.setMovable(false);
		el.setLayoutManager(new MigLayout(screen, "fill", "[150:150:150][grow]", "[][][]push"));

		// Checkbox container 1
		StyledContainer c2 = new StyledContainer(screen);
		c2.setLayoutManager(new MigLayout(screen, "wrap 1", "[]", "[][]"));

		c2.addElement(bloom = createCheckbox(SceneConfig.SCENE_BLOOM, "Bloom", SceneConfig.SCENE_BLOOM_DEFAULT));
		c2.addElement(ambientOcclusion = createCheckbox(SceneConfig.SCENE_SSAO, "Ambient Occlusion",
				SceneConfig.SCENE_SSAO_DEFAULT));

		el.addElement(c2, "span 2, wrap");
		// Dislay
		PushButton display = new PushButton(screen) {
			{
				setStyleClass("fancy");
			}
		};
		display.onMouseReleased(evt -> showDisplaySettingsWindow());
		display.setText("Display");
		display.setText("Display settings");
		el.addElement(display, "ax 50%, span 2, wrap");

		optionTabs.addTab("Video");
		optionTabs.addTabChild(0, el);
	}

	private void appTab() {
		TabPanelContent el = new TabPanelContent(screen);
		el.setResizable(false);
		el.setMovable(false);
		el.setLayoutManager(new MigLayout(screen, "wrap 3", "[shrink 0][grow, fill][]", "[]push"));
		el.addElement(createLabel("Workspace Directory"));
		workspaceDir = new TextField(screen);
		workspaceDir.onKeyboardReleased(evt -> prefs.put(BuildingsConfig.APP_WORKSPACE_DIR,
				evt.getElement().getText().replace("~", System.getProperty("user.home"))));
		workspaceDir.setText(prefs.get(BuildingsConfig.APP_WORKSPACE_DIR, BuildingsConfig.APP_WORKSPACE_DIR_DEFAULT)
				.replace(System.getProperty("user.home"), "~"));
		el.addElement(workspaceDir);
		PushButton open = new PushButton(screen) {
			{
				setStyleClass("fancy");
			}
		};
		open.onMouseReleased(evt -> {
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
		});
		open.setText("Open");
		open.setToolTipText("Open the workspace directory.");
		el.addElement(open);

		optionTabs.addTab("Application");
		optionTabs.addTabChild(3, el);
	}

	protected void setAdditionalDefaults() {
		workspaceDir.setText(BuildingsConfig.APP_WORKSPACE_DIR_DEFAULT.replace(System.getProperty("user.home"), "~"));
	}
}
