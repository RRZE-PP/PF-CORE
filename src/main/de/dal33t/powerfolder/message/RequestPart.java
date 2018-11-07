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

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.d2d.D2DEvent;
import de.dal33t.powerfolder.d2d.D2DObject;
import de.dal33t.powerfolder.d2d.NodeEvent;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.protocol.DataRangeProto;
import de.dal33t.powerfolder.protocol.FileInfoProto;
import de.dal33t.powerfolder.protocol.FilePartRequestProto;
import de.dal33t.powerfolder.util.Range;
import de.dal33t.powerfolder.util.Reject;

import java.io.IOException;

public class RequestPart extends Message implements D2DObject, D2DEvent
{
    private static final long serialVersionUID = 100L;

    protected FileInfo file;
    protected Range range;
    protected double progress;

    public RequestPart() {
        // Serialization constructor
    }

    public RequestPart(FileInfo file, double progress) {
        this(file, Range.getRangeByLength(0, file.getSize()), progress);
    }

    public RequestPart(FileInfo file, Range range, double progress) {
        super();
        this.file = file;
        this.range = range;
        this.progress = progress;
        validate();
    }

    @Override
    public String toString() {
        return "Request to download part of : " + file + ", range " + range;
    }

    /**
     * @return the file which has the requested part
     */
    public FileInfo getFile() {
        return file;
    }

    /**
     * @return the range of data that is requested
     */
    public Range getRange() {
        return range;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof RequestPart) {
            RequestPart pr = (RequestPart) obj;
            return pr.file.equals(file) && pr.range.equals(range);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return file.hashCode() ^ range.hashCode();
    }

    /**
     * The progress is a guess of the transfer progress. The downloader sets
     * this value so the uploader can show a progress to the user. The actual
     * progress is implementation dependent and is therefore given as a double
     * value in the range [0,1]
     *
     * @return the progress
     */
    public double getProgress() {
        return progress;
    }

    // Overridden due to validation!
    private void readObject(java.io.ObjectInputStream in) throws IOException,
        ClassNotFoundException
    {
        in.defaultReadObject();
        validate();
    }

    private void validate() {
        validateFile(file);
        validateRange(range);
        validateProgress(progress);
    }

    private void validateFile(FileInfo file) {
        Reject.ifNull(file, "File is null");
    }

    private void validateRange(Range range) {
        Reject.ifNull(range, "Range is null");
        if (range.getStart() < 0 || range.getEnd() > file.getSize()) {
            Reject.ifTrue(range.getStart() < 0
                || range.getEnd() > file.getSize(), "Invalid range: " + range);
        }
    }

    private void validateProgress(double progress) {
        if (progress < 0 || progress > 1) {
            Reject.ifTrue(progress < 0 || progress > 1, "Invalid progress: "
                + progress);
        }
    }

    /** initFromD2DMessage
     * Init from D2D message
     * @author Christoph Kappel <kappel@powerfolder.com>
     * @param  mesg  Message to use data from
     **/

    @Override
    public void
    initFromD2D(AbstractMessage mesg)
    {
      if(mesg instanceof FilePartRequestProto.FilePartRequest)
        {
          FilePartRequestProto.FilePartRequest proto =
            (FilePartRequestProto.FilePartRequest)mesg;

          this.file     = new FileInfo(proto.getFileInfo());
          this.range    = new Range(proto.getDataRange());
        }
    }

    /** toD2D
     * Convert to D2D message
     * @author Christoph Kappel <kappel@powerfolder.com>
     * @return Converted D2D message
     **/

    @Override
    public AbstractMessage
    toD2D()
    {
      FilePartRequestProto.FilePartRequest.Builder builder =
        FilePartRequestProto.FilePartRequest.newBuilder();

      // Translate old message name to new name defined in protocol file
      builder.setClazzName("FilePartRequest");
      builder.setFileInfo((FileInfoProto.FileInfo)this.file.toD2D());
      builder.setDataRange((DataRangeProto.DataRange)this.range.toD2D());

      return builder.build();
    }

    @Override
    public NodeEvent getNodeEvent() {
        return NodeEvent.FILE_SEARCH_REQUEST;
    }

}
