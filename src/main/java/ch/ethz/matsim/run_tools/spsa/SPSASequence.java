package ch.ethz.matsim.run_tools.spsa;

public interface SPSASequence {
	double getGradientFactor(int n);
	double getPerturbationFactor(int n);
}
