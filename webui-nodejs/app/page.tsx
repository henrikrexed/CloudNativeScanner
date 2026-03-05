"use client";

import { useEffect, useState, useCallback } from "react";
import Link from "next/link";
import {
  fetchTopics,
  fetchStats,
  type TopicSummary,
  type TopicsPage,
  type DashboardStats,
} from "@/lib/v2api";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import { StatCard } from "@/components/stat-card";
import { formatRelativeDate, truncate, qualityBadge } from "@/lib/utils";
import {
  BarChart3,
  TrendingUp,
  Clock,
  AlertTriangle,
  ChevronLeft,
  ChevronRight,
  ExternalLink,
  Search,
} from "lucide-react";

export default function DashboardPage() {
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [topicsData, setTopicsData] = useState<TopicsPage | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Filters
  const [page, setPage] = useState(0);
  const [categoryId, setCategoryId] = useState<string>("");
  const [sourceType, setSourceType] = useState<string>("");
  const [minQuality, setMinQuality] = useState<string>("");
  const [dateFrom, setDateFrom] = useState<string>("");
  const [dateTo, setDateTo] = useState<string>("");

  const pageSize = 20;

  const loadTopics = useCallback(async () => {
    const params: Record<string, unknown> = { page, size: pageSize };
    if (categoryId) params.categoryId = Number(categoryId);
    if (sourceType) params.sourceType = sourceType;
    if (minQuality) params.minQuality = Number(minQuality);
    if (dateFrom) params.dateFrom = dateFrom;
    if (dateTo) params.dateTo = dateTo;
    const data = await fetchTopics(params as Parameters<typeof fetchTopics>[0]);
    setTopicsData(data);
  }, [page, categoryId, sourceType, minQuality, dateFrom, dateTo]);

  // Initial load: stats + topics
  useEffect(() => {
    let cancelled = false;
    async function init() {
      setLoading(true);
      setError(null);
      try {
        const [s, _] = await Promise.all([fetchStats(), loadTopics()]);
        if (!cancelled) setStats(s);
      } catch (e) {
        if (!cancelled) setError("Failed to load dashboard data. Check that the API is running.");
        console.error("Failed to load dashboard", e);
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    init();
    return () => { cancelled = true; };
  }, [loadTopics]);

  const totalPages = topicsData ? Math.ceil(topicsData.total / pageSize) : 0;

  if (loading) {
    return <DashboardSkeleton />;
  }

  if (error) {
    return (
      <div className="text-center py-20">
        <AlertTriangle className="h-8 w-8 mx-auto text-red-400 mb-3" />
        <p className="text-gray-600">{error}</p>
        <Button variant="outline" className="mt-4" onClick={() => window.location.reload()}>
          Retry
        </Button>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Dashboard</h1>
        <p className="text-sm text-gray-500 mt-1">
          Overview of collected topics and pipeline status
        </p>
      </div>

      {/* Stats Cards */}
      {stats && (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <StatCard
            title="Total Topics"
            value={stats.totalTopics}
            icon={<BarChart3 className="h-5 w-5 text-primary-600" />}
          />
          <StatCard
            title="This Week"
            value={stats.topicsThisWeek}
            icon={<TrendingUp className="h-5 w-5 text-green-600" />}
          />
          <StatCard
            title="Pending Jobs"
            value={stats.pendingJobs}
            icon={<Clock className="h-5 w-5 text-yellow-600" />}
          />
          <StatCard
            title="Failed Jobs"
            value={stats.failedJobs}
            icon={<AlertTriangle className="h-5 w-5 text-red-600" />}
          />
        </div>
      )}

      {/* Filters */}
      <Card>
        <CardContent className="p-4">
          <div className="flex flex-wrap items-end gap-3">
            <div className="flex-1 min-w-[140px]">
              <label htmlFor="filter-source" className="text-xs font-medium text-gray-500 mb-1 block">
                Source
              </label>
              <Select
                id="filter-source"
                value={sourceType}
                onChange={(e) => {
                  setSourceType(e.target.value);
                  setPage(0);
                }}
              >
                <option value="">All Sources</option>
                {stats?.bySource.map((s) => (
                  <option key={s.name} value={s.name}>
                    {s.name}
                  </option>
                ))}
              </Select>
            </div>
            <div className="flex-1 min-w-[140px]">
              <label htmlFor="filter-category" className="text-xs font-medium text-gray-500 mb-1 block">
                Category
              </label>
              <Select
                id="filter-category"
                value={categoryId}
                onChange={(e) => {
                  setCategoryId(e.target.value);
                  setPage(0);
                }}
              >
                <option value="">All Categories</option>
                {stats?.byCategory.map((c) => (
                  <option key={c.name} value={String(c.id || c.name)}>
                    {c.name}
                  </option>
                ))}
              </Select>
            </div>
            <div className="w-[120px]">
              <label htmlFor="filter-quality" className="text-xs font-medium text-gray-500 mb-1 block">
                Min Quality
              </label>
              <Input
                id="filter-quality"
                type="number"
                min="0"
                max="1"
                step="0.1"
                placeholder="0.0"
                value={minQuality}
                onChange={(e) => {
                  setMinQuality(e.target.value);
                  setPage(0);
                }}
              />
            </div>
            <div className="w-[140px]">
              <label htmlFor="filter-from" className="text-xs font-medium text-gray-500 mb-1 block">
                From
              </label>
              <Input
                id="filter-from"
                type="date"
                value={dateFrom}
                onChange={(e) => {
                  setDateFrom(e.target.value);
                  setPage(0);
                }}
              />
            </div>
            <div className="w-[140px]">
              <label htmlFor="filter-to" className="text-xs font-medium text-gray-500 mb-1 block">
                To
              </label>
              <Input
                id="filter-to"
                type="date"
                value={dateTo}
                onChange={(e) => {
                  setDateTo(e.target.value);
                  setPage(0);
                }}
              />
            </div>
            <Button
              variant="outline"
              size="sm"
              onClick={() => {
                setCategoryId("");
                setSourceType("");
                setMinQuality("");
                setDateFrom("");
                setDateTo("");
                setPage(0);
              }}
            >
              Clear
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* Topic Feed */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle>
              Topics{" "}
              <span className="text-sm font-normal text-gray-500">
                ({topicsData?.total ?? 0})
              </span>
            </CardTitle>
          </div>
        </CardHeader>
        <CardContent>
          {topicsData?.topics.length === 0 ? (
            <div className="text-center py-12 text-gray-400">
              <Search className="h-8 w-8 mx-auto mb-2" />
              <p>No topics found</p>
            </div>
          ) : (
            <div className="space-y-3">
              {topicsData?.topics.map((topic) => (
                <TopicRow key={topic.id} topic={topic} />
              ))}
            </div>
          )}

          {/* Pagination */}
          {totalPages > 1 && (
            <nav aria-label="Pagination" className="flex items-center justify-between mt-6 pt-4 border-t border-gray-100">
              <p className="text-sm text-gray-500">
                Page {page + 1} of {totalPages}
              </p>
              <div className="flex gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  disabled={page === 0}
                  aria-label="Previous page"
                  onClick={() => setPage((p) => p - 1)}
                >
                  <ChevronLeft className="h-4 w-4" />
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  disabled={page >= totalPages - 1}
                  aria-label="Next page"
                  onClick={() => setPage((p) => p + 1)}
                >
                  <ChevronRight className="h-4 w-4" />
                </Button>
              </div>
            </nav>
          )}
        </CardContent>
      </Card>
    </div>
  );
}

function TopicRow({ topic }: { topic: TopicSummary }) {
  const quality = topic.quality_score ?? 0;

  return (
    <Link
      href={`/topics/${topic.id}`}
      className="flex items-start gap-4 p-4 rounded-lg border border-gray-100 hover:border-gray-200 hover:bg-gray-50/50 transition-colors group"
    >
      <div className="flex-1 min-w-0">
        <div className="flex items-center gap-2 mb-1">
          <h3 className="text-sm font-medium text-gray-900 group-hover:text-primary-700 truncate">
            {topic.title || "Untitled"}
          </h3>
          {topic.url && (
            <ExternalLink className="h-3 w-3 text-gray-400 flex-shrink-0" aria-hidden="true" />
          )}
        </div>
        {topic.summary && (
          <p className="text-xs text-gray-500 line-clamp-2">
            {truncate(topic.summary, 200)}
          </p>
        )}
        <div className="flex items-center gap-2 mt-2 flex-wrap">
          {topic.source_type && (
            <Badge variant="secondary">{topic.source_type}</Badge>
          )}
          {topic.pipeline_stage && (
            <Badge
              variant={
                topic.pipeline_stage === "analyzed" ? "success" : "outline"
              }
            >
              {topic.pipeline_stage}
            </Badge>
          )}
          {quality > 0 && (
            <span className={`text-xs font-medium ${qualityBadge(quality)} rounded-full px-2 py-0.5`}>
              {(quality * 100).toFixed(0)}%
            </span>
          )}
        </div>
      </div>
      <div className="text-xs text-gray-400 whitespace-nowrap flex-shrink-0">
        {topic.collected_at
          ? formatRelativeDate(topic.collected_at)
          : topic.source_date
          ? formatRelativeDate(topic.source_date)
          : ""}
      </div>
    </Link>
  );
}

function DashboardSkeleton() {
  return (
    <div className="space-y-6">
      <div>
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-4 w-72 mt-2" />
      </div>
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {[1, 2, 3, 4].map((i) => (
          <Card key={i}>
            <CardContent className="p-5">
              <Skeleton className="h-4 w-20" />
              <Skeleton className="h-8 w-16 mt-2" />
            </CardContent>
          </Card>
        ))}
      </div>
      <Card>
        <CardContent className="p-6 space-y-4">
          {[1, 2, 3, 4, 5].map((i) => (
            <Skeleton key={i} className="h-20 w-full" />
          ))}
        </CardContent>
      </Card>
    </div>
  );
}
