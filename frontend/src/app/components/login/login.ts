import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgIf } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, NgIf, RouterLink],
  template: `
    <div class="pt-16 flex flex-col items-center">
      <div class="w-full max-w-sm bg-white rounded-2xl border border-slate-200 shadow-sm p-8">
        <h1 class="text-lg font-semibold mb-6 text-center">Sign in</h1>

        <div *ngIf="error"
          class="mb-4 px-4 py-3 rounded-lg bg-red-50 border border-red-200 text-red-700 text-sm">
          {{ error }}
        </div>

        <div class="space-y-3">
          <input [(ngModel)]="username" type="text" placeholder="Username"
            class="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm
                   focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent" />
          <input [(ngModel)]="password" type="password" placeholder="Password"
            class="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm
                   focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent" />
          <button (click)="login()" [disabled]="loading"
            class="w-full bg-blue-600 hover:bg-blue-700 text-white text-sm font-medium
                   py-2 rounded-lg transition-colors disabled:opacity-50">
            {{ loading ? 'Signing in...' : 'Sign in' }}
          </button>
        </div>

        <p class="text-center text-sm text-slate-500 mt-5">
          No account?
          <a routerLink="/register" class="text-blue-600 hover:underline">Register</a>
        </p>
      </div>
    </div>
  `
})
export class LoginComponent {
  username = '';
  password = '';
  error = '';
  loading = false;

  private auth = inject(AuthService);
  private router = inject(Router);

  login(): void {
    this.error = '';
    this.loading = true;
    this.auth.login({ username: this.username, password: this.password }).subscribe({
      next: () => this.router.navigate(['/tasks']),
      error: err => {
        this.error = err.error?.message || 'Login failed';
        this.loading = false;
      }
    });
  }
}
