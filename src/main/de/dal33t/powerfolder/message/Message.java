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
 * $Id$
 */
package de.dal33t.powerfolder.message;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.d2d.D2DObject;

import java.io.Serializable;

/**
 * General superclass for all messages sent from or to another member.
 * <P>
 * ATTENTION: All extending classes have to define a serialVersionUID.
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.3 $
 */
public class Message implements Serializable {
    private static final long serialVersionUID = 100L;

    public Message() {
    }

    public void handle(Member node) {
        node.handleMessage(this, node.getPeer());
    }
}