import Link from "next/link";

const FEATURES = [
  {
    icon: "🔍",
    title: "Multi-platform search",
    desc: "Find developers across GitHub, GitLab, StackOverflow, and Bitbucket in one place.",
  },
  {
    icon: "📊",
    title: "Objective scoring",
    desc: "Every developer is scored 0-100 across 5 dimensions: activity, impact, contributions, collaboration, and tech stack.",
  },
  {
    icon: "📋",
    title: "Candidate pipeline",
    desc: "Save candidates with private notes, link their LinkedIn profile, and export your list as CSV.",
  },
  {
    icon: "✉",
    title: "Direct contact",
    desc: "See public contact emails right on the search results — no extra tools needed.",
  },
];

const STEPS = [
  { step: "1", title: "Search", desc: "Enter a skill, language, or name and pick a platform." },
  { step: "2", title: "Evaluate", desc: "Review the technical score and deep GitHub profile." },
  { step: "3", title: "Save & contact", desc: "Bookmark candidates, add notes, and reach out directly." },
];

export default function LandingPage() {
  return (
    <div className="min-h-[calc(100vh-57px)] bg-white">

      {/* ── Hero ── */}
      <section className="max-w-4xl mx-auto px-6 pt-20 pb-16 text-center">
        <span className="inline-block bg-blue-50 text-blue-700 text-xs font-semibold px-3 py-1 rounded-full mb-4 tracking-wide uppercase">
          For technical recruiters
        </span>
        <h1 className="text-4xl sm:text-5xl font-bold text-gray-900 mb-5 leading-tight">
          Find the right developers,<br />
          <span className="text-blue-600">faster.</span>
        </h1>
        <p className="text-lg text-gray-500 max-w-xl mx-auto mb-8">
          DevFinder searches GitHub, GitLab, StackOverflow, and Bitbucket, scores
          each developer objectively, and turns raw data into a shortlist your team
          can act on immediately.
        </p>
        <div className="flex gap-3 justify-center flex-wrap">
          <Link
            href="/register"
            className="bg-blue-600 text-white px-6 py-3 rounded-lg text-sm font-semibold hover:bg-blue-700 transition-colors"
          >
            Get started for free
          </Link>
          <Link
            href="/login"
            className="border border-gray-300 text-gray-700 px-6 py-3 rounded-lg text-sm font-semibold hover:border-gray-400 transition-colors"
          >
            Sign in
          </Link>
        </div>
      </section>

      {/* ── Features ── */}
      <section className="bg-gray-50 py-16">
        <div className="max-w-4xl mx-auto px-6">
          <h2 className="text-2xl font-bold text-center text-gray-900 mb-10">
            Everything a recruiter needs
          </h2>
          <div className="grid sm:grid-cols-2 gap-6">
            {FEATURES.map((f) => (
              <div key={f.title} className="bg-white border border-gray-200 rounded-xl p-6">
                <div className="text-3xl mb-3">{f.icon}</div>
                <h3 className="font-semibold text-gray-900 mb-1">{f.title}</h3>
                <p className="text-sm text-gray-500">{f.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ── How it works ── */}
      <section className="py-16">
        <div className="max-w-3xl mx-auto px-6">
          <h2 className="text-2xl font-bold text-center text-gray-900 mb-10">
            How it works
          </h2>
          <div className="flex flex-col sm:flex-row gap-6">
            {STEPS.map((s) => (
              <div key={s.step} className="flex-1 text-center">
                <div className="w-10 h-10 rounded-full bg-blue-600 text-white font-bold text-lg flex items-center justify-center mx-auto mb-3">
                  {s.step}
                </div>
                <h3 className="font-semibold text-gray-900 mb-1">{s.title}</h3>
                <p className="text-sm text-gray-500">{s.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ── CTA ── */}
      <section className="bg-blue-600 py-14 text-center">
        <h2 className="text-2xl font-bold text-white mb-3">Ready to build your shortlist?</h2>
        <p className="text-blue-100 text-sm mb-6">Free to use. No credit card required.</p>
        <Link
          href="/register"
          className="inline-block bg-white text-blue-700 font-semibold px-8 py-3 rounded-lg text-sm hover:bg-blue-50 transition-colors"
        >
          Create your account
        </Link>
      </section>
    </div>
  );
}
