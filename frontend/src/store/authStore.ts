import { create } from 'zustand';
import axios from 'axios';

interface AuthState {
  accessToken: string | null;
  isAuthenticated: boolean;
  isInitializing: boolean;
  setAccessToken: (token: string) => void;
  logout: () => void;
  initSession: () => Promise<void>;
}

export const useAuthStore = create<AuthState>((set, get) => ({
  accessToken: null,
  isAuthenticated: false,
  isInitializing: true,

  setAccessToken: (token) => set({ accessToken: token, isAuthenticated: true }),

  logout: () => set({ accessToken: null, isAuthenticated: false }),

  initSession: async () => {
    if (get().isAuthenticated) {
      set({ isInitializing: false });
      return;
    }
    try {
      const { data } = await axios.post<{ accessToken: string }>(
        '/api/v1/auth/refresh',
        null,
        { withCredentials: true },
      );
      set({ accessToken: data.accessToken, isAuthenticated: true });
    } catch {
      set({ accessToken: null, isAuthenticated: false });
    } finally {
      set({ isInitializing: false });
    }
  },
}));
