# VocabLens ranking

Backend Python 3.13 chạy trên Cloud Functions (`asia-southeast1`), lưu bảng xếp hạng trong Cloud Firestore. Android tuân thủ MVVM + Clean Architecture; các use case và repository interface nằm trong `domain`, Firebase/Room/WorkManager nằm trong `data`.

Backend tách `domain/` (model, validation, quy tắc XP), `application/` (use case và interface ledger), `data/` (Firestore transaction). `main.py` nhận callable và ánh xạ lỗi nghiệp vụ sang lỗi Firebase. Tên callable `submitFlashcardSession`, request/response và dữ liệu Firestore giữ nguyên để tương thích Android.

`data/callable_codec.py` giải mã protobuf `Int64Value`/`UInt64Value` mà Firebase Android SDK dùng cho Kotlin `Long`, trước khi kiểm tra nghiệp vụ. Thời gian từ Android được chuẩn hóa về số nguyên như client web; giới hạn và luật XP vẫn được giữ nguyên.

## Luồng XP

1. Khi hoàn thành flashcard, Android lưu XP cá nhân và bản ghi `xp_sync` trong cùng transaction Room.
2. WorkManager gửi các phiên chờ theo thứ tự thời gian khi có mạng và đúng tài khoản đăng nhập. Đăng nhập, hoàn thành phiên hoặc nút đồng bộ đều có thể lên lịch gửi.
3. Callable `submitFlashcardSession` lấy UID từ Firebase Auth; `ownerId` trong request chỉ để đối chiếu chủ hàng đợi khi đổi tài khoản. Client không được gửi số XP.
4. Server kiểm tra phiên/review, tự tính 5 XP/thẻ và ghi biên nhận + tổng XP trong transaction. Một `sessionId` chỉ cộng một lần; retry dữ liệu khác bị từ chối. Các phiên trùng thời gian của cùng user bị từ chối, kể cả phiên được gửi từ thiết bị khác.
5. Android lắng nghe top 50 và tổng XP của chính mình. Cùng XP thì cùng hạng (1, 1, 3); thứ tự trong nhóm đồng điểm ổn định theo UID.

`leaderboard/{uid}` chỉ chứa `userId`, `displayName`, `totalXp`, `updatedAt`, không chứa email. Tên được lấy từ Firebase Auth. Biên nhận `xpAccounts/{uid}/sessions/{sessionId}` chỉ backend truy cập, lưu hash thay vì toàn bộ từ đã học.

XP cũ vẫn giữ trong Room cho thống kê cá nhân, không được tự động chuyển lên ranking. Migration 1 → 2 chỉ thêm outbox. XP ranking là tổng đã được server chấp nhận, có thể khác tổng XP cá nhân.

## Chạy kiểm thử trên máy

Cần Python 3.13, Node.js để chạy Firebase CLI, JDK 21+ và Android SDK. Backend deploy chỉ dùng Python; `tools/firebase/package.json` chứa dev tooling và test client JavaScript để kiểm tra callable/Firestore Rules qua SDK thực. Các lệnh sau chạy từ thư mục gốc `D:\LTDD`:

```powershell
py -3.13 -m venv functions/venv
.\functions\venv\Scripts\python.exe -m pip install -r functions/requirements.txt
npm.cmd --prefix tools/firebase ci
# Chạy unit test Python từ thư mục functions:
Push-Location functions
.\venv\Scripts\python.exe -m unittest discover -s tests -v
Pop-Location
npm.cmd --prefix tools/firebase run test:emulator
.\gradlew.bat testDebugUnitTest
# Có Android emulator hoặc thiết bị kết nối:
.\gradlew.bat connectedDebugAndroidTest
```

Nếu Java trên PATH cũ, đặt JAVA_HOME cho terminal hiện tại tới JDK 21+ trước khi chạy Firebase Emulator.

Không bắt buộc activate virtualenv trong PowerShell: gọi trực tiếp `venv\Scripts\python.exe` như trên. Trên Linux/macOS, dùng `python3.13 -m venv functions/venv` và `functions/venv/bin/python`.

`test:emulator` tự khởi động/dừng Auth, Functions, Firestore bằng project giả `demo-vocablens`. Test kiểm tra xác thực, đổi chủ phiên, gửi đồng thời/retry, phiên trùng thời gian, dữ liệu sai, Security Rules và cập nhật realtime.

Test client còn gửi timestamp dưới dạng protobuf Long của Android. Mỗi test tạo user riêng và chỉ dọn dữ liệu của user đó; không xóa toàn bộ Firestore Emulator.

## Chạy app với Firebase Emulator

Terminal 1, ở thư mục gốc:

```powershell
npm.cmd --prefix tools/firebase run emulators
```

Terminal 2, với Android emulator đang chạy:

```powershell
.\gradlew.bat installDebug "-PfirebaseEmulatorHost=10.0.2.2"
```

Mở VocabLens, đăng ký tài khoản thử, lưu từ rồi hoàn thành lượt ôn. Từ Trang chủ chọn **Bảng xếp hạng**. Để thử nhiều user, đăng xuất và đăng ký tài khoản khác.

Debug build có `firebaseEmulatorHost` sử dụng Firebase app riêng và project `demo-vocablens`, tách cache đăng nhập khỏi production. Production/release không kết nối emulator. Nếu chạy trên điện thoại thật qua USB, dùng `adb reverse tcp:9099 tcp:9099`, tương tự cho 8080 và 5001, rồi build với `-PfirebaseEmulatorHost=127.0.0.1`.

Kết thúc thử nghiệm, build lại không có `-PfirebaseEmulatorHost` để dùng Firebase thật. Emulator mặc định không lưu dữ liệu khi dừng; các phiên còn chờ của user thử không ảnh hưởng tài khoản thật.

## Deploy sau khi bật Blaze

Project thật: `vocalens-3c76f`; database `(default)` đã được tạo ở Singapore. Chưa cần tạo collection thủ công.

```powershell
# Chạy từ tools/firebase để dùng CLI được khóa phiên bản trong package-lock:
cd tools/firebase
npx.cmd firebase login
npx.cmd firebase projects:list
npm.cmd run deploy
```

Kiểm tra project trước khi deploy. Lệnh triển khai callable, Firestore Rules và index; cần quyền tương ứng trên project và gói Blaze. Chờ index của leaderboard hoàn thành trước khi dùng ranking. Chạy app build bình thường để kết nối production.

Deploy rules sẽ thay rules mặc định chặn toàn bộ bằng rules trong repository. Nếu project đã có rules cho tính năng khác, hợp nhất trước khi deploy. Backend này chỉ đọc/ghi `leaderboard` và `xpAccounts`.

## Giới hạn và vận hành

- Payload một phiên: 1–5000 từ khác nhau, rating hợp lệ, thời gian tăng hợp lệ, phiên tối đa 24 giờ, không quá 5 phút trong tương lai. Server chấp nhận phiên offline cũ nếu không chồng lên phiên đã nhận. Giờ thiết bị sai hoặc phiên không hợp lệ có thể bị từ chối.
- Phiên bị từ chối vĩnh viễn hiển thị trong màn ranking; lỗi kết nối giữ PENDING để WorkManager thử lại. Không tự xóa phiên chưa gửi hoặc cộng lại XP cá nhân sau khi server trả kết quả.
- Backend xác minh cấu trúc/tính nhất quán và chống cộng trùng, chưa chứng minh người dùng thực sự học. Flashcard là tự đánh giá; client bị sửa vẫn có thể tạo các phiên giả hợp lệ. Trước khi dùng XP cho phần thưởng có giá trị, cần cơ chế phiên do server cấp, hạn mức và kiểm tra lạm dụng.
- App Check chưa bật cưỡng chế (`enforce_app_check=False`) để chưa chặn app hiện tại. Trước khi public, cấu hình Play Integrity/App Check cho Android rồi bật enforcement và kiểm thử lại.
- Giới hạn 5 function instances giúp kiểm soát tải, không phải hạn mức chi phí. Theo dõi usage và đặt cảnh báo ngân sách khi bật Blaze.
- Tên ranking hiện là tên trong Auth; chỉnh tên chỉ ở hồ sơ Room không cập nhật tên Auth. Ranking chưa có mùa/tuần/tháng hoặc thứ hạng chính xác ngoài top 50.
- Thư viện backend Python được khai báo trong `requirements.txt`; npm chỉ dùng cho dev tooling/emulator, không được deploy cùng Python function.

Tài liệu: [Callable Functions](https://firebase.google.com/docs/functions/callable), [Firestore listeners](https://firebase.google.com/docs/firestore/query-data/listen), [Emulator Suite](https://firebase.google.com/docs/emulator-suite).
