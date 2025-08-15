package com.server.running_handai.domain.course.service;

import com.server.running_handai.global.response.ResponseCode;
import com.server.running_handai.global.response.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
public class FileService {
    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${spring.cloud.aws.region.static}")
    private String region;

    @Value("${spring.cloud.aws.credentials.access-key}")
    private String accessKey;

    @Value("${spring.cloud.aws.credentials.secret-key}")
    private String secretKey;

    private final S3Client s3Client;

    public FileService(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    private static final String FILENAME_PATTERN = "[^A-Za-z0-9_-]";

    /**
     * MultipartFile을 S3 버킷에 업로드하고, 업로드된 파일의 URL을 반환합니다.
     * 파일에 따라 디렉토리로 구분하여 저장합니다. (예: gpx, image)
     *
     * @param multipartFile 업로드할 파일
     * @param directory S3 버킷 내 디렉토리
     * @return 업로드된 파일의 S3 URL
     */
    public String uploadFile(MultipartFile multipartFile, String directory) {
        String originalFileName = multipartFile.getOriginalFilename();

        if (originalFileName == null || originalFileName.isBlank()) {
            log.warn("[S3 파일 업로드] 파일명을 찾을 수 없어 기본값 제공");
            originalFileName = "file";
        }

        String contentType = guessContentType(originalFileName);
        validateFileType(originalFileName);

        String newFileName = changeFileName(originalFileName);
        String fileName = directory + "/" + UUID.randomUUID() + "_" + newFileName;

        try {
            return uploadToS3(fileName, contentType, multipartFile.getInputStream(), multipartFile.getSize());
        } catch (IOException e) {
            log.error("[S3 파일 업로드] 업로드 실패: 파일명={}, 대상경로={}", originalFileName, fileName, e);
            throw new BusinessException(ResponseCode.FILE_UPLOAD_FAILED);
        }
    }

    /**
     * 이미지 URL을 통해 파일을 S3 버킷에 업로드하고, 업로드된 파일의 URL을 반환합니다.
     * 파일에 따라 디렉토리로 구분하여 저장합니다. (예: gpx, image)
     *
     * @param fileUrl 이미지 URL
     * @param directory S3 버킷 내 디렉토리
     * @return 업로드된 파일의 S3 URL
     */
    public String uploadFileByUrl(String fileUrl, String directory) {
        try {
            URL url = new URL(fileUrl);
            String path = url.getPath();
            String originalFileName = path.substring(path.lastIndexOf('/') + 1);

            if (originalFileName.isBlank()) {
                log.warn("[S3 파일 업로드] 파일명을 찾을 수 없어 기본값 제공");
                originalFileName = "file";
            }

            String contentType = guessContentType(originalFileName);
            validateFileType(originalFileName);

            String newFileName = changeFileName(originalFileName);
            String fileName = directory + "/" + UUID.randomUUID() + "_" + newFileName;

            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setRequestMethod("GET");
            httpURLConnection.connect();

            try (InputStream inputStream = httpURLConnection.getInputStream()) {
                return uploadToS3(fileName, contentType, inputStream, httpURLConnection.getContentLengthLong());
            }
        } catch (IOException e) {
            log.error("[S3 파일 업로드] 업로드 실패: fileUrl={}, error={}", fileUrl, e.getMessage(), e);
            throw new BusinessException(ResponseCode.FILE_UPLOAD_FAILED);
        }
    }

    /**
     * S3 버킷에 저장된 파일의 Presigned GET URL을 발급합니다.
     * URL은 지정한 유효 시간 동안만 접근 가능합니다.
     *
     * @param fileUrl DB에 저장된 S3 파일 URL
     * @param minutes Presigned URL 유효 시간
     * @return Presigned GET URL
     */
    public String getPresignedGetUrl(String fileUrl, int minutes) {
        Duration duration = Duration.ofMinutes(minutes);
        String key = extractKeyFromUrl(fileUrl);

        try (S3Presigner s3Presigner = S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(accessKey, secretKey)
                        )
                )
                .build()) {

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            GetObjectPresignRequest getObjectPresignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(duration)
                    .getObjectRequest(getObjectRequest)
                    .build();

            URL presignedUrl = s3Presigner.presignGetObject(getObjectPresignRequest).url();
            log.info("[S3 presigned URL 발급] 성공: key={}, url={}", key, presignedUrl);
            return presignedUrl.toString();
        } catch (Exception e) {
            log.error("[S3 presigned URL 발급] 실패: key={}, duration={}분", key, minutes, e);
            throw new BusinessException(ResponseCode.PRESIGEND_URL_FAILED);
        }
    }

    /**
     * S3 버킷에 업로드된 파일을 삭제합니다.
     *
     * @param fileUrl DB에 저장된 S3 파일 URL
     */
    public void deleteFile(String fileUrl) {
        String key = extractKeyFromUrl(fileUrl);

        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
        } catch (Exception e) {
            log.error("[S3 파일 삭제] 삭제 실패: key={}", key);
            throw new BusinessException(ResponseCode.FILE_DELETE_FAILED);
        }
    }

    /**
     * S3 파일 URL에서 key를 추출합니다.
     *
     * @param fileUrl DB에 저장된 S3 파일 URL
     * @return S3 내부 Key 경로
     */
    private String extractKeyFromUrl(String fileUrl) {
        int index = fileUrl.indexOf(".amazonaws.com/");

        // fileUrl에 ".amazonaws.com/"이 없으면 -1을 반환
        if (index == -1) {
            throw new IllegalArgumentException("잘못된 S3 파일 URL 형식입니다: " + fileUrl);
        }

        return fileUrl.substring(index + ".amazonaws.com/".length());
    }

    /**
     * 파일 이름에서 Content Type을 추정합니다.
     * 확장자가 없거나 인식 불가할 경우 기본값으로 application/octet-stream을 반환합니다.
     *
     * @param filename 파일 이름
     * @return Content Type
     */
    private String guessContentType(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        String extension = (dotIndex == -1) ? "" : filename.substring(dotIndex + 1).toLowerCase();
        return switch (extension) {
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "gpx" -> "application/gpx+xml";
            default -> {
                log.warn("[S3 파일 업로드] 감지하지 못한 content-type: extension={}", extension);
                yield "application/octet-stream";
            }
        };
    }

    /**
     * 주어진 Content Type이 지원되는 타입인지 확인합니다.
     *
     * @param fileName 파일 이름
     */
    private void validateFileType(String fileName) {
        String lowerName = fileName.toLowerCase();

        boolean isSupported = lowerName.endsWith(".png") || lowerName.endsWith(".jpg") ||
                lowerName.endsWith(".jpeg") || (lowerName.endsWith(".gpx"));

        if (!isSupported) {
            throw new BusinessException(ResponseCode.UNSUPPORTED_FILE_TYPE);
        }
    }

    /**
     * 저장 시 UTF-8 인코딩이 필요없는 영문으로 파일명을 바꿉니다.
     * 확장자는 보존하고, 파일명이 비어있으면 기본값(file)을 사용합니다.
     *
     * @param originalFileName 원본 파일명
     * @return 허용된 문자로 이루어진 파일명
     */
    private String changeFileName(String originalFileName) {
        // 확장자 분리
        int dotIndex = originalFileName.lastIndexOf('.');
        String name = (dotIndex == -1) ? originalFileName : originalFileName.substring(0, dotIndex);
        String extension = (dotIndex == -1) ? "" : originalFileName.substring(dotIndex).toLowerCase();

        // 영문, 숫자, 하이픈, 언더스코어만 허용
        String newFileName = name.replaceAll(FILENAME_PATTERN, "");

        // 원본 파일명에 허용된 문자가 없어 빈 파일명일 경우 기본값 사용
        if (newFileName.isBlank()) {
            log.warn("[S3 파일 업로드] 원본 파일명에 허용된 문자가 없어 기본값 사용: originalFilName={}", originalFileName);
            newFileName = "file";
        }

        return newFileName + extension;
    }

    /**
     * InputStream을 받아 S3에 업로드하고 업로드된 파일 URL을 반환합니다.
     *
     * @param fileName S3에 저장할 파일 이름
     * @param contentType 파일의 Content Type
     * @param inputStream 업로드할 파일 데이터 스트림
     * @param contentLength 업로드 데이터 크기 (byte)
     * @return 업로드된 파일의 S3 URL
     */
    private String uploadToS3(String fileName, String contentType, InputStream inputStream, long contentLength) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(fileName)
                .contentType(contentType)
                .build();

        s3Client.putObject(
                putObjectRequest,
                software.amazon.awssdk.core.sync.RequestBody.fromInputStream(inputStream, contentLength)
        );

        return String.format(
                "https://%s.s3.%s.amazonaws.com/%s",
                bucket,
                region,
                fileName
        );
    }
}
