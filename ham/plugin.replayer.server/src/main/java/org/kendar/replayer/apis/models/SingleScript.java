package org.kendar.replayer.apis.models;

import java.util.ArrayList;
import java.util.List;

public class SingleScript {
    private List<SingleScriptLine> lines = new ArrayList<>();
    private SingleScriptFilter filter;
    private Long id;
    private String description;

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public List<SingleScriptLine> getLines() {
        return lines;
    }

    public void setLines(List<SingleScriptLine> lines) {
        this.lines = lines;
    }

    public SingleScriptFilter getFilter() {
        return filter;
    }

    public void setFilter(SingleScriptFilter filter) {
        this.filter = filter;
    }
}
