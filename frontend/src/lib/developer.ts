import { api } from "./api";

export interface LanguageStat {
  name: string;
  color: string;
  bytes: number;
  percentage: number;
}

export interface ContributionStats {
  totalCommits: number;
  totalPullRequests: number;
  totalIssues: number;
  totalPrReviews: number;
  totalContributions: number;
  last90DaysCommits: number;
  last30DaysCommits: number;
}

export interface ScoreBreakdown {
  recentActivity: number;
  projectImpact: number;
  contributions: number;
  collaboration: number;
  techStack: number;
  total: number;
  tier: string;
  aiSummary: string | null;
  aiInsights: string[] | null;
}

export interface DeveloperProfile {
  id: string;
  platform: string;
  username: string;
  displayName: string;
  avatarUrl: string;
  profileUrl: string;
  bio: string | null;
  location: string | null;
  company: string | null;
  joinedAt: string | null;
  email: string | null;
  blog: string | null;
  linkedinUrl: string | null;
  followers: number;
  following: number;
  totalPublicRepos: number;
  ownedRepos: number;
  totalStars: number;
  totalForks: number;
  topLanguages: LanguageStat[];
  contributionStats: ContributionStats;
  scoreBreakdown: ScoreBreakdown | null;
}

export async function getDeveloperProfile(
  platform: string,
  username: string
): Promise<DeveloperProfile> {
  const { data } = await api.get(
    `/api/developer/${platform}/${username}/profile`
  );
  return data.data as DeveloperProfile;
}
