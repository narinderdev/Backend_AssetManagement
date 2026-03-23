package com.example.eam.User.repository;

import com.example.eam.User.entity.UserCompany;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserCompanyRepository extends JpaRepository<UserCompany, Long> {

    List<UserCompany> findByUser_Id(Long userId);

    List<UserCompany> findByUser_IdAndCompany_ActiveTrue(Long userId);

    void deleteByUser_Id(Long userId);
}
