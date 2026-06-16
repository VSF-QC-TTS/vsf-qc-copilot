'use client';

import * as React from 'react';
import { ResponsiveContainer, PieChart, Pie } from 'recharts';
import { cn } from '@/lib/utils';

interface RedTeamResultsChartProps {
  successes: number;
  failures: number;
  errors: number;
  vulnerabilityRate: number;
}

export default function RedTeamResultsChart({
  successes,
  failures,
  errors,
  vulnerabilityRate,
}: RedTeamResultsChartProps) {
  const chartData = React.useMemo(() => {
    return [
      { name: 'Shielded', value: successes, fill: '#10b981' }, // Emerald
      { name: 'Exploited', value: failures, fill: '#ef4444' }, // Red
      { name: 'Errors', value: errors, fill: '#71717a' }, // Zinc
    ].filter((item) => item.value > 0);
  }, [successes, failures, errors]);

  return (
    <div className="relative w-28 h-28 flex items-center justify-center">
      <ResponsiveContainer width="100%" height="100%">
        <PieChart>
          <Pie
            data={chartData}
            cx="50%"
            cy="50%"
            innerRadius={36}
            outerRadius={48}
            paddingAngle={3}
            dataKey="value"
          />
        </PieChart>
      </ResponsiveContainer>
      <div className="absolute inset-0 flex flex-col items-center justify-center pointer-events-none">
        <span className={cn(
          'text-xl font-black leading-none',
          vulnerabilityRate > 0 ? 'text-red-500' : 'text-emerald-500'
        )}>
          {vulnerabilityRate}%
        </span>
        <span className="text-[8px] uppercase tracking-wider text-zinc-500 mt-1 font-bold">
          {vulnerabilityRate > 0 ? 'Rủi ro' : 'An toàn'}
        </span>
      </div>
    </div>
  );
}
