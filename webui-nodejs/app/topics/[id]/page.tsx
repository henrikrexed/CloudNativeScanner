"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import Link from "next/link";
import { fetchTopicDetail, generateContent } from "@/lib/v2api";
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
} from "lucide-react";

export default function TopicDetailPage() {
  const params = useParams();
  const router = useRouter();
  const id = Number(params.id);

  const [topic, setTopic] = useState<Record<string, unknown> | null>(null);
  const [relatedTopics, setRelatedTopics] = useState<Record<string, unknown>[]>([]);
  const [loading, setLoading] = useState(true);
  const [generating, setGenerating] = useState(false);
  const [genFormat, setGenFormat] = useState("blog_post");

  useEffect(() => {
    async function load() {
      setLoading(true);
      try {
        const data = await fetchTopicDetail(id);
        setRelatedTopics(
          (data.relatedTopics as Record<string, unknown>[]) || []
        );
        const { relatedTopics: _, ...topicData } = data;
        setTopic(topicData);
      } catch (e) {
        console.error("Failed to load topic", e);
      } finally {
        setLoading(false);
      }
    }
    if (id) load();
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

  if (!topic) {
    return (
      <div className="text-center py-20">
        <p className="text-gray-500">Topic not found</p>
        <Button variant="outline" className="mt-4" onClick={() => router.push("/")}>
          Back to Dashboard
        </Button>
      </div>
    );
  }

  const quality = Number(topic.quality_score || 0);
  const relevance = Number(topic.relevance_score || 0);
  const tags = topic.tags ? String(topic.tags).split(",").filter(Boolean) : [];

  return (
    <div className="space-y-6">
      {/* Back + Header */}
      <div>
        <button
          onClick={() => router.back()}
          className="flex items-center gap-1 text-sm text-gray-500 hover:text-gray-700 mb-3"
        >
          <ArrowLeft className="h-4 w-4" />
          Back
        </button>
        <div className="flex items-start justify-between gap-4">
          <div className="flex-1">
            <h1 className="text-xl font-bold text-gray-900">
              {String(topic.title)}
            </h1>
            <div className="flex items-center gap-3 mt-2">
              {topic.source_type && (
                <Badge variant="secondary">
                  <Globe className="h-3 w-3 mr-1" />
                  {String(topic.source_type)}
                </Badge>
              )}
              {topic.pipeline_stage && (
                <Badge
                  variant={
                    topic.pipeline_stage === "analyzed" ? "success" : "outline"
                  }
                >
                  {String(topic.pipeline_stage)}
                </Badge>
              )}
              {topic.source_date && (
                <span className="text-xs text-gray-400 flex items-center gap-1">
                  <Calendar className="h-3 w-3" />
                  {formatDate(String(topic.source_date))}
                </span>
              )}
            </div>
          </div>
          {topic.url && (
            <a
              href={String(topic.url)}
              target="_blank"
              rel="noopener noreferrer"
              className="flex items-center gap-1 text-sm text-primary-600 hover:text-primary-700"
            >
              <ExternalLink className="h-4 w-4" />
              Source
            </a>
          )}
        </div>
      </div>

      {/* Score Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <Card>
          <CardContent className="p-4 flex items-center gap-3">
            <Star className="h-5 w-5 text-yellow-500" />
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
            <Shield className="h-5 w-5 text-blue-500" />
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
              {String(topic.summary)}
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
            <div className="prose prose-sm max-w-none text-gray-700">
              <pre className="whitespace-pre-wrap text-sm font-sans bg-gray-50 rounded-lg p-4 max-h-[500px] overflow-y-auto">
                {String(topic.extracted_content)}
              </pre>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Generate Content */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Sparkles className="h-5 w-5 text-primary-600" />
            Generate Content
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex items-center gap-3">
            <Select
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
                  <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                  Generating...
                </>
              ) : (
                <>
                  <Sparkles className="h-4 w-4 mr-2" />
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
                  key={String(rt.id)}
                  href={`/topics/${rt.id}`}
                  className="flex items-center justify-between p-3 rounded-lg border border-gray-100 hover:bg-gray-50 transition-colors"
                >
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium text-gray-900 truncate">
                      {String(rt.title)}
                    </p>
                    <div className="flex items-center gap-2 mt-1">
                      {rt.source_type && (
                        <Badge variant="secondary" className="text-xs">
                          {String(rt.source_type)}
                        </Badge>
                      )}
                    </div>
                  </div>
                  <ExternalLink className="h-3 w-3 text-gray-400 flex-shrink-0" />
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
      <div className="grid grid-cols-3 gap-4">
        {[1, 2, 3].map((i) => (
          <Skeleton key={i} className="h-20" />
        ))}
      </div>
      <Skeleton className="h-40" />
      <Skeleton className="h-60" />
    </div>
  );
}
