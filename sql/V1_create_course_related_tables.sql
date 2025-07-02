-- Version 1: 프로젝트 초기 테이블 생성
-- 1. course 테이블 생성
CREATE TABLE course
(
    course_id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    created_at     DATETIME(6)                     NOT NULL,
    updated_at     DATETIME(6)                     NOT NULL,
    distance       INT                             NOT NULL,
    duration       INT                             NOT NULL,
    district       VARCHAR(255)                    NOT NULL,
    external_id    VARCHAR(255)                    NULL,
    name           VARCHAR(255)                    NOT NULL,
    tour_point     TEXT                            NULL,
    level          ENUM ('EASY', 'HARD', 'MEDIUM') NOT NULL,
    CONSTRAINT UK_course_external_id UNIQUE (external_id)
);


-- 2. course_image 테이블 생성
CREATE TABLE course_image
(
    course_img_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id     BIGINT       NOT NULL,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    url           VARCHAR(255) NOT NULL,
    CONSTRAINT UK_course_image_course_id UNIQUE (course_id),
    CONSTRAINT FK_course_image_to_course FOREIGN KEY (course_id) REFERENCES course (course_id)
);


-- 3. track_point 테이블 생성
CREATE TABLE track_point
(
    track_point_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id      BIGINT      NOT NULL,
    created_at     DATETIME(6) NOT NULL,
    updated_at     DATETIME(6) NOT NULL,
    lat            DOUBLE      NOT NULL,
    lon            DOUBLE      NOT NULL,
    ele            DOUBLE      NULL,
    sequence       INT         NULL,
    CONSTRAINT FK_track_point_to_course FOREIGN KEY (course_id) REFERENCES course (course_id)
);

-- 4. road_condition 테이블 생성
CREATE TABLE road_condition
(
    road_condition_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id         BIGINT      NOT NULL,
    created_at        DATETIME(6) NOT NULL,
    updated_at        DATETIME(6) NOT NULL,
    description       TEXT        NOT NULL,
    CONSTRAINT FK_road_condition_to_course FOREIGN KEY (course_id) REFERENCES course (course_id)
);