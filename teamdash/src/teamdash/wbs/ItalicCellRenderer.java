// Copyright (C) 2002-2018 Tuma Solutions, LLC
// Team Functionality Add-ons for the Process Dashboard
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

package teamdash.wbs;

import java.awt.Color;
import java.awt.Component;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.border.Border;

/** Special cell renderer for strings that can italicize displayed values.
 * 
 * The presence of a certain error message is interpreted as a flag that the
 * value should be displayed in italics (rather than in a bold colored font,
 * like the regular {@link DataTableCellRenderer} would do).
 */
public class ItalicCellRenderer extends DataTableCellRenderer {

    private String messageToItalicize;

    public ItalicCellRenderer(String messageToItalicize) {
        this.messageToItalicize = messageToItalicize;
    }


    public Component getTableCellRendererComponent(JTable table,
                                                   Object value,
                                                   boolean isSelected,
                                                   boolean hasFocus,
                                                   int row,
                                                   int column) {

        Component result = super.getTableCellRendererComponent
            (table, value, isSelected, hasFocus, row, column);

        if (value instanceof ErrorValue) {
            ErrorValue err = (ErrorValue) value;
            if (err.error != null &&
                err.error.equals(messageToItalicize)) {
                result.setForeground(Color.black);
                result.setFont(TableFontHandler.getItalic(table));
                setInheritedBorder((JComponent) result);
            }
        }

        return result;
    }

    protected void setInheritedBorder(JComponent jc) {
        Border b = jc.getBorder();
        b = getInheritedBorder(b);
        jc.setBorder(b);
    }

    private static Border getInheritedBorder(Border b) {
        if (b == null)
            return INHERITED_BORDER;
        Border result = INHERITED_BORDERS.get(b);
        if (result == null) {
            result = BorderFactory.createCompoundBorder(b, INHERITED_BORDER);
            INHERITED_BORDERS.put(b, result);
        }
        return result;
    }

    private static final Border INHERITED_BORDER = BorderFactory
            .createEmptyBorder(0, 10, 0, 0);

    private static final Map<Border, Border> INHERITED_BORDERS = Collections
            .synchronizedMap(new HashMap());

}
