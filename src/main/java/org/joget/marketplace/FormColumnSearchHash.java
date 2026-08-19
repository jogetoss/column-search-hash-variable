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

public class FormColumnSearchHash extends DefaultHashVariablePlugin {

    private final static String MESSAGE_PATH = "messages/FormHashSearch";

    // Override abstract methods
    @Override
    public String getName() {
        return "Form Column Search Hash Variable";
    }

    @Override
    public String getVersion() {
        return "8.0.7";
    }

    @Override
    public String getLabel() {
        // support i18n
        return AppPluginUtil.getMessage("org.joget.FormHashPlugin.FormHashSearch.pluginLabel", getClassName(), MESSAGE_PATH);
    }

    @Override
    public String getDescription() {
        // support i18n
        return AppPluginUtil.getMessage("org.joget.FormHashPlugin.FormHashSearch.pluginDesc", getClassName(), MESSAGE_PATH);
    }

    // Override hash variable abstract methods
    @Override
    public String processHashVariable(String variable) {
        // To process the hash variable in the plugin
        String result = "";
        String primaryKey = null;

        Integer index = null;
        List<String> parameters = new ArrayList<String>();

        // Retrieve the parameters (searching query, index)
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
             LogUtil.debug(FormColumnSearchHash.class.getName(), "#formLookup." + variable + "# is NULL");
             return "";
        }

        primaryKey = parameters.get(0);
        if (primaryKey.isEmpty()) {
            LogUtil.debug(FormColumnSearchHash.class.getName(), "#formLookup." + variable + "[]# is NULL");
            return "";
        }

        if (SearchCondition.isBlank(primaryKey)) {
            LogUtil.debug(FormColumnSearchHash.class.getName(), "#formLookup." + variable + "[" + primaryKey + "]# has no value to search for");
            return "";
        }

        if (parameters.size() > 1) {
            try {
                index = Integer.parseInt(parameters.get(1));
            } catch (NumberFormatException e) {
                LogUtil.debug(FormColumnSearchHash.class.getName(), "Invalid index: " + parameters.get(1));
            }
        }
        String temp[] = variable.split("\\.");
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
        try {
            // retrieve connection from the default datasource
            DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
            con = ds.getConnection();

            List<String> results = new ArrayList<String>();
            boolean anyMatched = false;

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
                        results.add(StringUtil.decryptContent(value));
                    }
                }
                rs.close();
                stmt.close();
            }

            // No records found for any of the searched values
            if (!anyMatched) {
                String originalTag = "#formLookup." + variable;
                for (String p : parameters) {
                    originalTag += "[" + p + "]";
                }
                originalTag += "#";
                return StringUtil.decryptContent(originalTag);
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
            
            // Default to first result for non-multirow
            result = results.get(0);

        } catch (Exception e) {
            LogUtil.error(FormColumnSearchHash.class.getName(), e, e.getMessage());
            return null;
        } finally {
            try {
                if (con != null) {
                    con.close();
                }
            } catch (SQLException e) { /* ignored */
            }
        }

        return result;
    }

    @Override
    public Collection<String> availableSyntax() {
        Collection<String> syntax = new ArrayList<String>();
        syntax.add("formLookup.TABLE.COLUMN[CONDITION=VALUE][INDEX]");
        syntax.add("formLookup.TABLE.COLUMN[CONDITION1=VALUE1,CONDITION2=VALUE2][INDEX]");
        return syntax;
    }

    // TODO: Assist user to generate the syntax
    @Override
    public String getPropertyAssistantDefinition() {
        return AppUtil.readPluginResource(getClass().getName(), "/properties/assist/FormHashSearch.json", null, true,MESSAGE_PATH);
    }

    @Override
    public String escapeHashVariableValue(String value) {
        value = value.replaceAll("&#35;", "#");
        return value;
    }

    // Override getPrefix
    @Override
    public String getPrefix() {
        return "formLookup";
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
