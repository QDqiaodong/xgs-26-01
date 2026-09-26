package com.example.coursematch.repository;

import com.example.coursematch.entity.MatchPlan;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MatchPlanRepository extends JpaRepository<MatchPlan, Long> {

    List<MatchPlan> findByCourseIdOrderByVersionNoDesc(Long courseId);

    List<MatchPlan> findAllByOrderByCreatedAtDesc();

    List<MatchPlan> findByStatusInOrderByCreatedAtDesc(List<String> statuses);

    /** 未发布方案：草稿/评审中/已通过/被驳回（资料变化需要失效的范围） */
    List<MatchPlan> findByStatusIn(List<String> statuses);

    Optional<MatchPlan> findByCourseIdAndVersionNo(Long courseId, Integer versionNo);

    long countByCourseId(Long courseId);

    /**
     * 悲观写锁锁定某课程的全部方案行。发布时先锁这批行，
     * 与槽位行锁配合，串行化同课程的并发发布/评审写入。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000"))
    @Query("select p from MatchPlan p where p.courseId = :courseId")
    List<MatchPlan> findByCourseIdForUpdate(Long courseId);

    /** 发布/指派时锁定单个方案行，防止重复发布、重复指派。 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000"))
    @Query("select p from MatchPlan p where p.id = :id")
    Optional<MatchPlan> findByIdForUpdate(Long id);
}
