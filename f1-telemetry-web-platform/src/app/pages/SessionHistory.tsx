import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router";
import { DataCard } from "../components/DataCard";
import { Button } from "../components/ui/button";
import { Input } from "../components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "../components/ui/select";
import { Search, Eye, Filter, Calendar, Pencil, Loader2 } from "lucide-react";
import { getSessions, updateSessionDisplayName } from "@/api/client";
import type { Session } from "@/api/types";
import { HttpError } from "@/api/types";
import { getTrackName } from "@/constants/tracks";
import { formatLapTime } from "@/api/format";
import { isValidSessionId } from "@/api/sessionId";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "../components/ui/dialog";
import { notify } from "@/notify";
import { Skeleton } from "../components/ui/skeleton";

const PAGE_SIZE = 20;

function resultDisplay(position: number | null | undefined): string {
  if (position == null) return "—";
  return `P${position}`;
}

export default function SessionHistory() {
  const [searchQuery, setSearchQuery] = useState("");
  const [sessions, setSessions] = useState<Session[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [offset, setOffset] = useState(0);
  const [pageSize] = useState(PAGE_SIZE);

  // Edit display name dialog
  const [editDialogOpen, setEditDialogOpen] = useState(false);
  const [editSession, setEditSession] = useState<Session | null>(null);
  const [editName, setEditName] = useState("");
  const [editSubmitting, setEditSubmitting] = useState(false);

  const fetchSessions = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await getSessions({ limit: pageSize, offset });
      setSessions(data);
    } catch (e) {
      const message =
        e instanceof HttpError ? e.message : "Failed to load sessions";
      setError(message);
      setSessions([]);
    } finally {
      setLoading(false);
    }
  }, [offset, pageSize]);

  useEffect(() => {
    fetchSessions();
  }, [fetchSessions]);

  const filteredSessions = sessions.filter(
    (session) =>
      getTrackName(session.trackId)
        .toLowerCase()
        .includes(searchQuery.toLowerCase()) ||
      (session.sessionType ?? "")
        .toLowerCase()
        .includes(searchQuery.toLowerCase()) ||
      (session.sessionDisplayName ?? "")
        .toLowerCase()
        .includes(searchQuery.toLowerCase())
  );

  const handleEditClick = (session: Session) => {
    setEditSession(session);
    setEditName(session.sessionDisplayName ?? session.id ?? "");
    setEditDialogOpen(true);
  };

  const handleEditSubmit = async () => {
    if (!editSession) return;
    const trimmed = editName.trim();
    if (!trimmed) {
      notify.warning("Name cannot be blank");
      return;
    }
    if (trimmed.length > 64) {
      notify.warning("Name must be 64 characters or less");
      return;
    }
    setEditSubmitting(true);
    try {
      await updateSessionDisplayName(editSession.id, trimmed);
      notify.success("Display name updated");
      setEditDialogOpen(false);
      setEditSession(null);
      fetchSessions();
    } catch {
      // toast already shown by client
    } finally {
      setEditSubmitting(false);
    }
  };

  const start = offset + 1;
  const end = offset + sessions.length;
  const hasNext = sessions.length >= pageSize;
  const hasPrev = offset > 0;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold mb-2">Session History</h1>
        <p className="text-text-secondary">
          View and analyze your previous racing sessions
        </p>
      </div>

      {/* Filters */}
      <DataCard>
        <div className="grid md:grid-cols-4 gap-4">
          <div className="md:col-span-2">
            <label className="text-xs text-text-secondary uppercase mb-2 block">
              Search
            </label>
            <div className="relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-text-secondary" />
              <Input
                placeholder="Search by track or session type..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="pl-10 bg-input-background border-border"
              />
            </div>
          </div>

          <div>
            <label className="text-xs text-text-secondary uppercase mb-2 block">
              Session Type
            </label>
            <Select defaultValue="all">
              <SelectTrigger className="bg-input-background border-border">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">All Types</SelectItem>
                <SelectItem value="race">Race</SelectItem>
                <SelectItem value="qualifying">Qualifying</SelectItem>
                <SelectItem value="practice">Practice</SelectItem>
                <SelectItem value="sprint">Sprint</SelectItem>
              </SelectContent>
            </Select>
          </div>

          <div>
            <label className="text-xs text-text-secondary uppercase mb-2 block">
              Sort By
            </label>
            <Select defaultValue="date">
              <SelectTrigger className="bg-input-background border-border">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="date">Date (Newest)</SelectItem>
                <SelectItem value="date-old">Date (Oldest)</SelectItem>
                <SelectItem value="result">Result</SelectItem>
                <SelectItem value="lap">Best Lap</SelectItem>
              </SelectContent>
            </Select>
          </div>
        </div>

        <div className="flex gap-2 mt-4">
          <Button variant="outline" size="sm" className="gap-2">
            <Calendar className="w-4 h-4" />
            Date Range
          </Button>
          <Button variant="outline" size="sm" className="gap-2">
            <Filter className="w-4 h-4" />
            More Filters
          </Button>
          <Button variant="ghost" size="sm" className="text-text-secondary hover:text-foreground">
            Reset
          </Button>
        </div>
      </DataCard>

      {/* Sessions Table */}
      <DataCard noPadding>
        {loading ? (
          <div className="p-6 space-y-4">
            <Skeleton className="h-10 w-full" />
            <Skeleton className="h-64 w-full" />
          </div>
        ) : error ? (
          <div className="p-12 text-center">
            <p className="text-destructive mb-4">{error}</p>
            <Button onClick={fetchSessions} variant="outline">
              Retry
            </Button>
          </div>
        ) : !loading && !error && sessions.length === 0 ? (
          <div className="p-12 text-center">
            <div className="w-16 h-16 bg-secondary/50 rounded-full flex items-center justify-center mx-auto mb-4">
              <Search className="w-8 h-8 text-text-secondary" />
            </div>
            <h3 className="text-lg font-semibold mb-2">No sessions found</h3>
            <p className="text-text-secondary">
              Sessions will appear here once you have completed them
            </p>
          </div>
        ) : (
          <>
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead className="bg-secondary/50 border-b border-border/50">
                  <tr>
                    <th className="px-4 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                      Track
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                      Date
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                      Session Type
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                      Best Lap
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                      Total Time
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                      Result
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                      Actions
                    </th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-border/30">
                  {filteredSessions.map((session) => {
                    const id = session.id;
                    const validId = isValidSessionId(id);
                    return (
                      <tr
                        key={id}
                        className="hover:bg-secondary/30 transition-colors"
                      >
                        <td className="px-4 py-4 font-medium">
                          {session.trackDisplayName ??
                            getTrackName(session.trackId)}
                        </td>
                        <td className="px-4 py-4 text-text-secondary">
                          {session.startedAt
                            ? new Date(session.startedAt).toLocaleDateString(
                                "en-US",
                                {
                                  month: "short",
                                  day: "numeric",
                                  year: "numeric",
                                }
                              )
                            : "—"}
                        </td>
                        <td className="px-4 py-4">
                          <span className="inline-flex px-2 py-1 rounded text-xs bg-secondary/50 border border-border">
                            {session.sessionType ?? "—"}
                          </span>
                        </td>
                        <td className="px-4 py-4 font-mono text-[#00E5FF]">
                          {session.bestLapTimeMs != null
                            ? formatLapTime(session.bestLapTimeMs)
                            : "—"}
                        </td>
                        <td className="px-4 py-4 font-mono text-text-secondary">
                          {session.totalTimeMs != null
                            ? formatLapTime(session.totalTimeMs)
                            : "—"}
                        </td>
                        <td className="px-4 py-4">
                          <span
                            className={`font-bold ${
                              session.finishingPosition === 1
                                ? "text-[#00FF85]"
                                : session.finishingPosition === 2 ||
                                    session.finishingPosition === 3
                                  ? "text-[#00E5FF]"
                                  : "text-text-secondary"
                            }`}
                          >
                            {resultDisplay(session.finishingPosition)}
                          </span>
                        </td>
                        <td className="px-4 py-4 flex items-center gap-2">
                          {validId && (
                            <Link to={`/app/sessions/${id}`}>
                              <Button
                                variant="ghost"
                                size="sm"
                                className="h-8 gap-2"
                              >
                                <Eye className="w-4 h-4" />
                                View
                              </Button>
                            </Link>
                          )}
                          <Button
                            variant="ghost"
                            size="sm"
                            className="h-8 gap-2"
                            onClick={() => handleEditClick(session)}
                          >
                            <Pencil className="w-4 h-4" />
                            Edit
                          </Button>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>

            {sessions.length > 0 && (
              <div className="p-4 border-t border-border/50 flex items-center justify-between">
                <div className="text-sm text-text-secondary">
                  Showing {start}–{end}
                </div>
                <div className="flex gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={!hasPrev}
                    onClick={() => setOffset((o) => Math.max(0, o - pageSize))}
                  >
                    Previous
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={!hasNext}
                    onClick={() => setOffset((o) => o + pageSize)}
                  >
                    Next
                  </Button>
                </div>
              </div>
            )}
          </>
        )}
      </DataCard>

      {/* Edit display name dialog */}
      <Dialog open={editDialogOpen} onOpenChange={setEditDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Edit session name</DialogTitle>
          </DialogHeader>
          <div className="py-2">
            <label className="text-sm text-text-secondary block mb-2">
              Display name (max 64 characters)
            </label>
            <Input
              value={editName}
              onChange={(e) => setEditName(e.target.value.slice(0, 64))}
              maxLength={64}
              placeholder="Session name"
              className="bg-input-background border-border"
            />
          </div>
          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => setEditDialogOpen(false)}
              disabled={editSubmitting}
            >
              Cancel
            </Button>
            <Button
              onClick={handleEditSubmit}
              disabled={editSubmitting || !editName.trim()}
              className="gap-2"
            >
              {editSubmitting && (
                <Loader2 className="w-4 h-4 animate-spin" />
              )}
              Save
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
