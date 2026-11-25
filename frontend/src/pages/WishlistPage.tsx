import { useMemo, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { motion } from "motion/react";
import { Heart, ShoppingBag, Trash2, X } from "lucide-react";
import { toast } from "sonner";
import { useAddToCart, useAllProducts } from "@/hooks/queries";
import { money } from "@/lib/format";
import { productImage } from "@/lib/images";
import { useAuth } from "@/stores/auth";
import { useWishlist } from "@/stores/wishlist";
import { ProductCard } from "@/components/product/ProductCard";
import { Button, EmptyState, Skeleton } from "@/components/ui";
import { ConfirmDialog } from "@/components/ui/Overlay";

export function WishlistPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const token = useAuth((state) => state.token);
  const wishlist = useWishlist();
  const { data: products, isLoading } = useAllProducts();
  const addToCart = useAddToCart();
  const [confirmClear, setConfirmClear] = useState(false);
  const [addingAll, setAddingAll] = useState(false);

  const items = wishlist.items;
  const productMap = useMemo(
    () => new Map((products ?? []).map((product) => [product.id, product])),
    [products],
  );
  const available = items
    .map((item) => productMap.get(item.id))
    .filter((product): product is NonNullable<typeof product> => Boolean(product));
  const missing = items.filter((item) => !productMap.has(item.id));

  function promptSignIn() {
    toast("Sign in to start a bag", {
      description: "Your wishlist stays saved in this browser.",
    });
    navigate(
      `/login?next=${encodeURIComponent(location.pathname + location.search)}`,
    );
  }

  function add(productId: number, name: string) {
    if (!token) {
      promptSignIn();
      return;
    }
    addToCart.mutate(
      { productId, quantity: 1 },
      {
        onSuccess: () => toast.success(`${name} added to your bag`),
        onError: (error) => toast.error(error.message),
      },
    );
  }

  async function addAll() {
    if (!token) {
      promptSignIn();
      return;
    }
    setAddingAll(true);
    let added = 0;
    try {
      for (const product of available) {
        await addToCart.mutateAsync({ productId: product.id, quantity: 1 });
        added += 1;
      }
    } catch (error) {
      toast.error((error as Error).message);
    } finally {
      setAddingAll(false);
    }
    if (added > 0) {
      toast.success(
        `${added} ${added === 1 ? "object" : "objects"} added to your bag`,
      );
    }
  }

  function remove(id: number) {
    wishlist.remove(id);
    toast.success("Removed from wishlist");
  }

  return (
    <div className="container-x py-10 md:py-14">
      <div className="flex flex-wrap items-end justify-between gap-4 border-b border-line pb-6">
        <div>
          <p className="text-[11px] uppercase tracking-[0.14em] text-muted">
            Saved for later
          </p>
          <h1 className="mt-2 font-display text-[clamp(2.6rem,5.6vw,4.6rem)] font-semibold leading-[0.94] tracking-[-0.05em]">
            Wishlist
            {items.length > 0 ? (
              <span className="ml-2 align-top font-sans text-sm font-normal tabular-nums text-muted">
                ({items.length})
              </span>
            ) : null}
          </h1>
          <p className="mt-3 max-w-lg text-sm leading-relaxed text-muted">
            Objects you are thinking about. Prices here follow the current
            shelf, not the moment you saved them.
          </p>
        </div>
        <div className="flex items-center gap-2">
          {items.length > 0 ? (
            <button
              type="button"
              onClick={() => setConfirmClear(true)}
              className="text-[12px] text-muted transition-colors hover:text-danger"
            >
              Clear wishlist
            </button>
          ) : null}
          <Link
            to="/shop"
            className="link-underline inline-flex items-center gap-1.5 text-[13px] font-medium"
          >
            Keep looking
          </Link>
        </div>
      </div>

      {items.length === 0 ? (
        <EmptyState
          title="Nothing saved yet."
          description="Tap the heart on any object to keep it here while you think it over."
          action={
            <Link
              to="/shop"
              className="inline-flex h-11 items-center gap-2 rounded-full bg-ink px-6 text-sm font-medium text-paper"
            >
              <Heart className="h-4 w-4" />
              Find something to save
            </Link>
          }
        />
      ) : isLoading ? (
        <div className="mt-8 grid grid-cols-2 gap-x-4 gap-y-10 md:grid-cols-3 xl:grid-cols-4 md:gap-x-6">
          {items.map((item) => (
            <div key={item.id}>
              <Skeleton className="aspect-[4/5] w-full" />
              <Skeleton className="mt-3 h-3.5 w-2/3" />
            </div>
          ))}
        </div>
      ) : (
        <>
          {available.length > 0 ? (
            <div className="mt-8 grid grid-cols-2 gap-x-4 gap-y-10 md:grid-cols-3 md:gap-x-6 xl:grid-cols-4">
              {available.map((product, index) => (
                <div key={product.id} className="relative">
                  <ProductCard
                    product={product}
                    priority={index < 4}
                    hideWishlist
                  />
                  <button
                    type="button"
                    onClick={() => remove(product.id)}
                    aria-label={`Remove ${product.name} from wishlist`}
                    className="absolute -right-1 -top-1 z-10 flex h-8 w-8 items-center justify-center rounded-full bg-paper text-muted shadow-[0_4px_16px_rgba(20,21,15,0.12)] transition-colors hover:bg-danger hover:text-white"
                  >
                    <X className="h-3.5 w-3.5" />
                  </button>
                </div>
              ))}
            </div>
          ) : null}

          {missing.length > 0 ? (
            <section className="mt-12">
              <h2 className="font-display text-2xl tracking-[-0.035em]">
                No longer on the shelf
              </h2>
              <p className="mt-1 text-[12px] text-muted">
                These pieces have sold out or left the collection.
              </p>
              <ul className="mt-5 divide-y divide-line border-t border-line">
                {missing.map((item, index) => (
                  <motion.li
                    key={item.id}
                    initial={{ opacity: 0, y: 8 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ delay: index * 0.04, duration: 0.3 }}
                    className="flex items-center gap-4 py-4"
                  >
                    <div className="h-16 w-14 shrink-0 overflow-hidden bg-paper-2 opacity-70">
                      <img
                        src={productImage(item.image)}
                        alt=""
                        className="h-full w-full object-cover grayscale"
                      />
                    </div>
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-[13px] font-medium">
                        {item.name}
                      </p>
                      <p className="text-[11px] text-muted">
                        {item.categoryName ?? "Object"} · {money(item.price)}
                      </p>
                    </div>
                    <button
                      type="button"
                      onClick={() => remove(item.id)}
                      aria-label={`Remove ${item.name} from wishlist`}
                      className="flex h-9 w-9 items-center justify-center rounded-full text-muted transition-colors hover:bg-danger/[0.08] hover:text-danger"
                    >
                      <Trash2 className="h-4 w-4" />
                    </button>
                  </motion.li>
                ))}
              </ul>
            </section>
          ) : null}

          {available.length > 0 ? (
            <div className="mt-10 flex flex-wrap items-center justify-between gap-4 border-t border-line pt-5">
              <p className="text-[11px] text-muted">
                {token
                  ? "Saved objects live in this browser, on this device."
                  : "Saved objects live in this browser. Sign in when you are ready to check out."}
              </p>
              <Button
                variant="outline"
                onClick={addAll}
                disabled={addToCart.isPending || addingAll}
              >
                <ShoppingBag className="h-4 w-4" />
                {addToCart.isPending || addingAll
                  ? "Adding…"
                  : available.length === 1
                    ? "Add to bag"
                    : `Add all ${available.length} to bag`}
              </Button>
            </div>
          ) : null}
        </>
      )}

      <ConfirmDialog
        open={confirmClear}
        onClose={() => setConfirmClear(false)}
        onConfirm={() => {
          for (const item of items) wishlist.remove(item.id);
          setConfirmClear(false);
          toast.success("Wishlist cleared");
        }}
        title="Clear your wishlist?"
        description="Every saved object will be removed from this browser."
        confirmLabel="Clear wishlist"
      />
    </div>
  );
}
