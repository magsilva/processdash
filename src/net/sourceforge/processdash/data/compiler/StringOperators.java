// Copyright (C) 2001-2003 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
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

package net.sourceforge.processdash.data.compiler;

import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.data.repository.DataRepository;

class StringOperators {

    private StringOperators() {}

    public static final Instruction CONCAT = new BinaryOperator("&") {
            protected SimpleData operate(SimpleData left, SimpleData right) {
                if (left == null) return right;
                if (right == null) return left;
                if (left instanceof ListData || right instanceof ListData) {
                    ListData result = new ListData();
                    result.addAll(left);
                    result.addAll(right);
                    return result;

                } else
                    return StringData.create(left.format() + right.format());
            } };

    public static final Instruction PATHCONCAT = new BinaryOperator("&/") {
            protected SimpleData operate(SimpleData left, SimpleData right) {
                if (left == null) return right;
                if (right == null) return left;
                return StringData.create(DataRepository.createDataName
                                         (left.format(), right.format()));
            } };
}
