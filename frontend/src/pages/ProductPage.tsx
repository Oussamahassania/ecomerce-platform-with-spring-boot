import { useMemo, useState } from "react";
import { Link, useLocation, useNavigate, useParams } from "react-router-dom";
import { motion, useReducedMotion } from "motion/react";
import {
  ArrowLeft,
  ArrowUpRight,
  Heart,
  Minus,
  Plus,
  ShieldCheck,
  ShoppingBag,
  Star,
  Truck,
} from "lucide-react";
import { toast } from "sonner";
import {
  useAddToCart,
  useAllProducts,
  useProduct,
  useProductRating,
} from "@/hooks/queries";
import { money, isNewProduct } from "@/lib/format";
import { productImage } from "@/lib/images";
import { useAuth } from "@/stores/auth";
import { useWishlist } from "@/stores/wishlist";
import { ProductReviews } from "@/components/product/ProductReviews";
import { ProductCard } from "@/components/product/ProductCard";
import {
  Badge,
  Button,
  EmptyState,
  Skeleton,
  Stars,
  cn,
} from "@/components/ui";

function Accordion({
  title,
  children,
  defaultOpen = false,
}: {
  title: string;
  children: React.ReactNode;
  defaultOpen?: boolean;
}) {
  return (
    <details
      open={defaultOpen}
      className="group border-b border-line [&_summary::-webkit-details-marker]:hidden"
    >
      <summary className="flex cursor-pointer list-none items-center justify-between gap-4 py-4 text-[13px] font-medium text-ink">
        {title}
        <span className="relative flex h-4 w-4 shrink-0 items-center justify-center">
          <span className="absolute h-px w-3.5 bg-ink" />
          <span className="absolute h-3.5 w-px bg-ink transition-transform duration-300 group-open:rotate-90 group-open:opacity-0" />
        </span>
      </summary>
      <div className="pb-5 text-[13px] leading-relaxed text-muted">
        {children}
      </div>
    </details>
  );
}

export function ProductPage() {
  const { id } = useParams();
  const productId = Number(id);
  const navigate = useNavigate();
  const location = useLocation();
  const reduce = useReducedMotion();
  const token = useAuth((state) => state.token);
  const wishlist = useWishlist();
  const addToCart = useAddToCart();

  const { data: product, isLoading, isError } = useProduct(
    Number.isFinite(productId) ? productId : undefined,
  );
  const { data: rating } = useProductRating(
    Number.isFinite(productId) ? productId : undefined,
  );
  const { data: allProducts } = useAllProducts();

  const [quantity, setQuantity] = useState(1);

  const related = useMemo(() => {
    if (!product || !allProducts) return [];
    const sameCategory = allProducts.filter(
      (item) =>
        item.id !== product.id &&
        item.categoryId === product.categoryId &&
        item.stock > 0,
    );
    const others = allProducts.filter(
      (item) =>
        item.id !== product.id &&
        item.categoryId !== product.categoryId &&
        item.stock > 0,
    );
    return [...sameCategory, ...others].slice(0, 4);
  }, [allProducts, product]);

  const saved = product ? wishlist.has(product.id) : false;
  const soldOut = (product?.stock ?? 0) <= 0;
  const lowStock = !soldOut && (product?.stock ?? 0) <= 5;

  function handleAdd() {
    if (!product) return;
    if (!token) {
      toast("Sign in to start a bag", {
        description: "Your bag is saved to your account.",
      });
      navigate(
        `/login?next=${encodeURIComponent(location.pathname + location.search)}`,
      );
      return;
    }
    addToCart.mutate(
      { productId: product.id, quantity },
      {
        onSuccess: () =>
          toast.success(
            `${quantity > 1 ? `${quantity} × ` : ""}${product.name} added to your bag`,
          ),
        onError: (error) => toast.error(error.message),
      },
    );
  }

  function handleWishlist() {
    if (!product) return;
    wishlist.toggle(product);
    toast.success(saved ? "Removed from wishlist" : "Saved to wishlist");
  }

  if (isError) {
    return (
      <div className="container-x py-16">
        <EmptyState
          title="This object is not on the shelf."
          description="It may have sold out or moved. The rest of the collection is still here."
          action={
            <Link
              to="/shop"
              className="inline-flex h-11 items-center rounded-full bg-ink px-6 text-sm font-medium text-paper"
            >
              Browse the shop
            </Link>
          }
        />
      </div>
    );
  }

  if (isLoading || !product) {
    return (
      <div className="container-x pt-8">
        <Skeleton className="h-3 w-52" />
        <div className="mt-8 grid gap-10 lg:grid-cols-12">
          <div className="lg:col-span-7">
            <Skeleton className="aspect-[4/5] w-full" />
          </div>
          <div className="space-y-4 lg:col-span-5">
            <Skeleton className="h-3 w-20" />
            <Skeleton className="h-14 w-3/4" />
            <Skeleton className="h-5 w-24" />
            <Skeleton className="h-24 w-full" />
            <Skeleton className="h-12 w-full" />
            <Skeleton className="h-12 w-full" />
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="pb-24 md:pb-0">
      <div className="container-x pt-6">
        <nav
          aria-label="Breadcrumb"
          className="flex flex-wrap items-center gap-1.5 text-[11px] text-muted"
        >
          <Link to="/" className="transition-colors hover:text-ink">
            Home
          </Link>
          <span aria-hidden>/</span>
          <Link to="/shop" className="transition-colors hover:text-ink">
            Shop
          </Link>
          {product.categoryId ? (
            <>
              <span aria-hidden>/</span>
              <Link
                to={`/shop?category=${product.categoryId}`}
                className="transition-colors hover:text-ink"
              >
                {product.categoryName}
              </Link>
            </>
          ) : null}
          <span aria-hidden>/</span>
          <span className="text-ink-2">{product.name}</span>
        </nav>

        <div className="mt-6 grid gap-10 lg:grid-cols-12 lg:gap-12">
          <div className="lg:col-span-7">
            <div className="lg:sticky lg:top-24">
              <motion.div
                initial={{ opacity: 0, scale: reduce ? 1 : 1.015 }}
                animate={{ opacity: 1, scale: 1 }}
                transition={{ duration: reduce ? 0 : 0.8, ease: [0.22, 1, 0.36, 1] }}
                className="group relative overflow-hidden bg-paper-2"
              >
                <div className="relative aspect-[4/5]">
                  <img
                    src={productImage(product.img_url)}
                    alt={product.name}
                    className="h-full w-full object-cover transition-transform duration-[1.2s] ease-[cubic-bezier(0.22,1,0.36,1)] group-hover:scale-[1.025]"
                  />
                  <div className="pointer-events-none absolute left-4 top-4 flex flex-wrap gap-1.5">
                    {soldOut ? (
                      <Badge tone="neutral">Sold out</Badge>
                    ) : isNewProduct(product.createdAt) ? (
                      <Badge tone="cobalt">New in</Badge>
                    ) : null}
                    {lowStock ? (
                      <Badge tone="butter">Only {product.stock} left</Badge>
                    ) : null}
                  </div>
                  <div className="absolute inset-x-0 bottom-0 flex items-end justify-between bg-gradient-to-t from-ink/45 to-transparent p-5 text-paper">
                    <p className="text-[11px] tracking-[0.08em] text-paper/75">
                      {product.categoryName ?? "FORMA EDITION"}
                    </p>
                    <p className="text-[11px] tabular-nums text-paper/75">
                      No. {String(product.id).padStart(2, "0")}
                    </p>
                  </div>
                </div>
              </motion.div>

              <div className="mt-4 hidden items-center justify-between border-t border-line pt-4 text-[11px] text-muted md:flex">
                <span className="inline-flex items-center gap-1.5">
                  <Truck className="h-3.5 w-3.5" /> Ships worldwide in 2–3 days
                </span>
                <span className="inline-flex items-center gap-1.5">
                  <ShieldCheck className="h-3.5 w-3.5" /> 30-day returns
                </span>
              </div>
            </div>
          </div>

          <div className="lg:col-span-5 lg:pt-2">
            <div className="flex items-center justify-between gap-3">
              <p className="text-[11px] uppercase tracking-[0.14em] text-muted">
                {product.categoryName ?? "Objects"}
              </p>
              <button
                type="button"
                onClick={handleWishlist}
                aria-pressed={saved}
                className={cn(
                  "inline-flex items-center gap-1.5 text-[12px] transition-colors",
                  saved ? "text-cobalt" : "text-muted hover:text-ink",
                )}
              >
                <Heart className={cn("h-3.5 w-3.5", saved && "fill-current")} />
                {saved ? "Saved" : "Save"}
              </button>
            </div>

            <h1 className="mt-3 font-display text-[clamp(2.8rem,5.6vw,4.8rem)] font-semibold leading-[0.92] tracking-[-0.055em]">
              {product.name}
            </h1>

            <button
              type="button"
              onClick={() =>
                document
                  .getElementById("reviews")
                  ?.scrollIntoView({ behavior: "smooth", block: "start" })
              }
              className="mt-4 inline-flex items-center gap-2 text-[12px] text-muted transition-colors hover:text-ink"
            >
              <Stars rating={rating?.averageRating ?? 0} size={13} />
              <span>
                {rating?.totalReviews
                  ? `${rating.averageRating.toFixed(1)} · ${rating.totalReviews} ${
                      rating.totalReviews === 1 ? "review" : "reviews"
                    }`
                  : "Be the first to review"}
              </span>
              <ArrowUpRight className="h-3 w-3" />
            </button>

            <div className="mt-6 flex items-baseline gap-3">
              <span className="font-display text-3xl tracking-[-0.04em] tabular-nums">
                {money(product.price)}
              </span>
              <span className="text-[11px] text-muted">
                incl. tax, shipping free
              </span>
            </div>

            <p className="mt-5 max-w-lg text-[15px] leading-relaxed text-ink-2">
              {product.description}
            </p>

            <div className="mt-6 flex items-center gap-2 text-[12px]">
              <span
                className={cn(
                  "h-1.5 w-1.5 rounded-full",
                  soldOut
                    ? "bg-danger"
                    : lowStock
                      ? "bg-butter"
                      : "bg-success",
                )}
              />
              <span className="text-muted">
                {soldOut
                  ? "Currently sold out — join the wishlist and we will notice."
                  : lowStock
                    ? `Only ${product.stock} left in this batch.`
                    : "In stock — packed and sent within two working days."}
              </span>
            </div>

            <div className="mt-7 flex flex-col gap-3 sm:flex-row">
              <div className="inline-flex h-12 shrink-0 items-center rounded-full border border-ink/20">
                <button
                  type="button"
                  aria-label="Decrease quantity"
                  disabled={soldOut || quantity <= 1}
                  onClick={() => setQuantity((value) => Math.max(1, value - 1))}
                  className="flex h-12 w-11 items-center justify-center rounded-l-full transition-colors hover:bg-ink/[0.06] disabled:opacity-30"
                >
                  <Minus className="h-4 w-4" />
                </button>
                <span className="min-w-7 text-center text-sm font-medium tabular-nums">
                  {quantity}
                </span>
                <button
                  type="button"
                  aria-label="Increase quantity"
                  disabled={soldOut || quantity >= product.stock}
                  onClick={() =>
                    setQuantity((value) =>
                      Math.min(product.stock || 1, value + 1),
                    )
                  }
                  className="flex h-12 w-11 items-center justify-center rounded-r-full transition-colors hover:bg-ink/[0.06] disabled:opacity-30"
                >
                  <Plus className="h-4 w-4" />
                </button>
              </div>
              <Button
                size="lg"
                onClick={handleAdd}
                disabled={soldOut || addToCart.isPending}
                className="h-12 flex-1"
              >
                <ShoppingBag className="h-4 w-4" />
                {soldOut
                  ? "Sold out"
                  : addToCart.isPending
                    ? "Adding to bag…"
                    : "Add to bag"}
              </Button>
            </div>

            <div className="mt-4 grid grid-cols-2 gap-2">
              <Button
                variant="outline"
                onClick={handleWishlist}
                className="h-11"
              >
                <Heart className={cn("h-4 w-4", saved && "fill-current")} />
                {saved ? "In wishlist" : "Wishlist"}
              </Button>
              <Link
                to="/cart"
                className="inline-flex h-11 items-center justify-center rounded-full border border-ink/25 text-sm font-medium transition-colors hover:border-ink hover:bg-ink hover:text-paper"
              >
                View bag
              </Link>
            </div>

            <p className="mt-4 text-[11px] leading-relaxed text-muted">
              Checkout is simulated for this demo. No card is charged and no
              real delivery is arranged.
            </p>

            <div className="mt-8 border-t border-line">
              <Accordion title="Shipping & returns" defaultOpen>
                <p>
                  Orders leave the studio within two working days and ship
                  worldwide. Delivery is free. If an object is not right for the
                  room, send it back within 30 days in its original packaging.
                </p>
              </Accordion>
              <Accordion title="Care">
                <p>
                  Dust with a soft, dry cloth. Keep liquids away from electrical
                  pieces and avoid direct sunlight on natural finishes. Small
                  marks are part of how honest materials age.
                </p>
              </Accordion>
              <Accordion title="Why it is here">
                <p>
                  We stock objects we would keep ourselves: quiet forms, honest
                  materials and a shape that still feels right after the
                  novelty has worn off.
                </p>
              </Accordion>
            </div>
          </div>
        </div>
      </div>

      <div className="container-x mt-20 md:mt-28">
        <ProductReviews productId={product.id} productName={product.name} />
      </div>

      {related.length > 0 ? (
        <section className="container-x mt-20 md:mt-28">
          <div className="flex flex-wrap items-end justify-between gap-4 border-b border-line pb-6">
            <div>
              <p className="text-[11px] uppercase tracking-[0.14em] text-muted">
                Keep looking
              </p>
              <h2 className="mt-2 font-display text-[clamp(2rem,3.8vw,3.2rem)] leading-none tracking-[-0.045em]">
                Pairs well with
              </h2>
            </div>
            <Link
              to="/shop"
              className="link-underline inline-flex items-center gap-1.5 text-[13px] font-medium"
            >
              Shop all
              <ArrowUpRight className="h-3.5 w-3.5" />
            </Link>
          </div>
          <div className="mt-8 grid grid-cols-2 gap-x-4 gap-y-10 md:grid-cols-4 md:gap-x-6">
            {related.map((item) => (
              <ProductCard key={item.id} product={item} />
            ))}
          </div>
        </section>
      ) : null}

      <div className="container-x mt-16">
        <button
          type="button"
          onClick={() => navigate("/shop")}
          className="inline-flex items-center gap-2 text-[13px] text-muted transition-colors hover:text-ink"
        >
          <ArrowLeft className="h-4 w-4" />
          Back to the shop
        </button>
      </div>

      <div className="h-20 md:hidden" aria-hidden />

      {/* Mobile purchase bar */}
      <div className="fixed inset-x-0 bottom-0 z-40 border-t border-line bg-paper/95 px-4 py-3 backdrop-blur-xl md:hidden">
        <div className="flex items-center gap-3">
          <div className="min-w-0 flex-1">
            <p className="truncate text-[12px] font-medium">{product.name}</p>
            <p className="text-[13px] tabular-nums text-muted">
              {money(product.price)}
            </p>
          </div>
          <Button
            onClick={handleAdd}
            disabled={soldOut || addToCart.isPending}
            className="h-11 shrink-0 px-5"
          >
            <ShoppingBag className="h-4 w-4" />
            {soldOut ? "Sold out" : "Add to bag"}
          </Button>
        </div>
      </div>
    </div>
  );
}
