import {
  Skeleton,
  SkeletonText,
  SkeletonTableRows,
} from '@/components/feedback/loading-skeleton';

export default function DashboardLoading() {
  return (
    <div className="space-y-6">
      {/* Page header skeleton */}
      <div className="flex items-center justify-between gap-4">
        <div className="space-y-2">
          <Skeleton className="h-8 w-48" />
          <SkeletonText width="w-64" />
        </div>
        <Skeleton className="h-10 w-32" />
      </div>

      {/* Filter tabs skeleton */}
      <div className="flex items-center gap-2">
        <Skeleton className="h-8 w-16" />
        <Skeleton className="h-8 w-16" />
        <Skeleton className="h-8 w-16" />
      </div>

      {/* Table skeleton */}
      <div className="rounded-lg border">
        <table className="w-full">
          <thead>
            <tr className="border-b">
              {Array.from({ length: 4 }, (_, i) => (
                <th key={i} className="px-4 py-3 text-left">
                  <Skeleton className="h-4 w-20" />
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            <SkeletonTableRows count={5} columns={4} />
          </tbody>
        </table>
      </div>
    </div>
  );
}
