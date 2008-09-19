package teamdash.wbs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Insets;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultCellEditor;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableColumn;

/** A graphical user interface for editing common workflows.
 */
public class WorkflowEditor {

    /** The team project that these workflows belong to. */
    TeamProject teamProject;
    /** The data model for the workflows */
    WorkflowModel workflowModel;
    /** The table to display the workflows in */
    WBSJTable table;
    /** The frame containing this workflow editor */
    JFrame frame;
    /** A toolbar for editing the workflows */
    JToolBar toolBar;
    /** An object for tracking undo operations */
    UndoList undoList;

    public WorkflowEditor(TeamProject teamProject) {
        this.teamProject = teamProject;
        this.workflowModel = new WorkflowModel
            (teamProject.getWorkflows(), teamProject.getTeamProcess());
        this.workflowModel.setEditingEnabled(teamProject.isReadOnly() == false);
        table = createWorkflowJTable
            (workflowModel, teamProject.getTeamProcess());

        undoList = new UndoList(workflowModel.getWBSModel());
        undoList.setForComponent(table);
        workflowModel.addTableModelListener(new UndoableEventRepeater());

        table.setEditingEnabled(teamProject.isReadOnly() == false);
        buildToolbar();
        frame = new JFrame(teamProject.getProjectName() +
                           " - Common Team Workflows");
        frame.getContentPane().add(new JScrollPane(table));
        frame.getContentPane().add(toolBar, BorderLayout.NORTH);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(600, 400);
        frame.setVisible(true);
    }

    public void show() {
        frame.setVisible(true);
        frame.toFront();
    }

    public void hide() {
        frame.setVisible(false);
    }


    public static WBSJTable createWorkflowJTable(WorkflowModel workflowModel, TeamProcess process) {
        // create the WBSJTable, then set its model to the workflow data model.
        WBSJTable table = new WBSJTable
            (workflowModel.getWBSModel(), process.getIconMap(),
             process.getNodeTypeMenu());
        table.setModel(workflowModel);
        // reset the row height, for proper display of wbs node icons.
        table.setRowHeight(19);
        // don't allow reordering, since the text displayed in several of the
        // columns is meant to be read from left to right.
        table.getTableHeader().setReorderingAllowed(false);
        // install the default editor for table data.
        table.setDefaultEditor(Object.class, new WorkflowCellEditor());
        table.selfName = "team workflow structure";

        TableColumn col;

        // customize the display of the "Name" column.
        col = table.getColumn("Name");
        col.setPreferredWidth(300);

        // customize the display of the "%" column.
        col = table.getColumn("%");
        DataTableStringSuffixRenderer render =
            new DataTableStringSuffixRenderer("% of ");
        render.setHorizontalAlignment(JLabel.RIGHT);
        col.setCellRenderer(render);
        col.setPreferredWidth(60);

        // customize the display of the "Rate" column.
        col = table.getColumn("Rate");
        col.setPreferredWidth(50);

        // customize the display and editing of the "Units" column.
        col = table.getColumn("Units");
        col.setCellRenderer(new DataTableStringSuffixRenderer(" per Hour"));
        CustomEditedColumn edCol =
            (CustomEditedColumn) workflowModel.getColumn(col.getModelIndex());
        col.setCellEditor(edCol.getCellEditor());
        col.setPreferredWidth(130);

        // customize the display of the "# People" column.
        col = table.getColumn("# People");
        col.setPreferredWidth(60);

        return table;
    }

    private void buildToolbar() {
        toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setMargin(new Insets(0,0,0,0));

        addToolbarButton(undoList.getUndoAction());
        addToolbarButton(undoList.getRedoAction());
        addToolbarButtons(table.getEditingActions());
        toolBar.addSeparator();
        addToolbarButtons(getWorkflowActions());
    }

    /** Add one or more buttons to the internal tool bar */
    private void addToolbarButtons(Action[] actions) {
        for (int i = 0; i < actions.length; i++)
            if (actions[i].getValue(Action.SMALL_ICON) != null)
                addToolbarButton(actions[i]);
    }

    /** Add a button to the internal tool bar */
    private void addToolbarButton(Action a) {
        JButton button = new JButton(a);
        //button.setMargin(new Insets(0, 0, 0, 0));
        button.setFocusPainted(false);
        button.setToolTipText((String) a.getValue(Action.NAME));
        button.setText(null);

        Icon icon = button.getIcon();
        if (icon != null && !(icon instanceof ImageIcon))
            button.setDisabledIcon(IconFactory.getModifiedIcon(icon,
                    IconFactory.DISABLED_ICON));

        toolBar.add(button);
    }

    public void addChangeListener(ChangeListener l) {
        undoList.addChangeListener(l);
    }

    public void removeChangeListener(ChangeListener l) {
        undoList.removeChangeListener(l);
    }

    private static class WorkflowCellEditor extends DefaultCellEditor {

        public WorkflowCellEditor() {
            super(new JTextField());
        }

        @Override
        public Component getTableCellEditorComponent(JTable table,
                Object value, boolean isSelected, int row, int column) {
            Component result = super.getTableCellEditorComponent(table,
                ErrorValue.unwrap(value), isSelected, row, column);

            if (result instanceof JTextField)
                ((JTextField) result).selectAll();

            return result;
        }

    }

    private class ExportAction extends AbstractAction {
        public ExportAction() {
            super("Export...", IconFactory.getExportIcon());
            super.putValue(SHORT_DESCRIPTION, "Export Workflows");
        }
        public void actionPerformed(ActionEvent e) {
            try {
                new WorkflowLibraryEditor(teamProject, frame, true);
            } catch (WorkflowLibraryEditor.UserCancelledException uce) {
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
    final Action EXPORT = new ExportAction();



    private class ImportAction extends AbstractAction {
        public ImportAction() {
            super("Import...", IconFactory.getImportIcon());
            super.putValue(SHORT_DESCRIPTION, "Import Workflows");
        }
        public void actionPerformed(ActionEvent e) {
            try {
                new WorkflowLibraryEditor(teamProject, frame, false);
                undoList.madeChange("Imported workflows");
            } catch (WorkflowLibraryEditor.UserCancelledException uce) {
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
    final Action IMPORT = new ImportAction();



    public Action[] getWorkflowActions() {
        if (teamProject.isReadOnly())
            IMPORT.setEnabled(false);
        return new Action[] { IMPORT, EXPORT };
    }

    private class UndoableEventRepeater implements TableModelListener {

        public void tableChanged(TableModelEvent e) {
            if (e.getColumn() > 0 && e.getFirstRow() > 0
                    && e.getFirstRow() == e.getLastRow())
                undoList.madeChange("Edited value");
        }

    }

    /*
    private Set saveListeners = null;
    public void addSaveListener(SaveListener l) {
        if (saveListeners == null)
            saveListeners = new HashSet();
        saveListeners.add(l);
    }
    public void removeSaveListener(SaveListener l) {
        if (saveListeners != null)
            saveListeners.remove(l);
    }
    */
}