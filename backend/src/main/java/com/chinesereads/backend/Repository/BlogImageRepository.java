package com.chinesereads.backend.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chinesereads.backend.Model.BlogImage;
import com.chinesereads.backend.Model.BlogPost;

public interface BlogImageRepository extends JpaRepository<BlogImage, Long> {

    void deleteByPost(BlogPost post);
}
