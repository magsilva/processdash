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

package net.sourceforge.processdash.data.compiler.function;

import java.util.Iterator;
import java.util.List;

import net.sourceforge.processdash.data.DateData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.data.compiler.AbstractFunction;
import net.sourceforge.processdash.data.compiler.ExpressionContext;
import net.sourceforge.processdash.data.repository.DataRepository;

public class Maxdate extends AbstractFunction {

    /** Perform a procedure call.
     *
     * This method <b>must</b> be thread-safe.
     */
    public Object call(List arguments, ExpressionContext context)
    {
        if (getArg(arguments, 0) instanceof StringData)
            return maxDateFor(arguments, context);
        else
            return maxDateOf(arguments, context);
    }

    private Object maxDateFor(List arguments, ExpressionContext context)
    {
        DateData result = null;
        Object val;

        String name = asString(getArg(arguments, 0));
        if (name == null) return null;

        Iterator i = collapseLists(arguments, 1).iterator();
        String path, dataName;
        while (i.hasNext()) {
            path = asStringVal(i.next());
            if (path == null) continue;

            dataName = DataRepository.createDataName(path, name);
            val = context.get(dataName);
            if (val == null)
                return null;
            if (val instanceof DateData &&
                (result == null || result.lessThan((DateData) val)))
                result = (DateData) val;
        }
        return result;
    }

    private Object maxDateOf(List arguments, ExpressionContext context)
    {
        DateData result = null;
        Object val;

        Iterator i = collapseLists(arguments, 0).iterator();
        while (i.hasNext()) {
            val = i.next();
            if (val == null)
                return null;
            if (val instanceof DateData &&
                (result == null || result.lessThan((DateData) val)))
                result = (DateData) val;
        }
        return result;
    }
}
