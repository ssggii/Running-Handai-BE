package com.server.running_handai.course.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Hidden
@Getter
@Setter
@JacksonXmlRootElement(localName = "gpx")
@JsonIgnoreProperties(ignoreUnknown = true)
public class GpxDto {

    @JacksonXmlProperty(localName = "trk")
    private Trk trk;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Trk {
        @JacksonXmlProperty(localName = "trkseg")
        @JacksonXmlElementWrapper(useWrapping = false)
        private List<Trkseg> trksegs;
    }

    @Getter
    @Setter
    public static class Trkseg {
        @JacksonXmlProperty(localName = "trkpt")
        @JacksonXmlElementWrapper(useWrapping = false)
        private List<Trkpt> trkpts;
    }

    @Getter
    @Setter
    public static class Trkpt {
        @JacksonXmlProperty(isAttribute = true)
        private double lat;

        @JacksonXmlProperty(isAttribute = true)
        private double lon;

        @JacksonXmlProperty(localName = "ele")
        private double ele;
    }
}
