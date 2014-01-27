package personas.ark.cs.cmu.edu.containers;


public enum EventRole {
	AGENT, PATIENT, MODIFIEE;
	public String toString() {
		if (this==AGENT) return "Agent";
		if (this==PATIENT) return "Patient";
		if (this==MODIFIEE) return "Modifiee";
		assert false;
		return null;
	}
	public EventRole other() {
		assert this==AGENT || this==PATIENT;
		return this==AGENT ? PATIENT : AGENT;
	}
}
