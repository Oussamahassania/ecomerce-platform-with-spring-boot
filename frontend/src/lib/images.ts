import { API_BASE } from "./api";

export function imageUrl(source: string | null | undefined): string | null {
  if (!source) return null;
  if (/^https?:\/\//i.test(source)) return source;
  if (source.startsWith("/api/")) return `${API_BASE}${source}`;
  return source;
}

export function productImage(source: string | null | undefined): string {
  return imageUrl(source) ?? "/images/products/placeholder.svg";
}
