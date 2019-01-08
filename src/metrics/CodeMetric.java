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
			Metric commentsMetric = findComments(delta);
			if (commentsMetric != null) {
				metrics.add(commentsMetric);
			}
		}
		return metrics;
	}
	
	private Metric findComments(Delta delta) {
		String[] commentStrings = {"//", "%", "#", "*"};

		Integer oldLinesSize = delta.getOriginal().getLines().size();
		Integer newLinesSize = delta.getRevised().getLines().size();
		
		List<String> lines = (List<String>) delta.getRevised().getLines();
		int amount = 0;
		for (int i = oldLinesSize; i < newLinesSize; i++) {
			String line = lines.get(i);
			if (Arrays.stream(commentStrings).anyMatch(line::contains)) {
				amount += 1;
			}
			
		}
		if (amount > 0) {
			return new Metric(0, tabName, startDate, endDate,
					"eclipse_comments_added",
					Integer.toString(amount),
					session);
		}
		return null;
	}
}
