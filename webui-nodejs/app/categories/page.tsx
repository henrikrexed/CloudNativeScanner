"use client";

import { useEffect, useState, useCallback } from "react";
import {
  fetchCategories,
  createCategory,
  updateCategory,
  deleteCategory,
  fetchScanners,
  type Category,
  type ScannerInfo,
} from "@/lib/v2api";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogClose,
} from "@/components/ui/dialog";
import { FolderOpen, Plus, Pencil, Trash2, Loader2, Hash, Ban, AlertTriangle } from "lucide-react";

export default function CategoriesPage() {
  const [categories, setCategories] = useState<Category[]>([]);
  const [scanners, setScanners] = useState<ScannerInfo[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<Category | null>(null);
  const [saving, setSaving] = useState(false);

  // Form state
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [keywords, setKeywords] = useState("");
  const [negativeKeywords, setNegativeKeywords] = useState("");
  const [sourceTypes, setSourceTypes] = useState("");
  const [scanInterval, setScanInterval] = useState("60");

  const loadData = useCallback(async () => {
    setError(null);
    try {
      const [cats, scans] = await Promise.all([
        fetchCategories(),
        fetchScanners(),
      ]);
      setCategories(cats);
      setScanners(scans);
    } catch (e) {
      setError("Failed to load categories");
      console.error("Failed to load categories", e);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData]);

  function openCreate() {
    setEditing(null);
    setName("");
    setDescription("");
    setKeywords("");
    setNegativeKeywords("");
    setSourceTypes("");
    setScanInterval("60");
    setDialogOpen(true);
  }

  function openEdit(cat: Category) {
    setEditing(cat);
    setName(cat.name);
    setDescription(cat.description || "");
    setKeywords(cat.keywords || "");
    setNegativeKeywords(cat.negative_keywords || "");
    setSourceTypes(cat.source_types || "");
    setScanInterval(String(cat.scan_interval_minutes || 60));
    setDialogOpen(true);
  }

  async function handleSave() {
    setSaving(true);
    try {
      const body = {
        name,
        description,
        keywords,
        negative_keywords: negativeKeywords,
        source_types: sourceTypes,
        scan_interval_minutes: Number(scanInterval),
      };

      if (editing) {
        await updateCategory(editing.id, body);
      } else {
        await createCategory(body);
      }
      setDialogOpen(false);
      await loadData();
    } catch (e) {
      console.error("Failed to save category", e);
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete(id: number) {
    if (!confirm("Delete this category? Topics won't be removed, but they'll be unlinked.")) {
      return;
    }
    try {
      await deleteCategory(id);
      await loadData();
    } catch (e) {
      console.error("Failed to delete category", e);
    }
  }

  if (loading) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-8 w-48" />
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {[1, 2, 3].map((i) => (
            <Skeleton key={i} className="h-48" />
          ))}
        </div>
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
          <h1 className="text-2xl font-bold text-gray-900">Categories</h1>
          <p className="text-sm text-gray-500 mt-1">
            Manage scanning categories with keywords and filters
          </p>
        </div>
        <Button onClick={openCreate}>
          <Plus className="h-4 w-4 mr-2" />
          New Category
        </Button>
      </div>

      {/* Category Grid */}
      {categories.length === 0 ? (
        <Card>
          <CardContent className="p-12 text-center">
            <FolderOpen className="h-10 w-10 mx-auto text-gray-300 mb-3" />
            <p className="text-gray-500">No categories yet</p>
            <Button variant="outline" className="mt-3" onClick={openCreate}>
              Create your first category
            </Button>
          </CardContent>
        </Card>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {categories.map((cat) => (
            <CategoryCard
              key={cat.id}
              category={cat}
              onEdit={() => openEdit(cat)}
              onDelete={() => handleDelete(cat.id)}
            />
          ))}
        </div>
      )}

      {/* Create/Edit Dialog */}
      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)}>
        <DialogContent>
          <DialogClose onClose={() => setDialogOpen(false)} />
          <DialogHeader>
            <DialogTitle>
              {editing ? "Edit Category" : "New Category"}
            </DialogTitle>
          </DialogHeader>

          <div className="space-y-4">
            <div>
              <label htmlFor="cat-name" className="text-sm font-medium text-gray-700">Name</label>
              <Input
                id="cat-name"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="e.g. Cloud Native"
                className="mt-1"
              />
            </div>
            <div>
              <label htmlFor="cat-desc" className="text-sm font-medium text-gray-700">
                Description
              </label>
              <Textarea
                id="cat-desc"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="What topics should this category capture?"
                rows={2}
                className="mt-1"
              />
            </div>
            <div>
              <label className="text-sm font-medium text-gray-700 flex items-center gap-1">
                <Hash className="h-3 w-3" />
                Keywords
              </label>
              <Textarea
                value={keywords}
                onChange={(e) => setKeywords(e.target.value)}
                placeholder="kubernetes, docker, service mesh (comma-separated)"
                rows={2}
                className="mt-1"
              />
              <p className="text-xs text-gray-400 mt-1">
                Comma-separated keywords for scanner search queries
              </p>
            </div>
            <div>
              <label className="text-sm font-medium text-gray-700 flex items-center gap-1">
                <Ban className="h-3 w-3" />
                Negative Keywords
              </label>
              <Textarea
                value={negativeKeywords}
                onChange={(e) => setNegativeKeywords(e.target.value)}
                placeholder="spam, ads, off-topic (comma-separated)"
                rows={2}
                className="mt-1"
              />
              <p className="text-xs text-gray-400 mt-1">
                Topics containing these words will be filtered out
              </p>
            </div>
            <div>
              <label className="text-sm font-medium text-gray-700">
                Source Types
              </label>
              <Input
                value={sourceTypes}
                onChange={(e) => setSourceTypes(e.target.value)}
                placeholder="medium, reddit, stackoverflow"
                className="mt-1"
              />
              <p className="text-xs text-gray-400 mt-1">
                Available: {scanners.map((s) => s.type).join(", ") || "none loaded"}
              </p>
            </div>
            <div>
              <label className="text-sm font-medium text-gray-700">
                Scan Interval (minutes)
              </label>
              <Input
                type="number"
                min="5"
                value={scanInterval}
                onChange={(e) => setScanInterval(e.target.value)}
                className="mt-1 w-32"
              />
            </div>

            <div className="flex justify-end gap-2 pt-2">
              <Button
                variant="outline"
                onClick={() => setDialogOpen(false)}
              >
                Cancel
              </Button>
              <Button onClick={handleSave} disabled={!name.trim() || saving}>
                {saving ? (
                  <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                ) : null}
                {editing ? "Update" : "Create"}
              </Button>
            </div>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );
}

function CategoryCard({
  category,
  onEdit,
  onDelete,
}: {
  category: Category;
  onEdit: () => void;
  onDelete: () => void;
}) {
  const keywords = category.keywords
    ? category.keywords.split(",").map((k) => k.trim()).filter(Boolean)
    : [];
  const negKeywords = category.negative_keywords
    ? category.negative_keywords.split(",").map((k) => k.trim()).filter(Boolean)
    : [];

  return (
    <Card className="flex flex-col">
      <CardHeader className="pb-3">
        <div className="flex items-start justify-between">
          <div className="flex items-center gap-2">
            <FolderOpen className="h-5 w-5 text-primary-600" />
            <CardTitle className="text-base">{category.name}</CardTitle>
          </div>
          <div className="flex items-center gap-1">
            <Button variant="ghost" size="icon" onClick={onEdit} aria-label={`Edit ${category.name}`}>
              <Pencil className="h-3.5 w-3.5" />
            </Button>
            <Button variant="ghost" size="icon" onClick={onDelete} aria-label={`Delete ${category.name}`}>
              <Trash2 className="h-3.5 w-3.5 text-red-500" />
            </Button>
          </div>
        </div>
      </CardHeader>
      <CardContent className="flex-1 space-y-3">
        {category.description && (
          <p className="text-xs text-gray-500 line-clamp-2">
            {category.description}
          </p>
        )}

        {keywords.length > 0 && (
          <div>
            <p className="text-xs font-medium text-gray-500 mb-1">Keywords</p>
            <div className="flex flex-wrap gap-1">
              {keywords.slice(0, 6).map((k) => (
                <Badge key={k} variant="default" className="text-xs">
                  {k}
                </Badge>
              ))}
              {keywords.length > 6 && (
                <Badge variant="secondary" className="text-xs">
                  +{keywords.length - 6}
                </Badge>
              )}
            </div>
          </div>
        )}

        {negKeywords.length > 0 && (
          <div>
            <p className="text-xs font-medium text-gray-500 mb-1">
              Negative Keywords
            </p>
            <div className="flex flex-wrap gap-1">
              {negKeywords.slice(0, 4).map((k) => (
                <Badge key={k} variant="destructive" className="text-xs">
                  {k}
                </Badge>
              ))}
              {negKeywords.length > 4 && (
                <Badge variant="secondary" className="text-xs">
                  +{negKeywords.length - 4}
                </Badge>
              )}
            </div>
          </div>
        )}

        <div className="flex items-center justify-between pt-2 border-t border-gray-100 text-xs text-gray-400">
          <span>{category.topic_count ?? 0} topics</span>
          {category.source_types && (
            <span>{category.source_types}</span>
          )}
        </div>
      </CardContent>
    </Card>
  );
}
