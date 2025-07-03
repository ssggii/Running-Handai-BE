-- Version 1: 프로젝트 초기 테이블 생성 및 인덱스 설정

create table course
(
    course_id      bigint auto_increment
        primary key,
    created_at     datetime(6)                                                                                                                            not null,
    updated_at     datetime(6)                                                                                                                            not null,
    area           enum ('HAEUN_GWANGAN', 'NORTHERN_BUSAN', 'SEOMYEON_DONGNAE', 'SONGJEONG_GIJANG', 'SOUTHERN_COAST', 'WESTERN_NAKDONGRIVER', 'WONDOSIM') not null,
    distance       int                                                                                                                                    not null,
    duration       int                                                                                                                                    not null,
    external_id    varchar(255)                                                                                                                           null,
    gpx_path       varchar(255)                                                                                                                           not null,
    level          enum ('EASY', 'HARD', 'MEDIUM')                                                                                                        not null,
    max_ele        double                                                                                                                                 null,
    min_ele        double                                                                                                                                 null,
    name           varchar(255)                                                                                                                           not null,                                                                                                                                 null,
    start_point    point                                                                                                                                  not null,
    tour_point     text                                                                                                                                   null,
    constraint UKftj9sywcqetdlrcts15h17nx3
        unique (external_id)
);

create spatial index idx_course_location on course (start_point); -- 공간 인덱스 생성

-- 2. course_image 테이블 생성
CREATE TABLE course_image
(
    course_img_id bigint auto_increment
        primary key,
    created_at    datetime(6)  not null,
    updated_at    datetime(6)  not null,
    img_url       varchar(255) not null,
    course_id     bigint       not null,
    constraint UKti4s11n8wee7ym2yndccdbms2
        unique (course_id),
    constraint FK5uweik1v6wv796ggohfekp7wb
        foreign key (course_id) references course (course_id)
);


-- 3. track_point 테이블 생성
CREATE TABLE track_point
(
    track_point_id bigint auto_increment
        primary key,
    created_at     datetime(6) not null,
    updated_at     datetime(6) not null,
    ele            double      not null,
    lat            double      not null,
    lon            double      not null,
    sequence       int         not null,
    course_id      bigint      not null,
    constraint FKpupqiw5q83q159swqraqw9hpm
        foreign key (course_id) references course (course_id)
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