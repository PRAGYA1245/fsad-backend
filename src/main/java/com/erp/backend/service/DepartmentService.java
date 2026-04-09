package com.erp.backend.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.backend.common.ApiException;
import com.erp.backend.dto.DepartmentRequest;
import com.erp.backend.dto.DepartmentResponse;
import com.erp.backend.entity.Department;
import com.erp.backend.repository.DepartmentRepository;

@Service
@Transactional
public class DepartmentService {

    private static final Logger log = LoggerFactory.getLogger(DepartmentService.class);

    private final DepartmentRepository departmentRepository;

    public DepartmentService(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    public List<DepartmentResponse> getAllDepartments() {
        return departmentRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public DepartmentResponse getDepartment(Long id) {
        return toResponse(findDepartmentEntity(id));
    }

    public Department findDepartmentEntity(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Department not found."));
    }

    public DepartmentResponse createDepartment(DepartmentRequest request) {
        if (departmentRepository.existsByCodeIgnoreCase(request.code())) {
            throw new ApiException(HttpStatus.CONFLICT, "Department code already exists.");
        }
        if (departmentRepository.existsByNameIgnoreCase(request.name())) {
            throw new ApiException(HttpStatus.CONFLICT, "Department name already exists.");
        }

        Department department = new Department();
        department.setCode(request.code().trim());
        department.setName(request.name().trim());
        department.setDescription(request.description());

        Department savedDepartment = departmentRepository.save(department);
        log.info("Created department {}", savedDepartment.getCode());
        return toResponse(savedDepartment);
    }

    public DepartmentResponse updateDepartment(Long id, DepartmentRequest request) {
        Department department = findDepartmentEntity(id);

        departmentRepository.findByCodeIgnoreCase(request.code())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ApiException(HttpStatus.CONFLICT, "Department code already exists.");
                });
        departmentRepository.findAll().stream()
                .filter(existing -> existing.getName().equalsIgnoreCase(request.name()))
                .filter(existing -> !existing.getId().equals(id))
                .findFirst()
                .ifPresent(existing -> {
                    throw new ApiException(HttpStatus.CONFLICT, "Department name already exists.");
                });

        department.setCode(request.code().trim());
        department.setName(request.name().trim());
        department.setDescription(request.description());

        Department savedDepartment = departmentRepository.save(department);
        log.info("Updated department {}", savedDepartment.getCode());
        return toResponse(savedDepartment);
    }

    public void deleteDepartment(Long id) {
        Department department = findDepartmentEntity(id);
        departmentRepository.delete(department);
        log.info("Deleted department {}", department.getCode());
    }

    private DepartmentResponse toResponse(Department department) {
        return new DepartmentResponse(
                department.getId(),
                department.getCode(),
                department.getName(),
                department.getDescription(),
                department.getUsers().size(),
                department.getCourses().size()
        );
    }
}
