import mysql.connector
import pandas as pd
import os

# MySQL 데이터베이스 연결 설정 (db_config는 기존 코드와 동일)
db_config = {
    'user': 'greenway',  # MySQL 사용자 이름
    'password': 'greenway',  # MySQL 비밀번호
    'host': 'localhost',  # MySQL 서버 주소
    'port': 3306,  # MySQL 포트
    'database': 'parking',  # 데이터베이스 이름
    'raise_on_warnings': True
}

# 데이터를 MySQL에서 불러와 파일로 저장하는 함수
def export_data_to_csv(batch_size=5000):
    conn = mysql.connector.connect(**db_config)
    cursor = conn.cursor()

    # 저장할 컬럼만 선택 (PKLT_NM, TPKCT, NOW_PRK_VHCL_CNT, timestamp, weekday)
    select_query = """
    SELECT PKLT_NM, TPKCT, NOW_PRK_VHCL_CNT, timestamp, weekday
    FROM parking_data
    LIMIT %s OFFSET %s
    """
    
    # 전체 데이터 개수 확인
    cursor.execute("SELECT COUNT(*) FROM parking_data")
    total_count = cursor.fetchone()[0]
    
    # 데이터를 몇 개의 파일로 저장할지 결정
    num_batches = (total_count // batch_size) + 1
    print(f"총 {total_count}개의 데이터를 {num_batches}개의 파일로 나눠 저장합니다.")

    # 배치 단위로 데이터를 CSV 파일에 저장
    for batch_number in range(num_batches):
        offset = batch_number * batch_size
        cursor.execute(select_query, (batch_size, offset))
        rows = cursor.fetchall()

        # DataFrame으로 변환
        df = pd.DataFrame(rows, columns=['PKLT_NM', 'TPKCT', 'NOW_PRK_VHCL_CNT', 'timestamp', 'weekday'])
        
        # 파일 이름 지정
        file_name = f"parking_data_batch_{batch_number + 1}.csv"
        
        # 파일로 저장
        df.to_csv(file_name, index=False)
        print(f"{file_name}에 데이터가 저장되었습니다.")

    cursor.close()
    conn.close()

# 실행
export_data_to_csv(batch_size=5000)
