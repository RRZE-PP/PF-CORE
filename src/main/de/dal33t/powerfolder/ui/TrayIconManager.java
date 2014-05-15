/*
 * Copyright 2004 - 2008 Christian Sprajc. All rights reserved.
 *
 * This file is part of PowerFolder.
 *
 * PowerFolder is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation.
 *
 * PowerFolder is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PowerFolder. If not, see <http://www.gnu.org/licenses/>.
 *
 * $Id: TrayIconManager.java 15105 2011-05-11 09:26:16Z harry $
 */
package de.dal33t.powerfolder.ui;

import java.awt.EventQueue;
import java.awt.Image;
import java.awt.TrayIcon;
import java.io.IOException;
import java.util.TimerTask;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.ui.event.SyncStatusEvent;
import de.dal33t.powerfolder.ui.event.SyncStatusListener;
import de.dal33t.powerfolder.ui.notices.SimpleNotificationNotice;
import de.dal33t.powerfolder.ui.util.DelayedUpdater;
import de.dal33t.powerfolder.ui.util.Icons;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.os.OSUtil;

/**
 * Encapsultes tray icon functionality. Anything to do with the tray icon should
 * be done *HERE*. This keeps all tray functionality encapsulated.
 * <p/>
 * Blink has the highest priority. If blink is true, the 'P' icon will blink and
 * the blinkText will be the tool tip, explaining why it is blinking.
 * <p/>
 * Sync has second highest priority. If syncing, the icon will rotate and tool
 * tip will say 'syncing'.
 * <p/>
 * Otherwise, normal or warning will display.
 */
public class TrayIconManager extends PFComponent {

    private static final long ROTATION_STEP_DELAY = 200L;

    private final UIController uiController;
    private TrayIcon trayIcon;
    private final AtomicInteger atomicAngle = new AtomicInteger();
    private final AtomicBoolean atomicConnectedAndLoggedIn = new AtomicBoolean();
    private final AtomicBoolean atomicSyncing = new AtomicBoolean();
    private final DelayedUpdater iconUpdater;

    public TrayIconManager(UIController uiController) {
        super(uiController.getController());
        this.uiController = uiController;

        if (OSUtil.isLinux()) {
            // PFC-2331
            whitelistSystray(getController());
        }

        iconUpdater = new DelayedUpdater(getController());

        Image image = Icons.getImageById(Icons.SYSTRAY_SYNC_COMPLETE);
        if (image == null) {
            logSevere("Unable to retrieve default system tray icon. "
                + "System tray disabled");
            OSUtil.disableSystray();
            return;
        }
        trayIcon = new TrayIcon(image);
        trayIcon.setImageAutoSize(true);
        updateConnectionStatus();
        updateIcon(SyncStatusEvent.NOT_STARTED);
        getController().getUIController().getApplicationModel()
            .addSyncStatusListener(new SyncStatusListener() {
                public void syncStatusChanged(final SyncStatusEvent event) {
                    iconUpdater.schedule(new Runnable() {
                        public void run() {
                            updateIcon(event);
                        }
                    });
                }

                public boolean fireInEventDispatchThread() {
                    return true;
                }
            });

        getController().scheduleAndRepeat(new SpinnerTask(),
            ROTATION_STEP_DELAY);
    }

    public static void whitelistSystray(Controller controller) {
        ScheduledFuture<?> fut = controller.schedule(new Runnable() {
            public void run() {
                try {
                    Runtime
                        .getRuntime()
                        .exec(
                            "gsettings set com.canonical.Unity.Panel systray-whitelist \"['all']\"");
                } catch (IOException e) {
                    Logger.getLogger(TrayIconManager.class.getName()).warning(
                        "Unable to whitelist application for system tray icon. "
                            + e);
                }
            }
        }, 0);
        try {
            fut.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            Logger.getLogger(TrayIconManager.class.getName()).warning(
                "Unable to whitelist application for system tray icon. " + e);
        }
    }

    /**
     * Only use this to actually display the TrayIcon. Any modifiers should be
     * done through this.
     *
     * @return
     */
    public TrayIcon getTrayIcon() {
        return trayIcon;
    }

    private void updateConnectionStatus() {
        ServerClient client = getController().getOSClient();
        boolean connected = client.isConnected();
        boolean loggedIn = client.isLoggedIn();

        // Do a notification if moved between connected and not connected.
        if (!PreferencesEntry.SHOW_SYSTEM_NOTIFICATIONS
            .getValueBoolean(getController()))
        {
            return;
        }
        if (atomicConnectedAndLoggedIn.getAndSet(connected && loggedIn) != connected
            && loggedIn)
        {
            // State changed, notify ui.
            String notificationText;
            String title = Translation
                .getTranslation("tray_icon_manager.status_change.title");
            if (connected && loggedIn) {
                notificationText = Translation
                    .getTranslation("tray_icon_manager.status_change.connected");
            } else if (!getController().getNodeManager().isStarted()) {
                notificationText = Translation
                    .getTranslation("tray_icon_manager.status_change.disabled");
            } else {
                notificationText = Translation
                    .getTranslation("tray_icon_manager.status_change.connecting");
            }
            uiController
                .getApplicationModel()
                .getNoticesModel()
                .handleNotice(
                    new SimpleNotificationNotice(title, notificationText));
        }
    }

    private void updateIcon(SyncStatusEvent event) {

        if (trayIcon == null) {
            // Tray icon not supported?
            return;
        }
        StringBuilder tooltip = new StringBuilder();

        tooltip.append(Translation.getTranslation("general.application.name")
            + ' ' + Controller.PROGRAM_VERSION);
        tooltip.append(" \n");

        Image image;

        boolean syncing = false;
        if (event.equals(SyncStatusEvent.PAUSED)) {
            image = Icons.getImageById(Icons.SYSTRAY_PAUSE);
            tooltip
                .append(Translation.getTranslation("systray.tooltip.paused"));
        } else if (event.equals(SyncStatusEvent.NOT_STARTED)) {
            image = Icons.getImageById(Icons.SYSTRAY_WARNING);
            tooltip.append(Translation
                .getTranslation("systray.tooltip.not_started"));
        } else if (event.equals(SyncStatusEvent.NO_FOLDERS)) {
            image = Icons.getImageById(Icons.SYSTRAY_WARNING);
            tooltip.append(Translation
                .getTranslation("systray.tooltip.no_folders"));
        } else if (event.equals(SyncStatusEvent.SYNCING)) {
            syncing = true;
            image = Icons.getImageById(Icons.SYSTRAY_SYNC_ANIMATION[atomicAngle
                .get()]);
            if (trayIcon != null) {
                trayIcon.setImage(image);
            }
            tooltip.append(Translation
                .getTranslation("systray.tooltip.syncing"));
            double overallSyncPercentage = getController().getUIController()
                .getApplicationModel().getFolderRepositoryModel()
                .getOverallSyncPercentage();
            if (overallSyncPercentage >= 0) {
                tooltip.append(' ');
                tooltip
                    .append(Format.formatDecimal(overallSyncPercentage) + '%');
            }
        } else if (event.equals(SyncStatusEvent.SYNCHRONIZED)) {
            image = Icons.getImageById(Icons.SYSTRAY_SYNC_COMPLETE);
            tooltip.append(Translation
                .getTranslation("systray.tooltip.in_sync"));
        } else if (event.equals(SyncStatusEvent.SYNC_INCOMPLETE)) {
            image = Icons.getImageById(Icons.SYSTRAY_SYNC_INCOMPLETE);
            tooltip.append(Translation
                .getTranslation("systray.tooltip.sync_incomplete"));
        } else if (event.equals(SyncStatusEvent.NOT_CONNECTED)) {
            image = Icons.getImageById(Icons.SYSTRAY_SYNC_INCOMPLETE);
            tooltip.append(Translation
                .getTranslation("systray.tooltip.not_connected"));
        } else if (event.equals(SyncStatusEvent.NOT_LOGGED_IN) || event.equals(SyncStatusEvent.LOGGING_IN)) {
            image = Icons.getImageById(Icons.SYSTRAY_WARNING);
            tooltip.append(Translation
                .getTranslation("systray.tooltip.not_logged_in"));
        } else if(event.equals(SyncStatusEvent.WARNING)) {
            image = Icons.getImageById(Icons.SYSTRAY_WARNING);
            tooltip.append(Translation
                .getTranslation("systray.tooltip.warning_notice"));
        } else if(event.equals(SyncStatusEvent.INFORMATION)) {
            image = Icons.getImageById(Icons.SYSTRAY_WARNING);
            tooltip.append(Translation
                .getTranslation("systray.tooltip.info_notice"));
        } else {
            logSevere("Not handling all sync states: " + event);
            image = Icons.getImageById(Icons.QUESTION);
        }

        atomicSyncing.set(syncing);

        trayIcon.setImage(image);
        trayIcon.setToolTip(tooltip.toString());
    }

    private void spinIcon() {
        if (atomicSyncing.get()) {
            int i = atomicAngle.incrementAndGet();
            if (i >= Icons.SYSTRAY_SYNC_ANIMATION.length) {
                atomicAngle.set(0);
                i = 0;

                // Update tool tip every time we pass zero.
                StringBuilder tooltip = new StringBuilder();
                tooltip.append(Translation
                    .getTranslation("systray.tooltip.syncing"));
                double overallSyncPercentage = getController()
                    .getUIController().getApplicationModel()
                    .getFolderRepositoryModel().getOverallSyncPercentage();
                if (overallSyncPercentage >= 0) {
                    tooltip.append(' ');
                    tooltip
                        .append(Format.formatDecimal(overallSyncPercentage) + '%');
                }
                trayIcon.setToolTip(tooltip.toString());
            }
            Image image = Icons.getImageById(Icons.SYSTRAY_SYNC_ANIMATION[i]);
            if (trayIcon != null) {
                trayIcon.setImage(image);
            }
        }
    }

    /**
     * Display tray sync icon in hi resolution? Linux needs to be low
     * resolution, otherwise it looks rubbish.
     *
     * @return
     */
    public static boolean isHiRes() {
        return OSUtil.isLinux() || OSUtil.isMacOS();
    }

    /**
     * Timer to rotate the icon.
     */
    private class SpinnerTask extends TimerTask {
        public void run() {
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    spinIcon();
                }
            });
        }
    }
}
