package com.mycsense.carbondb.architecture;

import com.hp.hpl.jena.rdf.model.*;
import com.mycsense.carbondb.domain.Reference;

import java.util.ArrayList;
import java.util.HashMap;

public class ReferenceRepo extends AbstractRepo {

    protected HashMap<String, Reference> refCache;

    public ReferenceRepo(Model model) {
        super(model);
        refCache = new HashMap<>();
    }

    public ArrayList<Reference> getReferences(Resource resource) {
        ArrayList<Reference> references = new ArrayList<>();

        StmtIterator i = resource.listProperties(Datatype.hasReference);
        while (i.hasNext()) {
            Statement stmt = i.next();
            references.add(getReference(stmt.getResource()));
        }

        return references;
    }

    protected Reference getReference(Resource referenceResource) {
        if (refCache.containsKey(referenceResource.getURI())) {
            String title = getStringForProperty(referenceResource, Datatype.title),
                    source = getStringForProperty(referenceResource, Datatype.source),
                    URL = getStringForProperty(referenceResource, Datatype.URL),
                    creator = getStringForProperty(referenceResource, Datatype.creator),
                    publisher = getStringForProperty(referenceResource, Datatype.publisher),
                    date = getStringForProperty(referenceResource, Datatype.date),
                    shortName = getStringForProperty(referenceResource, Datatype.shortName);

            refCache.put(referenceResource.getURI(), new Reference(title, source, URL, creator, publisher, date, referenceResource.getURI(), shortName));
        }
        return refCache.get(referenceResource.getURI());
    }

    protected String getStringForProperty(Resource resource, Property property) {
        if (resource.hasProperty(property)
                && resource.getProperty(property) != null) {
            return resource.getProperty(property).getString();
        }
        return null;
    }

}