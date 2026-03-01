import { useState } from "react";
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
import { Search, Eye, Filter, Calendar } from "lucide-react";
import { Link } from "react-router";

export default function SessionHistory() {
  const [searchQuery, setSearchQuery] = useState("");

  const sessions = [
    { id: 1, track: "Silverstone", date: "2026-02-24", sessionType: "Race", bestLap: "1:27.451", result: "P3", totalTime: "1:42:34.123" },
    { id: 2, track: "Monaco", date: "2026-02-23", sessionType: "Qualifying", bestLap: "1:12.234", result: "P5", totalTime: "18:23.456" },
    { id: 3, track: "Spa-Francorchamps", date: "2026-02-22", sessionType: "Race", bestLap: "1:44.556", result: "P2", totalTime: "1:38:12.789" },
    { id: 4, track: "Monza", date: "2026-02-21", sessionType: "Practice", bestLap: "1:21.112", result: "P7", totalTime: "32:45.234" },
    { id: 5, track: "Suzuka", date: "2026-02-20", sessionType: "Race", bestLap: "1:30.983", result: "P1", totalTime: "1:35:27.456" },
    { id: 6, track: "Interlagos", date: "2026-02-19", sessionType: "Sprint", bestLap: "1:10.234", result: "P4", totalTime: "24:56.789" },
    { id: 7, track: "Circuit of the Americas", date: "2026-02-18", sessionType: "Race", bestLap: "1:36.789", result: "P6", totalTime: "1:40:23.123" },
    { id: 8, track: "Zandvoort", date: "2026-02-17", sessionType: "Qualifying", bestLap: "1:11.456", result: "P3", totalTime: "22:34.567" },
  ];

  const filteredSessions = sessions.filter(session =>
    session.track.toLowerCase().includes(searchQuery.toLowerCase()) ||
    session.sessionType.toLowerCase().includes(searchQuery.toLowerCase())
  );

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold mb-2">Session History</h1>
        <p className="text-text-secondary">View and analyze your previous racing sessions</p>
      </div>

      {/* Filters */}
      <DataCard>
        <div className="grid md:grid-cols-4 gap-4">
          <div className="md:col-span-2">
            <label className="text-xs text-text-secondary uppercase mb-2 block">Search</label>
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
            <label className="text-xs text-text-secondary uppercase mb-2 block">Session Type</label>
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
            <label className="text-xs text-text-secondary uppercase mb-2 block">Sort By</label>
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
        {filteredSessions.length > 0 ? (
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
                {filteredSessions.map((session) => (
                  <tr key={session.id} className="hover:bg-secondary/30 transition-colors">
                    <td className="px-4 py-4 font-medium">{session.track}</td>
                    <td className="px-4 py-4 text-text-secondary">
                      {new Date(session.date).toLocaleDateString('en-US', { 
                        month: 'short', 
                        day: 'numeric',
                        year: 'numeric'
                      })}
                    </td>
                    <td className="px-4 py-4">
                      <span className="inline-flex px-2 py-1 rounded text-xs bg-secondary/50 border border-border">
                        {session.sessionType}
                      </span>
                    </td>
                    <td className="px-4 py-4 font-mono text-[#00E5FF]">{session.bestLap}</td>
                    <td className="px-4 py-4 font-mono text-text-secondary">{session.totalTime}</td>
                    <td className="px-4 py-4">
                      <span className={`font-bold ${
                        session.result === 'P1' ? 'text-[#00FF85]' :
                        session.result === 'P2' || session.result === 'P3' ? 'text-[#00E5FF]' :
                        'text-text-secondary'
                      }`}>
                        {session.result}
                      </span>
                    </td>
                    <td className="px-4 py-4">
                      <Link to={`/app/sessions/${session.id}`}>
                        <Button variant="ghost" size="sm" className="h-8 gap-2">
                          <Eye className="w-4 h-4" />
                          View
                        </Button>
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="p-12 text-center">
            <div className="w-16 h-16 bg-secondary/50 rounded-full flex items-center justify-center mx-auto mb-4">
              <Search className="w-8 h-8 text-text-secondary" />
            </div>
            <h3 className="text-lg font-semibold mb-2">No sessions found</h3>
            <p className="text-text-secondary">Try adjusting your search or filters</p>
          </div>
        )}

        {filteredSessions.length > 0 && (
          <div className="p-4 border-t border-border/50 flex items-center justify-between">
            <div className="text-sm text-text-secondary">
              Showing {filteredSessions.length} of {sessions.length} sessions
            </div>
            <div className="flex gap-2">
              <Button variant="outline" size="sm" disabled>
                Previous
              </Button>
              <Button variant="outline" size="sm">
                Next
              </Button>
            </div>
          </div>
        )}
      </DataCard>
    </div>
  );
}
