package org.joget.marketplace;

import java.util.Arrays;
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

    public boolean isSystemField(String columnName){
    String[] jogetColumns = {"id","datecreated", "datemodified", "createdby", "createdbyname", "modifiedby", "modifiedbyname"};
        for(String c : jogetColumns){
            if (columnName.toLowerCase().equals(c)){
                return true;
            }
        }
        return false;
    }
        
    public String generateQuery() {
        List systemFields = Arrays.asList("id", "dateCreated", "dateModified", "createdBy", "createdByName", "modifiedBy", "modifiedByName");
        String initQuery = " WHERE ";
        int i = 1;
        for (String s : this.queries.keySet()){
            
            if(!isSystemField(s)) {
                s = "c_" + s;
            }
            
            if (i == this.queries.size()){
                initQuery += s + " = " + "?";
            }else{
                initQuery += s + " = " + "?" + " AND ";
            }
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
