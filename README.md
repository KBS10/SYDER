# SYDER_Android
2020/05/02
-------------
* DB에서 데이터 가져와서 적용 -> volley실행 순서로 인한 오류
* volley실행 순서로 인한 오류 수정

2020/05/03 
-------------
* logout추가 
* DB변경에 따른 코드 수정

2020/05/04
-------------
* 마커 이미지 차로 수정 및 추가 png파일 추가 
* 비트이미지로 변경하여 필요한 크기로 조정

2020/05/05
-------------
* 마커모델 생성으로 동적으로 마커 생성 
* 마커 클릭 이벤트 처리 조정 및 ui제작

2020/05/07
-------------
* sendActivity 수정
* 주문 출발지 도착지 지정 및 레이아웃 표시
* activity_main 레이아웃 위치조정

2020/05/09
-------------
* sendActivity -> 현 volley의 실행순서 문제
* 주문 출발지 도착지 지정 및 삭제 제한, 마커 커스텀 
* sendActivity.xml 수정

2020/05/10
-------------
* 주문 출발지 도착지 지정시 나머지 마커 삭제 구현
* MarkerModel 생성자 추가
* 구현된 마커와 레이아웃 가시성 동적으로 변경
* volley 콜백 구현 중

2020/05/12
-------------
* socket.io를 이용한 소켓통신 틀 구성
* activity_send의 Handler로 volley 오류 수정

2020/05/13
-------------
* activity_send의 Handler -> callback 인자로 수정

2020/05/14
-------------
* 스토리보드에 따른 레이아웃 생성 -> 하나의 화면에 조건을 부여하여 몰아서 할지 여러개로 나눌지 고민중

2020/05/16
-------------
* 스토리보드에 따른 레이아웃 생성 -> 하나의 화면에 몰아서 하기로 -> 조건은 아직
* activity_send -- volley를 이용해 등록

2020/05/18
-------------
* FCM토큰 테스트 

2020/05/19
--------------
* refactoring -> xml_id, class_id
* QR코드 테스트

2020/05/20
--------------
* FCM 토큰 프로젝트에 적용 -> 실패

2020/05/21
---------------
* api orders/check, routes 적용
* 스토리보드에 나온 xml 완성

2020/05/23
---------------
* api orders/show 적용 -> 상황에 따른 동작 구현
* api orders/check 알고리즘 수정 -> 알림 시 알고리즘 

2020/05/24
---------------
* api orderCheck 적용
* RouteModel 생성
* api order/register 데이터 유동적으로 보내기
* Fcm token을 이용한 알림 적용 -> 아직 임의의 값만 

2020/05/25
----------------
* orders 오류 수정
* api consent req, res 적용 및 테스트완료

2020/05/26
----------------
* [happy ending]주문과정에 따른 api 테스트
* 수신자와 발신자 한기기에서 적용 됨 -> fcm을 이용해 나눠서 

2020/05/27
----------------
* fcm을 이용한 알림 테스트 -> 알림이 올 때도 있고 안 올 때도 있다.
* -> 문제점 : 알림 클릭시 앱이 실행을 안함 

2020/05/28
----------------
* fcm 알림 안 오는 오류 수정  
* 알림이 포그라운드에서만 클릭이벤트 실행-> 백그라운드에서는 이벤트 실행X

2020/05/30
-----------------
* 추가적인 알림들에 대한 이벤트 추가
* 소켓통신으로 차량위치 받아오기 -> 병합