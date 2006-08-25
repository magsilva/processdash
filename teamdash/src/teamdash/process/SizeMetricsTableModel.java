package teamdash.process;

import java.awt.Component;
import java.awt.Font;

import javax.swing.DefaultCellEditor;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.table.TableColumn;

import teamdash.process.CustomProcess.Item;

public class SizeMetricsTableModel extends ItemListTableModel {

    static final String SIZE_ITEM = CustomProcess.SIZE_METRIC;
    private static final String LONG_NAME = CustomProcess.LONG_NAME;
    private static final String NAME = CustomProcess.NAME;
    private static final String PRODUCT_NAME = CustomProcess.PRODUCT_NAME;
    private static final String UNITS = CustomProcess.UNITS;
    private static final String ICON_STYLE = CustomProcess.ICON_STYLE;


    static final int NAME_COL = 0;
    static final int PRODUCT_NAME_COL = 1;
    static final int UNITS_COL = 2;
    static final int SHORT_NAME_COL = 3;
    static final int ICON_STYLE_COL = 4;

    private Item[] impliedSizeMetrics;

    public SizeMetricsTableModel(CustomProcess p) {
        super(p, SIZE_ITEM);

        impliedSizeMetrics = new Item[] {
                makeImpliedSizeMetric(p, "DLD Lines",
                        "Detailed Design Document", "Objects"),
                makeImpliedSizeMetric(p, "LOC", "Software Component", "Objects")
                };
    }



    private Item makeImpliedSizeMetric(CustomProcess p, String name,
            String productName, String units) {
        Item newMetric = p.new Item(SIZE_ITEM);
        newMetric.putAttr(LONG_NAME, name);
        newMetric.putAttr(NAME, name);
        newMetric.putAttr(PRODUCT_NAME, productName);
        newMetric.putAttr(UNITS, units);
        newMetric.putAttr(CustomProcess.READ_ONLY, "t");
        return newMetric;
    }


    public void insertItem(int pos) {
        Item newMetric = getProcess().new Item(SIZE_ITEM);
        newMetric.putAttr(LONG_NAME, "Enter Metric Name");
        newMetric.putAttr(NAME, "Enter Metric Name");
        newMetric.putAttr(PRODUCT_NAME, "Enter Product Name");
        newMetric.putAttr(UNITS, "Pages");
        newMetric.putAttr(ICON_STYLE, "document");
        super.insertItem(newMetric, pos);
    }

    public int getRowCount() {
        return super.getRowCount() + 2;
    }

    public Item get(int pos) {
        int realItemCount = super.getRowCount();
        if (pos < realItemCount)
            return super.get(pos);
        else
            return impliedSizeMetrics[pos - realItemCount];
    }




    private static final String[] columnNames = {
        "Metric Name", "Work Product Name", "Type" };

    public int getColumnCount() {
        return 3;
    }

    public String getColumnName(int col) { return columnNames[col]; }

    private static final String[] columnAttrs = { LONG_NAME,
            PRODUCT_NAME, UNITS, NAME, ICON_STYLE };

    protected String[] getColumnAttrs() {
        return columnAttrs;
    }

    protected boolean isStructuralColumn(int column) {
        return (column == NAME_COL || column == PRODUCT_NAME_COL);
    }




    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        super.setValueAt(aValue, rowIndex, columnIndex);

        if (columnIndex == NAME_COL)
            super.setValueAt(aValue, rowIndex, SHORT_NAME_COL);

        else if (columnIndex == UNITS_COL) {
            if ("Pages".equalsIgnoreCase((String) aValue))
                super.setValueAt("document", rowIndex, ICON_STYLE_COL);
            else
                super.setValueAt(null, rowIndex, ICON_STYLE_COL);
        }
    }


    protected Object getItemDisplay(int column, Object value) {
        if (column == UNITS_COL) {
            String text = (String) value;
            return getUnitsDisplay(text);
        } else {
            return value;
        }
    }


    public JTable createJTable() {
        JTable table = new JTable(this);

        // adjust column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(100);
        table.getColumnModel().getColumn(1).setPreferredWidth(200);
        table.getColumnModel().getColumn(2).setPreferredWidth(100);

        table.setRowHeight(table.getRowHeight()+4);

        // draw read-only phases with a different appearance
        table.setDefaultRenderer(String.class, new ItemTableCellRenderer());

        // install a combo box as the editor for the "phase type" column
        TableColumn unitsColumn = table.getColumnModel().getColumn(UNITS_COL);
        JComboBox unitsEditor = new JComboBox(UNITS_TYPES);
        unitsEditor.setRenderer(new UnitsCellRenderer());
        unitsEditor.setFont
            (unitsEditor.getFont().deriveFont(Font.PLAIN));
        unitsColumn.setCellEditor(new DefaultCellEditor(unitsEditor));

        return table;
    }

    private class UnitsCellRenderer extends DefaultListCellRenderer {

        public Component getListCellRendererComponent(JList list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            value = getUnitsDisplay((String) value);
            return super.getListCellRendererComponent(list, value, index,
                    isSelected, cellHasFocus);
        }

    }


    private static final String[] UNITS_TYPES = { "Pages", "Objects" };


    public static String getUnitsDisplay(String unitsName) {
        if ("pages".equalsIgnoreCase(unitsName))
            return "Document";
        else
            return "Other";
    }







}
