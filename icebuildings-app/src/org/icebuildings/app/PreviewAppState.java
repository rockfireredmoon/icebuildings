package org.icebuildings.app;

import java.util.Objects;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.Preferences;

import org.icebuildings.BuildingsConfig;
import org.icebuildings.EntityBuildableControl;
import org.icescene.build.ObjectManipulatorControl;
import org.icescene.build.SelectionManager;
import org.icescene.environment.EnvironmentLight;
import org.icescene.io.MouseManager;
import org.icescene.props.BuildingXMLEntity;
import org.icescene.props.Entity;
import org.icescene.props.EntityFactory;
import org.icescene.scene.AbstractDebugSceneAppState;
import org.iceui.IceUI;

import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

public class PreviewAppState extends AbstractDebugSceneAppState {

	private EntityFactory loader;
	private final EnvironmentLight environmentLight;

	private BuildingXMLEntity building;
	private String ats;
	private Entity piece;
	private MouseManager mouseManager;
	private ObjectManipulatorControl omc;
	private SelectionManager<Entity, EntityBuildableControl> selectionManager;
	private Spatial buildingSpatial;
	private boolean editable;

	public PreviewAppState(EnvironmentLight environmentLight, SelectionManager<Entity, EntityBuildableControl> selectionManager,
			Node parentNode, Preferences prefs, EntityFactory loader) {
		super(prefs, parentNode);
		addPrefKeyPattern(BuildingsConfig.BUILDINGS + ".*");
		this.selectionManager = selectionManager;
		this.environmentLight = environmentLight;

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
		if (evt.getKey().equals(BuildingsConfig.BUILDINGS_LIGHT) || evt.getKey().equals(BuildingsConfig.BUILDINGS_LIGHT_COLOUR)) {
			setLightColour();
		}
	}

	public void setPiece(Entity piece) {
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
//					removeBuildableControlFromComponents();
				} else {
					selectionManager.setMouseEnabled(true);
//					addBuildableControlToComponents();
				}
			}
			this.editable = editable;
		}
	}

	public void setBuilding(BuildingXMLEntity building) {
		if (Objects.equals(building, this.building)) {
			return;
		}

		if (this.building != null)
			removeBuildableControlFromComponents();

		removeBuilding();
		this.building = building;
		createSpatial();
//		if (this.building != null && editable)
		if (this.building != null)
			addBuildableControlToComponents();
	}

	private void addBuildableControlToComponents() {
		for (Entity e : this.building.getComponent().getEntities()) {
			Spatial m = this.building.getMesh(e);
			if (m != null) {
				m.addControl(new EntityBuildableControl(assetManager, rootNode, this.building, e));
			}
		}
	}

	private void removeBuildableControlFromComponents() {
		for (Entity e : this.building.getComponent().getEntities()) {
			Spatial m = this.building.getMesh(e);
			if (m != null) {
				m.removeControl(EntityBuildableControl.class);
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

	public BuildingXMLEntity getSpatial() {
		return building;
	}

	protected void setLightColour() {
		environmentLight.setAmbientColor(IceUI.getColourPreference(prefs, BuildingsConfig.BUILDINGS_LIGHT_COLOUR,
				BuildingsConfig.BUILDINGS_LIGHT_COLOUR_DEFAULT).multLocal(
				prefs.getFloat(BuildingsConfig.BUILDINGS_LIGHT, BuildingsConfig.BUILDINGS_LIGHT_DEFAULT)));
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
