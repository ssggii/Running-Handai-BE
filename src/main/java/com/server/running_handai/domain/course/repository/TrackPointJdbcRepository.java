package com.server.running_handai.domain.course.repository;

import com.server.running_handai.domain.course.entity.TrackPoint;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 트랙포인트를 JDBC 배치로 한 번에 저장합니다.
 * TrackPoint는 IDENTITY 전략이라 JPA로 저장하면 행마다 INSERT가 즉시 실행되어 배치가 되지 않으므로, JPA를 거치지 않고 저장합니다.
 * 드라이버 옵션 rewriteBatchedStatements=true와 함께 쓰면 묶음마다 여러 행짜리 INSERT 한 문장으로 전송됩니다.
 */
@Repository
@RequiredArgsConstructor
public class TrackPointJdbcRepository {

    private static final int BATCH_SIZE = 1000;

    private static final String INSERT_SQL =
            "INSERT INTO track_point (course_id, lat, lon, ele, sequence, created_at, updated_at) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?)";

    private final JdbcTemplate jdbcTemplate;

    /**
     * 트랙포인트를 코스에 연결해 저장합니다. JPA를 거치지 않으므로 created_at, updated_at을 직접 채웁니다.
     *
     * @param courseId 트랙포인트를 연결할 코스 ID
     * @param trackPoints 저장할 트랙포인트 목록
     */
    public void batchInsert(Long courseId, List<TrackPoint> trackPoints) {
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        jdbcTemplate.batchUpdate(INSERT_SQL, trackPoints, BATCH_SIZE, (ps, trackPoint) -> {
            ps.setLong(1, courseId);
            ps.setDouble(2, trackPoint.getLat());
            ps.setDouble(3, trackPoint.getLon());
            ps.setDouble(4, trackPoint.getEle());
            ps.setInt(5, trackPoint.getSequence());
            ps.setTimestamp(6, now);
            ps.setTimestamp(7, now);
        });
    }
}
