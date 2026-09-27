package com.example.coursematch.repository;

import com.example.coursematch.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    /** 讲师档案ID对应的登录账号（用于把观察单定位到讲师本人会话） */
    Optional<User> findByInstructorId(Long instructorId);

    /** 某角色下全部启用账号（异议复核提醒广播给全体督导、负责人定位课程管理员） */
    @Query("select u from User u join u.roles r where r.name = :roleName and u.enabled = true")
    List<User> findEnabledByRoleName(String roleName);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}