import React, { useState } from 'react';
import { Habit } from '../../api/client';
import { Check, Flame, Clock, Archive, Trash2, Edit3, MessageSquare } from 'lucide-react';

interface HabitCardProps {
  habit: Habit;
  completedToday: boolean;
  currentStreak: number;
  noteToday?: string;
  onToggleCheckIn: (habitId: number, completed: boolean, note?: string) => Promise<void>;
  onEdit: (habit: Habit) => void;
  onArchive: (habitId: number, archive: boolean) => Promise<void>;
  onDelete: (habitId: number) => Promise<void>;
}

export const HabitCard: React.FC<HabitCardProps> = ({
  habit,
  completedToday,
  currentStreak,
  noteToday,
  onToggleCheckIn,
  onEdit,
  onArchive,
  onDelete,
}) => {
  const [showNoteInput, setShowNoteInput] = useState(false);
  const [note, setNote] = useState(noteToday || '');
  const [isLoading, setIsLoading] = useState(false);

  const handleToggle = async () => {
    setIsLoading(true);
    try {
      await onToggleCheckIn(habit.id, !completedToday, note.trim() || undefined);
    } finally {
      setIsLoading(false);
    }
  };

  const handleSaveNote = async () => {
    if (completedToday) {
      await onToggleCheckIn(habit.id, true, note.trim() || undefined);
    }
    setShowNoteInput(false);
  };

  return (
    <div
      className={`relative p-5 rounded-2xl bg-white dark:bg-slate-800 border transition-all duration-200 shadow-sm hover:shadow-md ${
        completedToday
          ? 'border-emerald-500/40 dark:border-emerald-500/30 ring-1 ring-emerald-500/10'
          : 'border-slate-200 dark:border-slate-700'
      }`}
    >
      <div className="flex items-start justify-between gap-3">
        {/* Left Color Bar & Habit Info */}
        <div className="flex items-start gap-3.5 flex-1 min-w-0">
          <div
            className="w-1.5 self-stretch rounded-full shrink-0"
            style={{ backgroundColor: habit.color || '#10B981' }}
          />

          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-2 flex-wrap">
              <h3 className="font-semibold text-slate-900 dark:text-white truncate text-base">
                {habit.name}
              </h3>
              {habit.archived && (
                <span className="px-2 py-0.5 text-[10px] uppercase font-bold tracking-wider rounded-md bg-amber-500/10 text-amber-500">
                  Archived
                </span>
              )}
            </div>

            {habit.description && (
              <p className="text-xs text-slate-500 dark:text-slate-400 mt-1 line-clamp-2">
                {habit.description}
              </p>
            )}

            <div className="flex items-center gap-3 mt-3 text-xs text-slate-500 dark:text-slate-400 flex-wrap">
              <span className="inline-flex items-center gap-1 font-medium bg-slate-100 dark:bg-slate-700/60 px-2 py-0.5 rounded-md">
                {habit.frequencyType === 'DAILY'
                  ? 'Daily'
                  : habit.frequencyType === 'WEEKDAYS'
                  ? 'Weekdays'
                  : habit.daysOfWeek || 'Specific Days'}
              </span>

              {habit.targetTime && (
                <span className="inline-flex items-center gap-1">
                  <Clock className="w-3.5 h-3.5 text-slate-400" />
                  {habit.targetTime}
                </span>
              )}

              <span className="inline-flex items-center gap-1 font-semibold text-orange-500 dark:text-orange-400">
                <Flame className="w-3.5 h-3.5 fill-current" />
                {currentStreak} {currentStreak === 1 ? 'day' : 'days'}
              </span>
            </div>
          </div>
        </div>

        {/* Check-in Toggle Button */}
        <div className="flex items-center gap-2 shrink-0">
          <button
            onClick={handleToggle}
            disabled={isLoading || habit.archived}
            className={`w-11 h-11 rounded-xl flex items-center justify-center transition-all ${
              completedToday
                ? 'bg-emerald-500 text-white shadow-lg shadow-emerald-500/25 scale-105'
                : 'border-2 border-slate-300 dark:border-slate-600 text-slate-300 dark:text-slate-600 hover:border-emerald-500 hover:text-emerald-500'
            }`}
            title={completedToday ? 'Mark incomplete' : 'Check in for today'}
          >
            <Check className={`w-6 h-6 stroke-[2.5] ${completedToday ? 'text-white' : ''}`} />
          </button>
        </div>
      </div>

      {/* Note preview or input */}
      {showNoteInput ? (
        <div className="mt-4 pt-3 border-t border-slate-100 dark:border-slate-700/60 flex items-center gap-2">
          <input
            type="text"
            value={note}
            onChange={(e) => setNote(e.target.value)}
            placeholder="Add a quick note..."
            className="flex-1 px-3 py-1.5 text-xs bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg text-slate-800 dark:text-white focus:outline-none focus:ring-1 focus:ring-emerald-500"
          />
          <button
            onClick={handleSaveNote}
            className="px-3 py-1.5 bg-emerald-500 text-white text-xs font-semibold rounded-lg hover:bg-emerald-600 transition"
          >
            Save
          </button>
          <button
            onClick={() => setShowNoteInput(false)}
            className="px-2 py-1.5 text-slate-400 text-xs hover:text-slate-600 transition"
          >
            Cancel
          </button>
        </div>
      ) : (
        noteToday && (
          <div className="mt-3 pt-2.5 border-t border-slate-100 dark:border-slate-700/60 text-xs text-slate-500 dark:text-slate-400 flex items-center gap-1.5 italic">
            <MessageSquare className="w-3 h-3 shrink-0 text-slate-400" />
            <span className="truncate">"{noteToday}"</span>
          </div>
        )
      )}

      {/* Action Footer */}
      <div className="mt-4 pt-3 border-t border-slate-100 dark:border-slate-700/60 flex items-center justify-between text-xs text-slate-400">
        <button
          onClick={() => setShowNoteInput(!showNoteInput)}
          className="hover:text-slate-600 dark:hover:text-slate-200 flex items-center gap-1 transition"
        >
          <MessageSquare className="w-3.5 h-3.5" />
          <span>{noteToday ? 'Edit Note' : 'Add Note'}</span>
        </button>

        <div className="flex items-center gap-2">
          <button
            onClick={() => onEdit(habit)}
            className="p-1 hover:text-slate-600 dark:hover:text-slate-200 rounded transition"
            title="Edit Habit"
          >
            <Edit3 className="w-3.5 h-3.5" />
          </button>
          <button
            onClick={() => onArchive(habit.id, !habit.archived)}
            className="p-1 hover:text-amber-500 rounded transition"
            title={habit.archived ? 'Unarchive' : 'Archive'}
          >
            <Archive className="w-3.5 h-3.5" />
          </button>
          <button
            onClick={() => onDelete(habit.id)}
            className="p-1 hover:text-rose-500 rounded transition"
            title="Delete Habit"
          >
            <Trash2 className="w-3.5 h-3.5" />
          </button>
        </div>
      </div>
    </div>
  );
};
