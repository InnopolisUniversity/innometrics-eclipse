package handlers;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IExecutionListener;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.*;
import org.eclipse.ui.commands.ICommandService;

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
		IWorkbenchWindow window = wb.getWorkbenchWindows()[0];
		addCodeListener(window.getActivePage().getActivePart());
		addPageListener(window.getActivePage());
		
		wb.addWindowListener(generateWindowListener());
		checkUnsendData();

		ICommandService service = wb.getService(ICommandService.class);

		IExecutionListener executionListener = new IExecutionListener() {

			@Override
			public void notHandled(String commandId, NotHandledException exception) {
				// TODO Auto-generated method stub

			}

			@Override
			public void postExecuteFailure(String commandId, ExecutionException exception) {
				// TODO Auto-generated method stub

			}

			@Override
			public void postExecuteSuccess(String commandId, Object returnValue) {
				Metric metric = new Metric(commandId, "eclipse_executed_command");
				metric.finish();
				saveMetric(metric);

			}

			@Override
			public void preExecute(String commandId, ExecutionEvent event) {
				// TODO Auto-generated method stub

			}

		};
		service.addExecutionListener(executionListener);
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
					saveMetric(metric);
				}
				codeMetric = new CodeMetric(fileName, code);
			}
		}
		codeMetrics.put(fileName, codeMetric);
	}
	
	private void addPageListener(IWorkbenchPage page) {
		IPartListener2 listener = pageListeners.get(page);
		if (listener == null) {
			listener = generateIPartListener2();
			page.addPartListener(listener);
			pageListeners.put(page, listener);
		}

	}
		
	private String getTextFromFile(ITextEditor source) {
		ITextEditor textEditor = (ITextEditor)source;

		IDocumentProvider provider = textEditor.getDocumentProvider();

		IEditorInput input = textEditor.getEditorInput();

		IDocument document = provider.getDocument(input);

		String text = document.get();

		return text;
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
						String text = getTextFromFile((ITextEditor) source);
						storeNewCodeMetric(title, text);
					}
				}
			};

			if (part instanceof ITextEditor) {
				String text = getTextFromFile((ITextEditor) part);
				storeNewCodeMetric(title, text);
			}
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

		};
	}

}
