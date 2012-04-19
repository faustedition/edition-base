package de.faustedition.dataimport;

import com.google.common.base.Joiner;
import de.faustedition.FaustURI;
import de.faustedition.Runtime;
import de.faustedition.document.ArchiveManager;
import de.faustedition.document.MaterialUnitManager;
import de.faustedition.genesis.GeneticRelationManager;
import de.faustedition.genesis.MacrogeneticRelationManager;
import de.faustedition.text.TextManager;
import de.faustedition.transcript.TranscriptManager;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;

public class DataImport extends Runtime implements Runnable {

	@Autowired
	private ArchiveManager archiveManager;

	@Autowired
	private TranscriptManager transcriptManager;

	@Autowired
	private MaterialUnitManager documentManager;

	@Autowired
	private TextManager textManager;

	@Autowired
	private Logger logger;

	@Autowired
	private GeneticRelationManager geneticRelationManager;

	@Autowired
	private MacrogeneticRelationManager macrogeneticRelationManager;

	public static void main(String[] args) throws Exception {
		try {
			main(DataImport.class, args);
		} finally {
			System.exit(0);
		}
	}

	@Override
	public void run() {
		logger.info("Importing all data into graph");
		final SortedSet<FaustURI> failed = new TreeSet<FaustURI>();
		final long startTime = System.currentTimeMillis();

		archiveManager.feedGraph();
		failed.addAll(transcriptManager.feedGraph());
		failed.addAll(documentManager.feedGraph());
		failed.addAll(macrogeneticRelationManager.feedGraph());
		failed.addAll(textManager.feedGraph());
		geneticRelationManager.feedGraph();
		

		logger.info(String.format("Import finished in %.3f seconds", (System.currentTimeMillis() - startTime) / 1000.0f));
		if (!failed.isEmpty()) {
			logger.severe("Failed imports:\n" + Joiner.on("\n").join(failed));
		}
	}
}
