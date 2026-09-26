package com.example.coursematch.repository;

import com.example.coursematch.entity.PublishedPlanSlot;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PublishedPlanSlotRepository extends JpaRepository<PublishedPlanSlot, Long> {

    Optional<PublishedPlanSlot> findByActivePlanId(Long activePlanId);

    /**
     * 悲观写锁锁定课程发布槽位。并发发布在此串行：
     * 先拿到锁的事务发布成功，后到的事务在锁内复查状态，得到明确冲突结果。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from PublishedPlanSlot s where s.courseId = :courseId")
    Optional<PublishedPlanSlot> findByCourseIdForUpdate(Long courseId);
}
