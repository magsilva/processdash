// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2003 Software Process Dashboard Initiative
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

import net.sourceforge.processdash.net.cache.CachedObject;
import net.sourceforge.processdash.net.cache.ObjectCache;

public class EVTaskListCached extends EVTaskListXMLAbstract {

    public static final String CACHED_OBJECT_TYPE =
        "pspdash.EVTaskListCached"; // legacy - OK
    public static final String LOCAL_NAME_ATTR = "LocalName";

    private int objectID;
    private String localName;
    private CachedObject object;

    private String xmlSource = null;

    private EVTaskListCached() { super(" ", " ", false); }

    public EVTaskListCached(String taskListName, ObjectCache cache) {
        super(taskListName, taskListName, false);

        int slashPos = taskListName.indexOf('/');
        localName = taskListName.substring(slashPos+1);
        objectID = Integer.parseInt(taskListName.substring(6, slashPos));

        object = cache.getCachedObject(objectID, -1);
        openXML();
    }


    private boolean openXML() {
        if (object == null) {
            createErrorRootNode
                (localName, resources.getString("Cannot_Open_Schedule_Error"));
            return false;
        }

        String xmlDoc = object.getString("UTF-8");
        String errorMessage = null;
        if (object.olderThanAge(3)) {
            errorMessage = object.getErrorMessage();
            String errorPrefix =
                (object.olderThanAge(5)
                 ? resources.getString("Out_Of_Date_Error")
                 : " " + resources.getString("Out_Of_Date_Warning")) + " ";
            if (errorMessage == null)
                errorMessage = errorPrefix;
            else if (xmlDoc != null)
                errorMessage = errorPrefix + errorMessage;
        }


        return openXML(xmlDoc, localName, errorMessage);
    }


    public void recalc() {
        if (object != null)
            object.refresh(0, 1000);
        openXML();

        super.recalc();
    }

    public static boolean validName(String taskListName) {
        return (taskListName != null &&
                taskListName.startsWith("cache:"));
    }

    public static boolean exists(String taskListName, ObjectCache cache) {
        try {
            if (taskListName == null) return false;

            int slashPos = taskListName.indexOf('/');
            if (slashPos < 7) return false;
            int objectID =
                Integer.parseInt(taskListName.substring(6, slashPos));
            CachedObject object = cache.getCachedObject(objectID, -1);
            return (object != null &&
                    CACHED_OBJECT_TYPE.equals(object.getType()));
        } catch (Exception e) { }
        return false;
    }

    public static String buildTaskListName(int id, String displayName) {
        return "cache:" + id + "/" + displayName;
    }

    public static String getNameFromXML(String xmlDoc) {
        EVTaskListCached tl = new EVTaskListCached();
        if (tl.openXML(xmlDoc, null))
            return ((EVTask) tl.root).name;
        else
            return null;
    }

}
