import { Link } from "react-router";
import { ArrowRight, Radio, GitCompare, Map, TrendingUp, Activity, CheckCircle2 } from "lucide-react";
import { Button } from "../components/ui/button";

export default function Landing() {
  const features = [
    {
      icon: Radio,
      title: "Live Telemetry Monitoring",
      description: "Real-time data streaming from F1 game with millisecond precision"
    },
    {
      icon: GitCompare,
      title: "Multi-Car Analytics",
      description: "Compare performance across all drivers in the session"
    },
    {
      icon: Activity,
      title: "Driver Comparison",
      description: "Analyze lap times, sectors, and telemetry side-by-side"
    },
    {
      icon: Map,
      title: "Track Visualization",
      description: "Interactive track maps with live positioning and sector times"
    },
    {
      icon: TrendingUp,
      title: "Strategy Insights",
      description: "Tyre degradation, fuel consumption, and pit stop analysis"
    }
  ];

  const steps = [
    {
      number: "01",
      title: "Enable UDP in F1 25",
      description: "Configure telemetry output in your F1 game settings"
    },
    {
      number: "02",
      title: "Connect to Platform",
      description: "Our platform automatically detects and connects to your game"
    },
    {
      number: "03",
      title: "Start Analyzing",
      description: "View live telemetry and analyze your racing performance"
    }
  ];

  return (
    <div className="min-h-screen bg-background">
      {/* Header */}
      <header className="fixed top-0 left-0 right-0 bg-secondary/80 backdrop-blur-xl border-b border-border z-50">
        <div className="max-w-7xl mx-auto px-6 h-16 flex items-center justify-between">
          <Link to="/" className="flex items-center gap-3">
            <div className="w-8 h-8 bg-gradient-to-br from-[#E10600] to-[#00E5FF] rounded-lg" />
            <span className="text-lg font-bold">F1 Telemetry</span>
          </Link>
          <Link to="/login">
            <Button variant="ghost" className="text-sm">
              Login
            </Button>
          </Link>
        </div>
      </header>

      {/* Hero Section */}
      <section className="pt-32 pb-20 px-6">
        <div className="max-w-7xl mx-auto">
          <div className="grid lg:grid-cols-2 gap-12 items-center">
            <div className="space-y-8">
              <h1 className="text-5xl lg:text-6xl font-bold leading-tight">
                Real-Time F1<br />
                <span className="bg-gradient-to-r from-[#00E5FF] to-[#E10600] bg-clip-text text-transparent">
                  Telemetry Analytics
                </span>
              </h1>
              <p className="text-xl text-text-secondary">
                Professional-grade telemetry analysis platform for F1 game players. 
                Monitor live data, compare performance, and optimize your racing strategy.
              </p>
              <div className="flex flex-wrap gap-4">
                <Link to="/register">
                  <Button size="lg" className="bg-[#00E5FF] hover:bg-[#00E5FF]/90 text-background font-semibold">
                    Start Free
                    <ArrowRight className="ml-2 w-4 h-4" />
                  </Button>
                </Link>
                <Link to="/app">
                  <Button size="lg" variant="outline" className="border-border">
                    View Demo
                  </Button>
                </Link>
              </div>
            </div>
            
            <div className="relative">
              <div className="aspect-square rounded-2xl bg-gradient-to-br from-[#E10600]/10 to-[#00E5FF]/10 border border-border p-8 backdrop-blur-sm">
                <div className="h-full flex flex-col justify-center gap-6">
                  <div className="flex gap-4 items-center">
                    <Activity className="w-12 h-12 text-[#00E5FF]" />
                    <div>
                      <div className="text-4xl font-bold font-mono text-[#00E5FF]">312</div>
                      <div className="text-sm text-text-secondary">KM/H</div>
                    </div>
                  </div>
                  <div className="grid grid-cols-2 gap-4">
                    {[
                      { label: 'Throttle', value: '98%', color: '#00FF85' },
                      { label: 'Brake', value: '0%', color: '#E10600' },
                      { label: 'RPM', value: '11,200', color: '#00E5FF' },
                      { label: 'Gear', value: '7', color: '#FACC15' }
                    ].map((stat) => (
                      <div key={stat.label} className="p-3 bg-card/50 rounded-lg border border-border">
                        <div className="text-xs text-text-secondary uppercase">{stat.label}</div>
                        <div className="text-xl font-bold font-mono" style={{ color: stat.color }}>
                          {stat.value}
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
              <div className="absolute -bottom-4 -right-4 w-64 h-64 bg-[#00E5FF]/20 rounded-full blur-3xl -z-10" />
              <div className="absolute -top-4 -left-4 w-64 h-64 bg-[#E10600]/20 rounded-full blur-3xl -z-10" />
            </div>
          </div>
        </div>
      </section>

      {/* Features Section */}
      <section className="py-20 px-6 bg-secondary/30">
        <div className="max-w-7xl mx-auto">
          <div className="text-center mb-16">
            <h2 className="text-3xl font-bold mb-4">Everything You Need to Race Faster</h2>
            <p className="text-lg text-text-secondary">
              Professional telemetry tools designed for serious sim racers
            </p>
          </div>
          
          <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
            {features.map((feature) => {
              const Icon = feature.icon;
              return (
                <div 
                  key={feature.title}
                  className="p-6 bg-card border border-border rounded-xl hover:border-[#00E5FF]/30 transition-all duration-200"
                >
                  <div className="w-12 h-12 bg-[#00E5FF]/10 rounded-lg flex items-center justify-center mb-4">
                    <Icon className="w-6 h-6 text-[#00E5FF]" />
                  </div>
                  <h3 className="text-lg font-semibold mb-2">{feature.title}</h3>
                  <p className="text-text-secondary">{feature.description}</p>
                </div>
              );
            })}
          </div>
        </div>
      </section>

      {/* How It Works */}
      <section className="py-20 px-6">
        <div className="max-w-7xl mx-auto">
          <div className="text-center mb-16">
            <h2 className="text-3xl font-bold mb-4">How It Works</h2>
            <p className="text-lg text-text-secondary">
              Get started in minutes with our simple setup process
            </p>
          </div>
          
          <div className="grid md:grid-cols-3 gap-8">
            {steps.map((step, idx) => (
              <div key={step.number} className="relative">
                <div className="text-center space-y-4">
                  <div className="text-6xl font-bold text-[#00E5FF]/20 font-mono">
                    {step.number}
                  </div>
                  <h3 className="text-xl font-semibold">{step.title}</h3>
                  <p className="text-text-secondary">{step.description}</p>
                </div>
                {idx < steps.length - 1 && (
                  <ArrowRight className="hidden md:block absolute top-8 -right-4 w-6 h-6 text-[#00E5FF]/30" />
                )}
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="py-20 px-6">
        <div className="max-w-4xl mx-auto">
          <div className="relative overflow-hidden rounded-2xl bg-gradient-to-r from-[#E10600]/20 to-[#00E5FF]/20 border border-[#00E5FF]/30 p-12 text-center shadow-[0_0_0_1px_rgba(0,229,255,0.35),0_0_48px_rgba(0,229,255,0.15)]">
            <h2 className="text-3xl font-bold mb-4">Ready to Improve Your Lap Times?</h2>
            <p className="text-lg text-text-secondary mb-8">
              Join thousands of drivers analyzing their telemetry and racing faster
            </p>
            <Link to="/register">
              <Button size="lg" className="bg-[#00E5FF] hover:bg-[#00E5FF]/90 text-background font-semibold">
                Start Free Today
                <ArrowRight className="ml-2 w-4 h-4" />
              </Button>
            </Link>
            <div className="mt-6 flex items-center justify-center gap-2 text-sm text-text-secondary">
              <CheckCircle2 className="w-4 h-4 text-[#00FF85]" />
              No credit card required
            </div>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="py-8 px-6 border-t border-border">
        <div className="max-w-7xl mx-auto text-center text-sm text-text-secondary">
          <p>© 2026 F1 Telemetry Platform. All rights reserved.</p>
        </div>
      </footer>
    </div>
  );
}
