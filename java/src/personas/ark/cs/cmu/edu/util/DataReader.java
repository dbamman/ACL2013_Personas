package personas.ark.cs.cmu.edu.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import personas.ark.cs.cmu.edu.PersonaModel;
import personas.ark.cs.cmu.edu.containers.Doc;
import personas.ark.cs.cmu.edu.containers.Entity;
import personas.ark.cs.cmu.edu.containers.EventArg;
import personas.ark.cs.cmu.edu.containers.EventRole;
import personas.ark.cs.cmu.edu.containers.EventTuple;

public class DataReader {

	/**
	 * Read all data needed for model
	 * 
	 * @param characterMetadata
	 *            Path to file containing metadata about the characters (age,
	 *            gender etc.)
	 * @param movieMetadata
	 *            Path to file containing metadata about the movie (genre)
	 * @param dataFile
	 *            Path to file containing all of the dependency tuples for all
	 *            characters/movies
	 * @param model
	 *            The model to train
	 */
	public static void read(String characterMetadata, String movieMetadata,
			String dataFile, PersonaModel model) {
		readCharacterMetadata(characterMetadata, model);
		readMovieMetadata(movieMetadata, model);
		finalizeFeatures(model);
		read(dataFile, model);
	}
	

	private static void readMovieMetadata(String infile,
			PersonaModel model) {
		model.movieTitles = new HashMap<String, String>();
		model.movieGenres = new HashMap<String, Set<String>>();
		model.genreIdToString = new HashMap<String, String>();

		try {
			BufferedReader in1 = new BufferedReader(new InputStreamReader(
					new FileInputStream(infile), "UTF-8"));
			String str1;
			int maxGenre = 0;

			while ((str1 = in1.readLine()) != null) {
				String[] parts = str1.split("\t");
				String id = parts[0];
				String title = parts[2];
				String genreString = parts[7];

				model.movieTitles.put(id, title);
				try {
					// JSONObject jsonObject =
					// JSONObject.fromObject(genreString);
					JSONObject jsonObject = (JSONObject) JSONValue
							.parse(genreString);
					Set<String> keys = jsonObject.keySet();
					model.movieGenres.put(id, keys);
					for (String key : keys) {
						int genreId = -1;
						if (model.featureIds.containsKey(key)) {
							genreId = model.featureIds.get(key);
						} else {
							genreId = model.numFeatures;
							model.featureIds.put(key, genreId);
							model.genreIdToString.put(key,
									(String) jsonObject.get(key));
							model.numFeatures++;
						}
					}
				} catch (Exception e1) {
					// do nothing
				}

			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*
	 * > quantile(ages$V1, seq(0,1,.1)) 0% 10% 20% 30% 40% 50% 60% 70% 80% 90%
	 * 100% 1 22 26 29 33 36 40 44 50 57 103
	 */
	private static int getAgeBucket(int age) {
		if (age > 57)
			return 9;
		if (age > 50)
			return 8;
		if (age > 44)
			return 7;
		if (age > 40)
			return 6;
		if (age > 36)
			return 5;
		if (age > 33)
			return 4;
		if (age > 29)
			return 3;
		if (age > 26)
			return 2;
		if (age > 22)
			return 1;
		if (age > 5)
			return 0;

		return -1;

	}

	private static int getFeatureId(String key, PersonaModel model) {
		int featureId = -1;

		if (model.featureIds.containsKey(key)) {
			featureId = model.featureIds.get(key);
		} else {
			featureId = model.numFeatures;
			model.featureIds.put(key, featureId);
			model.numFeatures++;
		}
		return featureId;
	}

	private static void readCharacterMetadata(String infile,
			PersonaModel model) {
		model.characterNames = new HashMap<String, String>();
		model.characterFeatures = new HashMap<String, HashSet<Integer>>();
		boolean includeActor = false;
		boolean includeGender = true;
		boolean includeAge = true;

		try {
			BufferedReader in1 = new BufferedReader(new InputStreamReader(
					new FileInputStream(infile), "UTF-8"));
			String str1;

			while ((str1 = in1.readLine()) != null) {
				HashSet<Integer> ids = new HashSet<Integer>();

				String[] parts = str1.split("\t");
				String id = parts[10];
				String name = parts[3];
				model.characterNames.put(id, name);
				if (parts.length >= 13 && includeActor) {
					String actor = parts[12];
					ids.add(getFeatureId("actor:" + actor, model));
				}
				int age = -1;
				try {
					age = Integer.valueOf(parts[9]);
				} catch (Exception e) {
					// do nothing
				}
				String gender = parts[5];

				if (includeGender && (gender.equals("M") || gender.equals("F"))) {
					ids.add(getFeatureId("gender:" + gender, model));
				}
				int ageBucket = getAgeBucket(age);
				if (includeAge && age > -1 && ageBucket >= 0) {
					ids.add(getFeatureId("age:" + ageBucket, model));
				}
				// System.out.println(id + "\t" + gender + "\t" + age + "\t" +
				// getAgeBucket(age) + "\t" + Arrays.toString(ids.toArray()));

				model.characterFeatures.put(id, ids);

			}

			in1.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void read(String infile, PersonaModel model) {
		System.err.println("Reading data...");
		model.data = new ArrayList<Doc>();

		try {
			BufferedReader in1 = new BufferedReader(new InputStreamReader(
					new FileInputStream(infile), "UTF-8"));
			String str1;

			while ((str1 = in1.readLine()) != null) {
				try {
					String[] mainparts = str1.split("\t");
					String movieID = mainparts[0];
					String agentLine = mainparts[1];
					String patientLine = mainparts[2];
					String modifierLine = mainparts[3];
					String entityNameString = mainparts[4];
					String entityFullNameString = mainparts[5];

					// key off entity ID for all of these.
					HashMap<String, Entity> entitiesByEID = new HashMap<String, Entity>();

					Doc doc = new Doc(model.A, model.personaRegression);
					doc.id = movieID;
					if (model.movieTitles.containsKey(movieID)) {
						doc.title = model.movieTitles.get(movieID);
					}

					/*
					 * Read entity head lemmas
					 */

					JSONObject jsonObject = (JSONObject) JSONValue
							.parse(entityNameString);
					for (String e : (Set<String>) jsonObject.keySet()) {
						String name = (String) jsonObject.get(e);
						e = e.toLowerCase();
						Entity entity = null;
						if (entitiesByEID.containsKey(e)) {
							entity = entitiesByEID.get(e);
						} else {
							entity = new Entity(e, model.A);
						}
						entity.setName(name);
						entitiesByEID.put(e, entity);
					}

					/*
					 * Read entity full text mentions
					 */
					jsonObject = (JSONObject) JSONValue
							.parse(entityFullNameString);
					for (String e : (Set<String>) jsonObject.keySet()) {
						// String e = it.next();
						String name = (String) jsonObject.get(e);
						e = e.toLowerCase();
						Entity entity = null;
						if (entitiesByEID.containsKey(e)) {
							entity = entitiesByEID.get(e);
						} else {
							entity = new Entity(e, model.A);
						}
						entity.setFullname(name);
						entitiesByEID.put(e, entity);

					}

					// read the tuple predarg fragments
					HashMap<String, EventTuple> eventsByTID = new HashMap<String, EventTuple>();
					readTupleArgs(EventRole.AGENT, agentLine, entitiesByEID,
							eventsByTID);
					readTupleArgs(EventRole.PATIENT, patientLine,
							entitiesByEID, eventsByTID);
					readModifierArgs(eventsByTID, modifierLine, entitiesByEID);

					if (model.movieGenres.containsKey(movieID)) {
						doc.genres = model.movieGenres.get(movieID);
					}

					doc.entities = entitiesByEID;

					for (EventTuple tuple : eventsByTID.values()) {
						doc.eventTuples.add(tuple);
					}

					model.data.add(doc);

				} catch (Exception e) {
					// e.printStackTrace();
				}

			}
			in1.close();
			System.out.println("done reading");

			// Make the verb vocabulary

			HashMap<String, Integer> verbCounts = new HashMap<String, Integer>();
			for (Doc doc : model.data) {
				for (EventTuple t : doc.eventTuples) {
					String verb = t.getVerbString();
					int count = 0;
					if (verbCounts.containsKey(verb)) {
						count = verbCounts.get(verb);
					}
					count++;
					verbCounts.put(verb, count);
				}
			}

			int threshold = 3;

			ArrayList<Object> sorted = Util.sortHashMapByValue(verbCounts);

			HashMap<String, Integer> verbVocab = new HashMap<String, Integer>();
			HashSet<String> thresholdVerbs = new HashSet<String>();

			int count = 0;
			int offset = 0;
			for (int i = offset; i < sorted.size() && i - offset < model.V; i++) {
				String verb = (String) sorted.get(i);
				int vcount = verbCounts.get(verb);
				if (vcount > threshold) {
					thresholdVerbs.add(verb);
					verbVocab.put(verb, count);
					count++;
				}
			}

			model.reverseVocab = new String[verbVocab.size()];
			model.V = verbVocab.size();
			System.err.println("Active vocab size: " + model.V);

			for (String word : verbVocab.keySet()) {
				int id = verbVocab.get(word);
				model.reverseVocab[id] = word;
			}

			// filter event tuples
			for (Doc doc : model.data) {
				List<EventTuple> keptEvents = Lists.newArrayList();
				for (EventTuple tuple : doc.eventTuples) {
					if (!verbVocab.containsKey(tuple.getVerbString()))
						continue;
					if (tuple.numArgs() == 0)
						continue;
					keptEvents.add(tuple);
					tuple.numberize(verbVocab);
					// U.p(tuple);
				}
				doc.eventTuples = keptEvents;
			}

			// only do these now that the event tuple set is settled.
			makeEntityArgIndexesAndFilter(model);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void readTupleArgs(EventRole role, String argLine,
			Map<String, Entity> entitiesByEID,
			Map<String, EventTuple> eventsByTID) {
		String[] parts = argLine.split(" ");
		for (String word : parts) {
			String[] wparts = word.split(":");
			String eKey = wparts[0].toLowerCase();
			String tupleID = wparts[1];
			String supertag = wparts[2];
			String verb = wparts[3];

			// rel doesn't matter but old rel = "nsubj", new rel = "p:nsubjpass"
			// String rel=wparts[5];

			if (!eventsByTID.containsKey(tupleID)) {
				eventsByTID.put(tupleID, new EventTuple(tupleID, verb));
			}
			EventArg arg = new EventArg(role, entitiesByEID.get(eKey));
			arg.tuple = eventsByTID.get(tupleID);
			arg.tuple.setArg(arg);
		}
	}

	private static void readModifierArgs(Map<String, EventTuple> eventsByTID,
			String argLine, Map<String, Entity> entitiesByEID) {
		String[] parts = argLine.split(" ");
		for (String word : parts) {
			String[] wparts = word.split(":");
			String eKey = wparts[0].toLowerCase();
			String modifierID = wparts[1];
			String supertag = wparts[2];
			String modifierLemma = wparts[3];

			EventTuple et = new EventTuple(modifierID, modifierLemma);
			EventArg arg = new EventArg(EventRole.MODIFIEE,
					entitiesByEID.get(eKey));
			arg.tuple = et;
			arg.tuple.setArg(arg);

			eventsByTID.put(modifierID, et);
		}

	}

	private static void makeEntityArgIndexesAndFilter(
			PersonaModel model) {
		HashMap<String, HashSet<Entity>> entityNames = Maps.newHashMap();

		for (Doc doc : model.data) {
			for (EventTuple t : doc.eventTuples) {
				for (EventArg arg : t.arguments.values()) {
					if (arg.role == EventRole.AGENT) {
						arg.entity.agentArgs.add(arg);
						arg.currentSample = (int) (Math.random() * model.K);
					} else if (arg.role == EventRole.PATIENT) {
						arg.entity.patientArgs.add(arg);
						arg.currentSample = (int) (Math.random() * model.K);
					} else if (arg.role == EventRole.MODIFIEE) {
						arg.entity.modifieeArgs.add(arg);
						arg.currentSample = (int) (Math.random() * model.K);
					}
				}
			}
			HashMap<String, Entity> keptEntities = Maps.newHashMap();

			// only keep resolved entities
			int minActions = 3;

			// make sure to only keep characters as double if they occur in
			// multiple *movies*
			// not the same character in the same movie (e.g., young and old)
			HashSet<String> seenCharacterNames = new HashSet<String>();

			for (Entity e : doc.entities.values()) {
				if ((e.getNumEvents()) >= minActions) {
					// if (e.id.startsWith("/m/") && (e.agentArgs.size() > 0 ||
					// e.patientArgs.size() > 0)) {
					if (model.characterNames.containsKey(e.id)) {
						e.name = model.characterNames.get(e.id).trim();
						if (e.name.matches(".* .*")
								&& !seenCharacterNames.contains(e.name)) {
							HashSet<Entity> es = null;
							if (entityNames.containsKey(e.name)) {
								es = entityNames.get(e.name);
							} else {
								es = new HashSet<Entity>();
							}
							es.add(e);
							entityNames.put(e.name, es);
							seenCharacterNames.add(e.name);
						}
					}
					keptEntities.put(e.id, e);
				}
			}
			doc.entities = keptEntities;
		}


		// set canonical entity Ids and character metadata
		int index = 0;
		int entities = 0;
		int numWithAge = 0;
		int numWithGender = 0;
		int numWithActor = 0;
		for (Doc doc : model.data) {
			for (Entity e : doc.entities.values()) {

				entities++;
				if (model.characterFeatures.containsKey(e.id)) {
					e.characterFeatures = model.characterFeatures.get(e.id);
					for (int f : e.characterFeatures) {
						String name = model.reverseFeatureIds[f];
						if (name.startsWith("gender:")) {
							numWithGender++;
						}
						if (name.startsWith("age:")) {
							numWithAge++;
						}
						if (name.startsWith("actor:")) {
							numWithActor++;
						}
					}
				}
				if (e.getNumEvents() > 0) {
					e.canonicalIndex = index++;
				}
			}
		}
		model.totalEntities = index;
		System.out.println(String.format(
				"Total entities: %s; w/ Age: %s; w/ Gender: %s; w/ Actor %s",
				entities, numWithAge, numWithGender, numWithActor));

	}

	private static void finalizeFeatures(PersonaModel model) {
		model.reverseFeatureIds = new String[model.featureIds.size()];
		for (String key : model.featureIds.keySet()) {
			int index = model.featureIds.get(key);
			model.reverseFeatureIds[index] = key;
		}
	}

}
