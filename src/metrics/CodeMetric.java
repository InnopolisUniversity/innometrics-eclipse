package metrics;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import difflib.Delta;
import difflib.DiffUtils;
import difflib.InsertDelta;
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
			Integer amount = delta.getRevised().getLines().size();
			if (amount > 0) {
				metrics.add(new Metric(0, tabName, startDate, endDate,
						"eclipse_lines_" + deltaName,
						Integer.toString(amount),
						session));
			}
		}
		return metrics;
	}
}
