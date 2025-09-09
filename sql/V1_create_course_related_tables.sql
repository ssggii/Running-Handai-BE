-- Version 1: 프로젝트 초기 테이블 생성 및 인덱스 설정

-- 1. course 테이블 생성
create table course
(
    distance    int                                                                                                                                           not null,
    duration    int                                                                                                                                           not null,
    max_ele     double                                                                                                                                        not null,
    min_ele     double                                                                                                                                        not null,
    course_id   bigint auto_increment
        primary key,
    created_at  datetime(6)                                                                                                                                   not null,
    updated_at  datetime(6)                                                                                                                                   not null,
    external_id varchar(255)                                                                                                                                  null,
    gpx_path    varchar(255)                                                                                                                                  not null,
    name        varchar(255)                                                                                                                                  not null,
    tour_point  text                                                                                                                                          null,
    area        enum ('HAEUN_GWANGAN', 'NORTHERN_BUSAN', 'SEOMYEON_DONGNAE', 'SONGJEONG_GIJANG', 'SOUTHERN_COAST', 'ETC', 'WESTERN_NAKDONGRIVER', 'WONDOSIM') not null,
    level       enum ('EASY', 'HARD', 'MEDIUM')                                                                                                               not null,
    start_point point                                                                                                                                         not null,
    constraint UK4xqvdpkafb91tt3hsb67ga3fj
        unique (name),
    constraint UKftj9sywcqetdlrcts15h17nx3
        unique (external_id)
);

create spatial index idx_course_location on course (start_point); -- 공간 인덱스 생성

-- 2. course_image 테이블 생성
create table course_image
(
    course_id     bigint       not null,
    course_img_id bigint auto_increment
        primary key,
    created_at    datetime(6)  not null,
    updated_at    datetime(6)  not null,
    img_url       varchar(255) not null,
    constraint UKti4s11n8wee7ym2yndccdbms2
        unique (course_id),
    constraint FK5uweik1v6wv796ggohfekp7wb
        foreign key (course_id) references course (course_id)
);

-- 3. course_themes 테이블 생성
create table course_themes
(
    course_course_id bigint                                                   not null,
    theme            enum ('DOWNTOWN', 'MOUNTAIN', 'RIVERSIDE', 'SEA', 'ETC') null,
    constraint FKlmlrl4xgc258abdvsrfh9pvft
        foreign key (course_course_id) references course (course_id)
);

-- 4. member 테이블 생성
create table member
(
    created_at    datetime(6)                       not null,
    member_id     bigint auto_increment
        primary key,
    updated_at    datetime(6)                       not null,
    nickname      varchar(20)                       not null,
    email         varchar(100)                      not null,
    provider_id   varchar(255)                      not null,
    refresh_token varchar(255)                      null,
    provider      enum ('GOOGLE', 'KAKAO', 'NAVER') not null,
    role          enum ('ADMIN', 'USER')            not null,
    constraint UKhh9kg6jti4n1eoiertn2k6qsc
        unique (nickname),
    constraint UKlxi0241cqql7g0knl5ssy59ij
        unique (provider, provider_id)
);

-- 5. bookmark 테이블 생성
create table bookmark
(
    course_id  bigint      not null,
    created_at datetime(6) not null,
    id         bigint auto_increment
        primary key,
    member_id  bigint      not null,
    updated_at datetime(6) not null,
    constraint bookmark_uk
        unique (member_id, course_id),
    constraint FK5bm7rup91j277mc7gg63akie2
        foreign key (member_id) references member (member_id),
    constraint FKt84bki61yn6g2g7k38hjclkxk
        foreign key (course_id) references course (course_id)
);

-- 6. road_condition 테이블 생성
create table road_condition
(
    course_id         bigint      null,
    created_at        datetime(6) not null,
    road_condition_id bigint auto_increment
        primary key,
    updated_at        datetime(6) not null,
    description       text        null,
    constraint FKsha81pvm99jy3b1lv7guo6dtx
        foreign key (course_id) references course (course_id)
);

-- 7. track_point 테이블 생성
create table track_point
(
    ele            double      not null,
    lat            double      not null,
    lon            double      not null,
    sequence       int         not null,
    course_id      bigint      not null,
    created_at     datetime(6) not null,
    track_point_id bigint auto_increment
        primary key,
    updated_at     datetime(6) not null,
    constraint FKpupqiw5q83q159swqraqw9hpm
        foreign key (course_id) references course (course_id)
);

-- 8. spot 테이블 생성
create table spot (
    spot_id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    external_id  VARCHAR(255) NOT NULL UNIQUE,
    name         VARCHAR(255) NOT NULL,
    address      VARCHAR(255) NOT NULL,
    description  TEXT,
    category     ENUM('NATURE', 'HISTORY', 'RECREATION', 'EXPERIENCE', 'INDUSTRIAL', 'ARCHITECTURE',
                   'KOREAN_FOOD', 'WESTERN_FOOD', 'JAPANESE_FOOD', 'CHINESE_FOOD', 'GLOBAL_FOOD',
                   'CAFE', 'CLUB', 'ETC') NOT NULL,
    lat          DOUBLE NOT NULL,
    lon          DOUBLE NOT NULL,
    created_at   DATETIME(6) NOT NULL,
    updated_at   DATETIME(6) NOT NULL
);

-- 9. course_spot 테이블 생성
CREATE TABLE course_spot (
    course_spot_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id      BIGINT NOT NULL,
    spot_id        BIGINT NOT NULL,
    created_at     DATETIME(6) NOT NULL,
    updated_at     DATETIME(6) NOT NULL,
    CONSTRAINT course_spot_uk UNIQUE (course_id, spot_id),
    CONSTRAINT fk_course FOREIGN KEY (course_id) REFERENCES course(course_id),
    CONSTRAINT fk_spot FOREIGN KEY (spot_id) REFERENCES spot(spot_id)
);

-- 10. spot_image 테이블 생성
CREATE TABLE spot_image (
    spot_img_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    img_url       VARCHAR(255) NOT NULL,
    original_url  VARCHAR(255) NOT NULL,
    spot_id       BIGINT NOT NULL UNIQUE,
    created_at    DATETIME(6) NOT NULL,
    updated_at    DATETIME(6) NOT NULL,
    CONSTRAINT fk_spot_image_spot FOREIGN KEY (spot_id) REFERENCES spot(spot_id)
);

-- 11. review 테이블 생성
create table review
(
    review_id  bigint auto_increment
        primary key,
    created_at datetime(6)   not null,
    updated_at datetime(6)   not null,
    contents   varchar(2000) not null,
    stars      double        not null,
    course_id  bigint        not null,
    member_id  bigint        null,
    constraint FKk0ccx5i4ci2wd70vegug074w1
        foreign key (member_id) references member (member_id)
            on delete set null,
    constraint FKprox8elgnr8u5wrq1983degk
        foreign key (course_id) references course (course_id)
            on delete cascade
);

-- course 테이블의 tour_point 컬럼 삭제
ALTER TABLE course DROP COLUMN tour_point;

-- course 테이블의 distance 컬럼 타입 변경
ALTER TABLE course MODIFY COLUMN distance DOUBLE NOT NULL;

-- course 테이블에 spot_status 컬럼 추가 및 코스별 초기화 상태 변경
ALTER TABLE course ADD COLUMN spot_status ENUM('COMPLETED', 'FAILED', 'IN_PROGRESS', 'NOT_APPLICABLE', 'NOT_STARTED') NOT NULL DEFAULT 'COMPLETED';
UPDATE course c SET c.spot_status = 'NOT_APPLICABLE' WHERE c.course_id = 85 -- 예시 (부산 외 코스)