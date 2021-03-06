/*
 * Copyright 2014, by Benjamin Bertin and Contributors.
 *
 * This file is part of CarbonDB-reasoner project <http://www.carbondb.org>
 *
 * CarbonDB-reasoner is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * CarbonDB-reasoner is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with CarbonDB-reasoner.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Contributor(s): -
 *
 */

package com.mycsense.carbondb.domain;

import com.mycsense.carbondb.NoElementFoundException;
import com.mycsense.carbondb.domain.group.Type;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.TreeSet;

public class Group {
    protected DimensionSet dimSet;
    /**
     * Contains the dimension set and the common keywords
     */
    protected DimensionSet fullDimSet;
    protected DimensionSet coordinates;
    protected TreeSet<Keyword> commonKeywords;
    protected Type type = Type.PROCESS;
    protected HashSet<SingleElement> elements;
    protected HashSet<Group> overlappingGroups;
    protected HashSet<SourceRelation> sourceRelations;

    protected String label;
    protected String id;
    protected Unit unit;
    protected String comment;
    protected Category category;

    protected ArrayList<Reference> references;

    public Group() {
        this(new DimensionSet());
    }

    public Group(Dimension... dimensions) {
        this(new DimensionSet(dimensions));
    }

    public Group(DimensionSet dimSet) {
        this(dimSet, new TreeSet<Keyword>());
    }

    public Group(DimensionSet dimSet, TreeSet<Keyword> commonKeywords) {
        this.dimSet = dimSet;
        fullDimSet = new DimensionSet(dimSet);
        overlappingGroups = new HashSet<>();
        sourceRelations = new HashSet<>();
        comment = "";
        setCommonKeywords(commonKeywords);
        createCoordinates();
    }

    public DimensionSet getDimSet() {
        return dimSet;
    }

    public TreeSet<Keyword> getCommonKeywords() {
        return commonKeywords;
    }

    public void addCommonKeyword(Keyword keyword) {
        commonKeywords.add(keyword);
        fullDimSet.add(new Dimension(keyword));
    }

    public void setCommonKeywords(TreeSet<Keyword> keywords) {
        this.commonKeywords = new TreeSet<>();
        for (Keyword keyword: keywords) {
            addCommonKeyword(keyword);
        }
    }

    public void createCoordinates() {
        coordinates = fullDimSet.combinations();
    }

    public String toString() {
        return "coordinates: " + coordinates.toString() + " dimSet: " + dimSet.toString() + " unit: " + unit;
    }

    public DimensionSet getCoordinates() {
        return coordinates;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setUnit(Unit unit) {
        this.unit = unit;
    }

    public Unit getUnit() {
        return unit;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public boolean hasCategory() {
        return null != category;
    }

    public DimensionSet getFullDimSet() {
        return fullDimSet;
    }

    public HashSet<SingleElement> getElements() {
        if (null == elements) {
            fetchElements();
        }
        return elements;
    }

    public void addElement(SingleElement element) {
        getElements().add(element);
        element.addGroup(this);
    }

    public boolean hasElement(SingleElement element) {
        return getElements().contains(element);
    }

    public HashSet<Group> getOverlappingGroups() {
        return overlappingGroups;
    }

    public void addOverlapingGroup(Group group) {
        if (this != group && !overlappingGroups.contains(group)) {
            overlappingGroups.add(group);
        }
    }

    public HashSet<SourceRelation> getSourceRelations() {
        return sourceRelations;
    }

    public void addSourceRelation(SourceRelation sourceRelation) {
        if (!sourceRelations.contains(sourceRelation)) {
            sourceRelations.add(sourceRelation);
        }
    }

    protected void fetchElements() {
        elements = new HashSet<>();
        for (Dimension coordinate : coordinates.dimensions) {
            try {
                if (type == Type.COEFFICIENT) {
                    addElement(CarbonOntology.getInstance().findCoefficient(coordinate, unit));
                }
                else {
                    addElement(CarbonOntology.getInstance().findProcess(coordinate, unit));
                }
            } catch (NoElementFoundException e) {
                // nothing to do here, there is no element at this coordinate
            }
        }
    }

    public ArrayList<Reference> getReferences() {
        return references;
    }

    public void setReferences(ArrayList<Reference> references) {
        this.references = references;
        for (Reference reference : references) {
            reference.addGroup(this);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Group))
            return false;
        if (obj == this)
            return true;

        Group rhs = (Group) obj;
        return new EqualsBuilder()
                .append(id, rhs.id)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(229, 881)
                .append(id)
                .toHashCode();
    }
}