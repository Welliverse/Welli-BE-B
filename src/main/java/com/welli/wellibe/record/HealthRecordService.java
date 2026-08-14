package com.welli.wellibe.record;

import com.welli.wellibe.user.User;
import com.welli.wellibe.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;
@Service
@RequiredArgsConstructor
public class HealthRecordService {

    private final HealthRecordRepository healthRecordRepository;
    private final UserRepository userRepository;

    @Transactional
    public HealthRecordResponse create(
            String email,
            HealthRecordCreateRequest request
    ) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("존재하지 않는 사용자입니다.")
                );

        HealthRecord record = HealthRecord.builder()
                .user(user)
                .type(request.type())
                .value(request.value())
                .photoUrl(request.photoUrl())
                .build();

        return toResponse(healthRecordRepository.save(record));
    }

    @Transactional(readOnly = true)
    public List<HealthRecordResponse> getMyRecords(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("존재하지 않는 사용자입니다.")
                );

        return healthRecordRepository
                .findByUserIdOrderByRecordedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public HealthRecordResponse uploadSkinPhoto(
            String email,
            MultipartFile photo
    ) {
        if (photo.isEmpty()) {
            throw new IllegalArgumentException("업로드할 사진이 없습니다.");
        }

        String contentType = photo.getContentType();

        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("이미지 파일만 업로드할 수 있습니다.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("존재하지 않는 사용자입니다.")
                );

        try {
            Path uploadDirectory = Path.of("uploads")
                    .toAbsolutePath()
                    .normalize();

            Files.createDirectories(uploadDirectory);

            String extension = switch (contentType) {
                case "image/png" -> ".png";
                case "image/gif" -> ".gif";
                case "image/webp" -> ".webp";
                default -> ".jpg";
            };

            String fileName = UUID.randomUUID() + extension;

            Files.copy(
                    photo.getInputStream(),
                    uploadDirectory.resolve(fileName),
                    StandardCopyOption.REPLACE_EXISTING
            );

            HealthRecord record = HealthRecord.builder()
                    .user(user)
                    .type(HealthRecordType.SKIN_PHOTO)
                    .value(Map.of("description", "피부 사진 기록"))
                    .photoUrl("/uploads/" + fileName)
                    .build();

            return toResponse(healthRecordRepository.save(record));

        } catch (IOException e) {
            throw new IllegalStateException("사진 저장에 실패했습니다.");
        }
    }
    private HealthRecordResponse toResponse(HealthRecord record) {
        return new HealthRecordResponse(
                record.getId(),
                record.getType(),
                record.getValue(),
                record.getPhotoUrl(),
                record.getRecordedAt()
        );
    }
}