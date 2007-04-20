// Copyright (C) 2003-2006 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net


package net.sourceforge.processdash.ev;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.swing.tree.TreePath;

import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.data.DataComparator;
import net.sourceforge.processdash.data.DoubleData;
import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.net.cache.ObjectCache;
import net.sourceforge.processdash.util.StringUtils;


public class EVTaskListData extends EVTaskList
    implements DashHierarchy.Listener
{

    public static final String TASK_LIST_FLAG = "plain";
    public static final String TASK_ORDINAL_PREFIX = "TST_";
    public static final String EST_HOURS_DATA_NAME = "Planned Hours";
    protected DataRepository data;
    protected DashHierarchy hierarchy;

    public EVTaskListData(String taskListName,
                          DataRepository data,
                          DashHierarchy hierarchy,
                          boolean willNeedChangeNotification) {
        super(taskListName, taskListName, willNeedChangeNotification);
        this.data = data;
        this.hierarchy = hierarchy;

        addTasksFromData(data, taskListName);
        schedule = getSchedule(data, taskListName);
        loadID(taskListName, data, EST_HOURS_DATA_NAME);
        assignToOwner();
        calculator = new EVCalculatorData((EVTask) root, schedule);
        ((EVTask) root).flag = TASK_LIST_FLAG;
        if (willNeedChangeNotification)
            hierarchy.addHierarchyListener(this);
    }
    public boolean isEditable() { return true; }

    private void addTasksFromData(DataRepository data, String taskListName) {
        // search for tasks that belong to the named task list.
        SortedMap tasks = new TreeMap(DataComparator.getInstance());
        String ordinalDataName = TASK_ORDINAL_PREFIX + taskListName;
        findTasksInHierarchy(tasks, ordinalDataName, PropertyKey.ROOT);

        // now add each task found to the task list.
        Iterator i = tasks.values().iterator();
        boolean willNeedChangeNotification = (recalcListeners != null);
        while (i.hasNext())
            addTask((String) i.next(), data, hierarchy, null,
                    willNeedChangeNotification);
    }

    private void findTasksInHierarchy(SortedMap tasks, String ordinalDataName,
            PropertyKey key) {
        String path = key.path();
        String dataName = DataRepository.createDataName(path, ordinalDataName);
        SimpleData value = data.getSimpleValue(dataName);
        if (value != null)
            tasks.put(value, path);

        for (int i = 0; i < hierarchy.getNumChildren(key); i++)
            findTasksInHierarchy(tasks, ordinalDataName, hierarchy.getChildKey(
                    key, i));
    }

    private EVSchedule getSchedule(DataRepository data, String taskListName) {
        String globalPrefix = MAIN_DATA_PREFIX + taskListName;
        String dataName =
            DataRepository.createDataName(globalPrefix, EST_HOURS_DATA_NAME);
        SimpleData d = data.getSimpleValue(dataName);
        if (d instanceof StringData) d = ((StringData) d).asList();
        if (d instanceof ListData)
            return new EVSchedule((ListData) d);
        else
            return new EVSchedule();
    }
    protected void assignToOwner() {
        String owner = ProcessDashboard.getOwnerName(data);
        if (owner != null) {
            EVTask r = (EVTask) root;
            owner = StringUtils.findAndReplace(owner, ",", " ");
            r.assignedTo = Collections.singletonList(owner);
        }
    }


    public void save(String newName) {
        EVTask r = (EVTask) root;

        // First, compile a list of all the elements in the datafile that
        // were previously used to save this task list.  (That way we'll
        // know what we need to delete.)
        String globalPrefix = MAIN_DATA_PREFIX + taskListName + "/";
        String ordinalPrefix = "/" + TASK_ORDINAL_PREFIX + taskListName;
        Iterator i = data.getKeys();
        Set oldNames = new HashSet();
        String dataName;
        while (i.hasNext()) {
            dataName = (String) i.next();
            if (dataName.startsWith(globalPrefix) ||
                dataName.endsWith(ordinalPrefix))
                oldNames.add(dataName);
        }

        // Now, save the data to the repository.
        if (newName != null) {
            globalPrefix = MAIN_DATA_PREFIX + newName;
            ordinalPrefix = TASK_ORDINAL_PREFIX + newName;
            for (int j = r.getNumChildren();  j-- > 0;  ) {
                dataName = DataRepository.createDataName
                    (r.getChild(j).getFullName(), ordinalPrefix);
                data.putValue(dataName, new DoubleData(j, false));
                oldNames.remove(dataName);
            }
            // save the schedule
            dataName = DataRepository.createDataName
                (globalPrefix, EST_HOURS_DATA_NAME);
            data.putValue(dataName, schedule.getSaveList());
            oldNames.remove(dataName);
            // save the task list unique ID
            dataName = DataRepository.createDataName
                (globalPrefix, ID_DATA_NAME);
            data.putValue(dataName, StringData.create(taskListID));
            oldNames.remove(dataName);

            taskListName = newName;
        }

        // Finally, delete any old unused data elements.
        i = oldNames.iterator();
        while (i.hasNext())
            data.removeValue((String) i.next());

        // allow our tasks to do the same thing.
        r.saveStructuralData(newName);
        r.saveDependencyInformation();

        super.save(newName);
    }

    public void hierarchyChanged(DashHierarchy.Event e) {
        if (someoneCares()) {
            EVTask r = (EVTask) root;

            // delete all the previous children.
            int n = r.getNumChildren();
            int[] childIndices = new int[n];
            Object[] children = new Object[n];
            while (n-- > 0)
                children[(childIndices[n] = n)] = r.getChild(n);
            r.destroy();
            fireTreeNodesRemoved
                (this, ((EVTask) r).getPath(), childIndices, children);

            // add the new kids.
            addTasksFromData(data, taskListName);
            fireTreeStructureChanged(this, r.getPath(), null, null);
            recalc();
        }
    }

    public EVTask createAndAddTask(String path,
                                   DataRepository data,
                                   DashHierarchy hierarchy,
                                   ObjectCache cache,
                                   boolean willNeedChangeNotification) {
        EVTask newTask = new EVTask(taskListName, path, data, hierarchy,
                                    willNeedChangeNotification ? this : null);
        if (((EVTask) root).add(newTask))
            return newTask;
        else
            return null;
    }

    protected boolean checkRemovable(TreePath path) {
        return path.getPathCount() > 1;
    }

    protected int doRemoveTask(EVTask parent, EVTask child) {
        if (parent == (EVTask) root)
            return super.doRemoveTask(parent, child);
        else {
            child.setUserPruned(!child.isUserPruned());
            return -1;
        }
    }

    public boolean explodeTask(TreePath path) { // move to the data version
        // for now, only remove tasks which are children of the root.
        int pathLen = path.getPathCount();
        if (pathLen != 2) return false;

        EVTask parent = (EVTask) path.getPathComponent(pathLen-2);
        EVTask child  = (EVTask) path.getPathComponent(pathLen-1);
        if (child.getNumChildren() == 0) return false;
        int pos = parent.remove(child);

        List leafTasks = child.getLeafTasks();
        int[] insertedIndicies = new int[leafTasks.size()];
        Object[] insertedChildren = new Object[leafTasks.size()];
        Iterator i = leafTasks.iterator();
        EVTask leaf;
        int leafNum = 0;
        while (i.hasNext()) {
            leaf = (EVTask) i.next();
            leaf.getParent().remove(leaf);
            leaf.name = leaf.fullName.substring(1);
            parent.add(pos+leafNum, leaf);

            insertedIndicies[leafNum] = leafNum + pos;
            insertedChildren[leafNum] = leaf;
            leafNum++;
        }
        child.destroy();

        // send the appropriate TreeModel events.
        int[] removedIndices = new int[] { pos };
        Object[] removedChildren = new Object[] { child };
        EVTask[] parentPath = ((EVTask) parent).getPath();
        fireTreeNodesRemoved
            (this, parentPath, removedIndices, removedChildren);
        fireTreeNodesInserted
            (this, parentPath, insertedIndicies, insertedChildren);
        return true;
    }

    public void recalcLeavesOnly() {
        this.calculator = new EVCalculatorLeavesOnly((EVTask) root);
        if (this.recalcTimer != null)
            this.recalcTimer.setInitialDelay(10);
    }

    public void dispose() {
        hierarchy.removeHierarchyListener(this);
        super.dispose();
    }

    public static boolean validName(String taskListName) {
        return (taskListName != null &&
                taskListName.length() > 0 &&
                taskListName.indexOf('/') == -1);
    }

    public static boolean exists(DataRepository data, String taskListName) {
        String dataName = DataRepository.createDataName
            (MAIN_DATA_PREFIX + taskListName, EST_HOURS_DATA_NAME);
        return data.getSimpleValue(dataName) != null;
    }

    public static String taskListNameFromDataElement(String dataName) {
        if (dataName == null ||
            !dataName.startsWith(MAIN_DATA_PREFIX) ||
            !dataName.endsWith("/" + EST_HOURS_DATA_NAME))
            return null;

        return dataName.substring
            (MAIN_DATA_PREFIX.length(),
             dataName.length() - EST_HOURS_DATA_NAME.length() - 1);
    }

}