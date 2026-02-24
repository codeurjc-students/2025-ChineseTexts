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

@Injectable({
  providedIn: 'root'
})
export class UserService {

  private apiUrl = '/api/users'; // Ajusta si tu backend usa otro path

  constructor(private http: HttpClient) {}

  register(user: UserDTO): Observable<any> {
    return this.http.post(`${this.apiUrl}/signup`, user);
  }
}
