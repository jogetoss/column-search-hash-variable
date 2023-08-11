package org.joget.marketplace;

import java.util.HashMap;
import java.util.List;
import java.util.Set;


public class SearchCondition {
    private HashMap<String, String> queries = new HashMap<String, String>();

    //Create constructor for this class
    public SearchCondition(HashMap<String, String> queries) {
        this.queries = queries;
    }

    // Create constructor for this class where one String params, gets parsed into a list of queries with columnNames and columnValues (queries are delimited by ',') (names and values delimited by '=')
    public SearchCondition(String query) {
        String[] queries = query.split(",");
        for (String q : queries) {
            this.queries.put(q.split("=")[0], q.split("=")[1]);
        }
    }

    public Set<String> getColumns() {
        return this.queries.keySet();
    }

    public List<String> getValues() {
        return (List<String>) this.queries.values();
    }

    public String generateQuery() {
        String initQuery = " WHERE ";
        int i = 1;
        for (String s : this.queries.keySet()){
            if (i == this.queries.size())
                initQuery += "c_" + s + " = " + "?";
            else
                initQuery += "c_" + s + " = " + "?" + " AND ";
            i++;
        }
        return initQuery;
    }

    public String[] generateParams(){
        String[] params = new String[this.queries.size()];
        int i = 0;
        for (String key : this.queries.keySet()){
            // params[i] = "c_" + key;
            params[i] = this.queries.get(key);
            // i += 2;
            i++;
        }
        return params;
    }
}
