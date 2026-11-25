const TOKEN_KEY = "forma.token";

export interface SessionInfo {
  email: string;
  roles: string[];
  role: "USER" | "ADMIN" | null;
  exp: number | null;
}

export function getToken(): string | null {
  try {
    return localStorage.getItem(TOKEN_KEY);
  } catch {
    return null;
  }
}

export function setToken(token: string | null): void {
  try {
    if (token) localStorage.setItem(TOKEN_KEY, token);
    else localStorage.removeItem(TOKEN_KEY);
  } catch {
    /* storage can be unavailable in private mode */
  }
}

function base64UrlDecode(input: string): string {
  const normalized = input.replace(/-/g, "+").replace(/_/g, "/");
  const padded = normalized.padEnd(
    normalized.length + ((4 - (normalized.length % 4)) % 4),
    "=",
  );
  return decodeURIComponent(
    Array.from(atob(padded))
      .map((char) => `%${char.charCodeAt(0).toString(16).padStart(2, "0")}`)
      .join(""),
  );
}

export function decodeToken(token: string): SessionInfo | null {
  try {
    const payload = token.split(".")[1];
    if (!payload) return null;
    const claims = JSON.parse(base64UrlDecode(payload)) as {
      sub?: string;
      roles?: string;
      exp?: number;
    };
    const roles = (claims.roles ?? "")
      .split(",")
      .map((role) => role.trim())
      .filter(Boolean);
    const role = roles.includes("ROLE_ADMIN")
      ? "ADMIN"
      : roles.includes("ROLE_USER")
        ? "USER"
        : null;
    return {
      email: claims.sub ?? "",
      roles,
      role,
      exp: claims.exp ? claims.exp * 1000 : null,
    };
  } catch {
    return null;
  }
}

export function isExpired(info: SessionInfo | null): boolean {
  return !info?.exp || info.exp <= Date.now();
}
