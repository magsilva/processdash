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

package net.sourceforge.processdash.data.util;


import java.util.Iterator;
import java.util.Vector;
import java.lang.Double;

import net.sourceforge.processdash.data.*;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.Filter;
import net.sourceforge.processdash.util.*;

import pspdash.Settings;

public class DataCorrelator {

    private DataRepository data;
    private String xName, yName;
    private Vector dataPoints;
    private Vector theFilter;
    public Vector dataNames;
    public LinearRegression r;
    public Correlation c;

    public DataCorrelator(DataRepository data, String xName, String yName,
                          Vector filter) {
        this.data  = data;
        this.xName = xName;
        this.yName = yName;
        this.theFilter = filter;

        recalc();
    }

    public Vector getDataNames () {
        return dataNames;
    }

    public Vector getDataPoints () {
        return dataPoints;
    }

    public void recalc() {
        scanRepository();
        r = new LinearRegression(dataPoints);
        c = new Correlation(dataPoints);
    }

    private boolean matchesFilter (String name) {
        return Filter.matchesFilter(theFilter, name);
    }

    private void scanRepository() {
        Iterator keys = data.getKeys();
        String name, xFullName, prefix, yFullName, completedFullName;
        int prefixPos;
        DoubleData x, y;
        DateData completed = null;
        double[] dataPoint;
        boolean onlyCompleted = onlyCompleted();

        dataPoints = new Vector();
        dataNames = new Vector();

        while (keys.hasNext()) {
            name = (String)keys.next();
            if (name.endsWith(xName) && matchesFilter(name)) try {
                prefix = name.substring(0, name.length() - xName.length());
                xFullName = name;
                yFullName = prefix + yName;
                completedFullName = prefix + "Completed";

                y = (DoubleData) data.getSimpleValue(yFullName);
                x = (DoubleData) data.getSimpleValue(xFullName);
                if (onlyCompleted)
                    completed = (DateData) data.getSimpleValue(completedFullName);

                if (numberIsOkay(x) && numberIsOkay(y) &&
                    (onlyCompleted == false || completed != null)) {

                    dataPoint = new double[] {x.getDouble(), y.getDouble()};
                    dataPoints.addElement(dataPoint);
                    dataNames.addElement(prefix);
                }
            } catch (Exception e) {};
        }
    }

    private static boolean numberIsOkay(DoubleData d) {
        if (d == null) return false;
        double dd = d.getDouble();
        if (Double.isNaN(dd) || Double.isInfinite(dd)) return false;
        return true;
    }

    private static Boolean onlyCompleted = null;
    private static boolean onlyCompleted() {
        if (onlyCompleted == null)
            onlyCompleted =
                new Boolean(Settings.getVal("probeDialog.onlyCompleted"));

        return onlyCompleted.booleanValue();
    }

}
