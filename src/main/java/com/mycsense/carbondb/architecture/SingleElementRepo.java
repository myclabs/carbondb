package com.mycsense.carbondb.architecture;

import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.vocabulary.RDF;
import com.mycsense.carbondb.*;
import com.mycsense.carbondb.domain.*;
import com.mycsense.carbondb.domain.Process;

import java.util.*;

public class SingleElementRepo extends AbstractRepo {

    protected UnitsRepo unitsRepo;
    protected HashMap<String, Resource> processesResourceCache;
    protected HashMap<String, Resource> coefficientsResourceCache;
    protected HashMap<String, Process> processesCache;
    protected HashMap<String, Coefficient> coefficientsCache;
    protected HashMap<String, ElementaryFlowType> flowTypesCache;

    public SingleElementRepo(Model model, UnitsRepo unitsRepo) {
        super(model);
        this.unitsRepo = unitsRepo;
        processesResourceCache = new HashMap<>();
        coefficientsResourceCache = new HashMap<>();
        processesCache = new HashMap<>();
        coefficientsCache = new HashMap<>();
        flowTypesCache = new HashMap<>();
    }

    public Process getProcess(Resource processResource) throws NoUnitException {
        if (!processesCache.containsKey(processResource.getURI())) {
            Unit unit = RepoFactory.unitsRepo.getUnit(processResource);
            Process process = new Process(getElementKeywords(processResource), unit);
            getElementaryFlowsForProcess(process, processResource);
            getImpactsForProcess(process, processResource);
            processesCache.put(processResource.getURI(), process);
        }
        return processesCache.get(processResource.getURI());
    }

    public Coefficient getCoefficient(Resource coefficientResource) throws NoUnitException {
        if (!coefficientsCache.containsKey(coefficientResource.getURI())) {
            Unit unit = RepoFactory.unitsRepo.getUnit(coefficientResource);
            Double conversionFactor = unit.getConversionFactor();
            Value value = new Value(coefficientResource.getProperty(Datatype.value).getDouble() / conversionFactor,
                    getUncertainty(coefficientResource));
            Coefficient coefficient = new Coefficient(getElementKeywords(coefficientResource), unit, value);
            coefficientsCache.put(coefficientResource.getURI(), coefficient);
        }
        return coefficientsCache.get(coefficientResource.getURI());
    }

    public ArrayList<Process> getProcesses() {
        ArrayList<Process> processes = new ArrayList<>();

        ResIterator i = model.listSubjectsWithProperty(RDF.type, Datatype.SingleProcess);
        while (i.hasNext()) {
            Resource resource = i.next();
            try {
                processes.add(getProcess(resource));
            } catch (NoUnitException e) {
                log.warn(e.getMessage());
            }
        }

        return processes;
    }

    public ArrayList<Coefficient> getCoefficients() {
        ArrayList<Coefficient> coefficients = new ArrayList<>();

        ResIterator i = model.listSubjectsWithProperty(RDF.type, Datatype.SingleCoefficient);
        while (i.hasNext()) {
            Resource resource = i.next();
            try {
                coefficients.add(getCoefficient(resource));
            } catch (NoUnitException e) {
                log.warn(e.getMessage());
            }
        }

        return coefficients;
    }

    protected Dimension getElementKeywords(Resource elementResource)
    {
        Selector selector = new SimpleSelector(elementResource, Datatype.hasTag, (RDFNode) null);
        StmtIterator iter = model.listStatements(selector);

        Dimension dim = new Dimension();
        if (iter.hasNext()) {
            while (iter.hasNext()) {
                Statement s = iter.nextStatement();
                Keyword keyword = new Keyword(getId(s.getObject().asResource()));
                keyword.setLabel(getLabelOrURI(s.getObject().asResource()));
                dim.add(keyword);
            }
        }
        return dim;
    }

    protected HashMap<String, ElementaryFlow> getElementaryFlowsForProcess(Process process, Resource resource)
    {
        Double conversionFactor = process.getUnit().getConversionFactor();
        HashMap<String, ElementaryFlow> flows = new HashMap<>();
        StmtIterator iter = resource.listProperties(Datatype.hasFlow);
        while (iter.hasNext()) {
            Resource emission = iter.nextStatement().getResource();
            if (emission.hasProperty(Datatype.hasElementaryFlowType) && null != emission.getProperty(Datatype.hasElementaryFlowType)
                    && emission.hasProperty(Datatype.value) && null != emission.getProperty(Datatype.value)) {
                String typeId = getId(emission.getProperty(Datatype.hasElementaryFlowType).getResource());
                Value value = new Value(emission.getProperty(Datatype.value).getDouble() / conversionFactor, getUncertainty(emission));
                try {
                    ElementaryFlowType flowType = CarbonOntology.getInstance().getElementaryFlowType(typeId);
                    process.addInputFlow(new ElementaryFlow(flowType, value));
                } catch (NotFoundException | AlreadyExistsException e) {
                    log.warn(e.getMessage());
                }

            }
        }
        return flows;
    }

    protected HashMap<String, Impact> getImpactsForProcess(Process process, Resource resource)
    {
        HashMap<String, Impact> impacts = new HashMap<>();
        StmtIterator iter = resource.listProperties(Datatype.hasImpact);

        while (iter.hasNext()) {
            Resource emission = iter.nextStatement().getResource();
            if (emission.hasProperty(Datatype.hasImpactType) && null != emission.getProperty(Datatype.hasImpactType)
                    && emission.hasProperty(Datatype.value) && null != emission.getProperty(Datatype.value)) {
                String typeId = getId(emission.getProperty(Datatype.hasImpactType).getResource());
                Value value = new Value(emission.getProperty(Datatype.value).getDouble(), getUncertainty(emission));

                try {
                    ImpactType impactType = CarbonOntology.getInstance().getImpactType(typeId);
                    process.addImpact(new Impact(impactType, value));
                } catch (AlreadyExistsException | NotFoundException e) {
                    log.warn(e.getMessage());
                }
            }
        }
        return impacts;
    }

    public Resource writeProcess(Process process)
    {
        Resource processResource = model.createResource(Datatype.getURI() + "sp/" + process.getId())
                .addProperty(RDF.type, Datatype.SingleProcess);
        processResource.addProperty(Datatype.hasUnit, model.createResource(process.getUnit().getId()));
        for (Keyword keyword: process.getKeywords().keywords) {
            Resource keywordResource = model.createResource(keyword.getId());
            processResource.addProperty(Datatype.hasTag, keywordResource);
        }
        for (ElementaryFlow flow : process.getCalculatedFlows().values()) {
            writeCalculatedElementaryFlow(processResource, flow);
        }
        for (Impact impact : process.getImpacts().values()) {
            writeImpact(processResource, impact);
        }
        return processResource;
    }

    protected void writeCalculatedElementaryFlow(Resource process, ElementaryFlow flow)
    {
        process.addProperty(Datatype.hasCalculatedFlow,
                model.createResource(Datatype.getURI() + AnonId.create().toString())
                        .addProperty(Datatype.hasElementaryFlowType, model.createResource(Datatype.getURI() + flow.getType().getId()))
                        .addProperty(Datatype.value, model.createTypedLiteral(flow.getValue().value))
                        .addProperty(Datatype.uncertainty, model.createTypedLiteral(flow.getValue().uncertainty))
                        .addProperty(RDF.type, Datatype.CalculateElementaryFlow));
    }

    protected void writeImpact(Resource process, Impact impact)
    {
        process.addProperty(Datatype.hasImpact,
                model.createResource(Datatype.getURI() + AnonId.create().toString())
                        .addProperty(Datatype.hasImpactType, model.createResource(Datatype.getURI() + impact.getType().getId()))
                        .addProperty(Datatype.value, model.createTypedLiteral(impact.getValue().value))
                        .addProperty(Datatype.uncertainty, model.createTypedLiteral(impact.getValue().uncertainty))
                        .addProperty(RDF.type, Datatype.Impact));
    }

}
