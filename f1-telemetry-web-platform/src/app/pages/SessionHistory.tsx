import { useCallback, useEffect, useRef, useState } from "react"
import { Link } from "react-router"
import { DataCard } from "../components/DataCard"
import { Button } from "../components/ui/button"
import { Input } from "../components/ui/input"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "../components/ui/select"
import { Search, Eye, Calendar, Pencil, Loader2 } from "lucide-react"
import { getSessions, updateSessionDisplayName } from "@/api/client"
import type { Session } from "@/api/types"
import { HttpError } from "@/api/types"
import { getTrackName, TRACK_OPTIONS } from "@/constants/tracks"
import { SESSION_TYPE_OPTIONS } from "@/constants/sessionTypes"
import { formatLapTime } from "@/api/format"
import { isValidSessionId } from "@/api/sessionId"
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "../components/ui/dialog"
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "../components/ui/popover"
import { Calendar as CalendarComponent } from "../components/ui/calendar"
import { notify } from "@/notify"
import { Skeleton } from "../components/ui/skeleton"

const PAGE_SIZE = 50
const DEFAULT_SORT = "startedAt_desc"

const SORT_OPTIONS: { value: string; label: string }[] = [
  { value: "startedAt_desc", label: "Date (Newest)" },
  { value: "startedAt_asc", label: "Date (Oldest)" },
  { value: "finishingPosition_asc", label: "Result" },
  { value: "bestLap_asc", label: "Best Lap" },
]

const STATE_OPTIONS: { value: string; label: string }[] = [
  { value: "", label: "All" },
  { value: "ACTIVE", label: "Active" },
  { value: "FINISHED", label: "Finished" },
]

/** Format date as YYYY-MM-DD in local calendar time (not UTC) for API filters. */
function formatDateForApi(d: Date): string {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, "0")
  const day = String(d.getDate()).padStart(2, "0")
  return `${y}-${m}-${day}`
}

function resultDisplay(position: number | null | undefined): string {
  if (position == null) return "—"
  return `P${position}`
}

export default function SessionHistory() {
  const [search, setSearch] = useState("")
  const [sessionType, setSessionType] = useState<string>("")
  const [trackId, setTrackId] = useState<number | "">("")
  const [sort, setSort] = useState(DEFAULT_SORT)
  const [state, setState] = useState("")
  const [dateFrom, setDateFrom] = useState<string>("")
  const [dateTo, setDateTo] = useState<string>("")

  const [sessions, setSessions] = useState<Session[]>([])
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [offset, setOffset] = useState(0)
  const [pageSize] = useState(PAGE_SIZE)

  const searchDebounceRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const [searchDebounced, setSearchDebounced] = useState("")

  const [editDialogOpen, setEditDialogOpen] = useState(false)
  const [editSession, setEditSession] = useState<Session | null>(null)
  const [editName, setEditName] = useState("")
  const [editSubmitting, setEditSubmitting] = useState(false)

  const fetchSessions = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const result = await getSessions({
        limit: pageSize,
        offset,
        sessionType: sessionType || undefined,
        trackId: trackId === "" ? undefined : trackId,
        search: searchDebounced || undefined,
        sort,
        state: state || undefined,
        dateFrom: dateFrom || undefined,
        dateTo: dateTo || undefined,
      })
      setSessions(result.sessions)
      setTotal(result.total)
    } catch (e) {
      const message =
        e instanceof HttpError ? e.message : "Failed to load sessions"
      setError(message)
      setSessions([])
      setTotal(0)
      notify.error(message)
    } finally {
      setLoading(false)
    }
  }, [
    offset,
    pageSize,
    sessionType,
    trackId,
    searchDebounced,
    sort,
    state,
    dateFrom,
    dateTo,
  ])

  useEffect(() => {
    const t = setTimeout(() => {
      setSearchDebounced(search)
      setOffset(0)
    }, 400)
    searchDebounceRef.current = t
    return () => {
      if (searchDebounceRef.current) clearTimeout(searchDebounceRef.current)
    }
  }, [search])

  useEffect(() => {
    fetchSessions()
  }, [fetchSessions])

  const handleSessionTypeChange = (value: string) => {
    setSessionType(value)
    setOffset(0)
  }
  const handleTrackChange = (value: string) => {
    setTrackId(value === "" ? "" : Number(value))
    setOffset(0)
  }
  const handleSortChange = (value: string) => {
    setSort(value)
    setOffset(0)
  }
  const handleStateChange = (value: string) => {
    setState(value)
    setOffset(0)
  }

  const handleDateRangeSelect = (range: { from?: Date; to?: Date } | undefined) => {
    if (!range) {
      setDateFrom("")
      setDateTo("")
    } else {
      setDateFrom(range.from ? formatDateForApi(range.from) : "")
      setDateTo(range.to ? formatDateForApi(range.to) : "")
    }
    setOffset(0)
  }

  const dateRangeValue =
    dateFrom && dateTo
      ? { from: new Date(dateFrom), to: new Date(dateTo) }
      : dateFrom
        ? { from: new Date(dateFrom), to: undefined }
        : undefined

  const handleReset = () => {
    setSearch("")
    setSearchDebounced("")
    setSessionType("")
    setTrackId("")
    setSort(DEFAULT_SORT)
    setState("")
    setDateFrom("")
    setDateTo("")
    setOffset(0)
  }

  const handleEditClick = (session: Session) => {
    setEditSession(session)
    setEditName(session.sessionDisplayName ?? session.id ?? "")
    setEditDialogOpen(true)
  }

  const handleEditSubmit = async () => {
    if (!editSession) return
    const trimmed = editName.trim()
    if (!trimmed) {
      notify.warning("Name cannot be blank")
      return
    }
    if (trimmed.length > 64) {
      notify.warning("Name must be 64 characters or less")
      return
    }
    setEditSubmitting(true)
    try {
      await updateSessionDisplayName(editSession.id, trimmed)
      notify.success("Display name updated")
      setEditDialogOpen(false)
      setEditSession(null)
      fetchSessions()
    } catch {
      // toast already shown by client
    } finally {
      setEditSubmitting(false)
    }
  }

  const start = total === 0 ? 0 : offset + 1
  const end = offset + sessions.length
  const hasNext = offset + sessions.length < total
  const hasPrev = offset > 0

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold mb-2">Session History</h1>
        <p className="text-text-secondary">
          View and analyze your previous racing sessions
        </p>
      </div>

      <DataCard>
        <div className="grid md:grid-cols-4 gap-4">
          <div className="md:col-span-2">
            <label className="text-xs text-text-secondary uppercase mb-2 block">
              Search
            </label>
            <div className="relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-text-secondary" />
              <Input
                placeholder="Search by name, track or session type..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                className="pl-10 bg-input-background border-border"
              />
            </div>
          </div>

          <div>
            <label className="text-xs text-text-secondary uppercase mb-2 block">
              Session Type
            </label>
            <Select value={sessionType || "all"} onValueChange={(v) => handleSessionTypeChange(v === "all" ? "" : v)}>
              <SelectTrigger className="bg-input-background border-border">
                <SelectValue placeholder="All Types" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">All Types</SelectItem>
                {SESSION_TYPE_OPTIONS.map((opt) => (
                  <SelectItem key={opt.value} value={opt.value}>
                    {opt.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div>
            <label className="text-xs text-text-secondary uppercase mb-2 block">
              Sort By
            </label>
            <Select value={sort} onValueChange={handleSortChange}>
              <SelectTrigger className="bg-input-background border-border">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {SORT_OPTIONS.map((opt) => (
                  <SelectItem key={opt.value} value={opt.value}>
                    {opt.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        </div>

        <div className="grid md:grid-cols-4 gap-4 mt-4">
          <div>
            <label className="text-xs text-text-secondary uppercase mb-2 block">
              Track
            </label>
            <Select
              value={trackId === "" ? "all" : String(trackId)}
              onValueChange={(v) => handleTrackChange(v === "all" ? "" : v)}
            >
              <SelectTrigger className="bg-input-background border-border">
                <SelectValue placeholder="All Tracks" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">All Tracks</SelectItem>
                {TRACK_OPTIONS.map((opt) => (
                  <SelectItem key={opt.value} value={String(opt.value)}>
                    {opt.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div>
            <label className="text-xs text-text-secondary uppercase mb-2 block">
              Date Range
            </label>
            <Popover>
              <PopoverTrigger asChild>
                <Button variant="outline" size="sm" className="w-full gap-2 justify-start">
                  <Calendar className="w-4 h-4" />
                  {dateFrom && dateTo
                    ? `${dateFrom} – ${dateTo}`
                    : dateFrom
                      ? dateFrom
                      : "Select dates"}
                </Button>
              </PopoverTrigger>
              <PopoverContent className="w-auto p-0" align="start">
                <CalendarComponent
                  mode="range"
                  selected={dateRangeValue}
                  onSelect={handleDateRangeSelect}
                  numberOfMonths={2}
                />
              </PopoverContent>
            </Popover>
          </div>

          <div>
            <label className="text-xs text-text-secondary uppercase mb-2 block">
              State (More Filters)
            </label>
            <Select value={state || "all"} onValueChange={(v) => handleStateChange(v === "all" ? "" : v)}>
              <SelectTrigger className="bg-input-background border-border">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {STATE_OPTIONS.map((opt) => (
                  <SelectItem key={opt.value || "all"} value={opt.value || "all"}>
                    {opt.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="flex items-end gap-2">
            <Button
              variant="ghost"
              size="sm"
              className="text-text-secondary hover:text-foreground"
              onClick={handleReset}
            >
              Reset
            </Button>
          </div>
        </div>
      </DataCard>

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
              Try changing filters or sessions will appear here once you have completed them
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
                  {sessions.map((session) => {
                    const id = session.id
                    const validId = isValidSessionId(id)
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
                    )
                  })}
                </tbody>
              </table>
            </div>

            {sessions.length > 0 && (
              <div className="p-4 border-t border-border/50 flex items-center justify-between">
                <div className="text-sm text-text-secondary">
                  Showing {start}–{end} of {total}
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
  )
}
