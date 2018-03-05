package ch.ethz.matsim.run_tools.analysis.distribution_distances;

import java.util.List;

public interface DistributionDistance {
	double compute(List<Double> referenceBins, List<Double> simulationBins);
}
