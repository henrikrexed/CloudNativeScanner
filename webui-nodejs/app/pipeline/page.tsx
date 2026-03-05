"use client";

import { useEffect, useState, useCallback, useMemo } from "react";
import {
  fetchPipelineStatus,
  fetchPipelineErrors,
  fetchScanHistory,
  fetchTopicsByStage,
  triggerScan,
  type PipelineSummary,
  type PipelineJobCount,
  type PipelineError,
  type ScanHistoryEntry,
  type TopicStageCount,
} from "@/lib/v2api";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { StatCard } from "@/components/stat-card";
import {
  Table,
  TableHeader,
  TableBody,
  TableRow,
  TableHead,
  TableCell,
} from "@/components/ui/table";
import { Tabs, TabsList, TabsTrigger, TabsContent } from "@/components/ui/tabs";
import { formatRelativeDate } from "@/lib/utils";
import {
  Activity,
  Play,
  AlertTriangle,
  CheckCircle2,
  Clock,
  XCircle,
  Loader2,
  RefreshCw,
  Zap,
  BarChart3,
} from "lucide-react";

export default function PipelinePage() {
  const [summary, setSummary] = useState<PipelineSummary | null>(null);
  const [jobCounts, setJobCounts] = useState<PipelineJobCount[]>([]);
  const [errors, setErrors] = useState<PipelineError[]>([]);
  const [scanHistory, setScanHistory] = useState<ScanHistoryEntry[]>([]);
  const [topicsByStage, setTopicsByStage] = useState<TopicStageCount[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [scanning, setScanning] = useState(false);
  const [tab, setTab] = useState("overview");

  const loadData = useCallback(async () => {
    setError(null);
    try {
      const [status, errs, history, stages] = await Promise.all([
        fetchPipelineStatus(),
        fetchPipelineErrors(),
        fetchScanHistory(),
        fetchTopicsByStage(),
      ]);
      setSummary(status.summary);
      setJobCounts(status.jobCounts);
      setErrors(errs);
      setScanHistory(history);
      setTopicsByStage(stages);
    } catch (e) {
      setError("Failed to load pipeline data");
      console.error("Failed to load pipeline data", e);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData]);

  async function handleTriggerScan() {
    setScanning(true);
    try {
      await triggerScan();
      await loadData();
    } catch (e) {
      console.error("Scan trigger failed", e);
    } finally {
      setScanning(false);
    }
  }

  // Compute total once, not per-item inside map
  const stageTotal = useMemo(
    () => topicsByStage.reduce((sum, s) => sum + (s.count || 0), 0),
    [topicsByStage]
  );

  if (loading) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-8 w-48" />
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {[1, 2, 3, 4].map((i) => (
            <Skeleton key={i} className="h-24" />
          ))}
        </div>
        <Skeleton className="h-64" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="text-center py-20">
        <AlertTriangle className="h-8 w-8 mx-auto text-red-400 mb-3" />
        <p className="text-gray-600">{error}</p>
        <Button variant="outline" className="mt-4" onClick={() => { setLoading(true); loadData(); }}>
          Retry
        </Button>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Pipeline Monitor</h1>
          <p className="text-sm text-gray-500 mt-1">
            Job queue status, scan history, and error tracking
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Button variant="outline" size="sm" onClick={loadData}>
            <RefreshCw className="h-4 w-4 mr-1" aria-hidden="true" />
            Refresh
          </Button>
          <Button onClick={handleTriggerScan} disabled={scanning}>
            {scanning ? (
              <>
                <Loader2 className="h-4 w-4 mr-2 animate-spin" aria-hidden="true" />
                Scanning...
              </>
            ) : (
              <>
                <Play className="h-4 w-4 mr-2" aria-hidden="true" />
                Trigger Scan
              </>
            )}
          </Button>
        </div>
      </div>

      {/* Summary Cards */}
      {summary && (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <StatCard
            title="Pending"
            value={summary.pending}
            icon={<Clock className="h-5 w-5 text-yellow-600" />}
          />
          <StatCard
            title="Processing"
            value={summary.processing}
            icon={<Zap className="h-5 w-5 text-blue-600" />}
          />
          <StatCard
            title="Completed"
            value={summary.completed}
            icon={<CheckCircle2 className="h-5 w-5 text-green-600" />}
          />
          <StatCard
            title="Failed"
            value={summary.failed}
            icon={<XCircle className="h-5 w-5 text-red-600" />}
          />
        </div>
      )}

      {/* Tabs */}
      <Tabs value={tab} onValueChange={setTab}>
        <TabsList>
          <TabsTrigger value="overview">Overview</TabsTrigger>
          <TabsTrigger value="errors">
            Errors{" "}
            {errors.length > 0 && (
              <Badge variant="destructive" className="ml-1 text-xs">
                {errors.length}
              </Badge>
            )}
          </TabsTrigger>
          <TabsTrigger value="history">Scan History</TabsTrigger>
        </TabsList>

        {/* Overview Tab */}
        <TabsContent value="overview">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Job Counts by Stage */}
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Activity className="h-5 w-5" aria-hidden="true" />
                  Jobs by Stage
                </CardTitle>
              </CardHeader>
              <CardContent>
                {jobCounts.length === 0 ? (
                  <p className="text-sm text-gray-400 text-center py-6">
                    No job data
                  </p>
                ) : (
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead>Stage</TableHead>
                        <TableHead>Status</TableHead>
                        <TableHead className="text-right">Count</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {jobCounts.map((row, i) => (
                        <TableRow key={`${row.stage}-${row.status}`}>
                          <TableCell className="font-medium text-sm">
                            {row.stage}
                          </TableCell>
                          <TableCell>
                            <StatusBadge status={row.status} />
                          </TableCell>
                          <TableCell className="text-right font-mono text-sm">
                            {row.count}
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                )}
              </CardContent>
            </Card>

            {/* Topics by Pipeline Stage */}
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <BarChart3 className="h-5 w-5" aria-hidden="true" />
                  Topics by Stage
                </CardTitle>
              </CardHeader>
              <CardContent>
                {topicsByStage.length === 0 ? (
                  <p className="text-sm text-gray-400 text-center py-6">
                    No data
                  </p>
                ) : (
                  <div className="space-y-3">
                    {topicsByStage.map((stage) => {
                      const pct = stageTotal > 0 ? (stage.count / stageTotal) * 100 : 0;

                      return (
                        <div key={stage.pipeline_stage}>
                          <div className="flex justify-between text-sm mb-1">
                            <span className="font-medium text-gray-700 capitalize">
                              {stage.pipeline_stage}
                            </span>
                            <span className="text-gray-500">{stage.count}</span>
                          </div>
                          <div className="h-2 bg-gray-100 rounded-full overflow-hidden" role="progressbar" aria-valuenow={pct} aria-valuemin={0} aria-valuemax={100}>
                            <div
                              className="h-full bg-primary-500 rounded-full transition-all"
                              style={{ width: `${pct}%` }}
                            />
                          </div>
                        </div>
                      );
                    })}
                  </div>
                )}
              </CardContent>
            </Card>
          </div>
        </TabsContent>

        {/* Errors Tab */}
        <TabsContent value="errors">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <AlertTriangle className="h-5 w-5 text-red-500" aria-hidden="true" />
                Recent Errors
              </CardTitle>
            </CardHeader>
            <CardContent>
              {errors.length === 0 ? (
                <div className="text-center py-8">
                  <CheckCircle2 className="h-8 w-8 mx-auto text-green-500 mb-2" aria-hidden="true" />
                  <p className="text-sm text-gray-500">No recent errors</p>
                </div>
              ) : (
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>ID</TableHead>
                      <TableHead>Stage</TableHead>
                      <TableHead>Error</TableHead>
                      <TableHead>Time</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {errors.map((err, i) => (
                      <TableRow key={err.id ?? i}>
                        <TableCell className="font-mono text-xs">
                          {err.id ?? err.topic_id ?? "-"}
                        </TableCell>
                        <TableCell>
                          <Badge variant="outline" className="text-xs">
                            {err.stage || "-"}
                          </Badge>
                        </TableCell>
                        <TableCell className="text-sm text-red-600 max-w-md truncate">
                          {err.error_message || "-"}
                        </TableCell>
                        <TableCell className="text-xs text-gray-400">
                          {err.updated_at
                            ? formatRelativeDate(err.updated_at)
                            : "-"}
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              )}
            </CardContent>
          </Card>
        </TabsContent>

        {/* Scan History Tab */}
        <TabsContent value="history">
          <Card>
            <CardHeader>
              <CardTitle>Scan History</CardTitle>
            </CardHeader>
            <CardContent>
              {scanHistory.length === 0 ? (
                <p className="text-sm text-gray-400 text-center py-8">
                  No scan history yet
                </p>
              ) : (
                <div className="overflow-x-auto">
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead>Category</TableHead>
                        <TableHead>Source</TableHead>
                        <TableHead>Topics Found</TableHead>
                        <TableHead>New Topics</TableHead>
                        <TableHead>Started</TableHead>
                        <TableHead>Status</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {scanHistory.map((scan, i) => (
                        <TableRow key={i}>
                          <TableCell className="font-medium text-sm">
                            {scan.category_name || scan.category_id || "-"}
                          </TableCell>
                          <TableCell className="text-sm">
                            {scan.source_type || "-"}
                          </TableCell>
                          <TableCell className="font-mono text-sm">
                            {scan.topics_found ?? "-"}
                          </TableCell>
                          <TableCell className="font-mono text-sm">
                            {scan.new_topics ?? "-"}
                          </TableCell>
                          <TableCell className="text-xs text-gray-400">
                            {scan.started_at
                              ? formatRelativeDate(scan.started_at)
                              : "-"}
                          </TableCell>
                          <TableCell>
                            <StatusBadge
                              status={scan.status || "unknown"}
                            />
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </div>
              )}
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  );
}

function StatusBadge({ status }: { status: string }) {
  const upper = status.toUpperCase();
  if (upper === "COMPLETED" || upper === "SUCCESS") {
    return <Badge variant="success">{status}</Badge>;
  }
  if (upper === "FAILED" || upper === "ERROR") {
    return <Badge variant="destructive">{status}</Badge>;
  }
  if (upper === "PROCESSING" || upper === "RUNNING") {
    return <Badge variant="default">{status}</Badge>;
  }
  return <Badge variant="warning">{status}</Badge>;
}
