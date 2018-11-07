/*
 * Copyright 2004 - 2009 Christian Sprajc. All rights reserved.
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
 * $Id: CompilingPatternMatch.java 8022 2009-05-21 07:46:07Z harry $
 */
package de.dal33t.powerfolder.util.pattern;

import de.dal33t.powerfolder.Constants;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

/**
 * Factory to retrieve the pattern match implementation which is most performant
 * for given pattern.
 *
 * @author sprajc
 */
public class PatternFactory {
    private static Logger LOG = Logger
        .getLogger(PatternFactory.class.getName());

    private PatternFactory() {}

    /**
     * Auto-chooses fastest implementation of pattern algo.
     *
     * @param patternText The pattern
     * @return a pattern implementation for the given pattern text.
     */
    public static @NotNull Pattern createPattern(@NotNull String patternText) {
        if (!patternText.contains("*")) {
            return new ExactMatchPattern(patternText);
        } else if (patternText.lastIndexOf('*') == 0) {
            return new EndMatchPattern(patternText);
        } else if (patternText.indexOf('*') == patternText.length() - 1) {
            return new StartMatchPattern(patternText);
        } else if (patternText.equalsIgnoreCase(
            DefaultExcludes.OFFICE_TEMP.getPattern()))
        {
            // This is a heuristic but much quicker implementation for ignoring
            // office temp files.
            return new OfficeTempFilesMatchPattern("~", "*.tmp");
        } else if (patternText.equalsIgnoreCase(
            DefaultExcludes.OFFICEX_TEMP.getPattern()))
        {
            // This is a heuristic but much quicker implementation for ignoring
            // officex temp files.
            return new OfficeTempFilesMatchPattern(Constants.MS_OFFICE_FILENAME_PREFIX, "*");
        } else if (patternText.equalsIgnoreCase(
            DefaultExcludes.LIBRE_TEMP.getPattern()))
        {
            // This is a heuristic but much quicker implementation for ignoring
            // libre/openoffice temp files.
            return new OfficeTempFilesMatchPattern(Constants.LIBRE_OFFICE_FILENAME_PREFIX, "*");
        } else {
            // Fallback solution: Works for all, but is not optimized.
            LOG.fine("Using fallback for pattern '" + patternText + "'");
            return new CompiledPattern(patternText);
        }
    }

}
