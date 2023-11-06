package org.joget.marketplace;

import org.joget.apps.app.model.DefaultHashVariablePlugin;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
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
        return "8.0.1";
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

        // Retrieve the primary key (searching query)
        if (variable.contains("[") && variable.contains("]")) {
            primaryKey = variable.substring(variable.indexOf("[") + 1, variable.indexOf("]"));
            if (primaryKey.isEmpty()) {
                LogUtil.debug(FormColumnSearchHash.class.getName(), "#formColumnSearch." + variable + "# is NULL");
                return "";
            }
            variable = variable.substring(0, variable.indexOf("["));
        }
        String temp[] = variable.split("\\.");

        String tableName = temp[0];
        String retrieveColumnName = temp[1];
        SearchCondition criteria = new SearchCondition(primaryKey);
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
            PreparedStatement stmt = con.prepareStatement(
                    "SELECT " + (primColumnSearch ? retrieveColumnName : "c_" + retrieveColumnName) + " FROM app_fd_" + tableName + criteria.generateQuery());
            int ordinalParam = 1;
            for (String s : criteria.generateParams()) {
                stmt.setObject(ordinalParam, s);
                ordinalParam++;
            }
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                // Finds the first matching row
                result = rs.getString((primColumnSearch ? retrieveColumnName : "c_" + retrieveColumnName));
            }

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

        return StringUtil.decryptContent(result);
    }

    @Override
    public Collection<String> availableSyntax() {
        Collection<String> syntax = new ArrayList<String>();
        syntax.add("formLookup.TABLE.COLUMN[CONDITION=VALUE]");
        syntax.add("formLookup.TABLE.COLUMN[CONDITION1=VALUE1,CONDITION2=VALUE2]");
        return syntax;
    }

    // TODO: Assist user to generate the syntax
    @Override
    public String getPropertyAssistantDefinition() {
        return AppUtil.readPluginResource(getClass().getName(), "/properties/assist/FormHashSearch.json", null, true,MESSAGE_PATH);
    }

    @Override
    public String escapeHashVariableValue(String value) {
        return AppUtil.escapeHashVariable(value);
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
