import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";

export default function SecurityPlaceholderPage() {
  return (
    <Alert className="max-w-md">
      <AlertTitle>Not available yet</AlertTitle>
      <AlertDescription>
        Password management isn&apos;t available yet. There is currently no way to change your
        password or set up other security options from here.
      </AlertDescription>
    </Alert>
  );
}
