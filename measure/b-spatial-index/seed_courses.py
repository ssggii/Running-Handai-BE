"""
B(반경 검색) 측정용 코스 시드 CSV 생성기.

가정(docs/개선작업/공통-0단계-가정.md):
- 전국 확장, 코스는 시·도 인구 비율로 분포하고 도심 주변에 몰린다
- 부산 코스 중 15.8%는 해운대해수욕장 반경 5km 안에 있다

출력:
- current : 57개. 운영과 같은 분포 (전부 부산, 9개가 해운대 반경 5km 안)
- part1   : course_id 1 ~ 42,000          (누적 4.2만 개)
- part2   : course_id 42,001 ~ 420,000    (누적 42만 개)
- part3   : course_id 420,001 ~ 4,200,000 (누적 420만 개)

난수 시드가 고정이라 몇 번을 돌려도 같은 데이터가 나온다 (개선 전후를 같은 데이터로 재기 위해서다).
CSV 컬럼: course_id,name,distance,duration,max_ele,min_ele,level,lat,lon

사용법: python3 -I seed_courses.py <출력 디렉터리>
"""
import csv
import math
import os
import random
import sys

SEED = 20261011
HAEUNDAE = (35.1587, 129.1604)  # 해운대해수욕장
RADIUS_KM = 5.0
HAEUNDAE_SHARE_IN_BUSAN = 9 / 57  # 운영 실측 15.8%

# 시·도별 인구(만 명, 주민등록 기준 근사)와 도심 중심 좌표. 중심마다 정규분포로 흩뿌린다
REGIONS = {
    "서울": (935, 4, [(37.5665, 126.9780), (37.4979, 127.0276), (37.5133, 127.1001), (37.5219, 126.9245), (37.6542, 127.0568)]),
    "부산": (325, None, None),  # 아래 BUSAN_CENTERS로 따로 처리
    "대구": (236, 6, [(35.8714, 128.6014)]),
    "인천": (302, 6, [(37.4563, 126.7052), (37.3826, 126.6562)]),
    "광주": (141, 5, [(35.1595, 126.8526)]),
    "대전": (144, 5, [(36.3504, 127.3845)]),
    "울산": (110, 6, [(35.5384, 129.3114)]),
    "세종": (39, 4, [(36.4800, 127.2890)]),
    "경기": (1370, 5, [(37.2636, 127.0286), (37.4200, 127.1267), (37.6584, 126.8320), (37.2411, 127.1776), (37.5034, 126.7660),
                      (37.3219, 126.8309), (37.1995, 126.8312), (37.6360, 127.2165), (36.9921, 127.1129), (37.7381, 127.0337)]),
    "강원": (151, 5, [(37.8813, 127.7298), (37.3422, 127.9202), (37.7519, 128.8761)]),
    "충북": (159, 5, [(36.6424, 127.4890), (36.9910, 127.9259)]),
    "충남": (213, 5, [(36.8151, 127.1139), (36.7898, 127.0019), (36.7848, 126.4503)]),
    "전북": (172, 5, [(35.8242, 127.1480), (35.9483, 126.9577), (35.9676, 126.7366)]),
    "전남": (178, 5, [(34.7604, 127.6622), (34.9506, 127.4872), (34.8118, 126.3922)]),
    "경북": (253, 5, [(36.0190, 129.3435), (36.1195, 128.3446), (35.8562, 129.2247), (36.5684, 128.7294)]),
    "경남": (322, 5, [(35.2280, 128.6811), (35.2285, 128.8894), (35.1800, 128.1076), (35.3350, 129.0373)]),
    "제주": (67, 5, [(33.4996, 126.5312), (33.2541, 126.5600)]),
}
# 부산에서 해운대 반경 밖 코스가 몰리는 곳: 서면, 남포, 다대포, 부산대, 기장, 명지, 사상, 동래
BUSAN_CENTERS = [(35.1578, 129.0600), (35.0980, 129.0300), (35.0466, 128.9663), (35.2318, 129.0838),
                 (35.2445, 129.2222), (35.0965, 128.9216), (35.1528, 128.9910), (35.2050, 129.0786)]
BUSAN_SIGMA_KM = 3

PARTS = [("part1", 1, 42_000), ("part2", 42_001, 420_000), ("part3", 420_001, 4_200_000)]
LEVELS = ["EASY", "MEDIUM", "HARD"]


def haversine_km(a, b):
    lat1, lon1, lat2, lon2 = map(math.radians, (*a, *b))
    h = math.sin((lat2 - lat1) / 2) ** 2 + math.cos(lat1) * math.cos(lat2) * math.sin((lon2 - lon1) / 2) ** 2
    return 2 * 6371 * math.asin(math.sqrt(h))


def offset(center, dx_km, dy_km):
    lat, lon = center
    return lat + dy_km / 111.0, lon + dx_km / (111.0 * math.cos(math.radians(lat)))


def around(rng, center, sigma_km):
    return offset(center, rng.gauss(0, sigma_km), rng.gauss(0, sigma_km))


def in_haeundae_radius(rng):
    # 반경 안에서 면적 기준으로 고르게
    r = RADIUS_KM * 0.999 * math.sqrt(rng.random())
    t = rng.uniform(0, 2 * math.pi)
    return offset(HAEUNDAE, r * math.cos(t), r * math.sin(t))


def busan_outside_radius(rng):
    while True:
        p = around(rng, rng.choice(BUSAN_CENTERS), BUSAN_SIGMA_KM)
        if haversine_km(p, HAEUNDAE) > RADIUS_KM:
            return p


def busan_point(rng):
    return in_haeundae_radius(rng) if rng.random() < HAEUNDAE_SHARE_IN_BUSAN else busan_outside_radius(rng)


def national_point(rng, names, weights):
    region = rng.choices(names, weights)[0]
    if region == "부산":
        return busan_point(rng)
    _, sigma, centers = REGIONS[region]
    return around(rng, rng.choice(centers), sigma)


def row(rng, course_id, point):
    distance = round(rng.uniform(3, 25), 3)
    duration = round(distance / 9 * 60)
    min_ele = round(rng.uniform(0, 50), 1)
    max_ele = round(min_ele + rng.uniform(5, 300), 1)
    return [course_id, f"seed-{course_id:07d}", distance, duration, max_ele, min_ele, rng.choice(LEVELS),
            f"{point[0]:.7f}", f"{point[1]:.7f}"]


def write(path, rows):
    with open(path, "w", newline="") as f:
        csv.writer(f).writerows(rows)


def main(out_dir):
    os.makedirs(out_dir, exist_ok=True)

    # 현재 운영 규모: 57개, 전부 부산, 9개가 반경 안
    rng = random.Random(SEED)
    current = [row(rng, i, in_haeundae_radius(rng) if i <= 9 else busan_outside_radius(rng)) for i in range(1, 58)]
    write(os.path.join(out_dir, "current.csv"), current)
    print(f"current: 57 rows, in-radius 9")

    # 전국 규모: 4.2만 → 42만 → 420만 (누적)
    rng = random.Random(SEED + 1)
    names = list(REGIONS)
    weights = [REGIONS[n][0] for n in names]
    total_in_radius = 0
    for name, start, end in PARTS:
        rows, in_radius = [], 0
        for course_id in range(start, end + 1):
            p = national_point(rng, names, weights)
            in_radius += haversine_km(p, HAEUNDAE) <= RADIUS_KM
            rows.append(row(rng, course_id, p))
        write(os.path.join(out_dir, f"{name}.csv"), rows)
        total_in_radius += in_radius
        print(f"{name}: ids {start}~{end}, cumulative {end:,} rows, cumulative in-radius (haversine) {total_in_radius:,}")


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else "data")
