import { Link } from "react-router";
import { Button } from "../components/ui/button";
import { Input } from "../components/ui/input";
import { Checkbox } from "../components/ui/checkbox";

export default function Login() {
  return (
    <div className="min-h-screen bg-background flex items-center justify-center p-6 relative overflow-hidden">
      {/* Background Pattern */}
      <div className="absolute inset-0 opacity-30">
        <div className="absolute top-20 left-20 w-96 h-96 bg-[#00E5FF]/10 rounded-full blur-3xl" />
        <div className="absolute bottom-20 right-20 w-96 h-96 bg-[#E10600]/10 rounded-full blur-3xl" />
      </div>

      <div className="w-full max-w-md relative z-10">
        {/* Logo */}
        <Link to="/" className="flex items-center justify-center gap-3 mb-8">
          <div className="w-10 h-10 bg-gradient-to-br from-[#E10600] to-[#00E5FF] rounded-xl" />
          <span className="text-2xl font-bold">F1 Telemetry</span>
        </Link>

        {/* Login Card */}
        <div className="bg-card border border-border rounded-xl p-8 shadow-2xl">
          <div className="mb-6">
            <h1 className="text-2xl font-bold mb-2">Welcome Back</h1>
            <p className="text-text-secondary">Sign in to access your telemetry dashboard</p>
          </div>

          <form className="space-y-4">
            <div className="space-y-2">
              <label htmlFor="email" className="text-sm">Email</label>
              <Input
                id="email"
                type="email"
                placeholder="driver@example.com"
                className="h-10 bg-input-background border-border"
              />
            </div>

            <div className="space-y-2">
              <label htmlFor="password" className="text-sm">Password</label>
              <Input
                id="password"
                type="password"
                placeholder="••••••••"
                className="h-10 bg-input-background border-border"
              />
            </div>

            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Checkbox id="remember" />
                <label htmlFor="remember" className="text-sm normal-case tracking-normal font-normal cursor-pointer">
                  Remember me
                </label>
              </div>
              <Link to="#" className="text-sm text-[#00E5FF] hover:underline">
                Forgot password?
              </Link>
            </div>

            <Link to="/app">
              <Button className="w-full h-10 bg-[#00E5FF] hover:bg-[#00E5FF]/90 text-background font-semibold">
                Login
              </Button>
            </Link>

            <div className="text-center text-sm text-text-secondary">
              Don't have an account?{' '}
              <Link to="/register" className="text-[#00E5FF] hover:underline font-medium">
                Create account
              </Link>
            </div>
          </form>
        </div>

        <div className="mt-6 text-center text-sm text-text-secondary">
          <p>By signing in, you agree to our Terms of Service</p>
        </div>
      </div>
    </div>
  );
}
