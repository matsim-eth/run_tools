package ch.ethz.matsim.run_tools.spsa.projection;

import java.util.List;

public interface SPSAProjection {
	List<Double> projectGradientCandidate(double c, List<Double> perturbation, List<Double> candidate);
	List<Double> projectObjectiveCandidate(List<Double> candidate);
}
