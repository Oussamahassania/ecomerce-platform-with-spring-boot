import { useMemo, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import {
  ArrowRight,
  Check,
  Eye,
  EyeOff,
  Lock,
  Mail,
  ShieldCheck,
  User,
} from "lucide-react";
import { toast } from "sonner";
import { useDevVerify, useRegister } from "@/hooks/queries";
import { AuthShell } from "@/components/auth/AuthShell";
import { Button, Field, Input, cn } from "@/components/ui";

const PASSWORD_RULES = [
  { label: "At least 8 characters", test: (value: string) => value.length >= 8 },
  { label: "One uppercase letter", test: (value: string) => /[A-Z]/.test(value) },
  { label: "One lowercase letter", test: (value: string) => /[a-z]/.test(value) },
  { label: "One number", test: (value: string) => /\d/.test(value) },
  {
    label: "One special character (@$!%*?&)",
    test: (value: string) => /[@$!%*?&]/.test(value),
  },
];

function safeNext(raw: string | null) {
  if (raw && raw.startsWith("/") && !raw.startsWith("//")) return raw;
  return "/account";
}

export function RegisterPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const next = safeNext(searchParams.get("next"));
  const register = useRegister();
  const devVerify = useDevVerify();

  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [dateOfBirth, setDateOfBirth] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [registeredEmail, setRegisteredEmail] = useState<string | null>(null);
  const [formError, setFormError] = useState<string | null>(null);

  const checks = useMemo(
    () => PASSWORD_RULES.map((rule) => ({ ...rule, met: rule.test(password) })),
    [password],
  );
  const strongEnough = checks.every((check) => check.met);

  const nextQuery = next !== "/account" ? `?next=${encodeURIComponent(next)}` : "";

  function submit(event: React.FormEvent) {
    event.preventDefault();
    setFormError(null);

    if (!fullName.trim()) {
      setFormError("Tell us what to call you.");
      return;
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())) {
      setFormError("That email does not look right.");
      return;
    }
    if (!strongEnough) {
      setFormError("The password does not meet every rule yet.");
      return;
    }
    if (password !== confirm) {
      setFormError("The two passwords do not match.");
      return;
    }

    register.mutate(
      {
        fullName: fullName.trim(),
        email: email.trim(),
        password,
        dateOfBirth: dateOfBirth || null,
      },
      {
        onSuccess: () => {
          setRegisteredEmail(email.trim());
          toast.success("Account created");
        },
      },
    );
  }

  function verifyNow() {
    if (!registeredEmail) return;
    devVerify.mutate(registeredEmail, {
      onSuccess: () => {
        toast.success("Email verified — sign in to continue");
        navigate(`/login${nextQuery}`, {
          replace: true,
          state: { email: registeredEmail },
        });
      },
      onError: (error) => toast.error(error.message),
    });
  }

  if (registeredEmail) {
    return (
      <AuthShell
        eyebrow="One more step"
        title={
          <>
            Check your
            <br />
            inbox<span className="text-cobalt">.</span>
          </>
        }
        description="Your account is created. In a live shop, a verification link would arrive by email. This local demo has no mail server, so verify it directly below."
        image="/images/products/halo-task-light.jpg"
        quote="A good account should feel like a quiet shelf, not a form to fight."
        attribution="FORMA — studio notes"
        footer={
          <p>
            Wrong address?{" "}
            <button
              type="button"
              onClick={() => {
                setRegisteredEmail(null);
                setPassword("");
                setConfirm("");
              }}
              className="font-medium text-ink underline decoration-ink/30 underline-offset-4 hover:decoration-ink"
            >
              Use another email
            </button>
          </p>
        }
      >
        <div className="rounded-sm border border-line bg-paper-3/60 p-5">
          <div className="flex items-center gap-3">
            <span className="flex h-10 w-10 items-center justify-center rounded-full bg-ink text-paper">
              <Mail className="h-4 w-4" />
            </span>
            <div className="min-w-0">
              <p className="text-[12px] text-muted">Verification for</p>
              <p className="truncate text-sm font-medium">{registeredEmail}</p>
            </div>
          </div>

          <div className="mt-5 space-y-3">
            <Button
              onClick={verifyNow}
              disabled={devVerify.isPending}
              className="w-full justify-between"
            >
              <span>
                {devVerify.isPending
                  ? "Verifying…"
                  : "Verify this account now"}
              </span>
              <ShieldCheck className="h-4 w-4" />
            </Button>
            <p className="text-[11px] leading-relaxed text-muted">
              This button calls the development-only{" "}
              <code className="rounded bg-ink/[0.06] px-1 py-0.5 text-[10px]">
                /api/dev/verify
              </code>{" "}
              endpoint. It exists because there is no SMTP server in local
              development.
            </p>
          </div>
        </div>

        <Link
          to={`/login${nextQuery}`}
          className="mt-6 inline-flex items-center gap-1.5 text-[13px] font-medium text-ink underline decoration-ink/30 underline-offset-4 hover:decoration-ink"
        >
          Go to sign in
          <ArrowRight className="h-3.5 w-3.5" />
        </Link>
      </AuthShell>
    );
  }

  return (
    <AuthShell
      eyebrow="Create an account"
      title={
        <>
          Make a place
          <br />
          for good things<span className="text-cobalt">.</span>
        </>
      }
      description="Save objects you are considering, keep your orders together and leave notes for other people."
      image="/images/products/loop-vase.jpg"
      quote="Buy less, choose well, make it last."
      attribution="Vivienne Westwood — a shop rule"
      footer={
        <p>
          Already have an account?{" "}
          <Link
            to={`/login${nextQuery}`}
            className="font-medium text-ink underline decoration-ink/30 underline-offset-4 hover:decoration-ink"
          >
            Sign in
          </Link>
        </p>
      }
    >
      <form onSubmit={submit} noValidate className="space-y-4">
        {formError || register.isError ? (
          <div
            role="alert"
            className="rounded-sm border border-danger/25 bg-danger/[0.05] px-4 py-3 text-[12px] leading-relaxed text-danger"
          >
            {formError ?? register.error?.message}
          </div>
        ) : null}

        <Field label="Full name" htmlFor="register-name">
          <div className="relative">
            <User className="pointer-events-none absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-muted" />
            <Input
              id="register-name"
              autoComplete="name"
              required
              value={fullName}
              onChange={(event) => setFullName(event.target.value)}
              placeholder="Nora Bennett"
              className="pl-11"
            />
          </div>
        </Field>

        <Field label="Email" htmlFor="register-email">
          <div className="relative">
            <Mail className="pointer-events-none absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-muted" />
            <Input
              id="register-email"
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

        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Password" htmlFor="register-password">
            <div className="relative">
              <Lock className="pointer-events-none absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-muted" />
              <Input
                id="register-password"
                type={showPassword ? "text" : "password"}
                autoComplete="new-password"
                required
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                placeholder="At least 8 characters"
                className="pl-11 pr-11"
              />
              <button
                type="button"
                onClick={() => setShowPassword((value) => !value)}
                aria-label={showPassword ? "Hide password" : "Show password"}
                className="absolute right-1.5 top-1/2 flex h-9 w-9 -translate-y-1/2 items-center justify-center rounded-full text-muted transition-colors hover:bg-ink/[0.06] hover:text-ink"
              >
                {showPassword ? (
                  <EyeOff className="h-4 w-4" />
                ) : (
                  <Eye className="h-4 w-4" />
                )}
              </button>
            </div>
          </Field>

          <Field label="Confirm password" htmlFor="register-confirm">
            <Input
              id="register-confirm"
              type={showPassword ? "text" : "password"}
              autoComplete="new-password"
              required
              value={confirm}
              onChange={(event) => setConfirm(event.target.value)}
              placeholder="Repeat it"
              aria-invalid={Boolean(confirm) && confirm !== password}
            />
          </Field>
        </div>

        <div className="rounded-sm border border-line bg-paper-3/50 p-4">
          <p className="text-[11px] font-medium text-ink-2">
            Password requirements
          </p>
          <ul className="mt-2 grid gap-1.5 sm:grid-cols-2">
            {checks.map((check) => (
              <li
                key={check.label}
                className={cn(
                  "flex items-center gap-1.5 text-[11px]",
                  check.met ? "text-success" : "text-muted",
                )}
              >
                <span
                  className={cn(
                    "flex h-4 w-4 items-center justify-center rounded-full border",
                    check.met
                      ? "border-success bg-success text-white"
                      : "border-line",
                  )}
                >
                  {check.met ? <Check className="h-2.5 w-2.5" /> : null}
                </span>
                {check.label}
              </li>
            ))}
          </ul>
        </div>

        <Field
          label="Date of birth (optional)"
          htmlFor="register-dob"
          hint="Only used to make sure the shop is appropriate for you."
        >
          <Input
            id="register-dob"
            type="date"
            autoComplete="bday"
            max={new Date().toISOString().slice(0, 10)}
            value={dateOfBirth}
            onChange={(event) => setDateOfBirth(event.target.value)}
            className="max-w-48"
          />
        </Field>

        <Button
          type="submit"
          size="lg"
          disabled={register.isPending}
          className="w-full justify-between"
        >
          <span>
            {register.isPending ? "Creating account…" : "Create account"}
          </span>
          <ArrowRight className="h-4 w-4" />
        </Button>

        <p className="text-[11px] leading-relaxed text-muted">
          By creating an account you agree to the demo’s terms: no real
          payments, no real deliveries, and no data leaving your machine.
        </p>
      </form>
    </AuthShell>
  );
}
