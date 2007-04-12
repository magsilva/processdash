package teamdash.templates.setup;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.sourceforge.processdash.util.RobustFileWriter;
import net.sourceforge.processdash.util.XMLUtils;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


public class TeamSettingsFile {

    public static class RelatedProject {
        public String shortName;
        public String projectID;
        public String teamDirectory;
        public String teamDirectoryUNC;
    }


    private File projDataDir;

    private String projectName;

    private String projectID;

    private String processID;

    private String templatePath;

    private String scheduleName;

    private List masterProjects;

    private List subprojects;

    private boolean isReadOnly;


    public TeamSettingsFile(File projDataDir) {
        this.projDataDir = projDataDir;
        this.masterProjects = new LinkedList();
        this.subprojects = new LinkedList();
        this.isReadOnly = false;
    }

    public File getSettingsFile() {
        return new File(projDataDir, "settings.xml");
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getProjectID() {
        return projectID;
    }

    public void setProjectID(String projectID) {
        this.projectID = projectID;
    }

    public String getProcessID() {
        return processID;
    }

    public void setProcessID(String processID) {
        this.processID = processID;
    }

    public String getTemplatePath() {
        return templatePath;
    }

    public void setTemplatePath(String templatePath) {
        this.templatePath = templatePath;
    }

    public String getScheduleName() {
        return scheduleName;
    }

    public void setScheduleName(String scheduleName) {
        this.scheduleName = scheduleName;
    }

    public void setProjectHierarchyPath(String path) {
        if (path == null)
            projectName = null;
        else {
            int pos = path.lastIndexOf('/');
            if (pos != -1)
                path = path.substring(pos + 1);
            projectName = path;
        }
    }

    public List getMasterProjects() {
        return masterProjects;
    }

    public List getSubprojects() {
        return subprojects;
    }

    public void setReadOnly() {
        this.isReadOnly = true;
    }

    public void read() throws IOException {
        File settingsFile = getSettingsFile();
        try {
            Element e = XMLUtils.parse(
                    new FileInputStream(settingsFile)).getDocumentElement();

            this.projectName = getAttribute(e, PROJECT_NAME_ATTR);
            this.projectID = getAttribute(e, PROJECT_ID_ATTR);
            this.processID = getAttribute(e, PROCESS_ID_ATTR);
            this.templatePath = getAttribute(e, TEMPLATE_PATH_ATTR);
            this.scheduleName = getAttribute(e, SCHEDULE_NAME_ATTR);

            readRelatedProjects(e, MASTER_PROJECT_TAG, masterProjects);
            readRelatedProjects(e, SUBPROJECT_TAG, subprojects);

        } catch (SAXException e) {
            IOException ioe = new IOException("Could not read " + settingsFile);
            ioe.initCause(e);
            throw ioe;
        }
    }

    private void readRelatedProjects(Element e, String tagName, List projects) {
        projects.clear();
        NodeList nodes = e.getElementsByTagName(tagName);
        for(int i = 0;  i < nodes.getLength();  i++)
            projects.add(readRelatedProject((Element) nodes.item(i)));
    }

    private RelatedProject readRelatedProject(Element e) {
        RelatedProject result = new RelatedProject();
        result.shortName = getAttribute(e, SHORT_NAME_ATTR);
        result.projectID = getAttribute(e, PROJECT_ID_ATTR);
        result.teamDirectory = getAttribute(e, TEAM_DIR_ATTR);
        result.teamDirectoryUNC = getAttribute(e, TEAM_DIR_UNC_ATTR);
        return result;
    }

    private String getAttribute(Element e, String attrName) {
        String result = e.getAttribute(attrName);
        if (XMLUtils.hasValue(result))
            return result;
        else
            return null;
    }


    public void write() throws IOException {
        if (isReadOnly)
            throw new IOException("Cannot save read-only file");

        File settingsFile = getSettingsFile();
        Writer out = new RobustFileWriter(settingsFile, "UTF-8");

        // write XML header
        out.write("<?xml version='1.0' encoding='UTF-8'?>\n");
        // open XML tag
        out.write("<project-settings");

        // write the project attributes
        writeAttr(out, "\n    ", PROJECT_NAME_ATTR, projectName);
        writeAttr(out, "\n    ", PROJECT_ID_ATTR, projectID);
        writeAttr(out, "\n    ", PROCESS_ID_ATTR, processID);
        writeAttr(out, "\n    ", TEMPLATE_PATH_ATTR, templatePath);
        writeAttr(out, "\n    ", SCHEDULE_NAME_ATTR, scheduleName);
        out.write(">\n");

        writeRelatedProjects(out, MASTER_PROJECT_TAG, masterProjects);
        writeRelatedProjects(out, SUBPROJECT_TAG, subprojects);

        out.write("</project-settings>\n");

        out.close();
    }

    private void writeRelatedProjects(Writer out, String tagName, List projects) throws IOException {
        for (Iterator i = projects.iterator(); i.hasNext();) {
            RelatedProject proj = (RelatedProject) i.next();
            out.write("    <");
            out.write(tagName);
            writeAttr(out, "\n        ", SHORT_NAME_ATTR, proj.shortName);
            writeAttr(out, "\n        ", PROJECT_ID_ATTR, proj.projectID);
            writeAttr(out, "\n        ", TEAM_DIR_ATTR, proj.teamDirectory);
            writeAttr(out, "\n        ", TEAM_DIR_UNC_ATTR, proj.teamDirectoryUNC);
            out.write("/>\n");
        }
    }

    private void writeAttr(Writer out, String indent, String name, String value)
            throws IOException {
        if (value != null) {
            out.write(indent);
            out.write(name);
            out.write("='");
            out.write(XMLUtils.escapeAttribute(value));
            out.write("'");
        }
    }


    private static final String PROJECT_NAME_ATTR = "projectName";

    private static final String PROJECT_ID_ATTR = "projectID";

    private static final String PROCESS_ID_ATTR = "processID";

    private static final String TEMPLATE_PATH_ATTR = "templatePath";

    private static final String SCHEDULE_NAME_ATTR = "scheduleName";

    private static final String SHORT_NAME_ATTR = "shortName";

    private static final String TEAM_DIR_ATTR = "teamDirectory";

    private static final String TEAM_DIR_UNC_ATTR = "teamDirectoryUNC";

    private static final String SUBPROJECT_TAG = "subproject";

    private static final String MASTER_PROJECT_TAG = "masterProject";
}