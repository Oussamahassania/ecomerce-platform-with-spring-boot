import { Link } from "react-router-dom";
import { Minus, Plus, Trash2 } from "lucide-react";
import { toast } from "sonner";
import { useCart, useRemoveCartItem, useUpdateCartItem } from "@/hooks/queries";
import { money } from "@/lib/format";
import { productImage } from "@/lib/images";
import { useUi } from "@/stores/ui";
import { Button, EmptyState, Skeleton, cn } from "@/components/ui";
import { Drawer } from "@/components/ui/Overlay";

export function CartDrawer() {
  const open = useUi((state) => state.cartOpen);
  const close = useUi((state) => state.closeCart);
  const { data: cart, isLoading } = useCart();
  const updateItem = useUpdateCartItem();
  const removeItem = useRemoveCartItem();
  const count =
    cart?.items.reduce((total, item) => total + item.quantity, 0) ?? 0;

  return (
    <Drawer
      open={open}
      onClose={close}
      title={`Your bag${count ? ` (${count})` : ""}`}
      footer={
        cart && cart.items.length > 0 ? (
          <div className="space-y-4">
            <div className="flex items-baseline justify-between">
              <span className="text-sm text-muted">Subtotal</span>
              <span className="font-display text-2xl tracking-[-0.03em] tabular-nums">
                {money(cart.total)}
              </span>
            </div>
            <p className="text-[12px] leading-relaxed text-muted">
              Shipping and taxes are included. Checkout is simulated — no card
              is charged.
            </p>
            <Link
              to="/checkout"
              onClick={close}
              className="flex h-12 w-full items-center justify-center rounded-full bg-cobalt text-sm font-medium text-white transition-colors hover:bg-cobalt-ink"
            >
              Continue to checkout
            </Link>
            <Link
              to="/cart"
              onClick={close}
              className="block text-center text-[13px] text-ink underline-offset-4 hover:underline"
            >
              View full bag
            </Link>
          </div>
        ) : null
      }
    >
      {isLoading ? (
        <div className="space-y-6 p-6">
          {Array.from({ length: 2 }).map((_, index) => (
            <div key={index} className="flex gap-4">
              <Skeleton className="h-28 w-24 shrink-0" />
              <div className="flex-1 space-y-3">
                <Skeleton className="h-3.5 w-2/3" />
                <Skeleton className="h-3.5 w-16" />
                <Skeleton className="h-8 w-28" />
              </div>
            </div>
          ))}
        </div>
      ) : !cart || cart.items.length === 0 ? (
        <EmptyState
          title="Your bag is empty."
          description="Good things take a little looking. Start with the newest objects in the shop."
          action={
            <Link
              to="/shop"
              onClick={close}
              className="inline-flex h-11 items-center justify-center rounded-full bg-ink px-6 text-sm font-medium text-paper transition-colors hover:bg-ink-2"
            >
              Shop all objects
            </Link>
          }
        />
      ) : (
        <ul className="divide-y divide-line">
          {cart.items.map((item) => {
            const busy =
              (updateItem.isPending &&
                updateItem.variables?.itemId === item.id) ||
              (removeItem.isPending && removeItem.variables === item.id);
            return (
              <li key={item.id} className="flex gap-4 px-6 py-5">
                <Link
                  to={`/product/${item.productId}`}
                  onClick={close}
                  className="h-28 w-24 shrink-0 overflow-hidden bg-paper-2"
                >
                  <img
                    src={productImage(item.productImage)}
                    alt={item.productName}
                    className="h-full w-full object-cover"
                  />
                </Link>
                <div className="flex min-w-0 flex-1 flex-col justify-between">
                  <div>
                    <div className="flex items-start justify-between gap-3">
                      <Link
                        to={`/product/${item.productId}`}
                        onClick={close}
                        className="text-sm font-medium leading-snug hover:underline"
                      >
                        {item.productName}
                      </Link>
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
                        className="text-muted transition-colors hover:text-danger disabled:opacity-40"
                      >
                        <Trash2 className="h-4 w-4" />
                      </button>
                    </div>
                    <p className="mt-1 text-[13px] tabular-nums text-muted">
                      {money(item.priceAtAdding)} each
                    </p>
                  </div>
                  <div className="flex items-center justify-between gap-3">
                    <div className="inline-flex h-9 items-center rounded-full border border-ink/20">
                      <button
                        type="button"
                        aria-label="Decrease quantity"
                        disabled={busy || item.quantity <= 1}
                        onClick={() =>
                          updateItem.mutate(
                            { itemId: item.id, quantity: item.quantity - 1 },
                            {
                              onError: (error) => toast.error(error.message),
                            },
                          )
                        }
                        className="flex h-9 w-9 items-center justify-center rounded-l-full transition-colors hover:bg-ink/[0.06] disabled:opacity-30"
                      >
                        <Minus className="h-3.5 w-3.5" />
                      </button>
                      <span
                        className={cn(
                          "min-w-7 text-center text-[13px] font-medium tabular-nums",
                          busy && "opacity-40",
                        )}
                      >
                        {item.quantity}
                      </span>
                      <button
                        type="button"
                        aria-label="Increase quantity"
                        disabled={busy}
                        onClick={() =>
                          updateItem.mutate(
                            { itemId: item.id, quantity: item.quantity + 1 },
                            {
                              onError: (error) => toast.error(error.message),
                            },
                          )
                        }
                        className="flex h-9 w-9 items-center justify-center rounded-r-full transition-colors hover:bg-ink/[0.06] disabled:opacity-30"
                      >
                        <Plus className="h-3.5 w-3.5" />
                      </button>
                    </div>
                    <span className="text-sm font-medium tabular-nums">
                      {money(item.subtotal)}
                    </span>
                  </div>
                </div>
              </li>
            );
          })}
        </ul>
      )}
    </Drawer>
  );
}
