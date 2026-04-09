package com.erp.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.erp.backend.entity.Notice;
import com.erp.backend.portal.Role;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    List<Notice> findAllByAudienceRoleIsNullOrAudienceRoleOrderByCreatedAtDesc(Role role);
}
