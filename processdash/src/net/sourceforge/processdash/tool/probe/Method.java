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

package net.sourceforge.processdash.tool.probe;


import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Vector;

import net.sourceforge.processdash.data.DoubleData;



class Method implements Comparable {

    /** A list of observations (in HTML format) made during the
     * calculation of this estimate. */
    Vector observations;

    /** A list of messages (in HTML format) describing errors
     * encountered during the calculation of this estimate. */
    Vector errorMessages;

    /** The rating given to this method. */
    double rating = 0.0;
    protected static final double CANNOT_CALCULATE = -100.0;
    protected static final double SERIOUS_PROBLEM = -10.0;
    protected static final double PROBE_METHOD_D = 5.0;

    /** Estimated object LOC */
    double estObjLOC;

    /** Historical data */
    HistData histData;


    /** Perform the calculations required for this PROBE Method. */
    public Method(HistData data, double estObjLOC,
                  String letter, String purpose) {
        this.observations = new Vector();
        this.errorMessages = new Vector();
        this.estObjLOC = estObjLOC;
        this.histData = data;
        this.rating = PROBE_METHOD_D;
        this.methodLetter = letter;
        this.methodPurpose = purpose;
    }

    /** Return a confidence rating describing the relative quality
     * of the estimate generated by this probe method.
     *
     * Various ratings and their meanings:<ul>
     *    <li> rating &gt;= +10 - excellent
     *    <li> rating = 5 - PROBE method D
     *    <li> rating = 0 - ambivalent
     *    <li> -1 &lt; rating &lt; 0 - "not recommended"
     *    <li> rating &lt;= -1.0 - "cannot calculate"
     * </ul>
     * The user will not be given an option of selecting methods
     * which have negative ratings. This method should not return
     * NaN or Infinity.
     */
    public double getRating() { return rating; }


    /** implement the <code>Comparable</code> interface. */
    public int compareTo(Object o) {
        double myRating = getRating();
        double yourRating = ((Method) o).getRating();
        if (myRating > yourRating) return -1; // sort descending
        if (myRating < yourRating) return 1;
        return 0;
    }

    protected String methodLetter;
    String getMethodLetter() { return methodLetter; }
    protected String methodPurpose;
    String getMethodPurpose() { return methodPurpose; }
    String getMethodName() {
        return getMethodLetter() + " for " + getMethodPurpose();
    }

    protected String TUT_SUFFIX = "tut.htm";
    public void useAltTutorial() { TUT_SUFFIX = "tut2.htm"; }

    public void printRow(PrintWriter out, boolean isBest, boolean isSelected) {
        out.print("<tr><td valign=middle>");

        if (getRating() > 0)
            printOption(out, isSelected);
        else
            out.print(NBSP);

        out.print("</td><td valign=middle>&nbsp;<br>");

        String link = ("<a href='"+getMethodPurpose()+getMethodLetter()+
                       TUT_SUFFIX+"' "+ProbeWizard.LINK_ATTRS+">");
        Iterator i;
        if (errorMessages.size() != 0) {

            out.print("<i>You " +
                      (getRating() > CANNOT_CALCULATE ?"should not" :"cannot")+
                      " use "+link+"PROBE method "+getMethodLetter()+"</a>"+
                      " for "+getMethodPurpose()+".\n");
            i = errorMessages.iterator();
            while (i.hasNext())
                out.println((String) i.next());
            out.print("</i>");

        } else {
            if (isBest)
                out.println("Your best option for estimating " +
                            getMethodPurpose() + " could be "+link+
                            "PROBE method " + getMethodLetter() + "</a>.");
            else
                out.println("Your next best option is probably "+link+
                            "PROBE method " + getMethodLetter() + "</a>.");

            i = observations.iterator();
            while (i.hasNext())
                out.println((String) i.next());
        }

        out.print("<br>&nbsp;</td><td valign=middle>");

        if (getRating() > CANNOT_CALCULATE)
            printChart(out);
        else
            out.print(NBSP);

        out.print("</td></tr>");
    }
    void printOption(PrintWriter out, boolean isSelected) { out.print(NBSP); }

    void printOption(PrintWriter out, double estimate, boolean isSelected,
                     double beta0, double beta1, double range,
                     double percent, double rSquared) {
        String purpose = getMethodPurpose();
        String letter = getMethodLetter();
        String qual = purpose + letter;
        out.print("<input type='radio' ");
        if (isSelected) out.print("checked ");
        out.print("name='" + purpose + "' ");
        out.print("value='" + letter + "'><tt>");
        out.print(formatNumber(estimate));
        if (range >= 0) {
            out.print(NBSP + PLUS_MINUS + NBSP);
            out.print(formatNumber(range));
        }
        out.print(NBSP);
        out.print(("size".equals(purpose)) ? "LOC" : "Hours");
        out.print(NBSP + NBSP + NBSP + "</tt>");
        printField(out, FLD_ESTIMATE, qual, estimate);
        printField(out, FLD_BETA0, qual, beta0);
        printField(out, FLD_BETA1, qual, beta1);
        printField(out, FLD_RANGE, qual, range);
        printField(out, FLD_PERCENT, qual, percent);
        printField(out, FLD_CORRELATION, qual, rSquared);
    }
    protected void printField(PrintWriter out, String name,
                              String qual, double value) {
        out.print("<input type='hidden' name='");
        out.print(qual);
        out.print(name);
        out.print("' value='");
        out.print(value);
        out.print("'>\n");
    }
    public static final String FLD_ESTIMATE = "Estimate";
    public static final String FLD_BETA0 = "Beta0";
    public static final String FLD_BETA1 = "Beta1";
    public static final String FLD_RANGE = "Range";
    public static final String FLD_PERCENT = "Percent";
    public static final String FLD_CORRELATION = "Corr";


    private static final String PLUS_MINUS = "&plusmn;";

    void printChart(PrintWriter out) {
        String url = histData.xyURL +
            "&qf=probe/"+getMethodPurpose()+getMethodLetter()+".rpt";
        out.print("<a href='"+url+"' target='popup' onClick='popup();'>");
        out.print("<img border=\"0\" src=\""+url+"&qf=probe/small.rpt\">");
        out.print("</a>\n");
    }

    void printTableRow(PrintWriter out, boolean isSelected) { }

    private void maybePrint(PrintWriter out, double num, String str) {
        if (Double.isNaN(num) || Double.isInfinite(num))
            out.print("&nbsp;");
        else
            out.print(str != null ? str : formatNumber(num));
    }

    void printTableRow(PrintWriter out, double estimate, boolean isSelected,
                       double beta0, double beta1, double range,
                       double rSquared, double stddev) {
        double rating = getRating();

        if (rating <= CANNOT_CALCULATE ||
            Double.isNaN(estimate) || Double.isInfinite(estimate)) return;
        String columnDivider = "</td><td ALIGN='CENTER'>";

        out.print("<tr><td ALIGN='RIGHT'>");
        if (isSelected)
            out.print("<img src='rarrow.gif' width=14 height=13>&nbsp;");
        out.print(getMethodLetter());
        out.print(columnDivider);
        maybePrint(out, estimate, null);
        out.print(columnDivider);
        maybePrint(out, rSquared, null);
        out.print(columnDivider);
        maybePrint(out, beta0, null);
        out.print(columnDivider);
        maybePrint(out, beta1, formatBeta1(beta1));
        out.print(columnDivider);
        maybePrint(out, range, null);
        out.print(columnDivider);
        double lpi = estimate - range; if (lpi < 0) lpi = 0;
        maybePrint(out, lpi, null);
        out.print(columnDivider);
        double upi = estimate + range; if (upi < 0) upi = 0;
        maybePrint(out, upi, null);
        out.print(columnDivider);
        double variance = stddev * stddev;
        maybePrint(out, variance, null);
        out.print(columnDivider);
        maybePrint(out, stddev, null);
        out.print(columnDivider);
        if (isSelected) out.print("<b>Selected.</b> ");
        if (rating <= SERIOUS_PROBLEM) out.print("<i>Do not use</i>");
        out.print("</td></tr>");
    }

    /** Examine beta values.
     */
    public void rateBetas(boolean checkBeta0, double beta0, double projection,
                          double beta1, double expectedBeta1,
                          String expectedBeta1Text) {

        double ratio = beta1 / expectedBeta1;
        if (ratio > 2 || ratio < 0.5) {
            rating = SERIOUS_PROBLEM;
            errorMessages.add
                ("The value of " + BETA1 + " is supposed to be close to " +
                 expectedBeta1Text + "; unfortunately, it is " +
                 "not. (" + BETA1 + " = " + formatBeta1(beta1) + ")");

        } else {
            rating += 1.0 - 2 * Math.abs(Math.log(ratio) / Math.log(2));
        }

        if (checkBeta0) {
            // Beta0 should be close to zero (substantially
            // smaller than the projected value).
            double percent = Math.abs(beta0 / projection);

            if (percent > 0.5) {
                rating = SERIOUS_PROBLEM;
                errorMessages.add
                    ("The value of " + BETA0 + " is supposed to be " +
                     "significantly smaller than the "+getMethodPurpose()+
                     " you are currently estimating; unfortunately, it is "+
                     "not. (" + BETA0 + " = " + formatNumber(beta0) + ")");
            } else {
                // We'll be ambivalent if Beta0 is one-quarter the size of
                // the projection. If it is any larger, we'll start taking
                // off rating points; if it is smaller, we'll add rating
                // points back in (up to a quarter of a point)
                rating += 0.25 - percent;
            }
        }
    }

    String formatBeta1(double beta1) {
        if ("time".equals(getMethodPurpose()))
            return formatNumber(1.0 / beta1) + " LOC/Hr";
        else
            return formatNumber(beta1);
    }
    double getExpectedBeta1() {
        if ("time".equals(getMethodPurpose()))
            return 1.0 / histData.getProductivity();
        else
            return  1;
    }
    String getExpectedBeta1Text() {
        if ("time".equals(getMethodPurpose()))
            return "your historical productivity (" +
                formatNumber(histData.getProductivity()) + " LOC/Hr)";
        else
            return "1";
    }

    String formatNumber(double num) { return DoubleData.formatNumber(num); }
    boolean badDouble(double d) {
        return Double.isNaN(d) || Double.isInfinite(d);
    }

    public static final String BETA0 =
        "<a href='params.htm' "+ProbeWizard.LINK_ATTRS+">beta0</a>";
    public static final String BETA1 =
        "<a href='params.htm' "+ProbeWizard.LINK_ATTRS+">beta1</a>";
    public static final String NBSP = "&nbsp;";
}
