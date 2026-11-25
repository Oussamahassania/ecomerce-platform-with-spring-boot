import { create } from "zustand";
import { decodeToken, getToken, isExpired, setToken } from "@/lib/session";
import { queryClient } from "@/lib/queryClient";

interface AuthState {
  token: string | null;
  email: string | null;
  role: "USER" | "ADMIN" | null;
  setSession: (token: string) => void;
  logout: () => void;
}

const storedToken = getToken();
const storedInfo = storedToken ? decodeToken(storedToken) : null;
const validSession = storedInfo && !isExpired(storedInfo) ? storedInfo : null;

if (storedToken && !validSession) setToken(null);

export const useAuth = create<AuthState>((set) => ({
  token: validSession ? storedToken : null,
  email: validSession?.email ?? null,
  role: validSession?.role ?? null,
  setSession: (token) => {
    const info = decodeToken(token);
    if (!info || isExpired(info)) return;
    setToken(token);
    set({ token, email: info.email, role: info.role });
  },
  logout: () => {
    setToken(null);
    set({ token: null, email: null, role: null });
    queryClient.clear();
  },
}));

export const selectIsAdmin = (state: AuthState) => state.role === "ADMIN";
