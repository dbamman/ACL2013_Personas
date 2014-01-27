package personas.ark.cs.cmu.edu.util;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

import edu.stanford.nlp.math.ArrayMath;
import edu.stanford.nlp.math.SloppyMath;

public class Util {

	static DecimalFormat df = new DecimalFormat("0.000");

	public static void safeNormalize(double[] arr) {
		double s = ArrayMath.sum(arr);
		if (s != 0) {
			ArrayMath.multiplyInPlace(arr, 1.0 / s);
		}
	}

	// sample from an unnormalized log distribution
	public static int sampleLogDomain(double[] arr) {
		double[] vals = new double[arr.length];
		vals[0] = arr[0];
		double running_total = arr[0];

		for (int i = 1; i < arr.length; i++) {
			running_total = add_log(running_total, arr[i]);
			vals[i] = running_total;
		}

		return sample_log_domain(vals, vals.length, running_total);
	}

	public static double add_log(double logA, double logB) {
		if (logA < logB) {
			return logB + Math.log(1 + Math.exp(logA - logB));
		} else {
			return logA + Math.log(1 + Math.exp(logB - logA));
		}
	}

	public static int sample_log_domain(double arr[], int max_index,
			double log_max) {
		Random rm = new Random();
		double rnd_value = Math.log(rm.nextDouble());
		rnd_value += log_max;
		for (int i = 0; i < max_index; i++) {

			if (rnd_value <= arr[i]) {
				return i;
			}
		}
		return -1;
	}

	public static double logSumOfExponentials(double[] xs) {
		if (xs.length == 1)
			return xs[0];
		double maximum = -Double.MAX_VALUE;
		for (int i = 0; i < xs.length; i++) {
			if (xs[i] > maximum) {
				maximum = xs[i];
			}
		}

		double max = maximum;
		double sum = 0.0;
		for (int i = 0; i < xs.length; ++i)
			if (xs[i] != Double.NEGATIVE_INFINITY)
				sum += java.lang.Math.exp(xs[i] - max);
		return max + java.lang.Math.log(sum);
	}

	public static ArrayList<Object> sortHashMapByValue(HashMap<?, ?> hm) {

		ArrayList<Object> sortedCollocates = new ArrayList<Object>();

		Set entries2 = hm.entrySet();

		Map.Entry[] entries = new Map.Entry[entries2.size()];

		Iterator<Map.Entry> it = entries2.iterator();
		int n = 0;
		while (it.hasNext()) {
			entries[n] = it.next();
			n++;
		}

		Arrays.sort(entries, new Comparator() {
			public int compare(Object lhs, Object rhs) {
				Map.Entry le = (Map.Entry) lhs;
				Map.Entry re = (Map.Entry) rhs;
				return ((Comparable) re.getValue()).compareTo((Comparable) le
						.getValue());
			}
		});

		for (int i = 0; i < entries.length; i++) {
			Map.Entry<Object, Integer> entry = entries[i];
			sortedCollocates.add(entry.getKey());
		}

		return sortedCollocates;

	}

	/**
	 * Sample from unnormalized discrete distribution
	 * 
	 * @param weights
	 *            is unnormalized
	 * @param psum
	 *            is sum(weights)
	 * @return \in [0,K)
	 **/
	public static int sampleSimple(double[] weights, double psum) {
		assert !ArrayMath.hasNaN(weights) && !ArrayMath.hasInfinite(weights)
				&& ArrayMath.countNegative(weights) == 0;
		double runningTotal = 0;
		double r = Math.random() * psum;
		for (int i = 0; i < weights.length; i++) {
			runningTotal += weights[i];
			if (r <= runningTotal) {
				return i;
			}
		}
		throw new RuntimeException("bad sampling weights");
	}

	/**
	 * Sample from unnormalized discrete distribution
	 * 
	 * @return \in [0,K)
	 **/
	public static int sampleSimple(double[] weights) {
		return sampleSimple(weights, ArrayMath.sum(weights));
	}

	public static double dirmultSymmLogprob(int[] countvec, double alpha_symm) {
		int N = ArrayMath.sum(countvec);
		return dirmultSymmLogprob(countvec, N, alpha_symm);
	}

	public static double dirmultSymmLogprob(double[] countvec, double alpha_symm) {
		double N = ArrayMath.sum(countvec);
		return dirmultSymmLogprob(countvec, N, alpha_symm);
	}

	/**
	 * Single-path version. For a symmetric Dirichlet. DM1(x|a) = G(A)/G(A+N)
	 * \prod_k G(a_k + x_k) / G(a_k) where A=sum(a_k), N=sum(x_k)
	 * 
	 * @param alpha_symm
	 *            : the a_k measure param (symm so same for all k), i.e. mean
	 *            times concentration
	 **/
	public static double dirmultSymmLogprob(int[] countvec, int countsum,
			double alpha_symm) {
		int K = countvec.length;
		int N = countsum;
		double A = alpha_symm * K; // concentration
		// lG(A) - lG(A+N) + sum_k lG(a_k + n_k) - sum_k lG(a_k)
		return -pochhammer(A, N) + faster_lgamma_sum(countvec, alpha_symm) - K
				* SloppyMath.lgamma(alpha_symm);
	}

	/** double version is COPY AND PASTE AND HACK **/
	public static double dirmultSymmLogprob(double[] countvec, double countsum,
			double alpha_symm) {
		int K = countvec.length;
		int N = (int) Math.round(countsum); // TODO bad
		double A = alpha_symm * K; // concentration
		// lG(A) - lG(A+N) + sum_k lG(a_k + n_k) - sum_k lG(a_k)
		return -pochhammer(A, N) + faster_lgamma_sum(countvec, alpha_symm) - K
				* SloppyMath.lgamma(alpha_symm);
	}

	/**
	 * Quickly compute \sum_k lgamma(countvec + alphascalar) where many entries
	 * of countvec are zero -- sparse data but densely represented.
	 * theoretically we could cache other low-count values also, but hard to get
	 * a speedup same as: scipy.special.gammaln(countvec + alpha_symm).sum()
	 **/
	public static double faster_lgamma_sum(int[] countvec, double alpha_symm) {
		double zeroval = SloppyMath.lgamma(alpha_symm);
		int ii, xx;
		int N = countvec.length;
		double ss = 0;
		for (ii = 0; ii < N; ii++) {
			xx = countvec[ii];
			if (xx == 0) {
				ss += zeroval;
			} else {
				ss += SloppyMath.lgamma(xx + alpha_symm);
			}
		}
		return ss;
	}

	/** double version is COPY AND PASTE AND HACK **/
	public static double faster_lgamma_sum(double[] countvec, double alpha_symm) {
		double zeroval = SloppyMath.lgamma(alpha_symm);
		int ii;
		double xx;
		int N = countvec.length;
		double ss = 0;
		for (ii = 0; ii < N; ii++) {
			xx = countvec[ii];
			if (xx == 0) {
				ss += zeroval;
			} else {
				ss += SloppyMath.lgamma(xx + alpha_symm);
			}
		}
		return ss;
	}

	public static void lgammaSumTest(String[] args) {
		int N = 10000000;
		int[] nums = new int[N];

		for (int i = 0; i < N; i++) {
			if (Math.random() < 0.3)
				nums[i] = 0;
			else
				nums[i] = (int) Math.round(Math.random() * 10);
		}
		for (int itr = 0; itr < 100; itr++) {
			faster_lgamma_sum(nums, 3.2);
		}
	}

	static class PHState {
		final static int CACHE_SIZE = 200;
		static double cache_x = -1;
		static double[] cache_v = new double[CACHE_SIZE];
		static int max_cached;
	}

	/**
	 * adapted from Tom Minka's lightspeed library Requires: n >= 0
	 **/
	public static double pochhammer_fancy(double x, int n) {
		double result;
		int i;
		/* the maximum n for which we have a cached value */
		if (n == 0)
			return 0;
		if (n > PHState.CACHE_SIZE) {
			if (x >= 1.e4 * n) {
				return Math.log(x) + (n - 1) * Math.log(x + n / 2);
			}
			return SloppyMath.lgamma(x + n) - SloppyMath.lgamma(x);
		}
		if (x != PHState.cache_x) {
			PHState.max_cached = 1;
			PHState.cache_v[0] = Math.log(x);
			PHState.cache_x = x;
		}
		if (n <= PHState.max_cached)
			return PHState.cache_v[n - 1];
		result = PHState.cache_v[PHState.max_cached - 1];
		x = x + PHState.max_cached - 1;
		for (i = PHState.max_cached; i < n; i++) {
			x = x + 1;
			result += Math.log(x);
			PHState.cache_v[i] = result;
		}
		PHState.max_cached = n;
		return result;
	}

	static double pochhammer_slow(double x, int n) {
		return SloppyMath.lgamma(x + n) - SloppyMath.lgamma(x);
	}

	static double pochhammer(double x, int n) {
		return pochhammer_slow(x, n);
	}

	static void pochhammerTest() {
		int N = 700;
		for (int i = 0; i < N; i++)
			for (int j = 0; j < N; j++)
				System.out.println(String.format("%d %d %.3f\n", i, j,
						pochhammer(i, j)));
	}

	// public static void main(String args[]) {pochhammerTest();}

	/**
	 * Slice sampling (Neal 2003; MacKay 2003, sec. 29.7)
	 * 
	 * logdist: log-density function of target distribution initial: initial
	 * state (D-dim vector) widths: step sizes for expanding the slice (D-dim
	 * vector)
	 * 
	 * This is my port of Iain Murray's
	 * http://homepages.inf.ed.ac.uk/imurray2/teaching/09mlss/slice_sample.m
	 * which in turn derives from MacKay. Murray notes where he found bugs in
	 * MacKay's pseudocode... good sign
	 **/
	public static List<double[]> slice_sample(
			Function<double[], Double> logdist, double[] initial,
			double[] widths, int niter) {
		boolean step_out = true;
		final int D = initial.length;
		assert widths.length == D;

		double[] state = initial;
		double log_Px = logdist.apply(state);

		List<double[]> history = Lists.newArrayList();

		for (int itr = 0; itr < niter; itr++) {
			// U.pf("Slice iter %d stats %s log_Px %f\n",itr,
			// Arrays.toString(state), log_Px);
			// if (itr%100==0) { U.pf("."); System.out.flush(); }
			double log_uprime = Math.log(Math.random()) + log_Px;

			// # Sweep through axes
			for (int dd = 0; dd < D; dd++) {
				double[] x_l = Arrays.copyOf(state, D), x_r = Arrays.copyOf(
						state, D), xprime = Arrays.copyOf(state, D);
				// # Create a horizontal interval (x_l, x_r) enclosing xx
				double r = Math.random();
				x_l[dd] = state[dd] - r * widths[dd];
				x_r[dd] = state[dd] + (1 - r) * widths[dd];
				if (step_out) {
					while (logdist.apply(x_l) > log_uprime)
						x_l[dd] -= widths[dd];
					while (logdist.apply(x_r) > log_uprime)
						x_r[dd] += widths[dd];
				}
				// # Inner loop:
				// # Propose xprimes and shrink interval until good one is
				// found.
				double zz = 0;
				while (true) {
					zz += 1;
					xprime[dd] = Math.random() * (x_r[dd] - x_l[dd]) + x_l[dd];
					log_Px = logdist.apply(xprime);
					if (log_Px > log_uprime) {
						break;
					} else {
						if (xprime[dd] > state[dd]) {
							x_r[dd] = xprime[dd];
						} else if (xprime[dd] < state[dd]) {
							x_l[dd] = xprime[dd];
						} else {
							assert false : "BUG, shrunk to current position and still not acceptable";
						}
					}
				}
				state[dd] = xprime[dd];
			}
			history.add(Arrays.copyOf(state, D));
		}
		return history;
	}

	static double triangleLP(double x) {
		boolean in_tri = 0 < x && x < 20;
		if (!in_tri)
			return -1e100;
		double p = (x * (x < 10 ? 1 : 0) + (20 - x) * (x >= 10 ? 1 : 0)) / 5;
		return Math.log(p);
	}

	/*
	 * Visual testing. Would be nice to Cook-Gelman-Rubin-style QQplot against
	 * truth. > x=read.table(pipe("grep SLICE out"))$V2 > plot(x) > acf(x) >
	 * plot(table(round(x)))
	 */
	static void triangleTest() {
		Function<double[], Double> logdist = new Function<double[], Double>() {
			@Override
			public Double apply(double[] input) {
				return triangleLP(input[0]);
			}
		};
		List<double[]> history = slice_sample(logdist, new double[] { 5 },
				new double[] { 1 }, 10000);
		for (double[] h : history) {
			System.out.println(String.format(("SLICE " + h[0])));
		}
	}

	// public static void main(String[] args) { triangleTest(); }

	public static int[] intRange(int n) {
		int[] ret = new int[n];
		for (int i = 0; i < n; i++) {
			ret[i] = i;
		}
		return ret;
	}

	public static int[] intCol(int[][] matrix, int col) {
		int[] ret = new int[matrix.length];
		for (int i = 0; i < matrix.length; i++) {
			ret[i] = matrix[i][col];
		}
		return ret;
	}

	public static String printSortedArray(double[] array) {
		StringBuffer buffer = new StringBuffer();
		HashMap<Integer, Double> hash = new HashMap<Integer, Double>();
		for (int j = 0; j < array.length; j++) {
			hash.put(j, array[j]);
		}
		ArrayList<Object> sorted = Util.sortHashMapByValue(hash);
		for (int k = 0; k < sorted.size(); k++) {
			int index = (Integer) sorted.get(k);
			buffer.append(df.format(hash.get(index)) + ":" + index);
			if (k < array.length - 1) {
				buffer.append(" ");
			}
		}
		return buffer.toString();
	}

}
