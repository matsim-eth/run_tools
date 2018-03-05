package ch.ethz.matsim.run_tools.analysis.distribution_distances;

import java.util.List;

public class L2Norm implements DistributionDistance {
	@Override
	public double compute(List<Double> referenceBins, List<Double> simulationBins) {
		double value = 0.0;

		for (int i = 0; i < referenceBins.size(); i++) {
			value += Math.pow(referenceBins.get(i) - simulationBins.get(i), 2.0);
		}

		return Math.sqrt(value);
	}
}
