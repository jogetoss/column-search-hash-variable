package org.joget.marketplace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.joget.commons.util.LogUtil;

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
            String[] pair = q.split("=", 2);

            if (pair.length != 2) {
                LogUtil.debug(SearchCondition.class.getName(),
                        "Invalid search condition: " + q + ". Expected format COLUMN=VALUE");
                throw new IllegalArgumentException(
                        "Invalid search condition \"" + q
                        + "\". Expected format: COLUMN=VALUE");
            }

            this.queries.put(pair[0], pair[1]);
        }
    }

    public Set<String> getColumns() {
        return this.queries.keySet();
    }

    public List<String> getValues() {
        return (List<String>) this.queries.values();
    }

    public boolean isSystemField(String columnName) {
        String[] jogetColumns = {"id", "datecreated", "datemodified", "createdby", "createdbyname", "modifiedby", "modifiedbyname"};
        for (String c : jogetColumns) {
            if (columnName.toLowerCase().equals(c)) {
                return true;
            }
        }
        return false;
    }

    public String generateQuery() {
        StringBuilder initQuery = new StringBuilder(" WHERE ");
        int i = 0;
        for (String s : this.queries.keySet()) {
            if (!isSystemField(s)) {
                s = "c_" + s;
            }
            if (i > 0) {
                initQuery.append(" AND ");
            }
            initQuery.append("FIND_IN_SET(?, REPLACE(").append(s).append(", ';', ','))");
            i++;
        }
        return initQuery.toString();
    }

    public String[] generateParams() {
        String[] params = new String[this.queries.size()];
        int i = 0;
        for (String key : this.queries.keySet()) {
            // params[i] = "c_" + key;
            params[i] = this.queries.get(key);
            // i += 2;
            i++;
        }
        return params;
    }

    // True when every condition's VALUE is blank (e.g. "id=" from an unresolved/empty nested hash)
    public static boolean isBlank(String primaryKey) {
        for (String pair : primaryKey.split(",")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2 && !kv[1].trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    // Splits any COLUMN=VALUE1;VALUE2 condition into one SearchCondition per value combination, so a multi-value key resolves to multiple single-value lookups
    public static List<SearchCondition> expand(String primaryKey) {
        LinkedHashMap<String, List<String>> parsed = new LinkedHashMap<String, List<String>>();

        for (String pair : primaryKey.split(",")) {
            String[] kv = pair.split("=", 2);

            if (kv.length != 2) {
                LogUtil.debug(SearchCondition.class.getName(),
                        "Invalid search condition: " + pair + ". Expected format COLUMN=VALUE");
                throw new IllegalArgumentException(
                        "Invalid search condition \"" + pair
                        + "\". Expected format: COLUMN=VALUE");
            }

            List<String> values = new ArrayList<String>();
            for (String value : kv[1].split(";", -1)) {
                values.add(value);
            }
            parsed.put(kv[0], values);
        }

        List<HashMap<String, String>> combinations = new ArrayList<HashMap<String, String>>();
        combinations.add(new HashMap<String, String>());
        for (Map.Entry<String, List<String>> entry : parsed.entrySet()) {
            List<HashMap<String, String>> expanded = new ArrayList<HashMap<String, String>>();
            for (HashMap<String, String> combination : combinations) {
                for (String value : entry.getValue()) {
                    HashMap<String, String> next = new HashMap<String, String>(combination);
                    next.put(entry.getKey(), value);
                    expanded.add(next);
                }
            }
            combinations = expanded;
        }

        List<SearchCondition> conditions = new ArrayList<SearchCondition>();
        for (HashMap<String, String> combination : combinations) {
            conditions.add(new SearchCondition(combination));
        }
        return conditions;
    }
}
