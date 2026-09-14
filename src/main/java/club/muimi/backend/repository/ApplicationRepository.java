package club.muimi.backend.repository;

import club.muimi.backend.common.enums.ApplicationStatus;
import club.muimi.backend.common.enums.Grade;
import club.muimi.backend.entity.Application;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    List<Application> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    List<Application> findAllByUserIdIn(Collection<Long> userIds);

    Optional<Application> findByIdAndUserId(Long id, Long userId);

    @Query(value = "select * from `application` where id = :id for update", nativeQuery = true)
    Optional<Application> findByIdForUpdate(Long id);

    Optional<Application> findByUserIdAndDirectionLevel2Id(Long userId, Long directionLevel2Id);

    boolean existsByUserIdAndDirectionLevel2Id(Long userId, Long directionLevel2Id);

    boolean existsByUserIdAndDirectionLevel2IdAndIdNot(Long userId, Long directionLevel2Id, Long id);

    boolean existsByUserId(Long userId);

    long countByUserId(Long userId);

    long countByUserIdAndStatus(Long userId, ApplicationStatus status);

    List<Application> findAllByStatus(ApplicationStatus status, Sort sort);

    @Query("""
            select a
            from Application a
            where a.status = club.muimi.backend.common.enums.ApplicationStatus.SUBMITTED
              and not exists (
                  select 1
                  from GroupMember gm
                  where gm.applicationId = a.id
              )
            """)
    List<Application> findAllUngroupedSubmittedApplications(Sort sort);

    @Query("""
            select a
            from Application a
            where (:status is null or a.status = :status)
              and (:directionLevel1Id is null or a.directionLevel1Id = :directionLevel1Id)
              and (:directionLevel2Id is null or a.directionLevel2Id = :directionLevel2Id)
              and (:grade is null or a.grade = :grade)
              and (:admissionYear is null or a.admissionYear = :admissionYear)
              and (:keyword is null
                   or lower(a.realName) like lower(concat('%', :keyword, '%'))
                   or lower(a.college) like lower(concat('%', :keyword, '%'))
                   or lower(a.major) like lower(concat('%', :keyword, '%'))
                   or lower(a.className) like lower(concat('%', :keyword, '%'))
                   or lower(a.phoneNumber) like lower(concat('%', :keyword, '%'))
                   or exists (
                       select 1
                       from User u
                       where u.id = a.userId
                         and (
                             lower(u.username) like lower(concat('%', :keyword, '%'))
                             or lower(u.email) like lower(concat('%', :keyword, '%'))
                         )
                   ))
            """)
    Page<Application> searchApplications(
            @Param("status") ApplicationStatus status,
            @Param("directionLevel1Id") Long directionLevel1Id,
            @Param("directionLevel2Id") Long directionLevel2Id,
            @Param("grade") Grade grade,
            @Param("admissionYear") Integer admissionYear,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    boolean existsByDirectionLevel1Id(Long directionLevel1Id);

    boolean existsByDirectionLevel2Id(Long directionLevel2Id);
}
