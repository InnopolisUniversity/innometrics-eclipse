package metrics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;

public class CodeMetric extends Metric {
	private String source = null;
	
	public CodeMetric(String name, String source) {
		super(name);
		this.source = source;
	}
	
	public List<Metric> finish(String newSource) {
		this.finish();
		List<String> sourceList = new ArrayList<String>(Arrays.asList(source.split("\n")));
		List<String> newSourceList = new ArrayList<String>(Arrays.asList(newSource.split("\n")));
		Patch diff = DiffUtils.diff(sourceList, newSourceList);
		List<Metric> metrics = new ArrayList<>();
		for (Delta delta: diff.getDeltas()) {
			String deltaName = delta.getClass().getName().replace("difflib.", "").replace("Delta", "").toLowerCase();
			Integer oldLinesSize = delta.getOriginal().getLines().size();
			Integer newLinesSize = delta.getRevised().getLines().size();
			Integer amount = Math.max(oldLinesSize, newLinesSize);
			
			if (amount > 0) {
				metrics.add(new Metric(0, tabName, startDate, endDate,
						"eclipse_lines_" + deltaName,
						Integer.toString(amount),
						session));
			}
			metrics.addAll(findComments(delta));
			metrics.addAll(findNewTests(delta));
		}
		return metrics;
	}
	
	private List<Metric> findComments(Delta delta) {
		String[] commentStrings = {"//", "%", "#", "*"};

		List<String> lines = (List<String>) delta.getRevised().getLines();
		int amountAdded = 0;
		for (String line: lines) {
			if (Arrays.stream(commentStrings).anyMatch(line::contains)) {
				amountAdded += 1;
			}
		}
		
		lines = (List<String>) delta.getOriginal().getLines();
		int amountDeleted = 0;
		for (String line: lines) {
			if (Arrays.stream(commentStrings).anyMatch(line::contains)) {
				amountDeleted += 1;
			}
		}
		List<Metric> metrics = new ArrayList<Metric>();
		if (amountAdded > 0) {
			metrics.add(new Metric(0, tabName, startDate, endDate,
					"eclipse_comments_added",
					Integer.toString(amountAdded),
					session));
		}
		if (amountDeleted > 0) {
			metrics.add(new Metric(0, tabName, startDate, endDate,
					"eclipse_comments_deleted",
					Integer.toString(amountDeleted),
					session));
		}
		return metrics;
	}
	
	private List<Metric> findNewTests(Delta delta) {
		String[] testDeclarationStrings = {"@Test", "@Given", "@When", "@Then"};
		
		List<String> lines = (List<String>) delta.getRevised().getLines();
		int amountAdded = 0;
		for (String line: lines) {
			if (Arrays.stream(testDeclarationStrings).anyMatch(line::contains)) {
				amountAdded += 1;
			}
		}
		
		lines = (List<String>) delta.getOriginal().getLines();
		int amountDeleted = 0;
		for (String line: lines) {
			if (Arrays.stream(testDeclarationStrings).anyMatch(line::contains)) {
				amountDeleted += 1;
			}
		}
		List<Metric> metrics = new ArrayList<Metric>();
		if (amountAdded > 0) {
			metrics.add(new Metric(0, tabName, startDate, endDate,
					"eclipse_tests_added",
					Integer.toString(amountAdded),
					session));
		}
		if (amountDeleted > 0) {
			metrics.add(new Metric(0, tabName, startDate, endDate,
					"eclipse_tests_deleted",
					Integer.toString(amountDeleted),
					session));
		}
		return metrics;
	}
}
