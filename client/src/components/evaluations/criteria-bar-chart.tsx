'use client';

import * as React from 'react';
import { ResponsiveContainer, BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Cell } from 'recharts';
import { useTranslations } from 'next-intl';

interface CriterionAgg {
  name: string;
  total: number;
  passed: number;
  passRate: number;
}

interface CriteriaBarChartProps {
  data: CriterionAgg[];
}

export default function CriteriaBarChart({ data }: CriteriaBarChartProps) {
  const t = useTranslations('evaluations');

  // Format Y-axis tick to truncate long criteria names on mobile/desktop
  const formatYAxis = (value: string) => {
    if (value.length > 25) {
      return value.slice(0, 22) + '...';
    }
    return value;
  };

  return (
    <div className="w-full h-80">
      <ResponsiveContainer width="100%" height="100%">
        <BarChart
          data={data}
          layout="vertical"
          margin={{ top: 10, right: 30, left: 10, bottom: 5 }}
        >
          <CartesianGrid strokeDasharray="3 3" horizontal={false} stroke="hsl(var(--border))" />
          <XAxis
            type="number"
            domain={[0, 100]}
            tickFormatter={(val) => `${val}%`}
            stroke="hsl(var(--muted-foreground))"
            fontSize={11}
          />
          <YAxis
            type="category"
            dataKey="name"
            width={120}
            tickFormatter={formatYAxis}
            stroke="hsl(var(--muted-foreground))"
            fontSize={11}
          />
          <Tooltip
            formatter={(
              value: string | number | readonly (string | number)[] | undefined,
              _name: string | number | undefined,
              item: { payload?: { passed?: number; total?: number } }
            ) => {
              const passed = item.payload?.passed ?? 0;
              const total = item.payload?.total ?? 0;
              return [`${value}% (${passed}/${total})`, t('passRate')];
            }}
            contentStyle={{
              backgroundColor: 'hsl(var(--background))',
              borderColor: 'hsl(var(--border))',
              borderRadius: '8px',
              fontSize: '12px',
              color: 'hsl(var(--foreground))',
            }}
          />
          <Bar dataKey="passRate" radius={[0, 4, 4, 0]} barSize={16}>
            {data.map((entry, index) => {
              // Color based on success rate
              let fill = 'hsl(var(--primary))';
              if (entry.passRate >= 80) fill = '#10b981'; // Emerald
              else if (entry.passRate <= 50) fill = '#ef4444'; // Red
              else fill = '#f59e0b'; // Amber
              // eslint-disable-next-line @typescript-eslint/no-deprecated
              return <Cell key={`cell-${index}`} fill={fill} />;
            })}
          </Bar>
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}
