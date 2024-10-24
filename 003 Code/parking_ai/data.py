import requests
import schedule
import time
import json
from datetime import datetime
from collections import defaultdict
import mysql.connector  # MySQL 연결을 위해 추가

# API URL과 인증키
API_KEY = "6f78465648636f643436687044576f"
API_URL = f"http://openapi.seoul.go.kr:8088/{API_KEY}/json/GetParkingInfo/"

# MySQL 데이터베이스 연결 설정
db_config = {
    'user': 'greenway',  # MySQL 사용자 이름
    'password': 'greenway',  # MySQL 비밀번호
    'host': 'localhost',  # MySQL 서버 주소 (포트 번호 제외)
    'port': 3306,  # MySQL 기본 포트 번호
    'database': 'parking',  # 사용할 데이터베이스 이름v
    'raise_on_warnings': True
}

# 주차 정보를 저장할 테이블 생성 (최초 한 번 실행 필요)
# def create_table_if_not_exists():
#     conn = mysql.connector.connect(**db_config)
#     cursor = conn.cursor()
#     cursor.execute("""
#         CREATE TABLE IF NOT EXISTS parking_data (
#             PKLT_CD VARCHAR(50),
#             PKLT_NM VARCHAR(255),
#             ADDR VARCHAR(255),
#             PRK_STTS_YN VARCHAR(5),
#             TPKCT INT,
#             NOW_PRK_VHCL_CNT INT,
#             NOW_PRK_VHCL_UPDT_TM VARCHAR(255),
#             timestamp DATETIME,
#             weekday VARCHAR(10),
#             PRIMARY KEY (PKLT_CD, timestamp)  -- PKLT_CD와 timestamp로 중복 방지
#         )
#     """)
#     conn.commit()
#     cursor.close()
#     conn.close()

# 주차 데이터를 MySQL에 삽입하는 함수
def save_to_mysql(data):
    conn = mysql.connector.connect(**db_config)
    cursor = conn.cursor()

    # INSERT 구문에 별칭을 추가하여 수정
    insert_query = """
    INSERT INTO parking_data (PKLT_CD, PKLT_NM, ADDR, PRK_STTS_YN, TPKCT, NOW_PRK_VHCL_CNT, NOW_PRK_VHCL_UPDT_TM, timestamp, weekday)
    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s) AS new
    ON DUPLICATE KEY UPDATE
        NOW_PRK_VHCL_CNT = new.NOW_PRK_VHCL_CNT,
        NOW_PRK_VHCL_UPDT_TM = new.NOW_PRK_VHCL_UPDT_TM,
        TPKCT = new.TPKCT
    """  # TPKCT는 중복이 있을 경우 카운트한 값으로 업데이트

    for record in data:
        cursor.execute(insert_query, (
            record['PKLT_CD'],
            record['PKLT_NM'],
            record['ADDR'],
            record['PRK_STTS_YN'],
            record['TPKCT'],
            record['NOW_PRK_VHCL_CNT'],
            record['NOW_PRK_VHCL_UPDT_TM'],
            record['timestamp'],
            record['weekday']
        ))
    
    conn.commit()
    cursor.close()
    conn.close()

def fetch_parking_data():
    all_data = []
    
    # 데이터 범위를 유연하게 설정
    for start in range(1, 2001, 1000):  # 1~1000, 1001~2000
        end = start + 999
        url = f"{API_URL}{start}/{end}"
        response = requests.get(url)

        if response.status_code == 200:
            try:
                data = response.json()
                if "GetParkingInfo" in data and "row" in data["GetParkingInfo"]:
                    for record in data["GetParkingInfo"]["row"]:
                        # PRK_STTS_YN이 1인 것만 처리
                        if record.get('PRK_STTS_YN') == "1":
                            filtered_record = {
                                'PKLT_CD': record.get('PKLT_CD'),
                                'PKLT_NM': record.get('PKLT_NM'),
                                'ADDR': record.get('ADDR'),
                                'PRK_STTS_YN': record.get('PRK_STTS_YN'),
                                'TPKCT': record.get('TPKCT'),
                                'NOW_PRK_VHCL_CNT': record.get('NOW_PRK_VHCL_CNT'),
                                'NOW_PRK_VHCL_UPDT_TM': record.get('NOW_PRK_VHCL_UPDT_TM')
                            }
                            all_data.append(filtered_record)
            except json.JSONDecodeError:
                print(f"JSON 디코딩 오류 발생: {response.text}")
        else:
            print(f"API 호출 실패: {response.status_code}, 응답: {response.text}")
    
    # 주차장 코드 기준으로 데이터 정리
    pklt_cd_count = defaultdict(list)

    # 중복 항목을 찾아내기 위해 주차장 코드를 카운트하고 각 항목을 리스트에 저장
    for record in all_data:
        pklt_cd = record['PKLT_CD']
        pklt_cd_count[pklt_cd].append(record)

    consolidated_data = []

    # 주차장 코드가 중복된 경우 카운트에 따라 TPKCT 수정
    for pklt_cd, records in pklt_cd_count.items():
        if len(records) > 1:
            # 중복된 경우, 중복된 횟수로 TPKCT 값을 설정
            first_record = records[0]  # 중복된 주차장 코드 중 첫 번째만 유지
            first_record['TPKCT'] = len(records)  # 중복된 횟수를 TPKCT에 설정
            consolidated_data.append(first_record)
        else:
            # 중복이 없으면 TPKCT 값을 그대로 유지
            consolidated_data.append(records[0])

    # 현재 시각 및 요일 추가
    timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    weekday = datetime.now().strftime("%A")

    for record in consolidated_data:
        record['timestamp'] = timestamp
        record['weekday'] = weekday

    # 데이터 저장 (MySQL로)
    save_to_mysql(consolidated_data)

    print(f"{timestamp} - 데이터가 성공적으로 저장되었습니다. (요일: {weekday})")

# 테이블 생성 (최초 한 번만 실행)
# create_table_if_not_exists()

# 매 시간의 0분, 10분, 20분, 30분, 40분, 50분에 함수를 실행하도록 스케줄 설정
for minute in ['00', '10', '20', '30', '40', '50']:
    schedule.every().hour.at(f":{minute}").do(fetch_parking_data)

print("정확히 10분 단위 데이터를 수집하는 스케줄러가 시작되었습니다.")

# 스케줄러를 실행
while True:
    schedule.run_pending()
    time.sleep(1)