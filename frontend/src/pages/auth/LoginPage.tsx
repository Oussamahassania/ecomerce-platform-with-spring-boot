import { useEffect, useState } from "react";
import { Link, useLocation, useNavigate, useSearchParams } from "react-router-dom";
import { ArrowRight, Eye, EyeOff, Lock, Mail } from "lucide-react";
import { toast } from "sonner";
import { useLogin } from "@/hooks/queries";
import { useAuth } from "@/stores/auth";
import { AuthShell } from "@/components/auth/AuthShell";
import { Button, Field, Input, cn } from "@/components/ui";

function safeNext(raw: string | null) {
  if (raw && raw.startsWith("/") && !raw.startsWith("//")) return raw;
  return "/account";
}

export function LoginPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const location = useLocation();
  const next = safeNext(searchParams.get("next"));
  const token = useAuth((state) => state.token);
  const login = useLogin();

  const stateEmail =
    (location.state as { email?: string } | null)?.email ?? "";

  const [email, setEmail] = useState(stateEmail);
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);

  useEffect(() => {
    if (token) navigate(next, { replace: true });
  }, [token, next, navigate]);

  function submit(event: React.FormEvent) {
    event.preventDefault();
    login.mutate(
      { email: email.trim(), password },
      {
        onSuccess: () => {
          toast.success("Welcome back");
          navigate(next, { replace: true });
        },
      },
    );
  }

  function fill(emailValue: string, passwordValue: string) {
    setEmail(emailValue);
    setPassword(passwordValue);
  }

  const nextQuery = next !== "/account" ? `?next=${encodeURIComponent(next)}` : "";

  return (
    <AuthShell
      eyebrow="Sign in"
      title={
        <>
          Good to see
          <br />
          you again<span className="text-cobalt">.</span>
        </>
      }
      description="Your bag, orders and wishlist are waiting where you left them."
      image="/images/products/column-lamp.jpg"
      quote="The objects you keep should make the room feel more like you."
      attribution="FORMA — studio notes"
      footer={
        <p>
          New here?{" "}
          <Link
            to={`/register${nextQuery}`}
            className="font-medium text-ink underline decoration-ink/30 underline-offset-4 hover:decoration-ink"
          >
            Create an account
          </Link>
        </p>
      }
    >
      <form onSubmit={submit} noValidate className="space-y-4">
        {login.isError ? (
          <div
            role="alert"
            className="rounded-sm border border-danger/25 bg-danger/[0.05] px-4 py-3 text-[12px] leading-relaxed text-danger"
          >
            {login.error.message}
          </div>
        ) : null}

        <Field label="Email" htmlFor="login-email">
          <div className="relative">
            <Mail className="pointer-events-none absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-muted" />
            <Input
              id="login-email"
              type="email"
              autoComplete="email"
              required
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              placeholder="you@example.com"
              className="pl-11"
            />
          </div>
        </Field>

        <Field label="Password" htmlFor="login-password">
          <div className="relative">
            <Lock className="pointer-events-none absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-muted" />
            <Input
              id="login-password"
              type={showPassword ? "text" : "password"}
              autoComplete="current-password"
              required
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              placeholder="Your password"
              className="pl-11 pr-12"
            />
            <button
              type="button"
              onClick={() => setShowPassword((value) => !value)}
              aria-label={showPassword ? "Hide password" : "Show password"}
              className="absolute right-2 top-1/2 flex h-9 w-9 -translate-y-1/2 items-center justify-center rounded-full text-muted transition-colors hover:bg-ink/[0.06] hover:text-ink"
            >
              {showPassword ? (
                <EyeOff className="h-4 w-4" />
              ) : (
                <Eye className="h-4 w-4" />
              )}
            </button>
          </div>
        </Field>

        <Button
          type="submit"
          size="lg"
          disabled={login.isPending || !email.trim() || !password}
          className="w-full justify-between"
        >
          <span>{login.isPending ? "Signing in…" : "Sign in"}</span>
          <ArrowRight className="h-4 w-4" />
        </Button>

        <p className="text-[11px] leading-relaxed text-muted">
          Accounts need a verified email before signing in. If you just
          registered in this demo, verify it with the helper on the next screen.
        </p>
      </form>

      <details className="mt-8 border-t border-line pt-5">
        <summary className="flex cursor-pointer list-none items-center justify-between text-[12px] text-muted [&::-webkit-details-marker]:hidden">
          Local demo accounts
          <span className="text-[10px] uppercase tracking-[0.12em]">
            click to fill
          </span>
        </summary>
        <div className="mt-3 grid gap-2 sm:grid-cols-2">
          <button
            type="button"
            onClick={() => fill("demo@forma.dev", "Demo1234!")}
            className={cn(
              "rounded-sm border border-line p-3 text-left transition-colors hover:border-ink/40",
              email === "demo@forma.dev" && "border-cobalt bg-cobalt/[0.04]",
            )}
          >
            <span className="block text-[12px] font-medium">Customer</span>
            <span className="mt-0.5 block text-[11px] text-muted">
              demo@forma.dev
            </span>
            <span className="block text-[11px] text-muted">Demo1234!</span>
          </button>
          <button
            type="button"
            onClick={() => fill("admin@forma.dev", "Admin123!")}
            className={cn(
              "rounded-sm border border-line p-3 text-left transition-colors hover:border-ink/40",
              email === "admin@forma.dev" && "border-cobalt bg-cobalt/[0.04]",
            )}
          >
            <span className="block text-[12px] font-medium">Admin</span>
            <span className="mt-0.5 block text-[11px] text-muted">
              admin@forma.dev
            </span>
            <span className="block text-[11px] text-muted">Admin123!</span>
          </button>
        </div>
      </details>
    </AuthShell>
  );
}
