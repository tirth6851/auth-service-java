import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";

export default function SessionsPlaceholderPage() {
  return (
    <Alert className="max-w-md">
      <AlertTitle>Not available yet</AlertTitle>
      <AlertDescription>
        Device/session management isn&apos;t available yet. There is currently no way to view or
        revoke individual sessions from here — logging out only ends the current session.
      </AlertDescription>
    </Alert>
  );
}
