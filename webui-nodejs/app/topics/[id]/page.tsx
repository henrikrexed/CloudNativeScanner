"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import Link from "next/link";
import {
  fetchTopicDetail,
  generateContent,
  type TopicDetail,
  type TopicSummary,
} from "@/lib/v2api";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { Select } from "@/components/ui/select";
import { formatDate, qualityBadge } from "@/lib/utils";
import {
  ArrowLeft,
  ExternalLink,
  Sparkles,
  Calendar,
  Globe,
  Star,
  Shield,
  Loader2,
  AlertTriangle,
} from "lucide-react";

export default function TopicDetailPage() {
  const params = useParams();
  const router = useRouter();
  const id = Number(params.id);

  const [topic, setTopic] = useState<TopicDetail | null>(null);
  const [relatedTopics, setRelatedTopics] = useState<TopicSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [generating, setGenerating] = useState(false);
  const [genFormat, setGenFormat] = useState("blog_post");

  useEffect(() => {
    if (isNaN(id)) {
      setError("Invalid topic ID");
      setLoading(false);
      return;
    }
    let cancelled = false;
    async function load() {
      setLoading(true);
      setError(null);
      try {
        const data = await fetchTopicDetail(id);
        if (cancelled) return;
        setRelatedTopics(data.relatedTopics || []);
        const { relatedTopics: _, ...topicData } = data;
        setTopic(topicData);
      } catch (e) {
        if (!cancelled) setError("Failed to load topic");
        console.error("Failed to load topic", e);
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    load();
    return () => { cancelled = true; };
  }, [id]);

  async function handleGenerate() {
    setGenerating(true);
    try {
      const result = await generateContent({
        topicId: id,
        outputFormat: genFormat,
      });
      if (result?.id) {
        router.push(`/studio?generated=${result.id}`);
      }
    } catch (e) {
      console.error("Generation failed", e);
    } finally {
      setGenerating(false);
    }
  }

  if (loading) {
    return <TopicSkeleton />;
  }

  if (error || !topic) {
    return (
      <div className="text-center py-20">
        <AlertTriangle className="h-8 w-8 mx-auto text-red-400 mb-3" />
        <p className="text-gray-500">{error || "Topic not found"}</p>
        <Button variant="outline" className="mt-4" onClick={() => router.push("/")}>
          Back to Dashboard
        </Button>
      </div>
    );
  }

  const quality = topic.quality_score ?? 0;
  const relevance = topic.relevance_score ?? 0;
  const tags = topic.tags ? topic.tags.split(",").filter(Boolean) : [];

  return (
    <div className="space-y-6">
      {/* Back + Header */}
      <div>
        <button
          onClick={() => router.back()}
          className="flex items-center gap-1 text-sm text-gray-500 hover:text-gray-700 mb-3"
        >
          <ArrowLeft className="h-4 w-4" aria-hidden="true" />
          Back
        </button>
        <div className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-4">
          <div className="flex-1">
            <h1 className="text-xl font-bold text-gray-900">
              {topic.title}
            </h1>
            <div className="flex items-center gap-3 mt-2 flex-wrap">
              {topic.source_type && (
                <Badge variant="secondary">
                  <Globe className="h-3 w-3 mr-1" aria-hidden="true" />
                  {topic.source_type}
                </Badge>
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
              {topic.source_date && (
                <span className="text-xs text-gray-400 flex items-center gap-1">
                  <Calendar className="h-3 w-3" aria-hidden="true" />
                  {formatDate(topic.source_date)}
                </span>
              )}
            </div>
          </div>
          {topic.url && (
            <a
              href={topic.url}
              target="_blank"
              rel="noopener noreferrer"
              className="flex items-center gap-1 text-sm text-primary-600 hover:text-primary-700"
            >
              <ExternalLink className="h-4 w-4" aria-hidden="true" />
              Source
            </a>
          )}
        </div>
      </div>

      {/* Score Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <Card>
          <CardContent className="p-4 flex items-center gap-3">
            <Star className="h-5 w-5 text-yellow-500" aria-hidden="true" />
            <div>
              <p className="text-xs text-gray-500">Quality Score</p>
              <p className={`text-lg font-bold ${qualityBadge(quality)} rounded px-1`}>
                {quality > 0 ? `${(quality * 100).toFixed(0)}%` : "N/A"}
              </p>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4 flex items-center gap-3">
            <Shield className="h-5 w-5 text-blue-500" aria-hidden="true" />
            <div>
              <p className="text-xs text-gray-500">Relevance Score</p>
              <p className="text-lg font-bold text-gray-900">
                {relevance > 0 ? `${(relevance * 100).toFixed(0)}%` : "N/A"}
              </p>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4">
            <p className="text-xs text-gray-500 mb-1">Tags</p>
            <div className="flex flex-wrap gap-1">
              {tags.length > 0 ? (
                tags.map((tag) => (
                  <Badge key={tag} variant="outline" className="text-xs">
                    {tag.trim()}
                  </Badge>
                ))
              ) : (
                <span className="text-sm text-gray-400">No tags</span>
              )}
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Summary */}
      {topic.summary && (
        <Card>
          <CardHeader>
            <CardTitle>Summary</CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-sm text-gray-700 leading-relaxed whitespace-pre-line">
              {topic.summary}
            </p>
          </CardContent>
        </Card>
      )}

      {/* Extracted Content */}
      {topic.extracted_content && (
        <Card>
          <CardHeader>
            <CardTitle>Extracted Content</CardTitle>
          </CardHeader>
          <CardContent>
            <pre className="whitespace-pre-wrap text-sm font-sans bg-gray-50 rounded-lg p-4 max-h-[500px] overflow-y-auto text-gray-700">
              {topic.extracted_content}
            </pre>
          </CardContent>
        </Card>
      )}

      {/* Generate Content */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Sparkles className="h-5 w-5 text-primary-600" aria-hidden="true" />
            Generate Content
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex items-center gap-3">
            <label htmlFor="gen-format" className="sr-only">Output format</label>
            <Select
              id="gen-format"
              value={genFormat}
              onChange={(e) => setGenFormat(e.target.value)}
              className="w-48"
            >
              <option value="blog_post">Blog Post</option>
              <option value="youtube_script">YouTube Script</option>
              <option value="linkedin_post">LinkedIn Post</option>
              <option value="newsletter">Newsletter</option>
            </Select>
            <Button onClick={handleGenerate} disabled={generating}>
              {generating ? (
                <>
                  <Loader2 className="h-4 w-4 mr-2 animate-spin" aria-hidden="true" />
                  Generating...
                </>
              ) : (
                <>
                  <Sparkles className="h-4 w-4 mr-2" aria-hidden="true" />
                  Generate
                </>
              )}
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* Related Topics */}
      {relatedTopics.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle>
              Related Topics{" "}
              <span className="text-sm font-normal text-gray-500">
                ({relatedTopics.length})
              </span>
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-2">
              {relatedTopics.map((rt) => (
                <Link
                  key={rt.id}
                  href={`/topics/${rt.id}`}
                  className="flex items-center justify-between p-3 rounded-lg border border-gray-100 hover:bg-gray-50 transition-colors"
                >
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium text-gray-900 truncate">
                      {rt.title}
                    </p>
                    <div className="flex items-center gap-2 mt-1">
                      {rt.source_type && (
                        <Badge variant="secondary" className="text-xs">
                          {rt.source_type}
                        </Badge>
                      )}
                    </div>
                  </div>
                  <ExternalLink className="h-3 w-3 text-gray-400 flex-shrink-0" aria-hidden="true" />
                </Link>
              ))}
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  );
}

function TopicSkeleton() {
  return (
    <div className="space-y-6">
      <Skeleton className="h-6 w-16" />
      <Skeleton className="h-8 w-96" />
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        {[1, 2, 3].map((i) => (
          <Skeleton key={i} className="h-20" />
        ))}
      </div>
      <Skeleton className="h-40" />
      <Skeleton className="h-60" />
    </div>
  );
}
