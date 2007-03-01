// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2005 Software Process Dashboard Initiative
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

package net.sourceforge.processdash.process;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import net.sourceforge.processdash.InternalSettings;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.Prop;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.net.http.WebServer;


public class LegacySupport {

    public static void fixupV13ScriptIDs(DashHierarchy hierarchy) {
        Hashtable brokenIDs = new Hashtable();
        brokenIDs.put("pspForEng/2A/script.htm", "PSP0.1-PFE-2A");
        brokenIDs.put("pspForEng/4A/script.htm", "PSP1-PFE-4A");
        brokenIDs.put("pspForEng/5A/script.htm", "PSP1.1-PFE-5A");
        brokenIDs.put("pspForEng/7A/script.htm", "PSP2-PFE-7A");
        brokenIDs.put("pspForEng/8A/script.htm", "PSP2.1-PFE-8A");
        brokenIDs.put("pspForMSE/2A/script.htm", "PSP0.1-MSE-2A");
        brokenIDs.put("pspForMSE/3B/script.htm", "PSP1-MSE-3B");
        brokenIDs.put("pspForMSE/4B/script.htm", "PSP1.0.1-MSE-4B");

        PropertyKey key;
        Prop        value;
        String      s;
        for (Iterator i = hierarchy.entrySet().iterator(); i.hasNext();) {
            Map.Entry e = (Map.Entry) i.next();
            key = (PropertyKey)e.getKey();
            value = (Prop)e.getValue();
            if (! Prop.hasValue(value.getID())) continue;
            if (! Prop.hasValue(s = value.getScriptFile ())) continue;
            s = (String) brokenIDs.get(s);
            if (s != null) value.setID(s);
        }
    }

    public static void configureRemoteListeningCapability(DataRepository data) {
        if (Settings.getBool(HTTP_CONFIGURATION_FLAG_SETTING, false))
            return;

        String remoteSetting =
            Settings.getVal(WebServer.HTTP_ALLOWREMOTE_SETTING);
        if ("never".equals(remoteSetting) &&
            WebServer.arePasswordsPresent(data))
            InternalSettings.set(WebServer.HTTP_ALLOWREMOTE_SETTING, "false");

        InternalSettings.set(HTTP_CONFIGURATION_FLAG_SETTING, "true");
    }
    private static final String HTTP_CONFIGURATION_FLAG_SETTING =
        "internal.ranListenerAutoConfig";
}