// Copyright (C) 2007 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC: processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.process.ui;

import java.awt.Toolkit;
import java.net.URL;
import java.net.URLConnection;

import net.sourceforge.processdash.net.http.WebServer;
import net.sourceforge.processdash.ui.Browser;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.HTTPUtils;

public class TriggerURI {

    /**
     * The name of a parameter that will be present on the query string of a
     * triggering request.
     */
    public static final String IS_TRIGGERING = "isTriggering";

    /**
     * Text that an HTML response document can include to indicate that a
     * trigger request performed all necessary work, and does not need to be
     * displayed to the user.
     */
    public static final String NULL_DOCUMENT_MARKER = "<!-- null document -->";

    /**
     * Header that an HTTP response can include to indicate that a trigger
     * request performed all necessary work, and does not need to be displayed
     * to the user.
     */
    public static final String NULL_DOCUMENT_HEADER = "X-Trigger-Null-Document";


    public static boolean isTrigger(String uri) {
        return uri != null && uri.indexOf("trigger") != -1;
    }

    public static void handle(String uri) {
        if (isTrigger(uri))
            new Thread(new TriggerLauncher(uri)).start();
        else
            Browser.launch(uri);
    }

    private static class TriggerLauncher implements Runnable {

        private String href;

        public TriggerLauncher(String href) {
            this.href = href;
        }

        public void run() {
            try {
                String uri = href;
                // prepend a slash if needed
                if (!uri.startsWith("/"))
                    uri = "/" + uri;
                // remove the anchor if it is present
                int pos = uri.indexOf('#');
                if (pos != -1)
                    uri = uri.substring(0, pos);
                // append an "isTriggering" query string
                uri = HTMLUtils.appendQuery(uri, IS_TRIGGERING);
                // retrieve the request
                URL u = new URL(WebServer.DASHBOARD_PROTOCOL + ":" + uri);
                URLConnection conn = u.openConnection();
                byte[] rawData = FileUtils.slurpContents(conn.getInputStream(),
                        true);
                String charset = HTTPUtils.getCharset(conn.getContentType());
                String result = new String(rawData, charset);
                // if anything other than a null document was returned, the
                // user needs to see it.
                if (result.indexOf(TriggerURI.NULL_DOCUMENT_MARKER) == -1
                        && conn.getHeaderField(NULL_DOCUMENT_HEADER) == null)
                    Browser.launch(href);
            } catch (Exception e) {
                e.printStackTrace();
                Toolkit.getDefaultToolkit().beep();
            }
        }
    }

}