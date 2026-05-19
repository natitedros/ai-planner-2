import { Component, inject } from '@angular/core';
import { Router, RouterOutlet } from '@angular/router';
import { AsyncPipe, NgIf } from '@angular/common';
import { AuthService } from './services/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, NgIf, AsyncPipe],
  template: `
    <div class="bg-slate-50 min-h-screen font-sans text-slate-900">
      <nav class="bg-white border-b border-slate-200 px-6 py-4 flex items-center justify-between">
        <span class="text-base font-semibold tracking-tight">TodoList</span>
        <div *ngIf="auth.username$ | async as username" class="flex items-center gap-5">
          <span class="text-sm text-slate-500">{{ username }}</span>
          <button (click)="auth.logout()"
            class="text-sm text-slate-500 hover:text-red-500 transition-colors">
            Sign out
          </button>
        </div>
      </nav>
      <main class="max-w-xl mx-auto px-4 pb-16">
        <router-outlet />
      </main>
    </div>
  `
})
export class AppComponent {
  auth = inject(AuthService);
}
