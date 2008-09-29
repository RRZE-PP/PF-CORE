/*
* Copyright 2004 - 2008 Christian Sprajc. All rights reserved.
*
* This file is part of PowerFolder.
*
* PowerFolder is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation.
*
* PowerFolder is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with PowerFolder. If not, see <http://www.gnu.org/licenses/>.
*
* $Id$
*/
package de.dal33t.powerfolder.ui.preferences;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.plugin.Plugin;
import de.dal33t.powerfolder.plugin.PluginEvent;
import de.dal33t.powerfolder.plugin.PluginManager;
import de.dal33t.powerfolder.plugin.PluginManagerListener;
import de.dal33t.powerfolder.ui.action.SelectionBaseAction;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.SelectionChangeEvent;
import de.dal33t.powerfolder.util.ui.SelectionModel;
import de.dal33t.powerfolder.util.ui.UIUtil;

public class PluginSettingsTab extends PFUIComponent implements PreferenceTab,
    PluginManagerListener
{

    private final static int PLUGIN_NAME_COL = 0;
    private final static int PLUGIN_DESCR_COL = 1;
    private final static int PLUGIN_CLASS_COL = 2;
    private final static int PLUGIN_STATUS_COL = 3;

    private PreferencesDialog preferencesDialog;

    private JPanel panel;
    private JTable pluginJTable;
    private JScrollPane pluginPane;
    private JButton settingsButton;
    private JButton enableButton;
    private SelectionModel selectionModel;

    public PluginSettingsTab(Controller controller,
        PreferencesDialog preferencesDialog)
    {
        super(controller);
        this.preferencesDialog = preferencesDialog;
        selectionModel = new SelectionModel();
        initComponents();
    }

    public boolean needsRestart() {
        return false;
    }

    public void save() {
    }

    public boolean validate() {
        return true;
    }

    public String getTabName() {
        return Translation.getTranslation("preferences.dialog.plugin.title");
    }

    public void undoChanges() {
    }

    /**
     * Creates the JPanel for plugin settings
     * 
     * @return the created panel
     */
    public JPanel getUIPanel() {
        if (panel == null) {
            FormLayout layout = new FormLayout("3dlu, pref:grow, 3dlu",
                "3dlu, fill:pref:grow, 3dlu, pref, 3dlu");
            PanelBuilder builder = new PanelBuilder(layout);
            CellConstraints cc = new CellConstraints();
            builder.add(pluginPane, cc.xy(2, 2));
            builder.add(getButtonBar(), cc.xy(2, 4));
            panel = builder.getPanel();
        }
        return panel;
    }

    private void initComponents() {
        pluginJTable = new JTable(new PluginTableModel());
        pluginJTable
            .setDefaultRenderer(Plugin.class, new PluginTableRenderer());
        pluginPane = new JScrollPane(pluginJTable);

        UIUtil.whiteStripTable(pluginJTable);
        UIUtil.removeBorder(pluginPane);
        UIUtil.setZeroHeight(pluginPane);

        pluginJTable.getSelectionModel().setSelectionMode(
            ListSelectionModel.SINGLE_SELECTION);
        pluginJTable.getSelectionModel().addListSelectionListener(
            new PluginTableListSelectionListener());

        settingsButton = new JButton(new SettingsAction(getController(),
            selectionModel));
        enableButton = new JButton(new EnableAction(getController(),
            selectionModel));

        pluginJTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    Plugin plugin = (Plugin) selectionModel.getSelection();
                    if (plugin == null) {
                        return;
                    }
                    if (getController().getPluginManager().isEnabled(plugin)) {
                        if (plugin.hasOptionsDialog()) {
                            plugin.showOptionsDialog(preferencesDialog);
                        }
                    } else {
                        // Enable
                        getController().getPluginManager().setEnabled(plugin,
                            true);
                        if (plugin.hasOptionsDialog()) {
                            plugin.showOptionsDialog(preferencesDialog);
                        }
                    }
                }
            }
        });
        getController().getPluginManager().addPluginManagerListener(this);
    }

    private Component getButtonBar() {
        // Disabled Enabled/Disable button
        return ButtonBarFactory.buildCenteredBar(enableButton, settingsButton);

        // return ButtonBarFactory.buildCenteredBar(settingsButton);
    }

    private class PluginTableModel extends AbstractTableModel {

        @Override
        public Class<?> getColumnClass(int columnIndex)
        {
            return Plugin.class;
        }

        @Override
        public String getColumnName(int column)
        {
            switch (column) {
                case PLUGIN_NAME_COL : {
                    return Translation
                        .getTranslation("preferences.dialog.plugin.name");
                }
                case PLUGIN_DESCR_COL : {
                    return Translation
                        .getTranslation("preferences.dialog.plugin.description");
                }
                case PLUGIN_CLASS_COL : {
                    return Translation
                        .getTranslation("preferences.dialog.plugin.class_name");
                }
                case PLUGIN_STATUS_COL : {
                    return Translation
                        .getTranslation("preferences.dialog.plugin.status");
                }
                default :
                    return null;
            }

        }

        public int getColumnCount() {
            return 2;
        }

        public int getRowCount() {
            PluginManager pluginManager = getController().getPluginManager();
            return pluginManager.countPlugins();
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            PluginManager pluginManager = getController().getPluginManager();
            List<Plugin> plugins = pluginManager.getPlugins();
            return plugins.get(rowIndex);
        }
    }

    private class PluginTableRenderer extends DefaultTableCellRenderer {

        public Component getTableCellRendererComponent(JTable table,
            Object value, boolean isSelected, boolean hasFocus, int row,
            int column)
        {
            String newValue = "";
            PluginManager pluginManager = getController().getPluginManager();
            List<Plugin> plugins = pluginManager.getPlugins();
            Plugin plugin = plugins.get(row);
            boolean enabled = getController().getPluginManager().isEnabled(
                plugin);
            if (enabled) {
                setForeground(Color.BLACK);
            } else {
                setForeground(Color.LIGHT_GRAY);
            }

            int columnInModel = UIUtil.toModel(table, column);
            switch (columnInModel) {
                case PLUGIN_NAME_COL : {
                    newValue = plugin.getName();
                    setToolTipText(plugin.getName());
                    setHorizontalAlignment(SwingConstants.LEFT);
                    break;
                }
                case PLUGIN_DESCR_COL : {
                    newValue = plugin.getDescription();
                    setToolTipText(plugin.getDescription());
                    setHorizontalAlignment(SwingConstants.LEFT);
                    break;
                }
                case PLUGIN_CLASS_COL : {
                    newValue = plugin.getClass().getName();
                    setToolTipText(plugin.getClass().getName());
                    setHorizontalAlignment(SwingConstants.LEFT);
                    break;
                }
                case PLUGIN_STATUS_COL : {
                    if (enabled) {
                        newValue = Translation
                            .getTranslation("preferences.dialog.plugin.status_enabled");
                    } else {
                        newValue = Translation
                            .getTranslation("preferences.dialog.plugin.status_disabled");
                    }
                    setToolTipText(newValue);
                    setHorizontalAlignment(SwingConstants.RIGHT);
                    break;
                }
                default :
                    return null;
            }
            return super.getTableCellRendererComponent(table, newValue,
                isSelected, hasFocus, row, column);
        }
    }

    private static class EnableAction extends SelectionBaseAction {

        public EnableAction(Controller controller, SelectionModel selectionModel)
        {
            super("plugin_enable", controller, selectionModel);
            setEnabled(false);
        }

        public void selectionChanged(SelectionChangeEvent event) {
            updateButton((Plugin) event.getSelection());
        }

        public void actionPerformed(ActionEvent e) {
            Plugin plugin = (Plugin) getSelectionModel().getSelection();
            boolean newStatus = !getController().getPluginManager().isEnabled(
                plugin);
            getController().getPluginManager().setEnabled(plugin, newStatus);
        }

        private void updateButton(Plugin plugin) {
            // HACK(tm) do not be able to disable the ProLoader!
            if (plugin == null
                || plugin.getClass().getName().equals(
                    Constants.PRO_LOADER_PLUGIN_CLASS))
            {
                setEnabled(false);
            } else {
                setEnabled(true);
                if (getController().getPluginManager().isEnabled(plugin)) {
                    putValue(NAME, Translation
                        .getTranslation("plugin_disable.name"));
                    putValue(SHORT_DESCRIPTION, Translation
                        .getTranslation("plugin_disable.description"));
                    putValue(ACCELERATOR_KEY, Translation
                        .getTranslation("plugin_disable.key"));
                } else {
                    putValue(NAME, Translation
                        .getTranslation("plugin_enable.name"));
                    putValue(SHORT_DESCRIPTION, Translation
                        .getTranslation("plugin_enable.description"));
                    putValue(ACCELERATOR_KEY, Translation
                        .getTranslation("plugin_enable.key"));
                }
            }
        }

    }

    private class SettingsAction extends SelectionBaseAction {

        public SettingsAction(Controller controller,
            SelectionModel selectionModel)
        {
            super("plugin_settings", controller, selectionModel);
            setEnabled(false);
        }

        public void selectionChanged(SelectionChangeEvent event) {
            Plugin plugin = (Plugin) event.getSelection();
            if (plugin != null) {
                setEnabled(getController().getPluginManager().isEnabled(plugin)
                    && plugin.hasOptionsDialog());
            }

        }

        public void actionPerformed(ActionEvent e) {
            Plugin plugin = (Plugin) selectionModel.getSelection();
            if (plugin != null && plugin.hasOptionsDialog()) {
                plugin.showOptionsDialog(preferencesDialog);
            }
        }
    }

    /**
     * updates the SelectionModel if some selection has changed in the plugin
     * table
     */
    private class PluginTableListSelectionListener implements
        ListSelectionListener
    {
        public void valueChanged(ListSelectionEvent e) {
            int[] selectedRows = pluginJTable.getSelectedRows();
            if (selectedRows.length != 0 && !e.getValueIsAdjusting()) {
                Object[] selectedObjects = new Object[selectedRows.length];
                for (int i = 0; i < selectedRows.length; i++) {
                    selectedObjects[i] = pluginJTable.getModel().getValueAt(
                        selectedRows[i], 0);
                }
                selectionModel.setSelections(selectedObjects);
            } else {
                selectionModel.setSelection(null);
            }
        }
    }

    public void pluginStatusChanged(PluginEvent pluginEvent) {
        ((PluginTableModel) pluginJTable.getModel()).fireTableRowsUpdated(-1,
            -1);
        settingsButton.setEnabled(false);
        enableButton.setEnabled(false);
    }
}
