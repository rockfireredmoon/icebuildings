package org.icebuildings.app;

import java.util.prefs.Preferences;

import org.icescene.IcemoonAppState;
import org.icescene.props.EntityFactory;
import org.iceui.controls.ElementStyle;

import com.jme3.input.event.MouseButtonEvent;
import com.jme3.math.Vector2f;

import icetone.controls.buttons.PushButton;
import icetone.core.StyledContainer;
import icetone.core.ZPriority;
import icetone.core.layout.ScreenLayoutConstraints;
import icetone.core.layout.mig.MigLayout;
import icetone.core.undo.UndoManager;
import icetone.extras.windows.DialogBox;

public class MenuAppState extends IcemoonAppState<IcemoonAppState<?>> {
	public enum ItemMenuActions {
		NEW_ITEM, DELETE_ITEM, EDIT_ITEM, CLONE_ITEM
	}

	private StyledContainer layer;
	private PushButton options;
	private PushButton exit;

	public MenuAppState(UndoManager undoManager, EntityFactory propFactory, Preferences prefs) {
		super(prefs);
	}

	@Override
	protected void postInitialize() {

		layer = new StyledContainer(screen);
		layer.setLayoutManager(new MigLayout(screen, "fill", "push[][][][]push", "[]push"));

		// Options
		options = new PushButton(screen) {
			{
				setStyleClass("fancy");
			}
		};
		options.onMouseReleased(evt -> toggleOptions());
		options.setText("Options");
		layer.addElement(options);

		// Exit
		exit = new PushButton(screen) {
			{
				setStyleClass("fancy");
			}
		};
		exit.onMouseReleased(evt -> exitApp());
		exit.setText("Exit");
		layer.addElement(exit);

		//
		app.getLayers(ZPriority.MENU).addElement(layer);
	}

	@Override
	protected void onCleanup() {
		app.getLayers(ZPriority.MENU).removeElement(layer);
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
		final DialogBox dialog = new DialogBox(screen, new Vector2f(15, 15), true) {
			{
				setStyleClass("large");
			}

			@Override
			public void onButtonCancelPressed(MouseButtonEvent evt, boolean toggled) {
				hide();
			}

			@Override
			public void onButtonOkPressed(MouseButtonEvent evt, boolean toggled) {
				app.stop();
			}
		};
		dialog.setDestroyOnHide(true);
		ElementStyle.warningColor(dialog.getDragBar());
		dialog.setWindowTitle("Confirm Exit");
		dialog.setButtonOkText("Exit");
		dialog.setMsg("Are you sure you wish to exit? Make sure you have saved!");
		dialog.setResizable(false);
		dialog.setMovable(false);
		dialog.setModal(true);
		screen.showElement(dialog, ScreenLayoutConstraints.center);
	}
}
