package com.example.demo.repository;

import com.example.demo.entity.Member;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// JpaRepository는 기본 CRUD 메서드 (findById, findAll, save, delete 등)는 자동 제공됩니다.
// 그 외는 스스로 구현
//save(): 새 엔티티 저장 또는 기존 엔티티 업데이트
//findById(): ID로 엔티티 조회
//findAll(): 모든 엔티티 조회
//deleteById(): ID로 엔티티 삭제
//delete(): 엔티티 삭제

@Repository
public interface MemberRepository extends CrudRepository<Member, String> {
    Optional<Member> findByEmail(String email);

}
