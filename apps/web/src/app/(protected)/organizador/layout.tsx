import { exigirPerfil } from "@/features/auth/server/sessaoServidor";

export default async function OrganizadorLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  await exigirPerfil("ORGANIZADOR");

  return children;
}
