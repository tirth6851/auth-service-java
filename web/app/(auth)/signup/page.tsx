import { AuthForm } from "@/components/auth-form";

export default function SignupPage() {
  return (
    <div className="flex flex-1 items-center justify-center p-6">
      <AuthForm mode="signup" />
    </div>
  );
}
