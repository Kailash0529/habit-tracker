// API Client with JWT authorization handling

const API_BASE = (import.meta.env.VITE_API_URL ? `${import.meta.env.VITE_API_URL}/api/v1` : '/api/v1');

export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data: T;
  timestamp: string;
}

export interface User {
  id: number;
  email: string;
  name: string;
  createdAt: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresInMs: number;
  user: User;
}

export interface Habit {
  id: number;
  userId: number;
  name: string;
  description?: string;
  frequencyType: 'DAILY' | 'WEEKDAYS' | 'SPECIFIC_DAYS';
  daysOfWeek?: string;
  targetTime?: string;
  color: string;
  archived: boolean;
  createdAt: string;
}

export interface HabitLog {
  id: number;
  habitId: number;
  logDate: string;
  completed: boolean;
  note?: string;
}

export interface HabitStats {
  habitId: number;
  currentStreak: number;
  longestStreak: number;
  completionRate: number;
  totalCompletedDays: number;
  totalScheduledDays: number;
}

export interface HeatmapDay {
  date: string;
  completedCount: number;
  scheduledCount: number;
  level: number;
  scheduled: boolean;
}

export interface HeatmapResponse {
  startDate: string;
  endDate: string;
  totalCompleted: number;
  totalScheduled: number;
  days: HeatmapDay[];
}

export interface TodayHabit {
  habit: Habit;
  completedToday: boolean;
  noteToday?: string;
  currentStreak: number;
}

export interface DaySummary {
  date: string;
  dayOfWeek: string;
  completedCount: number;
  scheduledCount: number;
}

export interface DashboardSummary {
  todayDate: string;
  todayCompletedCount: number;
  todayScheduledCount: number;
  overallCompletionRate: number;
  todayHabits: TodayHabit[];
  weeklyOverview: DaySummary[];
}

export function getToken(): string | null {
  return localStorage.getItem('token');
}

export function setToken(token: string): void {
  localStorage.setItem('token', token);
}

export function removeToken(): void {
  localStorage.removeItem('token');
}

async function request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
  const token = getToken();
  const headers = new Headers(options.headers || {});
  headers.set('Content-Type', 'application/json');

  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  const response = await fetch(`${API_BASE}${endpoint}`, {
    ...options,
    headers,
  });

  const data = await response.json().catch(() => null);

  if (!response.ok) {
    if (response.status === 401) {
      removeToken();
      window.dispatchEvent(new Event('auth:unauthorized'));
    }
    const errorMessage = data?.message || data?.error || `HTTP ${response.status}: Request failed`;
    throw new Error(errorMessage);
  }

  return (data as ApiResponse<T>).data;
}

export const api = {
  auth: {
    register: (body: { name: string; email: string; password: string }) =>
      request<AuthResponse>('/auth/register', { method: 'POST', body: JSON.stringify(body) }),
    login: (body: { email: string; password: string }) =>
      request<AuthResponse>('/auth/login', { method: 'POST', body: JSON.stringify(body) }),
  },
  habits: {
    list: (archived?: boolean) => {
      const query = archived !== undefined ? `?archived=${archived}` : '';
      return request<Habit[]>(`/habits${query}`);
    },
    get: (id: number) => request<Habit>(`/habits/${id}`),
    create: (body: {
      name: string;
      description?: string;
      frequencyType: 'DAILY' | 'WEEKDAYS' | 'SPECIFIC_DAYS';
      daysOfWeek?: string;
      targetTime?: string;
      color?: string;
    }) => request<Habit>('/habits', { method: 'POST', body: JSON.stringify(body) }),
    update: (
      id: number,
      body: {
        name: string;
        description?: string;
        frequencyType: 'DAILY' | 'WEEKDAYS' | 'SPECIFIC_DAYS';
        daysOfWeek?: string;
        targetTime?: string;
        color?: string;
        archived?: boolean;
      }
    ) => request<Habit>(`/habits/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
    delete: (id: number) => request<void>(`/habits/${id}`, { method: 'DELETE' }),
    archive: (id: number, archive: boolean) =>
      request<Habit>(`/habits/${id}/archive?archive=${archive}`, { method: 'PATCH' }),
    checkIn: (id: number, date: string, completed: boolean, note?: string) =>
      request<HabitLog>(`/habits/${id}/logs/${date}`, {
        method: 'PUT',
        body: JSON.stringify({ completed, note }),
      }),
    stats: (id: number) => request<HabitStats>(`/habits/${id}/stats`),
    getHeatmap: (id: number, from?: string, to?: string) => {
      const params = new URLSearchParams();
      if (from) params.set('from', from);
      if (to) params.set('to', to);
      const query = params.toString() ? `?${params.toString()}` : '';
      return request<HeatmapResponse>(`/habits/${id}/heatmap${query}`);
    },
  },
  dashboard: {
    summary: () => request<DashboardSummary>('/dashboard/summary'),
    getHeatmap: (from?: string, to?: string) => {
      const params = new URLSearchParams();
      if (from) params.set('from', from);
      if (to) params.set('to', to);
      const query = params.toString() ? `?${params.toString()}` : '';
      return request<HeatmapResponse>(`/dashboard/heatmap${query}`);
    },
  },
};
