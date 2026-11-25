import { create } from "zustand";
import { persist } from "zustand/middleware";
import type { Product } from "@/lib/types";

export interface WishlistItem {
  id: number;
  name: string;
  price: number;
  image: string | null;
  categoryName: string | null;
}

interface WishlistState {
  items: WishlistItem[];
  toggle: (product: Product) => void;
  remove: (id: number) => void;
  has: (id: number) => boolean;
}

export const useWishlist = create<WishlistState>()(
  persist(
    (set, get) => ({
      items: [],
      toggle: (product) =>
        set((state) => {
          const exists = state.items.some((item) => item.id === product.id);
          return {
            items: exists
              ? state.items.filter((item) => item.id !== product.id)
              : [
                  {
                    id: product.id,
                    name: product.name,
                    price: product.price,
                    image: product.img_url,
                    categoryName: product.categoryName,
                  },
                  ...state.items,
                ],
          };
        }),
      remove: (id) =>
        set((state) => ({
          items: state.items.filter((item) => item.id !== id),
        })),
      has: (id) => get().items.some((item) => item.id === id),
    }),
    { name: "forma.wishlist" },
  ),
);
