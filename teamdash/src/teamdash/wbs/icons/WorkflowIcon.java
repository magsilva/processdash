// Copyright (C) 2017 Tuma Solutions, LLC
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

package teamdash.wbs.icons;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;

import net.sourceforge.processdash.hier.ui.icons.Rectangle3DIcon;

public class WorkflowIcon extends Rectangle3DIcon {

    public WorkflowIcon(Color fill) {
        super(16, 16, fill, 3, 0, 3, 0);
    }

    @Override
    protected void paintIcon(Graphics2D g2, Shape clip, float scale) {
        super.paintIcon(g2, clip, scale);

        int t = 4, b = 11, l = 3, r = 11;

        g2.setStroke(new BasicStroke(1));
        g2.setColor(shadow);
        g2.drawLine(l, t, l, b);
        g2.drawLine(r, t, r, b);
        g2.setColor(highlight);
        l++; r++; b--;
        g2.drawLine(l, t, l, b);
        g2.drawLine(r, t, r, b);
    }

}
