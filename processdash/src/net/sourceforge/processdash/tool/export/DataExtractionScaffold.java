// Copyright (C) 2008-2009 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.export;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.InternalSettings;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.data.repository.InvalidDatafileFormat;
import net.sourceforge.processdash.ev.DefaultTaskLabeler;
import net.sourceforge.processdash.ev.EVTaskDependencyResolver;
import net.sourceforge.processdash.ev.EVTaskList;
import net.sourceforge.processdash.ev.EVTaskListData;
import net.sourceforge.processdash.ev.EVTaskListMerged;
import net.sourceforge.processdash.ev.EVTaskListRollup;
import net.sourceforge.processdash.ev.TaskLabeler;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.Prop;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.log.defects.DefectAnalyzer;
import net.sourceforge.processdash.log.time.DashboardTimeLog;
import net.sourceforge.processdash.log.time.TimeLog;
import net.sourceforge.processdash.log.time.WorkingTimeLog;
import net.sourceforge.processdash.net.cache.ObjectCache;
import net.sourceforge.processdash.net.http.WebServer;
import net.sourceforge.processdash.templates.TemplateLoader;
import net.sourceforge.processdash.tool.export.mgr.ExternalResourceManager;
import net.sourceforge.processdash.tool.export.mgr.ImportManager;
import net.sourceforge.processdash.util.StringUtils;

public class DataExtractionScaffold implements DashboardContext {

    private File dataDirectory;

    private boolean useExternalResourceMappingFile;

    private DataRepository data;

    private DashHierarchy hierarchy;

    private TimeLog timeLog;

    private Pattern dataLoadPattern;

    private Pattern dataPrunePattern;

    public DataExtractionScaffold() {}

    @Override
    protected void finalize() {
        tearDown();
    }

    public File getDataDirectory() {
        return dataDirectory;
    }

    public void setDataDirectory(File dataDirectory) {
        this.dataDirectory = dataDirectory;
    }

    public boolean isUseExternalResourceMappingFile() {
        return useExternalResourceMappingFile;
    }

    public void setUseExternalResourceMappingFile(boolean use) {
        this.useExternalResourceMappingFile = use;
    }

    public Pattern getDataLoadPattern() {
        return dataLoadPattern;
    }

    public void setDataLoadPattern(Pattern dataLoadPattern) {
        this.dataLoadPattern = dataLoadPattern;
    }

    public Pattern getDataPrunePattern() {
        return dataPrunePattern;
    }

    public void setDataPrunePattern(Pattern dataPrunePattern) {
        this.dataPrunePattern = dataPrunePattern;
    }

    public DataRepository getData() {
        return data;
    }

    public DashHierarchy getHierarchy() {
        return hierarchy;
    }

    public ObjectCache getCache() {
        return null;
    }

    public TimeLog getTimeLog() {
        return timeLog;
    }

    public WebServer getWebServer() {
        return null;
    }

    public void tearDown() {
        if (data != null) {
            data.shutDown();
            DataImporter.shutDown();
            data.purgeDataStructures();
            EVTaskDependencyResolver.getInstance().flushCaches();
        }
    }


    public void init() throws Exception {
        String dataDirPath = dataDirectory.getAbsolutePath()
                + System.getProperty("file.separator");

        // load and initialize settings
        String settingsFilename = dataDirPath
                + InternalSettings.getSettingsFilename();
        InternalSettings.initialize(settingsFilename);
        InternalSettings.setReadOnly(true);
        InternalSettings.set("templates.disableSearchPath", "true");
        InternalSettings.set("export.disableAutoExport", "true");
        InternalSettings.set("slowNetwork", "true");

        // reset the template loader search path
        TemplateLoader.resetTemplateURLs();

        // setup the defect analyzer
        DefectAnalyzer.setDataDirectory(dataDirPath);

        // possibly initialize external resource mappings
        if (useExternalResourceMappingFile)
            ExternalResourceManager.getInstance().initializeMappings(
                dataDirectory,
                ExternalResourceManager.INITIALIZATION_MODE_ARCHIVE);

        // create the data repository.
        data = new DataRepository();
        DashHierarchy templates = TemplateLoader.loadTemplates(data);
        data.setDatafileSearchURLs(TemplateLoader.getTemplateURLs());

        // open and load the the user's work breakdown structure
        hierarchy = new DashHierarchy(null);
        String hierFilename = dataDirPath + Settings.getFile("stateFile");
        hierarchy.loadXML(hierFilename, templates);
        data.setNodeComparator(hierarchy);

        // create the time log
        timeLog = new WorkingTimeLog(dataDirectory);
        DashboardTimeLog.setDefault(timeLog);

        // open all the datafiles that were specified in the properties file.
        data.startInconsistency();
        openDataFiles(dataDirPath, PropertyKey.ROOT);
        data.openDatafile("", dataDirPath + "global.dat");

        // import data files
        DataImporter.setDynamic(false);
        ImportManager.init(data);

        data.finishInconsistency();

        // configure the task dependency resolver
        EVTaskDependencyResolver.init(this);
        EVTaskDependencyResolver.getInstance().setDynamic(false);
    }

    private void openDataFiles(String property_directory, PropertyKey key)
            throws FileNotFoundException, IOException, InvalidDatafileFormat {
        Prop val = hierarchy.pget(key);
        String id = val.getID();
        String dataFile = val.getDataFile();

        if (shouldLoadDataFile(id, dataFile)) {
            // System.out.println("opening datafile for " + key.path());
            data.openDatafile(key.path(), property_directory + dataFile);
        }

        if (!shouldPruneDataLoad(id)) {
            for (int i = 0; i < hierarchy.getNumChildren(key); i++) {
                openDataFiles(property_directory, hierarchy.getChildKey(key, i));
            }
        }
    }

    private boolean shouldLoadDataFile(String id, String dataFile) {
        // no datafile? then don't load it.
        if (!StringUtils.hasValue(dataFile))
            return false;
        // if no load pattern has been set, all files should be loaded.
        if (dataLoadPattern == null)
            return true;
        // return true if the pattern matches this id.
        return StringUtils.hasValue(id)
                && dataLoadPattern.matcher(id).find();
    }

    private boolean shouldPruneDataLoad(String id) {
        return (dataPrunePattern != null
                && StringUtils.hasValue(id)
                && dataPrunePattern.matcher(id).find());
    }


    public List<EVTaskList> getEVTaskLists() {
        return getEVTaskLists(true, true);
    }

    public List<EVTaskList> getRollupEVTaskLists() {
        return getEVTaskLists(false, true);
    }

    private List<EVTaskList> getEVTaskLists(boolean includePersonal,
            boolean includeRollups) {
        String[] taskListNames = EVTaskList.findTaskLists(data);
        List<EVTaskList> result = new ArrayList<EVTaskList>(
                taskListNames.length);

        TaskLabeler taskLabeler = null;

        for (String taskListName : taskListNames) {
            EVTaskList tl = EVTaskList.openExisting(taskListName, data,
                hierarchy, null, false);
            if (tl instanceof EVTaskListData && !includePersonal)
                continue;
            if (tl instanceof EVTaskListRollup && !includeRollups)
                continue;
            tl.recalc();
            tl = new EVTaskListMerged(tl, false, true, null);

            if (taskLabeler == null)
                taskLabeler = new DefaultTaskLabeler(this);
            tl.setTaskLabeler(taskLabeler);

            result.add(tl);
        }

        return result;
    }

}