package handlers;

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
	private CodeMetric codeMetric;
	
	@Override
	public void earlyStartup() {
		System.out.println("Started");
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
	
	private void storeNewCodeMetric(String name, String code) {
		if (this.codeMetric == null) {
			this.codeMetric = new CodeMetric(name, code);
		} else {
			List<Metric> metrics = this.codeMetric.finish(code);
			for (Metric metric: metrics) {
				metric.print();
				saveMetric(metric);
			}
			this.codeMetric = new CodeMetric(name, code);
		}
	}

	private IWindowListener generateWindowListener() {
		return new IWindowListener() {
			@Override
			public void windowOpened(IWorkbenchWindow window) {
				System.out.println("Window started");
				IWorkbenchPage activePage = window.getActivePage();
				System.out.flush();
				System.out.flush();
				activePage.addPartListener(generateIPartListener2());
			}

			@Override
			public void windowDeactivated(IWorkbenchWindow window) {
				System.out.println("Window deactivated");
			}

			@Override
			public void windowClosed(IWorkbenchWindow window) {
				System.out.println("Window closed");
			}

			@Override
			public void windowActivated(IWorkbenchWindow window) {
				System.out.println("Window activated");
				IWorkbenchPage activePage = window.getActivePage();
				System.out.println(activePage.getActiveEditor().getEditorInput().getName());
				System.out.println(activePage.getActivePart().getTitle());
				activePage.getActivePart().addPropertyListener(new IPropertyListener() {

					@Override
					public void propertyChanged(Object source, int propId) {
						// TODO Auto-generated method stub
						System.out.println("Property changed");
						System.out.println(source);
						if (source instanceof ITextEditor)
						 {
						   ITextEditor textEditor = (ITextEditor)source;

						   IDocumentProvider provider = textEditor.getDocumentProvider();

						   IEditorInput input = textEditor.getEditorInput();

						   IDocument document = provider.getDocument(input);

						   String text = document.get();

						   storeNewCodeMetric("bla", text);
						 }
					}
					
				});
				activePage.addPartListener(generateIPartListener2());
			}
		};
	}

	private IPartListener2 generateIPartListener2() {
		return new IPartListener2() {

			@Override
			public void partOpened(IWorkbenchPartReference partRef) {
				System.out.println("Part Opened");
				
				System.out.println(partRef.getTitle());
				storeNewMetric(partRef.getTitle());
			}

			@Override
			public void partInputChanged(IWorkbenchPartReference partRef) {
				System.out.println("Part Input Changed");
				//checkPart(partRef);
			}

			@Override
			public void partVisible(IWorkbenchPartReference partRef) {
				System.out.println("Part visible");
			}

			@Override
			public void partHidden(IWorkbenchPartReference partRef) {
				System.out.println("Part hidden");
				System.out.println(partRef.getTitle());
			}

			@Override
			public void partDeactivated(IWorkbenchPartReference partRef) {
				System.out.println("Part deactivated");
				System.out.println(partRef.getTitle());
			}

			@Override
			public void partClosed(IWorkbenchPartReference partRef) {
				System.out.println("Part closed");
				System.out.println(partRef.getTitle());
			}

			@Override
			public void partBroughtToTop(IWorkbenchPartReference partRef) {
				System.out.println("Part Brought to top");
			}

			@Override
			public void partActivated(IWorkbenchPartReference partRef) {
				System.out.println("Part Activated");
				System.out.println(partRef.getTitle());
				storeNewMetric(partRef.getTitle());
			}
		};
	}
}
