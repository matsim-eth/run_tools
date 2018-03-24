package ch.ethz.matsim.run_tools.analysis;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class BinnedSampleCollector {
	final private List<Double> right;
	final private List<Double> samples = new LinkedList<>();

	public BinnedSampleCollector(List<Double> right) {
		this.right = right;

		for (int i = 0; i < right.size(); i++) {
			samples.add(0.0);
		}

		samples.add(0.0);
	}

	public void addSample(double sample) {
		int index = 0;

		while (index < right.size() && right.get(index) < sample) {
			index++;
		}

		if (index < samples.size()) {
			samples.set(index, samples.get(index) + 1.0);
		}
	}

	public List<Double> buildAbsoluteFrequencies() {
		return samples;
	}

	public List<Double> buildRelativeFrequencies() {
		double total = samples.stream().mapToDouble(d -> d).sum();
		return samples.stream().map(d -> d / total).collect(Collectors.toList());
	}
}
