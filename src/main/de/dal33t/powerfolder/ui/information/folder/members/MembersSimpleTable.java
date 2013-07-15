/*
 * Copyright 2004 - 2012 Christian Sprajc. All rights reserved.
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
 * $Id: MemberTable.java 5457 2008-10-17 14:25:41Z harry $
 */
package de.dal33t.powerfolder.ui.information.folder.members;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.DefaultCellEditor;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import com.jgoodies.binding.adapter.BasicComponentFactory;

import de.dal33t.powerfolder.light.AccountInfo;
import de.dal33t.powerfolder.security.FolderPermission;
import de.dal33t.powerfolder.ui.render.SortedTableHeaderRenderer;
import de.dal33t.powerfolder.ui.util.ColorUtil;
import de.dal33t.powerfolder.ui.util.Icons;
import de.dal33t.powerfolder.ui.util.UIUtil;
import de.dal33t.powerfolder.util.Translation;

/**
 * Table to display members of a folder.
 */
public class MembersSimpleTable extends JTable {

    private DefaultCellEditor cellEditor;

    /**
     * Constructor
     * 
     * @param model
     */
    public MembersSimpleTable(MembersSimpleTableModel model) {
        super(model);

        setRowHeight(Icons.getIconById(Icons.NODE_CONNECTED)
            .getIconHeight() + 3);
        setColumnSelectionAllowed(false);
        setShowGrid(false);

        setupColumns();

        getTableHeader().addMouseListener(new TableHeaderMouseListener());

        // Setup renderer
        MemberTableCellRenderer memberCellRenderer = new MemberTableCellRenderer();
        setDefaultRenderer(FolderMember.class, memberCellRenderer);
        setDefaultRenderer(FolderPermission.class, memberCellRenderer);
        setDefaultRenderer(String.class, memberCellRenderer);

        // Associate a header renderer with all columns.
        SortedTableHeaderRenderer.associateHeaderRenderer(model,
            getColumnModel(), MembersSimpleTableModel.COL_USERNAME, true);

        cellEditor = new DefaultCellEditor(createdEditComboBox(model));
        setDefaultEditor(FolderPermission.class, cellEditor);
    }

    void cancelCellEditing() {
        if (cellEditor != null) {
            cellEditor.cancelCellEditing();
        }
    }

    /**
     * Sets the column sizes of the table
     */
    private void setupColumns() {
        int totalWidth = getWidth();

        // Otherwise the table header is not visible:
        getTableHeader().setPreferredSize(new Dimension(totalWidth, 20));

        TableColumn column = getColumn(getColumnName(MembersSimpleTableModel.COL_TYPE));
        column.setPreferredWidth(28);
        column.setMinWidth(28);
        column.setMaxWidth(28);
        column = getColumn(getColumnName(MembersSimpleTableModel.COL_USERNAME));
        column.setPreferredWidth(100);
        column = getColumn(getColumnName(MembersSimpleTableModel.COL_PERMISSION));
        column.setPreferredWidth(100);
    }

    /**
     * Listener on table header, takes care about the sorting of table
     * 
     * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
     */
    private class TableHeaderMouseListener extends MouseAdapter {
        public void mouseClicked(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)) {
                JTableHeader tableHeader = (JTableHeader) e.getSource();
                int columnNo = tableHeader.columnAtPoint(e.getPoint());
                TableColumn column = tableHeader.getColumnModel().getColumn(
                    columnNo);
                int modelColumnNo = column.getModelIndex();
                TableModel model = tableHeader.getTable().getModel();
                if (model instanceof MembersSimpleTableModel) {
                    MembersSimpleTableModel membersTableModel = (MembersSimpleTableModel) model;
                    membersTableModel.sortBy(modelColumnNo);
                }
            }
        }
    }

    private class MemberTableCellRenderer extends DefaultTableCellRenderer {

        public Component getTableCellRendererComponent(JTable table,
            Object value, boolean isSelected, boolean hasFocus, int row,
            int column)
        {
            int actualColumn = UIUtil.toModel(table, column);
            MembersSimpleTableModel model = (MembersSimpleTableModel) getModel();
            FolderMember folderMember = model.getFolderMemberAt(row);

            Component defaultComp = super.getTableCellRendererComponent(table,
                value, isSelected, hasFocus, row, column);

            // Reset to defaults
            setEnabled(true);
            setIcon(null);
            setForeground(ColorUtil.getTextForegroundColor());

            boolean isServer = folderMember.getMember() != null
                && (model.getController().getOSClient()
                    .isClusterServer(folderMember.getMember()));

            if (actualColumn == MembersSimpleTableModel.COL_TYPE) {
                if (folderMember.getGroupInfo() != null) {
                    Icon icon = Icons.getIconById(Icons.NODE_GROUP);
                    setIcon(icon);
                }
                else {
                    AccountInfo member = folderMember.getAccountInfo();
//                    Icon icon = member != null ? Icons.getIconFor(member) : Icons
//                        .getIconById(Icons.NODE_DISCONNECTED);
                    setIcon(Icons.getIconByAccount(member, folderMember.getFolder().getController()));
                    setText("");
                }
            } else if (actualColumn == MembersSimpleTableModel.COL_USERNAME) {
                if (!model.getController().getOSClient().isConnected()) {
                    setText(Translation
                        .getTranslation("folder_member.not_connected_to_server"));
                    setForeground(Color.GRAY);
                } else if (isServer) {
                    setText(Translation.getTranslation("folder_member.server"));
                    setForeground(Color.GRAY);
                } else if (folderMember.getGroupInfo() != null) {
                    setText(folderMember.getGroupInfo().getDisplayName());
                } else if (folderMember.getAccountInfo() != null) {
                    setText(folderMember.getAccountInfo().getDisplayName());
                } else {
                    setText(Translation
                        .getTranslation("folder_member.not_logged_in"));
                    setForeground(Color.GRAY);
                }
            } else if (actualColumn == MembersSimpleTableModel.COL_PERMISSION) {
                boolean editable = model.isCellEditable(row, column);
                if (!editable) {
                    setForeground(Color.GRAY);
                }
                if (!model.isPermissionsRetrieved()) {
                    setText("");
                } else if (isServer) {
                    // Server has read/write by default
                    setText(Translation
                        .getTranslation("permissions.folder.read_write"));
                } else {
                    String name;
                    FolderPermission defPerm = model.getDefaultPermission();
                    if (folderMember.getPermission() != null) {
                        name = folderMember.getPermission().getName();
                    } else if (defPerm != null) {
                        name = defPerm.getName();
                        name += " (";
                        name += Translation
                            .getTranslation("permissions.folder.default");
                        name += ")";
                    } else {
                        name = Translation
                            .getTranslation("permissions.folder.no_access");
                    }
                    setText(name);
                }
            }

            if (!isSelected) {
                setBackground(row % 2 == 0
                    ? ColorUtil.EVEN_TABLE_ROW_COLOR
                    : ColorUtil.ODD_TABLE_ROW_COLOR);
            }

            return defaultComp;
        }
    }

    private JComboBox createdEditComboBox(final MembersSimpleTableModel model) {
        return BasicComponentFactory.createComboBox(model
            .getPermissionsListModel(), new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list,
                Object value, int index, boolean isSelected,
                boolean cellHasFocus)
            {
                Component comp = super.getListCellRendererComponent(list,
                    value, index, isSelected, cellHasFocus);

                if (value instanceof FolderPermission) {
                    setText(((FolderPermission) value).getName());
                } else {
                    int selectedRow = getSelectedRow();
                    FolderMember selectedMember = selectedRow >= 0 ? model
                        .getFolderMemberAt(selectedRow) : null;
                    if (selectedMember != null
                        && selectedMember.getMember() == null)
                    {
                        setText(Translation
                            .getTranslation("permissions.folder.no_access"));
                    } else {
                        FolderPermission defPerm = model.getDefaultPermission();
                        if (defPerm != null) {
                            setText(defPerm.getName()
                                + " ("
                                + Translation
                                    .getTranslation("permissions.folder.default")
                                + ')');
                        } else {
                            setText(Translation
                                .getTranslation("permissions.folder.no_access")
                                + " ("
                                + Translation
                                    .getTranslation("permissions.folder.default")
                                + ')');
                        }
                    }

                }
                return comp;
            }
        });
    }

}
