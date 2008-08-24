package teamdash.templates.tools;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.sourceforge.processdash.DashController;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.net.http.WebServer;
import net.sourceforge.processdash.tool.export.mgr.ExternalResourceManager;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.RuntimeUtils;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.XMLUtils;
import teamdash.FilenameMapper;
import teamdash.wbs.WBSEditor;

public class OpenWBSEditor extends TinyCGIBase {

    private static final String JAR_PARAM = "jar";

    public OpenWBSEditor() {
        this.charset = "UTF-8";
    }

    protected void writeHeader() {
    }
    private void writeHtmlHeader() {
        super.writeHeader();
    }

    /** Generate CGI script output. */
    protected void writeContents() throws IOException {
        if (parameters.containsKey(JAR_PARAM)) {
            serveJar();
            return;
        }

        boolean useJNLP = Settings.getBool("wbsEditor.useJNLP", false);
        if (parameters.containsKey("useJNLP"))
            useJNLP = true;
        else if (parameters.containsKey("isTriggering")
                || parameters.containsKey("trigger"))
            useJNLP = false;
        try {
            DashController.checkIP(env.get("REMOTE_ADDR"));
        } catch (IOException ioe) {
            useJNLP = true;
        }

        parseFormData();
        String directory = getParameter("directory");
        directory = FilenameMapper.remap(directory);

        if (useJNLP)
            writeJnlpFile(directory);
        else
            openInProcess(directory);
    }

    private String getSyncURL() {
        return makeAbsoluteURL(getParameter("syncURL"));
    }

    private String getReverseSyncURL() {
        return makeAbsoluteURL(getParameter("reverseSyncURL"));
    }

    private String makeAbsoluteURL(String uri) {
        if (uri == null || uri.trim().length() == 0)
            return null;
        if (uri.startsWith("http"))
            return uri;
        WebServer ws = getTinyWebServer();
        return "http://" + ws.getHostName(true) + ":" + ws.getPort() + uri;
    }

    private void openInProcess(String directory) {
        if (!checkEntryCriteria(directory))
            return;

        writeHtmlHeader();
        if (launchEditorProcess(directory)) {
            // if we successfully opened the WBS, write the null document.
            DashController.printNullDocument(out);
        } else {
            // if, for some reason, we weren't able to launch the WBS in a new
            // process, our best bet is to try JNLP instead.  Use an HTML
            // redirect to point the user to a forced JNLP page.
            out.print("<html><head>");
            out.print("<meta http-equiv='Refresh' CONTENT='0;URL=" +
                        "/team/tools/OpenWBSEditor.class?useJNLP&");
            out.print(env.get("QUERY_STRING"));
            out.print("'></head></html>");
        }
    }

    private boolean checkEntryCriteria(String directory) {
        String javaVersion = System.getProperty("java.version");
        if (javaVersion.startsWith("1.3")) {
            writeHtmlHeader();
            out.print(JAVA_VERSION_MSG1 + javaVersion + JAVA_VERSION_MSG2);
            return false;
        }

        File dir = new File(directory);
        if (!dir.isDirectory()) {
            writeHtmlHeader();
            out.print(DIR_MISSING_MSG1 + HTMLUtils.escapeEntities(directory) +
                      DIR_MISSING_MSG2);
            return false;
        }

        return true;
    }

    public Map<String, String> getLaunchProperties() {
        Map<String,String> result = new HashMap<String,String>();

        if (parameters.containsKey("bottomUp"))
            result.put("teamdash.wbs.bottomUp", "true");

        if (parameters.containsKey("team"))
            result.put("teamdash.wbs.showTeamMemberList", "true");

        if (Settings.getBool("READ_ONLY", false)
                || "true".equalsIgnoreCase(getParameter("forceReadOnly")))
            result.put("teamdash.wbs.readOnly", "true");

        result.put("teamdash.wbs.syncURL", getSyncURL());
        result.put("teamdash.wbs.reverseSyncURL", getReverseSyncURL());
        result.put("teamdash.wbs.owner", getOwner());
        result.put("teamdash.wbs.processSpecURL", getProcessURL());

        return result;
    }

    private String getProcessURL() {
        Object processURL = parameters.get("processURL");
        if (processURL instanceof String)
            return (String) processURL;
        else
            return null;
    }

    private static Hashtable editors = new Hashtable();

    private boolean launchEditorProcess(String directory) {
        String[] cmdLine = getProcessCmdLine(directory);
        if (cmdLine == null)
            return false;

        try {
            Process p = Runtime.getRuntime().exec(cmdLine);
            new OutputConsumer(p).start();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private String[] getProcessCmdLine(String directory) {
        String jreExecutable = RuntimeUtils.getJreExecutable();
        File classpath = RuntimeUtils.getClasspathFile(getClass());
        if (jreExecutable == null || classpath == null)
            return null;

        List cmd = new ArrayList();
        cmd.add(jreExecutable);

        String extraArgs = Settings.getVal("wbs.jvmArgs", "-Xmx200m");
        if (StringUtils.hasValue(extraArgs))
            cmd.addAll(Arrays.asList(extraArgs.split("\\s+")));

        // propagate security-related system properties
        cmd.addAll(Arrays.asList(RuntimeUtils.getPropagatedJvmArgs()));

        // set a reasonable application menu name on Mac OS X
        if ("Mac OS X".equalsIgnoreCase(System.getProperty("os.name")))
            cmd.add("-Xdock:name=WBS Editor");

        Map<String, String> props = getLaunchProperties();
        props.putAll(ExternalResourceManager.getInstance()
                .getJvmArgsForMapping());

        for (Map.Entry<String, String> e : props.entrySet()) {
            if (e.getValue() != null)
                cmd.add("-D" + e.getKey() + "=" + e.getValue());
        }

        cmd.add("-jar");
        cmd.add(classpath.getAbsolutePath());

        cmd.add(directory);

        return (String[]) cmd.toArray(new String[cmd.size()]);
    }

    private static class OutputConsumer extends Thread {
        Process p;
        public OutputConsumer(Process p) {
            this.p = p;
            setDaemon(true);
        }
        public void run() {
            RuntimeUtils.consumeOutput(p, System.out, System.err);
        }
    }


    protected void showEditorInternally(String directory, boolean bottomUp,
                boolean showTeam, boolean readOnly, String syncURL) {
        String key = directory;
        if (bottomUp)
            key = "bottomUp:" + key;

        WBSEditor editor = (WBSEditor) editors.get(key);
        if (editor != null && !editor.isDisposed()) {
            if (showTeam)
                editor.showTeamListEditor();
            else
                editor.raiseWindow();

        } else {
            editor = WBSEditor.createAndShowEditor(directory, bottomUp,
                    showTeam, syncURL, false, readOnly, getOwner());
            if (editor != null)
                editors.put(key, editor);
            else
                editors.remove(key);
        }
    }

    private void writeJnlpFile(String directory) {
        out.print("Content-type: application/x-java-jnlp-file\r\n\r\n");

        WebServer ws = getTinyWebServer();
        out.print("<?xml version='1.0' encoding='utf-8'?>\n");
        out.print("<jnlp spec='1.0+' codebase='http://");
        out.print(ws.getHostName(true));
        out.print(":");
        out.print(ws.getPort());
        out.print("/'>\n");

        out.print("<information>\n");
        out.print("<title>WBS Editor</title>\n");
        out.print("<vendor>Tuma Solutions, LLC</vendor>\n");
        out.print("<description>Work Breakdown Structure Editor</description>\n");
        out.print("</information>\n");

        out.print("<security><all-permissions/></security>\n");

        String path = (String) env.get("SCRIPT_NAME");
        int pos = path.lastIndexOf('/');
        String jarPath = path.substring(1, pos+1) + "TeamTools.jar";
        out.print("<resources>\n");
        out.print("<j2se version='1.5+' initial-heap-size='2M' max-heap-size='200M'/>\n");
        out.print("<jar href='");
        out.print(jarPath);
        out.print("'/>\n");

        Map<String, String> props = getLaunchProperties();
        for (Map.Entry<String, String> e : props.entrySet()) {
            if (e.getValue() != null) {
                out.print("<property name='");
                out.print(e.getKey());
                out.print("' value='");
                out.print(XMLUtils.escapeAttribute(e.getValue()));
                out.print("'/>\n");
            }
        }

        out.print("</resources>\n");

        out.print("<application-desc>\n");
        out.print("<argument>");
        out.print(XMLUtils.escapeAttribute(directory));
        out.print("</argument>\n");
        out.print("</application-desc>\n");

        out.print("</jnlp>\n");
        out.flush();
    }

    private void serveJar() throws IOException {
        URL myURL = getClass().getResource("OpenWBSEditor.class");
        String u = myURL.toString();
        int pos = u.indexOf("!/");
        if (!u.startsWith("jar:") || pos == -1)
            throw new IOException();

        URL jarURL = new URL(u.substring(4, pos));
        URLConnection conn = jarURL.openConnection();
        long modTime = conn.getLastModified();
        InputStream in = conn.getInputStream();

        out.print("Content-type: application/octet-stream\r\n");
        if (modTime > 0) {
            out.print("Last-Modified: ");
            out.print(dateFormat.format(new Date(modTime)));
            out.print("\r\n");
        }
        out.print("\r\n");
        out.flush();

        byte[] buf = new byte[1024];
        int bytesRead;
        while ((bytesRead = in.read(buf)) != -1)
            outStream.write(buf, 0, bytesRead);
        outStream.flush();
    }

    private static final DateFormat dateFormat =
                           // Tue, 05 Dec 2000 17:28:07 GMT
        new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);

    private static final String JAVA_VERSION_MSG1 =
        "<html><body><h1>Incorrect Java Version</h1>" +
        "Sorry, but the team planning tools require version 1.4 or higher " +
        "of the Java Runtime Environment (JRE).  You are currently running the " +
        "dashboard with version ";
    private static final String JAVA_VERSION_MSG2 =
        " of the JRE.  To use the team planning tools, please upgrade the " +
        "Java Runtime Environment on your computer, restart the dashboard, " +
        "and try again.</body></html>";


    private static final String DIR_MISSING_MSG1 =
        "<html><body><h1>Team Data Directory Missing</h1>" +
        "The team planning tools need access to the team data directory for " +
        "this project.  According to your current project settings, that " +
        "directory is <pre>";
    private static final String DIR_MISSING_MSG2 =
        "</pre>  Unfortunately, this directory does not appear to exist.  " +
        "Check to make certain that the directory is accessible and try " +
        "again.</body></html>";

}