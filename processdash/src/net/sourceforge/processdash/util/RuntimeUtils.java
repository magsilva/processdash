// Copyright (C) 2007 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLDecoder;
import java.security.AccessController;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RuntimeUtils {

    /** Return the path to the executable that can launch a new JVM.
     */
    public static String getJreExecutable() {
        File javaHome = new File(System.getProperty("java.home"));

        boolean isWindows = System.getProperty("os.name").toLowerCase()
                .indexOf("windows") != -1;
        String baseName = (isWindows ? "java.exe" : "java");

        String result = getExistingFile(javaHome, "bin", baseName);
        if (result == null)
            result = getExistingFile(javaHome, "sh", baseName);
        if (result == null)
            result = baseName;
        return result;
    }

    private static String getExistingFile(File dir, String subdir,
            String baseName) {
        dir = new File(dir, subdir);
        File file = new File(dir, baseName);
        if (file.exists())
            return file.getAbsolutePath();
        return null;
    }

    /** Returns a list of args that should be passed to any JVM that we
     * spawn.
     */
    public static String[] getPropagatedJvmArgs() {
        List<String> result = new ArrayList<String>();
        for (Map.Entry<String, String> e : PROPS_TO_PROPAGATE.entrySet()) {
            String propName = e.getKey();
            String propValue = e.getValue();

            if (propValue == null)
                propValue = System.getProperty(propName);

            if (propValue != null)
                result.add("-D" + propName + "=" + propValue);
        }
        return result.toArray(new String[result.size()]);
    }

    /** Register a particular system property as one that should be propagated
     * to child JVMs.
     * 
     * @param name the name of a property that should be propagated to
     *     child JVMs
     * @param value the value to propagate.  If null, the actual value will be
     *     retrieved on-the-fly via System.getProperty() at JVM creation time
     * @throws SecurityException if the caller does not have the appropriate
     *     permission to alter propagated system properties
     */
    public static void addPropagatedSystemProperty(String name, String value)
            throws SecurityException {
        if (!StringUtils.hasValue(name))
            throw new IllegalArgumentException("No property name was provided");

        checkJvmArgsPermission();
        PROPS_TO_PROPAGATE.put(name, value);
    }

    private static final Map<String,String> PROPS_TO_PROPAGATE =
        Collections.synchronizedMap(new HashMap<String,String>());
    static {
        addPropagatedSystemProperty("user.language", null);
        addPropagatedSystemProperty("java.util.logging.config.file", null);
    }

    /** Define the permission that is needed to alter propagated JVM args
     * 
     * @param p the permission to use
     * @throws SecurityException if a previous permission was set, and the
     *     caller does not have that permission.
     */
    public static void setJvmArgsPermission(Permission p)
            throws SecurityException {
        checkJvmArgsPermission();
        JVM_ARGS_PERMISSION = p;
    }

    private static Permission JVM_ARGS_PERMISSION = null;
    private static void checkJvmArgsPermission() {
        if (JVM_ARGS_PERMISSION != null)
            AccessController.checkPermission(JVM_ARGS_PERMISSION);
    }

    /** Return the file that forms the classpath for the given class.
     * 
     * If the class has been loaded from a local JAR file, this will return a
     * File object pointing to that JAR file.
     * 
     * If the class has been loaded from a local directory, this will return a
     * File object pointing to the directory that forms the base of the
     * classpath.
     * 
     * If a local file cannot be determined for the given class, returns null.
     */
    public static File getClasspathFile(Class clz) {
        String className = clz.getName();
        String baseName = className.substring(className.lastIndexOf(".") + 1);
        URL classUrl = clz.getResource(baseName + ".class");
        if (classUrl == null)
            return null;

        String classUrlStr = classUrl.toString();
        if (classUrlStr.startsWith("jar:file:"))
            return getJarBasedClasspath(classUrlStr);
        else if (classUrlStr.startsWith("file:"))
            return getDirBasedClasspath(classUrlStr, className);
        else
            return null;
    }

    /** Return a classpath for use with the packaged JAR file containing the
     * compiled classes used by the dashboard.
     * 
     * @param selfUrlStr the URL of the class file for this class; must be a
     *    jar:file: URL.
     * @return the JAR-based classpath in effect
     */
    private static File getJarBasedClasspath(String selfUrlStr) {
        // remove initial "jar:file:" and trailing "!/package/..." information
        selfUrlStr = selfUrlStr.substring(9, selfUrlStr.indexOf("!/"));

        String jarFileName;
        try {
            jarFileName = URLDecoder.decode(selfUrlStr, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // can't happen
            return null;
        }
        File jarFile = new File(jarFileName).getAbsoluteFile();
        return jarFile;
    }

    /** Return a classpath for use with an unpackaged class file
     * 
     * @param selfUrlStr the URL of the class file for this class; must be a
     *    file: URL pointing to a .class file in the "bin" directory of a
     *    process dashboard project directory
     * @return the classpath that can be used to launch a dashboard instance.
     *    This classpath will include the effective "bin" directory that
     *    contains this class, and will also include the JAR files in the
     *    "lib" directory of the process dashboard project directory.
     */
    private static File getDirBasedClasspath(String selfUrlStr, String className) {
        // remove initial "file:" and trailing "/net/..." information
        String classFilename = className.replace('.', '/');
        int packagePos = selfUrlStr.indexOf(classFilename);
        if (packagePos == -1)
            return null;
        selfUrlStr = selfUrlStr.substring(5, packagePos);

        File binDir;
        try {
            String path = URLDecoder.decode(selfUrlStr, "UTF-8");
            binDir = new File(path).getAbsoluteFile();
            return binDir;
        } catch (Exception e) {
            return null;
        }
    }

    /** Wait for a process to complete, and return the exit status.
     * 
     * This consumes output from the process, so it will not be blocked.  This
     * effectively works around limitations in <code>Process.waitFor()</code>.
     * 
     * @param p the process to wait for
     * @return the exit status of the process
     */
    public static int doWaitFor(Process p) {
        return consumeOutput(p, null, null);
    }

    /** Consume the output generated by a process until it completes, and
     * return its exit value.
     * 
     * The javadoc for the Runtime.exec() method casually mentions that if you
     * launch a process which generates output (to stdout or stderr), you must
     * consume that output, or the process will become blocked when its
     * OS-provided output buffers become full. This method consumes process
     * output, as required.
     * 
     * @param p the process to consume output for
     * @param destOut a stream to which stdout data should be copied.  If null,
     *    stdout data will be discarded
     * @param destErr a stream to which stderr data should be copied.  If null,
     *    stderr data will be discarded
     * @return the exit status of the process.
     */
    public static int consumeOutput(Process p, OutputStream destOut,
            OutputStream destErr) {
        return consumeOutput(p, destOut, destErr, false);
    }

    /**
     * Collect the output from a process.
     * 
     * @param p the process to run; this method will wait until the process
     *       terminates.
     * @param stdOut true if data from stdout should be collected
     * @param stdErr true if data from stderr should be collected
     * @return the bytes generated by the process.
     */
    public static byte[] collectOutput(Process p, boolean stdOut,
            boolean stdErr) {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        consumeOutput(p, stdOut ? buf : null, stdErr ? buf : null, true);
        return buf.toByteArray();
    }

    private static int consumeOutput(Process p, OutputStream destOut,
            OutputStream destErr, boolean eager) {

        int exitValue = -1; // returned to caller when p is finished

        try {

            InputStream in = p.getInputStream();
            InputStream err = p.getErrorStream();

            boolean finished = false; // Set to true when p is finished

            while (!finished) {
                try {
                    int c;

                    while (in.available() > 0 && (c = in.read()) != -1)
                        if (destOut != null)
                            destOut.write(c);

                    while (err.available() > 0 && (c = err.read()) != -1)
                        if (destErr != null)
                            destErr.write(c);

                    // Ask the process for its exitValue. If the process
                    // is not finished, an IllegalThreadStateException
                    // is thrown. If it is finished, we fall through and
                    // the variable finished is set to true.

                    exitValue = p.exitValue();
                    finished = true;

                } catch (IllegalThreadStateException e) {

                    // Process is not finished yet;
                    // Sleep a little to save on CPU cycles
                    Thread.sleep(eager ? 10 : 500);
                }
            }


        } catch (Exception e) {
            // unexpected exception! print it out for debugging...
            System.err.println("doWaitFor(): unexpected exception - "
                    + e.getMessage());
        }

        // return completion status to caller
        return exitValue;
    }


    /**
     * Utility routine to check that a given class provides a particular
     * method in the enclosing java runtime environment.
     * 
     * Note: this verifies the presence of a method by name only.  In the
     * future, if more stringent checks are required (for example, for specific
     * overloaded arguments), a new method can be introduced.
     * 
     * @param clazz the class to check
     * @param methodName a method to require
     * @throws UnsupportedOperationException if the method does not exist
     */
    public static void assertMethod(Class clazz, String methodName)
            throws UnsupportedOperationException {
        Method[] m = clazz.getMethods();
        for (Method method : m) {
            if (method.getName().equals(methodName))
                return;
        }
        throw new UnsupportedOperationException("Class " + clazz.getName()
                + " does not support method " + methodName);
    }

}
