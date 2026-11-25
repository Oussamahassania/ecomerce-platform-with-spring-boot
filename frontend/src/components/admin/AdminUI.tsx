import type { ReactNode } from "react";
import { cn } from "@/components/ui";

export function AdminPageHeader({
  eyebrow,
  title,
  description,
  action,
}: {
  eyebrow?: string;
  title: string;
  description?: string;
  action?: ReactNode;
}) {
  return (
    <div className="flex flex-wrap items-end justify-between gap-4 border-b border-line pb-5">
      <div>
        {eyebrow ? (
          <p className="text-[10px] uppercase tracking-[0.16em] text-muted">
            {eyebrow}
          </p>
        ) : null}
        <h1 className="mt-1.5 font-display text-[clamp(2.2rem,4.4vw,3.4rem)] font-semibold leading-[0.95] tracking-[-0.05em]">
          {title}
        </h1>
        {description ? (
          <p className="mt-2 max-w-xl text-[13px] leading-relaxed text-muted">
            {description}
          </p>
        ) : null}
      </div>
      {action ? <div className="flex flex-wrap items-center gap-2">{action}</div> : null}
    </div>
  );
}

export function StatCard({
  label,
  value,
  icon,
  detail,
  tone = "neutral",
  delay = 0,
}: {
  label: string;
  value: ReactNode;
  icon: ReactNode;
  detail?: string;
  tone?: "neutral" | "cobalt" | "butter" | "danger";
  delay?: number;
}) {
  const tones = {
    neutral: "bg-ink/[0.05] text-ink",
    cobalt: "bg-cobalt text-white",
    butter: "bg-butter text-ink",
    danger: "bg-danger/[0.08] text-danger",
  };

  return (
    <div
      className="border border-line bg-paper p-5 transition-colors hover:border-ink/25"
      style={{ animationDelay: `${delay}ms` }}
    >
      <div className="flex items-start justify-between gap-3">
        <p className="text-[11px] uppercase tracking-[0.12em] text-muted">
          {label}
        </p>
        <span
          className={cn(
            "flex h-8 w-8 items-center justify-center rounded-full",
            tones[tone],
          )}
        >
          {icon}
        </span>
      </div>
      <p className="mt-5 font-display text-[clamp(1.8rem,3vw,2.4rem)] leading-none tracking-[-0.045em] tabular-nums">
        {value}
      </p>
      {detail ? <p className="mt-2 text-[11px] text-muted">{detail}</p> : null}
    </div>
  );
}

export function TableShell({
  children,
  className,
}: {
  children: ReactNode;
  className?: string;
}) {
  return (
    <div className={cn("overflow-hidden border border-line bg-paper", className)}>
      {children}
    </div>
  );
}

export function AdminEmpty({
  title,
  description,
  action,
}: {
  title: string;
  description?: string;
  action?: ReactNode;
}) {
  return (
    <div className="flex flex-col items-center justify-center px-6 py-16 text-center">
      <p className="font-display text-2xl tracking-[-0.035em]">{title}</p>
      {description ? (
        <p className="mt-2 max-w-sm text-[13px] leading-relaxed text-muted">
          {description}
        </p>
      ) : null}
      {action ? <div className="mt-5">{action}</div> : null}
    </div>
  );
}
