package org.icebuildings.app;

import java.util.prefs.Preferences;

import org.icelib.UndoManager;
import org.icescene.IcemoonAppState;
import org.icescene.props.EntityFactory;
import org.iceui.controls.FancyButton;
import org.iceui.controls.FancyDialogBox;
import org.iceui.controls.FancyWindow;
import org.iceui.controls.UIUtil;

import com.jme3.input.event.MouseButtonEvent;
import com.jme3.math.Vector2f;

import icetone.core.Container;
import icetone.core.Element.ZPriority;
import icetone.core.layout.mig.MigLayout;

public class MenuAppState extends IcemoonAppState<IcemoonAppState<?>> {
	public enum ItemMenuActions {
		NEW_ITEM, DELETE_ITEM, EDIT_ITEM, CLONE_ITEM
	}

	private Container layer;
	private FancyButton options;
	private FancyButton exit;

	public MenuAppState(UndoManager undoManager, EntityFactory propFactory, Preferences prefs) {
		super(prefs);
	}

	@Override
	protected void postInitialize() {

		layer = new Container(screen);
		layer.setLayoutManager(new MigLayout(screen, "fill", "push[][][][]push", "[]push"));

		// Options
		options = new FancyButton(screen) {
			@Override
			public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
				toggleOptions();
			}
		};
		options.setText("Options");
		layer.addChild(options);

		// Exit
		exit = new FancyButton(screen) {
			@Override
			public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
				exitApp();
			}
		};
		exit.setText("Exit");
		layer.addChild(exit);

		//
		app.getLayers(ZPriority.MENU).addChild(layer);
	}

	@Override
	protected void onCleanup() {
		app.getLayers(ZPriority.MENU).removeChild(layer);
	}

	private void toggleOptions() {
		final OptionsAppState state = stateManager.getState(OptionsAppState.class);
		if (state == null) {
			stateManager.attach(new OptionsAppState(prefs));
		} else {
			stateManager.detach(state);
		}
	}

	private void exitApp() {
		final FancyDialogBox dialog = new FancyDialogBox(screen, new Vector2f(15, 15), FancyWindow.Size.LARGE, true) {
			@Override
			public void onButtonCancelPressed(MouseButtonEvent evt, boolean toggled) {
				hideWindow();
			}

			@Override
			public void onButtonOkPressed(MouseButtonEvent evt, boolean toggled) {
				app.stop();
			}
		};
		dialog.setDestroyOnHide(true);
		dialog.getDragBar().setFontColor(screen.getStyle("Common").getColorRGBA("warningColor"));
		dialog.setWindowTitle("Confirm Exit");
		dialog.setButtonOkText("Exit");
		dialog.setMsg("Are you sure you wish to exit? Make sure you have saved!");
		dialog.setIsResizable(false);
		dialog.setIsMovable(false);
		dialog.sizeToContent();
		UIUtil.center(screen, dialog);
		screen.addElement(dialog, null, true);
		dialog.showAsModal(true);
	}
}
