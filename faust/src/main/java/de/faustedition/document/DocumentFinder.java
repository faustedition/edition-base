package de.faustedition.document;

import java.util.Deque;
import java.util.logging.Logger;

import org.restlet.resource.ServerResource;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import de.faustedition.xml.XMLStorage;

@Singleton
public class DocumentFinder extends AbstractDocumentFinder {

	private final Provider<DocumentResource> documentResources;

	@Inject
	public DocumentFinder(XMLStorage xml, MaterialUnitManager documentManager, Provider<DocumentResource> documentResources,
			Logger logger) {
		super(xml, documentManager, logger);
		this.documentResources = documentResources;
	}

	@Override
	protected ServerResource getResource(Document document, Deque<String> postfix) {
		// Do not allow for arbitrary postfixes
		if (postfix.size() > 0)
			return null;
		final DocumentResource resource = documentResources.get();
		resource.setDocument(document);
		return resource;
	}

}