import { useEffect, useRef } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { motion, useReducedMotion } from "motion/react";
import { ArrowRight, CircleAlert, CircleCheck, Loader2, Mail } from "lucide-react";
import { useVerifyEmail } from "@/hooks/queries";

export function VerifyPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get("token");
  const verify = useVerifyEmail();
  const started = useRef(false);
  const reduce = useReducedMotion();

  useEffect(() => {
    if (!token || started.current) return;
    started.current = true;
    verify.mutate(token);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token]);

  const state = !token
    ? "missing"
    : verify.isPending
      ? "pending"
      : verify.isError
        ? "error"
        : verify.isSuccess
          ? "success"
          : "pending";

  return (
    <div className="container-x flex min-h-[68vh] items-center justify-center py-16">
      <motion.div
        initial={{ opacity: 0, y: reduce ? 0 : 12 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5 }}
        className="w-full max-w-xl border border-line bg-paper-3/50 p-7 text-center sm:p-10"
      >
        <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-ink text-paper">
          {state === "success" ? (
            <CircleCheck className="h-6 w-6" />
          ) : state === "error" || state === "missing" ? (
            <CircleAlert className="h-6 w-6" />
          ) : state === "pending" ? (
            <Loader2 className="h-6 w-6 animate-spin" />
          ) : (
            <Mail className="h-6 w-6" />
          )}
        </div>

        <p className="mt-6 text-[11px] uppercase tracking-[0.16em] text-muted">
          Email verification
        </p>

        {state === "success" ? (
          <>
            <h1 className="mt-3 font-display text-[clamp(2.4rem,5vw,3.6rem)] leading-[0.94] tracking-[-0.05em]">
              You are verified<span className="text-cobalt">.</span>
            </h1>
            <p className="mx-auto mt-4 max-w-sm text-sm leading-relaxed text-muted">
              {verify.data ?? "Your email is confirmed."} You can sign in now.
            </p>
            <Link
              to="/login"
              className="mt-7 inline-flex h-12 items-center gap-2 rounded-full bg-cobalt px-7 text-sm font-medium text-white transition-colors hover:bg-cobalt-ink"
            >
              Sign in
              <ArrowRight className="h-4 w-4" />
            </Link>
          </>
        ) : state === "error" || state === "missing" ? (
          <>
            <h1 className="mt-3 font-display text-[clamp(2.4rem,5vw,3.6rem)] leading-[0.94] tracking-[-0.05em]">
              That link did not work<span className="text-cobalt">.</span>
            </h1>
            <p className="mx-auto mt-4 max-w-sm text-sm leading-relaxed text-muted">
              {state === "missing"
                ? "The verification token is missing from this address."
                : verify.error?.message ??
                  "The token may have expired or already been used."}
            </p>
            <div className="mt-7 flex flex-wrap justify-center gap-2">
              <Link
                to="/register"
                className="inline-flex h-11 items-center rounded-full bg-ink px-6 text-sm font-medium text-paper"
              >
                Create an account
              </Link>
              <Link
                to="/login"
                className="inline-flex h-11 items-center rounded-full border border-ink/25 px-6 text-sm font-medium transition-colors hover:border-ink"
              >
                Go to sign in
              </Link>
            </div>
          </>
        ) : (
          <>
            <h1 className="mt-3 font-display text-[clamp(2.4rem,5vw,3.6rem)] leading-[0.94] tracking-[-0.05em]">
              Checking the link<span className="text-cobalt">…</span>
            </h1>
            <p className="mx-auto mt-4 max-w-sm text-sm leading-relaxed text-muted">
              One moment while we confirm the token with the shop.
            </p>
          </>
        )}

        <Link
          to="/"
          className="mt-8 inline-block text-[12px] text-muted underline decoration-line underline-offset-4 transition-colors hover:text-ink"
        >
          Back to the shop
        </Link>
      </motion.div>
    </div>
  );
}
