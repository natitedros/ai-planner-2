import { Component, Input, Output, EventEmitter } from '@angular/core';
import { NgIf, NgFor, NgClass, TitleCasePipe } from '@angular/common';
import { Task } from '../../models/task.model';

@Component({
  selector: 'app-task-card',
  standalone: true,
  imports: [NgIf, NgFor, NgClass, TitleCasePipe],
  template: `
    <div class="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">

      <!-- Parent task row -->
      <div class="flex items-start gap-3 p-4">

        <!-- Toggle button -->
        <button (click)="statusToggled.emit(task)"
          class="mt-0.5 flex-shrink-0 w-5 h-5 rounded-full border-2 flex items-center
                 justify-center transition-colors"
          [ngClass]="task.status === 'COMPLETED'
            ? 'bg-blue-600 border-blue-600 text-white'
            : 'border-slate-300 hover:border-blue-500'">
          <svg *ngIf="task.status === 'COMPLETED'"
            class="w-2.5 h-2.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="3"
              d="M5 13l4 4L19 7"/>
          </svg>
        </button>

        <!-- Task content -->
        <div class="flex-1 min-w-0">
          <p class="text-sm font-medium leading-snug"
            [ngClass]="task.status === 'COMPLETED'
              ? 'line-through text-slate-400' : 'text-slate-800'">
            {{ task.whatToDo }}
          </p>
          <div class="flex flex-wrap items-center gap-1.5 mt-2">
            <span *ngIf="task.dueDate"
              class="text-xs text-slate-500 bg-slate-100 px-2 py-0.5 rounded-full">
              {{ task.dueDate }}
            </span>
            <span [class]="priorityClass(task.priority)">{{ task.priority | titlecase }}</span>
            <span [class]="categoryClass(task.category)">{{ task.category | titlecase }}</span>
            <span *ngIf="subtasks.length"
              class="text-xs text-slate-400 bg-slate-100 px-2 py-0.5 rounded-full">
              {{ subtasks.length }} subtask{{ subtasks.length !== 1 ? 's' : '' }}
            </span>
          </div>
        </div>

        <!-- Actions -->
        <div class="flex items-center gap-1 flex-shrink-0">
          <button *ngIf="!subtasks.length" (click)="onDecompose()"
            [disabled]="decomposing"
            class="text-xs font-medium text-violet-600 hover:text-violet-800
                   border border-violet-200 hover:bg-violet-50 px-2.5 py-1
                   rounded-lg transition-colors disabled:opacity-50">
            {{ decomposing ? 'Working…' : 'Decompose' }}
          </button>
          <button (click)="taskDeleted.emit(task.id)"
            class="text-slate-300 hover:text-red-400 px-2 py-1 rounded-lg
                   transition-colors text-base leading-none font-medium">✕
          </button>
        </div>
      </div>

      <!-- Subtasks -->
      <div *ngIf="subtasks.length" class="border-t border-slate-100">
        <div *ngFor="let sub of subtasks; let last = last"
          class="flex items-start gap-3 pl-10 pr-4 py-3"
          [ngClass]="!last ? 'border-b border-slate-50' : ''">

          <button (click)="statusToggled.emit(sub)"
            class="mt-0.5 flex-shrink-0 w-4 h-4 rounded-full border-2 flex items-center
                   justify-center transition-colors"
            [ngClass]="sub.status === 'COMPLETED'
              ? 'bg-blue-600 border-blue-600 text-white'
              : 'border-slate-300 hover:border-blue-500'">
            <svg *ngIf="sub.status === 'COMPLETED'"
              class="w-2 h-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="3"
                d="M5 13l4 4L19 7"/>
            </svg>
          </button>

          <div class="flex-1 min-w-0">
            <p class="text-sm leading-snug"
              [ngClass]="sub.status === 'COMPLETED'
                ? 'line-through text-slate-400' : 'text-slate-700'">
              {{ sub.whatToDo }}
            </p>
            <div class="flex flex-wrap items-center gap-1.5 mt-1.5">
              <span *ngIf="sub.dueDate"
                class="text-xs text-slate-500 bg-slate-100 px-2 py-0.5 rounded-full">
                {{ sub.dueDate }}
              </span>
              <span [class]="priorityClass(sub.priority)">{{ sub.priority | titlecase }}</span>
              <span [class]="categoryClass(sub.category)">{{ sub.category | titlecase }}</span>
            </div>
          </div>

          <button (click)="taskDeleted.emit(sub.id)"
            class="text-slate-300 hover:text-red-400 px-2 py-1 rounded-lg
                   transition-colors text-base leading-none font-medium">✕
          </button>
        </div>
      </div>
    </div>
  `
})
export class TaskCardComponent {
  @Input() task!: Task;
  @Input() subtasks: Task[] = [];
  @Output() statusToggled = new EventEmitter<Task>();
  @Output() taskDeleted = new EventEmitter<number>();
  @Output() decomposed = new EventEmitter<number>();

  decomposing = false;

  onDecompose(): void {
    this.decomposing = true;
    this.decomposed.emit(this.task.id);
    setTimeout(() => this.decomposing = false, 3000);
  }

  priorityClass(p: string): string {
    const map: Record<string, string> = {
      HIGH:   'text-xs font-medium px-2 py-0.5 rounded-full bg-red-100 text-red-700',
      MEDIUM: 'text-xs font-medium px-2 py-0.5 rounded-full bg-amber-100 text-amber-700',
      LOW:    'text-xs font-medium px-2 py-0.5 rounded-full bg-green-100 text-green-700',
    };
    return map[p] ?? map['MEDIUM'];
  }

  categoryClass(c: string): string {
    const map: Record<string, string> = {
      WORK:     'text-xs font-medium px-2 py-0.5 rounded-full bg-blue-100 text-blue-700',
      PERSONAL: 'text-xs font-medium px-2 py-0.5 rounded-full bg-violet-100 text-violet-700',
      HEALTH:   'text-xs font-medium px-2 py-0.5 rounded-full bg-teal-100 text-teal-700',
      SCHOOL:   'text-xs font-medium px-2 py-0.5 rounded-full bg-yellow-100 text-yellow-700',
      SOCIAL:   'text-xs font-medium px-2 py-0.5 rounded-full bg-orange-100 text-orange-700',
    };
    return map[c] ?? map['PERSONAL'];
  }
}
