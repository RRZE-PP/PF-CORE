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
 * $Id: SingleFileRestoreTable.java 5178 2008-09-10 14:59:17Z harry $
 */
package de.dal33t.powerfolder.ui.wizard.table;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.ui.render.SortedTableHeaderRenderer;
import de.dal33t.powerfolder.ui.util.ColorUtil;
import de.dal33t.powerfolder.ui.util.Icons;
import de.dal33t.powerfolder.ui.wizard.data.SingleFileRestoreItem;
import de.dal33t.powerfolder.util.Format;

public class SingleFileRestoreTable extends JTable {

    public SingleFileRestoreTable(SingleFileRestoreTableModel model) {
        super(model);

        setRowHeight(Icons.getIconById(Icons.NODE_CONNECTED).getIconHeight() + 3);
        setColumnSelectionAllowed(false);
        setShowGrid(false);
        setupColumns();
        setDefaultRenderer(FileInfo.class, new MyDefaultTreeCellRenderer());
        getTableHeader().addMouseListener(new TableHeaderMouseListener());

        // Associate a header renderer with all columns.
        SortedTableHeaderRenderer.associateHeaderRenderer(model, getColumnModel(), 0, true);
    }

    /**
     * Sets the column sizes of the table
     */
    private void setupColumns() {
        int totalWidth = getWidth();

        // Otherwise the table header is not visible:
        getTableHeader().setPreferredSize(new Dimension(totalWidth, 20));

        TableColumn column = getColumn(getColumnName(0));
        column.setPreferredWidth(100);
        column = getColumn(getColumnName(1));
        column.setPreferredWidth(100);
        column = getColumn(getColumnName(2));
        column.setPreferredWidth(100);
        column = getColumn(getColumnName(3));
        column.setPreferredWidth(100);
    }

    public void setSelectedFileInfo(FileInfo selectedFileInfo) {
        if (selectedFileInfo != null) {
            for (int i = 0; i < getModel().getRowCount(); i++) {
                SingleFileRestoreItem item = (SingleFileRestoreItem) getModel().getValueAt(i, 1);;
                FileInfo value = item.getFileInfo();
                if (value.isVersionDateAndSizeIdentical(selectedFileInfo)) {
                    ListSelectionModel model = getSelectionModel();
                    model.setSelectionInterval(i, i);
                    break;
                }
            }
        }
    }

    public SingleFileRestoreItem getSelectedRestoreItem() {
        int row = getSelectedRow();
        if (row < 0) {
            return null;
        }
        return (SingleFileRestoreItem) getValueAt(row, 0);
    }

    /**
     * Listener on table header, takes care about the sorting of table
     *
     * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
     */
    private static class TableHeaderMouseListener extends MouseAdapter {
        public void mouseClicked(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)) {
                JTableHeader tableHeader = (JTableHeader) e.getSource();
                int columnNo = tableHeader.columnAtPoint(e.getPoint());
                TableColumn column = tableHeader.getColumnModel().getColumn(columnNo);
                int modelColumnNo = column.getModelIndex();
                TableModel model = tableHeader.getTable().getModel();
                if (model instanceof SingleFileRestoreTableModel) {
                    SingleFileRestoreTableModel restoreFilesTableModel = (SingleFileRestoreTableModel) model;
                    boolean freshSorted = restoreFilesTableModel.sortBy(modelColumnNo);
                    if (!freshSorted) {
                        // reverse list
                        restoreFilesTableModel.reverseList();
                    }
                }
            }
        }
    }

    private static class MyDefaultTreeCellRenderer extends DefaultTableCellRenderer {

        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            SingleFileRestoreItem restoreItem = (SingleFileRestoreItem) value;
            setIcon(null);
            String myValue = "";
            switch (column) {
                case SingleFileRestoreTableModel.COL_MODIFIED_DATE: // modified date
                    myValue = Format.formatDateShort(restoreItem.getFileInfo().getModifiedDate());
                    setHorizontalAlignment(RIGHT);
                    break;
                case SingleFileRestoreTableModel.COL_VERSION:  // version
                    myValue = Format.formatLong(restoreItem.getFileInfo().getVersion());
                    setHorizontalAlignment(RIGHT);
                    break;
                case SingleFileRestoreTableModel.COL_SIZE:  // size
                    myValue = Format.formatBytesShort(restoreItem.getFileInfo().getSize());
                    setHorizontalAlignment(RIGHT);
                    break;
                case SingleFileRestoreTableModel.COL_LOCAL:  // size
                    myValue = Format.formatBoolean(restoreItem.isLocal());
                    setHorizontalAlignment(RIGHT);
                    break;
            }

            Component c = super.getTableCellRendererComponent(table, myValue,
                    isSelected, hasFocus, row, column);

            if (!isSelected) {
                setBackground(row % 2 == 0 ? ColorUtil.EVEN_TABLE_ROW_COLOR
                        : ColorUtil.ODD_TABLE_ROW_COLOR);
            }

            return c;
        }
    }

}
