package de.faustedition.graph;

import static org.neo4j.graphdb.Direction.OUTGOING;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.faustedition.document.ArchiveCollection;
import de.faustedition.document.MaterialUnitCollection;
import de.faustedition.text.TextCollection;
import de.faustedition.transcript.TranscriptCollection;

@Singleton
public class FaustGraph extends NodeWrapper {
	public static final String PREFIX = "faust";

	private static final RelationshipType ROOT_RT = new FaustRelationshipType("root");
	private static final String ROOT_NAME_PROPERTY = ROOT_RT.name() + ".name";

	private static final String ARCHIVES_ROOT_NAME = PREFIX + ".archives";
	private static final String MATERIAL_UNITS_ROOT_NAME = PREFIX + ".material-units";
	private static final String TRANSCRIPTS_ROOT_NAME = PREFIX + ".transcripts";
	private static final String TEXTS_ROOT_NAME = PREFIX + ".texts";

	private final GraphDatabaseService db;

	@Inject
	public FaustGraph(GraphDatabaseService db) {
		super(db.getReferenceNode());
		this.db = db;
	}

	public GraphDatabaseService getGraphDatabaseService() {
		return db;
	}

	public ArchiveCollection getArchives() {
		return new ArchiveCollection(root(ARCHIVES_ROOT_NAME));
	}

	public TranscriptCollection getTranscripts() {
		return new TranscriptCollection(root(TRANSCRIPTS_ROOT_NAME));
	}

	public MaterialUnitCollection getMaterialUnits() {
		return new MaterialUnitCollection(root(MATERIAL_UNITS_ROOT_NAME));
	}

	public TextCollection getTexts() {
		return new TextCollection(root(TEXTS_ROOT_NAME));
	}

	protected Node root(String rootName) {
		for (Relationship r : node.getRelationships(ROOT_RT, OUTGOING)) {
			if (rootName.equals(r.getProperty(ROOT_NAME_PROPERTY))) {
				return r.getEndNode();
			}
		}

		Relationship r = node.createRelationshipTo(node.getGraphDatabase().createNode(), ROOT_RT);
		r.setProperty(ROOT_NAME_PROPERTY, rootName);
		return r.getEndNode();
	}
}