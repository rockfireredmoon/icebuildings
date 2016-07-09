package org.icebuildings.app;

import icemoon.iceloader.ServerAssetManager;

import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.icebuildings.BuildingEditorAppState;
import org.icebuildings.BuildingsConfig;
import org.icebuildings.BuildingsConstants;
import org.icebuildings.EntityBuildableControl;
import org.icelib.AppInfo;
import org.icelib.Appearance;
import org.icelib.UndoManager;
import org.icescene.HUDMessageAppState;
import org.icescene.IcesceneApp;
import org.icescene.assets.Assets;
import org.icescene.build.ObjectManipulatorManager;
import org.icescene.build.SelectionManager;
import org.icescene.environment.EnvironmentLight;
import org.icescene.environment.PostProcessAppState;
import org.icescene.io.ModifierKeysAppState;
import org.icescene.io.MouseManager;
import org.icescene.options.OptionsAppState;
import org.icescene.props.BuildingXMLEntity;
import org.icescene.props.Entity;
import org.icescene.props.EntityFactory;
import org.icescene.ui.WindowManagerAppState;
import org.lwjgl.opengl.Display;

import com.jme3.input.controls.ActionListener;
import com.jme3.math.Vector3f;

public class Icebuildings extends IcesceneApp implements ActionListener {

	private final static String MAPPING_OPTIONS = "Options";
	private static final Logger LOG = Logger.getLogger(Icebuildings.class.getName());

	public static void main(String[] args) throws Exception {

		AppInfo.context = Icebuildings.class;

		// Parse command line
		Options opts = createOptions();
		Assets.addOptions(opts);

		CommandLine cmdLine = parseCommandLine(opts, args);

		// A single argument must be supplied, the URL (which is used to
		// deterime router, which in turn locates simulator)
		if (cmdLine.getArgList().isEmpty()) {
			throw new Exception("No URL supplied.");
		}
		Icebuildings app = new Icebuildings(cmdLine);
		startApp(app, cmdLine, AppInfo.getName() + " - " + AppInfo.getVersion(), BuildingsConstants.APPSETTINGS_NAME);
	}

	private EntityFactory propFactory;
	private PreviewAppState previewAppState;

	private Icebuildings(CommandLine commandLine) {
		super(BuildingsConfig.get(), commandLine, BuildingsConstants.APPSETTINGS_NAME, "META-INF/BuildingsAssets.cfg");
		setUseUI(true);
		setPauseOnLostFocus(false);
	}

	@Override
	public void restart() {
		Display.setResizable(true);
		super.restart();
	}

	@Override
	public void destroy() {
		super.destroy();
		LOG.info("Destroyed application");
	}

	@Override
	public void onSimpleInitApp() {

		propFactory = new EntityFactory(this, rootNode);

		// The default creature
		Appearance appearance = new Appearance();

		appearance.setName(Appearance.Name.C2);
		appearance.setName(Appearance.Name.C2);
		appearance.setBody(Appearance.Body.NORMAL);
		appearance.setGender(Appearance.Gender.MALE);
		appearance.setRace(Appearance.Race.HART);
		appearance.setHead(Appearance.Head.NORMAL);

		flyCam.setDragToRotate(true);
		flyCam.setMoveSpeed(
				prefs.getFloat(BuildingsConfig.BUILDINGS_MOVE_SPEED, BuildingsConfig.BUILDINGS_MOVE_SPEED_DEFAULT));
		flyCam.setRotationSpeed(
				prefs.getFloat(BuildingsConfig.BUILDINGS_ROTATE_SPEED, BuildingsConfig.BUILDINGS_ROTATE_SPEED_DEFAULT));
		flyCam.setZoomSpeed(
				-prefs.getFloat(BuildingsConfig.BUILDINGS_ZOOM_SPEED, BuildingsConfig.BUILDINGS_ZOOM_SPEED_DEFAULT));
		flyCam.setEnabled(true);
		setPauseOnLostFocus(false);

		// Undo manager
		UndoManager undoManager = new UndoManager();

		// Light
		EnvironmentLight el = new EnvironmentLight(cam, rootNode, prefs);
		el.setDirectionalEnabled(false);
		el.setAmbientEnabled(true);

		// Need the post processor for bloom
		stateManager.attach(new PostProcessAppState(prefs, el));

		// For error messages and stuff
		stateManager.attach(new HUDMessageAppState());

		// Some windows need management
		stateManager.attach(new WindowManagerAppState(prefs));

		// Mouse manager requires modifier keys to be monitored
		stateManager.attach(new ModifierKeysAppState());

		// Mouse manager for dealing with clicking, dragging etc.
		final MouseManager mouseManager = new MouseManager(rootNode, getAlarm());
		stateManager.attach(mouseManager);

		// Select manager
		SelectionManager<Entity, EntityBuildableControl> selectionManager = new SelectionManager<Entity, EntityBuildableControl>(
				mouseManager, EntityBuildableControl.class);
		selectionManager.setMouseEnabled(false);

		// Object manipulator. Hooks into the select manager consuming its event
		new ObjectManipulatorManager<>(rootNode, this, selectionManager);

		// A menu
		stateManager.attach(new MenuAppState(undoManager, propFactory, prefs));

		// Other UI bits
		stateManager.attach(new UIAppState(undoManager, prefs));

		// The main on screen model
		previewAppState = new PreviewAppState(el, selectionManager, rootNode, prefs, propFactory);
		getStateManager().attach(previewAppState);

		// Building editor
		stateManager.attach(
				new BuildingEditorAppState(undoManager, prefs, propFactory, getAssets(), selectionManager, rootNode) {

					@Override
					protected void selectionChanged() {
						BuildingXMLEntity selectedBuilding = getSelectedBuilding();
						if (selectedBuilding != null) {
							previewAppState.setBuilding(selectedBuilding);
						}
					}

					protected void pieceSelected() {
						Entity selectedPiece = getSelectedPiece();
						if (selectedPiece != null) {
							previewAppState.setPiece(selectedPiece);
						}
					}

				});

		// Input
		getKeyMapManager().addMapping(MAPPING_OPTIONS);
		getKeyMapManager().addListener(this, MAPPING_OPTIONS);

		cam.setLocation(new Vector3f(-1, 9f, 32f));
	}

	@Override
	protected void configureAssetManager(ServerAssetManager serverAssetManager) {
		getAssets().setAssetsExternalLocation(
				prefs.get(BuildingsConfig.APP_WORKSPACE_DIR, BuildingsConfig.APP_WORKSPACE_DIR_DEFAULT));
	}

	public void onAction(String name, boolean isPressed, float tpf) {
		if (name.equals(MAPPING_OPTIONS)) {
			if (!isPressed) {
				final OptionsAppState state = stateManager.getState(OptionsAppState.class);
				if (state == null) {
					stateManager.attach(new OptionsAppState(prefs));
				} else {
					stateManager.detach(state);
				}
			}
		}
	}
}
