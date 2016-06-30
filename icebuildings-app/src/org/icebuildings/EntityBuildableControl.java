package org.icebuildings;

import java.beans.PropertyChangeEvent;

import org.icescene.props.BuildingXMLEntity;
import org.icescene.props.Entity;
import org.icescene.scene.AbstractBuildableControl;

import com.jme3.asset.AssetManager;
import com.jme3.scene.Node;

public class EntityBuildableControl extends AbstractBuildableControl<Entity> {

	public EntityBuildableControl(AssetManager assetManager, Node toolsNode, BuildingXMLEntity building, Entity buildable) {
		super(assetManager, toolsNode, buildable);
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
	}

	@Override
	protected void onApply(AbstractBuildableControl<Entity> actualBuildable) {
	}

}
