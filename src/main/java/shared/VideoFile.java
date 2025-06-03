package shared;

import java.io.Serializable;
import java.util.Objects;

public class VideoFile implements Serializable {
    private String name;
    private String format;
    private String resolution;

    public VideoFile(String name, String format, String resolution) {
        this.name = name;
        this.format = format;
        this.resolution = resolution;
    }

    public String getName() {
        return name;
    }

    public String getFormat() {
        return format;
    }

    public String getResolution() {
        return resolution;
    }

    public String getFilename() {
        return name + "-" + resolution + "." + format;
    }

    @Override
    public String toString() {
        return getFilename();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VideoFile)) return false;
        VideoFile that = (VideoFile) o;
        return Objects.equals(name, that.name)
                && Objects.equals(format, that.format)
                && Objects.equals(resolution, that.resolution);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, format, resolution);
    }
}
