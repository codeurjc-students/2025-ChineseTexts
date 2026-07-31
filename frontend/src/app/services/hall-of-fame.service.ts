import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

/**
 * Cliente de la API del Hall of Fame (/api/hall-of-fame).
 *
 * Las lecturas (getAll, URLs de foto) son públicas; las escrituras requieren
 * sesión de ADMIN y viajan con `withCredentials`. Las fotos se sirven por su
 * endpoint propio; para forzar recarga tras subir una nueva se añade un
 * parámetro de versión (evita problemas de hidratación en SSR porque sólo
 * cambia tras una acción del usuario en el navegador).
 */

export interface HallOfFameSocial {
  id?: number;
  label: string;
  icon: string;
  url: string;
  displayOrder: number;
}

export interface HallOfFameEntry {
  id?: number;
  name: string;
  slug: string;
  taglineEn: string;
  taglineEs: string;
  bioEn: string;
  bioEs: string;
  discountCode: string;
  displayOrder: number;
  hasPhoto: boolean;
  badges: string[];
  socials: HallOfFameSocial[];
}

@Injectable({ providedIn: 'root' })
export class HallOfFameService {

  private apiUrl = '/api/hall-of-fame';

  constructor(private http: HttpClient) {}

  // ---------- Entradas ----------
  getAll(): Observable<HallOfFameEntry[]> {
    return this.http.get<HallOfFameEntry[]>(this.apiUrl);
  }

  /** Página detalle pública /hall-of-fame/:slug (404 si el slug no existe). */
  getBySlug(slug: string): Observable<HallOfFameEntry> {
    return this.http.get<HallOfFameEntry>(`${this.apiUrl}/slug/${slug}`);
  }

  create(entry: Partial<HallOfFameEntry>): Observable<HallOfFameEntry> {
    return this.http.post<HallOfFameEntry>(this.apiUrl, entry, { withCredentials: true });
  }

  update(id: number, entry: Partial<HallOfFameEntry>): Observable<HallOfFameEntry> {
    return this.http.put<HallOfFameEntry>(`${this.apiUrl}/${id}`, entry, { withCredentials: true });
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`, { withCredentials: true });
  }

  // ---------- Foto ----------
  photoUrl(id: number, version = 0): string {
    return version ? `${this.apiUrl}/${id}/photo?v=${version}` : `${this.apiUrl}/${id}/photo`;
  }

  uploadPhoto(id: number, file: File): Observable<void> {
    const form = new FormData();
    form.append('image', file);
    return this.http.post<void>(`${this.apiUrl}/${id}/photo`, form, { withCredentials: true });
  }

  deletePhoto(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}/photo`, { withCredentials: true });
  }

  // ---------- Redes sociales ----------
  addSocial(entryId: number, social: HallOfFameSocial): Observable<HallOfFameSocial> {
    return this.http.post<HallOfFameSocial>(`${this.apiUrl}/${entryId}/socials`, social, { withCredentials: true });
  }

  updateSocial(id: number, social: HallOfFameSocial): Observable<HallOfFameSocial> {
    return this.http.put<HallOfFameSocial>(`${this.apiUrl}/socials/${id}`, social, { withCredentials: true });
  }

  deleteSocial(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/socials/${id}`, { withCredentials: true });
  }
}
