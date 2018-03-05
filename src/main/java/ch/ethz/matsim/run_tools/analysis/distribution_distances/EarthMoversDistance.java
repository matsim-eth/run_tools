package ch.ethz.matsim.run_tools.analysis.distribution_distances;

import java.util.LinkedList;
import java.util.List;

public class EarthMoversDistance implements DistributionDistance {
	@Override
	public double compute(List<Double> referenceBins, List<Double> simulationBins) {
		List<Double> emd = new LinkedList<>();
		emd.add(0.0);

		for (int i = 1; i < referenceBins.size() + 1; i++) {
			emd.add(simulationBins.get(i - 1) + emd.get(i - 1) - referenceBins.get(i - 1));
		}

		return emd.stream().mapToDouble(d -> d).map(Math::abs).sum();
	}
}
