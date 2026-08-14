package com.welli.wellibe.record;

import com.welli.wellibe.user.User;
import com.welli.wellibe.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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