import { redirect } from "next/navigation";
import { getIdentity } from "@/lib/auth";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

export default async function AccountPage() {
  const identity = await getIdentity();

  if (!identity) {
    redirect("/login?next=/account");
  }

  return (
    <Card className="max-w-md">
      <CardHeader>
        <CardTitle>Account</CardTitle>
      </CardHeader>
      <CardContent className="flex flex-col gap-2 text-sm">
        <div>
          <span className="text-muted-foreground">Email: </span>
          {identity.email}
        </div>
        <div>
          <span className="text-muted-foreground">User ID: </span>
          {identity.userId}
        </div>
      </CardContent>
    </Card>
  );
}
