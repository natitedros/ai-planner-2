export interface Task {
  id: number;
  whatToDo: string;
  dueDate: string;
  priority: 'HIGH' | 'MEDIUM' | 'LOW';
  category: 'SCHOOL' | 'WORK' | 'PERSONAL' | 'HEALTH' | 'SOCIAL';
  status: 'PENDING' | 'COMPLETED';
  parentId: number | null;
  subtasks?: Task[];
}

export interface CreateTaskRequest {
  whatToDo: string;
  dueDate: string;
  priority: string;
  category: string;
}
