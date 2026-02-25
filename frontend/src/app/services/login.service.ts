import { HttpClient } from "@angular/common/http";
import { UserDTO } from "./users.service";
import { BehaviorSubject, Observable, of } from "rxjs";
import { catchError, tap } from "rxjs/operators";
import { Inject, Injectable, PLATFORM_ID } from "@angular/core";
import { isPlatformBrowser } from "@angular/common";

const BASE_URL = "/api/auth/";

@Injectable({
  providedIn: 'root'
})
export class LoginService {

  private loggedInSubject = new BehaviorSubject<boolean>(false);
  public loggedIn$ = this.loggedInSubject.asObservable();

  private user?: UserDTO;

  constructor(
    private http: HttpClient,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {
    if (isPlatformBrowser(this.platformId)) {
      const savedUser = localStorage.getItem("user");
      if (savedUser) {
        this.user = JSON.parse(savedUser);
        this.loggedInSubject.next(true);
      } else {
        this.reqIsLogged().subscribe();
      }
    }
  }


  public login(user: string, pass: string): Observable<any> {
    return this.http.post(BASE_URL + "login", { username: user, password: pass }, { withCredentials: true }).pipe(
      tap(() => {
        // después del login, refrescar el usuario
        this.reqIsLogged().subscribe();
      })
    );
  }

  public logout(): Observable<any> {
    return this.http.post(BASE_URL + "logout", {}, { withCredentials: true }).pipe(
      tap(() => {
        console.log("LOGOUT: Successfully");
        this.user = undefined;
        localStorage.removeItem("user");
        this.loggedInSubject.next(false);
      })
    );
  }

  public reqIsLogged(): Observable<UserDTO | null> {
    return this.http.get<UserDTO>("/api/users/me", { withCredentials: true }).pipe(
      tap((user) => {
        this.user = user;
        localStorage.setItem("user", JSON.stringify(user));
        this.loggedInSubject.next(true);
      }),
      catchError((error) => {
        if (error.status !== 401) {
          console.error("Error checking session: ", error);
        }
        this.user = undefined;
        localStorage.removeItem("user");
        this.loggedInSubject.next(false);
        return of(null);
      })
    );
  }

  public currentUser(): UserDTO | undefined {
    return this.user;
  }

  public isLogged(): boolean {
    return this.loggedInSubject.getValue();
  }

  public isRoleUser(): boolean {
    return this.currentUser()?.roles.includes("USER") ?? false;
  }

  public isRoleAdmin(): boolean {
    return this.currentUser()?.roles.includes("ADMIN") ?? false;
  }
}