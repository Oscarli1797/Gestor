import { api, saveToken, clearToken } from "./api";

export interface UserInfo {
  name: string;
  email: string;
  roles: string[];
}

export async function login(username: string, password: string): Promise<string> {
  const form = new URLSearchParams();
  form.append("username", username);
  form.append("password", password);

  const { data } = await api.post("/api/auth/login", form, {
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
  });
  const token: string = data.data;
  saveToken(token);
  return token;
}

export async function logout(): Promise<void> {
  await api.post("/api/auth/logout").catch(() => {});
  clearToken();
}

export async function register(name: string, email: string, password: string) {
  const { data } = await api.post("/api/auth/register", { name, email, password });
  return data;
}

export async function verify(code: string) {
  const { data } = await api.post("/api/auth/verify", { code });
  return data;
}

export async function getMe(): Promise<UserInfo> {
  const { data } = await api.get("/api/auth/me");
  return data.data as UserInfo;
}

export async function forgotPassword(email: string) {
  const { data } = await api.post("/api/auth/forgot-password", { email });
  return data;
}

export async function resetPassword(code: string, newPassword: string) {
  const { data } = await api.post("/api/auth/reset-password", { code, newPassword });
  return data;
}
