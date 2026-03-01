import { createBrowserRouter } from "react-router";
import { LiveTelemetryProvider } from "@/ws";
import Landing from "./pages/Landing";
import Login from "./pages/Login";
import Register from "./pages/Register";
import AppLayout from "./components/AppLayout";
import Dashboard from "./pages/Dashboard";
import LiveOverview from "./pages/LiveOverview";
import LiveTelemetry from "./pages/LiveTelemetry";
import LiveTrackMap from "./pages/LiveTrackMap";
import SessionHistory from "./pages/SessionHistory";
import SessionDetails from "./pages/SessionDetails";
import DriverComparison from "./pages/DriverComparison";
import StrategyView from "./pages/StrategyView";
import Settings from "./pages/Settings";

export const router = createBrowserRouter([
  {
    path: "/",
    Component: Landing,
  },
  {
    path: "/login",
    Component: Login,
  },
  {
    path: "/register",
    Component: Register,
  },
  {
    path: "/app",
    Component: function AppWithLive() {
      return (
        <LiveTelemetryProvider>
          <AppLayout />
        </LiveTelemetryProvider>
      );
    },
    children: [
      {
        index: true,
        Component: Dashboard,
      },
      {
        path: "live",
        Component: LiveOverview,
      },
      {
        path: "live/telemetry",
        Component: LiveTelemetry,
      },
      {
        path: "live/track-map",
        Component: LiveTrackMap,
      },
      {
        path: "sessions",
        Component: SessionHistory,
      },
      {
        path: "sessions/:id",
        Component: SessionDetails,
      },
      {
        path: "sessions/:id/strategy",
        Component: StrategyView,
      },
      {
        path: "comparison",
        Component: DriverComparison,
      },
      {
        path: "strategy",
        Component: StrategyView,
      },
      {
        path: "settings",
        Component: Settings,
      },
    ],
  },
]);
