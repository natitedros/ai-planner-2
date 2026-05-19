import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Task, CreateTaskRequest } from '../models/task.model';

@Injectable({ providedIn: 'root' })
export class TaskService {

  private apiUrl = `${environment.apiUrl}/api/items`;

  constructor(private http: HttpClient) {}

  getTasks(): Observable<Task[]> {
    return this.http.get<Task[]>(this.apiUrl);
  }

  createTask(req: CreateTaskRequest): Observable<Task> {
    return this.http.post<Task>(this.apiUrl, req);
  }

  updateStatus(id: number, status: 'PENDING' | 'COMPLETED'): Observable<Task> {
    return this.http.put<Task>(`${this.apiUrl}/${id}`, { status });
  }

  deleteTask(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${id}`);
  }

  decompose(id: number): Observable<{ subtasks: Task[] }> {
    return this.http.post<{ subtasks: Task[] }>(`${this.apiUrl}/${id}/decompose`, {});
  }
}
