import { useEffect, useMemo, useRef, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { motion, useReducedMotion } from "motion/react";
import {
  ArrowLeft,
  ArrowRight,
  Check,
  CreditCard,
  Lock,
  ShieldCheck,
  Truck,
} from "lucide-react";
import { toast } from "sonner";
import {
  useCart,
  useCheckout,
  useMe,
  usePayOrder,
} from "@/hooks/queries";
import { money } from "@/lib/format";
import { productImage } from "@/lib/images";
import {
  Button,
  EmptyState,
  Field,
  Input,
  Select,
  Skeleton,
  cn,
} from "@/components/ui";

interface CheckoutDetails {
  fullName: string;
  email: string;
  phone: string;
  address1: string;
  address2: string;
  city: string;
  postalCode: string;
  country: string;
}

const EMPTY_DETAILS: CheckoutDetails = {
  fullName: "",
  email: "",
  phone: "",
  address1: "",
  address2: "",
  city: "",
  postalCode: "",
  country: "Ireland",
};

const STORAGE_KEY = "forma.checkout";

const COUNTRIES = [
  "Ireland",
  "United Kingdom",
  "France",
  "Germany",
  "Netherlands",
  "Spain",
  "Italy",
  "Portugal",
  "Sweden",
  "Denmark",
  "United States",
  "Canada",
  "Japan",
  "Australia",
];

function readSavedDetails(): CheckoutDetails {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return EMPTY_DETAILS;
    return { ...EMPTY_DETAILS, ...(JSON.parse(raw) as Partial<CheckoutDetails>) };
  } catch {
    return EMPTY_DETAILS;
  }
}

function newIdempotencyKey() {
  if (typeof crypto !== "undefined" && "randomUUID" in crypto) {
    return crypto.randomUUID();
  }
  return `forma-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

export function CheckoutPage() {
  const navigate = useNavigate();
  const reduce = useReducedMotion();
  const { data: cart, isLoading } = useCart();
  const { data: me } = useMe();
  const checkout = useCheckout();
  const pay = usePayOrder();
  const idempotencyKey = useRef(newIdempotencyKey());

  const [details, setDetails] = useState<CheckoutDetails>(readSavedDetails);
  const [saveDetails, setSaveDetails] = useState(true);
  const [errors, setErrors] = useState<Partial<Record<keyof CheckoutDetails, string>>>({});
  const [placing, setPlacing] = useState(false);

  useEffect(() => {
    if (!me) return;
    setDetails((current) => ({
      ...current,
      fullName: current.fullName || me.fullName,
      email: current.email || me.email,
    }));
  }, [me]);

  const count =
    cart?.items.reduce((total, item) => total + item.quantity, 0) ?? 0;

  const summary = useMemo(() => {
    const items = cart?.items ?? [];
    return {
      items,
      total: cart?.total ?? 0,
    };
  }, [cart]);

  function update<K extends keyof CheckoutDetails>(
    key: K,
    value: CheckoutDetails[K],
  ) {
    setDetails((current) => ({ ...current, [key]: value }));
    setErrors((current) => ({ ...current, [key]: undefined }));
  }

  function validate() {
    const next: Partial<Record<keyof CheckoutDetails, string>> = {};
    if (!details.fullName.trim()) next.fullName = "Who should we address this to?";
    if (!details.email.trim()) next.email = "We need an email for the receipt.";
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(details.email.trim()))
      next.email = "That email does not look right.";
    if (!details.address1.trim()) next.address1 = "Add a street and number.";
    if (!details.city.trim()) next.city = "Add a city or town.";
    if (!details.postalCode.trim()) next.postalCode = "Add a postal code.";
    if (!details.country.trim()) next.country = "Choose a country.";
    setErrors(next);
    return Object.keys(next).length === 0;
  }

  async function placeOrder(event: React.FormEvent) {
    event.preventDefault();
    if (!cart || cart.items.length === 0) return;
    if (!validate()) {
      toast.error("A few delivery details are missing");
      return;
    }

    setPlacing(true);
    if (saveDetails) {
      try {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(details));
      } catch {
        /* storage may be unavailable */
      }
    }

    let orderId: number | null = null;
    try {
      const order = await checkout.mutateAsync();
      orderId = order.id;
      const payment = await pay.mutateAsync({
        orderId: order.id,
        paymentMethod: "card",
        idempotencyKey: idempotencyKey.current,
      });
      if (payment.status === "PAID") {
        toast.success("Order placed — payment approved");
        navigate(`/orders/${order.id}?placed=1`, { replace: true });
      } else {
        toast.error("Payment was declined. You can retry from the order page.", {
          description: "The order is saved and your bag has been cleared.",
        });
        navigate(`/orders/${order.id}?payment=failed`, { replace: true });
      }
    } catch (error) {
      if (orderId) {
        toast.error("The order was created, but payment did not go through.", {
          description: "Retry the payment from the order page.",
        });
        navigate(`/orders/${orderId}`, { replace: true });
      } else {
        toast.error((error as Error).message);
        setPlacing(false);
      }
    }
  }

  if (isLoading) {
    return (
      <div className="container-x py-12">
        <Skeleton className="h-3 w-32" />
        <Skeleton className="mt-4 h-14 w-72" />
        <div className="mt-10 grid gap-10 lg:grid-cols-12">
          <div className="space-y-5 lg:col-span-7">
            <Skeleton className="h-72 w-full" />
            <Skeleton className="h-72 w-full" />
          </div>
          <div className="lg:col-span-5">
            <Skeleton className="h-96 w-full" />
          </div>
        </div>
      </div>
    );
  }

  if ((!cart || cart.items.length === 0) && !placing) {
    return (
      <div className="container-x py-16">
        <EmptyState
          title="There is nothing to check out."
          description="Your bag is empty. Add an object or two, then come back here."
          action={
            <Link
              to="/shop"
              className="inline-flex h-11 items-center rounded-full bg-ink px-6 text-sm font-medium text-paper"
            >
              Shop all objects
            </Link>
          }
        />
      </div>
    );
  }

  return (
    <div className="container-x py-10 md:py-14">
      <div className="border-b border-line pb-6">
        <Link
          to="/cart"
          className="inline-flex items-center gap-1.5 text-[12px] text-muted transition-colors hover:text-ink"
        >
          <ArrowLeft className="h-3.5 w-3.5" />
          Back to bag
        </Link>
        <div className="mt-3 flex flex-wrap items-end justify-between gap-4">
          <h1 className="font-display text-[clamp(2.6rem,5.6vw,4.6rem)] font-semibold leading-[0.94] tracking-[-0.05em]">
            Checkout
          </h1>
          <ol className="flex items-center gap-2 pb-1 text-[11px] text-muted">
            <li className="text-ink">Bag</li>
            <li aria-hidden>→</li>
            <li className="font-medium text-cobalt">Details & payment</li>
            <li aria-hidden>→</li>
            <li>Done</li>
          </ol>
        </div>
      </div>

      <form
        onSubmit={placeOrder}
        noValidate
        className="mt-8 grid gap-10 lg:grid-cols-12 lg:gap-14"
      >
        <div className="space-y-10 lg:col-span-7">
          <motion.section
            initial={{ opacity: 0, y: reduce ? 0 : 12 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.45 }}
          >
            <div className="flex items-baseline gap-3 border-b border-line pb-3">
              <span className="font-display text-lg text-muted">01</span>
              <h2 className="font-display text-2xl tracking-[-0.035em]">
                Contact
              </h2>
            </div>
            <div className="mt-5 grid gap-4 sm:grid-cols-2">
              <Field
                label="Full name"
                htmlFor="checkout-name"
                error={errors.fullName}
              >
                <Input
                  id="checkout-name"
                  autoComplete="name"
                  value={details.fullName}
                  onChange={(event) => update("fullName", event.target.value)}
                  placeholder="Nora Bennett"
                />
              </Field>
              <Field
                label="Email"
                htmlFor="checkout-email"
                error={errors.email}
              >
                <Input
                  id="checkout-email"
                  type="email"
                  autoComplete="email"
                  value={details.email}
                  onChange={(event) => update("email", event.target.value)}
                  placeholder="you@example.com"
                />
              </Field>
              <Field
                label="Phone (optional)"
                htmlFor="checkout-phone"
                className="sm:col-span-2 sm:max-w-xs"
                hint="Only used if the courier needs to reach you."
              >
                <Input
                  id="checkout-phone"
                  type="tel"
                  autoComplete="tel"
                  value={details.phone}
                  onChange={(event) => update("phone", event.target.value)}
                  placeholder="+353 …"
                />
              </Field>
            </div>
          </motion.section>

          <motion.section
            initial={{ opacity: 0, y: reduce ? 0 : 12 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.45, delay: 0.06 }}
          >
            <div className="flex items-baseline gap-3 border-b border-line pb-3">
              <span className="font-display text-lg text-muted">02</span>
              <h2 className="font-display text-2xl tracking-[-0.035em]">
                Delivery
              </h2>
            </div>
            <div className="mt-5 grid gap-4 sm:grid-cols-2">
              <Field
                label="Address"
                htmlFor="checkout-address1"
                error={errors.address1}
                className="sm:col-span-2"
              >
                <Input
                  id="checkout-address1"
                  autoComplete="address-line1"
                  value={details.address1}
                  onChange={(event) => update("address1", event.target.value)}
                  placeholder="Street and number"
                />
              </Field>
              <Field
                label="Apartment, suite (optional)"
                htmlFor="checkout-address2"
                className="sm:col-span-2"
              >
                <Input
                  id="checkout-address2"
                  autoComplete="address-line2"
                  value={details.address2}
                  onChange={(event) => update("address2", event.target.value)}
                  placeholder="Unit, floor, building"
                />
              </Field>
              <Field label="City" htmlFor="checkout-city" error={errors.city}>
                <Input
                  id="checkout-city"
                  autoComplete="address-level2"
                  value={details.city}
                  onChange={(event) => update("city", event.target.value)}
                  placeholder="Dublin"
                />
              </Field>
              <Field
                label="Postal code"
                htmlFor="checkout-postal"
                error={errors.postalCode}
              >
                <Input
                  id="checkout-postal"
                  autoComplete="postal-code"
                  value={details.postalCode}
                  onChange={(event) => update("postalCode", event.target.value)}
                  placeholder="D02 X285"
                />
              </Field>
              <Field
                label="Country"
                htmlFor="checkout-country"
                error={errors.country}
                className="sm:col-span-2 sm:max-w-xs"
              >
                <Select
                  id="checkout-country"
                  autoComplete="country-name"
                  value={details.country}
                  onChange={(event) => update("country", event.target.value)}
                >
                  {COUNTRIES.map((country) => (
                    <option key={country} value={country}>
                      {country}
                    </option>
                  ))}
                </Select>
              </Field>
            </div>
            <label className="mt-4 flex cursor-pointer items-start gap-2.5 text-[12px] text-muted">
              <input
                type="checkbox"
                checked={saveDetails}
                onChange={(event) => setSaveDetails(event.target.checked)}
                className="mt-0.5 h-4 w-4 accent-cobalt"
              />
              Keep these details in this browser for next time. This demo does
              not send delivery details to a server.
            </label>
          </motion.section>

          <motion.section
            initial={{ opacity: 0, y: reduce ? 0 : 12 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.45, delay: 0.12 }}
          >
            <div className="flex items-baseline gap-3 border-b border-line pb-3">
              <span className="font-display text-lg text-muted">03</span>
              <h2 className="font-display text-2xl tracking-[-0.035em]">
                Payment
              </h2>
            </div>
            <div className="mt-5 space-y-3">
              <div className="flex items-start gap-4 rounded-sm border border-cobalt bg-cobalt/[0.04] p-4">
                <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-cobalt text-white">
                  <CreditCard className="h-4 w-4" />
                </span>
                <div className="flex-1">
                  <div className="flex items-center justify-between gap-3">
                    <p className="text-sm font-medium">Card — simulated</p>
                    <span className="flex h-5 w-5 items-center justify-center rounded-full bg-cobalt text-white">
                      <Check className="h-3 w-3" />
                    </span>
                  </div>
                  <p className="mt-1 text-[12px] leading-relaxed text-muted">
                    No card details are collected. The demo gateway approves
                    roughly nine in ten attempts, so you can test retrying a
                    declined payment.
                  </p>
                </div>
              </div>

              <div className="flex items-start gap-3 rounded-sm border border-line p-4 text-[12px] leading-relaxed text-muted">
                <Lock className="mt-0.5 h-4 w-4 shrink-0" />
                <p>
                  Payment is processed against the order after it is created.
                  If it is declined, the order stays in your account with a
                  “Payment failed” status and a retry button.
                </p>
              </div>
            </div>
          </motion.section>
        </div>

        <aside className="lg:col-span-5">
          <div className="sticky top-24 border border-ink/15 bg-paper-3/60 p-5 md:p-6">
            <div className="flex items-baseline justify-between">
              <h2 className="font-display text-2xl tracking-[-0.035em]">
                Your order
              </h2>
              <span className="text-[11px] tabular-nums text-muted">
                {count} {count === 1 ? "item" : "items"}
              </span>
            </div>

            <ul className="mt-5 max-h-80 space-y-3 overflow-y-auto pr-1">
              {summary.items.map((item) => (
                <li key={item.id} className="flex gap-3">
                  <div className="relative h-16 w-14 shrink-0 overflow-hidden bg-paper-2">
                    <img
                      src={productImage(item.productImage)}
                      alt={item.productName}
                      className="h-full w-full object-cover"
                    />
                    <span className="absolute right-0 top-0 flex h-4 min-w-4 items-center justify-center bg-ink px-1 text-[9px] font-semibold text-paper">
                      {item.quantity}
                    </span>
                  </div>
                  <div className="flex min-w-0 flex-1 items-start justify-between gap-2">
                    <div className="min-w-0">
                      <p className="truncate text-[13px] font-medium">
                        {item.productName}
                      </p>
                      <p className="text-[11px] text-muted">
                        {money(item.priceAtAdding)} each
                      </p>
                    </div>
                    <p className="shrink-0 text-[12px] tabular-nums">
                      {money(item.subtotal)}
                    </p>
                  </div>
                </li>
              ))}
            </ul>

            <dl className="mt-5 space-y-2.5 border-t border-line pt-4 text-[13px]">
              <div className="flex justify-between">
                <dt className="text-muted">Subtotal</dt>
                <dd className="tabular-nums">{money(summary.total)}</dd>
              </div>
              <div className="flex justify-between">
                <dt className="text-muted">Shipping</dt>
                <dd className="text-success">Free</dd>
              </div>
              <div className="flex justify-between">
                <dt className="text-muted">Tax</dt>
                <dd className="text-muted">Included</dd>
              </div>
            </dl>
            <div className="mt-4 flex items-baseline justify-between border-t border-line pt-4">
              <span className="text-sm font-medium">Total</span>
              <span className="font-display text-3xl tracking-[-0.04em] tabular-nums">
                {money(summary.total)}
              </span>
            </div>

            <Button
              type="submit"
              size="lg"
              disabled={placing || !cart || cart.items.length === 0}
              className="mt-6 w-full justify-between bg-cobalt hover:bg-cobalt-ink"
            >
              <span>{placing ? "Placing order…" : "Place order"}</span>
              <ArrowRight className="h-4 w-4" />
            </Button>

            <div className="mt-5 grid gap-2 text-[11px] text-muted">
              <span className="inline-flex items-center gap-2">
                <ShieldCheck className="h-3.5 w-3.5" /> Simulated secure payment
              </span>
              <span className="inline-flex items-center gap-2">
                <Truck className="h-3.5 w-3.5" /> Free worldwide shipping
              </span>
            </div>
          </div>
        </aside>
      </form>

      <div
        aria-hidden
        className={cn(
          "pointer-events-none fixed inset-0 z-[70] flex items-center justify-center bg-paper/70 backdrop-blur-sm transition-opacity",
          placing ? "opacity-100" : "opacity-0",
        )}
      >
        {placing ? (
          <div className="flex flex-col items-center gap-3 text-center">
            <span className="relative flex h-12 w-12 items-center justify-center">
              <span className="absolute inset-0 animate-ping rounded-full bg-cobalt/20" />
              <span className="relative flex h-10 w-10 items-center justify-center rounded-full bg-cobalt text-white">
                <Lock className="h-4 w-4" />
              </span>
            </span>
            <p className="font-display text-2xl tracking-[-0.03em]">
              Creating your order…
            </p>
            <p className="max-w-xs text-[12px] text-muted">
              Reserving stock and talking to the (simulated) payment gateway.
            </p>
          </div>
        ) : null}
      </div>
    </div>
  );
}
