package org.icebuildings;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.icelib.Icelib;
import org.icelib.XDesktop;
import org.icescene.FileMonitor;
import org.icescene.FileMonitor.Monitor;
import org.icescene.HUDMessageAppState;
import org.icescene.HUDMessageAppState.Channel;
import org.icescene.IcesceneApp;
import org.iceui.controls.ElementStyle;
import org.iceui.controls.ImageTableCell;
import org.iceui.controls.TabPanelContent;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.math.Vector2f;
import com.jme3.texture.Texture;

import icemoon.iceloader.IndexItem;
import icemoon.iceloader.ServerAssetManager;
import icetone.controls.buttons.CheckBox;
import icetone.controls.buttons.PushButton;
import icetone.controls.containers.Frame;
import icetone.controls.extras.Indicator;
import icetone.controls.table.Table;
import icetone.controls.table.TableCell;
import icetone.controls.table.TableRow;
import icetone.controls.text.Label;
import icetone.controls.text.TextField;
import icetone.core.BaseElement;
import icetone.core.BaseScreen;
import icetone.core.Orientation;
import icetone.core.StyledContainer;
import icetone.core.ToolKit;
import icetone.core.layout.ScreenLayoutConstraints;
import icetone.core.layout.mig.MigLayout;
import icetone.core.utils.Alarm;
import icetone.core.utils.Alarm.AlarmTask;
import icetone.extras.windows.InputBox;

public class ATSTab extends TabPanelContent {
	final static Logger LOG = Logger.getLogger(ATSTab.class.getName());

	final static String[] PREVIEW_IMAGE_PATTERNS = { "${name}.png", "${name}-walls.png", "${name}-interior.png",
			"${name}-exterior.png", "${name}1.png" };

	private Table atsTable;
	private TextField filter;
	private PushButton cloneATSButton;
	private PushButton dirButton;
	private AlarmTask task;
	private File cloningATSDir;
	private boolean cloning;
	private Thread cloneThread;
	private Monitor monitor;
	private CheckBox bldg;
	private CheckBox cav;
	private CheckBox dng;

	public ATSTab(BaseScreen screen) {
		super(screen);

		// Models
		atsTable = new Table(screen);
		// atsTable.setHeadersVisible(false);
		atsTable.addColumn("Preview");
		atsTable.addColumn("Name");
		atsTable.setColumnResizeMode(Table.ColumnResizeMode.AUTO_LAST);
		atsTable.getColumns().get(0).setWidth(96);

		// Types
		BaseElement type = new BaseElement(screen);
		type.setLayoutManager(new MigLayout(screen, "", "[][][]", "[]"));
		bldg = new CheckBox(screen);
		bldg.onMouseReleased(evt -> refilter());
		bldg.setText("Buildings");
		bldg.setChecked(true);

		cav = new CheckBox(screen);
		cav.onChange(evt -> refilter());
		cav.setChecked(true);
		cav.setText("Caves");
		dng = new CheckBox(screen);
		dng.onChange(evt -> refilter());
		dng.setChecked(true);
		dng.setText("Dungeon");
		type.addElement(bldg);
		type.addElement(cav);
		type.addElement(dng);

		// Top
		BaseElement top = new BaseElement(screen);
		top.setLayoutManager(new MigLayout(screen, "", "[][fill,grow][]", "[]"));
		top.addElement(new Label("Find:", screen));
		filter = new TextField(screen);
		filter.onKeyboardReleased(evt -> refilter());
		top.addElement(filter);
		top.addElement(new PushButton(screen, "Clear").onMouseReleased(evt -> {
			filter.setText("");
			refilter();
		}));

		// Add
		cloneATSButton = new PushButton(screen, "Clone") {
			{
				setStyleClass("fancy");
			}
		};
		cloneATSButton.onMouseReleased(evt -> askForCloneName(getATSName()));
		cloneATSButton.setText("Clone");

		// Save
		dirButton = new PushButton(screen) {
			{
				setStyleClass("fancy");
			}
		};
		dirButton.onMouseReleased(evt -> {
			File f = getATSFolder();
			try {
				XDesktop.getDesktop().open(f);
			} catch (Exception ex) {
				LOG.log(Level.SEVERE, String.format("Failed to open folder %s", f), ex);
				error(String.format("Failed to open folder %s", f), ex);
			}
		});
		dirButton.setText("Folder");
		dirButton.setToolTipText("Open the folder where the local copy of this ATS is stored.");

		// Buttons
		StyledContainer bottom = new StyledContainer(screen);
		bottom.setLayoutManager(new MigLayout(screen, "fill", "[]push[]", "[]"));
		bottom.addElement(cloneATSButton);
		bottom.addElement(dirButton);

		setLayoutManager(new MigLayout(screen, "wrap 1", "[fill,grow]", "[shrink 0][shrink 0][fill,grow][shrink 0]"));
		addElement(top);
		addElement(type);
		addElement(atsTable);
		addElement(bottom);

		atsTable.onChanged(evt -> {
			atsSelected();
			onSelectedChanged();
			setAvailable();
		});
		new Thread("LoadATS") {
			@Override
			public void run() {
				loadTable(true);
			}
		}.start();
	}

	public boolean isCloning() {
		return cloning;
	}

	public String getATSName() {
		TableRow row = atsTable.getSelectedRow();
		return row == null ? null : (String) row.getValue();
	}

	protected void atsSelected() {
		if (monitor != null) {
			monitor.stop();
			monitor = null;
		}
		if (atsTable.isAnythingSelected()) {
			File dir = getATSFolder();
			if (dir.exists()) {
				try {
					monitor = ((IcesceneApp) ToolKit.get().getApplication()).getMonitor().monitorDirectory(dir,
							new FileMonitor.Listener() {
								private Alarm.AlarmTask s;

								public void fileUpdated(final File file) {
									triggerReload(file);
								}

								public void fileCreated(File file) {
									triggerReload(file);
								}

								public void fileDeleted(File file) {
									triggerReload(file);
								}

								private void triggerReload(final File file) {
									// We can easily get multiple update events,
									// so
									// defer the reload
									if (s != null) {
										s.cancel();
									}
									s = ((IcesceneApp) ToolKit.get().getApplication()).getAlarm()
											.timed(new Callable<Void>() {
												public Void call() throws Exception {
													LOG.info(String.format("ATS folder %s changed", file));
													((ServerAssetManager) ToolKit.get().getApplication()
															.getAssetManager()).clearCache();
													refilter();
													onSelectedChanged();
													return null;
												}
											}, 1);
								}
							});
				} catch (IOException e) {
					LOG.log(Level.SEVERE, "Failed to monitor diretory.", e);
				}
			}
		}
	}

	protected void onSelectedChanged() {
	}

	private void askForCloneName(final String sourceTemplate) {
		final InputBox dialog = new InputBox(screen, new Vector2f(15, 15), true) {
			{
				setStyleClass("large");
			}

			@Override
			public void onButtonCancelPressed(MouseButtonEvent evt, boolean toggled) {
				hide();
			}

			@Override
			public void onButtonOkPressed(MouseButtonEvent evt, String text, boolean toggled) {
				// Check the name doesn't already exist
				File f = ((IcesceneApp) ToolKit.get().getApplication()).getAssets()
						.getExternalAssetFile(String.format("ATS/%s", text));
				if (f.exists()) {
					ToolKit.get().getApplication().getStateManager().getState(HUDMessageAppState.class)
							.message(Channel.ERROR, String.format("The ATS %s already exists locally."));
					// hideWindow();
				} else {
					if (!text.startsWith("ATS-")) {
						String err = String.format("The name must begin with 'ATS-'.");
						error(err);

					} else {
						hide();
						cloneATS(sourceTemplate, text.replace(" ", "").replace("/", "").replace("\\", ""));
					}
				}
			}
		};
		dialog.setDestroyOnHide(true);
		ElementStyle.warningColor(dialog.getDragBar());
		dialog.setWindowTitle("New Clone");
		dialog.setButtonOkText("Clone");
		dialog.setMsg(getATSName() + " Copy");
		dialog.setResizable(false);
		dialog.setMovable(false);
		dialog.setWidth(300);
		dialog.setModal(true);
		screen.showElement(dialog, ScreenLayoutConstraints.center);
	}

	private void error(String err) {
		ToolKit.get().getApplication().getStateManager().getState(HUDMessageAppState.class).message(Channel.ERROR, err);
	}

	private void error(String err, Exception t) {
		ToolKit.get().getApplication().getStateManager().getState(HUDMessageAppState.class).message(Channel.ERROR, err,
				t);
	}

	private void cloneATS(String sourceTemplate, String newATSName) {
		final String newATSDir = String.format("ATS/%s", newATSName);
		LOG.info(String.format("New ATS directory will be %s", newATSDir));
		cloningATSDir = new File(((IcesceneApp) ToolKit.get().getApplication()).getAssets().getExternalAssetsFolder(),
				newATSDir.replace('/', File.separatorChar));
		final Set<IndexItem> atsAssets = ((ServerAssetManager) ToolKit.get().getApplication().getAssetManager())
				.getAssetsMatching(String.format("ATS/%s/.*", sourceTemplate));

		long totalSize = 0;
		for (IndexItem it : atsAssets) {
			long size = it.getSize();
			totalSize += size;
		}

		if (totalSize == 0) {
			error("There is nothing to clone. I am confused :\\");
			return;
		}

		final boolean useFileCount = true;
		final long fTotalSize = totalSize;

		LOG.info(String.format("Will clone %d bytes of ATS %s", totalSize, sourceTemplate));

		final Label progressTitle = new Label("Cloneing ..", screen);
		final Frame w = new Frame(screen, Vector2f.ZERO, false) {
			{
				setStyleClass("large");
			}
		};

		w.getContentArea().setLayoutManager(new MigLayout(screen, "fill, wrap 1", "[]", "[][]"));
		w.setResizable(false);
		w.setMovable(false);
		w.setWindowTitle("Cloneing");
		w.getContentArea().addElement(progressTitle, "growx, wrap");
		final Indicator overallProgress = new Indicator(screen, Orientation.HORIZONTAL);
		overallProgress.setMaxValue(useFileCount ? atsAssets.size() : 100);
		overallProgress.setCurrentValue(0);
		w.setDestroyOnHide(true);
		w.getContentArea().addElement(overallProgress, "shrink 0, growx, wrap");
		w.sizeToContent();
		w.setWidth(200);
		screen.addElement(w);
		screen.showElement(w, ScreenLayoutConstraints.center);

		cloning = true;
		setAvailable();

		cloneThread = new Thread("Clone" + sourceTemplate) {
			@Override
			public void run() {
				final ThreadLocal<Long> total = new ThreadLocal<Long>();
				total.set(0l);
				try {
					int index = 0;
					for (IndexItem i : atsAssets) {
						if (useFileCount) {
							final int fi = ++index;
							ToolKit.get().getApplication().enqueue(new Callable<Void>() {
								public Void call() throws Exception {
									overallProgress.setCurrentValue(fi);
									return null;
								}
							});
						}
						LOG.info("Cloning " + i.getName());
						final AssetInfo inf = ToolKit.get().getApplication().getAssetManager()
								.locateAsset(new AssetKey<String>(i.getName()));
						ToolKit.get().getApplication().enqueue(new Callable<Void>() {
							public Void call() throws Exception {
								progressTitle.setText(Icelib.getFilename(inf.getKey().getName()));
								return null;
							}
						});
						InputStream in = inf.openStream();
						try {
							String name = i.getName();

							String basename = Icelib.getFilename(name);
							if (basename.toLowerCase().startsWith(sourceTemplate.toLowerCase() + ".")) {
								name = newATSDir + "/" + newATSName + basename.substring(sourceTemplate.length());
							} else if (basename.toLowerCase().startsWith(sourceTemplate.toLowerCase() + "-")) {
								name = newATSDir + "/" + newATSName + basename.substring(sourceTemplate.length());
							} else {
								LOG.warning(String.format("Unsure how to rename file %s in %s", basename, newATSDir));
								name = newATSDir + "/" + basename;
							}

							File outputFile = Icelib.makeParent(((IcesceneApp) ToolKit.get().getApplication())
									.getAssets().getExternalAssetFile(name));
							OutputStream out = new BufferedOutputStream(new FileOutputStream(outputFile), 65536);
							if (!useFileCount) {
								out = new FilterOutputStream(out) {

									private long lastUpdate = -1;

									@Override
									public void write(int b) throws IOException {
										super.write(b);
										total.set(total.get() + 1);
										updateProgress();
									}

									@Override
									public void write(byte[] b, int off, int len) throws IOException {
										super.write(b, off, len);
										total.set(total.get() + len);
										updateProgress();
									}

									@Override
									public void close() throws IOException {
										super.close();
										showUpdate(overallProgress, total);
										;
									}

									private void updateProgress() throws IOException {
										if (Thread.interrupted()) {
											throw new IOException(new InterruptedException());
										}
										long now = System.currentTimeMillis();
										if (lastUpdate == -1 || now > lastUpdate + 100) {
											lastUpdate = now;
											showUpdate(overallProgress, total);
										}
									}

									private void showUpdate(final Indicator overallProgress,
											final ThreadLocal<Long> total) {
										final long fTot = total.get();
										ToolKit.get().getApplication().enqueue(new Callable<Void>() {
											public Void call() throws Exception {
												double d = (double) fTot / (double) fTotalSize;
												float pc = (float) d * 100f;
												overallProgress.setCurrentValue(pc);
												return null;
											}
										});
									}
								};
							}
							try {
								IOUtils.copy(in, out);
							} finally {
								out.close();
							}

							// Post processing of files
							if (name.endsWith(".material")) {
								String material = IOUtils.toString(outputFile.toURI());
								material = Icelib.replaceAll("material " + sourceTemplate, "material " + newATSName,
										material, true);
								material = Icelib.replaceAll("texture " + sourceTemplate + "-",
										"texture " + newATSName + "-", material, true);
								FileOutputStream fos = new FileOutputStream(outputFile);
								try {
									IOUtils.write(material, fos);
								} finally {
									fos.close();
								}
							} else if (name.endsWith(".mesh.xml")) {
								String xml = IOUtils.toString(outputFile.toURI());
								xml = Icelib.replaceAll("<texture alias=\"Diffuse\" name=\"" + sourceTemplate + "-",
										"<texture alias=\"Diffuse\" name=\"" + newATSName + "-", xml, true);
								xml = Icelib.replaceAll("<texture alias=\"Diffuse\" name=\"" + sourceTemplate + ".",
										"<texture alias=\"Diffuse\" name=\"" + newATSName + ".", xml, true);
								FileOutputStream fos = new FileOutputStream(outputFile);
								try {
									IOUtils.write(xml, fos);
								} finally {
									fos.close();
								}
							}
						} finally {
							in.close();
						}
					}

					// Re-index
					((ServerAssetManager) ToolKit.get().getApplication().getAssetManager()).index();

					cloning = false;
					ToolKit.get().getApplication().enqueue(new Callable<Void>() {
						public Void call() throws Exception {
							setAvailable();
							w.hide();
							refilter();
							return null;
						}
					});
				} catch (Exception e) {
					cloning = false;
					if (e.getCause() instanceof InterruptedException) {
						LOG.warning("Interrupted.");
					} else {
						error("Failed to clone terrain.", e);
						LOG.log(Level.SEVERE, "Failed to clone.", e);
					}
					clearUpClonedDirectory();
					ToolKit.get().getApplication().enqueue(new Callable<Void>() {
						public Void call() throws Exception {
							setAvailable();
							w.hide();
							return null;
						}
					});
				} finally {
				}
			}
		};
		cloneThread.setPriority(Thread.MIN_PRIORITY);
		cloneThread.start();

	}

	private void refilter() {
		if (task != null) {
			task.cancel();
		}
		task = ((IcesceneApp) screen.getApplication()).getAlarm().timed(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				loadTable(false);
				return null;
			}
		}, 2.0f);
	}

	private void loadTable(boolean queueUpdate) {
		final String selected = getATSName();
		if (queueUpdate) {
			try {
				ToolKit.get().getApplication().enqueue(new Callable<Void>() {
					public Void call() throws Exception {
						resetTable();
						return null;
					}
				}).get();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		} else {
			resetTable();
		}

		String ft = filter.getText().toLowerCase();
		for (String s : atsNames()) {

			if (StringUtils.isBlank(ft) || s.toLowerCase().contains(ft)) {
				addATSRow(s, queueUpdate);
			}
		}

		if (queueUpdate) {
			try {
				ToolKit.get().getApplication().enqueue(new Callable<Void>() {
					public Void call() throws Exception {
						select(selected);
						return null;
					}
				}).get();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		} else {
			select(selected);
		}
	}

	private void select(String atsName) {
		atsTable.setSelectedRowObjects(Arrays.asList(atsName));
		if (!atsTable.isAnythingSelected())
			selectFirstRow();
		else
			atsTable.scrollToRow(atsTable.getSelectedRowIndex());
	}

	private void selectFirstRow() {
		if (!atsTable.isAnythingSelected()) {
			atsTable.setSelectedRowIndex(0);
			atsTable.scrollToRow(0);
		}
	}

	private Set<String> atsNames() {
		final Set<String> atsNames = new LinkedHashSet<String>();
		for (String s : ((ServerAssetManager) ToolKit.get().getApplication().getAssetManager())
				.getAssetNamesMatching("ATS/ATS-.*/.*\\.png")) {
			atsNames.add(FilenameUtils.normalizeNoEndSeparator(FilenameUtils.getPath(s)));
		}
		return atsNames;
	}

	private void addATSRow(final String atsPath, boolean queue) {
		// Model
		String name = FilenameUtils.getName(atsPath);
		if ((name.startsWith("ATS-Dungeon") && dng.isChecked())
				|| (name.startsWith("ATS-Cave") && cav.isChecked())
				|| (!name.startsWith("ATS-Dungeon") && !name.startsWith("ATS-Cave") && bldg.isChecked())) {
			String prevImg = findPreviewImage(name);
			final Texture tex = prevImg == null ? null
					: ToolKit.get().getApplication().getAssetManager().loadTexture(prevImg);
			if (queue) {
				((IcesceneApp) ToolKit.get().getApplication()).run(new Runnable() {
					public void run() {
						doAdd(atsPath, tex);
					}
				});
			} else {
				doAdd(atsPath, tex);
			}
		}

	}

	private void doAdd(String atsPath, Texture tex) {
		String atsName = FilenameUtils.getName(atsPath);
		TableRow row = new TableRow(screen, atsTable, atsName);
		ImageTableCell cell1 = new ImageTableCell(screen, atsName, 96);
		if (tex != null) {
			cell1.setImageTexture(tex);
		}
		cell1.setMaxDimensions(cell1.getPreferredDimensions());
		TableCell cell2 = new TableCell(screen, atsName, atsName);
		if (getATSFolder(atsName).isDirectory())
			ElementStyle.successColor(cell2);
		row.addElement(cell1);
		row.addElement(cell2);
		row.setToolTipText(atsPath);
		atsTable.addRow(row);
		setAvailable();
	}

	private void setAvailable() {
		dirButton.setEnabled(atsTable.isAnythingSelected() && getATSFolder().isDirectory());
		cloneATSButton.setEnabled(atsTable.isAnythingSelected());
	}

	private File getATSFolder() {
		return getATSFolder(getATSName());
	}

	private File getATSFolder(String atsName) {
		return atsName == null ? null
				: ((IcesceneApp) ToolKit.get().getApplication()).getAssets()
						.getExternalAssetFile(String.format("ATS/%s", atsName));
	}

	private String findPreviewImage(String atsName) {
		for (String s : ((ServerAssetManager) ToolKit.get().getApplication().getAssetManager())
				.getAssetNamesMatching(String.format("ATS/%s/.*\\.png", atsName))) {
			String b = FilenameUtils.getName(s);
			for (String p : PREVIEW_IMAGE_PATTERNS) {
				if (b.toLowerCase().matches(p.replace("${name}", atsName.toLowerCase()))) {
					return s;
				}
			}

		}
		return null;
	}

	private void resetTable() {
		atsTable.removeAllRows();
	}

	protected void clearUpClonedDirectory() {
		if (cloningATSDir != null) {
			try {
				LOG.info(String.format("Clearing up partially cloned directory %s", cloningATSDir));
				FileUtils.deleteDirectory(cloningATSDir);
			} catch (IOException ex) {
				LOG.log(Level.SEVERE,
						String.format("Failed to clearing up partially cloned directory.%", cloningATSDir));
			} finally {
				cloningATSDir = null;
			}

		}
	}
}
