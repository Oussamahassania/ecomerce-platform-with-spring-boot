import { useState } from "react";
import { Link } from "react-router-dom";
import { motion } from "motion/react";
import { ArrowLeft, ArrowRight, Minus, Plus, ShieldCheck, Trash2, Truck } from "lucide-react";
import { toast } from "sonner";
import {
  useCart,
  useClearCart,
  useRemoveCartItem,
  useUpdateCartItem,
} from "@/hooks/queries";
import { money } from "@/lib/format";
import { productImage } from "@/lib/images";
import { useUi } from "@/stores/ui";
import {
  Button,
  EmptyState,
  QuantityStepper,
  Skeleton,
  cn,
} from "@/components/ui";
import { ConfirmDialog } from "@/components/ui/Overlay";

export function CartPage() {
  const { data: cart, isLoading } = useCart();
  const updateItem = useUpdateCartItem();
  const removeItem = useRemoveCartItem();
  const clearCart = useClearCart();
  const openCart = useUi((state) => state.openCart);
  const [confirmClear, setConfirmClear] = useState(false);

  const count =
    cart?.items.reduce((total, item) => total + item.quantity, 0) ?? 0;

  if (isLoading) {
    return (
      <div className="container-x py-12">
        <Skeleton className="h-3 w-24" />
        <Skeleton className="mt-4 h-14 w-64" />
        <div className="mt-10 grid gap-10 lg:grid-cols-12">
          <div className="space-y-6 lg:col-span-8">
            {Array.from({ length: 2 }).map((_, index) => (
              <div key={index} className="flex gap-5 border-b border-line pb-6">
                <Skeleton className="h-36 w-28 shrink-0" />
                <div className="flex-1 space-y-3">
                  <Skeleton className="h-4 w-1/2" />
                  <Skeleton className="h-3 w-20" />
                  <Skeleton className="h-10 w-32" />
                </div>
              </div>
            ))}
          </div>
          <div className="lg:col-span-4">
            <Skeleton className="h-64 w-full" />
          </div>
        </div>
      </div>
    );
  }

  if (!cart || cart.items.length === 0) {
    return (
      <div className="container-x py-16">
        <EmptyState
          title="Your bag is empty."
          description="Nothing in here yet. The newest objects are a good place to start — or revisit the things you saved."
          action={
            <div className="flex flex-wrap justify-center gap-2">
              <Link
                to="/shop"
                className="inline-flex h-11 items-center rounded-full bg-ink px-6 text-sm font-medium text-paper transition-colors hover:bg-ink-2"
              >
                Shop all objects
              </Link>
              <Link
                to="/wishlist"
                className="inline-flex h-11 items-center rounded-full border border-ink/25 px-6 text-sm font-medium transition-colors hover:border-ink"
              >
                Open wishlist
              </Link>
            </div>
          }
        />
      </div>
    );
  }

  return (
    <div className="container-x py-10 md:py-14">
      <div className="flex flex-wrap items-end justify-between gap-4 border-b border-line pb-6">
        <div>
          <Link
            to="/shop"
            className="inline-flex items-center gap-1.5 text-[12px] text-muted transition-colors hover:text-ink"
          >
            <ArrowLeft className="h-3.5 w-3.5" />
            Continue shopping
          </Link>
          <h1 className="mt-3 font-display text-[clamp(2.6rem,5.6vw,4.6rem)] font-semibold leading-[0.94] tracking-[-0.05em]">
            Your bag
            <span className="ml-2 align-top font-sans text-sm font-normal tabular-nums text-muted">
              ({count})
            </span>
          </h1>
        </div>
        <button
          type="button"
          onClick={() => setConfirmClear(true)}
          className="text-[12px] text-muted transition-colors hover:text-danger"
        >
          Clear bag
        </button>
      </div>

      <div className="mt-8 grid gap-10 lg:grid-cols-12 lg:gap-14">
        <div className="lg:col-span-8">
          <ul className="divide-y divide-line border-t border-line">
            {cart.items.map((item, index) => {
              const busy =
                (updateItem.isPending &&
                  updateItem.variables?.itemId === item.id) ||
                (removeItem.isPending && removeItem.variables === item.id);
              return (
                <motion.li
                  key={item.id}
                  layout
                  initial={{ opacity: 0, y: 10 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: index * 0.04, duration: 0.35 }}
                  className={cn(
                    "flex gap-4 py-6 transition-opacity sm:gap-6",
                    busy && "opacity-60",
                  )}
                >
                  <Link
                    to={`/product/${item.productId}`}
                    className="h-32 w-24 shrink-0 overflow-hidden bg-paper-2 sm:h-40 sm:w-32"
                  >
                    <img
                      src={productImage(item.productImage)}
                      alt={item.productName}
                      className="h-full w-full object-cover transition-transform duration-500 hover:scale-[1.04]"
                    />
                  </Link>

                  <div className="flex min-w-0 flex-1 flex-col justify-between gap-4">
                    <div className="flex items-start justify-between gap-4">
                      <div className="min-w-0">
                        <Link
                          to={`/product/${item.productId}`}
                          className="font-display text-xl tracking-[-0.03em] transition-colors hover:text-cobalt sm:text-2xl"
                        >
                          {item.productName}
                        </Link>
                        <p className="mt-1 text-[12px] tabular-nums text-muted">
                          {money(item.priceAtAdding)} each
                        </p>
                      </div>
                      <button
                        type="button"
                        aria-label={`Remove ${item.productName}`}
                        disabled={busy}
                        onClick={() =>
                          removeItem.mutate(item.id, {
                            onSuccess: () => toast.success("Item removed"),
                            onError: (error) => toast.error(error.message),
                          })
                        }
                        className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-muted transition-colors hover:bg-danger/[0.08] hover:text-danger disabled:opacity-40"
                      >
                        <Trash2 className="h-4 w-4" />
                      </button>
                    </div>

                    <div className="flex flex-wrap items-center justify-between gap-3">
                      <QuantityStepper
                        size="sm"
                        value={item.quantity}
                        disabled={busy}
                        onChange={(value) =>
                          updateItem.mutate(
                            { itemId: item.id, quantity: value },
                            {
                              onError: (error) => toast.error(error.message),
                            },
                          )
                        }
                      />
                      <p className="text-[15px] font-medium tabular-nums">
                        {money(item.subtotal)}
                      </p>
                    </div>
                  </div>
                </motion.li>
              );
            })}
          </ul>

          <div className="mt-6 flex flex-wrap items-center gap-x-6 gap-y-2 text-[12px] text-muted">
            <span className="inline-flex items-center gap-1.5">
              <Truck className="h-3.5 w-3.5" /> Free worldwide shipping
            </span>
            <span className="inline-flex items-center gap-1.5">
              <ShieldCheck className="h-3.5 w-3.5" /> 30-day returns
            </span>
            <button
              type="button"
              onClick={openCart}
              className="text-ink underline decoration-ink/30 underline-offset-4 hover:decoration-ink"
            >
              Open quick bag
            </button>
          </div>
        </div>

        <aside className="lg:col-span-4">
          <div className="sticky top-24 border border-ink/15 bg-paper-3/60 p-6">
            <h2 className="font-display text-2xl tracking-[-0.035em]">
              Order summary
            </h2>
            <dl className="mt-5 space-y-3 border-b border-line pb-5 text-[13px]">
              <div className="flex items-center justify-between">
                <dt className="text-muted">Subtotal</dt>
                <dd className="tabular-nums">{money(cart.total)}</dd>
              </div>
              <div className="flex items-center justify-between">
                <dt className="text-muted">Shipping</dt>
                <dd className="text-success">Free</dd>
              </div>
              <div className="flex items-center justify-between">
                <dt className="text-muted">Tax</dt>
                <dd className="text-muted">Included</dd>
              </div>
            </dl>
            <div className="mt-5 flex items-baseline justify-between">
              <span className="text-sm font-medium">Total</span>
              <span className="font-display text-3xl tracking-[-0.04em] tabular-nums">
                {money(cart.total)}
              </span>
            </div>

            <Link
              to="/checkout"
              className="group mt-6 flex h-13 w-full items-center justify-between rounded-full bg-cobalt px-6 text-sm font-medium text-white transition-colors hover:bg-cobalt-ink"
            >
              Checkout
              <ArrowRight className="h-4 w-4 transition-transform group-hover:translate-x-0.5" />
            </Link>

            <p className="mt-4 text-[11px] leading-relaxed text-muted">
              Checkout is simulated — no card is charged. You will choose a
              payment method on the next step.
            </p>

            <div className="mt-6 space-y-2.5 border-t border-line pt-5 text-[12px] text-muted">
              <p className="flex items-center gap-2">
                <ShieldCheck className="h-4 w-4 text-ink/60" />
                Secure, simulated payment
              </p>
              <p className="flex items-center gap-2">
                <Truck className="h-4 w-4 text-ink/60" />
                Packed in two working days
              </p>
            </div>
          </div>
        </aside>
      </div>

      <ConfirmDialog
        open={confirmClear}
        onClose={() => setConfirmClear(false)}
        onConfirm={() =>
          clearCart.mutate(undefined, {
            onSuccess: () => {
              setConfirmClear(false);
              toast.success("Bag cleared");
            },
            onError: (error) => toast.error(error.message),
          })
        }
        title="Clear your bag?"
        description="Every item will be removed. Saved wishlist pieces stay put."
        confirmLabel="Clear bag"
        loading={clearCart.isPending}
      />
    </div>
  );
}
