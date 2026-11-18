# Kiểm tra model nhận diện của VocabLens

Bài test `YoloModelSmokeTest` chạy độc lập với giao diện camera, trên điện thoại hoặc máy ảo Android. Model và nhãn được đọc từ assets của ứng dụng; ảnh mẫu được đọc từ assets của APK kiểm thử.

## Luồng camera đã tích hợp

Trong ứng dụng, mở tab Camera, cấp quyền và chọn **Chụp và nhận diện**. Ảnh được xoay đúng chiều theo CameraX, giới hạn cạnh dài ở 1280 pixel và xử lý hoàn toàn trên thiết bị. Sau khi phân tích, màn hình hiển thị ảnh có khung và danh sách nhãn tiếng Anh cùng điểm tin cậy. Những vật trùng lớp được gộp thành một mục với điểm cao nhất. Chọn **Chụp lại** để quay về camera, hoặc **Thử lại** khi phân tích lỗi. Ảnh chỉ giữ trong bộ nhớ, được bỏ khi chụp lại/rời màn hình; chưa nối nghĩa tiếng Việt và lưu từ.

Luồng kiến trúc: `CameraRoute → CameraViewModel → RecognizeObjectsUseCase → ObjectRecognitionRepository → LocalObjectRecognitionRepository`. Domain không phụ thuộc Bitmap hoặc CameraX. Tầng data chạy LiteRT ở dispatcher nền và đóng Interpreter sau mỗi lần nhận diện. Callback cũ bị bỏ qua khi chụp lại hoặc điều hướng đi.

Kiểm thử tích hợp: `LocalObjectRecognitionTest` chạy ảnh mẫu qua repository dùng trong ứng dụng; `CameraScreenTest` chụp, nhận diện và chụp lại hai lần bằng CameraX trên thiết bị. Unit test `YoloPostprocessorTest` và `CameraViewModelTest` kiểm tra tọa độ, NMS, chống bấm chụp trùng, kết quả rỗng, thử lại và hủy callback cũ. Cần thử thêm trên điện thoại thật ở cả chiều dọc/ngang, camera trước/sau và khi thu hồi quyền.

## Chạy trong Android Studio

1. Mở Device Manager và khởi động máy ảo, hoặc kết nối điện thoại đã bật USB debugging.
2. Mở `app/src/androidTest/java/com/luudev/vocalens/data/recognition/YoloModelSmokeTest.kt`.
3. Bấm nút Run cạnh class `YoloModelSmokeTest` và chọn thiết bị.
4. Mở Logcat, lọc `tag:VocabLensModelTest` để xem shape, kiểu dữ liệu, thời gian chạy, nhãn, điểm số và tọa độ.
5. Trong Device Explorer, mở `/data/data/com.luudev.vocalens/files/model-smoke-test/` và lưu hai file về máy:
   - `detections.txt`: kết quả nhận diện trước/sau NMS và các thông số xử lý.
   - `detections.jpg`: ảnh gốc có khung và nhãn để kiểm tra tọa độ trực quan.

Hoặc chạy từ PowerShell tại thư mục dự án sau khi thiết bị đã kết nối:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.luudev.vocalens.data.recognition.YoloModelSmokeTest'
```

## Test thực hiện những gì?

- Kiểm tra input `FLOAT32 [1, 3, 640, 640]`, output `FLOAT32 [1, 605, 8400]` và 601 nhãn không rỗng.
- Resize ảnh giữ tỉ lệ và thêm viền xám 114 để được ảnh 640 × 640; chuyển RGB sang NCHW, chia từng giá trị màu cho 255.
- Chạy CPU với 4 luồng; kiểm tra đầu ra không chứa NaN hoặc Infinity.
- Diễn giải 4 kênh đầu là `cx, cy, width, height`; 601 kênh tiếp theo là điểm số lớp. Chọn lớp cao điểm nhất cho mỗi vị trí, giữ điểm từ 0.25.
- Mặc định diễn giải tọa độ chuẩn hóa 0–1: nhân với 640, bỏ phần padding và chuyển hộp về kích thước ảnh gốc.
- NMS chỉ loại hộp trùng cùng lớp khi IoU > 0.45, tối đa 100 kết quả.
- Ghi báo cáo và ảnh trước khi kiểm tra có kết quả và có nhãn `Man` (người đi bộ trong ảnh mẫu).
- Có bài test riêng kiểm tra NMS giữ lại vật khác lớp và các hộp không giao nhau.

Các giả định về RGB/255, tọa độ và thứ tự nhãn cần được xác nhận bằng kết quả thực tế. Test pass chỉ là kiểm tra ban đầu, không chứng minh độ chính xác toàn bộ model. Hãy kiểm tra ảnh có khung và thử thêm vài ảnh đồ vật quen thuộc trước khi tích hợp camera. Nếu Interpreter không khởi tạo/chạy được, cần xem lỗi operator/runtime trước; chưa thể kết luận model chạy được chỉ từ shape.

Lần chạy trên máy ảo với tọa độ chuẩn hóa cho 52 ứng viên, còn 9 kết quả sau NMS, gồm `Man`, `Footwear`, `Wheel`, `Woman`; điểm cao nhất khoảng 0.647. Model chưa nhận ra `Bus` ở ngưỡng 0.25. Vì vậy mặc định xác nhận nhãn `Man` có thật trong ảnh; để đánh giá riêng xe buýt, truyền `expectedLabel=Bus` (hiện kiểm tra đó vẫn thất bại).

## Thử ảnh khác hoặc tọa độ chuẩn hóa

Thêm một ảnh JPG/PNG đã xoay đúng chiều vào `app/src/androidTest/assets/`, ví dụ `apple.jpg`, rồi chạy:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.luudev.vocalens.data.recognition.YoloModelSmokeTest' '-Pandroid.testInstrumentationRunnerArguments.imageAsset=apple.jpg' '-Pandroid.testInstrumentationRunnerArguments.expectedLabel=Apple'
```

Model hiện tại dùng mặc định `coordinateSpace=normalized`. Khi kiểm tra một bản export khác dùng tọa độ pixel, truyền `'-Pandroid.testInstrumentationRunnerArguments.coordinateSpace=pixels'`. Luôn kiểm tra ảnh có khung để xác nhận cách giải mã tọa độ.

## Nguồn ảnh mẫu

`app/src/androidTest/assets/bus.jpg` được lấy từ kho chính thức Ultralytics:
https://github.com/ultralytics/ultralytics/blob/main/ultralytics/assets/bus.jpg

Ảnh chỉ đóng gói trong APK kiểm thử. Đây cũng là ảnh được dùng trong ví dụ dự đoán của Ultralytics: https://docs.ultralytics.com/modes/predict/
