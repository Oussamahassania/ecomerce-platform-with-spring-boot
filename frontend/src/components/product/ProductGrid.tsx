import type { Product } from "@/lib/types";
import { ProductCard } from "./ProductCard";
import { EmptyState, Skeleton } from "@/components/ui";
import { cn } from "@/components/ui";

export function ProductGrid({
  products,
  loading = false,
  skeletonCount = 8,
  columns = "four",
  emptyTitle = "Nothing here yet",
  emptyDescription,
  emptyAction,
}: {
  products: Product[] | undefined;
  loading?: boolean;
  skeletonCount?: number;
  columns?: "three" | "four";
  emptyTitle?: string;
  emptyDescription?: string;
  emptyAction?: React.ReactNode;
}) {
  const grid = cn(
    "grid gap-x-4 gap-y-10 md:gap-x-6",
    columns === "four"
      ? "grid-cols-2 md:grid-cols-3 xl:grid-cols-4"
      : "grid-cols-2 md:grid-cols-3",
  );

  if (loading) {
    return (
      <div className={grid}>
        {Array.from({ length: skeletonCount }).map((_, index) => (
          <div key={index}>
            <Skeleton className="aspect-[4/5] w-full" />
            <div className="mt-3 flex items-center justify-between gap-4">
              <Skeleton className="h-3.5 w-2/3" />
              <Skeleton className="h-3.5 w-12" />
            </div>
          </div>
        ))}
      </div>
    );
  }

  if (!products || products.length === 0) {
    return (
      <EmptyState
        title={emptyTitle}
        description={emptyDescription}
        action={emptyAction}
      />
    );
  }

  return (
    <div className={grid}>
      {products.map((product, index) => (
        <ProductCard key={product.id} product={product} priority={index < 4} />
      ))}
    </div>
  );
}
