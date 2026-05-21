import axios from "axios";
import Cookies from "js-cookie";

const TOKEN_KEY = "gp_token";

// Use relative URLs — Next.js rewrites /api/* → Spring Boot backend
export const api = axios.create({
  baseURL: "",
  withCredentials: false,
});

// Attach JWT on every request
api.interceptors.request.use((config) => {
  const token = Cookies.get(TOKEN_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// On 401 → clear token and redirect to login
api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401 && typeof window !== "undefined") {
      Cookies.remove(TOKEN_KEY);
      window.location.href = "/login";
    }
    return Promise.reject(err);
  }
);

export const saveToken = (token: string) =>
  Cookies.set(TOKEN_KEY, token, { expires: 1, sameSite: "Lax" });

export const clearToken = () => Cookies.remove(TOKEN_KEY);

export const getToken = () => Cookies.get(TOKEN_KEY) ?? null;
