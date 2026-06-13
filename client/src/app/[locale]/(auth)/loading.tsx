import { Skeleton, SkeletonText } from '@/components/feedback/loading-skeleton';

export default function AuthLoading() {
  return (
    <div className="w-full space-y-6">
      {/* Title skeleton */}
      <div className="space-y-2 text-center">
        <Skeleton className="mx-auto h-7 w-48" />
        <SkeletonText width="w-64" className="mx-auto" />
      </div>

      {/* Form fields skeleton */}
      <div className="space-y-4 rounded-lg border bg-card p-6">
        {Array.from({ length: 2 }, (_, i) => (
          <div key={i} className="space-y-2">
            <SkeletonText width="w-20" />
            <Skeleton className="h-10 w-full" />
          </div>
        ))}
        <Skeleton className="h-10 w-full" />
      </div>

      {/* Footer links skeleton */}
      <div className="flex justify-center">
        <SkeletonText width="w-40" />
      </div>
    </div>
  );
}
