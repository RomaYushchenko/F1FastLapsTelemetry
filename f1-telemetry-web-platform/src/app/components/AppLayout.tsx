import { Outlet, Link, useLocation } from "react-router";
import {
  LayoutDashboard,
  Radio,
  Activity,
  Map,
  History,
  GitCompare,
  TrendingUp,
  Settings as SettingsIcon,
  Bell,
  User,
  Wifi,
  WifiOff,
  Menu,
  X,
  CheckCheck,
} from "lucide-react";
import { useEffect, useState } from "react";
import { useLiveTelemetry } from "@/ws";
import { cn } from "./ui/utils";
import { StatusBadge } from "./StatusBadge";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "./ui/popover";
import { Button } from "./ui/button";
import { ScrollArea } from "./ui/scroll-area";
import {
  getNotifications,
  subscribe,
  markAllAsRead,
  type NotificationItem,
} from "@/notificationStore";

const navigation = [
  { name: 'Overview', href: '/app', icon: LayoutDashboard, group: 'Overview' },
  { name: 'Live Overview', href: '/app/live', icon: Radio, group: 'Live' },
  { name: 'Live Telemetry', href: '/app/live/telemetry', icon: Activity, group: 'Live' },
  { name: 'Live Track Map', href: '/app/live/track-map', icon: Map, group: 'Live' },
  { name: 'Session History', href: '/app/sessions', icon: History, group: 'Analysis' },
  { name: 'Driver Comparison', href: '/app/comparison', icon: GitCompare, group: 'Analysis' },
  { name: 'Strategy View', href: '/app/strategy', icon: TrendingUp, group: 'Analysis' },
  { name: 'Settings', href: '/app/settings', icon: SettingsIcon, group: 'Settings' },
];

const groups = ['Overview', 'Live', 'Analysis', 'Settings'];

function notificationTypeLabel(type: NotificationItem["type"]): string {
  switch (type) {
    case "error":
      return "Error";
    case "success":
      return "Success";
    case "warning":
      return "Warning";
    case "info":
      return "Info";
    default:
      return type;
  }
}

function connectionStatusDisplay(status: ReturnType<typeof useLiveTelemetry>['status']) {
  switch (status) {
    case 'live':
      return { label: 'Live' as const, variant: 'active' as const };
    case 'waiting':
      return { label: 'Waiting', variant: 'warning' };
    case 'no-data':
      return { label: 'No Data', variant: 'finished' };
    case 'disconnected':
      return { label: 'Disconnected', variant: 'warning' };
    case 'error':
      return { label: 'Error', variant: 'error' };
    default:
      return { label: 'Waiting', variant: 'warning' };
  }
}

export default function AppLayout() {
  const location = useLocation();
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const { status } = useLiveTelemetry();
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const { label: connectionLabel, variant: connectionVariant } = connectionStatusDisplay(status);

  useEffect(() => {
    return subscribe(setNotifications);
  }, []);

  const unreadCount = notifications.filter((n) => !n.read).length;

  return (
    <div className="min-h-screen bg-background">
      {/* Header */}
      <header className="fixed top-0 left-0 right-0 h-16 bg-secondary border-b border-border z-50">
        <div className="h-full px-6 flex items-center justify-between">
          <div className="flex items-center gap-4">
            <button 
              onClick={() => setSidebarCollapsed(!sidebarCollapsed)}
              className="lg:hidden p-2 hover:bg-sidebar-accent rounded-lg transition-colors"
            >
              {sidebarCollapsed ? <Menu className="w-5 h-5" /> : <X className="w-5 h-5" />}
            </button>
            <Link to="/app" className="flex items-center gap-3">
              <div className="w-8 h-8 bg-gradient-to-br from-[#E10600] to-[#00E5FF] rounded-lg" />
              <span className="text-lg font-bold">F1 Telemetry</span>
            </Link>
          </div>
          
          <div className="flex items-center gap-4">
            {/* Connection Status */}
            <div className="flex items-center gap-2">
              {status === 'live' ? (
                <Wifi className="w-4 h-4 text-[#00E5FF]" />
              ) : status === 'error' ? (
                <WifiOff className="w-4 h-4 text-[#EF4444]" />
              ) : (
                <Wifi className="w-4 h-4 text-[#FACC15]" />
              )}
              <StatusBadge variant={connectionVariant}>{connectionLabel}</StatusBadge>
            </div>

            {/* Notifications */}
            <Popover>
              <PopoverTrigger asChild>
                <button
                  type="button"
                  className="p-2 hover:bg-sidebar-accent rounded-lg transition-colors relative"
                  aria-label="Notifications"
                >
                  <Bell className="w-5 h-5" />
                  {unreadCount > 0 && (
                    <span className="absolute top-1.5 right-1.5 w-2 h-2 bg-[#E10600] rounded-full" />
                  )}
                </button>
              </PopoverTrigger>
              <PopoverContent className="w-80 p-0" align="end">
                <div className="flex items-center justify-between px-3 py-2 border-b border-border/50">
                  <span className="text-sm font-medium">Notifications</span>
                  {notifications.length > 0 && (
                    <Button
                      variant="ghost"
                      size="sm"
                      className="h-7 text-xs gap-1"
                      onClick={markAllAsRead}
                    >
                      <CheckCheck className="w-3.5 h-3.5" />
                      Mark all read
                    </Button>
                  )}
                </div>
                <ScrollArea className="h-[280px]">
                  {notifications.length === 0 ? (
                    <div className="py-6 text-center text-sm text-text-secondary">
                      No notifications
                    </div>
                  ) : (
                    <ul className="py-1">
                      {notifications.map((n) => (
                        <li
                          key={n.id}
                          className={cn(
                            "px-3 py-2 text-sm border-b border-border/30 last:border-0",
                            !n.read && "bg-secondary/30"
                          )}
                        >
                          <span
                            className={cn(
                              "text-xs font-medium uppercase",
                              n.type === "error" && "text-red-500",
                              n.type === "success" && "text-green-500",
                              n.type === "warning" && "text-amber-500",
                              n.type === "info" && "text-text-secondary"
                            )}
                          >
                            {notificationTypeLabel(n.type)}
                          </span>
                          <p className="mt-0.5 break-words">{n.message}</p>
                        </li>
                      ))}
                    </ul>
                  )}
                </ScrollArea>
              </PopoverContent>
            </Popover>

            {/* User Menu */}
            <button className="flex items-center gap-2 p-2 hover:bg-sidebar-accent rounded-lg transition-colors">
              <div className="w-8 h-8 bg-gradient-to-br from-[#00E5FF] to-[#00FF85] rounded-full flex items-center justify-center">
                <User className="w-4 h-4 text-background" />
              </div>
              <span className="text-sm font-medium hidden md:block">Driver</span>
            </button>
          </div>
        </div>
      </header>

      {/* Sidebar */}
      <aside className={cn(
        "fixed left-0 top-16 bottom-0 bg-sidebar border-r border-sidebar-border transition-all duration-200 z-40",
        sidebarCollapsed ? "w-20" : "w-64"
      )}>
        <nav className="p-4 space-y-6">
          {groups.map((group) => (
            <div key={group}>
              {!sidebarCollapsed && (
                <div className="px-3 mb-2 text-xs text-text-secondary uppercase tracking-wider font-medium">
                  {group}
                </div>
              )}
              <div className="space-y-1">
                {navigation.filter(item => item.group === group).map((item) => {
                  const isActive = location.pathname === item.href;
                  const Icon = item.icon;
                  
                  return (
                    <Link
                      key={item.name}
                      to={item.href}
                      className={cn(
                        "flex items-center gap-3 px-3 py-2 rounded-lg transition-all duration-150",
                        "hover:bg-sidebar-accent",
                        isActive && "bg-sidebar-accent text-[#00E5FF] shadow-[inset_3px_0_0_#00E5FF,0_0_0_1px_rgba(0,229,255,0.2),0_0_16px_rgba(0,229,255,0.1)]"
                      )}
                      title={sidebarCollapsed ? item.name : undefined}
                    >
                      <Icon className="w-5 h-5 flex-shrink-0" />
                      {!sidebarCollapsed && (
                        <span className="text-sm font-medium">{item.name}</span>
                      )}
                      {!sidebarCollapsed && item.group === 'Live' && isActive && (
                        <span className="ml-auto w-2 h-2 bg-[#00E5FF] rounded-full animate-pulse" />
                      )}
                    </Link>
                  );
                })}
              </div>
            </div>
          ))}
        </nav>
      </aside>

      {/* Main Content */}
      <main className={cn(
        "pt-16 transition-all duration-200",
        sidebarCollapsed ? "ml-20" : "ml-64"
      )}>
        <div className="max-w-[1440px] mx-auto p-6">
          <Outlet />
        </div>
      </main>
    </div>
  );
}
