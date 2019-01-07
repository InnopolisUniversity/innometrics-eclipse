package handlers;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.*;
import db.Database;
import metrics.CodeMetric;
import metrics.Metric;
import server.Server;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

public class EclipseLoggerStartup extends AbstractUIPlugin implements IStartup {
	
	private Metric metric;
	private HashMap<String, CodeMetric> codeMetrics = new HashMap<String, CodeMetric>();
	private HashMap<IWorkbenchPart, IPropertyListener> partListeners = new HashMap<IWorkbenchPart, IPropertyListener>();
	private HashMap<IWorkbenchPage, IPartListener2> pageListeners = new HashMap<IWorkbenchPage, IPartListener2>();
	
	@Override
	public void earlyStartup() {
		sendOfflineData();
		IWorkbench wb = PlatformUI.getWorkbench();
		wb.addWindowListener(generateWindowListener());
		checkUnsendData();
	}
	
	private void checkUnsendData() {
		final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
	    executorService.scheduleAtFixedRate(
	    		new Runnable() {
	    	        @Override
	    	        public void run() {
	    				sendOfflineData();
	    	        }},
	    		0, 100, TimeUnit.SECONDS);
	}
	
	private void saveMetric(Metric metric) {
		Database db = Database.getDB();
		db.insertNewMetric(metric);
	}
	
	private void sendOfflineData() {
		Database db = Database.getDB();
		List<Metric> metrics = db.getMetrics();
		if (metrics.isEmpty()) {
			return;
		}
		if (Server.getInstance().sendMetrics(metrics)) {
			db.deleteMetrics(metrics);
		}
		
	}
	
	private void storeNewMetric(String name) {
		if (this.metric == null) {
			this.metric = new Metric(name);
		} else {
			this.metric.finish();
			saveMetric(metric);
			this.metric = new Metric(name);
		}
	}
	
	private void stopMetric() {
		if (this.metric != null) {
			this.metric.finish();
			saveMetric(metric);
			this.metric = null;
		}
		
	}
	
	private void storeNewCodeMetric(String fileName, String code) {
		CodeMetric codeMetric = codeMetrics.get(fileName);
		if (codeMetric == null) {
			codeMetric = new CodeMetric(fileName, code);
		} else {
			List<Metric> metrics = codeMetric.finish(code);
			if (metrics.size() > 0) {
				for (Metric metric: metrics) {
					metric.print();
					saveMetric(metric);
				}
				codeMetric = new CodeMetric(fileName, code);
			}
		}
		codeMetrics.put(fileName, codeMetric);
	}

	private IWindowListener generateWindowListener() {
		return new IWindowListener() {
			@Override
			public void windowOpened(IWorkbenchWindow window) {
				addCodeListener(window.getActivePage().getActivePart());
				addPageListener(window.getActivePage());
			}

			@Override
			public void windowDeactivated(IWorkbenchWindow window) {
				stopMetric();
			}

			@Override
			public void windowClosed(IWorkbenchWindow window) {
				stopMetric();
			}

			@Override
			public void windowActivated(IWorkbenchWindow window) {
				addCodeListener(window.getActivePage().getActivePart());
				addPageListener(window.getActivePage());
			}
			
			private void addPageListener(IWorkbenchPage page) {
				IPartListener2 listener = pageListeners.get(page);
				if (listener == null) {
					listener = generateIPartListener2();
					page.addPartListener(listener);
					pageListeners.put(page, listener);
				}
				
			}
			
			private void addCodeListener(IWorkbenchPart part) {
				String title = part.getTitle();

				IPropertyListener listener = partListeners.get(part);
				if (listener == null) {
					listener = new IPropertyListener() {

						@Override
						public void propertyChanged(Object source, int propId) {
							if (source instanceof ITextEditor)
							 {
							   ITextEditor textEditor = (ITextEditor)source;

							   IDocumentProvider provider = textEditor.getDocumentProvider();

							   IEditorInput input = textEditor.getEditorInput();

							   IDocument document = provider.getDocument(input);

							   String text = document.get();

							   storeNewCodeMetric(title, text);
							 }
						}
					};
					part.addPropertyListener(listener);
					partListeners.put(part, listener);
				}
			}
			
			private IPartListener2 generateIPartListener2() {
				return new IPartListener2() {

					@Override
					public void partOpened(IWorkbenchPartReference partRef) {
						storeNewMetric(partRef.getTitle());
						addCodeListener(partRef.getPart(true));
					}

					@Override
					public void partInputChanged(IWorkbenchPartReference partRef) {
					}

					@Override
					public void partVisible(IWorkbenchPartReference partRef) {
					}

					@Override
					public void partHidden(IWorkbenchPartReference partRef) {
					}

					@Override
					public void partDeactivated(IWorkbenchPartReference partRef) {
					}

					@Override
					public void partClosed(IWorkbenchPartReference partRef) {
					}

					@Override
					public void partBroughtToTop(IWorkbenchPartReference partRef) {
					}

					@Override
					public void partActivated(IWorkbenchPartReference partRef) {
						storeNewMetric(partRef.getTitle());
						addCodeListener(partRef.getPart(true));
					}
				};
			}
		};
	}

}
