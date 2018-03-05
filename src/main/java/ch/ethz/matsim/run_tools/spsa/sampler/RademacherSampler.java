package ch.ethz.matsim.run_tools.spsa.sampler;

import java.util.Random;

public class RademacherSampler implements SPSASampler {
	final private Random random;

	public RademacherSampler(Random random) {
		this.random = random;
	}

	@Override
	public double sample() {
		double r = random.nextDouble();

		if (r < 0.5) {
			return -0.5;
		} else {
			return 0.5;
		}
	}
}
