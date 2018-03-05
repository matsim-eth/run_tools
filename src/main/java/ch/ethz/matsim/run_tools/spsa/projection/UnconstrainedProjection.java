package ch.ethz.matsim.run_tools.spsa.projection;

import java.util.List;

public class UnconstrainedProjection implements SPSAProjection {
	@Override
	public List<Double> projectGradientCandidate(double c, List<Double> perturbation, List<Double> candidate) {
		return candidate;
	}

	@Override
	public List<Double> projectObjectiveCandidate(List<Double> candidate) {
		return candidate;
	}
}
