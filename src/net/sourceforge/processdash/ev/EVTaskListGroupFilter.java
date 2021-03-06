// Copyright (C) 2016-2017 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ev;

import java.util.Collections;
import java.util.Set;

import net.sourceforge.processdash.team.group.UserFilter;
import net.sourceforge.processdash.team.group.UserGroup;

public class EVTaskListGroupFilter implements EVTaskListFilter {

    private Set<String> datasetIDs;

    public EVTaskListGroupFilter(UserFilter f) {
        if (f == null)
            this.datasetIDs = Collections.EMPTY_SET;
        else if (!UserGroup.isEveryone(f))
            this.datasetIDs = f.getDatasetIDs();
    }

    @Override
    public boolean include(String taskListID) {
        if (datasetIDs == null)
            // a null datasetIDs set is the indicator of the "everyone" group.
            return true;
        else if (datasetIDs.isEmpty())
            return false;

        String datasetID = ImportedEVManager.getInstance().getSrcDatasetID(
            taskListID);
        return datasetIDs.contains(datasetID);
    }

}
