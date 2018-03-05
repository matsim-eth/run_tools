package ch.ethz.matsim.run_tools.analysis.distribution_distances;

import java.util.List;

public class L1Norm implements DistributionDistance {
	@Override
	public double compute(List<Double> referenceBins, List<Double> simulationBins) {
		double value = 0.0;

		for (int i = 0; i < referenceBins.size(); i++) {
			value += Math.abs(referenceBins.get(i) - simulationBins.get(i));
		}
		
		return value;
	}
}
