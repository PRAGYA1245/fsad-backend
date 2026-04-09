package com.erp.backend.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.backend.common.ApiException;
import com.erp.backend.dto.NoticeRequest;
import com.erp.backend.dto.NoticeResponse;
import com.erp.backend.entity.AppUser;
import com.erp.backend.entity.Notice;
import com.erp.backend.repository.NoticeRepository;

@Service
@Transactional
public class NoticeService {

    private static final Logger log = LoggerFactory.getLogger(NoticeService.class);

    private final NoticeRepository noticeRepository;
    private final CurrentUserService currentUserService;

    public NoticeService(NoticeRepository noticeRepository, CurrentUserService currentUserService) {
        this.noticeRepository = noticeRepository;
        this.currentUserService = currentUserService;
    }

    public List<NoticeResponse> getVisibleNotices(Jwt jwt) {
        AppUser user = currentUserService.getCurrentUser(jwt);
        return noticeRepository.findAllByAudienceRoleIsNullOrAudienceRoleOrderByCreatedAtDesc(user.getRole()).stream()
                .map(this::toResponse)
                .toList();
    }

    public NoticeResponse createNotice(Jwt jwt, NoticeRequest request) {
        AppUser author = currentUserService.getCurrentUser(jwt);
        Notice notice = new Notice();
        notice.setTitle(request.title().trim());
        notice.setMessage(request.message().trim());
        notice.setLevel(request.level());
        notice.setAudienceRole(request.audienceRole());
        notice.setAuthor(author);

        Notice savedNotice = noticeRepository.save(notice);
        log.info("User {} created notice {}", author.getUsername(), savedNotice.getTitle());
        return toResponse(savedNotice);
    }

    public NoticeResponse updateNotice(Long id, NoticeRequest request) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Notice not found."));

        notice.setTitle(request.title().trim());
        notice.setMessage(request.message().trim());
        notice.setLevel(request.level());
        notice.setAudienceRole(request.audienceRole());

        Notice savedNotice = noticeRepository.save(notice);
        log.info("Updated notice {}", savedNotice.getId());
        return toResponse(savedNotice);
    }

    public void deleteNotice(Long id) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Notice not found."));
        noticeRepository.delete(notice);
        log.info("Deleted notice {}", notice.getId());
    }

    public NoticeResponse toResponse(Notice notice) {
        return new NoticeResponse(
                notice.getId(),
                notice.getTitle(),
                notice.getMessage(),
                notice.getLevel(),
                notice.getAudienceRole(),
                notice.getAuthor().getFullName(),
                notice.getCreatedAt()
        );
    }
}
