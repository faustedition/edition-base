package de.faustedition.model.manuscript;

import java.util.Collection;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import com.google.common.collect.Lists;

import de.faustedition.model.document.ArchiveFacet;
import de.faustedition.model.document.DatingFacet;
import de.faustedition.model.document.DocumentStructureNode;
import de.faustedition.model.document.DocumentStructureNodeFacet;
import de.faustedition.model.document.DocumentStructureNodeType;
import de.faustedition.model.document.FacsimileFacet;
import de.faustedition.model.document.FacsimileFile;
import de.faustedition.model.document.LegacyMetadataFacet;
import de.faustedition.model.document.PrintReferenceFacet;
import de.faustedition.model.document.TranscriptionFacet;
import de.faustedition.model.metadata.MetadataAssignment;
import de.faustedition.model.tei.TEIDocumentManager;
import de.faustedition.util.LoggingUtil;
import de.faustedition.util.XMLUtil;

@Service
public class DataMigrationService
{
	@Autowired
	private SessionFactory sessionFactory;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Autowired
	private TEIDocumentManager teiDocumentManager;

	@Autowired
	private TaskExecutor taskExecutor;

	@PostConstruct
	public void migrate()
	{
		taskExecutor.execute(new Runnable()
		{

			@Override
			public void run()
			{
				new TransactionTemplate(transactionManager).execute(new TransactionCallbackWithoutResult()
				{

					@Override
					protected void doInTransactionWithoutResult(TransactionStatus status)
					{
						Session session = sessionFactory.getCurrentSession();
						if (DocumentStructureNode.existsAny(session))
						{
							return;
						}

						LoggingUtil.LOG.info("Migrating data to document structure model");
						doMigrate(session);
					}
				});
			}
		});
	}

	private void doMigrate(Session session)
	{
		int repositoryCount = 0;
		for (Repository repository : Repository.find(session))
		{
			DocumentStructureNode repositoryNode = new DocumentStructureNode();
			repositoryNode.setNodeType(DocumentStructureNodeType.REPOSITORY);
			repositoryNode.setNodeOrder(repositoryCount++);
			repositoryNode.setName(repository.getName());
			repositoryNode.setParent(null);
			repositoryNode.save(session);
			LoggingUtil.LOG.info("Migrated: " + repositoryNode.toString());

			doMigrate(session, repository, repositoryNode);
		}
	}

	private void doMigrate(Session session, Repository repository, DocumentStructureNode repositoryNode)
	{
		int portfolioCount = 0;
		for (Portfolio portfolio : Portfolio.find(session, repository))
		{
			DocumentStructureNode portfolioNode = new DocumentStructureNode();
			portfolioNode.setNodeType(DocumentStructureNodeType.FILE);
			portfolioNode.setNodeOrder(portfolioCount++);
			portfolioNode.setParent(repositoryNode);
			portfolioNode.setName(portfolio.getName());
			portfolioNode.save(session);
			for (DocumentStructureNodeFacet facet : buildPortfolioFacets(session, repository, portfolio))
			{
				facet.setFacettedNode(portfolioNode);
				facet.save(session);
			}
			LoggingUtil.LOG.info("Migrated: " + portfolioNode.toString());

			doMigrate(session, portfolio, portfolioNode);
			session.flush();
			session.clear();
		}

	}

	private void doMigrate(Session session, Portfolio portfolio, DocumentStructureNode portfolioNode)
	{
		int pageCount = 0;
		for (Manuscript manuscript : Manuscript.find(session, portfolio))
		{
			DocumentStructureNode pageNode = new DocumentStructureNode();
			pageNode.setNodeType(DocumentStructureNodeType.PAGE);
			pageNode.setNodeOrder(pageCount++);
			pageNode.setParent(portfolioNode);
			pageNode.setName(StringUtils.stripStart(manuscript.getName(), "0"));
			pageNode.save(session);

			Facsimile facsimile = Facsimile.find(session, manuscript, manuscript.getName());
			if (facsimile != null)
			{
				FacsimileFile facsimileFile = new FacsimileFile();
				facsimileFile.setPath(facsimile.getImagePath());
				facsimileFile.save(session);

				FacsimileFacet facsimileFacet = new FacsimileFacet();
				facsimileFacet.setFacettedNode(pageNode);
				facsimileFacet.setFacsimileFile(facsimileFile);
				facsimileFacet.save(session);

				Transcription transcription = Transcription.find(session, facsimile);
				if (transcription != null)
				{
					TranscriptionFacet transcriptionFacet = new TranscriptionFacet();
					transcriptionFacet.setFacettedNode(pageNode);
					transcriptionFacet.setCreated(transcription.getCreated());
					transcriptionFacet.setLastModified(transcriptionFacet.getLastModified());
					transcriptionFacet.setDocumentData(XMLUtil.serialize(transcription.buildTEIDocument(teiDocumentManager).getDocument(), false));
					transcriptionFacet.save(session);
				}
			}

		}
	}

	private Collection<DocumentStructureNodeFacet> buildPortfolioFacets(Session session, Repository repository, Portfolio portfolio)
	{
		ArchiveFacet archiveFacet = new ArchiveFacet();
		archiveFacet.setRepository(repository.getName());
		archiveFacet.setCallnumber(portfolio.getName());

		PrintReferenceFacet printReferenceFacet = null;
		LegacyMetadataFacet legacyMetadataFacet = null;
		DatingFacet datingFacet = null;

		for (MetadataAssignment ma : MetadataAssignment.find(session, Portfolio.class.getName(), portfolio.getId()))
		{
			String field = ma.getField();
			if ("callnumber_old".equals(field))
			{
				String[] callnumbers = StringUtils.split(ma.getValue());
				for (int cc = 0; cc < callnumbers.length; cc++)
				{
					callnumbers[cc] = StringUtils.substringBeforeLast(callnumbers[cc].trim(), "*");
				}

				archiveFacet.setLegacyCallnumber("");
				for (String callnumber : callnumbers)
				{
					if (!archiveFacet.getLegacyCallnumber().contains(callnumber))
					{
						archiveFacet.setLegacyCallnumber(archiveFacet.getLegacyCallnumber() + " " + callnumber);
					}
				}
				archiveFacet.setLegacyCallnumber(StringUtils.trimToNull(archiveFacet.getLegacyCallnumber()));
			}
			else if ("id_weimarer_ausgabe".equals(field) || "print_weimarer_ausgabe".equals(field) || "print_weimarer_ausgabe_additional".equals(field))
			{
				String id = normalizeWhitespace(StringUtils.remove(StringUtils.remove("oS", ma.getValue()), "-")).trim();
				if (id.length() > 0)
				{
					if (printReferenceFacet == null)
					{
						printReferenceFacet = new PrintReferenceFacet();
					}
					if (printReferenceFacet.getReferenceWeimarerAusgabe() == null)
					{
						printReferenceFacet.setReferenceWeimarerAusgabe("");
					}
					printReferenceFacet.setReferenceWeimarerAusgabe(StringUtils.trimToNull(printReferenceFacet.getReferenceWeimarerAusgabe() + " " + id));
				}
			}
			else if ("manuscript_reference_weimarer_ausgabe".equals(field))
			{
				if (printReferenceFacet == null)
				{
					printReferenceFacet = new PrintReferenceFacet();
				}
				printReferenceFacet.setManuscriptReferenceWeimarerAusgabe(normalizeWhitespace(ma.getValue()));
			}
			else if ("id_paralipomenon_weimarer_ausgabe".equals(field))
			{
				if (printReferenceFacet == null)
				{
					printReferenceFacet = new PrintReferenceFacet();
				}
				printReferenceFacet.setParalipomenonReferenceWeimarerAusgabe(normalizeWhitespace(ma.getValue()));
			}
			else if ("record_number".equals(field))
			{
				if (legacyMetadataFacet == null)
				{
					legacyMetadataFacet = new LegacyMetadataFacet();
				}
				legacyMetadataFacet.setRecordNumber(normalizeWhitespace(ma.getValue()));
			}
			else if ("hand_1".equals(field) || "hand_4".equals(field) || "hand_7".equals(field))
			{
				if (legacyMetadataFacet == null)
				{
					legacyMetadataFacet = new LegacyMetadataFacet();
				}
				if (legacyMetadataFacet.getHands() == null)
				{
					legacyMetadataFacet.setHands("");
				}
				String value = normalizeWhitespace(ma.getValue());
				if (!legacyMetadataFacet.getHands().contains(value))
				{
					legacyMetadataFacet.setHands(legacyMetadataFacet.getHands() + " " + value);
				}
				legacyMetadataFacet.setHands(StringUtils.trimToNull(legacyMetadataFacet.getHands()));
			}
			else if ("work_genetic_level_goethe".equals(field) || "work_genetic_level_custom".equals(field))
			{
				if (legacyMetadataFacet == null)
				{
					legacyMetadataFacet = new LegacyMetadataFacet();
				}
				if (legacyMetadataFacet.getGeneticLevel() == null)
				{
					legacyMetadataFacet.setGeneticLevel("");
				}
				legacyMetadataFacet.setGeneticLevel(StringUtils.trimToNull(legacyMetadataFacet.getGeneticLevel() + " " + normalizeWhitespace(ma.getValue())));
			}
			else if ("remarks".equals(field))
			{
				if (legacyMetadataFacet == null)
				{
					legacyMetadataFacet = new LegacyMetadataFacet();
				}
				legacyMetadataFacet.setRemarks(normalizeWhitespace(ma.getValue()));
			}
			else if ("dating_normalized".equals(field) || "dating_given".equals(field))
			{
				if (datingFacet == null)
				{
					datingFacet = new DatingFacet();
				}
				if (datingFacet.getRemarks() == null)
				{
					datingFacet.setRemarks("");
				}
				datingFacet.setRemarks(StringUtils.trimToNull(datingFacet.getRemarks() + " " + normalizeWhitespace(ma.getValue())));
			}

		}
		List<DocumentStructureNodeFacet> facets = Lists.newArrayList();
		facets.add(archiveFacet);
		if (printReferenceFacet != null)
		{
			facets.add(printReferenceFacet);
		}
		if (legacyMetadataFacet != null)
		{
			facets.add(legacyMetadataFacet);
		}
		if (datingFacet != null)
		{
			facets.add(datingFacet);
		}
		return facets;
	}

	private static String normalizeWhitespace(String str)
	{
		return str.replaceAll("\\s+", " ");
	}
}
