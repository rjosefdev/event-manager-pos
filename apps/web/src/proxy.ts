import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";
import { NOME_COOKIE_SESSAO } from "@/features/auth/sessionCookie";

export function proxy(request: NextRequest) {
  if (request.cookies.has(NOME_COOKIE_SESSAO)) {
    return NextResponse.next();
  }

  const loginUrl = new URL("/login", request.url);
  loginUrl.searchParams.set("redirect", request.nextUrl.pathname);

  return NextResponse.redirect(loginUrl);
}

export const config = {
  matcher: ["/participante/:path*", "/organizador/:path*"],
};
