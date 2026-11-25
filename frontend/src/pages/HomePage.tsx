import { useMemo, useRef, useState } from "react";
import { Link } from "react-router-dom";
import {
  AnimatePresence,
  motion,
  useReducedMotion,
} from "motion/react";
import { ArrowRight, ArrowUpRight, ChevronLeft, ChevronRight } from "lucide-react";
import {
  useAllProducts,
  useCategories,
} from "@/hooks/queries";
import { money } from "@/lib/format";
import { productImage } from "@/lib/images";
import { ProductCard } from "@/components/product/ProductCard";
import { SectionHeader, Skeleton } from "@/components/ui";
import type { Category, Product } from "@/lib/types";

function categoryPreview(categoryId: number, products: Product[]) {
  return products.find((product) => product.categoryId === categoryId);
}

export function HomePage() {
  const { data: products, isLoading } = useAllProducts();
  const { data: categories } = useCategories();
  const reduce = useReducedMotion();
  const railRef = useRef<HTMLDivElement>(null);
  const [activeCategory, setActiveCategory] = useState<number | null>(null);

  const sorted = useMemo(
    () =>
      [...(products ?? [])].sort(
        (a, b) =>
          new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime(),
      ),
    [products],
  );

  const hero = sorted[0];
  const secondary = sorted[2] ?? sorted[1];
  const newIn = sorted.slice(0, 6);
  const desk = categories?.find((category) => category.name === "Desk");
  const featured =
    products?.find((product) => product.name === "Halo Task Light") ??
    sorted[2];

  const counts = useMemo(() => {
    const map = new Map<number, number>();
    for (const product of products ?? []) {
      if (product.categoryId) {
        map.set(product.categoryId, (map.get(product.categoryId) ?? 0) + 1);
      }
    }
    return map;
  }, [products]);

  const active =
    categories?.find((category) => category.id === activeCategory) ??
    categories?.[0];
  const activePreview =
    active && products
      ? categoryPreview(active.id, products)
      : undefined;

  function scrollRail(direction: 1 | -1) {
    railRef.current?.scrollBy({
      left: direction * 340,
      behavior: "smooth",
    });
  }

  const heroLines = ["Objects", "that earn", "their place"];

  return (
    <div>
      {/* Hero */}
      <section className="container-x pb-8 pt-8 md:pt-12">
        <div className="grid items-end gap-12 lg:grid-cols-12 lg:gap-8">
          <div className="lg:col-span-7 lg:pb-4">
            <h1 className="font-display text-[clamp(3.4rem,8.6vw,8rem)] font-semibold leading-[0.86] tracking-[-0.055em]">
              <motion.span
                initial="hidden"
                animate="visible"
                transition={{ staggerChildren: reduce ? 0 : 0.09 }}
                className="block"
              >
                {heroLines.map((line, index) => (
                  <span key={line} className="block overflow-hidden pb-[0.06em]">
                    <motion.span
                      className="block"
                      variants={{
                        hidden: { y: reduce ? 0 : "112%" },
                        visible: {
                          y: 0,
                          transition: {
                            duration: reduce ? 0 : 0.95,
                            ease: [0.22, 1, 0.36, 1],
                          },
                        },
                      }}
                    >
                      {index === 2 ? (
                        <>
                          their place
                          <span className="text-cobalt">.</span>
                        </>
                      ) : (
                        line
                      )}
                    </motion.span>
                  </span>
                ))}
              </motion.span>
            </h1>

            <div className="mt-8 flex max-w-2xl flex-col gap-7 border-t border-line pt-6 sm:flex-row sm:items-end sm:justify-between">
              <motion.p
                initial={{ opacity: 0, y: reduce ? 0 : 10 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: reduce ? 0 : 0.55, duration: 0.6 }}
                className="max-w-sm text-[15px] leading-relaxed text-ink-2"
              >
                Audio, lighting and desk tools chosen for how they look after
                ten years — not ten minutes.
              </motion.p>
              <motion.div
                initial={{ opacity: 0, y: reduce ? 0 : 10 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: reduce ? 0 : 0.65, duration: 0.6 }}
              >
                <Link
                  to="/shop"
                  className="group inline-flex h-12 items-center gap-2 rounded-full bg-ink pl-6 pr-4 text-sm font-medium text-paper transition-colors hover:bg-cobalt"
                >
                  Shop the collection
                  <span className="flex h-7 w-7 items-center justify-center rounded-full bg-paper/10 transition-transform duration-300 group-hover:translate-x-0.5">
                    <ArrowRight className="h-4 w-4" />
                  </span>
                </Link>
              </motion.div>
            </div>
          </div>

          <div className="lg:col-span-5">
            <motion.div
              initial={{ opacity: 0, scale: reduce ? 1 : 1.035 }}
              animate={{ opacity: 1, scale: 1 }}
              transition={{ delay: reduce ? 0 : 0.2, duration: reduce ? 0 : 1.1, ease: [0.22, 1, 0.36, 1] }}
              className="relative"
            >
              <div
                aria-hidden
                className="absolute -right-2.5 -top-2.5 h-full w-full bg-cobalt md:-right-3.5 md:-top-3.5"
              />
              <Link
                to={hero ? `/product/${hero.id}` : "/shop"}
                className="relative block aspect-[4/5] overflow-hidden bg-paper-2"
              >
                {hero ? (
                  <img
                    src={productImage(hero.img_url)}
                    alt={hero.name}
                    className="h-full w-full object-cover"
                  />
                ) : (
                  <Skeleton className="h-full w-full" />
                )}
                <div className="absolute inset-x-0 bottom-0 flex items-end justify-between gap-4 bg-gradient-to-t from-ink/55 to-transparent p-5 text-paper">
                  <div>
                    <p className="text-[11px] text-paper/75">New in</p>
                    <p className="font-display text-2xl leading-none tracking-[-0.04em]">
                      {hero?.name ?? "Loading objects"}
                    </p>
                  </div>
                  <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-paper text-ink">
                    <ArrowUpRight className="h-4 w-4" />
                  </span>
                </div>
              </Link>

              {secondary ? (
                <motion.div
                  initial={{ opacity: 0, y: reduce ? 0 : 16 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: reduce ? 0 : 0.7, duration: 0.6 }}
                  className="absolute -bottom-6 -left-3 hidden items-center gap-3 bg-paper p-3 pr-4 shadow-[0_16px_40px_rgba(20,21,15,0.14)] sm:flex md:-left-6"
                >
                  <Link
                    to={`/product/${secondary.id}`}
                    className="h-16 w-14 shrink-0 overflow-hidden bg-paper-2"
                  >
                    <img
                      src={productImage(secondary.img_url)}
                      alt={secondary.name}
                      className="h-full w-full object-cover"
                    />
                  </Link>
                  <div className="min-w-0">
                    <p className="text-[11px] text-muted">Also new</p>
                    <Link
                      to={`/product/${secondary.id}`}
                      className="block max-w-36 truncate text-[13px] font-medium"
                    >
                      {secondary.name}
                    </Link>
                    <p className="text-[12px] tabular-nums text-muted">
                      {money(secondary.price)}
                    </p>
                  </div>
                </motion.div>
              ) : null}
            </motion.div>
          </div>
        </div>

        <div className="mt-14 flex flex-wrap items-center justify-between gap-4 border-t border-line pt-4 text-[11px] text-muted">
          <span>Independent makers, small batches</span>
          <span className="hidden sm:inline">Ships worldwide</span>
          <span>30-day returns</span>
          <span className="hidden md:inline">Repair-friendly by design</span>
        </div>
      </section>

      {/* Category marquee */}
      <section
        aria-label="Shop categories"
        className="mt-10 overflow-hidden border-y border-ink/10 bg-cobalt text-paper"
      >
        <div className="flex w-max animate-marquee items-center py-3.5 hover:[animation-play-state:paused] md:py-4">
          {[...(categories ?? []), ...(categories ?? [])].map(
            (category, index) => (
              <Link
                key={`${category.id}-${index}`}
                to={`/shop?category=${category.id}`}
                className="flex items-center whitespace-nowrap"
              >
                <span className="px-6 font-display text-2xl tracking-[-0.04em] md:px-8 md:text-4xl">
                  {category.name}
                </span>
                <span className="text-butter">✳</span>
              </Link>
            ),
          )}
        </div>
      </section>

      {/* New in */}
      <section className="container-x mt-20 md:mt-24">
        <SectionHeader
          title="New in"
          description="The latest arrivals, in the order they landed on the shelf."
          action={
            <div className="flex items-center gap-3">
              <div className="hidden items-center gap-1.5 md:flex">
                <button
                  type="button"
                  aria-label="Scroll left"
                  onClick={() => scrollRail(-1)}
                  className="flex h-10 w-10 items-center justify-center rounded-full border border-ink/15 transition-colors hover:bg-ink hover:text-paper"
                >
                  <ChevronLeft className="h-4 w-4" />
                </button>
                <button
                  type="button"
                  aria-label="Scroll right"
                  onClick={() => scrollRail(1)}
                  className="flex h-10 w-10 items-center justify-center rounded-full border border-ink/15 transition-colors hover:bg-ink hover:text-paper"
                >
                  <ChevronRight className="h-4 w-4" />
                </button>
              </div>
              <Link
                to="/shop"
                className="link-underline inline-flex items-center gap-1.5 text-[13px] font-medium"
              >
                Shop all
                <ArrowUpRight className="h-3.5 w-3.5" />
              </Link>
            </div>
          }
        />

        <div
          ref={railRef}
          className="no-scrollbar -mx-5 mt-8 flex snap-x snap-mandatory gap-4 overflow-x-auto px-5 pb-2 md:mx-0 md:gap-6 md:px-0"
        >
          {isLoading
            ? Array.from({ length: 4 }).map((_, index) => (
                <div
                  key={index}
                  className="w-[74%] shrink-0 sm:w-[46%] lg:w-[30%] xl:w-[23.5%]"
                >
                  <Skeleton className="aspect-[4/5] w-full" />
                  <Skeleton className="mt-3 h-3.5 w-2/3" />
                </div>
              ))
            : newIn.map((product) => (
                <div
                  key={product.id}
                  className="w-[74%] shrink-0 snap-start sm:w-[46%] lg:w-[30%] xl:w-[23.5%]"
                >
                  <ProductCard product={product} />
                </div>
              ))}
        </div>
      </section>

      {/* Categories */}
      <section className="container-x mt-24">
        <SectionHeader
          title="Shop by category"
          description="Five ways into the shop, depending on what your room is missing."
        />
        <div className="mt-8 grid gap-10 lg:grid-cols-12">
          <div className="lg:col-span-7">
            {categories?.map((category) => (
              <CategoryRow
                key={category.id}
                category={category}
                count={counts.get(category.id) ?? 0}
                onHover={() => setActiveCategory(category.id)}
              />
            ))}
            {!categories ? (
              <div className="space-y-4 py-6">
                {Array.from({ length: 4 }).map((_, index) => (
                  <Skeleton key={index} className="h-14 w-full" />
                ))}
              </div>
            ) : null}
          </div>

          <div className="hidden lg:col-span-5 lg:block">
            <div className="sticky top-28">
              <div className="relative aspect-[4/5] overflow-hidden bg-paper-2">
                <AnimatePresence mode="wait">
                  {activePreview ? (
                    <motion.img
                      key={activePreview.id}
                      src={productImage(activePreview.img_url)}
                      alt={activePreview.name}
                      initial={{ opacity: 0, scale: reduce ? 1 : 1.03 }}
                      animate={{ opacity: 1, scale: 1 }}
                      exit={{ opacity: 0 }}
                      transition={{ duration: reduce ? 0 : 0.4 }}
                      className="absolute inset-0 h-full w-full object-cover"
                    />
                  ) : null}
                </AnimatePresence>
                {active ? (
                  <div className="absolute inset-x-0 bottom-0 bg-gradient-to-t from-ink/60 to-transparent p-5 pt-16 text-paper">
                    <p className="font-display text-2xl tracking-[-0.04em]">
                      {active.name}
                    </p>
                    <p className="mt-1 max-w-xs text-[12px] leading-relaxed text-paper/80">
                      {active.description}
                    </p>
                  </div>
                ) : null}
              </div>
              <p className="mt-3 text-[11px] text-muted">
                {activePreview ? activePreview.name : "The shop, in five parts"}
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* Featured band */}
      <section className="mt-24 bg-cobalt text-paper">
        <div className="container-x grid gap-10 py-14 md:py-20 lg:grid-cols-12 lg:items-center">
          <div className="lg:col-span-6">
            <p className="text-[12px] text-paper/60">The desk edit</p>
            <h2 className="mt-3 font-display text-[clamp(2.6rem,5.4vw,5rem)] font-semibold leading-[0.93] tracking-[-0.05em]">
              A quieter place
              <br />
              to think.
            </h2>
            <p className="mt-5 max-w-md text-sm leading-relaxed text-paper/75">
              A desk should hold the work, not compete with it. The desk edit
              gathers low-glare light, a silent clock and the small tray that
              keeps everything else in one place.
            </p>
            <Link
              to={desk ? `/shop?category=${desk.id}` : "/shop"}
              className="mt-8 inline-flex h-12 items-center gap-2 rounded-full bg-paper px-6 text-sm font-medium text-ink transition-colors hover:bg-butter"
            >
              Shop desk objects
              <ArrowUpRight className="h-4 w-4" />
            </Link>
          </div>

          <div className="lg:col-span-5 lg:col-start-8">
            {featured ? (
              <Link to={`/product/${featured.id}`} className="group block">
                <div className="relative aspect-[4/5] overflow-hidden bg-paper">
                  <img
                    src={productImage(featured.img_url)}
                    alt={featured.name}
                    className="h-full w-full object-cover transition-transform duration-700 group-hover:scale-[1.03]"
                  />
                </div>
                <div className="mt-3 flex items-baseline justify-between text-[12px]">
                  <span className="text-paper/80">{featured.name}</span>
                  <span className="tabular-nums text-paper/80">
                    {money(featured.price)}
                  </span>
                </div>
              </Link>
            ) : (
              <Skeleton className="aspect-[4/5] w-full bg-paper/20" />
            )}
          </div>
        </div>
      </section>

      {/* Values */}
      <section className="container-x mt-24">
        <div className="grid gap-10 border-t border-line pt-8 md:grid-cols-3 md:gap-14">
          {[
            {
              title: "Small batch",
              copy: "We buy in small runs so makers can keep their standards and their margins. When something is gone, it may stay gone.",
            },
            {
              title: "Made to repair",
              copy: "Replaceable pads, standard screws, honest materials. The best object is the one you do not need to replace.",
            },
            {
              title: "No mystery",
              copy: "Every listing says where it is made, what it is made of and what happens if something goes wrong.",
            },
          ].map((value) => (
            <div key={value.title}>
              <h3 className="font-display text-2xl tracking-[-0.035em]">
                {value.title}
              </h3>
              <p className="mt-3 max-w-sm text-sm leading-relaxed text-muted">
                {value.copy}
              </p>
            </div>
          ))}
        </div>
      </section>

      {/* Closing */}
      <section className="container-x mt-24">
        <div className="flex flex-col items-start justify-between gap-8 border-t border-ink pt-10 md:flex-row md:items-end">
          <p className="max-w-3xl font-display text-[clamp(2.4rem,5.6vw,4.6rem)] font-semibold leading-[0.96] tracking-[-0.05em]">
            Find something that stays.
          </p>
          <Link
            to="/shop"
            className="group inline-flex h-12 shrink-0 items-center gap-2 rounded-full border border-ink px-6 text-sm font-medium transition-colors hover:bg-ink hover:text-paper"
          >
            Browse the shop
            <ArrowUpRight className="h-4 w-4 transition-transform group-hover:rotate-45" />
          </Link>
        </div>
      </section>
    </div>
  );
}

function CategoryRow({
  category,
  count,
  onHover,
}: {
  category: Category;
  count: number;
  onHover: () => void;
}) {
  return (
    <Link
      to={`/shop?category=${category.id}`}
      onMouseEnter={onHover}
      onFocus={onHover}
      className="group flex items-center justify-between gap-4 border-b border-line py-6 md:py-7"
    >
      <span className="font-display text-[clamp(2rem,4.4vw,3.6rem)] leading-none tracking-[-0.05em] transition-all duration-300 group-hover:translate-x-1.5 group-hover:text-cobalt">
        {category.name}
      </span>
      <span className="flex shrink-0 items-center gap-4 md:gap-8">
        <span className="hidden text-[12px] tabular-nums text-muted sm:inline">
          {count} {count === 1 ? "object" : "objects"}
        </span>
        <ArrowUpRight className="h-5 w-5 text-ink/40 transition-all duration-300 group-hover:rotate-45 group-hover:text-cobalt" />
      </span>
    </Link>
  );
}
