import type { ReactNode } from "react";
import { Link } from "react-router-dom";
import { motion, useReducedMotion } from "motion/react";
import { ArrowLeft } from "lucide-react";
import { productImage } from "@/lib/images";

export function AuthShell({
  eyebrow,
  title,
  description,
  image,
  quote,
  attribution,
  children,
  footer,
}: {
  eyebrow: string;
  title: ReactNode;
  description?: string;
  image: string;
  quote: string;
  attribution: string;
  children: ReactNode;
  footer?: ReactNode;
}) {
  const reduce = useReducedMotion();

  return (
    <div className="container-x py-8 md:py-12">
      <div className="grid min-h-[70vh] overflow-hidden border border-ink/10 lg:grid-cols-12">
        <div className="order-2 flex flex-col justify-between p-6 sm:p-10 lg:order-1 lg:col-span-7 lg:p-14">
          <div>
            <div className="flex items-center justify-between gap-4">
              <Link
                to="/"
                className="font-display text-2xl font-bold leading-none tracking-[-0.07em]"
              >
                FORMA
                <span className="ml-0.5 inline-block h-1.5 w-1.5 rounded-full bg-cobalt align-baseline" />
              </Link>
              <Link
                to="/shop"
                className="inline-flex items-center gap-1.5 text-[12px] text-muted transition-colors hover:text-ink"
              >
                <ArrowLeft className="h-3.5 w-3.5" />
                Back to shop
              </Link>
            </div>

            <div className="mt-12 max-w-md">
              <p className="text-[11px] uppercase tracking-[0.16em] text-muted">
                {eyebrow}
              </p>
              <h1 className="mt-3 font-display text-[clamp(2.8rem,5.6vw,4.6rem)] font-semibold leading-[0.9] tracking-[-0.055em]">
                {title}
              </h1>
              {description ? (
                <p className="mt-4 text-sm leading-relaxed text-muted">
                  {description}
                </p>
              ) : null}

              <div className="mt-8">{children}</div>
            </div>
          </div>

          {footer ? (
            <div className="mt-12 border-t border-line pt-5 text-[13px] text-muted">
              {footer}
            </div>
          ) : null}
        </div>

        <div className="relative order-1 min-h-56 overflow-hidden bg-ink lg:order-2 lg:col-span-5">
          <motion.img
            src={productImage(image)}
            alt=""
            initial={{ scale: reduce ? 1 : 1.06 }}
            animate={{ scale: 1 }}
            transition={{ duration: reduce ? 0 : 1.4, ease: [0.22, 1, 0.36, 1] }}
            className="absolute inset-0 h-full w-full object-cover opacity-90"
          />
          <div className="absolute inset-0 bg-gradient-to-t from-ink/85 via-ink/20 to-transparent" />
          <div className="absolute inset-x-0 bottom-0 p-6 text-paper sm:p-10">
            <p className="max-w-sm font-display text-2xl leading-[1.1] tracking-[-0.035em] md:text-3xl">
              “{quote}”
            </p>
            <p className="mt-3 text-[11px] text-paper/60">{attribution}</p>
          </div>
        </div>
      </div>
    </div>
  );
}
