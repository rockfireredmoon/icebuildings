package org.icebuildings;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.logging.Logger;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.icelib.Icelib;
import org.icelib.UndoManager;
import org.icescene.Alarm.AlarmTask;
import org.icescene.IcemoonAppState;
import org.icescene.IcesceneApp;
import org.icescene.NodeVisitor;
import org.icescene.NodeVisitor.Visit;
import org.icescene.assets.Assets;
import org.icescene.build.SelectionManager;
import org.icescene.build.SelectionManager.Listener;
import org.icescene.propertyediting.PropertiesPanel;
import org.icescene.props.BuildingXMLEntity;
import org.icescene.props.Entity;
import org.icescene.props.EntityFactory;
import org.icescene.ui.WindowManagerAppState;
import org.iceui.HPosition;
import org.iceui.VPosition;
import org.iceui.XTabPanelContent;
import org.iceui.controls.ElementStyle;
import org.iceui.controls.FancyButton;
import org.iceui.controls.FancyPersistentWindow;
import org.iceui.controls.FancyWindow;
import org.iceui.controls.IconTabControl;
import org.iceui.controls.SaveType;

import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.math.Vector2f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

import icemoon.iceloader.ServerAssetManager;
import icetone.controls.buttons.ButtonAdapter;
import icetone.controls.buttons.CheckBox;
import icetone.controls.lists.Table;
import icetone.controls.lists.Table.TableCell;
import icetone.controls.lists.Table.TableRow;
import icetone.controls.text.Label;
import icetone.controls.text.TextField;
import icetone.core.Container;
import icetone.core.Element;
import icetone.core.layout.BorderLayout;
import icetone.core.layout.mig.MigLayout;

/**
 */
public class BuildingEditorAppState extends IcemoonAppState<IcemoonAppState<?>>
		implements PreferenceChangeListener, Listener<Entity, EntityBuildableControl> {

	private static final Logger LOG = Logger.getLogger(BuildingEditorAppState.class.getName());
	protected FancyPersistentWindow window;
	private EntityFactory propFactory;
	private Table buildingsTable;
	private FancyButton addButton;
	private FancyButton removeButton;
	private FancyButton saveButton;
	private Table piecesTable;
	private boolean adjust;
	private Set<String> atsMeshes = new HashSet<String>();
	private TextField filter;
	private AlarmTask task;
	private BuildingXMLEntity building;
	private PropertiesPanel<Entity> pieceEditor;
	private IconTabControl tabs;
	private ATSTab atsTab;
	private SelectionManager<Entity, EntityBuildableControl> selectionManager;
	private Node parentNode;
	private CheckBox bldg;
	private CheckBox cav;
	private CheckBox dng;

	public BuildingEditorAppState(UndoManager undoManager, Preferences pref, EntityFactory propFactory, Assets assets,
			SelectionManager<Entity, EntityBuildableControl> selectionManager, Node parentNode) {
		super(pref);
		addPrefKeyPattern(BuildingsConfig.BUILDINGS + ".*");

		this.parentNode = parentNode;
		this.selectionManager = selectionManager;
		this.propFactory = propFactory;
	}

	public String getSelectedBuildingName() {
		TableRow row = buildingsTable.getSelectedRow();
		return row == null || !row.isLeaf() ? null : (String) row.getValue();
	}

	public Entity getSelectedPiece() {
		TableRow row = piecesTable.getSelectedRow();
		return row == null ? null : (Entity) row.getValue();
	}

	@Override
	public void postInitialize() {

		assetManager = app.getAssetManager();
		screen = ((IcesceneApp) app).getScreen();

		window = new FancyPersistentWindow(screen, BuildingsConfig.BUILDINGS, 8, VPosition.TOP, HPosition.LEFT,
				new Vector2f(600, 520), FancyWindow.Size.SMALL, false, SaveType.POSITION_AND_SIZE, prefs) {
			@Override
			protected void onCloseWindow() {
				super.onCloseWindow();
				app.getStateManager().detach(BuildingEditorAppState.this);
			}
		};

		// Window management (if available)
		WindowManagerAppState win = stateManager.getState(WindowManagerAppState.class);
		if (win != null) {
			window.setMinimizable(true);
		}

		// window.setIsResizable(false);
		window.setWindowTitle("Building");

		tabs = new IconTabControl(screen);
		tabs.addTabWithIcon("Building", "Interface/Styles/Gold/Common/Icons/home.png");
		tabs.addTabChild(0, buildingTab());
		tabs.addTabWithIcon("ATS", "Interface/Styles/Gold/Common/Icons/paint.png");
		tabs.addTabChild(1, atsTab = new ATSTab(screen) {
			@Override
			protected void onSelectedChanged() {
				rebuildPieces();
				selectionManager.clearSelection();
				selectionChanged();
			}

		});
		tabs.addTabWithIcon("Parts", "Interface/Styles/Gold/Common/Icons/bricks.png");
		tabs.addTabChild(2, piecesTab());

		// Content
		final Element windowContent = window.getContentArea();
		windowContent.setLayoutManager(new BorderLayout()); // Top
		windowContent.addChild(tabs, BorderLayout.Border.CENTER);

		// Pack and show
		setAvailable();
		window.setIsResizable(true);
		screen.addElement(window, null, true);
		window.showWithEffect();

		adjust(true);
		try {
			rebuildBuildingsTable();
		} finally {
			adjust(false);
		}

		//
		selectionManager.addListener(this);
	}

	public BuildingXMLEntity getSelectedBuilding() {
		return building;
	}

	@Override
	public void selectionChanged(SelectionManager<Entity, EntityBuildableControl> selectionManager) {
		adjust = true;
		try {
			Entity e = selectionManager.getFirstSelectedProp();
			if (e == null) {
				piecesTable.clearSelection();
			} else {
				piecesTable.setSelectedRowObjects(Arrays.asList(e));
				piecesTable.scrollToSelected();
			}
		} finally {
			adjust = false;
		}
	}

	@Override
	protected void handlePrefUpdateSceneThread(PreferenceChangeEvent evt) {
	}

	protected void pieceSelected() {

	}

	protected void selectionChanged() {

	}

	@Override
	protected void onCleanup() {
		super.onCleanup();
		selectionManager.removeListener(this);
		window.setDestroyOnHide(true);
		window.hideWithEffect();
	}

	private void pieceChanged() {
		Entity selectedPiece = getSelectedPiece();
		pieceEditor.setObject(selectedPiece);
		NodeVisitor v = new NodeVisitor(parentNode);
		v.visit(new Visit() {
			@Override
			public void visit(Spatial node) {
				EntityBuildableControl ebc = node.getControl(EntityBuildableControl.class);
				if (ebc != null) {
					if (ebc.getEntity().equals(selectedPiece)) {
						selectionManager.select(ebc, 0);
						// TODO visitor needs a way to break out
					}
				}
			}
		});
	}

	private void setAvailable() {

	}

	private XTabPanelContent buildingTab() {

		// Top
		Element top = new Element(screen);
		top.setLayoutManager(new MigLayout(screen, "", "[][fill,grow][]", "[]"));
		top.addChild(new Label("Find:", screen));
		filter = new TextField(screen) {

			@Override
			public void controlKeyPressHook(KeyInputEvent evt, String text) {
				refilter();
			}
		};
		top.addChild(filter);
		ButtonAdapter clearFilter = new ButtonAdapter(screen) {
			@Override
			public void onButtonMouseLeftDown(MouseButtonEvent evt, boolean toggled) {
				filter.setText("");
				refilter();
			}
		};
		clearFilter.setText("Clear");
		top.addChild(clearFilter);

		// Types
		Element type = new Element(screen);
		type.setLayoutManager(new MigLayout(screen, "", "[][][]", "[]"));
		bldg = new CheckBox(screen) {
			@Override
			public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
				refilter();
			}
		};
		bldg.setLabelText("Buildings");
		bldg.setIsChecked(true);
		cav = new CheckBox(screen) {
			@Override
			public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
				refilter();
			}
		};
		cav.setLabelText("Caves");
		cav.setIsChecked(true);
		dng = new CheckBox(screen) {
			@Override
			public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
				refilter();
			}
		};
		dng.setLabelText("Dungeon");
		dng.setIsChecked(true);
		type.addChild(bldg);
		type.addChild(cav);
		type.addChild(dng);

		// Models
		buildingsTable = new Table(screen) {
			@Override
			public void onChange() {
				if (!adjust) {
					String n = getSelectedBuildingName();
					if (n != null) {
						rebuildPieces();
						setAvailable();
						selectionChanged();
					}
				}
			}
		};
		buildingsTable.setHeadersVisible(false);
		buildingsTable.addColumn("Model");
		buildingsTable.setColumnResizeMode(Table.ColumnResizeMode.AUTO_ALL);

		// Add
		addButton = new FancyButton(screen) {
			@Override
			public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
			}
		};
		addButton.setText("Add");

		// Remove
		removeButton = new FancyButton(screen) {
			@Override
			public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
			}
		};
		removeButton.setText("Remove");

		// Save
		saveButton = new FancyButton(screen) {
			@Override
			public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
				// try {
				// } catch (IOException ioe) {
				// error("Failed to save environment.", ioe);
				// LOG.log(Level.SEVERE, "Failed to save environment.", ioe);
				// }
			}
		};
		saveButton.setText("Save");

		// Buttons
		Container bottom = new Container(screen);
		bottom.setLayoutManager(new MigLayout(screen, "fill", "[][]push[]", "[]"));
		bottom.addChild(addButton);
		bottom.addChild(removeButton);
		bottom.addChild(saveButton);

		// Tab
		XTabPanelContent tab = new XTabPanelContent(screen);
		tab.setLayoutManager(new MigLayout(screen, "wrap 1", "[fill,grow]", "[][][fill,grow][]"));
		tab.addChild(top);
		tab.addChild(type);
		tab.addChild(buildingsTable);
		tab.addChild(bottom);

		return tab;
	}

	private XTabPanelContent piecesTab() {

		// Pieces
		piecesTable = new Table(screen) {

			@Override
			public void onChange() {
				if (!adjust) {
					pieceChanged();
					setAvailable();
					pieceSelected();
				}
			}
		};
		piecesTable.setHeadersVisible(true);
		piecesTable.addColumn("Name");
		piecesTable.addColumn("ATS");
		piecesTable.setColumnResizeMode(Table.ColumnResizeMode.AUTO_FIRST);
		piecesTable.getColumns().get(1).setWidth(64);

		pieceEditor = new PropertiesPanel<Entity>(screen, prefs);

		// Tab
		XTabPanelContent tab = new XTabPanelContent(screen);
		tab.setLayoutManager(new BorderLayout());
		tab.addChild(piecesTable, BorderLayout.Border.NORTH);
		tab.addChild(pieceEditor, BorderLayout.Border.CENTER);

		return tab;
	}

	private void refilter() {
		if (task != null) {
			task.cancel();
		}
		task = getApp().getAlarm().timed(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				rebuildBuildingsTable();
				return null;
			}
		}, 2.0f);
	}

	private void rebuildBuildingsTable() {
		LOG.info("Rebuilding building table");
		resetBuildingsTable();
		if (bldg.getIsChecked())
			filterRows(((ServerAssetManager) app.getAssetManager()).getAssetNamesMatching("Bldg/Bldg-.*/.*\\.csm.xml"));
		if (cav.getIsChecked())
			filterRows(((ServerAssetManager) app.getAssetManager()).getAssetNamesMatching("Cav/Cav-.*/.*\\.csm.xml"));
		if (dng.getIsChecked())
			filterRows(((ServerAssetManager) app.getAssetManager()).getAssetNamesMatching("Dng/Dng-.*/.*\\.csm.xml"));
		buildingsTable.pack();
		if (!buildingsTable.isAnythingSelected() && buildingsTable.getRowCount() > 0)
			buildingsTable.setSelectedRowIndex(0);
	}

	private void filterRows(final Set<String> xmls) {
		String ft = filter.getText().toLowerCase();
		for (String s : xmls) {
			if (StringUtils.isBlank(ft) || s.toLowerCase().contains(ft))
				addBuildingRow(s, false);
		}
	}

	private void checkAvailableATS() {
		adjust(true);
		try {
			String atsName = atsTab.getATSName();
			atsMeshes.clear();
			for (String s : ((ServerAssetManager) app.getAssetManager())
					.getAssetNamesMatching(String.format("ATS/%s/.*\\.mesh.*", atsName))) {
				String b = FilenameUtils.getName(s);
				if (b.endsWith(".xml")) {
					b = FilenameUtils.getBaseName(b);
				}
				b = FilenameUtils.getBaseName(b);
				atsMeshes.add(b);
			}

			for (TableRow r : piecesTable.getRows()) {
				Entity piece = (Entity) r.getValue();
				String ats = FilenameUtils.getBaseName(piece.getMesh().toString().replace("$(ATS)", atsName));
				TableCell cb = r.getCell(1);
				boolean exists = atsMeshes.contains(ats);
				cb.setText(exists ? "Yes" : "No");
				if (exists) {
					ElementStyle.successColor(screen, cb);
				} else {
					ElementStyle.errorColor(screen, cb);
				}
			}
		} finally {
			adjust(false);
		}
	}

	private void rebuildPieces() {
		LOG.info("Rebuilding pieces table");
		piecesTable.removeAllRows();
		String selectedBuildingName = getSelectedBuildingName();
		String atsName = atsTab.getATSName();
		if (atsName != null) {
			if (selectedBuildingName == null) {
				info("Pick a building in the Building tab to activate the preview");
			} else {
				building = (BuildingXMLEntity) propFactory.getProp(selectedBuildingName + "?ATS=" + atsName);
				if (building != null) {
					for (Entity e : building.getComponent().getEntities()) {
						String mesh = e.getMesh();
						if (!StringUtils.isBlank(mesh)) {
							String atsMesh = mesh.replace("$(ATS)", atsName);
							String baseMeshName = FilenameUtils.getBaseName(atsMesh);
							TableRow row = new TableRow(screen, piecesTable, e);
							Table.TableCell nameCell = new Table.TableCell(screen, baseMeshName, atsMesh);
							row.addChild(nameCell);
							Table.TableCell atsCell = new Table.TableCell(screen, "");
							row.addChild(atsCell);
							piecesTable.addRow(row, false);
						}
					}
				}
			}
		} else {
			if (selectedBuildingName != null) {
				info("Pick a skin in the ATS tab to activate the preview.");
			}
		}
		piecesTable.pack();
		checkAvailableATS();
	}

	private void addBuildingRow(final String buildingComponentPath, boolean pack) {
		// Is there a row for the parent folder?
		String buildingTypePath = FilenameUtils.normalizeNoEndSeparator(FilenameUtils.getPath(buildingComponentPath));
		String buildingType = FilenameUtils.getName(buildingTypePath);
		String buildingName = FilenameUtils.getBaseName(FilenameUtils.getBaseName(buildingComponentPath));

		Table.TableRow parentRow = null;

		for (TableRow r : buildingsTable.getRows()) {
			if (r.getValue().equals(buildingType)) {
				parentRow = (TableRow) r;
			}
		}
		if (parentRow == null) {
			parentRow = new TableRow(screen, buildingsTable, buildingType);
			parentRow.addCell(buildingType, buildingType);
			parentRow.setLeaf(false);
			buildingsTable.addRow(parentRow, pack);
		}

		// Model
		String buildingPath = String.format("%s#%s", buildingType, buildingName);
		final Table.TableCell cell1 = new Table.TableCell(screen, buildingName, buildingPath);
		final Table.TableRow row = new Table.TableRow(screen, buildingsTable, buildingPath);
		row.addChild(cell1);
		row.setToolTipText(Icelib.getBasename(Icelib.getFilename(buildingName)));
		row.setLeaf(true);
		parentRow.addRow(row, pack);

		setAvailable();
	}

	private void adjust(boolean adjust) {
		if (adjust == this.adjust) {
			throw new IllegalStateException("Adjusting already " + adjust);
		}
		this.adjust = adjust;
	}

	private void resetBuildingsTable() {
		buildingsTable.removeAllRows();
		if (building != null) {
			window.setWindowTitle(String.format("Building (%s)", building.getAssetName()));
		} else {
			window.setWindowTitle("Building");
		}
	}

}
