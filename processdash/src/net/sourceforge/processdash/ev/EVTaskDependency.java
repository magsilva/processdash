// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2006 Software Process Dashboard Initiative
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

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.ImmutableStringData;
import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.util.XMLUtils;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class EVTaskDependency {

    private static final Logger logger = Logger
            .getLogger(EVTaskDependency.class.getName());

    private String taskID;

    private String displayName;

    public EVTaskDependency(String taskID, String displayName) {
        this.taskID = taskID;
        this.displayName = displayName;
    }

    public EVTaskDependency(Element e) {
        this.taskID = getAttr(e, TASK_ID_ATTR);
        this.displayName = getAttr(e, DISPLAY_NAME_ATTR);
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getTaskID() {
        return taskID;
    }

    public boolean equals(Object obj) {
        if (obj instanceof EVTaskDependency) {
            EVTaskDependency that = (EVTaskDependency) obj;
            return this.taskID.equals(that.taskID);
        } else
            return false;
    }

    public int hashCode() {
        return taskID.hashCode();
    }

    public void getAsXML(StringBuffer out) {
        out.append("<").append(DEPENDENCY_TAG);
        addAttr(out, TASK_ID_ATTR, getTaskID());
        addAttr(out, DISPLAY_NAME_ATTR, getDisplayName());
        out.append("/>");
    }

    private void addAttr(StringBuffer out, String name, String value) {
        if (XMLUtils.hasValue(value))
            out.append(" ").append(name).append("='").append(
                    XMLUtils.escapeAttribute(value)).append("'");
    }

    private String getAttr(Element e, String name) {
        String result = e.getAttribute(name);
        if (XMLUtils.hasValue(result))
            return result;
        else
            return null;
    }


    public static void addTaskID(DataContext data, String taskPath, String id) {
        String dataName = DataRepository.createDataName(taskPath,
                TASK_ID_DATA_NAME);
        SimpleData currentValue = data.getSimpleValue(dataName);
        ListData newValue;
        if (currentValue instanceof ListData)
            newValue = (ListData) currentValue;
        else
            newValue = new ListData();
        if (newValue.setAdd(id))
            data.putValue(dataName, newValue);
    }

    public static void removeTaskID(DataContext data, String taskPath, String id) {
        String dataName = DataRepository.createDataName(taskPath,
                TASK_ID_DATA_NAME);
        SimpleData currentValue = data.getSimpleValue(dataName);
        if (currentValue instanceof ListData) {
            ListData list = (ListData) currentValue;
            if (list.remove(id))
                data.putValue(dataName, list);
        }
    }

    public static boolean addTaskDependencies(DataContext data,
            String taskPath, List dependencies) {
        return addTaskDependencies(data, taskPath, dependencies, false);
    }

    public static boolean addTaskDependencies(DataContext data,
            String taskPath, List dependencies, boolean whatIfMode) {

        if (dependencies == null || dependencies.isEmpty())
            return false;

        boolean madeChange = false;
        List list = getDependencies(data, taskPath);
        if (list == null) {
            list = dependencies;
            madeChange = true;
        } else {
            for (Iterator i = dependencies.iterator(); i.hasNext();) {
                EVTaskDependency d = (EVTaskDependency) i.next();
                if (!list.contains(d)) {
                    list.add(d);
                    madeChange = true;
                }
            }
        }

        if (madeChange && whatIfMode == false)
            saveDependencies(data, taskPath, list);

        return madeChange;
    }

    public static List getDependencies(DataContext data, String taskPath) {
        String dataName = DataRepository.createDataName(taskPath,
                TASK_DEPENDENCIES_DATA_NAME);
        SimpleData currentValue = data.getSimpleValue(dataName);
        if (currentValue == null || !currentValue.test())
            return null;

        List result = new LinkedList();
        try {
            Element e = XMLUtils.parse(currentValue.format())
                    .getDocumentElement();
            NodeList nl = e.getElementsByTagName(DEPENDENCY_TAG);
            for (int i = 0; i < nl.getLength(); i++) {
                result.add(new EVTaskDependency((Element) nl.item(i)));
            }

        } catch (Exception e) {
            logger.log(Level.WARNING, "Unable to parse dependencies for "
                    + taskPath, e);
        }
        return result;
    }

    private static void saveDependencies(DataContext data, String taskPath,
            Collection dependencies) {
        StringBuffer xml = new StringBuffer();
        xml.append("<list>");
        for (Iterator i = dependencies.iterator(); i.hasNext();) {
            EVTaskDependency d = (EVTaskDependency) i.next();
            d.getAsXML(xml);
        }
        xml.append("</list>");

        String dataName = DataRepository.createDataName(taskPath,
                TASK_DEPENDENCIES_DATA_NAME);
        data.putValue(dataName, new ImmutableStringData(xml.toString()));
    }


    private static final String TASK_ID_DATA_NAME = "EV_Task_IDs";

    private static final String TASK_DEPENDENCIES_DATA_NAME = "EV_Task_Dependencies";

    private static final String DEPENDENCY_TAG = "dependency";

    private static final String TASK_ID_ATTR = "tid";

    private static final String DISPLAY_NAME_ATTR = "name";
}
