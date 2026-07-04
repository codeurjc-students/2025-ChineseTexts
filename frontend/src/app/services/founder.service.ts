import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

/**
 * Cliente de la API del perfil del creador (/api/founder).
 *
 * Las lecturas (getProfile, URLs de imagen) son públicas; las escrituras
 * requieren sesión de ADMIN y viajan con `withCredentials`. Las imágenes se
 * sirven por endpoints propios; para forzar recarga tras subir una nueva se
 * añade un parámetro de versión (evita problemas de hidratación en SSR porque
 * sólo cambia tras una acción del usuario en el navegador).
 */

export interface FounderItem {
  id?: number;
  heading: string;
  subheading: string;
  period: string;
  location: string;
  description: string;
  linkUrl: string;
  linkLabel: string;
  displayOrder: number;
  hasLogo: boolean;
}

export interface FounderSection {
  id?: number;
  title: string;
  type: string;
  displayOrder: number;
  items: FounderItem[];
}

export interface FounderSocial {
  id?: number;
  label: string;
  icon: string;
  url: string;
  displayOrder: number;
}

export interface FounderProfile {
  id?: number;
  name: string;
  role: string;
  tagline: string;
  location: string;
  summary: string;
  hasPhoto: boolean;
  socials: FounderSocial[];
  sections: FounderSection[];
}

@Injectable({ providedIn: 'root' })
export class FounderService {

  private apiUrl = '/api/founder';

  constructor(private http: HttpClient) {}

  // ---------- Perfil ----------
  getProfile(): Observable<FounderProfile> {
    return this.http.get<FounderProfile>(this.apiUrl);
  }

  updateProfile(profile: FounderProfile): Observable<FounderProfile> {
    return this.http.put<FounderProfile>(this.apiUrl, profile, { withCredentials: true });
  }

  // ---------- Foto ----------
  photoUrl(version = 0): string {
    return version ? `${this.apiUrl}/photo?v=${version}` : `${this.apiUrl}/photo`;
  }

  uploadPhoto(file: File): Observable<void> {
    const form = new FormData();
    form.append('image', file);
    return this.http.post<void>(`${this.apiUrl}/photo`, form, { withCredentials: true });
  }

  deletePhoto(): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/photo`, { withCredentials: true });
  }

  // ---------- Enlaces / redes ----------
  addSocial(social: FounderSocial): Observable<FounderSocial> {
    return this.http.post<FounderSocial>(`${this.apiUrl}/socials`, social, { withCredentials: true });
  }

  updateSocial(id: number, social: FounderSocial): Observable<FounderSocial> {
    return this.http.put<FounderSocial>(`${this.apiUrl}/socials/${id}`, social, { withCredentials: true });
  }

  deleteSocial(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/socials/${id}`, { withCredentials: true });
  }

  // ---------- Secciones ----------
  addSection(section: FounderSection): Observable<FounderSection> {
    return this.http.post<FounderSection>(`${this.apiUrl}/sections`, section, { withCredentials: true });
  }

  updateSection(id: number, section: FounderSection): Observable<FounderSection> {
    return this.http.put<FounderSection>(`${this.apiUrl}/sections/${id}`, section, { withCredentials: true });
  }

  deleteSection(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/sections/${id}`, { withCredentials: true });
  }

  // ---------- Ítems ----------
  addItem(sectionId: number, item: FounderItem): Observable<FounderItem> {
    return this.http.post<FounderItem>(`${this.apiUrl}/sections/${sectionId}/items`, item, { withCredentials: true });
  }

  updateItem(id: number, item: FounderItem): Observable<FounderItem> {
    return this.http.put<FounderItem>(`${this.apiUrl}/items/${id}`, item, { withCredentials: true });
  }

  deleteItem(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/items/${id}`, { withCredentials: true });
  }

  // ---------- Logo de un ítem ----------
  itemLogoUrl(id: number, version = 0): string {
    return version ? `${this.apiUrl}/items/${id}/logo?v=${version}` : `${this.apiUrl}/items/${id}/logo`;
  }

  uploadItemLogo(id: number, file: File): Observable<void> {
    const form = new FormData();
    form.append('image', file);
    return this.http.post<void>(`${this.apiUrl}/items/${id}/logo`, form, { withCredentials: true });
  }

  deleteItemLogo(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/items/${id}/logo`, { withCredentials: true });
  }
}
