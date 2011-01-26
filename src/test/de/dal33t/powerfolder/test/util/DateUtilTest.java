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
* $Id: AddLicenseHeader.java 4282 2008-06-16 03:25:09Z tot $
*/
package de.dal33t.powerfolder.test.util;

import java.util.Date;
import java.util.Calendar;
import java.util.GregorianCalendar;

import junit.framework.TestCase;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.DateUtil;
import org.jdesktop.swingx.calendar.DateUtils;

public class DateUtilTest extends TestCase {

    public void testDefault() {
        assertTrue(DateUtil.equalsFileDateCrossPlattform(new Date(1000), new Date(3000)));
        assertTrue(DateUtil.equalsFileDateCrossPlattform(new Date(11111111), new Date(11111000)));
        assertFalse(DateUtil.equalsFileDateCrossPlattform(new Date(222222222), new Date(222220000)));
        
        // 2000 milliseconds we assume the same
        assertFalse(DateUtil.isNewerFileDateCrossPlattform(new Date(3000),new Date(1000)));
        // 2001 milliseconds we assume different
        assertTrue(DateUtil.isNewerFileDateCrossPlattform(new Date(3001),new Date(1000)));
        // other is newer
        assertFalse(DateUtil.isNewerFileDateCrossPlattform(new Date(1000),new Date(3001)));
    }
    
    public void testSpecial() {
        assertTrue(DateUtil.equalsFileDateCrossPlattform(new Date(1146605870000L), new Date(1146605868805L)));
    }

    public void testDateInFuture() {
        Calendar cal = new GregorianCalendar();
        // Today is not more than 15 days ahead.
        assertFalse("Future date fault 0", DateUtil.isDateMoreThanNDaysInFuture(cal.getTime(), 15));
        cal.add(Calendar.DATE, 10);
        // Ten days in the future is not more than 15 days ahead.
        assertFalse("Future date fault 10", DateUtil.isDateMoreThanNDaysInFuture(cal.getTime(), 15));
        cal.add(Calendar.DATE, 10);
        // Twenty days in the future is more than 15 days ahead.
        assertTrue("Future date fault 20", DateUtil.isDateMoreThanNDaysInFuture(cal.getTime(), 15));
    }

    public void testBeforeEndOfDate() {
        Calendar cal = new GregorianCalendar();
        cal.add(Calendar.DATE, -1);
        assertTrue("Yesterday is before end of today", DateUtil.isBeforeEndOfDate(cal.getTime(), new Date()));
        cal.add(Calendar.DATE, 1);
        assertTrue("Today is before end of today", DateUtil.isBeforeEndOfDate(cal.getTime(), new Date()));
        cal.add(Calendar.DATE, 1);
        assertFalse("Tomorrow is not before end of today", DateUtil.isBeforeEndOfDate(cal.getTime(), new Date()));
    }

    public void testZeroTime() {
        Date date = new Date();
        Date result = DateUtil.zeroTime(date);
        assertFalse("Dates are the same", date.equals(result));

        Calendar dateCal = Calendar.getInstance();
        dateCal.setTime(date);

        Calendar resultCal = Calendar.getInstance();
        resultCal.setTime(result);

        assertSame("Days are different", dateCal.get(Calendar.DAY_OF_YEAR),
                resultCal.get(Calendar.DAY_OF_YEAR));
        assertFalse("The rest are same",
                dateCal.get(Calendar.HOUR_OF_DAY) == resultCal.get(Calendar.HOUR_OF_DAY) &&
                        dateCal.get(Calendar.MINUTE) == resultCal.get(Calendar.MINUTE) &&
                        dateCal.get(Calendar.SECOND) == resultCal.get(Calendar.SECOND) &&
                        dateCal.get(Calendar.MILLISECOND) == resultCal.get(Calendar.MILLISECOND));
    }

    public void testTruncateToHour() {
        Date date = new Date();
        Date result = DateUtil.truncateToHour(date);
        assertFalse("Dates are the same", date.equals(result));

        Calendar dateCal = Calendar.getInstance();
        dateCal.setTime(date);

        Calendar resultCal = Calendar.getInstance();
        resultCal.setTime(result);

        assertSame("Days are different", dateCal.get(Calendar.DAY_OF_YEAR),
                resultCal.get(Calendar.DAY_OF_YEAR));
        assertSame("Hours are different", dateCal.get(Calendar.HOUR_OF_DAY),
                resultCal.get(Calendar.HOUR_OF_DAY));
        assertFalse("The rest are same",
                dateCal.get(Calendar.MINUTE) == resultCal.get(Calendar.MINUTE) &&
                        dateCal.get(Calendar.SECOND) == resultCal.get(Calendar.SECOND) &&
                        dateCal.get(Calendar.MILLISECOND) == resultCal.get(Calendar.MILLISECOND));
    }

}
