import React, { useState } from 'react';
import { HeatmapDay } from '../../api/client';
import { ChevronLeft, ChevronRight } from 'lucide-react';

interface HeatmapGridProps {
  days: HeatmapDay[];
  colorBase?: string;
  onDayClick?: (day: HeatmapDay) => void;
  title?: string;
  year?: number;
  onYearChange?: (year: number) => void;
}

const MONTH_NAMES = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
const WEEKDAYS = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];

export const HeatmapGrid: React.FC<HeatmapGridProps> = ({
  days,
  colorBase = '#10B981',
  onDayClick,
  title,
  year = new Date().getFullYear(),
  onYearChange,
}) => {
  const [hoveredDay, setHoveredDay] = useState<{ day: HeatmapDay; x: number; y: number } | null>(null);

  if (!days || days.length === 0) {
    return null;
  }

  // Group days into columns of weeks (7 days each)
  // Ensure the grid starts on a Monday
  const firstDate = new Date(days[0].date);
  const dayOfWeekOffset = (firstDate.getDay() + 6) % 7; // Monday = 0, Sunday = 6

  // Pad beginning with empty slots if first date isn't Monday
  const paddedDays: (HeatmapDay | null)[] = Array(dayOfWeekOffset).fill(null);
  paddedDays.push(...days);

  // Group into columns of 7 days
  const weeks: (HeatmapDay | null)[][] = [];
  for (let i = 0; i < paddedDays.length; i += 7) {
    weeks.push(paddedDays.slice(i, i + 7));
  }

  // Calculate month label positions
  const monthLabels: { label: string; colIndex: number }[] = [];
  let lastMonth = -1;
  weeks.forEach((week, colIndex) => {
    const validDay = week.find((d) => d !== null);
    if (validDay) {
      const month = new Date(validDay.date).getMonth();
      if (month !== lastMonth) {
        monthLabels.push({ label: MONTH_NAMES[month], colIndex });
        lastMonth = month;
      }
    }
  });

  // Generate color styles for level 0 to 4 based on colorBase
  const getSquareStyle = (day: HeatmapDay) => {
    if (!day.scheduled) {
      return {
        backgroundColor: 'transparent',
        border: '1px dashed rgba(148, 163, 184, 0.4)',
      };
    }

    if (day.level === 0) {
      return {};
    }

    // Dynamic opacity/shade based on colorBase
    const opacities = [0, 0.3, 0.55, 0.8, 1];
    const opacity = opacities[day.level] || 1;

    return {
      backgroundColor: colorBase,
      opacity: opacity,
    };
  };

  const currentYear = new Date().getFullYear();

  return (
    <div className="bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-2xl p-5 shadow-sm">
      {/* Header with Title and Year Selector */}
      <div className="flex items-center justify-between mb-4 flex-wrap gap-2">
        <div>
          <h3 className="font-semibold text-slate-800 dark:text-white text-sm">
            {title || 'Consistency Graph'}
          </h3>
          <p className="text-xs text-slate-400">
            {days.filter((d) => d.completedCount > 0).length} active days in this range
          </p>
        </div>

        {onYearChange && (
          <div className="flex items-center gap-1.5 bg-slate-100 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 px-2 py-1 rounded-xl text-xs font-semibold text-slate-700 dark:text-slate-300">
            <button
              onClick={() => onYearChange(year - 1)}
              className="p-1 hover:text-emerald-500 rounded transition"
              title="Previous Year"
            >
              <ChevronLeft className="w-3.5 h-3.5" />
            </button>
            <span className="px-1">{year}</span>
            <button
              onClick={() => onYearChange(year + 1)}
              disabled={year >= currentYear}
              className="p-1 hover:text-emerald-500 disabled:opacity-30 rounded transition"
              title="Next Year"
            >
              <ChevronRight className="w-3.5 h-3.5" />
            </button>
          </div>
        )}
      </div>

      {/* Grid Container with horizontal scroll */}
      <div className="overflow-x-auto pb-2 custom-scrollbar">
        <div className="inline-block min-w-full">
          {/* Month Header Row */}
          <div className="flex mb-1.5 ml-7 text-[10px] text-slate-400 relative h-4">
            {monthLabels.map((m, idx) => (
              <span
                key={`${m.label}-${idx}`}
                style={{ left: `${m.colIndex * 15}px` }}
                className="absolute font-medium"
              >
                {m.label}
              </span>
            ))}
          </div>

          {/* Grid: Days of Week + Columns */}
          <div className="flex gap-2">
            {/* Weekday labels (Mon, Wed, Fri) */}
            <div className="flex flex-col justify-between text-[9px] text-slate-400 py-0.5 select-none w-5 shrink-0">
              <span className="h-3 flex items-center">{WEEKDAYS[0]}</span>
              <span className="h-3 flex items-center"></span>
              <span className="h-3 flex items-center">{WEEKDAYS[2]}</span>
              <span className="h-3 flex items-center"></span>
              <span className="h-3 flex items-center">{WEEKDAYS[4]}</span>
              <span className="h-3 flex items-center"></span>
              <span className="h-3 flex items-center"></span>
            </div>

            {/* Matrix of Columns (Weeks) */}
            <div className="flex gap-[3px]">
              {weeks.map((week, colIdx) => (
                <div key={`col-${colIdx}`} className="flex flex-col gap-[3px]">
                  {week.map((day, rowIdx) => {
                    if (!day) {
                      return <div key={`empty-${colIdx}-${rowIdx}`} className="w-3 h-3" />;
                    }

                    const isLevelZero = day.level === 0 && day.scheduled;

                    return (
                      <div
                        key={day.date}
                        onClick={() => onDayClick && onDayClick(day)}
                        onMouseEnter={(e) => {
                          const rect = e.currentTarget.getBoundingClientRect();
                          setHoveredDay({ day, x: rect.left + rect.width / 2, y: rect.top });
                        }}
                        onMouseLeave={() => setHoveredDay(null)}
                        style={getSquareStyle(day)}
                        className={`w-3 h-3 rounded-[2.5px] cursor-pointer transition-all duration-150 hover:scale-125 hover:z-10 ${
                          isLevelZero
                            ? 'bg-slate-100 dark:bg-slate-700/60 hover:bg-slate-200 dark:hover:bg-slate-600'
                            : ''
                        }`}
                      />
                    );
                  })}
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>

      {/* Footer: Legend */}
      <div className="mt-3 pt-3 border-t border-slate-100 dark:border-slate-700/60 flex items-center justify-between text-[11px] text-slate-400">
        <span>Click on any square to inspect or log activity</span>

        <div className="flex items-center gap-1.5">
          <span>Less</span>
          <div className="w-2.5 h-2.5 rounded-[2px] bg-slate-100 dark:bg-slate-700/60" />
          <div className="w-2.5 h-2.5 rounded-[2px]" style={{ backgroundColor: colorBase, opacity: 0.3 }} />
          <div className="w-2.5 h-2.5 rounded-[2px]" style={{ backgroundColor: colorBase, opacity: 0.55 }} />
          <div className="w-2.5 h-2.5 rounded-[2px]" style={{ backgroundColor: colorBase, opacity: 0.8 }} />
          <div className="w-2.5 h-2.5 rounded-[2px]" style={{ backgroundColor: colorBase, opacity: 1 }} />
          <span>More</span>
        </div>
      </div>

      {/* Floating Tooltip */}
      {hoveredDay && (
        <div
          style={{
            position: 'fixed',
            left: `${hoveredDay.x}px`,
            top: `${hoveredDay.y - 8}px`,
            transform: 'translate(-50%, -100%)',
            pointerEvents: 'none',
          }}
          className="z-50 bg-slate-900/95 text-white text-[11px] px-2.5 py-1.5 rounded-lg shadow-xl border border-slate-700 whitespace-nowrap backdrop-blur-sm animate-fadeIn"
        >
          <span className="font-semibold text-emerald-400">
            {hoveredDay.day.date}
          </span>
          <span className="text-slate-300">
            : {hoveredDay.day.completedCount} of {hoveredDay.day.scheduledCount} habits completed
          </span>
          {!hoveredDay.day.scheduled && (
            <span className="text-slate-400 italic"> (Rest day)</span>
          )}
        </div>
      )}
    </div>
  );
};
