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

package pspdash;

import java.util.Date;
import java.text.MessageFormat;


public class EVMetricsRollup extends EVMetrics {

    /** The date the project is forecast to complete */
    Date independentForecastDate = null;

    public void reset(Date effectiveDate) {
        totalPlanTime = earnedValueTime = actualTime = planTime = 0.0;
        currentDate = effectiveDate;
        startDate = independentForecastDate = null;
    }

    public void addMetrics(EVMetrics that) {
        this.totalPlanTime   += that.totalPlanTime;
        this.earnedValueTime += that.earnedValueTime;
        this.actualTime      += that.actualTime;
        this.planTime        += that.planTime;
        this.startDate =
            EVScheduleRollup.minDate(this.startDate, that.startDate);
        this.independentForecastDate =
            EVScheduleRollup.maxDate(this.independentForecastDate,
                                     that.independentForecastDate());
    }


    public double independentForecastDuration() {
        Date s, e;
        if ((s = startDate()) == null) return Double.NaN;
        if ((e = independentForecastDate()) == null) return Double.NaN;
        return (e.getTime() - s.getTime()) / (double) MINUTE_MILLIS;
    }
    public Date independentForecastDate() {
        return independentForecastDate;
    }

    public double optimizedForecastDuration() {
        return super.independentForecastDuration();
    }

    public Date optimizedForecastDate() {
        // use the extrapolation algorithm that EVMetrics uses to
        // calculate the independentForecastDate.
        Date s;
        if ((s = startDate()) == null) return null;
        double duration = optimizedForecastDuration();
        if (badDouble(duration)) return null;
        return new Date(s.getTime() + (long) (duration * MINUTE_MILLIS));
    }




    public String optimizedForecastDuration(int style) {
        double d= optimizedForecastDuration();
        if (badDouble(d)) return null;
        return MessageFormat.format(OFD_FORMATS[style], durationArgs(d));
    }
    static final String[] OFD_FORMATS = {
        "Optimized Forecast Duration",
        "{0}",
        "{1}",
        "If you rebalanced subschedules optimally (so they all complete at the same time), the total work schedule is forecast to be {1} (based upon your current rate of task completion to date)." };


    public String optimizedForecastDate(int style) {
        Date d =  optimizedForecastDate();
        if (d == null) return null;
        return MessageFormat.format(OFDT_FORMATS[style], args(d));
    }
    static final String[] OFDT_FORMATS = {
        "Optimized Forecast Completion Date",
        "{0,date}",
        "{0,date}",
        "If you rebalanced subschedules optimally (so they all complete at the same time), the total work is forecast to be completed {0,date} (based upon your current rate of task completion to date)." };


    public String optimizedPlanDate(int style) {
        return OPD_FORMATS[style];
    }
    static final String[] OPD_FORMATS = {
        "Optimized Plan Date",
        "NYI", //"{0,date}",
        "NYI", //"{0,date}",
        "Not yet implemented." }; // "If you rebalanced subschedules optimally, the overall plan would complete {0,date}." };


    public String independentForecastDuration(int style) {
        double d= independentForecastDuration();
        if (badDouble(d)) return null;
        return MessageFormat.format(IFD_ROLL_FORMATS[style], durationArgs(d));
    }
    static final String[] IFD_ROLL_FORMATS = {
        "Forecast Duration",
        "{0}",
        "{1}",
        "If subschedules are not rebalanced, and each continues at its current rate of task completion to date, the total work schedule is forecast to be {1}." };


    public String independentForecastDate(int style) {
        Date d =  independentForecastDate();
        if (d == null) return null;
        return MessageFormat.format(IFDT_ROLL_FORMATS[style], args(d));
    }
    static final String[] IFDT_ROLL_FORMATS = {
        "Forecast Completion Date",
        "{0,date}",
        "{0,date}",
        "If subschedules are not rebalanced, and each continues at its current rate of task completion to date, the total work is forecast to be completed {0,date}." };



    /*
     *TableModel Interface
     */

    public Object getValueAt(int row, int col) {
        switch (row) {
        case 14: return optimizedForecastDuration(col);
        case 15: return optimizedForecastDate(col);
        case 16: return optimizedPlanDate(col);

        default: return super.getValueAt(row, col);
        }
    }
    public int getRowCount() { return 17; }

}
