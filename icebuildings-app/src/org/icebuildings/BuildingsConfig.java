package org.icebuildings;

import java.util.prefs.Preferences;

import org.icelib.AbstractConfig;
import org.icescene.SceneConfig;

import com.jme3.math.ColorRGBA;

public class BuildingsConfig extends SceneConfig {

	public final static String ATSEDITOR = "ATSEditor";
	
	public final static String BUILDINGS = "Buildings";
	public final static String BUILDINGS_LIGHT = BUILDINGS + "Light";
	public final static float BUILDINGS_LIGHT_DEFAULT = 3.0f;
	public final static String BUILDINGS_LIGHT_COLOUR = BUILDINGS + "LightColour";
	public final static ColorRGBA BUILDINGS_LIGHT_COLOUR_DEFAULT = ColorRGBA.White;

	// Camera move speed (build mode)
	public final static String BUILDINGS_MOVE_SPEED = BUILDINGS + "MoveSpeed";
	public final static float BUILDINGS_MOVE_SPEED_DEFAULT = 50f;
	// Camera zoom speed
	public final static String BUILDINGS_ZOOM_SPEED = BUILDINGS + "ZoomSpeed";
	public final static float BUILDINGS_ZOOM_SPEED_DEFAULT = 20f;
	// Camera rotate speed
	public final static String BUILDINGS_ROTATE_SPEED = BUILDINGS + "RotateSpeed";
	public final static float BUILDINGS_ROTATE_SPEED_DEFAULT = 10f;

	public static Object getDefaultValue(String key) {
		return AbstractConfig.getDefaultValue(BuildingsConfig.class, key);
	}

	public static Preferences get() {
		return Preferences.userRoot().node(BuildingsConstants.APPSETTINGS_NAME).node("game");
	}
}
