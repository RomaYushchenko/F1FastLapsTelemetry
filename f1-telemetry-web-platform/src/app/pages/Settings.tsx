import { DataCard } from "../components/DataCard";
import { Button } from "../components/ui/button";
import { Input } from "../components/ui/input";
import { Switch } from "../components/ui/switch";
import { 
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "../components/ui/select";
import { Separator } from "../components/ui/separator";
import { User, Bell, Sliders, Trash2 } from "lucide-react";

export default function Settings() {
  return (
    <div className="space-y-6 max-w-4xl">
      <div>
        <h1 className="text-3xl font-bold mb-2">Settings</h1>
        <p className="text-text-secondary">Manage your account and telemetry preferences</p>
      </div>

      {/* Profile Settings */}
      <DataCard>
        <div className="flex items-center gap-3 mb-6">
          <div className="p-2 bg-[#00E5FF]/10 rounded-lg">
            <User className="w-5 h-5 text-[#00E5FF]" />
          </div>
          <h2 className="text-xl font-bold">Profile Information</h2>
        </div>

        <div className="space-y-4">
          <div className="grid md:grid-cols-2 gap-4">
            <div className="space-y-2">
              <label htmlFor="name" className="text-sm">Name</label>
              <Input
                id="name"
                type="text"
                defaultValue="Max Verstappen"
                className="bg-input-background border-border"
              />
            </div>
            <div className="space-y-2">
              <label htmlFor="email" className="text-sm">Email</label>
              <Input
                id="email"
                type="email"
                defaultValue="driver@example.com"
                className="bg-input-background border-border"
              />
            </div>
          </div>

          <div className="space-y-2">
            <label htmlFor="driver-number" className="text-sm">Driver Number</label>
            <Input
              id="driver-number"
              type="text"
              defaultValue="1"
              className="bg-input-background border-border max-w-xs"
            />
          </div>

          <div className="pt-4">
            <Button className="bg-[#00E5FF] hover:bg-[#00E5FF]/90 text-background">
              Save Changes
            </Button>
          </div>
        </div>
      </DataCard>

      {/* Appearance */}
      <DataCard>
        <div className="flex items-center gap-3 mb-6">
          <div className="p-2 bg-[#A855F7]/10 rounded-lg">
            <Sliders className="w-5 h-5 text-[#A855F7]" />
          </div>
          <h2 className="text-xl font-bold">Appearance</h2>
        </div>

        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <div>
              <div className="font-medium">Theme</div>
              <div className="text-sm text-text-secondary">Currently set to dark mode</div>
            </div>
            <Select defaultValue="dark">
              <SelectTrigger className="w-[180px] bg-input-background border-border">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="dark">Dark</SelectItem>
                <SelectItem value="light">Light</SelectItem>
                <SelectItem value="system">System</SelectItem>
              </SelectContent>
            </Select>
          </div>
        </div>
      </DataCard>

      {/* Telemetry Preferences */}
      <DataCard>
        <div className="flex items-center gap-3 mb-6">
          <div className="p-2 bg-[#00FF85]/10 rounded-lg">
            <Sliders className="w-5 h-5 text-[#00FF85]" />
          </div>
          <h2 className="text-xl font-bold">Telemetry Preferences</h2>
        </div>

        <div className="space-y-6">
          <div className="space-y-2">
            <label htmlFor="units" className="text-sm">Units System</label>
            <Select defaultValue="metric">
              <SelectTrigger className="bg-input-background border-border max-w-xs">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="metric">Metric (km/h, °C)</SelectItem>
                <SelectItem value="imperial">Imperial (mph, °F)</SelectItem>
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-2">
            <label htmlFor="smoothing" className="text-sm">Data Smoothing</label>
            <Select defaultValue="medium">
              <SelectTrigger className="bg-input-background border-border max-w-xs">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="none">None (Raw)</SelectItem>
                <SelectItem value="low">Low</SelectItem>
                <SelectItem value="medium">Medium</SelectItem>
                <SelectItem value="high">High</SelectItem>
              </SelectContent>
            </Select>
            <p className="text-sm text-text-secondary">
              Applies smoothing to telemetry graphs for better readability
            </p>
          </div>

          <div className="space-y-2">
            <label htmlFor="update-rate" className="text-sm">Update Rate</label>
            <Select defaultValue="20">
              <SelectTrigger className="bg-input-background border-border max-w-xs">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="10">10 Hz</SelectItem>
                <SelectItem value="20">20 Hz</SelectItem>
                <SelectItem value="30">30 Hz</SelectItem>
                <SelectItem value="60">60 Hz</SelectItem>
              </SelectContent>
            </Select>
            <p className="text-sm text-text-secondary">
              Higher rates provide more detail but may affect performance
            </p>
          </div>

          <Separator className="bg-border/50" />

          <div className="flex items-center justify-between">
            <div>
              <div className="font-medium">Auto-save Sessions</div>
              <div className="text-sm text-text-secondary">Automatically save telemetry data</div>
            </div>
            <Switch defaultChecked />
          </div>

          <div className="flex items-center justify-between">
            <div>
              <div className="font-medium">Record Video</div>
              <div className="text-sm text-text-secondary">Save video alongside telemetry (requires storage)</div>
            </div>
            <Switch />
          </div>
        </div>
      </DataCard>

      {/* Alert Settings */}
      <DataCard>
        <div className="flex items-center gap-3 mb-6">
          <div className="p-2 bg-[#FACC15]/10 rounded-lg">
            <Bell className="w-5 h-5 text-[#FACC15]" />
          </div>
          <h2 className="text-xl font-bold">Alert Settings</h2>
        </div>

        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <div>
              <div className="font-medium">Penalty Alerts</div>
              <div className="text-sm text-text-secondary">Notify when you receive a penalty</div>
            </div>
            <Switch defaultChecked />
          </div>

          <div className="flex items-center justify-between">
            <div>
              <div className="font-medium">Safety Car Alerts</div>
              <div className="text-sm text-text-secondary">Notify when safety car is deployed</div>
            </div>
            <Switch defaultChecked />
          </div>

          <div className="flex items-center justify-between">
            <div>
              <div className="font-medium">Packet Loss Alerts</div>
              <div className="text-sm text-text-secondary">Warn when packet loss exceeds threshold</div>
            </div>
            <Switch defaultChecked />
          </div>

          <div className="space-y-2">
            <label htmlFor="packet-threshold" className="text-sm">Packet Loss Threshold</label>
            <div className="flex items-center gap-3">
              <Input
                id="packet-threshold"
                type="number"
                defaultValue="5"
                className="bg-input-background border-border max-w-[100px]"
              />
              <span className="text-sm text-text-secondary">%</span>
            </div>
          </div>
        </div>
      </DataCard>

      {/* UDP Connection */}
      <DataCard>
        <div className="flex items-center gap-3 mb-6">
          <h2 className="text-xl font-bold">UDP Connection Settings</h2>
        </div>

        <div className="space-y-4">
          <div className="p-4 bg-[#00E5FF]/10 border border-[#00E5FF]/30 rounded-lg">
            <h3 className="font-semibold mb-2">Connection Instructions</h3>
            <ol className="text-sm text-text-secondary space-y-2 list-decimal list-inside">
              <li>Open F1 25 and go to Settings → Telemetry Settings</li>
              <li>Set UDP Telemetry to "On"</li>
              <li>Set UDP Broadcast Mode to "On"</li>
              <li>Set UDP IP Address to your computer's local IP</li>
              <li>Set UDP Port to: <span className="font-mono font-bold text-foreground">20777</span></li>
              <li>Set UDP Send Rate to "20Hz" or higher</li>
            </ol>
          </div>

          <div className="grid md:grid-cols-2 gap-4">
            <div className="space-y-2">
              <label htmlFor="udp-port" className="text-sm">UDP Port</label>
              <Input
                id="udp-port"
                type="text"
                defaultValue="20777"
                className="bg-input-background border-border font-mono"
              />
            </div>
            <div className="space-y-2">
              <label htmlFor="udp-ip" className="text-sm">Local IP Address</label>
              <Input
                id="udp-ip"
                type="text"
                defaultValue="192.168.1.100"
                className="bg-input-background border-border font-mono"
                readOnly
              />
            </div>
          </div>

          <div className="flex gap-3">
            <Button className="bg-[#00E5FF] hover:bg-[#00E5FF]/90 text-background">
              Test Connection
            </Button>
            <Button variant="outline">
              View Diagnostics
            </Button>
          </div>
        </div>
      </DataCard>

      {/* Danger Zone */}
      <DataCard variant="error">
        <div className="flex items-center gap-3 mb-6">
          <div className="p-2 bg-[#E10600]/10 rounded-lg">
            <Trash2 className="w-5 h-5 text-[#E10600]" />
          </div>
          <h2 className="text-xl font-bold">Danger Zone</h2>
        </div>

        <div className="space-y-4">
          <div className="p-4 border border-border/50 rounded-lg">
            <h3 className="font-semibold mb-2">Delete All Session Data</h3>
            <p className="text-sm text-text-secondary mb-4">
              Permanently delete all your recorded telemetry sessions. This action cannot be undone.
            </p>
            <Button variant="outline" className="border-[#E10600] text-[#E10600] hover:bg-[#E10600] hover:text-white">
              Delete All Sessions
            </Button>
          </div>

          <div className="p-4 border border-border/50 rounded-lg">
            <h3 className="font-semibold mb-2">Delete Account</h3>
            <p className="text-sm text-text-secondary mb-4">
              Permanently delete your account and all associated data. This action cannot be undone.
            </p>
            <Button variant="outline" className="border-[#E10600] text-[#E10600] hover:bg-[#E10600] hover:text-white">
              Delete Account
            </Button>
          </div>
        </div>
      </DataCard>
    </div>
  );
}
