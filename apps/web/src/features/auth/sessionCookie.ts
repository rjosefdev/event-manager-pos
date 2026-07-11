export const NOME_COOKIE_SESSAO = "event_manager_sessao";
export const CAMINHO_COOKIE_SESSAO = "/";
const DURACAO_FALLBACK_SEGUNDOS = 60 * 60 * 8;

export function normalizarTokenCookie(valor: string) {
  try {
    return decodeURIComponent(valor);
  } catch {
    return valor;
  }
}

export function criarCookieSessao(tokenAcesso: string, expiraEm?: string) {
  const maxAge = calcularMaxAge(expiraEm);
  const secure = typeof window !== "undefined" && window.location.protocol === "https:" ? "; Secure" : "";

  return `${NOME_COOKIE_SESSAO}=${encodeURIComponent(tokenAcesso)}; Path=${CAMINHO_COOKIE_SESSAO}; Max-Age=${maxAge}; SameSite=Lax${secure}`;
}

export function criarCookieSessaoExpirado() {
  return `${NOME_COOKIE_SESSAO}=; Path=${CAMINHO_COOKIE_SESSAO}; Max-Age=0; SameSite=Lax`;
}

function calcularMaxAge(expiraEm?: string) {
  if (!expiraEm) {
    return DURACAO_FALLBACK_SEGUNDOS;
  }

  const expiraEmMs = new Date(expiraEm).getTime();
  if (!Number.isFinite(expiraEmMs)) {
    return DURACAO_FALLBACK_SEGUNDOS;
  }

  return Math.max(0, Math.floor((expiraEmMs - Date.now()) / 1000));
}
