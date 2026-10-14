package com.server.running_handai.global.measure;

import com.server.running_handai.domain.course.dto.CourseCreateRequestDto;
import com.server.running_handai.domain.course.service.CourseCreationService;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 회원 코스 생성을 인증 없이 호출하는 측정용 API입니다. (measure 프로필 전용)
 */
@Profile("measure")
@RestController
@RequestMapping("/measure/courses")
@RequiredArgsConstructor
public class CourseCreateMeasureController {

    private final CourseCreationService courseCreationService;

    /**
     * 측정용 GPX와 썸네일로 회원 코스를 생성하고 걸린 시간을 돌려줍니다.
     */
    @PostMapping
    public Map<String, Object> create(@RequestParam long memberId, @RequestParam(defaultValue = "1156") int points) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        CourseCreateRequestDto request = new CourseCreateRequestDto(
                "측정" + suffix, "끝" + suffix,
                new BytesMultipartFile("gpxFile", "course.gpx", gpx(points)),
                new BytesMultipartFile("thumbnailImage", "thumbnail.webp", new byte[1024]),
                false);

        long start = System.nanoTime();
        Long courseId = courseCreationService.createMemberCourse(memberId, request);
        double elapsedMs = (System.nanoTime() - start) / 1_000_000.0;

        return Map.of("courseId", courseId, "elapsedMs", elapsedMs);
    }

    /**
     * 해운대해수욕장에서 북동쪽으로 이어지는 측정용 GPX를 만듭니다.
     */
    private static byte[] gpx(int points) {
        StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><gpx><trk><trkseg>");
        for (int i = 0; i < points; i++) {
            sb.append(String.format("<trkpt lat=\"%.6f\" lon=\"%.6f\"><ele>%d</ele></trkpt>",
                    35.1587 + i * 0.0001, 129.1604 + i * 0.0001, 10 + i % 50));
        }
        return sb.append("</trkseg></trk></gpx>").toString().getBytes(StandardCharsets.UTF_8);
    }

    private record BytesMultipartFile(String name, String originalFilename, byte[] bytes) implements MultipartFile {

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getOriginalFilename() {
            return originalFilename;
        }

        @Override
        public String getContentType() {
            return "application/octet-stream";
        }

        @Override
        public boolean isEmpty() {
            return bytes.length == 0;
        }

        @Override
        public long getSize() {
            return bytes.length;
        }

        @Override
        public byte[] getBytes() {
            return bytes;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(bytes);
        }

        @Override
        public void transferTo(File dest) throws java.io.IOException {
            Files.write(dest.toPath(), bytes);
        }
    }
}
