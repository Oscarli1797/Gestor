"use client";

import { useState } from "react";
import { api } from "@/lib/api";

interface Consulta {
  idConsulta: string;
  nombre: string;
  autor: string;
  numeroVisitante: number;
}

const PLATFORMS = [
  { value: 1, label: "GitHub" },
  { value: 2, label: "GitLab" },
  { value: 3, label: "StackOverflow" },
  { value: 4, label: "Bitbucket" },
];

export default function HomePage() {
  const [platform, setPlatform] = useState(1);
  const [query, setQuery] = useState("");
  const [results, setResults] = useState<Consulta[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  async function handleSearch(e: React.FormEvent) {
    e.preventDefault();
    if (!query.trim()) return;
    setLoading(true);
    setError("");
    try {
      const { data } = await api.get("/api/search", {
        params: { platform, query },
      });
      setResults(data.data ?? []);
    } catch (err: unknown) {
      const msg =
        err instanceof Error ? err.message : "Search failed. Please try again.";
      setError(msg);
    } finally {
      setLoading(false);
    }
  }

  async function handleExport() {
    const res = await api.get("/api/export", {
      params: { platform, query },
      responseType: "blob",
    });
    const url = URL.createObjectURL(res.data);
    const a = document.createElement("a");
    a.href = url;
    a.download = "consulta.txt";
    a.click();
    URL.revokeObjectURL(url);
  }

  return (
    <div className="max-w-4xl mx-auto px-4 py-10">
      <h1 className="text-2xl font-bold mb-6">Search Projects</h1>

      <form onSubmit={handleSearch} className="flex gap-2 mb-6">
        <select
          value={platform}
          onChange={(e) => setPlatform(Number(e.target.value))}
          className="border border-gray-300 rounded px-3 py-2 text-sm bg-white"
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
          placeholder="Search keyword…"
          className="flex-1 border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
        />

        <button
          type="submit"
          disabled={loading}
          className="bg-blue-600 text-white px-4 py-2 rounded text-sm hover:bg-blue-700 disabled:opacity-50"
        >
          {loading ? "Searching…" : "Search"}
        </button>
      </form>

      {error && <p className="text-red-600 text-sm mb-4">{error}</p>}

      {results.length > 0 && (
        <>
          <div className="flex items-center justify-between mb-3">
            <p className="text-sm text-gray-500">{results.length} results</p>
            <button
              onClick={handleExport}
              className="text-sm text-blue-600 hover:underline"
            >
              Export .txt
            </button>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-sm border border-gray-200 rounded">
              <thead className="bg-gray-100 text-left">
                <tr>
                  <th className="px-4 py-2 font-medium">ID</th>
                  <th className="px-4 py-2 font-medium">Name</th>
                  <th className="px-4 py-2 font-medium">Author / Owner</th>
                  <th className="px-4 py-2 font-medium text-right">Stars / Views</th>
                </tr>
              </thead>
              <tbody>
                {results.map((r, i) => (
                  <tr key={i} className="border-t border-gray-100 hover:bg-gray-50">
                    <td className="px-4 py-2 text-gray-400 font-mono text-xs">
                      {r.idConsulta}
                    </td>
                    <td className="px-4 py-2 font-medium">{r.nombre}</td>
                    <td className="px-4 py-2 text-gray-600">{r.autor}</td>
                    <td className="px-4 py-2 text-right text-gray-600">
                      {r.numeroVisitante.toLocaleString()}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}
    </div>
  );
}
