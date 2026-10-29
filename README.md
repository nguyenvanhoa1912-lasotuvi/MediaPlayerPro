# Media Player Pro – Ứng dụng phát nhạc & video cho Android

Kotlin · Jetpack Compose (Material 3) · AndroidX Media3 (ExoPlayer + MediaSession) · minSdk 26 (Android 8.0) · targetSdk 35 (Android 15)

## 1. Tính năng

| Nhóm | Chi tiết |
|---|---|
| Thư viện | Tự quét toàn bộ nhạc/video qua MediaStore; tự làm mới khi thêm/xoá tệp |
| Tìm kiếm | Tìm không dấu tiếng Việt ("xom truong" → "Xóm Trường") theo tên, nghệ sĩ, album |
| Sắp xếp | Mới thêm, Tên A→Z (chuẩn tiếng Việt), Thời lượng, Dung lượng |
| Phát nhạc | Phát nền, thông báo điều khiển, màn hình khoá, tai nghe Bluetooth; tự dừng khi rút tai nghe; xử lý audio focus (cuộc gọi đến) |
| Màn hình Đang phát | Ảnh bìa, thanh tua, Trộn bài, Lặp (tất cả/1 bài), Tốc độ 0.5x–2x, Hẹn giờ tắt 15–90 phút (chạy trong Service, vẫn hoạt động khi đóng app) |
| Video | Toàn màn hình, tự xoay theo tỉ lệ video, 3 chế độ khung hình (Vừa/Phóng/Kéo giãn), phụ đề, Picture-in-Picture (cửa sổ nổi) khi bấm Home |
| Mở từ app khác | "Mở bằng Media Player Pro" từ File Manager, Zalo, Drive… |
| Giao diện | Nền đậm navy, điểm nhấn cam/cyan, edge-to-edge, mini player |

## 2. Build & cài đặt

1. Giải nén → Android Studio (Ladybug 2024.2 trở lên) → **File › Open** → chọn thư mục `MediaPlayerPro`.
2. Chờ Gradle Sync (Android Studio tự tải Gradle 8.9 + thư viện).
   - Nếu báo thiếu Gradle wrapper: **Settings › Build Tools › Gradle › Gradle JDK = 17**, rồi Sync lại.
3. Cắm điện thoại (bật USB debugging) → bấm **Run ▶**.
4. Xuất APK/AAB phát hành: **Build › Generate Signed Bundle / APK** (release đã bật R8 thu gọn mã).

## 3. Cấu trúc mã nguồn

```
app/src/main/java/com/mediaplayerpro/app/
├── MainActivity.kt              # Quyền, Intent "Mở bằng", PiP, điều hướng
├── playback/PlaybackService.kt  # ExoPlayer + MediaSession, hẹn giờ tắt
├── data/MediaRepository.kt      # Truy vấn MediaStore (audio/video)
└── ui/
    ├── MainViewModel.kt         # Trạng thái, MediaController, lọc/sắp xếp
    ├── screens/                 # Library, NowPlaying, Video
    ├── components/MediaThumbnail.kt  # Ảnh bìa/khung hình + cache
    ├── theme/Theme.kt           # Bảng màu nền đậm
    └── util/Format.kt           # Định dạng thời gian, dung lượng, bỏ dấu
```

## 4. Checklist kiểm thử trước khi phát hành

- [ ] Android 8, 12, 13, 14, 15: cấp/từ chối quyền, mở lại Cài đặt
- [ ] Phát nhạc → khoá màn hình → điều khiển từ thông báo/lock screen
- [ ] Rút tai nghe → tự dừng; có cuộc gọi đến → tự dừng rồi tiếp tục
- [ ] Hẹn giờ 15 phút → vuốt tắt app → nhạc vẫn dừng đúng giờ
- [ ] Video dọc/ngang → tự xoay; bấm Home → vào PiP; đóng PiP → video dừng
- [ ] Mở tệp .mp3/.mp4 từ File Manager và Zalo
- [ ] Thư viện > 2.000 tệp: cuộn mượt, tìm kiếm không giật

## 5. Ghi chú kỹ thuật

- Đổi tên gói: sửa `namespace`/`applicationId` trong `app/build.gradle.kts` trước khi đưa lên Google Play.
- Ảnh bìa trên thông báo dùng URI albumart; một số máy Android 10+ có thể không hiển thị (ảnh bìa trong app vẫn hiển thị).
- Định dạng hỗ trợ theo ExoPlayer: MP3, AAC/M4A, FLAC, OGG/Opus, WAV, MP4, MKV, WebM, 3GP, TS… Codec phần cứng phụ thuộc thiết bị.
