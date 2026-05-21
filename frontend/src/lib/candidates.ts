import { api } from "./api";

export interface SavedCandidate {
  id: number;
  developerId: string;
  platform: string;
  username: string;
  displayName: string;
  avatarUrl: string | null;
  profileUrl: string;
  location: string | null;
  score: number | null;
  scoreTier: string | null;
  status: string;
  email: string | null;
  blog: string | null;
  linkedinUrl: string | null;
  notes: string | null;
  savedAt: string;
}

export const CANDIDATE_STATUSES = [
  "saved",
  "contacted",
  "replied",
  "interviewing",
  "offered",
  "rejected",
] as const;

export type CandidateStatus = (typeof CANDIDATE_STATUSES)[number];

export async function listCandidates(): Promise<SavedCandidate[]> {
  const { data } = await api.get("/api/candidates");
  return data.data as SavedCandidate[];
}

export async function isCandidateSaved(developerId: string): Promise<boolean> {
  const { data } = await api.get("/api/candidates/saved", {
    params: { developerId },
  });
  return data.data as boolean;
}

export async function saveCandidate(payload: {
  developerId: string;
  platform: string;
  username: string;
  displayName?: string;
  avatarUrl?: string;
  profileUrl?: string;
  location?: string;
  score?: number;
  scoreTier?: string;
  email?: string;
  blog?: string;
  notes?: string;
}): Promise<SavedCandidate> {
  const { data } = await api.post("/api/candidates", payload);
  return data.data as SavedCandidate;
}

export async function removeCandidate(developerId: string): Promise<void> {
  await api.delete("/api/candidates", { params: { developerId } });
}

export async function updateNotes(
  developerId: string,
  notes: string
): Promise<SavedCandidate> {
  const { data } = await api.patch("/api/candidates/notes", { notes }, {
    params: { developerId },
  });
  return data.data as SavedCandidate;
}

export async function updateStatus(
  developerId: string,
  status: string
): Promise<SavedCandidate> {
  const { data } = await api.patch("/api/candidates/status", { status }, {
    params: { developerId },
  });
  return data.data as SavedCandidate;
}

export async function updateLinkedIn(
  developerId: string,
  linkedinUrl: string
): Promise<SavedCandidate> {
  const { data } = await api.patch("/api/candidates/linkedin", { linkedinUrl }, {
    params: { developerId },
  });
  return data.data as SavedCandidate;
}
