import { create } from "zustand";

interface UiState {
  cartOpen: boolean;
  searchOpen: boolean;
  menuOpen: boolean;
  openCart: () => void;
  closeCart: () => void;
  toggleSearch: () => void;
  closeSearch: () => void;
  openMenu: () => void;
  closeMenu: () => void;
  closeAll: () => void;
}

export const useUi = create<UiState>((set) => ({
  cartOpen: false,
  searchOpen: false,
  menuOpen: false,
  openCart: () => set({ cartOpen: true, searchOpen: false, menuOpen: false }),
  closeCart: () => set({ cartOpen: false }),
  toggleSearch: () =>
    set((state) => ({
      searchOpen: !state.searchOpen,
      cartOpen: false,
      menuOpen: false,
    })),
  closeSearch: () => set({ searchOpen: false }),
  openMenu: () => set({ menuOpen: true, searchOpen: false, cartOpen: false }),
  closeMenu: () => set({ menuOpen: false }),
  closeAll: () => set({ cartOpen: false, searchOpen: false, menuOpen: false }),
}));
