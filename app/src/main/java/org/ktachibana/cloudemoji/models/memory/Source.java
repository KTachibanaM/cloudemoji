package org.ktachibana.cloudemoji.models.memory;

import java.util.ArrayList;
import java.util.List;

/**
 * POJO class holding information and a list of categories
 */
@org.parceler.Parcel
public class Source {
    String alias;
    List<String> information;
    List<Category> categories;

    public Source() { /*Required empty bean constructor*/ }

    public Source(String alias, ArrayList<String> information, List<Category> categories) {
        this.alias = alias;
        this.information = information;
        this.categories = categories;
    }

    public List<String> getInformation() {
        return information;
    }

    public List<Category> getCategories() {
        return categories;
    }

    public List<Entry> getAllEntries() {
        List<Entry> entries = new ArrayList<Entry>();
        for (Category category : categories) {
            entries.addAll(category.getEntries());
        }
        return entries;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || !(o instanceof Source))
            return false;

        Source source = (Source) o;

        if (!categories.equals(source.categories))
            return false;
        if (!information.equals(source.information))
            return false;
        if (!alias.equals(source.alias))
            return false;

        return true;
    }
}
