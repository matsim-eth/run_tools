package ch.ethz.matsim.run_tools.analysis;

import java.util.LinkedList;
import java.util.List;

public class BinnedSampleCollector {
	final private List<Double> right;
	final private List<List<Double>> samples = new LinkedList<>();
	private double total = 0.0;

	public BinnedSampleCollector(List<Double> right) {
		this.right = right;

		for (int i = 0; i < right.size(); i++) {
			samples.add(new LinkedList<>());
		}
	}

	public void addSample(double sample) {
		int index = 0;

		while (index < right.size() && right.get(index) < sample) {
			index++;
		}

		if (index < right.size()) {
			samples.get(index).add(sample);
			total += sample;
		}
	}

	public List<Double> build() {
		List<Double> bins = new LinkedList<>();

		for (List<Double> binSamples : samples) {
			bins.add(binSamples.stream().mapToDouble(d -> d).sum() / total);
		}

		return bins;
	}
}
