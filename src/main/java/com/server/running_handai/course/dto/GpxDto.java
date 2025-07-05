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

    @JacksonXmlProperty(localName = "rte")
    private Rte rte;

    // 트랙(trk) 구조
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

    // 루트(rte) 구조 파싱
    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Rte {
        @JacksonXmlProperty(localName = "name")
        private String name;

        @JacksonXmlProperty(localName = "rtept")
        @JacksonXmlElementWrapper(useWrapping = false)
        private List<Rtept> rtepts;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Rtept {
        @JacksonXmlProperty(isAttribute = true)
        private double lat;

        @JacksonXmlProperty(isAttribute = true)
        private double lon;

        @JacksonXmlProperty(localName = "ele")
        private double ele;
    }
}
