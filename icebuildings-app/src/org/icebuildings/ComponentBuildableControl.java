package org.icebuildings;

import java.util.logging.Level;

import org.icescene.ogreparticle.OGREParticleEmitter;
import org.icescene.ogreparticle.OGREParticleScript;
import org.icescene.props.ComponentPiece;
import org.icescene.props.Entity;
import org.icescene.props.XMLProp;
import org.icescene.props.XRef;
import org.icescene.scene.AbstractBuildableControl;
import org.icescene.scene.Buildable;

import com.jme3.asset.AssetManager;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

import icetone.core.undo.UndoManager;
import icetone.core.undo.UndoableCommand;

public final class ComponentBuildableControl extends DefaultBuildableControl {
	private UndoManager undoManager;

	public ComponentBuildableControl(AssetManager assetManager, Node toolsNode, XMLProp component, Buildable buildable,
			UndoManager undoManager) {
		super(assetManager, toolsNode, component, buildable);
		this.undoManager = undoManager;
	}

	@Override
	protected void onApply(AbstractBuildableControl<Buildable> actualBuildable) {
		if (actualBuildable.hasChanged()) {
			if (actualBuildable.getEntity() instanceof ComponentPiece) {
				ComponentPiece e = (ComponentPiece) actualBuildable.getEntity();
				if (undoManager == null) {
					e.setTranslation(actualBuildable.getSpatial().getLocalTranslation());
					e.setRotation(actualBuildable.getSpatial().getLocalRotation());
				} else {
					undoManager.storeAndExecute(new ApplyChangesCommand(e, spatial));
				}
			}
		}
	}

	class ApplyChangesCommand implements UndoableCommand {

		private Vector3f loc;
		private Quaternion rot;
		private ComponentPiece piece;
		private Spatial spatial;

		public ApplyChangesCommand(ComponentPiece piece, Spatial spatial) {
			this.loc = spatial.getLocalTranslation().clone();
			this.rot = spatial.getLocalRotation().clone();
			this.piece = piece;
			this.spatial = spatial;
		}

		public void undoCommand() {
			swap();
		}

		public void doCommand() {
			swap();
		}

		protected void swap() {
			Vector3f xloc = piece.getTranslation().clone();
			Quaternion xrot = piece.getRotation().clone();
			piece.setTranslation(loc);
			piece.setRotation(rot);
			spatial.setLocalTranslation(loc);
			spatial.setLocalRotation(rot);
			loc = xloc;
			rot = xrot;
		}
	}
}