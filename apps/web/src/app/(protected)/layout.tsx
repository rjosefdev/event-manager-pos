import { AuthProvider } from "@/features/auth/hooks/useSessao";
import { obterSessaoProtegida } from "@/features/auth/server/sessaoServidor";

export default async function ProtectedLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  const sessao = await obterSessaoProtegida();

  return (
    <AuthProvider sessaoInicial={sessao}>
      <main className="app-shell">
        {children}
      </main>
    </AuthProvider>
  );
}
