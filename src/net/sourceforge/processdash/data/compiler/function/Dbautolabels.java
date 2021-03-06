// Copyright (C) 2013 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 3
// of the License, or (at your option) any later version.
//
// Additional permissions also apply; see the README-license.txt
// file in the project root directory for more information.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.data.compiler.function;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;

import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.compiler.ExpressionContext;
import net.sourceforge.processdash.ev.TaskLabeler;
import net.sourceforge.processdash.tool.db.QueryRunner;
import net.sourceforge.processdash.tool.db.QueryUtils;
import net.sourceforge.processdash.util.PatternList;
import net.sourceforge.processdash.util.StringUtils;

public class Dbautolabels extends DbAbstractFunction {

    private static final String COMPLETED_TASKS = "Completed_Tasks";

    private static final String COMPLETED_COMPONENTS = "Completed_Components";

    static final String[] AUTO_LABEL_NAMES = { COMPLETED_TASKS,
            COMPLETED_COMPONENTS };

    /**
     * Perform a procedure call.
     * 
     * This method <b>must</b> be thread-safe.
     * 
     * Expected arguments: (project selection criteria)
     */
    public Object call(List arguments, ExpressionContext context) {
        // get the object for executing database queries
        QueryRunner queryRunner = getDbObject(context, QueryRunner.class);
        if (queryRunner == null)
            return null;

        // retrieve the criteria that should be used to narrow the query
        List criteria = collapseLists(arguments, 0);

        try {
            ListData result = new ListData();
            calcCompletedTasks(context, criteria, result);
            calcCompletedComponents(queryRunner, criteria, result);
            return result;

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error while calculating", e);
            return null;
        }
    }


    /**
     * Calculate the task IDs for tasks that have been completed.
     */
    private void calcCompletedTasks(ExpressionContext context, List criteria,
            ListData result) throws Exception {

        List<String> completedTaskIDs = queryHql(context, BASE_TASK_QUERY, "f",
            criteria);
        filterPspTaskPhaseIDs(completedTaskIDs);

        result.add(TaskLabeler.LABEL_PREFIX + COMPLETED_TASKS);
        result.add(TaskLabeler.LABEL_HIDDEN_MARKER);
        for (String oneItem : completedTaskIDs)
            result.add(oneItem);
    }

    private static final String BASE_TASK_QUERY = "select "
            + "f.planItem.identifier " //
            + "from TaskStatusFact f " //
            + "where f.versionInfo.current = 1 " //
            + "group by f.planItem.identifier " //
            + "having max(f.actualCompletionDateDim.key) < 99990000";

    private void filterPspTaskPhaseIDs(List<String> completedTasks) {
        // The database query will return individual results for each phase in
        // a PSP task.  Those results are incompatible with the other label
        // sources in a team project, which attach labels to the parent PSP
        // task instead.  This method fixes that discrepancy.

        // First we scan the list of task IDs looking for PSP phases. We remove
        // the phase entries from the list, and keep track of the number of
        // completed phases we find for each PSP task.
        HashMap<String, Integer> pspTasks = new HashMap();
        for (Iterator i = completedTasks.iterator(); i.hasNext();) {
            String taskID = (String) i.next();
            if (PSP_PHASES.matches(taskID)) {
                i.remove();
                int colonPos = taskID.lastIndexOf(':');
                String pspTaskID = taskID.substring(0, colonPos);
                Integer numCompletedPhases = pspTasks.get(pspTaskID);
                numCompletedPhases = (numCompletedPhases == null ? 1
                        : numCompletedPhases + 1);
                pspTasks.put(pspTaskID, numCompletedPhases);
            }
        }

        // Now look through the PSP tasks we found.  Any tasks that had 8
        // completed phases will represent a completed PSP project.  Add those
        // completed PSP projects to our completed task list.
        for (Entry<String, Integer> e : pspTasks.entrySet()) {
            if (NUM_PSP_PHASES.equals(e.getValue()))
                completedTasks.add(e.getKey());
        }
    }

    private static final PatternList PSP_PHASES = new PatternList();
    static {
        PSP_PHASES.addLiteralEndsWith(":Planning");
        PSP_PHASES.addLiteralEndsWith(":Design");
        PSP_PHASES.addLiteralEndsWith(":Design Review");
        PSP_PHASES.addLiteralEndsWith(":Code");
        PSP_PHASES.addLiteralEndsWith(":Code Review");
        PSP_PHASES.addLiteralEndsWith(":Compile");
        PSP_PHASES.addLiteralEndsWith(":Test");
        PSP_PHASES.addLiteralEndsWith(":Postmortem");
    }
    private static final Integer NUM_PSP_PHASES = 8;


    /**
     * Calculate the task IDs for tasks underneath completed components
     */
    private void calcCompletedComponents(QueryRunner queryRunner,
            List criteria, ListData result) throws Exception {
        // build the effective query and associated argument list
        List queryArgs = new ArrayList();

        StringBuilder baseQuery = new StringBuilder(BASE_COMPONENT_QUERY);
        QueryUtils.addCriteriaToHql(baseQuery, "f", queryArgs, criteria);

        StringBuilder outerQuery = new StringBuilder(OUTER_COMPONENT_QUERY);
        QueryUtils.addCriteriaToHql(outerQuery, "p", queryArgs, criteria);

        String query = outerQuery.toString();
        query = StringUtils.findAndReplace(query, "p.planItem", "pe");
        query = StringUtils.findAndReplace(query, "SUBQUERY",
            baseQuery.toString());

        // if we know that the query won't return any result, don't bother
        // running it against the database.
        if (query.indexOf(QueryUtils.IMPOSSIBLE_CONDITION) != -1)
            return;

        // run the query
        Object[] queryArgArray = queryArgs.toArray();
        List<String> rawData = queryRunner.queryHql(query, queryArgArray);

        // extract the results
        result.add(TaskLabeler.LABEL_PREFIX + COMPLETED_COMPONENTS);
        result.add(TaskLabeler.LABEL_HIDDEN_MARKER);
        for (String oneItem : rawData)
            result.add(oneItem);
    }

    private static final String BASE_COMPONENT_QUERY = "select "
            + "f.planItem.wbsElement.key " //
            + "from TaskStatusFact f " //
            + "where f.versionInfo.current = 1 " //
            + "group by f.planItem.wbsElement.key " //
            + "having max(f.actualCompletionDateDim.key) < 99990000";

    private static final String OUTER_COMPONENT_QUERY = "select " //
            + "pi.identifier " //
            + "from PlanItem pi, PlanItem pe " //
            + "where pi.wbsElement.key = pe.wbsElement.key "
            + "and pi.project.key = pe.project.key "
            + "and pe.leafComponent = 1 " //
            + "and pe.wbsElement.key in (SUBQUERY)";

}
