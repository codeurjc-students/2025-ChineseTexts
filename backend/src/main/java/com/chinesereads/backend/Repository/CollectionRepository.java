package com.chinesereads.backend.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chinesereads.backend.Model.Collection;

public interface CollectionRepository extends JpaRepository<Collection, Long>{

}
