import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgIf } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [FormsModule, NgIf, RouterLink],
  template: `
    <div class="pt-16 flex flex-col items-center">
      <div class="w-full max-w-sm bg-white rounded-2xl border border-slate-200 shadow-sm p-8">
        <h1 class="text-lg font-semibold mb-6 text-center">Create account</h1>

        <div *ngIf="error"
          class="mb-4 px-4 py-3 rounded-lg bg-red-50 border border-red-200 text-red-700 text-sm">
          {{ error }}
        </div>
        <div *ngIf="success"
          class="mb-4 px-4 py-3 rounded-lg bg-green-50 border border-green-200 text-green-700 text-sm">
          Account created! Redirecting to login...
        </div>

        <div class="space-y-3">
          <input [(ngModel)]="username" type="text" placeholder="Username"
            class="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm
                   focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent" />
          <input [(ngModel)]="email" type="email" placeholder="Email"
            class="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm
                   focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent" />
          <input [(ngModel)]="password" type="password" placeholder="Password"
            class="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm
                   focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent" />
          <button (click)="register()" [disabled]="loading"
            class="w-full bg-blue-600 hover:bg-blue-700 text-white text-sm font-medium
                   py-2 rounded-lg transition-colors disabled:opacity-50">
            {{ loading ? 'Creating...' : 'Create account' }}
          </button>
        </div>

        <p class="text-center text-sm text-slate-500 mt-5">
          Already have an account?
          <a routerLink="/login" class="text-blue-600 hover:underline">Sign in</a>
        </p>
      </div>
    </div>
  `
})
export class RegisterComponent {
  username = '';
  email = '';
  password = '';
  error = '';
  success = false;
  loading = false;

  private auth = inject(AuthService);
  private router = inject(Router);

  register(): void {
    this.error = '';
    this.loading = true;
    this.auth.register({ username: this.username, email: this.email, password: this.password })
      .subscribe({
        next: () => {
          this.success = true;
          setTimeout(() => this.router.navigate(['/login']), 1500);
        },
        error: err => {
          this.error = err.error?.message || 'Registration failed';
          this.loading = false;
        }
      });
  }
}
