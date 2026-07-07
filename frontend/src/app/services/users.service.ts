import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface UserDTO {
  id: number | null;
  email: string;
  name: string;
  language: string;
  collections: any[];
  roles: string[];
  password: string;
  newPassword: string | null;
  /** GDPR: whether the user accepted the terms of use on signup (validated server-side). */
  termsAccepted?: boolean;
}

/** One row of the admin user list. */
export interface AdminUserSummary {
  id: number;
  email: string;
  name: string;
  language: string;
  roles: string[];
  blocked: boolean;
  registrationDate: string | null;
  lastAccess: string | null;
}

export interface AdminCollectionSummary {
  id: number;
  title: string;
  flashcardsCount: number;
}

/** Full user profile shown on the admin detail page. */
export interface AdminUserDetail {
  id: number;
  email: string;
  name: string;
  language: string;
  roles: string[];
  blocked: boolean;
  registrationDate: string | null;
  lastAccess: string | null;
  collectionsCount: number;
  flashcardsCount: number;
  textsCount: number;
  collections: AdminCollectionSummary[];
}

@Injectable({ providedIn: 'root' })
export class UserService {

  private apiUrl = '/api/users';

  constructor(private http: HttpClient) {}

  register(user: UserDTO): Observable<any> {
    return this.http.post(`${this.apiUrl}/signup`, user);
  }

  updateProfile(data: { name: string; language: string }): Observable<UserDTO> {
    return this.http.put<UserDTO>(`${this.apiUrl}/me`, data, { withCredentials: true });
  }

  checkPassword(password: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/me/check-password`,
      { password }, { withCredentials: true });
  }

  changePassword(newPassword: string): Observable<any> {
    return this.http.put(`${this.apiUrl}/me/password`,
      { newPassword }, { withCredentials: true });
  }

  /** Permanently deletes the currently authenticated user's own account. */
  deleteOwnAccount(): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/me`, { withCredentials: true });
  }

  // ——————————————————————— Admin user management ———————————————————————

  getUsers(search: string, page: number, size: number): Observable<AdminUserSummary[]> {
    const params = new HttpParams()
      .set('search', search ?? '')
      .set('page', page)
      .set('size', size);
    return this.http.get<AdminUserSummary[]>(this.apiUrl, { params, withCredentials: true });
  }

  getUserDetail(id: number): Observable<AdminUserDetail> {
    return this.http.get<AdminUserDetail>(`${this.apiUrl}/${id}`, { withCredentials: true });
  }

  updateUser(id: number, data: { name?: string; language?: string; roles?: string[] }): Observable<AdminUserDetail> {
    return this.http.put<AdminUserDetail>(`${this.apiUrl}/${id}`, data, { withCredentials: true });
  }

  setUserBlocked(id: number, blocked: boolean): Observable<AdminUserSummary> {
    return this.http.patch<AdminUserSummary>(`${this.apiUrl}/${id}/blocked`,
      { blocked }, { withCredentials: true });
  }

  deleteUser(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`, { withCredentials: true });
  }
}