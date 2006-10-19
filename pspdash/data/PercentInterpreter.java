// PSP Dashboard - Data Automation Tool for PSP-like processes
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
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net


package pspdash.data;


class PercentInterpreter extends DoubleInterpreter {

    public PercentInterpreter(Repository r, String name,
                              int numDigits, boolean readOnly) {
        super(r, name, numDigits, readOnly);
    }

    public static String getString(double value, int numDigits) {
        String result = DoubleData.formatNumber(value * 100.0, numDigits);
        return (result.startsWith("#") ? result : result + "%");
    }


    public String getString() {
        if (value instanceof DoubleData && value.isDefined())
            return getString(((DoubleData) value).value, numDigits);
        else
            return super.getString();
    }

    public void setString(String s) throws MalformedValueException {
        // remove final "%" if it is present, then parse as usual.
        if (s != null) s = s.replace('%', ' ').trim();
        DoubleData d = new DoubleData(s);
        d.value /= 100.0;
        value = d;
    }

}