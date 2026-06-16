'use client';

import * as React from 'react';
import { ResponsiveContainer, PieChart, Pie, Tooltip } from 'recharts';
import { useTranslations } from 'next-intl';

interface RunDonutChartProps {
  passed: number;
  failed: number;
  warning: number;
  error: number;
  passRate: number;
}

export default function RunDonutChart({
  passed,
  failed,
  warning,
  error,
  passRate,
}: RunDonutChartProps) {
  const t = useTranslations('status');
  const tEval = useTranslations('evaluations');

  const data = React.useMemo(() => [
    { name: t('PASS'), value: passed, fill: '#10b981' }, // Emerald
    { name: t('FAIL'), value: failed, fill: '#ef4444' }, // Red
    { name: t('WARNING'), value: warning, fill: '#f59e0b' }, // Amber
    { name: t('ERROR'), value: error, fill: '#71717a' }, // Zinc/Gray
  ].filter(item => item.value > 0), [t, passed, failed, warning, error]);

  const passRatePercent = Math.round(passRate * 100);

  return (
    <div className="relative w-full h-64 flex items-center justify-center">
      <ResponsiveContainer width="100%" height="100%">
        <PieChart>
          <Pie
            data={data}
            cx="50%"
            cy="50%"
            innerRadius={65}
            outerRadius={85}
            paddingAngle={2}
            dataKey="value"
          />
          <Tooltip
            contentStyle={{
              backgroundColor: 'hsl(var(--background))',
              borderColor: 'hsl(var(--border))',
              borderRadius: '8px',
              fontSize: '12px',
              color: 'hsl(var(--foreground))',
            }}
          />
        </PieChart>
      </ResponsiveContainer>
      
      {/* Center text overlay */}
      <div className="absolute inset-0 flex flex-col items-center justify-center pointer-events-none">
        <span className="text-2xl font-bold text-foreground">{passRatePercent}%</span>
        <span className="text-[10px] uppercase tracking-wider text-muted-foreground font-semibold">
          {tEval('passRate')}
        </span>
      </div>
    </div>
  );
}
