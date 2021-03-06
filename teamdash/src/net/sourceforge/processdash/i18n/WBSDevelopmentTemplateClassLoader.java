// Copyright (C) 2012-2017 Tuma Solutions, LLC
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

package net.sourceforge.processdash.i18n;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import net.sourceforge.processdash.util.RuntimeUtils;


/**
 * Resource bundle classloader customized for use during the development of the
 * teamdash project; finds localization resources in plain directories based on
 * the expected directory structure of the development environment.
 */
public class WBSDevelopmentTemplateClassLoader extends SafeTemplateClassLoader {

    public WBSDevelopmentTemplateClassLoader() {
        getBaseDir();
    }

    @Override
    protected URL findResourceImpl(String resourceName) {
        try {
            File baseDir = getBaseDir();
            if (resourceName.startsWith("resources/WBSEditor"))
                baseDir = new File(baseDir, "teamdash");
            File resource = new File(baseDir, mapToTemplates(resourceName));
            if (resource.isFile())
                return resource.toURI().toURL();
        } catch (MalformedURLException e) {
        }

        return null;
    }

    File baseDir = null;

    private File getBaseDir() {
        if (baseDir == null) {
            if (!Boolean.getBoolean("WBSDevelopmentTemplateClassLoader.enabled"))
                throw new UnsupportedOperationException("Not enabled");
            File cp = RuntimeUtils.getClasspathFile(getClass());
            if (cp == null || !cp.isDirectory())
                throw new UnsupportedOperationException();
            baseDir = cp.getParentFile();
        }

        return baseDir;
    }

}
