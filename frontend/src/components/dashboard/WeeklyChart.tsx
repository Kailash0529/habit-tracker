import React from 'react';
import { DaySummary } from '../../api/client';
import {
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  Cell,
} from 'recharts';

interface WeeklyChartProps {
  data: DaySummary[];
}

export const WeeklyChart: React.FC<WeeklyChartProps> = ({ data }) => {
  if (!data || data.length === 0) {
    return null;
  }

  const chartData = data.map((d) => ({
    name: d.dayOfWeek,
    completed: d.completedCount,
    scheduled: d.scheduledCount,
    rate: d.scheduledCount > 0 ? Math.round((d.completedCount / d.scheduledCount) * 100) : 0,
  }));

  return (
    <div className="bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-2xl p-5 shadow-sm">
      <div className="flex items-center justify-between mb-4">
        <div>
          <h3 className="font-semibold text-slate-800 dark:text-white text-sm">
            7-Day Activity
          </h3>
          <p className="text-xs text-slate-400">Completed vs scheduled habits</p>
        </div>
        <div className="flex items-center gap-3 text-xs">
          <div className="flex items-center gap-1.5">
            <span className="w-2.5 h-2.5 rounded-sm bg-emerald-500" />
            <span className="text-slate-500 dark:text-slate-400">Completed</span>
          </div>
          <div className="flex items-center gap-1.5">
            <span className="w-2.5 h-2.5 rounded-sm bg-slate-200 dark:bg-slate-700" />
            <span className="text-slate-500 dark:text-slate-400">Scheduled</span>
          </div>
        </div>
      </div>

      <div className="h-44 w-full">
        <ResponsiveContainer width="100%" height="100%">
          <BarChart data={chartData} margin={{ top: 5, right: 10, left: -25, bottom: 0 }}>
            <XAxis
              dataKey="name"
              stroke="#94a3b8"
              fontSize={11}
              tickLine={false}
              axisLine={false}
            />
            <YAxis
              stroke="#94a3b8"
              fontSize={11}
              tickLine={false}
              axisLine={false}
              allowDecimals={false}
            />
            <Tooltip
              cursor={{ fill: 'transparent' }}
              content={({ active, payload }) => {
                if (active && payload && payload.length) {
                  const d = payload[0].payload;
                  return (
                    <div className="bg-slate-900 text-white text-xs px-3 py-2 rounded-xl shadow-xl border border-slate-700">
                      <p className="font-semibold mb-1">{d.name}</p>
                      <p className="text-emerald-400">{d.completed} completed</p>
                      <p className="text-slate-400">{d.scheduled} scheduled</p>
                      <p className="text-slate-300 font-bold mt-1">{d.rate}% completion</p>
                    </div>
                  );
                }
                return null;
              }}
            />
            <Bar dataKey="scheduled" fill="#334155" opacity={0.3} radius={[4, 4, 0, 0]} />
            <Bar dataKey="completed" radius={[4, 4, 0, 0]}>
              {chartData.map((entry, index) => (
                <Cell
                  key={`cell-${index}`}
                  fill={entry.rate === 100 ? '#10B981' : entry.rate > 0 ? '#34D399' : '#94A3B8'}
                />
              ))}
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
};
