import { Link, useLocation, useNavigate } from "react-router-dom";
import { Heart, Plus } from "lucide-react";
import { toast } from "sonner";
import type { Product } from "@/lib/types";
import { money, isNewProduct } from "@/lib/format";
import { productImage } from "@/lib/images";
import { useAddToCart } from "@/hooks/queries";
import { useAuth } from "@/stores/auth";
import { useWishlist } from "@/stores/wishlist";
import { Badge, cn } from "@/components/ui";

export function ProductCard({
  product,
  priority = false,
  compact = false,
  hideWishlist = false,
}: {
  product: Product;
  priority?: boolean;
  compact?: boolean;
  hideWishlist?: boolean;
}) {
  const navigate = useNavigate();
  const location = useLocation();
  const token = useAuth((state) => state.token);
  const wishlist = useWishlist();
  const saved = wishlist.has(product.id);
  const addToCart = useAddToCart();
  const soldOut = product.stock <= 0;
  const lowStock = product.stock > 0 && product.stock <= 5;

  function handleAdd() {
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
      { productId: product.id, quantity: 1 },
      {
        onSuccess: () => toast.success(`${product.name} added to your bag`),
        onError: (error) => toast.error(error.message),
      },
    );
  }

  function handleWishlist() {
    wishlist.toggle(product);
    toast.success(saved ? "Removed from wishlist" : "Saved to wishlist");
  }

  return (
    <article className={cn("group relative", compact && "w-full")}>
      <div className="relative">
        <Link
          to={`/product/${product.id}`}
          className="block overflow-hidden bg-paper-2"
          aria-label={product.name}
        >
          <div className="relative aspect-[4/5]">
            <img
              src={productImage(product.img_url)}
              alt={product.name}
              loading={priority ? "eager" : "lazy"}
              className={cn(
                "h-full w-full object-cover transition-transform duration-700 ease-[cubic-bezier(0.22,1,0.36,1)] group-hover:scale-[1.035]",
                soldOut && "opacity-60 grayscale-[0.35]",
              )}
            />
            <div className="pointer-events-none absolute left-3 top-3 flex flex-wrap gap-1.5">
              {soldOut ? (
                <Badge tone="neutral">Sold out</Badge>
              ) : isNewProduct(product.createdAt) ? (
                <Badge tone="cobalt">New in</Badge>
              ) : null}
              {lowStock ? <Badge tone="butter">Only {product.stock} left</Badge> : null}
            </div>
          </div>
        </Link>

        {hideWishlist ? null : (
          <button
            type="button"
            onClick={handleWishlist}
            aria-label={saved ? "Remove from wishlist" : "Save to wishlist"}
            aria-pressed={saved}
            className={cn(
              "absolute right-3 top-3 flex h-9 w-9 items-center justify-center rounded-full backdrop-blur-md transition-colors",
              saved
                ? "bg-ink text-paper"
                : "bg-paper/80 text-ink hover:bg-paper",
            )}
          >
            <Heart className={cn("h-4 w-4", saved && "fill-current")} />
          </button>
        )}

        {!soldOut ? (
          <div className="absolute inset-x-3 bottom-3 translate-y-0 opacity-100 transition-all duration-300 md:translate-y-2 md:opacity-0 md:group-hover:translate-y-0 md:group-hover:opacity-100 md:group-focus-within:translate-y-0 md:group-focus-within:opacity-100">
            <button
              type="button"
              onClick={handleAdd}
              disabled={addToCart.isPending}
              className="flex h-11 w-full items-center justify-between rounded-full bg-paper/95 px-4 text-[13px] font-medium text-ink shadow-[0_8px_24px_rgba(20,21,15,0.12)] backdrop-blur-md transition-colors hover:bg-cobalt hover:text-white disabled:opacity-60"
            >
              <span>{addToCart.isPending ? "Adding…" : "Add to bag"}</span>
              <Plus className="h-4 w-4" />
            </button>
          </div>
        ) : null}
      </div>

      <Link
        to={`/product/${product.id}`}
        className="mt-3 flex flex-col gap-0.5 sm:flex-row sm:items-start sm:justify-between sm:gap-4"
      >
        <div className="min-w-0">
          {product.categoryName ? (
            <p className="text-[11px] text-muted">{product.categoryName}</p>
          ) : null}
          <h3 className="mt-0.5 truncate text-[15px] font-medium tracking-[-0.01em] text-ink">
            {product.name}
          </h3>
        </div>
        <p className="shrink-0 text-[15px] font-medium tabular-nums text-ink">
          {money(product.price)}
        </p>
      </Link>
    </article>
  );
}
