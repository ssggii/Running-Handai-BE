package com.server.running_handai.course.service;

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

    /**
     * MultipartFile을 S3 버킷에 업로드하고, 업로드된 파일의 URL을 반환합니다.
     * 파일에 따라 디렉토리로 구분하여 저장합니다. (예: gpx, image)
     *
     * @param multipartFile 업로드할 파일
     * @param directory S3 버킷 내 디렉토리
     * @return 업로드된 파일의 S3 URL
     */
    public String uploadFile(MultipartFile multipartFile, String directory) throws IOException {
        String originalFileName = multipartFile.getOriginalFilename();
        String fileName = directory + "/" + UUID.randomUUID() + "_" + originalFileName;
        String contentType = multipartFile.getContentType();

        String lowerName = fileName.toLowerCase();
        if (lowerName.endsWith(".png")) {
            contentType = "image/png";
        } else if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) {
            contentType = "image/jpeg";
        } else if (lowerName.endsWith(".gpx")) {
            contentType = "application/gpx+xml";
        } else {
            throw new IllegalArgumentException("지원하지 않는 파일 형식입니다.");
        }

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(fileName)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(
                    putObjectRequest,
                    software.amazon.awssdk.core.sync.RequestBody.fromInputStream(multipartFile.getInputStream(), multipartFile.getSize())
            );

            String fileUrl = String.format(
                    "https://%s.s3.%s.amazonaws.com/%s",
                    bucket,
                    region,
                    fileName
            );
            return fileUrl;
        } catch (IOException e) {
            log.error("[S3 파일 업로드] 업로드 실패: 파일명={}, 대상경로={}", originalFileName, fileName, e);
            throw new BusinessException(ResponseCode.FILE_UPLOAD_FAILED);
        }
    }

    /**
     * S3 버킷에 저장된 파일의 Presigned GET URL을 발급합니다.
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
     */
    private String extractKeyFromUrl(String fileUrl) {
        int index = fileUrl.indexOf(".amazonaws.com/");

        // fileUrl에 ".amazonaws.com/"이 없으면 -1을 반환
        if (index == -1) {
            throw new IllegalArgumentException("잘못된 S3 파일 URL 형식입니다: " + fileUrl);
        }

        return fileUrl.substring(index + ".amazonaws.com/".length());
    }
}
