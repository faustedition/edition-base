package de.faustedition.reasoning;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

public class InscriptionRelationTest {

	private Inscription inscriptionA;
	private Inscription inscriptionB;
	private Inscription inscriptionC;
	private Inscription inscriptionD;
	
	@org.junit.Before
	public void setup() {
		this.inscriptionA = new Inscription(Arrays.asList(
				5, 6, 7), "A");
		this.inscriptionB = new Inscription(Arrays.asList(
				6, 7, 8), "B");
		this.inscriptionC = new Inscription(Arrays.asList(
				8, 9, 10), "C");
		this.inscriptionD = new Inscription(Arrays.asList(
				4, 9, 10), "D");

	}

	@Test
	public void testAreParadigmaticallyRelated() {
		assertTrue(InscriptionRelations.areParadigmaticallyRelated(this.inscriptionA, this.inscriptionB));
		assertFalse(InscriptionRelations.areParadigmaticallyRelated(this.inscriptionB, this.inscriptionC));
	}
	
	@Test
	public void testSyntagmaticallyPrecedes() {
		assertTrue(InscriptionRelations.syntagmaticallyPrecedesByAverage(this.inscriptionA, this.inscriptionB));
		assertTrue(InscriptionRelations.syntagmaticallyPrecedesByAverage(this.inscriptionA, this.inscriptionC));
		assertTrue(InscriptionRelations.syntagmaticallyPrecedesByAverage(this.inscriptionB, this.inscriptionC));

		assertFalse(InscriptionRelations.syntagmaticallyPrecedesByAverage(this.inscriptionC, this.inscriptionC));
		assertFalse(InscriptionRelations.syntagmaticallyPrecedesByAverage(this.inscriptionC, this.inscriptionA));
	}
	
	@Test
	public void testExclusivelyContains() {
		assertTrue(InscriptionRelations.covers(this.inscriptionD, this.inscriptionA));
		assertFalse(InscriptionRelations.covers(this.inscriptionB, this.inscriptionA));
	}
}
