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

package net.sourceforge.processdash.data.compiler;

import net.sourceforge.processdash.data.SaveableData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;

/** This class creates a local namespace around another expression
 *  context, allowing the data element [_] to be locally set.
 */
public class LocalExpressionContext implements ExpressionContext {

    public static final String LOCALVAR_NAME = "_";

    private ExpressionContext context;
    private SimpleData localValue = null;

    public LocalExpressionContext(ExpressionContext context) {
        this.context = context;
    }

    public void setLocalValue(SimpleData value) {
        localValue = value;
    }

    public void setLocalValue(Object v) {
        if (v instanceof SimpleData)
            setLocalValue((SimpleData) v);
        else if (v instanceof String)
            setLocalValue(StringData.create((String) v));
        else if (v instanceof SaveableData)
            setLocalValue(((SaveableData) v).getSimpleValue());
        else
            setLocalValue(null);
    }

    public SimpleData get(String dataName) {
        if ("_".equals(dataName))
            return localValue;
        else
            return context.get(dataName);
    }

    public String resolveName(String dataName) {
        if ("_".equals(dataName))
            return null;
        else
            return context.resolveName(dataName);
    }
}