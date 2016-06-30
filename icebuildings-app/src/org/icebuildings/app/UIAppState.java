package org.icebuildings.app;

import java.util.prefs.Preferences;

import org.icebuildings.BuildingsConfig;
import org.icelib.UndoManager;
import org.icescene.scene.AbstractSceneUIAppState;
import org.iceui.IceUI;
import org.iceui.controls.color.ColorFieldControl;

import com.jme3.input.event.MouseButtonEvent;
import com.jme3.math.ColorRGBA;

import icetone.controls.buttons.CheckBox;
import icetone.controls.lists.FloatRangeSliderModel;
import icetone.controls.lists.Slider;
import icetone.core.Element.Orientation;
import icetone.core.layout.mig.MigLayout;

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
		lightColour = new ColorFieldControl(screen, IceUI.getColourPreference(prefs, BuildingsConfig.BUILDINGS_LIGHT_COLOUR,
				BuildingsConfig.BUILDINGS_LIGHT_COLOUR_DEFAULT), false, true, true) {
			@Override
			protected void onChangeColor(ColorRGBA newColor) {
				IceUI.setColourPreferences(prefs, BuildingsConfig.BUILDINGS_LIGHT_COLOUR, newColor);
			}
		};
		lightColour.setToolTipText("Light colour");
		layer.addChild(lightColour);

		// Light
		light = new Slider<Float>(screen, Orientation.HORIZONTAL, true) {
			@Override
			public void onChange(Float value) {
				prefs.putFloat(BuildingsConfig.BUILDINGS_LIGHT, (Float) value);
			}
		};
		light.setSliderModel(new FloatRangeSliderModel(0, 5,
				prefs.getFloat(BuildingsConfig.BUILDINGS_LIGHT, BuildingsConfig.BUILDINGS_LIGHT_DEFAULT), 0.25f));
		light.setToolTipText("Light Amount");
		layer.addChild(light);

		// Edit preview
		editPreview = new CheckBox(screen) {
			@Override
			public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
				PreviewAppState pas = app.getStateManager().getState(PreviewAppState.class);
				if (pas != null) {
					pas.setEditable(toggled);
				}
			}
		};
		editPreview.setLabelText("Mouse editing");
		PreviewAppState pas = app.getStateManager().getState(PreviewAppState.class);
		editPreview.setIsChecked(pas != null && pas.isEditable());
		layer.addChild(editPreview);

	}

	@Override
	protected MigLayout createLayout() {
		return new MigLayout(screen, "fill", "[][:75:][]push[][][][]", "[]push");
	}
}
