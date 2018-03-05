package ch.ethz.matsim.run_tools.analysis.distribution_distances;

import java.util.List;

public class HellingerDistance implements DistributionDistance {
	@Override
	public double compute(List<Double> referenceBins, List<Double> simulationBins) {
		double result = 0.0;

		for (int i = 0; i < referenceBins.size(); i++) {
			result += Math.pow(Math.sqrt(simulationBins.get(i)) + Math.sqrt(referenceBins.get(i)), 2.0);
		}

		return Math.sqrt(result) / Math.sqrt(2.0);
	}
}
