package club.muimi.backend.repository;

import club.muimi.backend.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Optional<Notification> findByRecipientUserIdAndBizKey(Long recipientUserId, String bizKey);

    @Query("""
            select n
            from Notification n
            where n.recipientUserId = :recipientUserId
              and (:unreadOnly = false or n.readAt is null)
            """)
    Page<Notification> findPageByRecipientUserId(
            @Param("recipientUserId") Long recipientUserId,
            @Param("unreadOnly") boolean unreadOnly,
            Pageable pageable
    );

    long countByRecipientUserIdAndReadAtIsNull(Long recipientUserId);

    List<Notification> findAllByRecipientUserIdAndReadAtIsNull(Long recipientUserId);

    List<Notification> findAllByRecipientUserIdInAndBizKeyIn(Collection<Long> recipientUserIds, Collection<String> bizKeys);

    @Modifying
    @Query("""
            delete from Notification n
            where n.relatedType = :relatedType
              and n.relatedId = :relatedId
            """)
    int deleteAllByRelated(
            @Param("relatedType") String relatedType,
            @Param("relatedId") Long relatedId
    );

    @Query("""
            select distinct n.relatedId
            from Notification n
            where n.relatedType = :relatedType
              and n.relatedId is not null
            """)
    List<Long> findRelatedIdsByRelatedType(@Param("relatedType") String relatedType);

    @Modifying
    @Query("""
            delete from Notification n
            where n.relatedType = :relatedType
              and n.relatedId in :relatedIds
            """)
    int deleteAllByRelatedTypeAndRelatedIdIn(
            @Param("relatedType") String relatedType,
            @Param("relatedIds") Collection<Long> relatedIds
    );
}
