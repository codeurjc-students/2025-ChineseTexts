import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

/** Resumen para tarjetas (listado público y admin) — sin cuerpos. */
export interface BlogPostSummary {
  id: number;
  slug: string;
  titleEn: string | null;
  titleEs: string | null;
  excerptEn: string | null;
  excerptEs: string | null;
  hasCover: boolean;
  published: boolean;
  publishedOn: string | null;
  updatedAt: string | null;
}

/** Post completo (detalle público por slug + editor admin por id). */
export interface BlogPost extends BlogPostSummary {
  contentEn: string | null;
  contentEs: string | null;
}

/** Payload parcial del editor: un campo omitido/null se deja como está. */
export interface BlogPostUpsert {
  slug?: string;
  titleEn?: string;
  titleEs?: string;
  excerptEn?: string;
  excerptEs?: string;
  contentEn?: string;
  contentEs?: string;
  published?: boolean;
}

/**
 * API del blog (/api/blog). Lecturas públicas sin credenciales; el resto es
 * admin y viaja con la cookie de sesión.
 */
@Injectable({ providedIn: 'root' })
export class BlogService {

  private readonly base = '/api/blog';

  constructor(private http: HttpClient) {}

  // ---------- Público ----------

  getPublished(): Observable<BlogPostSummary[]> {
    return this.http.get<BlogPostSummary[]>(this.base);
  }

  getBySlug(slug: string): Observable<BlogPost> {
    return this.http.get<BlogPost>(`${this.base}/slug/${slug}`);
  }

  /** URL de la portada con cache-buster (misma técnica que las fotos del HoF). */
  coverUrl(id: number, version = 0): string {
    return `${this.base}/${id}/cover?v=${version}`;
  }

  // ---------- Admin ----------

  getAll(): Observable<BlogPostSummary[]> {
    return this.http.get<BlogPostSummary[]>(`${this.base}/all`, { withCredentials: true });
  }

  getById(id: number): Observable<BlogPost> {
    return this.http.get<BlogPost>(`${this.base}/${id}`, { withCredentials: true });
  }

  create(post: BlogPostUpsert): Observable<BlogPost> {
    return this.http.post<BlogPost>(this.base, post, { withCredentials: true });
  }

  update(id: number, post: BlogPostUpsert): Observable<BlogPost> {
    return this.http.put<BlogPost>(`${this.base}/${id}`, post, { withCredentials: true });
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`, { withCredentials: true });
  }

  uploadCover(id: number, file: File): Observable<void> {
    const form = new FormData();
    form.append('image', file);
    return this.http.put<void>(`${this.base}/${id}/cover`, form, { withCredentials: true });
  }

  deleteCover(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}/cover`, { withCredentials: true });
  }

  uploadImage(file: File, postId?: number): Observable<{ id: number; url: string }> {
    const form = new FormData();
    form.append('image', file);
    if (postId != null) form.append('postId', String(postId));
    return this.http.post<{ id: number; url: string }>(`${this.base}/images`, form, { withCredentials: true });
  }
}
