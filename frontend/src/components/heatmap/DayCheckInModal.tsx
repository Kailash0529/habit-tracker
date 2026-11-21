import React, { useState } from 'react';
import { Habit, HeatmapDay } from '../../api/client';
import { X, Check } from 'lucide-react';

interface DayCheckInModalProps {
  day: HeatmapDay | null;
  habits: Habit[];
  onClose: () => void;
  onCheckIn: (habitId: number, date: string, completed: boolean) => Promise<void>;
}

export const DayCheckInModal: React.FC<DayCheckInModalProps> = ({
  day,
  habits,
  onClose,
  onCheckIn,
}) => {
  const [loadingHabitId, setLoadingHabitId] = useState<number | null>(null);

  if (!day) return null;

  const handleToggle = async (habitId: number, currentCompleted: boolean) => {
    setLoadingHabitId(habitId);
    try {
      await onCheckIn(habitId, day.date, !currentCompleted);
    } finally {
      setLoadingHabitId(null);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-fadeIn">
      <div className="w-full max-w-md bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-2xl shadow-2xl overflow-hidden">
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-200 dark:border-slate-700">
          <div>
            <h2 className="text-base font-bold text-slate-800 dark:text-white">
              Check-ins for {day.date}
            </h2>
            <p className="text-xs text-slate-400">
              {day.completedCount} of {day.scheduledCount} habits completed
            </p>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-700 transition"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="p-6 max-h-96 overflow-y-auto space-y-3">
          {habits.map((habit) => {
            const isCompleted = day.completedCount > 0; // fallback or toggle
            return (
              <div
                key={habit.id}
                className="flex items-center justify-between p-3.5 rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-900/50"
              >
                <div className="flex items-center gap-3">
                  <div
                    className="w-2.5 h-2.5 rounded-full"
                    style={{ backgroundColor: habit.color || '#10B981' }}
                  />
                  <div>
                    <p className="text-sm font-semibold text-slate-800 dark:text-white">
                      {habit.name}
                    </p>
                    <p className="text-[11px] text-slate-400">
                      {habit.frequencyType === 'DAILY' ? 'Daily' : habit.frequencyType}
                    </p>
                  </div>
                </div>

                <button
                  onClick={() => handleToggle(habit.id, isCompleted)}
                  disabled={loadingHabitId === habit.id}
                  className="px-3 py-1.5 rounded-lg text-xs font-semibold flex items-center gap-1.5 transition bg-emerald-500 hover:bg-emerald-600 text-white shadow-sm"
                >
                  <Check className="w-3.5 h-3.5" />
                  <span>Toggle Check-in</span>
                </button>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
};
