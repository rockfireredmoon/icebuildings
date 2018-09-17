package org.icebuildings.app;

import java.util.List;
import java.util.Objects;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.Preferences;

import org.icebuildings.BuildingsConfig;
import org.icebuildings.ComponentBuildableControl;
import org.icebuildings.DefaultBuildableControl;
import org.icescene.build.ObjectManipulatorControl;
import org.icescene.build.SelectionManager;
import org.icescene.environment.EnvironmentLight;
import org.icescene.io.MouseManager;
import org.icescene.props.Entity;
import org.icescene.props.EntityFactory;
import org.icescene.props.XMLProp;
import org.icescene.props.XRef;
import org.icescene.scene.AbstractBuildableControl;
import org.icescene.scene.AbstractDebugSceneAppState;
import org.icescene.scene.Buildable;
import org.iceui.IceUI;

import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

import icetone.core.undo.UndoManager;

public class PreviewAppState extends AbstractDebugSceneAppState {

	private EntityFactory loader;
	private final EnvironmentLight environmentLight;

	private XMLProp building;
	private String ats;
	private Buildable piece;
	private MouseManager mouseManager;
	private ObjectManipulatorControl omc;
	private SelectionManager<Buildable, DefaultBuildableControl> selectionManager;
	private Spatial buildingSpatial;
	private boolean editable;
	private UndoManager undoManager;

	public PreviewAppState(EnvironmentLight environmentLight,
			SelectionManager<Buildable, DefaultBuildableControl> selectionManager, Node parentNode, Preferences prefs,
			EntityFactory loader, UndoManager undoManager) {
		super(prefs, parentNode);
		addPrefKeyPattern(BuildingsConfig.BUILDINGS + ".*");
		this.selectionManager = selectionManager;
		this.environmentLight = environmentLight;
		this.undoManager = undoManager;

		this.loader = loader;
	}

	@Override
	public void postInitialize() {
		super.postInitialize();

		setLightColour();
	}

	@Override
	protected void handlePrefUpdateSceneThread(PreferenceChangeEvent evt) {
		super.handlePrefUpdateSceneThread(evt);
		if (evt.getKey().equals(BuildingsConfig.BUILDINGS_LIGHT)
				|| evt.getKey().equals(BuildingsConfig.BUILDINGS_LIGHT_COLOUR)) {
			setLightColour();
		}
	}

	public void setPiece(Buildable piece) {
		// TODO bit extreme
		removeBuilding();
		this.piece = piece;
		createSpatial();
	}

	public boolean isEditable() {
		return editable;
	}

	public void setEditable(boolean editable) {
		if (editable != this.editable) {
			if (building != null) {
				if (!editable) {
					removeManipulator();
					selectionManager.setMouseEnabled(false);
					// removeBuildableControlFromComponents();
				} else {
					selectionManager.setMouseEnabled(true);
					// addBuildableControlToComponents();
				}
			}
			this.editable = editable;
		}
	}

	public void setBuilding(XMLProp building) {
		if (Objects.equals(building, this.building)) {
			return;
		}

		if (this.building != null)
			removeBuildableControlFromComponents();

		removeBuilding();
		this.building = building;
		createSpatial();
		// if (this.building != null && editable)
		if (this.building != null)
			addBuildableControlToComponents();
	}

	private void addBuildableControlToComponents() {
		createBuildableControls(building.getComponent().getEntities());
		createBuildableControls(building.getComponent().getXRefs());
	}

	protected void createBuildableControls(List<? extends Buildable> l) {
		for (Buildable e : l) {
			Spatial m = this.building.getMesh(e);
			if (m != null) {
				m.addControl(createBuildableControl(e));
			}
		}
	}

	protected void onApply(AbstractBuildableControl<Buildable> actualBuildable) {
	}

	protected DefaultBuildableControl createBuildableControl(Buildable e) {
		return new ComponentBuildableControl(assetManager, rootNode, this.building, e, undoManager);
	}

	private void removeBuildableControlFromComponents() {
		for (Entity e : this.building.getComponent().getEntities()) {
			Spatial m = this.building.getMesh(e);
			if (m != null) {
				m.removeControl(DefaultBuildableControl.class);
			}
		}
		for (XRef e : this.building.getComponent().getXRefs()) {
			Spatial m = this.building.getMesh(e);
			if (m != null) {
				m.removeControl(DefaultBuildableControl.class);
			}
		}
	}

	private void removeBuilding() {
		removeManipulator();
		if (this.building != null) {
			this.building.getSpatial().removeFromParent();
		}
	}

	@Override
	public void onCleanup() {
		super.onCleanup();
	}

	protected void modelLoaded() {
		// For sub-classes to overide and be notified of when the preview model
		// loads
	}

	public XMLProp getSpatial() {
		return building;
	}

	protected void setLightColour() {
		environmentLight.setAmbientColor(IceUI
				.getColourPreference(prefs, BuildingsConfig.BUILDINGS_LIGHT_COLOUR,
						BuildingsConfig.BUILDINGS_LIGHT_COLOUR_DEFAULT)
				.multLocal(prefs.getFloat(BuildingsConfig.BUILDINGS_LIGHT, BuildingsConfig.BUILDINGS_LIGHT_DEFAULT)));
	}

	private void removeManipulator() {
		if (omc != null && omc.getSpatial() != null) {
			omc.getSpatial().removeControl(omc);
		}
	}

	private void createSpatial() {

		// building.setATS(ats);
		// if (piece != null) {
		// Spatial s = (Spatial) building.getMesh(piece);
		// if (s != null) {
		// omc = new ObjectManipulatorControl(app.getRootNode(), mouseManager,
		// app);
		// s.addControl(omc);
		// }
		// }
		// building.setScale(new Vector3f(0.25f, 0.25f, 0.25f));
		Spatial spatial = building.getSpatial();
		app.getRootNode().attachChild(spatial);
	}

}
