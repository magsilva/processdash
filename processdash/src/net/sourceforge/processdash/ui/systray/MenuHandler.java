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

package net.sourceforge.processdash.ui.systray;

import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.EventHandler;
import java.beans.PropertyChangeListener;

import net.sourceforge.processdash.DashController;
import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.log.time.TimeLoggingModel;

/**
 * Creates and manages the popup menu for the dashboard tray icon.
 * 
 * This class creates the popup menu and initializes it with appropriate menu
 * items. Then, it registers as a listener for relevant changes in application
 * state, and updates the contents of the popup menu appropriately.
 * 
 * @author tuma
 */
public class MenuHandler {

    private PopupMenu popupMenu;

    private ActionListener showWindowAction;

    private ActionListener playPauseAction;

    private ActionListener changeTaskAction;

    private static final Resources res = Resources
            .getDashBundle("ProcessDashboard.SysTray.Menu");


    public MenuHandler(ProcessDashboard pdash, TrayIcon icon) {
        this.popupMenu = new PopupMenu();
        icon.setPopupMenu(popupMenu);

        createSharedActions(pdash);

        popupMenu.add(makeShowWindowMenuItem(pdash));
        popupMenu.add(makeChangeTaskMenuItem());
        popupMenu.add(new PlayPauseMenuItem(pdash.getTimeLoggingModel()));
        popupMenu.addSeparator();
        popupMenu.add(makeExitMenuItem(pdash));

        // TODO: still need to add other menu items:
        // - play/pause timer
        // - change active task
        // - listen to script menu & mirror its contents
        // - Maybe mirror configuration menu?
    }

    public ActionListener getDefaultAction() {
        return showWindowAction;
    }

    public ActionListener getPlayPauseAction() {
        return playPauseAction;
    }

    public ActionListener getChangeTaskAction() {
        return changeTaskAction;
    }

    private void createSharedActions(ProcessDashboard pdash) {
        showWindowAction = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                DashController.raiseWindow();
            }
        };

        final TimeLoggingModel timeLoggingModel = pdash.getTimeLoggingModel();
        playPauseAction = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                timeLoggingModel.setPaused(!timeLoggingModel.isPaused());
            }
        };

        changeTaskAction = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // TODO Auto-generated method stub
            }
        };
    }

    private MenuItem makeShowWindowMenuItem(ProcessDashboard pdash) {
        String windowTitle = pdash.getTitle();
        String menuText = res.format("Show_Window_FMT", windowTitle);
        MenuItem showWindow = new MenuItem(menuText);
        showWindow.addActionListener(showWindowAction);
        return showWindow;
    }

    private MenuItem makeChangeTaskMenuItem() {
        MenuItem changeTaskItem = new MenuItem(res.getString("Change_Task"));
        changeTaskItem.addActionListener(changeTaskAction);
        return changeTaskItem;
    }

    private MenuItem makeExitMenuItem(ProcessDashboard pdash) {
        MenuItem exitItem = new MenuItem(res.getString("Exit"));
        exitItem.addActionListener(EventHandler.create(ActionListener.class,
            pdash, "exitProgram"));
        return exitItem;
    }

    public class PlayPauseMenuItem extends MenuItem {
        TimeLoggingModel timeLoggingModel;

        public PlayPauseMenuItem(TimeLoggingModel timeLoggingModel) {
            this.timeLoggingModel = timeLoggingModel;

            PropertyChangeListener pcl = EventHandler.create(
                PropertyChangeListener.class, this, "update");
            timeLoggingModel.addPropertyChangeListener(pcl);

            addActionListener(playPauseAction);

            update();
        }

        public void update() {
            String display;
            if (!timeLoggingModel.isLoggingAllowed())
                display = res.getString("StartStop.Disabled");
            else if (timeLoggingModel.isPaused())
                display = res.getString("StartStop.Paused");
            else
                display = res.getString("StartStop.Timing");
            setLabel(display);
        }
    }

}
