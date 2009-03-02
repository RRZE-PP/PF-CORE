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
 * $Id: JavaVersion.java 6419 2009-01-19 01:03:34Z tot $
 */
package de.dal33t.powerfolder.util;

/**
 * Class representing a Java version. It follows the java.runtime.version
 * format of
 * &lt;major&gt;.&lt;minor&gt;.&lt;point&gt;_&lt;build&gt;-b&lt;update&gt;,
 * eg 1.6.2_10-b12
 * It implements Comparable&lt;JavaVersion&gt; by traversing the version
 * values.
 */
public class JavaVersion implements Comparable<JavaVersion> {

    private static JavaVersion systemVersion;

    private final int major;
    private final int minor;
    private final int point;
    private final int build;
    private final int update;

    /**
     * Constructor
     *
     * @param major
     * @param minor
     * @param point
     * @param build
     * @param update
     */
    public JavaVersion(int major, int minor, int point, int build, int update)
    {
        this.major = major;
        this.minor = minor;
        this.point = point;
        this.build = build;
        this.update = update;
    }

    /**
     * Constructor, defaulting update to zero.
     *
     * @param major
     * @param minor
     * @param point
     * @param build
     */
    public JavaVersion(int major, int minor, int point, int build) {
        this(major, minor, point, build, 0);
    }

    /**
     * Constructor, defaulting build and update to zero.
     *
     * @param major
     * @param minor
     * @param point
     */
    public JavaVersion(int major, int minor, int point) {
        this(major, minor, point, 0, 0);
    }

    /**
     * Constructor, defaulting point, build and update to zero.
     *
     * @param major
     * @param minor
     */
    public JavaVersion(int major, int minor) {
        this(major, minor, 0, 0, 0);
    }

    /**
     * Returns the build value of the version.
     *
     * @return
     */
    public int getBuild() {
        return build;
    }

    /**
     * Returns the major value of the version.
     *
     * @return
     */
    public int getMajor() {
        return major;
    }

    /**
     * Returns the minor value of the version.
     *
     * @return
     */
    public int getMinor() {
        return minor;
    }

    /**
     * Returns the point value of the version.
     *
     * @return
     */
    public int getPoint() {
        return point;
    }

    /**
     * Returns the update value of the version.
     *
     * @return
     */
    public int getUpdate() {
        return update;
    }

    /**
     * Tests for equality with another object. If object is a JavaVersion,
     * tests all version values.
     *
     * @param obj
     * @return
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        JavaVersion that = (JavaVersion) obj;

        if (build != that.build) {
            return false;
        }
        if (major != that.major) {
            return false;
        }
        if (minor != that.minor) {
            return false;
        }
        if (point != that.point) {
            return false;
        }
        if (update != that.update) {
            return false;
        }

        return true;
    }

    /**
     * Hash of the version values.
     *
     * @return
     */
    public int hashCode() {
        int result = major;
        result = 31 * result + minor;
        result = 31 * result + point;
        result = 31 * result + build;
        result = 31 * result + update;
        return result;
    }

    /**
     * Compare to another JavaVersion, progressing down major, minor, point,
     * build and finally update.
     *
     * @param o
     * @return
     */
    public int compareTo(JavaVersion o) {
        if (major == o.major) {
            if (minor == o.minor) {
                if (point == o.point) {
                    if (build == o.build) {
                        if (update == o.update) {
                            return 0;
                        } else  {
                            return update - o.update;
                        }
                    } else  {
                        return build - o.build;
                    }
                } else  {
                    return point - o.point;
                }
            } else  {
                return minor - o.minor;
            }
        } else  {
            return major - o.major;
        }
    }

    /**
     * Displays as
     * &lt;major&gt;.&lt;minor&gt;.&lt;point&gt;_&lt;build&gt;-b&lt;update&gt;
     * It skips 'build' and 'update' values if not available (zero).
     *
     * @return
     */
    public String toString() {
        if (build > 0) {
            if (update > 0) {
                if (update <= 10) {
                    // update like '-b0x'
                    return major + "." + minor + '.' + point + '_' + build +
                            "-b0" + update;
                } else {
                    return major + "." + minor + '.' + point + '_' + build +
                            "-b" + update;
                }
            } else {
                return major + "." + minor + '.' + point + '_' + build;
            }
        } else {
            return major + "." + minor + '.' + point;
        }
    }

    /**
     * Gets the system version of Java. First tries 'java.runtime.version',
     * then 'java.version', then 'java.specification.version', otherwise
     * it folds.
     *
     * @return
     */
    public static JavaVersion systemVersion() {
        if (systemVersion == null) {
            String versionText = System.getProperty("java.runtime.version");
            if (versionText == null) {
                versionText = System.getProperty("java.version");
            }
            if (versionText == null) {
                versionText = System.getProperty("java.specification.version");
            }
            if (versionText == null) {
                throw new IllegalStateException(
                        "Could not retrieve the system version of Java.");
            }
            systemVersion = parse(versionText);
        }

        return systemVersion;
    }

    /**
     * Parse a string version into a Java Version. Expects something in between
     * '1.6' and '1.6.2_10-b12' format.
     *
     * @param version
     * @return
     */
    public static JavaVersion parse(String version) {
        String[] strings = version.split("\\.");
        if (strings.length < 2) {
            throw new IllegalStateException(
                    "Could not parse system version of Java: " + version);
        }
        String majorString = strings[0];
        String minorString = strings[1];

        // Defaults
        String pointString = "0";
        String buildString = "0";
        String updateString = "0";

        if (strings.length >= 3) {
            String strings1 = strings[2];
            if (strings1.contains("_")) {
                String[] strings2 = strings1.split("_");
                pointString = strings2[0];
                if (strings2.length >= 2) {
                    String[] strings3 = strings2[1].split("\\-b");
                    buildString = strings3[0];
                    if (strings3.length >= 2) {
                        updateString = strings3[1];
                    }
                }
            } else {
                pointString = strings1;
            }
        }

        return new JavaVersion(Integer.parseInt(majorString),
                Integer.parseInt(minorString), Integer.parseInt(pointString),
                Integer.parseInt(buildString), Integer.parseInt(updateString));
    }
}
