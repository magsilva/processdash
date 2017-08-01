// Copyright (C) 2017 Tuma Solutions, LLC
// REST API Add-on for the Process Dashboard
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

package net.sourceforge.processdash.rest.to;

import java.util.Date;
import java.util.LinkedHashMap;

public class JsonMap extends LinkedHashMap {

    public JsonMap(Object... values) {
        for (int i = 0; i < values.length; i += 2)
            set((String) values[i], values[i + 1]);
    }

    public JsonMap set(String name, Object value) {
        if (value instanceof JsonDate)
            put(name, value);
        else if (value instanceof Date)
            put(name, new JsonDate((Date) value));
        else if (value != null)
            put(name, value);
        return this;
    }

    public <T> T getAttr() {
        String methodName = new Exception().getStackTrace()[1].getMethodName();
        if (!methodName.startsWith("get"))
            throw new IllegalStateException();
        String attrName = methodName.substring(3, 4).toLowerCase()
                + methodName.substring(4);
        return (T) super.get(attrName);
    }

}
