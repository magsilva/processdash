// Copyright (C) 2006 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
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

package net.sourceforge.processdash.net.cms;

import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sourceforge.processdash.ui.web.reports.analysis.AnalysisPage;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;

public class FramesetPageAssemblers {

    private static final String FRAME_PARAM = "frame";
    private static final String FRAME_TOC = "toc";
    private static final String FRAME_CONTENT = "content";
    private static final String FRAME_NONE = "none";

    private static final String SECTION_ID_PARAM = "section";


    /** Determine whether it would be appropriate to view this page using
     * frames.
     * 
     * @param page the page to display
     * @param parameters the parameters passed to the CmsContentDispatcher
     * @return a {@link PageAssembler} to use.  If frames are not appropriate,
     *     returns null.
     */
    public static PageAssembler getViewAssembler(PageContentTO page,
            Map parameters) {
        if (hasSectionHeading(page) == false)
            return null;

        String frameParam = (String) parameters.get(FRAME_PARAM);
        if (FRAME_NONE.equals(frameParam))
            return null;
        else if (FRAME_TOC.equals(frameParam))
            return new TocPageAssembler();
        else if (FRAME_CONTENT.equals(frameParam))
            return new SectionContentAssembler();
        else
            return new FramesetPageAssembler();
    }


    /** Determine whether it would be appropriate to edit a portion of this
     * page, as a result of a request to edit from frames mode.
     * 
     * @param page the page to edit
     * @param parameters the parameters passed to the CmsContentDispatcher
     * @return a {@link PageAssembler} to use.  If a frameset-based editor is
     *     not appropriate, returns null.
     */
    public static PageAssembler getEditAssembler(PageContentTO page,
            Map parameters) {

        String frameParam = (String) parameters.get(FRAME_PARAM);
        if (FRAME_TOC.equals(frameParam))
            return new EditTocPageAssembler();
        else if (FRAME_CONTENT.equals(frameParam))
            return new EditSectionPageAssembler();
        else
            return null;
    }


    /** When a page is saved and we are redirecting to the page which will
     * render the saved results, the content dispatcher will call this method
     * to determine whether we need to add any parameters to the resulting URL.
     * 
     *  When the user edits a single section of a page, and saves those changes,
     *  they presumably want to that section of the page to be displayed after
     *  the save is complete.  In that scenario, this method will return a
     *  query parameter that will trigger the display of that section of the
     *  page.  In all other scenarios, this method will return null.
     * 
     * @param parameters the parameters that were posted to the save operation
     */
    public static String getExtraSaveParams(Map parameters) {
        String sectionID = (String) parameters.get(SECTION_ID_PARAM);
        if (sectionID != null)
            return SECTION_ID_PARAM + "=" + HTMLUtils.urlEncode(sectionID);
        else
            return null;
    }



    /** A PageAssembler that can display the top-level frameset */
    private static class FramesetPageAssembler extends
            AbstractViewPageAssembler {

        protected boolean shouldInvokeSnippet(PageContentTO page,
                SnippetInstanceTO snip) {
            return false;
        }

        protected void writePage(Writer out, Set headerItems, PageContentTO page)
                throws IOException {
            out.write("<html>\n");
            writeHead(out, Collections.EMPTY_SET, page);

            out.write("<frameset cols=\"153,*\">\n");
            String selfUrl = CmsContentDispatcher.getSimpleSelfUri(environment,
                    false);

            out.write("<frame name=\"toc\" src=\"");
            out.write(HTMLUtils.appendQuery(selfUrl, FRAME_PARAM, FRAME_TOC));
            out.write("\">\n");

            out.write("<frame name=\"contents\" src=\"");
            out.write(HTMLUtils.appendQuery(selfUrl, FRAME_PARAM, FRAME_CONTENT));
            out.write("\" scrolling=\"yes\">\n");

            out.write("</frameset>\n");
            out.write("</html>\n");
        }

    }


    /** A PageAssembler that can display the table of contents, containing
     * a list of the sections in a page */
    private static class TocPageAssembler extends AbstractViewPageAssembler {

        public void setParameters(Map parameters) {
            parameters.put("mode", "toc");
            super.setParameters(parameters);
        }

        protected boolean shouldInvokeSnippet(PageContentTO page,
                SnippetInstanceTO snip) {
            return PageSectionHelper.isSectionHeading(snip);
        }

        protected String getEditURI(Map env) {
            return HTMLUtils.appendQuery(CmsContentDispatcher.getSimpleSelfUri(
                    env, false), "mode", "edit");
        }

        protected void writePage(Writer out, Set headerItems, PageContentTO page)
                throws IOException {

            out.write("<html>\n");
            writeHead(out, headerItems, page);
            out.write("<body>\n");

            out.write("<h2>");
            writeEditLink(out, "22", "_top");
            out.write(getPageTitle(page));
            out.write("</h2>\n\n");

            String selfUrl = CmsContentDispatcher.getSimpleSelfUri(environment,
                    true);
            String singlePageUrl = HTMLUtils.appendQuery(selfUrl, FRAME_PARAM,
                    FRAME_NONE);
            selfUrl = HTMLUtils.appendQuery(selfUrl, FRAME_PARAM, FRAME_CONTENT);
            for (Iterator i = page.getContentSnippets().iterator(); i.hasNext();) {
                SnippetInstanceTO snip = (SnippetInstanceTO) i.next();
                if (PageSectionHelper.isSectionHeading(snip)) {
                    out.write("<p><a target=\"contents\" href=\"");
                    String sectionUrl = HTMLUtils.appendQuery(selfUrl,
                            SECTION_ID_PARAM, snip.getInstanceID());
                    out.write(HTMLUtils.escapeEntities(sectionUrl));
                    out.write("\">");
                    out.write(snip.getGeneratedContent());
                    out.write("</a></p>\n\n");
                }
            }

            out.write("<hr>\n");

            String anchorHtml = "<a target=\"_top\" href=\""
                    + HTMLUtils.escapeEntities(singlePageUrl) + "\">";
            String html = resources.getString("No_Frames_HTML");
            html = StringUtils.findAndReplace(html, "<A>", anchorHtml);
            html = StringUtils.findAndReplace(html, "<a>", anchorHtml);

            out.write("<p>");
            out.write(html);
            out.write("</p>\n\n");

            out.write("</body>\n</html>\n");
        }

    }


    /** A PageAssembler that can display the content for a single section
     * of a page. */
    private static class SectionContentAssembler extends
            AbstractViewPageAssembler {

        private PageSectionHelper sectionHelper;

        public void setParameters(Map parameters) {
            super.setParameters(parameters);
            String sectionID = (String) parameters.get(SECTION_ID_PARAM);
            sectionHelper = new PageSectionHelper(sectionID);
        }

        protected boolean shouldInvokeSnippet(PageContentTO page,
                SnippetInstanceTO snip) {
            return sectionHelper.test(snip);
        }

        protected String getEditURI(Map env) {
            return HTMLUtils.appendQuery(CmsContentDispatcher.getSimpleSelfUri(
                    env, false), "mode", "edit");
        }

        protected void writePage(Writer out, Set headerItems, PageContentTO page)
                throws IOException {

            out.write("<html>\n");
            writeHead(out, headerItems, page);
            out.write("<body>\n");

            out.write("<h1>");
            writeEditLink(out, "32", null);
            out.write(esc(AnalysisPage.localizePrefix(prefix)));
            out.write("</h1>\n");

            out.write("<h2>");
            out.write(getPageTitle(page));
            out.write("</h2>\n\n");
            out.write("<form>\n\n");

            for (Iterator i = page.getContentSnippets().iterator(); i.hasNext();) {
                SnippetInstanceTO snip = (SnippetInstanceTO) i.next();
                writeSnippet(out, snip);
            }

            out.write("</form>\n\n");
            out.write("<script src='/data.js' type='text/javascript'> </script>\n");
            out.write("</body>\n");
            out.write("</html>\n");
        }
    }


    /** A PageAssembler that can display the sections of a page for editing
     * and rearrangement.
     */
    private static class EditTocPageAssembler extends
            EditSinglePageAssembler {

        protected boolean shouldInvokeSnippet(PageContentTO page,
                SnippetInstanceTO snip) {
            boolean result = PageSectionHelper.isSectionHeading(snip);
            if (result) {
                addFlagParam(snip, "Edit_TOC_Mode", parameters);
                snip.setAlternateName(resources
                        .getString("SectionHeading.TOC_Label"));
            }
            return result;
        }

        protected void writePageSnippetEditors(Writer out, PageContentTO page)
                throws IOException {

            boolean sectionsHaveStarted = false;

            List contentSnippets = page.getContentSnippets();
            for (Iterator i = contentSnippets.iterator(); i.hasNext();) {
                SnippetInstanceTO snip = (SnippetInstanceTO) i.next();
                boolean isSectionHeading = PageSectionHelper
                        .isSectionHeading(snip);

                if (isSectionHeading) {
                    if (sectionsHaveStarted) {
                        // close the div for the previous section.
                        out.write("</div>\n\n");
                    } else {
                        out.write("<div id='snippetContainer'>\n\n");
                        sectionsHaveStarted = true;
                    }
                    writeDivWrapperStart(out, snip.getNamespace());
                }

                int style = (isSectionHeading ? EDIT_STYLE_UNWRAPPED
                        : EDIT_STYLE_INVISIBLE);
                writeSnippet(out, snip, style);
            }


            if (sectionsHaveStarted) {
                // close the div for the previous section.
                out.write("</div>\n\n");
                // close the snippet container div
                out.write("</div>\n\n");
            } else {
                // write an empty snippet container div.
                out.write("<div id='snippetContainer'>\n\n");
                out.write("</div>\n\n");
            }

            setAddNewExtraArgs(out, "&NS_Edit_TOC_Mode=t"
                    + "&altNameKey=TOC_Label&snippetID="
                    + PageSectionHelper.SECTION_HEADING_SNIP_ID);
        }

    }


    private static class EditSectionPageAssembler extends
            EditSinglePageAssembler {

        private PageSectionHelper sectionHelper;

        public void setParameters(Map parameters) {
            super.setParameters(parameters);
            String sectionID = (String) parameters.get(SECTION_ID_PARAM);
            sectionHelper = new PageSectionHelper(sectionID);
        }

        protected boolean shouldInvokeSnippet(PageContentTO page,
                SnippetInstanceTO snip) {
            int status = sectionHelper.getStatus(snip);
            if (status == PageSectionHelper.STATUS_START)
                addFlagParam(snip, "Edit_Single_Section_Mode", parameters);

            return (status == PageSectionHelper.STATUS_START
                    || status == PageSectionHelper.STATUS_DURING);
        }

        protected void writePageMetadataEditors(Writer out, PageContentTO page)
                throws IOException {
            writeHidden(out, PAGE_TITLE, page.getPageTitle());
        }

        protected void writePageSnippetEditors(Writer out, PageContentTO page)
                throws IOException {

            sectionHelper.reset();

            out.write("<div class='cmsMetadataSection'>\n\n");

            List contentSnippets = page.getContentSnippets();
            for (Iterator i = contentSnippets.iterator(); i.hasNext();) {
                SnippetInstanceTO snip = (SnippetInstanceTO) i.next();
                int snipStatus = sectionHelper.getStatus(snip);

                if (snipStatus == PageSectionHelper.STATUS_END)
                    // close the snippetContainer div
                    out.write("</div>\n\n");

                if (snipStatus == PageSectionHelper.STATUS_START) {
                    // write out a snippet editor for the section heading
                    writeSnippet(out, snip, EDIT_STYLE_CHROMELESS);
                    // close the cmsMetadataSection div
                    out.write("</div>\n\n");
                    // start the snippet container div
                    out.write("<div id='snippetContainer'>\n\n");

                } else {
                    boolean isActiveSnippet =
                        (snipStatus == PageSectionHelper.STATUS_DURING);
                    int style = (isActiveSnippet ? EDIT_STYLE_WRAPPED
                            : EDIT_STYLE_INVISIBLE);
                    writeSnippet(out, snip, style);
                }
            }

            int finalStatus = sectionHelper.getStatus(null);
            switch (finalStatus) {
            case PageSectionHelper.STATUS_BEFORE:
                // we never saw the section in question.  This is most
                // certainly an error;  how to handle it?
                break;

            case PageSectionHelper.STATUS_END:
                // close the snippet container div
                out.write("</div>\n\n");
                break;

            case PageSectionHelper.STATUS_START:   // can't happen
            case PageSectionHelper.STATUS_DURING:  // can't happen
            case PageSectionHelper.STATUS_AFTER:   // no action needed.
            }

            setAddNewExtraArgs(out, "&NS_deny="
                    + PageSectionHelper.SECTION_HEADING_SNIP_ID);
            writeHidden(out, SECTION_ID_PARAM, sectionHelper
                    .getSectionInstanceID());
        }

    }


    /** Write a script to the page that will register extra args to be used
     * during the "add snippet" operation. */
    private static void setAddNewExtraArgs(Writer out, String args)
            throws IOException {
        out.write("<script type=\"text/javascript\">");
        out.write("DashCMS.setAddNewExtraArgs('");
        out.write(args);
        out.write("');</script>\n");
    }


    /** In preparation for invoking a snippet, set a new parameter value
     * 
     * @param snip the snippet we're about to invoke
     * @param paramName the name of a parameter to set
     * @param parameters the parameters map
     */
    private static void addFlagParam(SnippetInstanceTO snip, String paramName,
            Map parameters) {
        String ns = snip.getNamespace();
        parameters.put(ns + paramName, "t");
        parameters.put(ns + paramName + "_ALL", new String[] { "t" });
    }


    /** Return true if the given page contains at least one section heading. */
    private static boolean hasSectionHeading(PageContentTO page) {
        for (Iterator i = page.getContentSnippets().iterator(); i.hasNext();) {
            SnippetInstanceTO snip = (SnippetInstanceTO) i.next();
            if (PageSectionHelper.isSectionHeading(snip))
                return true;
        }
        return false;
    }

}
