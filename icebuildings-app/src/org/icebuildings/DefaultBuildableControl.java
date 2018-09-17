package org.icebuildings;

import java.beans.PropertyChangeEvent;

import org.icescene.props.XMLProp;
import org.icescene.scene.AbstractBuildableControl;
import org.icescene.scene.Buildable;

import com.jme3.asset.AssetManager;
import com.jme3.scene.Node;

public class DefaultBuildableControl extends AbstractBuildableControl<Buildable> {

	public DefaultBuildableControl(AssetManager assetManager, Node toolsNode, XMLProp building, Buildable buildable) {
		super(assetManager, toolsNode, buildable);
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
	}

	@Override
	protected void onApply(AbstractBuildableControl<Buildable> actualBuildable) {
	}

}
