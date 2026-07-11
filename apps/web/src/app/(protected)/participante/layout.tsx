import { exigirPerfil } from "@/features/auth/server/sessaoServidor";

export default async function ParticipanteLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  await exigirPerfil("PARTICIPANTE");

  return children;
}
