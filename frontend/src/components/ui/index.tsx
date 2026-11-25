import type {
  ButtonHTMLAttributes,
  InputHTMLAttributes,
  ReactNode,
  SelectHTMLAttributes,
  TextareaHTMLAttributes,
} from "react";
import { ChevronDown, ChevronLeft, ChevronRight, Loader2, Minus, Plus, Star } from "lucide-react";
import clsx from "clsx";
import type { OrderStatus, PaymentStatus } from "@/lib/types";

export const cn = clsx;

/* -------------------------------- button --------------------------------- */

type Variant =
  | "primary"
  | "ink"
  | "butter"
  | "outline"
  | "ghost"
  | "danger"
  | "dangerGhost"
  | "subtle";
type Size = "sm" | "md" | "lg" | "icon";

const variantClasses: Record<Variant, string> = {
  primary: "bg-cobalt text-white hover:bg-cobalt-ink",
  ink: "bg-ink text-paper hover:bg-ink-2",
  butter: "bg-butter text-ink hover:bg-butter/85",
  outline: "border border-ink/25 text-ink hover:border-ink hover:bg-ink hover:text-paper",
  ghost: "text-ink hover:bg-ink/[0.06]",
  danger: "bg-danger text-white hover:bg-danger/90",
  dangerGhost: "text-danger hover:bg-danger/[0.08]",
  subtle: "bg-ink/[0.06] text-ink hover:bg-ink/10",
};

const sizeClasses: Record<Size, string> = {
  sm: "h-9 px-4 text-[13px]",
  md: "h-11 px-5 text-sm",
  lg: "h-14 px-7 text-[15px]",
  icon: "h-10 w-10 p-0",
};

export function buttonStyles({
  variant = "primary",
  size = "md",
  className,
}: {
  variant?: Variant;
  size?: Size;
  className?: string;
} = {}) {
  return cn(
    "inline-flex select-none items-center justify-center gap-2 whitespace-nowrap rounded-full font-medium leading-none transition-[background-color,color,border-color,transform,opacity] duration-200 active:scale-[0.985] disabled:pointer-events-none disabled:opacity-40",
    variantClasses[variant],
    sizeClasses[size],
    className,
  );
}

export function Button({
  variant = "primary",
  size = "md",
  className,
  children,
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: Variant;
  size?: Size;
}) {
  return (
    <button
      className={buttonStyles({ variant, size, className })}
      {...props}
    >
      {children}
    </button>
  );
}

/* --------------------------------- fields -------------------------------- */

export function Field({
  label,
  htmlFor,
  hint,
  error,
  children,
  className,
}: {
  label: string;
  htmlFor?: string;
  hint?: string;
  error?: string;
  children: ReactNode;
  className?: string;
}) {
  return (
    <div className={cn("space-y-2", className)}>
      <label
        htmlFor={htmlFor}
        className="block text-[13px] font-medium text-ink-2"
      >
        {label}
      </label>
      {children}
      {error ? (
        <p className="text-[12px] leading-snug text-danger">{error}</p>
      ) : hint ? (
        <p className="text-[12px] leading-snug text-muted">{hint}</p>
      ) : null}
    </div>
  );
}

export function Input({
  className,
  ...props
}: InputHTMLAttributes<HTMLInputElement>) {
  return (
    <input
      className={cn(
        "h-12 w-full rounded-sm border border-ink/20 bg-paper-3/50 px-4 text-[15px] text-ink placeholder:text-muted/70 transition-colors focus:border-ink focus:outline-none disabled:opacity-50",
        className,
      )}
      {...props}
    />
  );
}

export function Textarea({
  className,
  ...props
}: TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return (
    <textarea
      className={cn(
        "min-h-32 w-full resize-y rounded-sm border border-ink/20 bg-paper-3/50 px-4 py-3 text-[15px] leading-relaxed text-ink placeholder:text-muted/70 transition-colors focus:border-ink focus:outline-none disabled:opacity-50",
        className,
      )}
      {...props}
    />
  );
}

export function Select({
  className,
  children,
  ...props
}: SelectHTMLAttributes<HTMLSelectElement>) {
  return (
    <div className="relative">
      <select
        className={cn(
          "h-12 w-full appearance-none rounded-sm border border-ink/20 bg-paper-3/50 pl-4 pr-10 text-[15px] text-ink transition-colors focus:border-ink focus:outline-none disabled:opacity-50",
          className,
        )}
        {...props}
      >
        {children}
      </select>
      <ChevronDown
        aria-hidden
        className="pointer-events-none absolute right-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-ink/50"
      />
    </div>
  );
}

/* --------------------------------- badges -------------------------------- */

type BadgeTone =
  | "neutral"
  | "cobalt"
  | "butter"
  | "success"
  | "danger"
  | "outline";

const badgeTones: Record<BadgeTone, string> = {
  neutral: "bg-ink/[0.07] text-ink-2",
  cobalt: "bg-cobalt text-white",
  butter: "bg-butter text-ink",
  success: "bg-success text-white",
  danger: "bg-danger text-white",
  outline: "border border-ink/25 text-ink-2",
};

export function Badge({
  tone = "neutral",
  className,
  children,
}: {
  tone?: BadgeTone;
  className?: string;
  children: ReactNode;
}) {
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-[11px] font-medium leading-none",
        badgeTones[tone],
        className,
      )}
    >
      {children}
    </span>
  );
}

export function OrderStatusBadge({ status }: { status: OrderStatus }) {
  const map: Record<OrderStatus, { tone: BadgeTone; label: string }> = {
    PENDING: { tone: "butter", label: "Pending" },
    CONFIRMED: { tone: "cobalt", label: "Confirmed" },
    SHIPPED: { tone: "neutral", label: "Shipped" },
    DELIVERED: { tone: "success", label: "Delivered" },
    CANCELLED: { tone: "danger", label: "Cancelled" },
  };
  const item = map[status];
  return <Badge tone={item.tone}>{item.label}</Badge>;
}

export function PaymentStatusBadge({ status }: { status: PaymentStatus }) {
  const map: Record<PaymentStatus, { tone: BadgeTone; label: string }> = {
    PENDING: { tone: "outline", label: "Payment pending" },
    PAID: { tone: "success", label: "Paid" },
    FAILED: { tone: "danger", label: "Payment failed" },
    REFUNDED: { tone: "neutral", label: "Refunded" },
  };
  const item = map[status];
  return <Badge tone={item.tone}>{item.label}</Badge>;
}

/* -------------------------------- feedback ------------------------------- */

export function Spinner({ className }: { className?: string }) {
  return <Loader2 className={cn("h-4 w-4 animate-spin", className)} />;
}

export function Skeleton({ className }: { className?: string }) {
  return (
    <div
      aria-hidden
      className={cn("animate-shimmer rounded-sm bg-ink/[0.09]", className)}
    />
  );
}

export function EmptyState({
  title,
  description,
  action,
  className,
}: {
  title: string;
  description?: string;
  action?: ReactNode;
  className?: string;
}) {
  return (
    <div
      className={cn(
        "flex flex-col items-center justify-center px-6 py-20 text-center",
        className,
      )}
    >
      <p className="max-w-md font-display text-3xl tracking-[-0.04em] text-ink">
        {title}
      </p>
      {description ? (
        <p className="mt-3 max-w-sm text-sm leading-relaxed text-muted">
          {description}
        </p>
      ) : null}
      {action ? <div className="mt-7">{action}</div> : null}
    </div>
  );
}

/* --------------------------------- rating -------------------------------- */

export function Stars({
  rating,
  size = 14,
  className,
}: {
  rating: number;
  size?: number;
  className?: string;
}) {
  const rounded = Math.round(rating);
  return (
    <span
      className={cn("inline-flex items-center gap-0.5", className)}
      role="img"
      aria-label={`${rating.toFixed(1)} out of 5 stars`}
    >
      {Array.from({ length: 5 }).map((_, index) => (
        <Star
          key={index}
          width={size}
          height={size}
          strokeWidth={1.6}
          className={
            index < rounded
              ? "fill-ink text-ink"
              : "fill-transparent text-ink/25"
          }
        />
      ))}
    </span>
  );
}

/* ------------------------------- quantities ------------------------------ */

export function QuantityStepper({
  value,
  onChange,
  min = 1,
  max = 99,
  size = "md",
  disabled,
}: {
  value: number;
  onChange: (value: number) => void;
  min?: number;
  max?: number;
  size?: "sm" | "md";
  disabled?: boolean;
}) {
  const button =
    "flex items-center justify-center text-ink transition-colors hover:bg-ink/[0.06] disabled:opacity-30 disabled:hover:bg-transparent";
  return (
    <div
      className={cn(
        "inline-flex items-center rounded-full border border-ink/20",
        size === "sm" ? "h-9" : "h-12",
      )}
    >
      <button
        type="button"
        aria-label="Decrease quantity"
        className={cn(
          button,
          "rounded-l-full",
          size === "sm" ? "h-9 w-9" : "h-12 w-12",
        )}
        disabled={disabled || value <= min}
        onClick={() => onChange(Math.max(min, value - 1))}
      >
        <Minus className={size === "sm" ? "h-3.5 w-3.5" : "h-4 w-4"} />
      </button>
      <span
        className={cn(
          "min-w-8 text-center font-medium tabular-nums",
          size === "sm" ? "text-[13px]" : "text-sm",
        )}
      >
        {value}
      </span>
      <button
        type="button"
        aria-label="Increase quantity"
        className={cn(
          button,
          "rounded-r-full",
          size === "sm" ? "h-9 w-9" : "h-12 w-12",
        )}
        disabled={disabled || value >= max}
        onClick={() => onChange(Math.min(max, value + 1))}
      >
        <Plus className={size === "sm" ? "h-3.5 w-3.5" : "h-4 w-4"} />
      </button>
    </div>
  );
}

/* --------------------------------- layout -------------------------------- */

export function SectionHeader({
  title,
  description,
  action,
  className,
}: {
  title: string;
  description?: string;
  action?: ReactNode;
  className?: string;
}) {
  return (
    <div
      className={cn(
        "flex flex-wrap items-end justify-between gap-4 border-b border-line pb-6",
        className,
      )}
    >
      <div>
        <h2 className="font-display text-[clamp(1.9rem,3.4vw,3rem)] leading-[1.02] tracking-[-0.045em]">
          {title}
        </h2>
        {description ? (
          <p className="mt-2 max-w-lg text-sm leading-relaxed text-muted">
            {description}
          </p>
        ) : null}
      </div>
      {action}
    </div>
  );
}

export function Pagination({
  page,
  totalPages,
  onChange,
}: {
  page: number;
  totalPages: number;
  onChange: (page: number) => void;
}) {
  if (totalPages <= 1) return null;
  const start = Math.max(0, Math.min(page - 2, totalPages - 5));
  const pages = Array.from(
    { length: Math.min(5, totalPages) },
    (_, index) => start + index,
  );

  return (
    <nav
      aria-label="Pagination"
      className="mt-14 flex items-center justify-center gap-1.5"
    >
      <button
        type="button"
        aria-label="Previous page"
        disabled={page === 0}
        onClick={() => onChange(page - 1)}
        className="flex h-10 w-10 items-center justify-center rounded-full border border-ink/15 transition-colors hover:bg-ink hover:text-paper disabled:pointer-events-none disabled:opacity-30"
      >
        <ChevronLeft className="h-4 w-4" />
      </button>
      {pages.map((item) => (
        <button
          key={item}
          type="button"
          aria-current={item === page ? "page" : undefined}
          onClick={() => onChange(item)}
          className={cn(
            "h-10 min-w-10 rounded-full px-3 text-[13px] font-medium tabular-nums transition-colors",
            item === page
              ? "bg-cobalt text-white"
              : "text-ink hover:bg-ink/[0.06]",
          )}
        >
          {item + 1}
        </button>
      ))}
      <button
        type="button"
        aria-label="Next page"
        disabled={page >= totalPages - 1}
        onClick={() => onChange(page + 1)}
        className="flex h-10 w-10 items-center justify-center rounded-full border border-ink/15 transition-colors hover:bg-ink hover:text-paper disabled:pointer-events-none disabled:opacity-30"
      >
        <ChevronRight className="h-4 w-4" />
      </button>
    </nav>
  );
}
