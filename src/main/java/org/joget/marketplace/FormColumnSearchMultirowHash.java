package org.joget.marketplace;

import org.joget.apps.app.model.DefaultHashVariablePlugin;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.sql.DataSource;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.StringUtil;
import org.joget.apps.app.service.AppPluginUtil;
import org.joget.apps.app.service.AppUtil;

public class FormColumnSearchMultirowHash extends DefaultHashVariablePlugin {

    private final static String MESSAGE_PATH = "messages/FormHashSearch";

    @Override
    public String getName() {
        return "Form Column Search Multirow Hash Variable";
    }

    @Override
    public String getVersion() {
        return "8.0.7";
    }

    @Override
    public String getLabel() {
        // Reuse existing i18n message keys
        return AppPluginUtil.getMessage("org.joget.FormHashPlugin.FormHashSearch.pluginLabel", getClassName(), MESSAGE_PATH);
    }

    @Override
    public String getDescription() {
        // Reuse existing i18n message keys
        return AppPluginUtil.getMessage("org.joget.FormHashPlugin.FormHashSearch.pluginDesc", getClassName(), MESSAGE_PATH);
    }

    @Override
    public String processHashVariable(String variable) {
        String primaryKey = null;
        String separator = ";"; // Default separator
        Integer index = null;
        List<String> parameters = new ArrayList<String>();

        // Retrieve the parameters (searching query, separator, index)
        int bracketIndex = variable.indexOf("[");
        if (bracketIndex != -1) {
            String paramsPart = variable.substring(bracketIndex);
            
            // Extract content between brackets
            int start = 0;
            while ((start = paramsPart.indexOf("[", start)) != -1) {
                int end = paramsPart.indexOf("]", start);
                if (end != -1) {
                    parameters.add(paramsPart.substring(start + 1, end));
                    start = end + 1;
                } else {
                    break;
                }
            }
            variable = variable.substring(0, bracketIndex);
        }

        if (parameters.isEmpty()) {
             LogUtil.debug(FormColumnSearchMultirowHash.class.getName(), "#formLookupMultirow." + variable + "# is NULL");
             return "";
        }

        primaryKey = parameters.get(0);
        if (primaryKey.isEmpty()) {
            LogUtil.debug(FormColumnSearchMultirowHash.class.getName(), "#formLookupMultirow." + variable + "[]# is NULL");
            return "";
        }

        if (SearchCondition.isBlank(primaryKey)) {
            LogUtil.debug(FormColumnSearchMultirowHash.class.getName(), "#formLookupMultirow." + variable + "[" + primaryKey + "]# has no value to search for");
            return "";
        }

        if (parameters.size() > 1) {
            separator = parameters.get(1).replace("SPACE", " ");
        }

        if (parameters.size() > 2) {
            try {
                index = Integer.parseInt(parameters.get(2));
            } catch (NumberFormatException e) {
                LogUtil.debug(FormColumnSearchMultirowHash.class.getName(), "Invalid index: " + parameters.get(2));
            }
        }

        String[] temp = variable.split("\\.");
        String tableName = temp[0];
        String retrieveColumnName = temp[1];
        List<SearchCondition> conditionList = SearchCondition.expand(primaryKey);
        Connection con = null;

        String[] jogetColumns = {"id","datecreated", "datemodified", "createdby", "createdbyname", "modifiedby", "modifiedbyname"};
        Boolean primColumnSearch = false;
        for(String c : jogetColumns){
            if (retrieveColumnName.toLowerCase().equals(c)){
                primColumnSearch = true;
                break;
            }
        }

        List<String> results = new ArrayList<String>();
        boolean anyMatched = false;

        try {
            // retrieve connection from the default datasource
            DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
            con = ds.getConnection();

            // Run one lookup per expanded condition (e.g. one per id in a multi-value key), preserving input order
            for (SearchCondition criteria : conditionList) {
                PreparedStatement stmt = con.prepareStatement(
                        "SELECT " + (primColumnSearch ? retrieveColumnName : "c_" + retrieveColumnName) + " FROM app_fd_" + tableName + criteria.generateQuery());
                int ordinalParam = 1;
                for (String s : criteria.generateParams()) {
                    stmt.setObject(ordinalParam, s);
                    ordinalParam++;
                }

                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    anyMatched = true;
                    String value = rs.getString((primColumnSearch ? retrieveColumnName : "c_" + retrieveColumnName));
                    if (value != null) {
                        // Decrypt each value individually before joining
                        results.add(StringUtil.decryptContent(value));
                    }
                }
                rs.close();
                stmt.close();
            }

            // No records found for any of the searched values
            if (!anyMatched) {
                String originalTag = "#formLookupMultirow." + variable;
                for (String p : parameters) {
                    originalTag += "[" + p + "]";
                }
                originalTag += "#";
                return StringUtil.decryptContent(originalTag);
            }

        } catch (Exception e) {
            LogUtil.error(FormColumnSearchMultirowHash.class.getName(), e, e.getMessage());
            return null;
        } finally {
            try {
                if (con != null) {
                    con.close();
                }
            } catch (SQLException e) { /* ignored */
            }
        }

        if (results.isEmpty()) {
            return "";
        }

        if (index != null) {
            if (index >= 0 && index < results.size()) {
                return results.get(index);
            } else {
                return "";
            }
        }

        // Join all results with separator
        return String.join(separator, results);
    }

    @Override
    public Collection<String> availableSyntax() {
        Collection<String> syntax = new ArrayList<String>();
        syntax.add("formLookupMultirow.TABLE.COLUMN[CONDITION=VALUE][SEPARATOR][INDEX]");
        syntax.add("formLookupMultirow.TABLE.COLUMN[CONDITION1=VALUE1,CONDITION2=VALUE2][SEPARATOR][INDEX]");
        return syntax;
    }

    @Override
    public String getPropertyAssistantDefinition() {
        return AppUtil.readPluginResource(getClass().getName(), "/properties/assist/FormHashSearchMultirow.json", null, true, MESSAGE_PATH);
    }

    @Override
    public String escapeHashVariableValue(String value) {
        value = value.replaceAll("&#35;", "#");
        return value;
    }

    @Override
    public String getPrefix() {
        return "formLookupMultirow";
    }

    @Override
    public String getClassName() {
        return this.getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return ""; // Hash Variable does not support property options
    }
}

