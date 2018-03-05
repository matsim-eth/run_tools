package ch.ethz.matsim.run_tools.analysis.distribution_distances;

import java.util.List;

public class KullbackLeiblerDivergence implements DistributionDistance {
	@Override
	public double compute(List<Double> referenceBins, List<Double> simulationBins) {
		double result = 0.0;

		for (int i = 0; i < referenceBins.size(); i++) {
			result += referenceBins.get(i) * Math.log(simulationBins.get(i) / referenceBins.get(i));
		}

		return -result;
	}
}
