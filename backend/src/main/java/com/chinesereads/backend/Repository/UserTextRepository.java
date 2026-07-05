package com.chinesereads.backend.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chinesereads.backend.Model.User;
import com.chinesereads.backend.Model.UserText;

public interface UserTextRepository extends JpaRepository<UserText, Long> {

    List<UserText> findByOwnerOrderByCreationDateDescIdDesc(User owner);

    long countByOwner(User owner);
}
