package org.icebuildings.app;

import java.util.prefs.Preferences;

import org.icebuildings.BuildingsConfig;
import org.icescene.scene.AbstractSceneUIAppState;
import org.iceui.IceUI;

import icetone.controls.buttons.CheckBox;
import icetone.controls.lists.FloatRangeSliderModel;
import icetone.controls.lists.Slider;
import icetone.core.Orientation;
import icetone.core.layout.mig.MigLayout;
import icetone.core.undo.UndoManager;
import icetone.extras.chooser.ColorFieldControl;

public class UIAppState extends AbstractSceneUIAppState {

	private ColorFieldControl lightColour;
	private Slider<Float> light;
	private CheckBox editPreview;

	public UIAppState(UndoManager undoManager, Preferences prefs) {
		super(undoManager, prefs);
	}

	@Override
	protected void addBefore() {

		// Light Colour
		lightColour = new ColorFieldControl(screen, IceUI.getColourPreference(prefs,
				BuildingsConfig.BUILDINGS_LIGHT_COLOUR, BuildingsConfig.BUILDINGS_LIGHT_COLOUR_DEFAULT), true, true);
		lightColour.onChange(evt -> IceUI.setColourPreferences(prefs, BuildingsConfig.BUILDINGS_LIGHT_COLOUR, evt.getNewValue()));
		lightColour.setToolTipText("Light colour");
		layer.addElement(lightColour);

		// Light
		light = new Slider<Float>(screen, Orientation.HORIZONTAL);
		light.onChanged(evt -> prefs.putFloat(BuildingsConfig.BUILDINGS_LIGHT, evt.getNewValue()));
		light.setSliderModel(new FloatRangeSliderModel(0, 5,
				prefs.getFloat(BuildingsConfig.BUILDINGS_LIGHT, BuildingsConfig.BUILDINGS_LIGHT_DEFAULT), 0.25f));
		light.setToolTipText("Light Amount");
		layer.addElement(light);

		// Edit preview
		editPreview = new CheckBox(screen);
		editPreview.onChange(evt -> {
			PreviewAppState pas = app.getStateManager().getState(PreviewAppState.class);
			if (pas != null) {
				pas.setEditable(evt.getNewValue());
			}
		});
		editPreview.setText("Mouse editing");
		PreviewAppState pas = app.getStateManager().getState(PreviewAppState.class);
		editPreview.setChecked(pas != null && pas.isEditable());
		layer.addElement(editPreview);

	}

	@Override
	protected MigLayout createLayout() {
		return new MigLayout(screen, "fill", "[][:75:][]push[][][][]", "[]push");
	}
}
