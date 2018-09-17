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
import org.icescene.IcemoonAppState;
import org.icescene.IcesceneApp;
import org.icescene.NodeVisitor;
import org.icescene.NodeVisitor.Visit;
import org.icescene.NodeVisitor.VisitResult;
import org.icescene.assets.Assets;
import org.icescene.build.SelectionManager;
import org.icescene.build.SelectionManager.Listener;
import org.icescene.propertyediting.PropertiesPanel;
import org.icescene.props.Entity;
import org.icescene.props.EntityFactory;
import org.icescene.props.XMLProp;
import org.icescene.props.XRef;
import org.icescene.scene.Buildable;
import org.iceui.controls.ElementStyle;
import org.iceui.controls.TabPanelContent;

import com.jme3.font.BitmapFont.Align;
import com.jme3.font.BitmapFont.VAlign;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

import icemoon.iceloader.ServerAssetManager;
import icetone.controls.buttons.CheckBox;
import icetone.controls.buttons.PushButton;
import icetone.controls.containers.TabControl;
import icetone.controls.containers.TabControl.TabButton;
import icetone.controls.table.Table;
import icetone.controls.table.TableCell;
import icetone.controls.table.TableRow;
import icetone.controls.text.Label;
import icetone.controls.text.TextField;
import icetone.core.BaseElement;
import icetone.core.Size;
import icetone.core.StyledContainer;
import icetone.core.layout.Border;
import icetone.core.layout.BorderLayout;
import icetone.core.layout.mig.MigLayout;
import icetone.core.undo.UndoManager;
import icetone.core.utils.Alarm.AlarmTask;
import icetone.extras.appstates.FrameManagerAppState;
import icetone.extras.windows.PersistentWindow;
import icetone.extras.windows.SaveType;

/**
 */
public class BuildingEditorAppState extends IcemoonAppState<IcemoonAppState<?>>
		implements PreferenceChangeListener, Listener<Buildable, DefaultBuildableControl> {

	private static final Logger LOG = Logger.getLogger(BuildingEditorAppState.class.getName());
	protected PersistentWindow window;
	private EntityFactory propFactory;
	private Table buildingsTable;
	private PushButton addButton;
	private PushButton removeButton;
	private PushButton saveButton;
	private Table builablesTable;
	private boolean adjust;
	private Set<String> atsMeshes = new HashSet<String>();
	private TextField filter;
	private AlarmTask task;
	private XMLProp building;
	private PropertiesPanel<Buildable> buildableEditor;
	private TabControl tabs;
	private ATSTab atsTab;
	private SelectionManager<Buildable, DefaultBuildableControl> selectionManager;
	private Node parentNode;
	private CheckBox bldg;
	private CheckBox cav;
	private CheckBox dng;
	private CheckBox cl;
	private UndoManager undoManager;

	public BuildingEditorAppState(UndoManager undoManager, Preferences pref, EntityFactory propFactory, Assets assets,
			SelectionManager<Buildable, DefaultBuildableControl> selectionManager, Node parentNode) {
		super(pref);
		addPrefKeyPattern(BuildingsConfig.BUILDINGS + ".*");

		this.parentNode = parentNode;
		this.selectionManager = selectionManager;
		this.propFactory = propFactory;
		this.undoManager = undoManager;
	}

	public String getSelectedBuildingName() {
		TableRow row = buildingsTable.getSelectedRow();
		return row == null || !row.isLeaf() ? null : (String) row.getValue();
	}

	public Buildable getSelectedBuilable() {
		TableRow row = builablesTable.getSelectedRow();
		return row == null ? null : (Buildable) row.getValue();
	}

	@Override
	public void postInitialize() {

		assetManager = app.getAssetManager();
		screen = ((IcesceneApp) app).getScreen();

		window = new PersistentWindow(screen, BuildingsConfig.BUILDINGS, 8, VAlign.Top, Align.Left, new Size(600, 520),
				false, SaveType.POSITION_AND_SIZE, prefs) {
			@Override
			protected void onCloseWindow() {
				super.onCloseWindow();
				app.getStateManager().detach(BuildingEditorAppState.this);
			}
		};

		// Window management (if available)
		FrameManagerAppState win = stateManager.getState(FrameManagerAppState.class);
		if (win != null) {
			window.setMinimizable(true);
		}

		// window.setIsResizable(false);
		window.setWindowTitle("Building");

		tabs = new TabControl(screen);
		tabs.addStyleClass("editor-tabs");
		tabs.addTab(new TabButton(screen) {
			{
				setStyleId("building");
			}
		}, buildingTab());
		tabs.addTab(new TabButton(screen) {
			{
				setStyleId("ats");
			}
		}, atsTab = new ATSTab(screen) {
			@Override
			protected void onSelectedChanged() {
				rebuildPieces();
				selectionManager.clearSelection();
				selectionChanged();
			}

		});
		tabs.addTab(new TabButton(screen) {
			{
				setStyleId("pieces");
			}
		}, piecesTab());

		// Content
		final BaseElement windowContent = window.getContentArea();
		windowContent.setLayoutManager(new BorderLayout()); // Top
		windowContent.addElement(tabs, Border.CENTER);

		// Pack and show
		setAvailable();
		window.setResizable(true);
		screen.showElement(window);

		rebuildBuildingsTable();

		//
		selectionManager.addListener(this);
	}

	public XMLProp getSelectedBuilding() {
		return building;
	}

	@Override
	public void selectionChanged(SelectionManager<Buildable, DefaultBuildableControl> selectionManager) {
		adjust = true;
		try {
			Buildable e = selectionManager.getFirstSelectedProp();
			LOG.info(String.format("Selected %s", e));
			if (e == null) {
				builablesTable.runAdjusting(() -> builablesTable.clearSelection());
			} else {
				if (e instanceof Buildable && (!builablesTable.isAnythingSelected()
						|| !e.equals(builablesTable.getSelectedRow().getValue()))) {
					builablesTable.setSelectedRowObjects(Arrays.asList(e));
					builablesTable.scrollToSelected();
				}
			}
		} finally {
			adjust = false;
		}
	}

	@Override
	protected void handlePrefUpdateSceneThread(PreferenceChangeEvent evt) {
	}

	protected void buildableSelected() {

	}

	protected void selectionChanged() {

	}

	protected void refreshPiece() {
		getSelectedBuilding().reload();
	}

	@Override
	protected void onCleanup() {
		super.onCleanup();
		selectionManager.removeListener(this);
		window.setDestroyOnHide(true);
		window.hide();
	}

	private void buildableChanged() {
		Buildable selectedPiece = getSelectedBuilable();
		buildableEditor.setObject(selectedPiece);
		if (selectedPiece == null)
			selectionManager.clearSelection();
		else {
			NodeVisitor v = new NodeVisitor(parentNode);
			v.visit(new Visit() {
				@Override
				public VisitResult visit(Spatial node) {
					DefaultBuildableControl ebc = node.getControl(DefaultBuildableControl.class);
					if (ebc != null) {
						if (ebc.getEntity().equals(selectedPiece)) {
							selectionManager.select(ebc, 0);
							return VisitResult.END;
						}
					}
					return VisitResult.CONTINUE;
				}
			});
		}
	}

	private void setAvailable() {

	}

	private TabPanelContent buildingTab() {

		// Top
		BaseElement top = new BaseElement(screen);
		top.setLayoutManager(new MigLayout(screen, "", "[][fill,grow][]", "[]"));
		top.addElement(new Label("Find:", screen));
		filter = new TextField(screen);
		filter.onKeyboardReleased(evt -> refilter());
		top.addElement(filter);
		PushButton clearFilter = new PushButton(screen);
		clearFilter.onMouseReleased(evt -> {
			filter.setText("");
			refilter();
		});
		clearFilter.setText("Clear");
		top.addElement(clearFilter);

		// Types
		BaseElement type = new BaseElement(screen);
		type.setLayoutManager(new MigLayout(screen, "", "[][][][]", "[]"));
		bldg = new CheckBox(screen);
		bldg.onChange(evt -> refilter());
		bldg.setText("Buildings");
		bldg.setChecked(true);
		cav = new CheckBox(screen);
		cav.onChange(evt -> refilter());
		cav.setText("Caves");
		cav.setChecked(true);
		dng = new CheckBox(screen);
		dng.onChange(evt -> refilter());
		dng.setText("Dungeon");
		dng.setChecked(true);
		cl = new CheckBox(screen);
		cl.onChange(evt -> refilter());
		cl.setText("CL");
		cl.setChecked(true);
		type.addElement(bldg);
		type.addElement(cav);
		type.addElement(dng);
		type.addElement(cl);

		// Models
		buildingsTable = new Table(screen);
		buildingsTable.setHeadersVisible(false);
		buildingsTable.addColumn("Model");
		buildingsTable.setColumnResizeMode(Table.ColumnResizeMode.AUTO_ALL);
		buildingsTable.onChanged(evt -> {
			if (!evt.getSource().isAdjusting()) {
				String n = getSelectedBuildingName();
				if (n != null) {
					rebuildPieces();
					setAvailable();
					selectionChanged();
				}
			}
		});

		// Add
		addButton = new PushButton(screen) {
			{
				setStyleClass("fancy");
			}
		};
		addButton.setText("Add");

		// Remove
		removeButton = new PushButton(screen) {
			{
				setStyleClass("fancy");
			}
		};
		removeButton.setText("Remove");

		// Save
		saveButton = new PushButton(screen) {
			{
				setStyleClass("fancy");
			}
		};
		saveButton.onMouseReleased(evt -> {
			// try {
			// } catch (IOException ioe) {
			// error("Failed to save environment.", ioe);
			// LOG.log(Level.SEVERE, "Failed to save environment.", ioe);
			// }
		});
		saveButton.setText("Save");

		// Buttons
		StyledContainer bottom = new StyledContainer(screen);
		bottom.setLayoutManager(new MigLayout(screen, "fill", "[][]push[]", "[]"));
		bottom.addElement(addButton);
		bottom.addElement(removeButton);
		bottom.addElement(saveButton);

		// Tab
		TabPanelContent tab = new TabPanelContent(screen);
		tab.setLayoutManager(
				new MigLayout(screen, "wrap 1", "[fill,grow]", "[shrink 0][shrink 0][fill,grow][shrink 0]"));
		tab.addElement(top);
		tab.addElement(type);
		tab.addElement(buildingsTable);
		tab.addElement(bottom);

		return tab;
	}

	private TabPanelContent piecesTab() {

		// Pieces
		builablesTable = new Table(screen);
		builablesTable.onChanged(evt -> {
			if (!evt.getSource().isAdjusting()) {
				buildableChanged();
				setAvailable();
				buildableSelected();
			}
		});
		builablesTable.setHeadersVisible(true);
		builablesTable.addColumn("Name");
		builablesTable.addColumn("ATS");
		builablesTable.setColumnResizeMode(Table.ColumnResizeMode.AUTO_FIRST);
		builablesTable.getColumns().get(1).setWidth(64);

		buildableEditor = new PropertiesPanel<Buildable>(screen, prefs, undoManager);

		// Tab
		TabPanelContent tab = new TabPanelContent(screen);
		tab.setLayoutManager(new MigLayout("wrap 1", "[grow, fill]", "[grow,fill][grow,fill]"));
		tab.addElement(builablesTable);
		tab.addElement(buildableEditor);

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
		builablesTable.invalidate();
		resetBuildingsTable();
		if (cl.isChecked())
			filterRows(
					((ServerAssetManager) app.getAssetManager()).getAssetNamesMatching("CL/CL-Candel.*/.*\\.csm.xml"));
		// if (bldg.getIsChecked())
		// filterRows(((ServerAssetManager)
		// app.getAssetManager()).getAssetNamesMatching("Bldg/Bldg-.*/.*\\.csm.xml"));
		// if (cav.getIsChecked())
		// filterRows(((ServerAssetManager)
		// app.getAssetManager()).getAssetNamesMatching("Cav/Cav-.*/.*\\.csm.xml"));
		// if (dng.getIsChecked())
		// filterRows(((ServerAssetManager)
		// app.getAssetManager()).getAssetNamesMatching("Dng/Dng-.*/.*\\.csm.xml"));
		builablesTable.validate();
		if (!buildingsTable.isAnythingSelected() && buildingsTable.getRowCount() > 0)
			buildingsTable.runAdjusting(() -> buildingsTable.setSelectedRowIndex(0));
	}

	private void filterRows(final Set<String> xmls) {
		String ft = filter.getText().toLowerCase();
		for (String s : xmls) {
			if (StringUtils.isBlank(ft) || s.toLowerCase().contains(ft))
				addBuildingRow(s);
		}
	}

	private void checkAvailableATS() {
		adjust(true);
		try {
			String atsName = atsTab.getATSName();
			atsMeshes.clear();
			for (String s : ((ServerAssetManager) app.getAssetManager())
					.getAssetNamesMatching(String.format("ATS/%s/.*\\.mesh.* ", atsName))) {
				String b = FilenameUtils.getName(s);
				if (b.endsWith(".xml")) {
					b = FilenameUtils.getBaseName(b);
				}
				b = FilenameUtils.getBaseName(b);
				atsMeshes.add(b);
			}

			for (TableRow r : builablesTable.getRows()) {
				Buildable b = (Buildable) r.getValue();
				TableCell cb = r.getCell(1);
				if (b instanceof Entity) {
					Entity e = (Entity) b;
					String ats = FilenameUtils.getBaseName(e.getMesh().toString().replace("$(ATS)", atsName));
					boolean exists = atsMeshes.contains(ats);
					cb.setText(exists ? "Yes" : "No");
					if (exists) {
						ElementStyle.successColor(cb);
					} else {
						ElementStyle.errorColor(cb);
					}
				} else
					cb.setText("N/A");
			}
		} finally {
			adjust(false);
		}
	}

	private void rebuildPieces() {
		LOG.info("Rebuilding pieces table");

		builablesTable.invalidate();
		builablesTable.removeAllRows();
		String selectedBuildingName = getSelectedBuildingName();
		String atsName = null;
		if (selectedBuildingName == null) {
			info("Pick a building in the Building tab to activate the preview");
		} else {
			if (!selectedBuildingName.startsWith("CL-")) {
				atsName = atsTab.getATSName();
			}
			building = propFactory
					.getProp(selectedBuildingName + (StringUtils.isBlank(atsName) ? "" : "?ATS=" + atsName));
			if (building != null) {
				for (Entity e : building.getComponent().getEntities()) {
					String mesh = e.getMesh();
					if (!StringUtils.isBlank(mesh)) {
						String atsMesh = mesh.replace("$(ATS)", atsName == null ? "NO_ATS" : atsName);
						String baseMeshName = FilenameUtils.getBaseName(atsMesh);
						TableRow row = new TableRow(screen, builablesTable, e);
						TableCell nameCell = new TableCell(screen, baseMeshName, atsMesh);
						row.addElement(nameCell);
						TableCell atsCell = new TableCell(screen, "");
						row.addElement(atsCell);
						builablesTable.addRow(row);
					}
				}

				for (XRef e : building.getComponent().getXRefs()) {
					TableRow row = new TableRow(screen, builablesTable, e);
					row.addCell(e.getCRef(), e);
					row.addCell("", null);
					builablesTable.addRow(row);
				}
			}
		}
		builablesTable.validate();

		checkAvailableATS();
	}

	private void addBuildingRow(final String buildingComponentPath) {
		// Is there a row for the parent folder?
		String buildingTypePath = FilenameUtils.normalizeNoEndSeparator(FilenameUtils.getPath(buildingComponentPath));
		String buildingType = FilenameUtils.getName(buildingTypePath);
		String buildingName = FilenameUtils.getBaseName(FilenameUtils.getBaseName(buildingComponentPath));

		TableRow parentRow = null;

		for (TableRow r : buildingsTable.getRows()) {
			if (r.getValue().equals(buildingType)) {
				parentRow = (TableRow) r;
			}
		}
		if (parentRow == null) {
			parentRow = new TableRow(screen, buildingsTable, buildingType);
			parentRow.addCell(buildingType, buildingType);
			parentRow.setLeaf(false);
			buildingsTable.addRow(parentRow);
		}

		// Model
		String buildingPath = String.format("%s#%s", buildingType, buildingName);
		final TableCell cell1 = new TableCell(screen, buildingName, buildingPath);
		final TableRow row = new TableRow(screen, buildingsTable, buildingPath);
		row.addElement(cell1);
		row.setToolTipText(Icelib.getBasename(Icelib.getFilename(buildingName)));
		row.setLeaf(true);
		parentRow.addRow(row);

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
