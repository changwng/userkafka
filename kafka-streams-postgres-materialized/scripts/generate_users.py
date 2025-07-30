import sys
import time
import requests

# 사용자 수
#USER_COUNT = 500_000
USER_COUNT_SRT = 5001
USER_COUNT = 10000

API_URL = 'http://localhost:8081/api/users'


def send_user(name, email, department, status):
    headers = {"Content-Type": "application/json"}
    data = {
        "name": name,
        "email": email,
        "department": department,
        "status": status
    }
    response = requests.post(API_URL, json=data, headers=headers)
    return response.status_code, response.text


def main():
    start_time = time.time()
    for i in range(USER_COUNT_SRT, USER_COUNT + 1):
        name = f'사용자{i}'
        email = f'user{i}@example.com'
        department = '개발팀'
        status = 'ACTIVE'
        status_code, resp_text = send_user(name, email, department, status)
        if i % 1000 == 0:
            elapsed = time.time() - start_time
            print(f"{i}명 생성 완료 (경과 시간: {elapsed:.2f}초, 마지막 응답: {status_code})")
    end_time = time.time()
    total_time = end_time - start_time
    print(f'총 {USER_COUNT}명 사용자 생성 완료. 전체 소요 시간: {total_time:.2f}초')


if __name__ == "__main__":
    main() 