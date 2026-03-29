import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
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
}