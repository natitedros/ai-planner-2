import { Component, Output, EventEmitter, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TaskService } from '../../services/task';

@Component({
  selector: 'app-add-task-form',
  standalone: true,
  imports: [FormsModule],
  template: `
    <div class="bg-white rounded-2xl border border-slate-200 shadow-sm p-5">
      <div class="space-y-3">
        <input [(ngModel)]="whatToDo" type="text"
          placeholder="What needs to be done?"
          class="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm
                 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent" />

        <div class="grid grid-cols-3 gap-2">
          <input [(ngModel)]="dueDate" type="date"
            class="px-3 py-2 border border-slate-300 rounded-lg text-sm
                   focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent" />

          <select [(ngModel)]="priority"
            class="px-3 py-2 border border-slate-300 rounded-lg text-sm
                   focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent">
            <option value="MEDIUM">Medium</option>
            <option value="HIGH">High</option>
            <option value="LOW">Low</option>
          </select>

          <select [(ngModel)]="category"
            class="px-3 py-2 border border-slate-300 rounded-lg text-sm
                   focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent">
            <option value="PERSONAL">Personal</option>
            <option value="WORK">Work</option>
            <option value="SCHOOL">School</option>
            <option value="HEALTH">Health</option>
            <option value="SOCIAL">Social</option>
          </select>
        </div>

        <div class="flex justify-end gap-2 pt-1">
          <button (click)="cancelled.emit()"
            class="px-4 py-2 text-sm text-slate-500 hover:text-slate-800 transition-colors">
            Cancel
          </button>
          <button (click)="submit()" [disabled]="!whatToDo || loading"
            class="bg-blue-600 hover:bg-blue-700 text-white text-sm font-medium
                   px-4 py-2 rounded-lg transition-colors disabled:opacity-50">
            {{ loading ? 'Saving...' : 'Save' }}
          </button>
        </div>
      </div>
    </div>
  `
})
export class AddTaskFormComponent {
  @Output() taskAdded = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<void>();

  whatToDo = '';
  dueDate = '';
  priority = 'MEDIUM';
  category = 'PERSONAL';
  loading = false;

  private taskService = inject(TaskService);

  submit(): void {
    if (!this.whatToDo.trim()) return;
    this.loading = true;
    this.taskService.createTask({
      whatToDo: this.whatToDo,
      dueDate: this.dueDate,
      priority: this.priority,
      category: this.category
    }).subscribe({
      next: () => {
        this.reset();
        this.taskAdded.emit();
      },
      error: () => this.loading = false
    });
  }

  private reset(): void {
    this.whatToDo = '';
    this.dueDate = '';
    this.priority = 'MEDIUM';
    this.category = 'PERSONAL';
    this.loading = false;
  }
}
