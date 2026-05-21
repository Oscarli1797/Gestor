"use client";

import { useEffect, useState } from "react";
import Image from "next/image";
import Link from "next/link";
import {
  listCandidates,
  removeCandidate,
  updateNotes,
  updateLinkedIn,
  updateStatus,
  SavedCandidate,
  CANDIDATE_STATUSES,
} from "@/lib/candidates";

const STATUS_LABELS: Record<string, string> = {
  saved:        "Saved",
  contacted:    "Contacted",
  replied:      "Replied",
  interviewing: "Interviewing",
  offered:      "Offered",
  rejected:     "Rejected",
};

const STATUS_COLORS: Record<string, string> = {
  saved:        "bg-gray-100 text-gray-600 border-gray-300",
  contacted:    "bg-blue-100 text-blue-700 border-blue-300",
  replied:      "bg-cyan-100 text-cyan-700 border-cyan-300",
  interviewing: "bg-amber-100 text-amber-700 border-amber-300",
  offered:      "bg-green-100 text-green-700 border-green-300",
  rejected:     "bg-red-100 text-red-600 border-red-300",
};

const TIER_COLORS: Record<string, string> = {
  Expert:      "bg-purple-100 text-purple-700",
  Senior:      "bg-amber-100  text-amber-700",
  "Mid-level": "bg-green-100  text-green-700",
  Junior:      "bg-blue-100   text-blue-700",
  Beginner:    "bg-gray-100   text-gray-600",
};

export default function CandidatesPage() {
  const [candidates, setCandidates] = useState<SavedCandidate[]>([]);
  const [loading, setLoading] = useState(true);
  const [filterStatus, setFilterStatus] = useState<string>("all");

  useEffect(() => {
    listCandidates()
      .then(setCandidates)
      .finally(() => setLoading(false));
  }, []);

  function handleRemove(developerId: string) {
    removeCandidate(developerId).then(() =>
      setCandidates((prev) => prev.filter((c) => c.developerId !== developerId))
    );
  }

  function handleNotesChange(developerId: string, notes: string) {
    setCandidates((prev) =>
      prev.map((c) => (c.developerId === developerId ? { ...c, notes } : c))
    );
  }

  function handleNotesSave(developerId: string, notes: string) {
    updateNotes(developerId, notes);
  }

  function handleLinkedInChange(developerId: string, linkedinUrl: string) {
    setCandidates((prev) =>
      prev.map((c) => (c.developerId === developerId ? { ...c, linkedinUrl } : c))
    );
  }

  function handleLinkedInSave(developerId: string, linkedinUrl: string) {
    updateLinkedIn(developerId, linkedinUrl);
  }

  function handleStatusChange(developerId: string, status: string) {
    setCandidates((prev) =>
      prev.map((c) => (c.developerId === developerId ? { ...c, status } : c))
    );
    updateStatus(developerId, status);
  }

  const statusCounts = CANDIDATE_STATUSES.reduce<Record<string, number>>(
    (acc, s) => {
      acc[s] = candidates.filter((c) => c.status === s).length;
      return acc;
    },
    {}
  );

  const visible =
    filterStatus === "all"
      ? candidates
      : candidates.filter((c) => c.status === filterStatus);

  if (loading)
    return <p className="p-10 text-sm text-gray-400">Loading candidates…</p>;

  return (
    <div className="max-w-4xl mx-auto px-4 py-10">
      <div className="flex items-center justify-between mb-4">
        <h1 className="text-2xl font-bold">Saved Candidates</h1>
        <span className="text-sm text-gray-400">{candidates.length} total</span>
      </div>

      {/* Status filter bar */}
      <div className="flex gap-2 flex-wrap mb-6">
        <button
          onClick={() => setFilterStatus("all")}
          className={`px-3 py-1 text-sm rounded-full border font-medium transition-colors ${
            filterStatus === "all"
              ? "bg-gray-800 text-white border-gray-800"
              : "bg-white text-gray-600 border-gray-300 hover:border-gray-500"
          }`}
        >
          All ({candidates.length})
        </button>
        {CANDIDATE_STATUSES.map((s) => (
          <button
            key={s}
            onClick={() => setFilterStatus(s)}
            className={`px-3 py-1 text-sm rounded-full border font-medium transition-colors ${
              filterStatus === s
                ? STATUS_COLORS[s] + " border-2"
                : "bg-white text-gray-500 border-gray-200 hover:border-gray-400"
            }`}
          >
            {STATUS_LABELS[s]} ({statusCounts[s] ?? 0})
          </button>
        ))}
      </div>

      {candidates.length === 0 && (
        <div className="text-center py-16 text-gray-400">
          <p className="text-lg mb-2">No candidates saved yet.</p>
          <Link href="/search" className="text-blue-600 hover:underline text-sm">
            Search for developers →
          </Link>
        </div>
      )}

      {candidates.length > 0 && visible.length === 0 && (
        <div className="text-center py-10 text-gray-400 text-sm">
          No candidates with status &quot;{STATUS_LABELS[filterStatus]}&quot;.
        </div>
      )}

      <div className="flex flex-col gap-4">
        {visible.map((c) => (
          <CandidateCard
            key={c.id}
            candidate={c}
            onRemove={handleRemove}
            onNotesChange={handleNotesChange}
            onNotesSave={handleNotesSave}
            onLinkedInChange={handleLinkedInChange}
            onLinkedInSave={handleLinkedInSave}
            onStatusChange={handleStatusChange}
          />
        ))}
      </div>
    </div>
  );
}

// ─── Card ─────────────────────────────────────────────────────────────────────

function CandidateCard({
  candidate: c,
  onRemove,
  onNotesChange,
  onNotesSave,
  onLinkedInChange,
  onLinkedInSave,
  onStatusChange,
}: {
  candidate: SavedCandidate;
  onRemove: (id: string) => void;
  onNotesChange: (id: string, notes: string) => void;
  onNotesSave: (id: string, notes: string) => void;
  onLinkedInChange: (id: string, url: string) => void;
  onLinkedInSave: (id: string, url: string) => void;
  onStatusChange: (id: string, status: string) => void;
}) {
  const tierClass = c.scoreTier
    ? (TIER_COLORS[c.scoreTier] ?? "bg-gray-100 text-gray-600")
    : "";

  return (
    <div className="bg-white border border-gray-200 rounded-lg p-4">
      <div className="flex items-start gap-4">
        {/* Avatar */}
        {c.avatarUrl ? (
          <Image
            src={c.avatarUrl}
            alt={c.displayName}
            width={48}
            height={48}
            className="rounded-full shrink-0"
            unoptimized
          />
        ) : (
          <div className="w-12 h-12 rounded-full bg-gray-200 shrink-0 flex items-center justify-center text-gray-500 font-bold">
            {(c.displayName ?? c.username)[0]?.toUpperCase()}
          </div>
        )}

        {/* Info */}
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 flex-wrap mb-0.5">
            <span className="font-semibold text-gray-900">
              {c.displayName ?? c.username}
            </span>
            <span className="text-gray-400 text-sm">@{c.username}</span>
            {c.scoreTier && (
              <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${tierClass}`}>
                {c.scoreTier}
              </span>
            )}
            {c.score != null && (
              <span className="text-xs text-gray-400">Score: {c.score}</span>
            )}
          </div>

          <div className="flex gap-3 text-xs text-gray-400 mb-2 flex-wrap">
            {c.location && <span>📍 {c.location}</span>}
            <span className="capitalize">{c.platform}</span>
            <span>Saved {new Date(c.savedAt).toLocaleDateString()}</span>
          </div>

          <div className="flex gap-3 text-xs flex-wrap">
            {c.platform === "github" && (
              <Link
                href={`/developer/${c.platform}/${c.username}`}
                className="text-blue-600 hover:underline"
              >
                View profile →
              </Link>
            )}
            <a
              href={c.profileUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="text-blue-600 hover:underline"
            >
              {c.platform === "github" ? "GitHub" : c.platform} ↗
            </a>
            {c.email && (
              <a
                href={`mailto:${c.email}`}
                className="text-green-600 hover:underline"
              >
                ✉ {c.email}
              </a>
            )}
            {c.blog && (
              <a
                href={c.blog.startsWith("http") ? c.blog : `https://${c.blog}`}
                target="_blank"
                rel="noopener noreferrer"
                className="text-blue-600 hover:underline"
              >
                🔗 Website ↗
              </a>
            )}
            {c.linkedinUrl && (
              <a
                href={c.linkedinUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="text-blue-700 hover:underline font-medium"
              >
                in LinkedIn ↗
              </a>
            )}
          </div>
        </div>

        {/* Status + Remove */}
        <div className="flex items-center gap-2 shrink-0">
          <select
            value={c.status ?? "saved"}
            onChange={(e) => onStatusChange(c.developerId, e.target.value)}
            className={`text-xs border rounded px-2 py-1 font-medium focus:outline-none cursor-pointer ${
              STATUS_COLORS[c.status ?? "saved"] ?? "bg-gray-100 text-gray-600 border-gray-300"
            }`}
          >
            {CANDIDATE_STATUSES.map((s) => (
              <option key={s} value={s}>
                {STATUS_LABELS[s]}
              </option>
            ))}
          </select>
          <button
            onClick={() => onRemove(c.developerId)}
            className="text-gray-300 hover:text-red-500 text-xl leading-none"
            title="Remove candidate"
          >
            ×
          </button>
        </div>
      </div>

      {/* LinkedIn URL */}
      <div className="mt-3 pt-3 border-t border-gray-100">
        <label className="text-xs text-gray-400 block mb-1">LinkedIn URL</label>
        <input
          type="url"
          className="w-full text-sm border border-gray-200 rounded px-2 py-1.5 focus:outline-none focus:ring-1 focus:ring-blue-400"
          placeholder="https://linkedin.com/in/username"
          value={c.linkedinUrl ?? ""}
          onChange={(e) => onLinkedInChange(c.developerId, e.target.value)}
          onBlur={(e) => onLinkedInSave(c.developerId, e.target.value)}
        />
      </div>

      {/* Notes */}
      <div className="mt-2">
        <label className="text-xs text-gray-400 block mb-1">Notes</label>
        <textarea
          className="w-full text-sm border border-gray-200 rounded px-2 py-1.5 resize-none focus:outline-none focus:ring-1 focus:ring-blue-400"
          rows={2}
          placeholder="Add private notes about this candidate…"
          value={c.notes ?? ""}
          onChange={(e) => onNotesChange(c.developerId, e.target.value)}
          onBlur={(e) => onNotesSave(c.developerId, e.target.value)}
        />
      </div>
    </div>
  );
}
