// PSP Dashboard - Data Automation Tool for PSP-like processes
// Copyright (C) 1999  United States Air Force
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
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  ken.raisor@hill.af.mil


package pspdash.data;


import netscape.javascript.JSObject;


class NSCheckboxField extends NSField {

    // The values of these literals are an unending source of iritation.
    // Each version of Netscape seems to want different values for true
    // and false...Netscape 4.0 wanted Double(1) and Double(0), for example.
    // As of 4.75, it appears that getMember("checked") will return
    // Boolean.TRUE or Boolean.FALSE.  But setMember("checked", Boolean.FALSE)
    // causes the box to become checked again...?!?  setMember("checked", null)
    // unchecks the box.  So after much trial and error, these are the values
    // that must be used for it to work... :-b
    private static Object HTML_TRUE = Boolean.TRUE;
    private static Object HTML_FALSE = null;


    public NSCheckboxField(JSObject element, Repository data, String dataPath) {
        super(element, data, dataPath);
    }


    public void fetch() {
        variantValue = i.getBoolean();
    }

    public void paint() {
        element.setMember
            ("checked", (Boolean.TRUE.equals(variantValue) ? HTML_TRUE : HTML_FALSE));
    }

    public void parse() {
        variantValue = new Boolean(HTML_TRUE.equals(element.getMember("checked")));
    }

}
