package com.chinesereads.backend.Model;

import java.sql.Blob;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;

/**
 * Imagen inline de un post del blog, insertada desde el editor Quill y servida
 * en /api/blog/images/{id}. El post puede ser null mientras la imagen se sube
 * durante la edición de un borrador aún sin guardar; al borrar un post se
 * eliminan sus imágenes asociadas (BlogService.delete → deleteByPost). Las
 * huérfanas con post null se toleran en v1: solo las crea el admin y su
 * volumen es mínimo.
 */
@Entity
public class BlogImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Lob
    private Blob image;

    @ManyToOne
    @JoinColumn(name = "post_id")
    private BlogPost post;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Blob getImage() {
        return image;
    }

    public void setImage(Blob image) {
        this.image = image;
    }

    public BlogPost getPost() {
        return post;
    }

    public void setPost(BlogPost post) {
        this.post = post;
    }
}
