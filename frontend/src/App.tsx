import React, { useState, useEffect, useCallback } from 'react';
import { AuthProvider, useAuth } from './context/AuthContext';
import { AuthPage } from './components/auth/AuthPage';
import { Navbar } from './components/layout/Navbar';
import { HabitCard } from './components/habits/HabitCard';
import { HabitModal } from './components/habits/HabitModal';
import { WeeklyChart } from './components/dashboard/WeeklyChart';
import { HeatmapGrid } from './components/heatmap/HeatmapGrid';
import { DayCheckInModal } from './components/heatmap/DayCheckInModal';
import { api, Habit, DashboardSummary, HeatmapResponse, HeatmapDay } from './api/client';
import { CheckCircle2, Flame, TrendingUp, Sparkles, Plus, Grid } from 'lucide-react';

const DashboardContent: React.FC = () => {
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [habits, setHabits] = useState<Habit[]>([]);
  const [activeTab, setActiveTab] = useState<'today' | 'all' | 'archived'>('today');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingHabit, setEditingHabit] = useState<Habit | null>(null);

  // Heatmap state
  const [heatmapData, setHeatmapData] = useState<HeatmapResponse | null>(null);
  const [selectedHabitForHeatmap, setSelectedHabitForHeatmap] = useState<number | 'all'>('all');
  const [heatmapYear, setHeatmapYear] = useState<number>(new Date().getFullYear());
  const [clickedDay, setClickedDay] = useState<HeatmapDay | null>(null);

  const [isDark, setIsDark] = useState<boolean>(() => {
    return (
      localStorage.getItem('theme') === 'dark' ||
      (!('theme' in localStorage) && window.matchMedia('(prefers-color-scheme: dark)').matches)
    );
  });

  useEffect(() => {
    if (isDark) {
      document.documentElement.classList.add('dark');
      localStorage.setItem('theme', 'dark');
    } else {
      document.documentElement.classList.remove('dark');
      localStorage.setItem('theme', 'light');
    }
  }, [isDark]);

  const loadData = useCallback(async () => {
    try {
      const [sum, habitList] = await Promise.all([
        api.dashboard.summary(),
        api.habits.list(activeTab === 'archived' ? true : undefined),
      ]);
      setSummary(sum);
      setHabits(habitList);
    } catch (err) {
      console.error('Failed to load dashboard data', err);
    }
  }, [activeTab]);

  const loadHeatmap = useCallback(async () => {
    try {
      const currentYear = new Date().getFullYear();
      let from: string | undefined;
      let to: string | undefined;

      if (heatmapYear !== currentYear) {
        from = `${heatmapYear}-01-01`;
        to = `${heatmapYear}-12-31`;
      }

      let response: HeatmapResponse;
      if (selectedHabitForHeatmap === 'all') {
        response = await api.dashboard.getHeatmap(from, to);
      } else {
        response = await api.habits.getHeatmap(selectedHabitForHeatmap, from, to);
      }
      setHeatmapData(response);
    } catch (err) {
      console.error('Failed to load heatmap data', err);
    }
  }, [selectedHabitForHeatmap, heatmapYear]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  useEffect(() => {
    loadHeatmap();
  }, [loadHeatmap]);

  const handleToggleCheckIn = async (habitId: number, completed: boolean, note?: string) => {
    const today = new Date().toISOString().split('T')[0];
    try {
      await api.habits.checkIn(habitId, today, completed, note);
      await Promise.all([loadData(), loadHeatmap()]);
    } catch (err: any) {
      alert(err.message || 'Check-in failed');
    }
  };

  const handleRetroactiveCheckIn = async (habitId: number, date: string, completed: boolean) => {
    try {
      await api.habits.checkIn(habitId, date, completed);
      await Promise.all([loadData(), loadHeatmap()]);
      setClickedDay(null);
    } catch (err: any) {
      alert(err.message || 'Retroactive check-in failed');
    }
  };

  const handleSaveHabit = async (habitData: any) => {
    if (editingHabit) {
      await api.habits.update(editingHabit.id, habitData);
    } else {
      await api.habits.create(habitData);
    }
    setEditingHabit(null);
    await Promise.all([loadData(), loadHeatmap()]);
  };

  const handleArchive = async (habitId: number, archive: boolean) => {
    await api.habits.archive(habitId, archive);
    await Promise.all([loadData(), loadHeatmap()]);
  };

  const handleDelete = async (habitId: number) => {
    if (window.confirm('Are you sure you want to delete this habit and all its logs?')) {
      await api.habits.delete(habitId);
      await Promise.all([loadData(), loadHeatmap()]);
    }
  };

  const openCreateModal = () => {
    setEditingHabit(null);
    setIsModalOpen(true);
  };

  const openEditModal = (habit: Habit) => {
    setEditingHabit(habit);
    setIsModalOpen(true);
  };

  const displayedHabits =
    activeTab === 'today'
      ? summary?.todayHabits || []
      : habits.map((h) => {
          const todayMatch = summary?.todayHabits.find((th) => th.habit.id === h.id);
          return {
            habit: h,
            completedToday: todayMatch ? todayMatch.completedToday : false,
            noteToday: todayMatch?.noteToday,
            currentStreak: todayMatch ? todayMatch.currentStreak : 0,
          };
        });

  const maxStreak =
    summary?.todayHabits.reduce(
      (max, item) => Math.max(max, item.currentStreak),
      0
    ) || 0;

  const activeHabitForColor =
    selectedHabitForHeatmap !== 'all'
      ? habits.find((h) => h.id === selectedHabitForHeatmap)
      : null;

  return (
    <div className="min-h-screen bg-slate-50 text-slate-900 dark:bg-slate-900 dark:text-slate-100 flex flex-col font-sans transition-colors duration-150">
      <Navbar
        onNewHabit={openCreateModal}
        isDark={isDark}
        onToggleTheme={() => setIsDark(!isDark)}
      />

      <main className="flex-1 max-w-5xl mx-auto w-full p-4 sm:p-6 space-y-6">
        {/* KPI Summary Cards */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <div className="p-5 bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-2xl shadow-sm">
            <div className="flex items-center justify-between text-slate-500 dark:text-slate-400">
              <span className="text-xs font-semibold uppercase tracking-wider">Today's Progress</span>
              <CheckCircle2 className="w-4 h-4 text-emerald-500" />
            </div>
            <p className="text-2xl font-bold mt-2">
              {summary ? `${summary.todayCompletedCount} / ${summary.todayScheduledCount}` : '0 / 0'}
            </p>
            <p className="text-xs text-slate-400 mt-1">habits completed today</p>
          </div>

          <div className="p-5 bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-2xl shadow-sm">
            <div className="flex items-center justify-between text-slate-500 dark:text-slate-400">
              <span className="text-xs font-semibold uppercase tracking-wider">Top Active Streak</span>
              <Flame className="w-4 h-4 text-orange-500 fill-orange-500" />
            </div>
            <p className="text-2xl font-bold mt-2">
              {maxStreak} <span className="text-sm font-normal text-slate-400">days</span>
            </p>
            <p className="text-xs text-slate-400 mt-1">current best habit run</p>
          </div>

          <div className="p-5 bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-2xl shadow-sm">
            <div className="flex items-center justify-between text-slate-500 dark:text-slate-400">
              <span className="text-xs font-semibold uppercase tracking-wider">Today's Rate</span>
              <TrendingUp className="w-4 h-4 text-blue-500" />
            </div>
            <p className="text-2xl font-bold mt-2">
              {summary ? `${summary.overallCompletionRate}%` : '0%'}
            </p>
            <p className="text-xs text-slate-400 mt-1">daily completion percentage</p>
          </div>
        </div>

        {/* GitHub-Style Contribution Heatmap */}
        {heatmapData && (
          <div>
            <div className="flex items-center justify-between mb-2">
              <div className="flex items-center gap-1.5 text-xs text-slate-500 dark:text-slate-400 font-semibold uppercase tracking-wider">
                <Grid className="w-3.5 h-3.5 text-emerald-500" />
                <span>Habit Heatmap Matrix</span>
              </div>

              {/* View Selector (Overall or specific habit) */}
              <select
                value={selectedHabitForHeatmap}
                onChange={(e) => {
                  const val = e.target.value;
                  setSelectedHabitForHeatmap(val === 'all' ? 'all' : Number(val));
                }}
                className="px-2.5 py-1 text-xs bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl text-slate-700 dark:text-slate-300 focus:outline-none focus:ring-1 focus:ring-emerald-500"
              >
                <option value="all">Overall Consistency (All Habits)</option>
                {habits.map((h) => (
                  <option key={h.id} value={h.id}>
                    {h.name}
                  </option>
                ))}
              </select>
            </div>

            <HeatmapGrid
              days={heatmapData.days}
              colorBase={activeHabitForColor?.color || '#10B981'}
              title={
                activeHabitForColor
                  ? `${activeHabitForColor.name} Consistency`
                  : 'Overall Habit Consistency'
              }
              year={heatmapYear}
              onYearChange={(y) => setHeatmapYear(y)}
              onDayClick={(day) => setClickedDay(day)}
            />
          </div>
        )}

        {/* 7-Day Activity Chart */}
        {summary && summary.weeklyOverview && summary.weeklyOverview.length > 0 && (
          <WeeklyChart data={summary.weeklyOverview} />
        )}

        {/* Habits Header & Tabs */}
        <div className="flex items-center justify-between border-b border-slate-200 dark:border-slate-800 pb-3">
          <div className="flex items-center gap-2">
            <button
              onClick={() => setActiveTab('today')}
              className={`px-3.5 py-1.5 text-xs font-bold rounded-xl transition ${
                activeTab === 'today'
                  ? 'bg-emerald-500 text-white shadow-sm'
                  : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
              }`}
            >
              Today's Habits
            </button>
            <button
              onClick={() => setActiveTab('all')}
              className={`px-3.5 py-1.5 text-xs font-bold rounded-xl transition ${
                activeTab === 'all'
                  ? 'bg-emerald-500 text-white shadow-sm'
                  : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
              }`}
            >
              All Habits
            </button>
            <button
              onClick={() => setActiveTab('archived')}
              className={`px-3.5 py-1.5 text-xs font-bold rounded-xl transition ${
                activeTab === 'archived'
                  ? 'bg-emerald-500 text-white shadow-sm'
                  : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
              }`}
            >
              Archived
            </button>
          </div>

          <span className="text-xs text-slate-400 font-medium">
            {displayedHabits.length} {displayedHabits.length === 1 ? 'habit' : 'habits'}
          </span>
        </div>

        {/* Habits List */}
        {displayedHabits.length > 0 ? (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {displayedHabits.map((item) => (
              <HabitCard
                key={item.habit.id}
                habit={item.habit}
                completedToday={item.completedToday}
                currentStreak={item.currentStreak}
                noteToday={item.noteToday}
                onToggleCheckIn={handleToggleCheckIn}
                onEdit={openEditModal}
                onArchive={handleArchive}
                onDelete={handleDelete}
              />
            ))}
          </div>
        ) : (
          <div className="p-12 text-center bg-white dark:bg-slate-800 border border-dashed border-slate-300 dark:border-slate-700 rounded-3xl">
            <div className="inline-flex p-3 bg-emerald-50 dark:bg-emerald-500/10 text-emerald-500 rounded-2xl mb-3">
              <Sparkles className="w-6 h-6" />
            </div>
            <h3 className="text-base font-semibold text-slate-900 dark:text-white">
              {activeTab === 'archived' ? 'No archived habits' : 'No habits scheduled for today'}
            </h3>
            <p className="text-xs text-slate-500 dark:text-slate-400 max-w-sm mx-auto mt-1 mb-4">
              {activeTab === 'archived'
                ? 'Habits you archive will appear here.'
                : 'Create your first habit to begin building daily routines and tracking streaks.'}
            </p>
            {activeTab !== 'archived' && (
              <button
                onClick={openCreateModal}
                className="inline-flex items-center gap-1.5 px-4 py-2 bg-emerald-500 hover:bg-emerald-600 text-white text-xs font-semibold rounded-xl shadow-md shadow-emerald-500/20 transition"
              >
                <Plus className="w-3.5 h-3.5" />
                Create Habit
              </button>
            )}
          </div>
        )}
      </main>

      <HabitModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onSave={handleSaveHabit}
        initialHabit={editingHabit}
      />

      <DayCheckInModal
        day={clickedDay}
        habits={habits}
        onClose={() => setClickedDay(null)}
        onCheckIn={handleRetroactiveCheckIn}
      />
    </div>
  );
};

export function App() {
  const { isAuthenticated } = useAuth();
  return isAuthenticated ? <DashboardContent /> : <AuthPage />;
}

export default function Root() {
  return (
    <AuthProvider>
      <App />
    </AuthProvider>
  );
}
