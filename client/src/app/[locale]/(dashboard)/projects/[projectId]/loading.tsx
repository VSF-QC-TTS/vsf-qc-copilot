import {
  Skeleton,
  SkeletonText,
  SkeletonTableRows,
} from '@/components/feedback/loading-skeleton';

export default function ProjectDetailLoading() {
  return (
    <div className="space-y-6">
      {/* Page header skeleton */}
      <div className="flex items-center justify-between gap-4">
        <div className="space-y-2">
          <Skeleton className="h-8 w-56" />
          <SkeletonText width="w-40" />
        </div>
        <Skeleton className="h-10 w-28" />
      </div>

      {/* Project overview card */}
      <div className="rounded-lg border p-6 space-y-4">
        <Skeleton className="h-6 w-36" />
        <div className="grid gap-4 sm:grid-cols-2">
          {Array.from({ length: 4 }, (_, i) => (
            <div key={i} className="space-y-1">
              <SkeletonText width="w-24" />
              <SkeletonText width="w-40" />
            </div>
          ))}
        </div>
      </div>

      {/* Quick links skeleton */}
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-5">
        {Array.from({ length: 5 }, (_, i) => (
          <Skeleton key={i} className="h-20 rounded-lg" />
        ))}
      </div>

      {/* Recent evaluations table skeleton */}
      <div className="rounded-lg border">
        <div className="border-b px-4 py-3">
          <Skeleton className="h-5 w-40" />
        </div>
        <table className="w-full">
          <tbody>
            <SkeletonTableRows count={3} columns={4} />
          </tbody>
        </table>
      </div>
    </div>
  );
}
