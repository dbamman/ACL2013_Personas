package personas.ark.cs.cmu.edu.containers;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;

import edu.stanford.nlp.math.ArrayMath;

public class Doc {

	public String id;
	public String title;
	public Set<String> genres;
	public HashMap<String, Entity> entities;
	public List<EventTuple> eventTuples;

	public double[] prior;

	public int[] currentPersonaSamples;

	public Doc(int A, boolean personaRegression) {
		prior = new double[A];
		Arrays.fill(prior, 1.0);
		ArrayMath.normalize(prior);
		eventTuples = Lists.newArrayList();
		if (!personaRegression) {
			currentPersonaSamples = new int[A];
		}
	}

}
