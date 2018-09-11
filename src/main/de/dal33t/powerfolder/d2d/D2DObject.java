/*
 * Copyright 2015 Christian Sprajc. All rights reserved.
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
 * @author Christoph Kappel <kappel@powerfolder.com>
 * @version $Id$
 */

package de.dal33t.powerfolder.d2d;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.message.Message;

public interface
D2DObject {

    /**
     * initFromD2D
     * Init from D2D message
     *
     * @param mesg {@link Message} to use data from
     **/
    void initFromD2D(AbstractMessage mesg);

    /**
     * toD2DMessage
     * Convert to D2D message
     *
     * @return Converted {@link AbstractMessage D2D message}
     **/
    AbstractMessage toD2D();

    default void handle(Member node) {
        node.handleMessage((Message)this, node.getPeer());
    }

}
