package personas.ark.cs.cmu.edu;

import java.util.List;

import personas.ark.cs.cmu.edu.containers.Doc;
import personas.ark.cs.cmu.edu.util.Util;

import com.google.common.base.Function;

import edu.stanford.nlp.math.ArrayMath;

public class HyperparameterOptimization {

	public static void resampleConcs(final PersonaModel model) {
		Function<double[], Double> wLL = new Function<double[], Double>() {
			@Override
			public Double apply(double[] input) {
				return wordLL(Math.exp(input[0]), model);
			}
		};

		Function<double[], Double> LaLL = new Function<double[], Double>() {
			@Override
			public Double apply(double[] input) {
				return LaLL(Math.exp(input[0]), model);
			}
		};

		Function<double[], Double> LpLL = new Function<double[], Double>() {
			@Override
			public Double apply(double[] input) {
				return LpLL(Math.exp(input[0]), model);
			}
		};
		Function<double[], Double> LmLL = new Function<double[], Double>() {
			@Override
			public Double apply(double[] input) {
				// laplace prior (why not...) for numerical stability on 0
				// datapoints
				double x = Math.exp(input[0]);
				return LmLL(x, model) - 1e-6 * x;
			}
		};

		Function<double[], Double> thetaLL = new Function<double[], Double>() {
			@Override
			public Double apply(double[] input) {
				// laplace prior (why not...) for numerical stability on 0
				// datapoints
				double x = Math.exp(input[0]);
				return thetaLL(x, model) - 1e-6 * x;
			}
		};

		
		List<double[]> history = Util.slice_sample(wLL,
				new double[] { Math.log(model.gamma) }, new double[] { 1 }, 30);
		double newGamma = Math.exp(history.get(history.size() - 1)[0]);
		System.out.println(String.format("gamma %.6g -> %.6g", model.gamma,
				newGamma));
		model.gamma = newGamma;

		history = Util.slice_sample(LaLL, new double[] { Math.log(model.nuA) },
				new double[] { 1 }, 30);
		newGamma = Math.exp(history.get(history.size() - 1)[0]);
		System.out.println(String.format("nuA %.6g -> %.6g", model.nuA,
				newGamma));
		model.nuA = newGamma;

		history = Util.slice_sample(LpLL, new double[] { Math.log(model.nuP) },
				new double[] { 1 }, 30);
		newGamma = Math.exp(history.get(history.size() - 1)[0]);
		System.out.println(String.format("nuP %.6g -> %.6g", model.nuP,
				newGamma));
		model.nuP = newGamma;

		history = Util.slice_sample(LmLL, new double[] { Math.log(model.nuM) },
				new double[] { 1 }, 30);
		newGamma = Math.exp(history.get(history.size() - 1)[0]);
		System.out.println(String.format("nuM %.6g -> %.6g", model.nuM,
				newGamma));
		model.nuM = newGamma;

		if (!model.personaRegression) {
			history = Util.slice_sample(thetaLL,
					new double[] { Math.log(model.alpha) },
					new double[] { 1 }, 30);
			newGamma = Math.exp(history.get(history.size() - 1)[0]);
			System.out.println(String.format(
					"alpha %.6g -> %.6g",
					model.alpha, newGamma));
			model.alpha = newGamma;
		}

	}


	public static double totalLL(PersonaModel model) {
		double ll = 0;
		double wordLL = wordLL(model.gamma, model);
		double LaLL = LaLL(model.nuA, model);
		double LpLL = LpLL(model.nuP, model);
		double LmLL = LmLL(model.nuM, model);
		double thetaLL = 0;
		if (!model.personaRegression) {
			thetaLL = thetaLL(model.alpha, model);
		}
		ll += (wordLL + LaLL + LpLL + LmLL + thetaLL);

		return ll;
	}

	public static double wordLL(double newGamma, PersonaModel model) {
		// gamma is the symm measure. V*gamma is the concentration.
		double ll = 0;
		// attributephis
		for (int k = 0; k < model.K; k++) {
			ll += Util.dirmultSymmLogprob(model.phis[k],
					model.phiTotals[k], newGamma);
		}

		return ll;
	}

	public static double LaLL(double newGamma, PersonaModel model) {
		double ll = 0;
		for (int k = 0; k < model.A; k++) {
			ll += Util.dirmultSymmLogprob(model.LAgents[k], model.LAgentTotal[k], newGamma);
		}
		return ll;
	}

	public static double LpLL(double newGamma, PersonaModel model) {
		double ll = 0;
		for (int k = 0; k < model.A; k++) {
			ll += Util.dirmultSymmLogprob(model.LPatients[k], model.LPatientTotal[k],
					newGamma);
		}
		return ll;
	}

	public static double LmLL(double newGamma, PersonaModel model) {
		double ll = 0;
		for (int k = 0; k < model.A; k++) {
			ll += Util.dirmultSymmLogprob(model.LModifiers[k], model.LModifierTotal[k],
					newGamma);
		}
		return ll;
	}

	public static double thetaLL(double newAlpha, PersonaModel model) {
		double ll = 0;

		for (Doc doc : model.data) {
			int sum = ArrayMath.sum(doc.currentPersonaSamples);
			ll += Util.dirmultSymmLogprob(doc.currentPersonaSamples, sum,
					newAlpha);
		}

		return ll;
	}

	
}
