/* $Id: Logger.java,v 1.45 2006/04/26 14:24:33 totmacherr Exp $
 */
package de.dal33t.powerfolder.util;

import java.awt.Color;
import java.io.*;
import java.util.*;

import javax.swing.text.*;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ClassUtils;

import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.net.ConnectionHandler;

/**
 * Logger class
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.45 $
 */
public class Logger {    
    private static final String DEBUG_DIR = "debug";
    private static final String EOL = "\r\n";
    
    // log levels
    public static final String INFO = "INFO";
    public static final String WARN = "WARN";
    public static final String ERROR = "ERROR";
    public static final String DEBUG = "DEBUG";
    public static final String VERBOSE = "VERBOSE";

    // for short text:
    private static final String VERBS = "VERBS";

    private Object base;

    private static File logFile;
    private static OutputStream logFileOut;
    private static StyledDocument logBuffer;
    private static int nLogLines;
    private static boolean logToConsoleEnabled;
    private static boolean logToTextPanelEnabled;
    private static boolean noAWTLibs;

    private static Set logClasses = new HashSet();

    private static Map logColors = new HashMap();
    // A set of excluded classes in the logger
    private static Set excludedConsoleClasses = new HashSet();
    private static Set excludedTextPanelClasses = new HashSet();
    private static Set excludedConsoleLogLevels = new HashSet();

    private static Set excludedTextPanelLogLevels = new HashSet();

    private static boolean logToFileEnabled;

    static {
        // console Enabled by default
        logToConsoleEnabled = true;

        // textPanel by default disabled
        logToTextPanelEnabled = false;
        
        //write logfiles by default        
        logToFileEnabled = true;

        //excludedConsoleClasses.add(Folder.class);
        //excludedConsoleClasses.add(TransferManager.class);
        //excludedConsoleClasses.add(ConnectionHandler.class);
        //excludedConsoleClasses.add(Member.class);
        //excludedConsoleClasses.add(NodeManager.class);
        
        excludedTextPanelClasses.add(Folder.class);
        //excludedTextPanelClasses.add(TransferManager.class);
        excludedTextPanelClasses.add(ConnectionHandler.class);
        //excludedTextPanelClasses.add(Member.class);
        //excludedTextPanelClasses.add(NodeManager.class);
        
        excludedConsoleLogLevels.add(VERBOSE);
        excludedTextPanelLogLevels.add(VERBOSE);
        //excludedConsoleLogLevels.add(DEBUG);
        // excludedTextPanelLogLevels.add(DEBUG);
        //excludedConsoleLogLevels.add(WARN);
        //excludedTextPanelLogLevels.add(WARN);

        if (!excludedConsoleClasses.isEmpty()) {
            System.err.println("Excluding from logging: "
                + excludedConsoleClasses);
        }

        // Okay lets check if we have an AWT system
        try {
            Color col = Color.RED;
            col.brighter();

            SimpleAttributeSet warn = new SimpleAttributeSet();
            StyleConstants.setForeground(warn, Color.BLUE);
            logColors.put(WARN, warn);

            SimpleAttributeSet error = new SimpleAttributeSet();
            StyleConstants.setForeground(error, Color.RED);
            logColors.put(ERROR, error);

            SimpleAttributeSet info = new SimpleAttributeSet();
            StyleConstants.setForeground(info, Color.BLACK);
            logColors.put(INFO, info);

            SimpleAttributeSet verbose = new SimpleAttributeSet();
            StyleConstants.setForeground(verbose, Color.GRAY);
            logColors.put(VERBOSE, verbose);

            SimpleAttributeSet debug = new SimpleAttributeSet();
            StyleConstants.setForeground(debug, Color.GREEN.darker());
            logColors.put(DEBUG, debug);

            // Okay we have AWT
            noAWTLibs = false;
        } catch (Error e) {
            // ERROR ? Okay no AWT
            noAWTLibs = true;
        }
    }

    private static File getDebugDir() {
        return new File(DEBUG_DIR);
    }
    
    public Set getLogClasses() {
        return logClasses;
    }

    private Logger(Object base) {
        this.base = base;
        logClasses.add(base.getClass());
    }

    /**
     * Deletes the debug log directory
     */
    public static final void deleteDebugDir() {
        try {
            FileUtils.deleteDirectory(getDebugDir());
        } catch (IOException e) {
            System.err.println("Unable to delete debug directory: " + e);
        }
    }

    /**
     * Returns a simple logger
     * 
     * @param base
     * @return
     */
    public static final Logger getLogger(Object base) {
        return new Logger(base);
    }

    public static void setLogToFileEnable(boolean logToFile) {
        logToFileEnabled = logToFile;
    }

    public static boolean isLogToFileEnabled() {
        return logToFileEnabled;
    }

    /**
     * Sets the logfile for all logging output (verbose included)
     * 
     * @param logFilename
     */
    public static final void setLogFile(String logFilename) {
        File debugDir = getDebugDir();
        File detailLogsDir = new File(debugDir, "detaillogs");      
        debugDir.mkdir();
        detailLogsDir.mkdirs();
        logFile = new File(debugDir, logFilename);
                
        try {
            if (logFile.exists()) {
                logFile.delete();
            }
            logFile.createNewFile();
        } catch (IOException e) {
            System.err.println("Unable to create logfile '"
                + logFile.getAbsolutePath() + "'");
            e.printStackTrace();
            logFile = null;
        }
    }

    /**
     * Enables/Disables the Console loggin.
     * <p>
     * Clears the logging document if log disabled
     * 
     * @param enabled
     */
    public static void setEnabledConsoleLogging(boolean enabled) {
        logToConsoleEnabled = enabled;
    }

    /**
     * Enables/Disables the TextPanel loggin.
     * <p>
     * Clears the logging document if log disabled
     * 
     * @param enabled
     */
    public static void setEnabledTextPanelLogging(boolean enabled) {
        logToTextPanelEnabled = enabled;
        if (!logToTextPanelEnabled) {
            // Clear log document
            logBuffer = null;
        }
    }

    /**
     * Excludes a loglevel on the console output
     * 
     * @param logLevel
     */
    public static void setExcludeConsoleLogLevel(String logLevel) {
        excludedConsoleLogLevels.add(logLevel);
    }

    public static void removeExcludeTextPanelLogLevel(String logLevel) {
        excludedTextPanelLogLevels.remove(logLevel);
    }

    public static boolean isExludedTextPanelLogLevel(String logLevel) {
        return excludedTextPanelLogLevels.contains(logLevel);
    }

    /**
     * Excludes a loglevel on the TextPanel output
     * 
     * @param logLevel
     */
    public static void setExcludeTextPanelLogLevel(String logLevel) {
        excludedTextPanelLogLevels.add(logLevel);
    }

    /**
     * Answers if this logger logs on verbose level.
     * 
     * @return
     */
    public final boolean isVerbose() {
        return (!isExludedTextPanelLogLevel(VERBOSE) || isExludedTextPanelLogLevel(VERBOSE));
    }

    /**
     * Answers if this logger is excluded from console logging
     * 
     * @return
     */
    public final boolean isExcludedFromConsole() {
        return excludedConsoleClasses.contains(base.getClass());
    }

    /**
     * Answers if this logger is excluded from logging
     * 
     * @return
     */
    public final boolean isExcludedFromTextPanel() {
        return excludedTextPanelClasses.contains(base.getClass());
    }

    public final boolean isExcluded() {
        return isExcludedFromTextPanel() || isExcludedFromConsole();
    }

    /**
     * Resets the logbuffer with a max number of buffers lines
     * 
     * @param lines
     */
    public static final void setLogBuffer(int lines) {
        if (lines < 2) {
            throw new IllegalArgumentException(
                "Number of logbuffer lines must be at least 2");
        }
        nLogLines = lines;
    }

    /**
     * Returns the current total log buffer
     * 
     * @return
     */
    public synchronized static StyledDocument getLogBuffer() {
        if (logBuffer == null) {
            logBuffer = new DefaultStyledDocument();
        }
        return logBuffer;
    }

    public void info(Object str) {
        log(INFO, "" + str, null);
    }

    public void debug(Object str) {
        log(DEBUG, "" + str, null);
    }

    public void warn(Object str) {
        log(WARN, str == null ? null : str.toString(), null);
    }

    public void warn(Object str, Throwable throwable) {
        log(WARN, str == null ? null : str.toString(), throwable);
    }

    public void error(String str) {
        log(ERROR, str, null);
    }

    public void error(Throwable throwable) {
        if (throwable != null) {
            log(ERROR, null, throwable);
        }
    }

    public void error(String str, Throwable throwable) {
        error(str);
        error(throwable);
    }

    public void verbose(Throwable throwable) {
        if (throwable != null) {
            log(VERBOSE, null, throwable);
        }
    }

    public void verbose(Object str) {
        log(VERBOSE, str == null ? null : str.toString(), null);
    }

    public void verbose(Object str, Throwable throwable) {
        log(VERBOSE, str == null ? null : str.toString(), throwable);
    }

    /**
     * Logs a throwable
     * 
     * @param level
     * @param message
     */
    // private void log(String level, Throwable t) {
    // log(level, null, t);
    // }
        
    /**
     * Logs a message
     * 
     * @param level
     * @param message
     * @param throwable
     */
    private void log(String level, String message, Throwable throwable) {
        if (!(logToConsoleEnabled && logToTextPanelEnabled)) {
            // both disabled return
            return;
        }

        String levelMsg = level;
        if (levelMsg.length() == 4) {
            levelMsg += " ";
        }

        if (level == VERBOSE) {
            levelMsg = VERBS;
        }

        boolean excludeConsole = excludedConsoleLogLevels.contains(level)
            || isExcludedFromConsole();
        boolean excludeTextPanel = excludedTextPanelLogLevels.contains(level)
            || isExcludedFromTextPanel();

        if (!excludeConsole || !excludeTextPanel) {
            Date now = new Date();
            if (message == null) {
                if (throwable != null) {
                    message = "Exception Thrown: ";
                }
            }
            String detailLogMessage = Format.DETAILED_TIME_FOMRAT.format(now)
                + " " + levelMsg + " [" + getLoggerName() + "]: " + message
                + EOL;
            String shortLogMessage = Format.TIME_ONLY_DATE_FOMRAT.format(now)
                + " " + levelMsg + " [" + getLoggerName() + "]: " + message
                + EOL;
            if (throwable != null) {
                String stackTrace = stackTraceToString(throwable);
                detailLogMessage += stackTrace + EOL;
                shortLogMessage += stackTrace + EOL;
            }
            if (!excludeConsole) {
                getPrintStream(level).print(shortLogMessage);

                if (logToFileEnabled && logFile != null) {
                    writeToLogFile(detailLogMessage);
                }
            }

            if (!excludeTextPanel) {
                if (!noAWTLibs) {
                    // Only for awt capable systems
                    try {
                        MutableAttributeSet set = (MutableAttributeSet) logColors
                            .get(level);
                        if (logBuffer == null) {
                            getLogBuffer();
                        }

                        synchronized (logBuffer) {
                            logBuffer.insertString(logBuffer.getLength(),
                                detailLogMessage, set);

                            if (logBuffer.getLength() > nLogLines) {
                                logBuffer.remove(0, detailLogMessage.length());
                            }
                        }
                    } catch (RuntimeException e) {
                        e.printStackTrace();
                    } catch (BadLocationException e) {
                        // Ignore
                    } catch (Error e) {
                        e.printStackTrace();
                    }
                }
            }
        }

    }

    /**
     * Writes a message to the logfile
     * 
     * @param message
     */
    private void writeToLogFile(String message) {
        if (!logToFileEnabled) {
            return;
        }
        if (logFile == null) {
            return;
        }
        // append
        synchronized (logFile) {
            try {
                if (logFileOut == null) {
                    logFileOut = new BufferedOutputStream(new FileOutputStream(
                        logFile, true));
                }
                logFileOut.write(message.getBytes());
                logFileOut.flush();
            } catch (IOException e) {
                System.err.println("Unable to write to logfile '"
                    + logFile.getAbsolutePath() + "'. " + e.getMessage());
                e.printStackTrace();
            }
            // now write into detail log
            File singleLog = new File(getDebugDir() , "detaillogs/" + getLoggerName()
                + ".log.txt");
            try {
                if (!singleLog.exists()) {
                    singleLog.createNewFile();
                }
                FileOutputStream fOut = new FileOutputStream(singleLog, true);
                fOut.write(message.getBytes());
                fOut.close();
            } catch (IOException e) {
                System.err.println("Unable to write to logfile '"
                    + singleLog.getAbsolutePath() + "'. " + e.getMessage());
                // e.printStackTrace();
            }
        }
    }

    /**
     * Answers the printstream for the level
     * 
     * @param level
     * @return
     */
    private PrintStream getPrintStream(String level) {
        PrintStream out = (ERROR == level) || (WARN == level)
            ? System.err
            : System.out;
        return out;
    }

    /**
     * Helper method
     * 
     * @param e
     * @return
     */
    private static String stackTraceToString(Throwable e) {
        if (e == null) {
            return "no valid Exception to Trace";
        }
        ByteArrayOutputStream baout = new ByteArrayOutputStream();
        PrintStream pstream = new PrintStream(baout, true);
        e.printStackTrace(pstream);
        String result = baout.toString();
        pstream.close();
        try {
            baout.close();
        } catch (IOException x) {
        }
        return result;
    }

    /**
     * Returns the name of the logger. may be overriden by Loggable
     * 
     * @return
     */
    private String getLoggerName() {
        if (base == null) {
            return null;
        }
        String shortname;
        if (base instanceof Class) {
            shortname = ClassUtils.getShortClassName((Class) base);
        } else if ((base instanceof Loggable)
            && ((Loggable) base).getLoggerName() != null)
        {
            shortname = ((Loggable) base).getLoggerName();
        } else {
            shortname = ClassUtils.getShortClassName(base.getClass());
        }

        return shortname;
    }
}