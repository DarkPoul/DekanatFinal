package com.esvar.dekanat.service;

import com.esvar.dekanat.entity.ControlMethodEntity;
import com.esvar.dekanat.entity.MarksEntity;
import com.esvar.dekanat.entity.PlansEntity;
import com.esvar.dekanat.entity.StudentEntity;
import com.esvar.dekanat.repository.ControlMethodRepository;
import com.esvar.dekanat.repository.MarksRepository;
import com.esvar.dekanat.security.SecurityService;
import com.esvar.dekanat.service.exception.MarkLockedException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Service
public class MarksService {

    private final MarksRepository marksRepository;
    private final ControlMethodRepository controlMethodRepository;
    private final RatingService ratingService;
    private final SecurityService securityService;

    @PersistenceContext
    private EntityManager entityManager;

    public MarksService(MarksRepository marksRepository, ControlMethodRepository controlMethodRepository, RatingService ratingService, SecurityService securityService) {
        this.marksRepository = marksRepository;
        this.controlMethodRepository = controlMethodRepository;
        this.ratingService = ratingService;
        this.securityService = securityService;
    }

    /**
     * Зберігає нову оцінку.
     *
     * @param mark MarksEntity - об'єкт для збереження.
     */
    @Transactional
    public MarksEntity saveMark(MarksEntity mark) {
        return saveMarkInternal(mark, false);
    }

    @Transactional
    public MarksEntity saveMarkAllowingLockedUpdate(MarksEntity mark) {
        return saveMarkInternal(mark, true);
    }

    private MarksEntity saveMarkInternal(MarksEntity mark, boolean allowLockedOverride) {
        if (mark == null || mark.getStudent() == null || mark.getPlan() == null || mark.getControlMethod() == null) {
            throw new IllegalArgumentException("Студент, план і метод контролю повинні бути задані.");
        }
        mark.setLastUpdated(new Timestamp(System.currentTimeMillis()));
        mark.setLastUpdatedBy(
                securityService.getCurrentUserModel()
                        .orElseThrow(() -> new IllegalStateException("No authenticated user"))
        );
        boolean exists = marksRepository.existsByStudentIdAndPlanIdAndControlMethodId(
                mark.getStudent().getId(),
                mark.getPlan().getId(),
                mark.getControlMethod().getId()
        );
        MarksEntity saved;
        if (exists) {
            Optional<MarksEntity> existingOptional = marksRepository.findByStudentIdAndPlanIdAndControlMethodId(
                    mark.getStudent().getId(),
                    mark.getPlan().getId(),
                    mark.getControlMethod().getId()
            );

            MarksEntity existing = existingOptional.orElseThrow(() -> new IllegalArgumentException("Оцінка не знайдена."));
            if (existing.isLocked() && !allowLockedOverride) {
                throw new MarkLockedException("Оцінка заблокована і не може бути змінена без розблокування.");
            }

            existing.setFinalGrade(mark.getFinalGrade());
            existing.setLocked(mark.isLocked());
            existing.setLastUpdated(mark.getLastUpdated());
            existing.setLastUpdatedBy(mark.getLastUpdatedBy());
            saved = marksRepository.save(existing);
        } else {
            StudentEntity managedStudent = entityManager.getReference(StudentEntity.class, mark.getStudent().getId());
            PlansEntity managedPlan = entityManager.getReference(PlansEntity.class, mark.getPlan().getId());
            ControlMethodEntity managedControl = entityManager.getReference(ControlMethodEntity.class, mark.getControlMethod().getId());

            mark.setStudent(managedStudent);
            mark.setPlan(managedPlan);
            mark.setControlMethod(managedControl);
            saved = marksRepository.save(mark);
        }
        ratingService.updateRatingForStudent(saved.getStudent());
        return saved;
    }

    @Transactional
    public MarksEntity unlockMark(Long markId) {
        if (markId == null) {
            throw new IllegalArgumentException("Ідентифікатор оцінки повинен бути заданий.");
        }
        MarksEntity existing = marksRepository.findById(markId)
                .orElseThrow(() -> new IllegalArgumentException("Оцінка не знайдена."));
        if (!canCurrentUserUnlockMarks()) {
            throw new AccessDeniedException("У вас немає прав для розблокування оцінок.");
        }
        if (!existing.isLocked()) {
            return existing;
        }
        existing.setLocked(false);
        existing.setLastUpdated(new Timestamp(System.currentTimeMillis()));
        existing.setLastUpdatedBy(
                securityService.getCurrentUserModel()
                        .orElseThrow(() -> new IllegalStateException("No authenticated user"))
        );
        return saveMarkInternal(existing, true);
    }

    private boolean canCurrentUserUnlockMarks() {
        UserDetails userDetails = securityService.getAuthenticatedUser();
        if (userDetails == null) {
            throw new IllegalStateException("No authenticated user");
        }
        return userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.startsWith("ROLE_ADMIN") || authority.startsWith("ROLE_DEKANAT"));
    }


    /**
     * Отримує оцінку за студента та план.
     *
     * @param student   StudentEntity - студент.
     * @param updatedPlan PlansEntity - план.
     * @return MarksEntity - знайдена оцінка або null, якщо не знайдено.
     */
    public MarksEntity getMarkByStudentAndPlan(StudentEntity student, PlansEntity updatedPlan) {
        if (student == null || updatedPlan == null) {
            return null; // Якщо студент або план відсутні, повертаємо null
        }

        return marksRepository.findByStudentIdAndPlanId(student.getId(), updatedPlan.getId()).orElse(null);
    }

    public Long getLastId() {
        return marksRepository.findMaxId().orElse(0L);
    }

    public List<MarksEntity> findMarksByPlan(PlansEntity plansEntity) {
        return marksRepository.findByPlan(plansEntity);
    }

    public List<MarksEntity> findMarksByPlanAndTypeControl(PlansEntity plansEntity, String typeControl) {
        if (plansEntity == null || typeControl == null) {
            return List.of();
        }

        String trimmedTypeControl = typeControl.trim();
        if (trimmedTypeControl.isEmpty()) {
            return List.of();
        }

        ControlMethodEntity controlMethod = controlMethodRepository.findByName(trimmedTypeControl);
        if (controlMethod == null) {
            return List.of();
        }

        return marksRepository.findByPlanAndControlMethod(plansEntity, controlMethod);
    }

    public List<MarksEntity> findMarksByPlanAndControlMethod(PlansEntity plan, ControlMethodEntity controlMethod) {
        if (plan == null || controlMethod == null) {
            return List.of();
        }

        return marksRepository.findByPlanAndControlMethod(plan, controlMethod);
    }

    public String getMarkForTypeControl(StudentEntity studentEntity, PlansEntity plansEntity, String typeControl) {
        ControlMethodEntity method = controlMethodRepository.findByName(typeControl);
        if (method == null) {
            return "0";
        }
        Optional<MarksEntity> opt = marksRepository.findByStudentIdAndPlanIdAndControlMethodId(
                studentEntity.getId(),
                plansEntity.getId(),
                method.getId()
        );
        if (opt.isPresent() && opt.get().getFinalGrade() != 0) {
            return String.valueOf(opt.get().getFinalGrade());
        }
        return "0";
    }

    /**
     * Returns all marks for the given student.
     *
     * @param student StudentEntity - student for which marks are requested.
     * @return list of MarksEntity
     */
    public List<MarksEntity> getMarksByStudent(StudentEntity student) {
        if (student == null) {
            return List.of();
        }
        return marksRepository.findByStudentId(student.getId());
    }

    /**
     * Get mark by its id.
     *
     * @param id identifier of mark
     * @return MarksEntity or null if not found
     */
    public MarksEntity getMarkById(Long id) {
        return marksRepository.findById(id).orElse(null);
    }

    /**
     * Batch save for a list of marks.
     * Each mark is processed via {@link #saveMark(MarksEntity)}.
     *
     * @param marks list of entities to save
     */
    @Transactional
    public void saveMarks(List<MarksEntity> marks) {
        if (marks == null || marks.isEmpty()) {
            return;
        }
        for (MarksEntity mark : marks) {
            saveMark(mark);
        }
    }

    @Transactional
    public void deleteByPlanId(Long planId) {
        if (planId != null) {
            marksRepository.deleteByPlanId(planId);
        }
    }

    @Transactional
    public void deleteByPlanIdAndStudentIds(Long planId, List<Long> studentIds) {
        if (planId != null && studentIds != null && !studentIds.isEmpty()) {
            marksRepository.deleteByPlanIdAndStudentIds(planId, studentIds);
        }
    }


}
