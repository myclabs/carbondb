package com.mycsense.carbondb.domain;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.mycsense.carbondb.domain.dimension.Orientation;

public class Dimension
{
    public HashSet<Keyword> keywords;
    public HashMap<Integer, String> keywordsPosition;
    public Orientation orientation = Orientation.NONE;

    public Dimension() {
        keywords = new HashSet<>();
        keywordsPosition = new HashMap<>();
    }

    public Dimension(Keyword... pKeywords) {
        keywords = new HashSet<>();
        keywordsPosition = new HashMap<>();
        Collections.addAll(keywords, pKeywords);
    }

    public Dimension(Dimension dimension) {
        keywords = new HashSet<>(dimension.keywords);
    }

    public int size() {
        return keywords.size();
    }

    public boolean add(Keyword keyword) {
        return keywords.add(keyword);
    }

    public void addKeywordPosition(Integer position, String keywordURI) {
        keywordsPosition.put(position, keywordURI);
    }

    public String toString() {
        return keywords.toString();
    }

    public boolean contains(Keyword keyword) {
        return keywords.contains(keyword);
    }

    public boolean isEmpty() {
        return keywords.isEmpty();
    }

    public Object[] toArray() {
        return keywords.toArray();
    }

    @Override
    public boolean equals(Object obj) {
        // Alternative: use Guava (from Google)
        if (!(obj instanceof Dimension))
            return false;
        if (obj == this)
            return true;

        Dimension rhs = (Dimension) obj;
        return new EqualsBuilder()
                  .append(keywords, rhs.keywords)
                  .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(3, 67)
                  .append(keywords)
                  .toHashCode();
    }

    public Boolean hasCommonKeywords(Dimension dimension)
    {
        for (Keyword keyword: keywords) {
            if (dimension.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    public void setOrientation(Orientation orientation) {
        this.orientation = orientation;
    }

    public Orientation getOrientation() {
        return orientation;
    }
}