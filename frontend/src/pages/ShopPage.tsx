import { useEffect, useMemo, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { Search, SlidersHorizontal, X } from "lucide-react";
import { useCategories, useShopProducts } from "@/hooks/queries";
import { ProductGrid } from "@/components/product/ProductGrid";
import { cn, Input, Pagination, Select } from "@/components/ui";

const SORTS = [
  { value: "createdAt:DESC", label: "Newest first" },
  { value: "price:ASC", label: "Price, low to high" },
  { value: "price:DESC", label: "Price, high to low" },
  { value: "name:ASC", label: "Name, A to Z" },
];

const PRICES = [
  { value: "all", label: "Any price" },
  { value: "under-100", label: "Under €100" },
  { value: "100-200", label: "€100 – €200" },
  { value: "over-200", label: "€200 and up" },
];

function priceToParams(value: string): {
  minPrice: number | null;
  maxPrice: number | null;
} {
  if (value === "under-100") return { minPrice: null, maxPrice: 100 };
  if (value === "100-200") return { minPrice: 100, maxPrice: 200 };
  if (value === "over-200") return { minPrice: 200, maxPrice: null };
  return { minPrice: null, maxPrice: null };
}

function paramsToPrice(min: number | null, max: number | null): string {
  if (min === null && max === 100) return "under-100";
  if (min === 100 && max === 200) return "100-200";
  if (min === 200 && max === null) return "over-200";
  return "all";
}

export function ShopPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const { data: categories } = useCategories();

  const q = searchParams.get("q") ?? "";
  const category = searchParams.get("category")
    ? Number(searchParams.get("category"))
    : null;
  const minPrice = searchParams.get("min")
    ? Number(searchParams.get("min"))
    : null;
  const maxPrice = searchParams.get("max")
    ? Number(searchParams.get("max"))
    : null;
  const sortValue = `${searchParams.get("sort") ?? "createdAt"}:${searchParams.get("order") ?? "DESC"}`;
  const page = Number(searchParams.get("page") ?? 0);
  const priceValue = paramsToPrice(minPrice, maxPrice);

  const [term, setTerm] = useState(q);

  useEffect(() => {
    setTerm(q);
  }, [q]);

  function update(
    patch: Record<string, string | null>,
    resetPage = true,
  ) {
    const next = new URLSearchParams(searchParams);
    for (const [key, value] of Object.entries(patch)) {
      if (value === null || value === "") next.delete(key);
      else next.set(key, value);
    }
    if (resetPage) next.delete("page");
    setSearchParams(next, { replace: true });
  }

  useEffect(() => {
    const handle = window.setTimeout(() => {
      const trimmed = term.trim();
      if (trimmed === q) return;
      update({ q: trimmed || null });
    }, 380);
    return () => window.clearTimeout(handle);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [term, q]);

  const activeCategory = categories?.find((item) => item.id === category);
  const hasFilters =
    Boolean(q) || Boolean(category) || Boolean(priceValue !== "all");

  const heading = activeCategory
    ? activeCategory.name
    : q
      ? `Results for “${q}”`
      : "All objects";

  const query = useMemo(
    () => ({
      q: q || undefined,
      category,
      minPrice,
      maxPrice,
      sort: sortValue.split(":")[0],
      order: sortValue.split(":")[1] as "ASC" | "DESC",
      page,
      size: 12,
    }),
    [q, category, minPrice, maxPrice, sortValue, page],
  );

  const { data, isLoading, isFetching, isError, error, refetch } =
    useShopProducts(query);

  function changePage(next: number) {
    update({ page: next === 0 ? null : String(next) }, false);
    document
      .getElementById("shop-results")
      ?.scrollIntoView({ behavior: "smooth", block: "start" });
  }

  return (
    <div>
      <section className="container-x pt-10 md:pt-14">
        <div className="flex flex-wrap items-end justify-between gap-4 border-b border-line pb-6">
          <div>
            <p className="text-[12px] text-muted">
              {isLoading
                ? "Loading the shelf"
                : `${data?.totalElements ?? 0} ${
                    data?.totalElements === 1 ? "object" : "objects"
                  }`}
            </p>
            <h1 className="mt-2 font-display text-[clamp(2.6rem,5.6vw,4.6rem)] font-semibold leading-[0.95] tracking-[-0.05em]">
              {heading}
            </h1>
            {activeCategory?.description ? (
              <p className="mt-3 max-w-lg text-sm leading-relaxed text-muted">
                {activeCategory.description}
              </p>
            ) : (
              <p className="mt-3 max-w-lg text-sm leading-relaxed text-muted">
                Audio, lighting, desk tools and objects — chosen one at a time.
              </p>
            )}
          </div>
          {hasFilters ? (
            <button
              type="button"
              onClick={() => setSearchParams({}, { replace: true })}
              className="inline-flex items-center gap-1.5 text-[13px] text-muted transition-colors hover:text-ink"
            >
              <X className="h-3.5 w-3.5" />
              Clear filters
            </button>
          ) : null}
        </div>
      </section>

      <div className="sticky top-[68px] z-30 border-b border-line bg-paper/92 backdrop-blur-xl md:top-[76px]">
        <div className="container-x py-3.5">
          <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
            <div className="flex flex-1 items-center gap-2">
              <div className="relative w-full max-w-xs">
                <Search className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-muted" />
                <Input
                  value={term}
                  onChange={(event) => setTerm(event.target.value)}
                  placeholder="Search objects"
                  aria-label="Search objects"
                  className="h-10 rounded-full border-ink/15 bg-paper pl-10 text-[13px]"
                />
                {term ? (
                  <button
                    type="button"
                    aria-label="Clear search"
                    onClick={() => {
                      setTerm("");
                      update({ q: null });
                    }}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-muted hover:text-ink"
                  >
                    <X className="h-3.5 w-3.5" />
                  </button>
                ) : null}
              </div>
              <div className="hidden items-center gap-1.5 text-[11px] text-muted sm:flex">
                <SlidersHorizontal className="h-3.5 w-3.5" />
                Filters
              </div>
            </div>

            <div className="grid grid-cols-2 gap-2 lg:flex lg:items-center">
              <div className="min-w-0 lg:hidden">
                <Select
                  aria-label="Filter by category"
                  value={category ?? ""}
                  onChange={(event) =>
                    update({ category: event.target.value || null })
                  }
                  className="h-10 rounded-full border-ink/15 bg-paper text-[13px]"
                >
                  <option value="">All categories</option>
                  {categories?.map((item) => (
                    <option key={item.id} value={item.id}>
                      {item.name}
                    </option>
                  ))}
                </Select>
              </div>
              <div className="min-w-0 lg:hidden">
                <Select
                  aria-label="Filter by price"
                  value={priceValue}
                  onChange={(event) => {
                    const { minPrice: min, maxPrice: max } = priceToParams(
                      event.target.value,
                    );
                    update({
                      min: min === null ? null : String(min),
                      max: max === null ? null : String(max),
                    });
                  }}
                  className="h-10 rounded-full border-ink/15 bg-paper text-[13px]"
                >
                  {PRICES.map((price) => (
                    <option key={price.value} value={price.value}>
                      {price.label}
                    </option>
                  ))}
                </Select>
              </div>
              <div className="col-span-2 min-w-0 lg:w-52 lg:flex-none">
                <Select
                  aria-label="Sort products"
                  value={SORTS.some((item) => item.value === sortValue) ? sortValue : "createdAt:DESC"}
                  onChange={(event) => {
                    const [sort, order] = event.target.value.split(":");
                    update({ sort, order });
                  }}
                  className="h-10 rounded-full border-ink/15 bg-paper text-[13px]"
                >
                  {SORTS.map((sort) => (
                    <option key={sort.value} value={sort.value}>
                      {sort.label}
                    </option>
                  ))}
                </Select>
              </div>
            </div>
          </div>

          <div className="no-scrollbar mt-3 flex items-center gap-1.5 overflow-x-auto">
            <FilterChip
              active={!category}
              onClick={() => update({ category: null })}
            >
              All
            </FilterChip>
            {categories?.map((item) => (
              <FilterChip
                key={item.id}
                active={category === item.id}
                onClick={() =>
                  update({ category: String(item.id) })
                }
              >
                {item.name}
              </FilterChip>
            ))}
            <span className="mx-2 h-4 w-px shrink-0 bg-line" />
            {PRICES.map((price) => (
              <FilterChip
                key={price.value}
                active={priceValue === price.value}
                onClick={() => {
                  const { minPrice: min, maxPrice: max } = priceToParams(
                    price.value,
                  );
                  update({
                    min: min === null ? null : String(min),
                    max: max === null ? null : String(max),
                  });
                }}
              >
                {price.label}
              </FilterChip>
            ))}
          </div>
        </div>
      </div>

      <section id="shop-results" className="container-x scroll-mt-40 pt-8">
        {isError ? (
          <div className="py-20 text-center">
            <p className="font-display text-2xl">The shelf did not load.</p>
            <p className="mt-2 text-sm text-muted">
              {(error as Error).message}
            </p>
            <button
              type="button"
              onClick={() => refetch()}
              className="mt-5 rounded-full bg-ink px-5 py-2.5 text-sm text-paper"
            >
              Try again
            </button>
          </div>
        ) : (
          <div
            className={cn(
              "transition-opacity duration-300",
              isFetching && !isLoading && "opacity-60",
            )}
          >
            <ProductGrid
              products={data?.content}
              loading={isLoading}
              skeletonCount={8}
              columns="four"
              emptyTitle="No objects match those filters."
              emptyDescription="Try widening the price range or clearing the search."
              emptyAction={
                <button
                  type="button"
                  onClick={() => setSearchParams({}, { replace: true })}
                  className="rounded-full bg-ink px-6 py-3 text-sm font-medium text-paper"
                >
                  Show everything
                </button>
              }
            />
          </div>
        )}

        {data ? (
          <>
            <p className="mt-10 text-center text-[11px] tabular-nums text-muted">
              Showing {data.content.length} of {data.totalElements} objects
            </p>
            <Pagination
              page={data.number}
              totalPages={data.totalPages}
              onChange={changePage}
            />
          </>
        ) : null}
      </section>
    </div>
  );
}

function FilterChip({
  active,
  onClick,
  children,
}: {
  active: boolean;
  onClick: () => void;
  children: React.ReactNode;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-pressed={active}
      className={cn(
        "shrink-0 rounded-full border px-3.5 py-1.5 text-[12px] font-medium transition-colors",
        active
          ? "border-ink bg-ink text-paper"
          : "border-ink/15 text-ink-2 hover:border-ink/40",
      )}
    >
      {children}
    </button>
  );
}
