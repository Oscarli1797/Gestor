"use client";

import { useState } from "react";
import Image from "next/image";
import Link from "next/link";
import { api } from "@/lib/api";

interface Developer {
  id: string;
  platform: string;
  username: string;
  displayName: string;
  avatarUrl: string;
  profileUrl: string;
  bio: string | null;
  location: string | null;
  company: string | null;
  followers: number | null;
  publicRepos: number | null;
  email: string | null;
  blog: string | null;
}

const PLATFORMS = [
  { value: 1, label: "GitHub" },
  { value: 2, label: "GitLab" },
  { value: 3, label: "StackOverflow" },
  { value: 4, label: "Bitbucket" },
];

const PLATFORM_COLORS: Record<string, string> = {
  github: "bg-gray-900 text-white",
  gitlab: "bg-orange-500 text-white",
  stackoverflow: "bg-orange-400 text-white",
  bitbucket: "bg-blue-600 text-white",
};

export default function SearchPage() {
  const [platform, setPlatform] = useState(1);
  const [query, setQuery] = useState("");
  const [location, setLocation] = useState("");
  const [results, setResults] = useState<Developer[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [searched, setSearched] = useState(false);

  async function handleSearch(e: React.FormEvent) {
    e.preventDefault();
    if (!query.trim() && !location.trim()) return;
    setLoading(true);
    setError("");
    setSearched(false);
    try {
      const { data } = await api.get("/api/search", {
        params: { platform, query, location },
      });
      setResults(data.data ?? []);
      setSearched(true);
    } catch {
      setError("Search failed. Please try again.");
    } finally {
      setLoading(false);
    }
  }

  async function handleExport() {
    const res = await api.get("/api/export", {
      params: { platform, query, location },
      responseType: "blob",
    });
    const url = URL.createObjectURL(res.data);
    const a = document.createElement("a");
    a.href = url;
    a.download = "developers.csv";
    a.click();
    URL.revokeObjectURL(url);
  }

  const locationSupported = platform === 1; // GitHub only

  return (
    <div className="max-w-4xl mx-auto px-4 py-10">
      <h1 className="text-2xl font-bold mb-2">Find Developers</h1>
      <p className="text-sm text-gray-500 mb-6">
        Search by skill, language, or keyword. Filter by location on GitHub.
      </p>

      <form onSubmit={handleSearch} className="space-y-2 mb-8">
        {/* Row 1: platform + keyword + search button */}
        <div className="flex gap-2">
          <select
            value={platform}
            onChange={(e) => setPlatform(Number(e.target.value))}
            className="border border-gray-300 rounded px-3 py-2 text-sm bg-white shrink-0"
          >
            {PLATFORMS.map((p) => (
              <option key={p.value} value={p.value}>
                {p.label}
              </option>
            ))}
          </select>

          <input
            type="text"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder='Skill or keyword — e.g. "React", "machine learning"'
            className="flex-1 border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          />

          <button
            type="submit"
            disabled={loading}
            className="bg-blue-600 text-white px-5 py-2 rounded text-sm font-medium hover:bg-blue-700 disabled:opacity-50 shrink-0"
          >
            {loading ? "Searching…" : "Search"}
          </button>
        </div>

        {/* Row 2: location filter */}
        <div className="flex items-center gap-2">
          <div className="relative flex-1 max-w-xs">
            <span className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 text-sm">📍</span>
            <input
              type="text"
              value={location}
              onChange={(e) => setLocation(e.target.value)}
              disabled={!locationSupported}
              placeholder={locationSupported ? 'City or country — e.g. "Madrid", "Spain"' : "Location filter: GitHub only"}
              className="w-full border border-gray-300 rounded pl-8 pr-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:bg-gray-50 disabled:text-gray-400 disabled:cursor-not-allowed"
            />
          </div>
          {locationSupported && location && (
            <button
              type="button"
              onClick={() => setLocation("")}
              className="text-xs text-gray-400 hover:text-gray-600"
            >
              Clear
            </button>
          )}
          {!locationSupported && (
            <span className="text-xs text-gray-400">
              Location search is only available on GitHub
            </span>
          )}
        </div>
      </form>

      {error && <p className="text-red-600 text-sm mb-4">{error}</p>}

      {searched && results.length === 0 && (
        <p className="text-gray-500 text-sm">No developers found for &quot;{query}&quot;.</p>
      )}

      {results.length > 0 && (
        <>
          <div className="flex items-center justify-between mb-4">
            <p className="text-sm text-gray-500">{results.length} developers found</p>
            <button
              onClick={handleExport}
              className="text-sm text-blue-600 hover:underline"
            >
              Export CSV
            </button>
          </div>

          <div className="flex flex-col gap-3">
            {results.map((dev) => (
              <DeveloperCard key={dev.id} dev={dev} />
            ))}
          </div>
        </>
      )}
    </div>
  );
}

function DeveloperCard({ dev }: { dev: Developer }) {
  const badgeClass = PLATFORM_COLORS[dev.platform] ?? "bg-gray-400 text-white";

  return (
    <div className="flex items-start gap-4 bg-white border border-gray-200 rounded-lg p-4 hover:shadow-sm transition-shadow">
      {dev.avatarUrl ? (
        <Image
          src={dev.avatarUrl}
          alt={dev.displayName}
          width={48}
          height={48}
          className="rounded-full shrink-0"
          unoptimized
        />
      ) : (
        <div className="w-12 h-12 rounded-full bg-gray-200 shrink-0 flex items-center justify-center text-gray-400 font-bold text-lg">
          {(dev.displayName ?? dev.username)[0]?.toUpperCase()}
        </div>
      )}

      <div className="flex-1 min-w-0">
        <div className="flex items-center gap-2 flex-wrap mb-1">
          <a
            href={dev.profileUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="font-semibold text-gray-900 hover:text-blue-600 truncate"
          >
            {dev.displayName ?? dev.username}
          </a>
          <span className="text-gray-400 text-sm">@{dev.username}</span>
          <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${badgeClass}`}>
            {dev.platform}
          </span>
        </div>

        {dev.bio && (
          <p className="text-sm text-gray-600 mb-1 line-clamp-2">{dev.bio}</p>
        )}

        <div className="flex items-center gap-4 text-xs text-gray-400 flex-wrap">
          {dev.location && <span>📍 {dev.location}</span>}
          {dev.company && <span>🏢 {dev.company}</span>}
          {dev.followers != null && (
            <span>
              {dev.platform === "stackoverflow" ? "⭐ Reputation" : "👥 Followers"}:{" "}
              <strong className="text-gray-600">{dev.followers.toLocaleString()}</strong>
            </span>
          )}
          {dev.publicRepos != null && dev.platform !== "stackoverflow" && (
            <span>
              📦 Repos: <strong className="text-gray-600">{dev.publicRepos}</strong>
            </span>
          )}
          {dev.publicRepos != null && dev.platform === "stackoverflow" && (
            <span>
              💬 Answers: <strong className="text-gray-600">{dev.publicRepos}</strong>
            </span>
          )}
          {dev.email && (
            <a href={`mailto:${dev.email}`} className="text-green-600 hover:underline">
              ✉ {dev.email}
            </a>
          )}
        </div>

        {dev.platform === "github" && (
          <div className="mt-2">
            <Link
              href={`/developer/github/${dev.username}`}
              className="text-xs text-blue-600 hover:underline"
            >
              View full profile →
            </Link>
          </div>
        )}
      </div>
    </div>
  );
}
