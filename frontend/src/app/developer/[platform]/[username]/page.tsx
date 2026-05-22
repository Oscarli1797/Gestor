"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import Image from "next/image";
import Link from "next/link";
import {
  getDeveloperProfile,
  DeveloperProfile,
  LanguageStat,
  ScoreBreakdown,
} from "@/lib/developer";
import {
  isCandidateSaved,
  saveCandidate,
  removeCandidate,
} from "@/lib/candidates";

// ─── Tier config ─────────────────────────────────────────────────────────────

const TIER_STYLES: Record<string, { bg: string; text: string; ring: string }> = {
  Expert:    { bg: "bg-purple-100", text: "text-purple-700", ring: "ring-purple-400" },
  Senior:    { bg: "bg-amber-100",  text: "text-amber-700",  ring: "ring-amber-400"  },
  "Mid-level": { bg: "bg-green-100",  text: "text-green-700",  ring: "ring-green-400"  },
  Junior:    { bg: "bg-blue-100",   text: "text-blue-700",   ring: "ring-blue-400"   },
  Beginner:  { bg: "bg-gray-100",   text: "text-gray-600",   ring: "ring-gray-300"   },
};

const SCORE_DIMENSIONS = [
  { key: "recentActivity" as const, label: "Recent Activity", weight: "25%" },
  { key: "projectImpact"  as const, label: "Project Impact",  weight: "25%" },
  { key: "contributions"  as const, label: "Contributions",   weight: "20%" },
  { key: "collaboration"  as const, label: "Collaboration",   weight: "15%" },
  { key: "techStack"      as const, label: "Tech Stack",      weight: "15%" },
];

// ─── Page ─────────────────────────────────────────────────────────────────────

export default function DeveloperProfilePage() {
  const { platform, username } = useParams<{
    platform: string;
    username: string;
  }>();

  const [profile, setProfile] = useState<DeveloperProfile | null>(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [saved, setSaved] = useState(false);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (platform !== "github") {
      setError("Deep profiles are currently only available for GitHub developers.");
      setLoading(false);
      return;
    }
    getDeveloperProfile(platform, username)
      .then((p) => {
        setProfile(p);
        return isCandidateSaved(p.id);
      })
      .then(setSaved)
      .catch(() =>
        setError("Profile not found or GitHub token is not configured.")
      )
      .finally(() => setLoading(false));
  }, [platform, username]);

  async function handleToggleSave() {
    if (!profile) return;
    setSaving(true);
    try {
      if (saved) {
        await removeCandidate(profile.id);
        setSaved(false);
      } else {
        await saveCandidate({
          developerId: profile.id,
          platform: profile.platform,
          username: profile.username,
          displayName: profile.displayName,
          avatarUrl: profile.avatarUrl,
          profileUrl: profile.profileUrl,
          location: profile.location ?? undefined,
          score: profile.scoreBreakdown?.total,
          scoreTier: profile.scoreBreakdown?.tier,
          email: profile.email ?? undefined,
          blog: profile.blog ?? undefined,
        });
        setSaved(true);
      }
    } finally {
      setSaving(false);
    }
  }

  if (loading)
    return <p className="p-10 text-gray-400 text-sm">Loading profile…</p>;

  if (error)
    return (
      <div className="max-w-2xl mx-auto px-4 py-10">
        <BackLink />
        <p className="text-red-600 text-sm">{error}</p>
      </div>
    );

  if (!profile) return null;

  const { contributionStats: cs, scoreBreakdown: sb } = profile;
  const tierStyle = sb ? (TIER_STYLES[sb.tier] ?? TIER_STYLES.Beginner) : null;

  return (
    <div className="max-w-3xl mx-auto px-4 py-10">
      <BackLink />

      {/* ── Header ── */}
      <div className="flex items-start gap-5 mb-8">
        {profile.avatarUrl && (
          <Image
            src={profile.avatarUrl}
            alt={profile.displayName}
            width={80}
            height={80}
            className="rounded-full shrink-0"
            unoptimized
          />
        )}

        <div className="flex-1">
          <div className="flex items-center gap-3 flex-wrap">
            <h1 className="text-2xl font-bold">{profile.displayName}</h1>
            {sb && tierStyle && (
              <span
                className={`text-xs font-semibold px-2 py-0.5 rounded-full ${tierStyle.bg} ${tierStyle.text}`}
              >
                {sb.tier}
              </span>
            )}
          </div>
          <a
            href={profile.profileUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="text-gray-500 text-sm hover:text-blue-600"
          >
            @{profile.username}
          </a>
          {profile.bio && (
            <p className="text-gray-600 text-sm mt-2 max-w-lg">{profile.bio}</p>
          )}
          <div className="flex gap-4 mt-2 text-xs text-gray-400 flex-wrap">
            {profile.location && <span>📍 {profile.location}</span>}
            {profile.company && <span>🏢 {profile.company}</span>}
            {profile.joinedAt && (
              <span>📅 Joined {profile.joinedAt.slice(0, 7)}</span>
            )}
            {profile.email && (
              <a
                href={`mailto:${profile.email}`}
                className="text-blue-500 hover:underline"
              >
                ✉ {profile.email}
              </a>
            )}
            {profile.blog && (
              <a
                href={profile.blog.startsWith("http") ? profile.blog : `https://${profile.blog}`}
                target="_blank"
                rel="noopener noreferrer"
                className="text-blue-500 hover:underline"
              >
                🔗 Website
              </a>
            )}
            {profile.linkedinUrl && (
              <a
                href={profile.linkedinUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="text-blue-700 hover:underline font-medium"
              >
                in LinkedIn
              </a>
            )}
          </div>
        </div>

        {/* Save button */}
        <button
          onClick={handleToggleSave}
          disabled={saving}
          className={`shrink-0 px-4 py-2 rounded text-sm font-medium transition-colors disabled:opacity-50 ${
            saved
              ? "bg-green-100 text-green-700 hover:bg-red-100 hover:text-red-700"
              : "bg-blue-600 text-white hover:bg-blue-700"
          }`}
        >
          {saving ? "…" : saved ? "✓ Saved" : "Save"}
        </button>

        {/* Score circle */}
        {sb && tierStyle && (
          <div
            className={`shrink-0 w-20 h-20 rounded-full ring-4 flex flex-col items-center justify-center ${tierStyle.ring} ${tierStyle.bg}`}
          >
            <span className={`text-2xl font-bold ${tierStyle.text}`}>
              {sb.total}
            </span>
            <span className={`text-xs ${tierStyle.text}`}>/ 100</span>
          </div>
        )}
      </div>

      {/* ── AI Recruiter Insights ── */}
      {sb?.aiSummary && (
        <Section title="AI Recruiter Insights">
          <div className="bg-gradient-to-br from-purple-50 to-blue-50 border border-purple-200 rounded-lg p-4">
            <p className="text-sm text-gray-700 mb-3 leading-relaxed">{sb.aiSummary}</p>
            {sb.aiInsights && sb.aiInsights.length > 0 && (
              <ul className="space-y-1.5">
                {sb.aiInsights.map((insight, i) => (
                  <li key={i} className="flex items-start gap-2 text-sm text-gray-600">
                    <span className="text-purple-500 mt-0.5 shrink-0">▸</span>
                    {insight}
                  </li>
                ))}
              </ul>
            )}
            <p className="text-xs text-gray-400 mt-3">Generated by Claude AI</p>
          </div>
        </Section>
      )}

      {/* ── Score breakdown ── */}
      {sb && (
        <Section title="Score Breakdown">
          <div className="flex flex-col gap-2">
            {SCORE_DIMENSIONS.map((d) => (
              <ScoreDimRow
                key={d.key}
                label={d.label}
                weight={d.weight}
                value={sb[d.key]}
              />
            ))}
          </div>
        </Section>
      )}

      {/* ── Overview stats ── */}
      <Section title="Overview">
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
          <StatCard label="Followers"   value={profile.followers.toLocaleString()} />
          <StatCard label="Repos"       value={profile.ownedRepos.toLocaleString()} />
          <StatCard label="Total Stars" value={profile.totalStars.toLocaleString()} />
          <StatCard label="Total Forks" value={profile.totalForks.toLocaleString()} />
        </div>
      </Section>

      {/* ── Contributions ── */}
      <Section title="Contributions (last year)">
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 mb-3">
          <StatCard label="Commits"      value={cs.totalCommits.toLocaleString()}      accent />
          <StatCard label="Pull Requests" value={cs.totalPullRequests.toLocaleString()} accent />
          <StatCard label="Issues"       value={cs.totalIssues.toLocaleString()}        accent />
          <StatCard label="PR Reviews"   value={cs.totalPrReviews.toLocaleString()}     accent />
        </div>
        <div className="grid grid-cols-2 gap-3">
          <StatCard label="Last 30 days" value={cs.last30DaysCommits.toLocaleString()} />
          <StatCard label="Last 90 days" value={cs.last90DaysCommits.toLocaleString()} />
        </div>
      </Section>

      {/* ── Languages ── */}
      {profile.topLanguages.length > 0 && (
        <Section title="Top Languages">
          <LanguageBar languages={profile.topLanguages} />
          <div className="mt-4 flex flex-col gap-2">
            {profile.topLanguages.map((lang) => (
              <LangRow key={lang.name} lang={lang} />
            ))}
          </div>
        </Section>
      )}
    </div>
  );
}

// ─── Sub-components ───────────────────────────────────────────────────────────

function BackLink() {
  return (
    <Link href="/search" className="text-sm text-blue-600 hover:underline mb-6 block">
      ← Back to search
    </Link>
  );
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div className="mb-8">
      <h2 className="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-3">
        {title}
      </h2>
      {children}
    </div>
  );
}

function StatCard({
  label,
  value,
  accent,
}: {
  label: string;
  value: string;
  accent?: boolean;
}) {
  return (
    <div
      className={`rounded-lg p-3 text-center border ${
        accent ? "border-blue-100 bg-blue-50" : "border-gray-100 bg-white"
      }`}
    >
      <div className="text-xl font-bold text-gray-900">{value}</div>
      <div className="text-xs text-gray-400 mt-0.5">{label}</div>
    </div>
  );
}

function ScoreDimRow({
  label,
  weight,
  value,
}: {
  label: string;
  weight: string;
  value: number;
}) {
  const color =
    value >= 80 ? "#7c3aed" :
    value >= 65 ? "#d97706" :
    value >= 50 ? "#16a34a" :
    value >= 35 ? "#2563eb" : "#9ca3af";

  return (
    <div className="flex items-center gap-3 text-sm">
      <span className="w-36 text-gray-700 shrink-0">{label}</span>
      <span className="text-xs text-gray-400 w-8 shrink-0">{weight}</span>
      <div className="flex-1 bg-gray-100 rounded-full h-2">
        <div
          className="h-2 rounded-full transition-all"
          style={{ width: `${value}%`, backgroundColor: color }}
        />
      </div>
      <span className="text-gray-600 font-semibold w-8 text-right">{value}</span>
    </div>
  );
}

function LanguageBar({ languages }: { languages: LanguageStat[] }) {
  return (
    <div className="flex h-3 rounded-full overflow-hidden gap-0.5">
      {languages.map((lang) => (
        <div
          key={lang.name}
          style={{ width: `${lang.percentage}%`, backgroundColor: lang.color || "#cccccc" }}
          title={`${lang.name} ${lang.percentage}%`}
        />
      ))}
    </div>
  );
}

function LangRow({ lang }: { lang: LanguageStat }) {
  return (
    <div className="flex items-center gap-2 text-sm">
      <span
        className="w-3 h-3 rounded-full shrink-0"
        style={{ backgroundColor: lang.color || "#cccccc" }}
      />
      <span className="w-28 text-gray-700">{lang.name}</span>
      <div className="flex-1 bg-gray-100 rounded-full h-1.5">
        <div
          className="h-1.5 rounded-full"
          style={{ width: `${lang.percentage}%`, backgroundColor: lang.color || "#cccccc" }}
        />
      </div>
      <span className="text-gray-400 w-12 text-right">{lang.percentage}%</span>
    </div>
  );
}
