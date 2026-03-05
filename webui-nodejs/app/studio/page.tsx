"use client";

import { Suspense, useEffect, useState, useCallback } from "react";
import { useSearchParams } from "next/navigation";
import {
  fetchUserContent,
  uploadContent,
  importContentFromUrl,
  deleteUserContent,
  analyzeStyle,
  fetchStyleProfile,
  fetchGeneratedContent,
  fetchGeneratedContentDetail,
  regenerateContent,
  exportMarkdown,
  type UserContent,
  type GeneratedContent,
} from "@/lib/v2api";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Badge } from "@/components/ui/badge";
import { Select } from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import { Tabs, TabsList, TabsTrigger, TabsContent } from "@/components/ui/tabs";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogClose,
} from "@/components/ui/dialog";
import { formatDate, truncate } from "@/lib/utils";
import {
  Upload,
  Link2,
  Sparkles,
  Palette,
  FileText,
  Trash2,
  RefreshCw,
  Download,
  Loader2,
  Eye,
  MessageSquare,
  AlertTriangle,
} from "lucide-react";

// Wrap in Suspense for useSearchParams (Next.js 14 requirement)
export default function StudioPageWrapper() {
  return (
    <Suspense fallback={<StudioSkeleton />}>
      <ContentStudioPage />
    </Suspense>
  );
}

function ContentStudioPage() {
  const searchParams = useSearchParams();
  const generatedHighlight = searchParams?.get("generated");

  const [tab, setTab] = useState("content");
  const [userContent, setUserContent] = useState<UserContent[]>([]);
  const [generated, setGenerated] = useState<GeneratedContent[]>([]);
  const [styleProfile, setStyleProfile] = useState<Record<string, string> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Upload dialog
  const [uploadOpen, setUploadOpen] = useState(false);
  const [uploadMode, setUploadMode] = useState<"paste" | "url">("paste");
  const [uploadTitle, setUploadTitle] = useState("");
  const [uploadText, setUploadText] = useState("");
  const [uploadUrl, setUploadUrl] = useState("");
  const [uploadType, setUploadType] = useState("blog_post");
  const [uploading, setUploading] = useState(false);

  // View generated content
  const [viewOpen, setViewOpen] = useState(false);
  const [viewContent, setViewContent] = useState<GeneratedContent | null>(null);
  const [feedback, setFeedback] = useState("");
  const [regenerating, setRegenerating] = useState(false);

  const loadData = useCallback(async () => {
    setError(null);
    try {
      const [uc, gen, sp] = await Promise.all([
        fetchUserContent(),
        fetchGeneratedContent(),
        fetchStyleProfile().catch(() => null),
      ]);
      setUserContent(uc);
      setGenerated(gen);
      setStyleProfile(sp);
    } catch (e) {
      setError("Failed to load studio data");
      console.error("Failed to load studio data", e);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData]);

  useEffect(() => {
    if (generatedHighlight) {
      setTab("generated");
    }
  }, [generatedHighlight]);

  async function handleUpload() {
    setUploading(true);
    try {
      if (uploadMode === "url") {
        await importContentFromUrl(uploadUrl);
      } else {
        await uploadContent({
          title: uploadTitle,
          content: uploadText,
          contentType: uploadType,
        });
      }
      setUploadOpen(false);
      setUploadTitle("");
      setUploadText("");
      setUploadUrl("");
      await loadData();
    } catch (e) {
      console.error("Upload failed", e);
    } finally {
      setUploading(false);
    }
  }

  async function handleAnalyze(contentId: number) {
    try {
      await analyzeStyle(contentId);
      await loadData();
    } catch (e) {
      console.error("Analysis failed", e);
    }
  }

  async function handleDeleteContent(id: number) {
    if (!confirm("Delete this content?")) return;
    try {
      await deleteUserContent(id);
      await loadData();
    } catch (e) {
      console.error("Delete failed", e);
    }
  }

  async function openGenerated(id: number) {
    try {
      const detail = await fetchGeneratedContentDetail(id);
      setViewContent(detail);
      setFeedback("");
      setViewOpen(true);
    } catch (e) {
      console.error("Failed to load generated content", e);
    }
  }

  async function handleRegenerate() {
    if (!viewContent || !feedback.trim()) return;
    setRegenerating(true);
    try {
      const result = await regenerateContent(viewContent.id, feedback);
      if (result?.id) {
        await openGenerated(result.id);
        await loadData();
      }
    } catch (e) {
      console.error("Regeneration failed", e);
    } finally {
      setRegenerating(false);
    }
  }

  async function handleExport(id: number) {
    try {
      const md = await exportMarkdown(id);
      const blob = new Blob([md], { type: "text/markdown" });
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `generated-${id}.md`;
      a.click();
      URL.revokeObjectURL(url);
    } catch (e) {
      console.error("Export failed", e);
    }
  }

  if (loading) {
    return <StudioSkeleton />;
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
          <h1 className="text-2xl font-bold text-gray-900">Content Studio</h1>
          <p className="text-sm text-gray-500 mt-1">
            Upload content to learn your style, then generate new content
          </p>
        </div>
        <Button onClick={() => setUploadOpen(true)}>
          <Upload className="h-4 w-4 mr-2" aria-hidden="true" />
          Upload Content
        </Button>
      </div>

      {/* Style Profile Card */}
      {styleProfile && (
        <Card className="border-primary-200 bg-primary-50/30">
          <CardContent className="p-4">
            <div className="flex items-center gap-2 mb-2">
              <Palette className="h-4 w-4 text-primary-600" aria-hidden="true" />
              <h3 className="text-sm font-semibold text-primary-900">
                Your Writing Style Profile
              </h3>
            </div>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-3 text-xs">
              {Object.entries(styleProfile).map(([key, value]) => (
                <div key={key}>
                  <span className="text-gray-500 capitalize">
                    {key.replace(/_/g, " ")}
                  </span>
                  <p className="font-medium text-gray-800 mt-0.5">
                    {value}
                  </p>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      )}

      {/* Tabs */}
      <Tabs value={tab} onValueChange={setTab}>
        <TabsList>
          <TabsTrigger value="content">
            Your Content ({userContent.length})
          </TabsTrigger>
          <TabsTrigger value="generated">
            Generated ({generated.length})
          </TabsTrigger>
        </TabsList>

        {/* Your Content Tab */}
        <TabsContent value="content">
          {userContent.length === 0 ? (
            <Card>
              <CardContent className="p-12 text-center">
                <Upload className="h-10 w-10 mx-auto text-gray-300 mb-3" aria-hidden="true" />
                <p className="text-gray-500">
                  Upload your existing content to teach the AI your writing style
                </p>
                <Button
                  variant="outline"
                  className="mt-3"
                  onClick={() => setUploadOpen(true)}
                >
                  Upload Content
                </Button>
              </CardContent>
            </Card>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {userContent.map((uc) => (
                <Card key={uc.id}>
                  <CardContent className="p-4">
                    <div className="flex items-start justify-between mb-2">
                      <div className="flex-1 min-w-0">
                        <h4 className="text-sm font-medium text-gray-900 truncate">
                          {uc.title || "Untitled"}
                        </h4>
                        <div className="flex items-center gap-2 mt-1">
                          <Badge variant="secondary" className="text-xs">
                            {uc.content_type || "unknown"}
                          </Badge>
                          {uc.style_analyzed ? (
                            <Badge variant="success" className="text-xs">
                              Style Analyzed
                            </Badge>
                          ) : (
                            <Badge variant="warning" className="text-xs">
                              Not Analyzed
                            </Badge>
                          )}
                        </div>
                      </div>
                      <Button
                        variant="ghost"
                        size="icon"
                        aria-label={`Delete ${uc.title || "content"}`}
                        onClick={() => handleDeleteContent(uc.id)}
                      >
                        <Trash2 className="h-3.5 w-3.5 text-red-500" />
                      </Button>
                    </div>
                    {uc.content && (
                      <p className="text-xs text-gray-500 line-clamp-3 mb-3">
                        {truncate(uc.content, 200)}
                      </p>
                    )}
                    <div className="flex items-center gap-2">
                      {!uc.style_analyzed && (
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => handleAnalyze(uc.id)}
                        >
                          <Sparkles className="h-3 w-3 mr-1" aria-hidden="true" />
                          Analyze Style
                        </Button>
                      )}
                      {uc.created_at && (
                        <span className="text-xs text-gray-400 ml-auto">
                          {formatDate(uc.created_at)}
                        </span>
                      )}
                    </div>
                  </CardContent>
                </Card>
              ))}
            </div>
          )}
        </TabsContent>

        {/* Generated Content Tab */}
        <TabsContent value="generated">
          {generated.length === 0 ? (
            <Card>
              <CardContent className="p-12 text-center">
                <FileText className="h-10 w-10 mx-auto text-gray-300 mb-3" aria-hidden="true" />
                <p className="text-gray-500">
                  No generated content yet. Go to a topic and click Generate.
                </p>
              </CardContent>
            </Card>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {generated.map((g) => (
                <Card
                  key={g.id}
                  className={
                    generatedHighlight === String(g.id)
                      ? "ring-2 ring-primary-500"
                      : ""
                  }
                >
                  <CardContent className="p-4">
                    <div className="flex items-start justify-between mb-2">
                      <div>
                        <Badge variant="default" className="text-xs">
                          {(g.output_format || "unknown").replace(/_/g, " ")}
                        </Badge>
                        {g.model_used && (
                          <Badge
                            variant="secondary"
                            className="text-xs ml-1"
                          >
                            {g.model_used}
                          </Badge>
                        )}
                      </div>
                      <div className="flex items-center gap-1">
                        <Button
                          variant="ghost"
                          size="icon"
                          aria-label="View content"
                          onClick={() => openGenerated(g.id)}
                        >
                          <Eye className="h-3.5 w-3.5" />
                        </Button>
                        <Button
                          variant="ghost"
                          size="icon"
                          aria-label="Export as markdown"
                          onClick={() => handleExport(g.id)}
                        >
                          <Download className="h-3.5 w-3.5" />
                        </Button>
                      </div>
                    </div>
                    {g.generated_text && (
                      <p className="text-xs text-gray-500 line-clamp-4">
                        {truncate(g.generated_text, 250)}
                      </p>
                    )}
                    {g.created_at && (
                      <p className="text-xs text-gray-400 mt-2">
                        {formatDate(g.created_at)}
                      </p>
                    )}
                  </CardContent>
                </Card>
              ))}
            </div>
          )}
        </TabsContent>
      </Tabs>

      {/* Upload Dialog */}
      <Dialog open={uploadOpen} onClose={() => setUploadOpen(false)}>
        <DialogContent className="max-w-xl">
          <DialogClose onClose={() => setUploadOpen(false)} />
          <DialogHeader>
            <DialogTitle>Upload Content</DialogTitle>
          </DialogHeader>

          <Tabs
            value={uploadMode}
            onValueChange={(v) => setUploadMode(v as "paste" | "url")}
          >
            <TabsList className="w-full">
              <TabsTrigger value="paste" className="flex-1">
                <FileText className="h-3 w-3 mr-1" aria-hidden="true" />
                Paste Text
              </TabsTrigger>
              <TabsTrigger value="url" className="flex-1">
                <Link2 className="h-3 w-3 mr-1" aria-hidden="true" />
                Import URL
              </TabsTrigger>
            </TabsList>

            <TabsContent value="paste">
              <div className="space-y-3">
                <div>
                  <label htmlFor="upload-title" className="text-sm font-medium text-gray-700">
                    Title
                  </label>
                  <Input
                    id="upload-title"
                    value={uploadTitle}
                    onChange={(e) => setUploadTitle(e.target.value)}
                    placeholder="Article title"
                    className="mt-1"
                  />
                </div>
                <div>
                  <label htmlFor="upload-type" className="text-sm font-medium text-gray-700">
                    Content Type
                  </label>
                  <Select
                    id="upload-type"
                    value={uploadType}
                    onChange={(e) => setUploadType(e.target.value)}
                    className="mt-1"
                  >
                    <option value="blog_post">Blog Post</option>
                    <option value="youtube_script">YouTube Script</option>
                    <option value="linkedin_post">LinkedIn Post</option>
                    <option value="newsletter">Newsletter</option>
                  </Select>
                </div>
                <div>
                  <label htmlFor="upload-content" className="text-sm font-medium text-gray-700">
                    Content
                  </label>
                  <Textarea
                    id="upload-content"
                    value={uploadText}
                    onChange={(e) => setUploadText(e.target.value)}
                    placeholder="Paste your content here..."
                    rows={8}
                    className="mt-1"
                  />
                </div>
              </div>
            </TabsContent>

            <TabsContent value="url">
              <div>
                <label htmlFor="upload-url" className="text-sm font-medium text-gray-700">
                  URL
                </label>
                <Input
                  id="upload-url"
                  value={uploadUrl}
                  onChange={(e) => setUploadUrl(e.target.value)}
                  placeholder="https://medium.com/your-article"
                  className="mt-1"
                />
                <p className="text-xs text-gray-400 mt-1">
                  Content will be extracted automatically from the URL
                </p>
              </div>
            </TabsContent>
          </Tabs>

          <div className="flex justify-end gap-2 mt-4">
            <Button variant="outline" onClick={() => setUploadOpen(false)}>
              Cancel
            </Button>
            <Button
              onClick={handleUpload}
              disabled={
                uploading ||
                (uploadMode === "paste"
                  ? !uploadTitle.trim() || !uploadText.trim()
                  : !uploadUrl.trim())
              }
            >
              {uploading ? (
                <Loader2 className="h-4 w-4 mr-2 animate-spin" aria-hidden="true" />
              ) : (
                <Upload className="h-4 w-4 mr-2" aria-hidden="true" />
              )}
              Upload
            </Button>
          </div>
        </DialogContent>
      </Dialog>

      {/* View Generated Content Dialog */}
      <Dialog open={viewOpen} onClose={() => setViewOpen(false)}>
        <DialogContent className="max-w-2xl max-h-[80vh] overflow-y-auto">
          <DialogClose onClose={() => setViewOpen(false)} />
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <FileText className="h-5 w-5" aria-hidden="true" />
              Generated Content
              {viewContent?.output_format && (
                <Badge variant="default" className="text-xs ml-2">
                  {viewContent.output_format.replace(/_/g, " ")}
                </Badge>
              )}
            </DialogTitle>
          </DialogHeader>

          {viewContent && (
            <div className="space-y-4">
              <pre className="whitespace-pre-wrap text-sm font-sans bg-gray-50 rounded-lg p-4 max-h-[400px] overflow-y-auto text-gray-700">
                {viewContent.generated_text || ""}
              </pre>

              {/* Regenerate with feedback */}
              <div className="border-t border-gray-200 pt-4">
                <label htmlFor="regen-feedback" className="text-sm font-medium text-gray-700 flex items-center gap-1 mb-2">
                  <MessageSquare className="h-4 w-4" aria-hidden="true" />
                  Regenerate with Feedback
                </label>
                <div className="flex gap-2">
                  <Textarea
                    id="regen-feedback"
                    value={feedback}
                    onChange={(e) => setFeedback(e.target.value)}
                    placeholder="e.g. make it more technical, add code examples, shorter intro..."
                    rows={2}
                    className="flex-1"
                  />
                  <Button
                    onClick={handleRegenerate}
                    disabled={!feedback.trim() || regenerating}
                    aria-label="Regenerate"
                    className="self-end"
                  >
                    {regenerating ? (
                      <Loader2 className="h-4 w-4 animate-spin" />
                    ) : (
                      <RefreshCw className="h-4 w-4" />
                    )}
                  </Button>
                </div>
              </div>

              <div className="flex justify-end gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => handleExport(viewContent.id)}
                >
                  <Download className="h-4 w-4 mr-1" aria-hidden="true" />
                  Export Markdown
                </Button>
              </div>
            </div>
          )}
        </DialogContent>
      </Dialog>
    </div>
  );
}

function StudioSkeleton() {
  return (
    <div className="space-y-6">
      <Skeleton className="h-8 w-48" />
      <Skeleton className="h-10 w-80" />
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {[1, 2, 3, 4].map((i) => (
          <Skeleton key={i} className="h-32" />
        ))}
      </div>
    </div>
  );
}
