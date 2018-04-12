package de.dal33t.powerfolder;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public enum DocumentType {

    DOCUMENT("doc", "docx", "odt", "epub", "xps"),
    SPREADSHEET("xls", "xlsx", "ods", "csv"),
    PRESENTATION("ppt", "pptx", "odp"),
    IMAGE("png", "jpg", "jpeg", "gif", "bmp"),
    AUDIO("wav", "mp3", "ogg", "oga", "webma", "fla", "flac", "m3u8a", "rtmpa", "djvu"),
    VIDEO("mp4", "flv", "rtmp", "rtmpv", "m4v", "ogv", "webmv", "m3uv", "m3u8v"),
    PDF("pdf"),
    TEXT("txt", "rtf");

    private Collection<String> extensions;

    DocumentType(String... extensions) {
        this.extensions = Collections.unmodifiableCollection(
            Arrays.asList(extensions));
    }

    public Collection<String> getExtensions() {
        return extensions;
    }

    public String toRegExp() {
        StringBuilder sb = new StringBuilder();
        for (String ext:
             extensions) {
            sb.append(ext);
            sb.append("|");
        }
        String s = sb.toString();
        if (s.length() > 0) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }
}
