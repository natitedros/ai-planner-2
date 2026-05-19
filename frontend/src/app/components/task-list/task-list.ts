import { Component, OnInit, inject } from '@angular/core';
import { NgIf, NgFor } from '@angular/common';
import { TaskService } from '../../services/task';
import { Task } from '../../models/task.model';
import { TaskCardComponent } from '../task-card/task-card';
import { AddTaskFormComponent } from '../add-task-form/add-task-form';

@Component({
  selector: 'app-task-list',
  standalone: true,
  imports: [NgIf, NgFor, TaskCardComponent, AddTaskFormComponent],
  template: `
    <div class="pt-8">
      <div class="flex items-center justify-between mb-5">
        <h1 class="text-lg font-semibold">My Tasks</h1>
        <button (click)="showForm = !showForm"
          class="bg-blue-600 hover:bg-blue-700 text-white text-sm font-medium
                 px-4 py-2 rounded-lg transition-colors">
          {{ showForm ? '✕ Cancel' : '+ Add task' }}
        </button>
      </div>

      <app-add-task-form *ngIf="showForm"
        (taskAdded)="onTaskAdded()"
        (cancelled)="showForm = false"
        class="block mb-6">
      </app-add-task-form>

      <div *ngIf="loading" class="text-center py-20 text-slate-400 text-sm">
        Loading tasks...
      </div>

      <div *ngIf="!loading && parents.length === 0"
        class="text-center py-20 text-slate-400">
        <div class="text-5xl mb-3">✓</div>
        <p class="text-sm">Nothing here yet. Add your first task above.</p>
      </div>

      <div *ngIf="!loading && parents.length > 0" class="space-y-3">
        <app-task-card
          *ngFor="let task of parents"
          [task]="task"
          [subtasks]="subtasksMap[task.id] || []"
          (statusToggled)="onStatusToggled($event)"
          (taskDeleted)="onTaskDeleted($event)"
          (decomposed)="onDecomposed($event)">
        </app-task-card>
      </div>
    </div>
  `
})
export class TaskListComponent implements OnInit {
  parents: Task[] = [];
  subtasksMap: Record<number, Task[]> = {};
  showForm = false;
  loading = false;

  private taskService = inject(TaskService);

  ngOnInit(): void {
    this.loadTasks();
  }

  loadTasks(): void {
    this.loading = true;
    this.taskService.getTasks().subscribe({
      next: tasks => {
        this.parents = tasks.filter(t => !t.parentId);
        this.subtasksMap = {};
        tasks.filter(t => t.parentId).forEach(t => {
          if (!this.subtasksMap[t.parentId!]) this.subtasksMap[t.parentId!] = [];
          this.subtasksMap[t.parentId!].push(t);
        });
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }

  onTaskAdded(): void {
    this.showForm = false;
    this.loadTasks();
  }

  onStatusToggled(task: Task): void {
    const newStatus = task.status === 'COMPLETED' ? 'PENDING' : 'COMPLETED';
    this.taskService.updateStatus(task.id, newStatus).subscribe(() => this.loadTasks());
  }

  onTaskDeleted(id: number): void {
    this.taskService.deleteTask(id).subscribe(() => this.loadTasks());
  }

  onDecomposed(id: number): void {
    this.taskService.decompose(id).subscribe(() => this.loadTasks());
  }
}
