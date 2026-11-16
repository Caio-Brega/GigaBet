/**
 * Cliente da API do GigaBet. O Vite encaminha /api para o backend Java na porta 8080.
 */

const API_BASE = '/api';

export class ApiClient {
  static getToken() {
    return localStorage.getItem('gigabet_token');
  }

  static setToken(token) {
    if (token) localStorage.setItem('gigabet_token', token);
    else localStorage.removeItem('gigabet_token');
  }

  static async request(endpoint, options = {}) {
    const token = this.getToken();
    const headers = {
      'Content-Type': 'application/json',
      ...(token ? { 'Authorization': `Bearer ${token}` } : {}),
      ...(options.headers || {})
    };

    try {
      const res = await fetch(`${API_BASE}${endpoint}`, {
        ...options,
        headers
      });

      const data = await res.json().catch(() => ({}));

      if (!res.ok) {
        throw new Error(data.erro || `Erro na requisição: ${res.status}`);
      }

      return data;
    } catch (err) {
      if (err instanceof TypeError) {
        throw new Error('Servidor indisponível. Suba o Postgres e a API.');
      }
      throw err;
    }
  }

  static async logout() {
    try {
      if (this.getToken()) {
        await this.request('/auth/sessao', { method: 'DELETE' });
      }
    } catch {
      // A sessão local sai mesmo se o token já não valer no servidor.
    }
    this.setToken(null);
  }

  // --- Auth ---
  static async login(email, senha) {
    const data = await this.request('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, senha })
    });
    if (data.token) this.setToken(data.token);
    return data;
  }

  static async cadastro({ nome, cpf, email, senha, aceitouTermo }) {
    const data = await this.request('/auth/cadastro', {
      method: 'POST',
      body: JSON.stringify({ nome, cpf, email, senha, aceitouTermo })
    });
    if (data.token) this.setToken(data.token);
    return data;
  }

  static async me() {
    return this.request('/auth/me');
  }

  static async deposito(valor) {
    return this.request('/auth/deposito', {
      method: 'POST',
      body: JSON.stringify({ valor: valor.toString() })
    });
  }

  // --- Partidas ---
  static async listarPartidas() {
    return this.request('/partidas');
  }

  static async cadastrarPartida({ mandante, visitante, inicio }) {
    return this.request('/partidas', {
      method: 'POST',
      body: JSON.stringify({ mandante, visitante, inicio })
    });
  }

  static async registrarResultado(partidaId, { golsMandante, golsVisitante, cartoesAmarelos }) {
    return this.request(`/partidas/${partidaId}/resultado`, {
      method: 'POST',
      body: JSON.stringify({
        golsMandante: golsMandante.toString(),
        golsVisitante: golsVisitante.toString(),
        cartoesAmarelos: cartoesAmarelos.toString()
      })
    });
  }

  // --- Equipes ---
  static async listarEquipes() {
    return this.request('/equipes');
  }

  static async cadastrarEquipe(nome) {
    return this.request('/equipes', {
      method: 'POST',
      body: JSON.stringify({ nome })
    });
  }

  // --- Apostas ---
  static async cotar(pedido) {
    return this.request('/apostas/cotar', {
      method: 'POST',
      body: JSON.stringify(pedido)
    });
  }

  static async confirmarAposta(pedido) {
    return this.request('/apostas/confirmar', {
      method: 'POST',
      body: JSON.stringify(pedido)
    });
  }

  static async historicoApostas() {
    return this.request('/apostas/historico');
  }

  // --- Relatórios e Auditoria (Admin) ---
  static async relatorioPartida(partidaId) {
    return this.request(`/relatorios/partida/${partidaId}`);
  }

  static async relatorioApostador(busca) {
    return this.request(`/relatorios/apostador?busca=${encodeURIComponent(busca)}`);
  }

  static async listarAuditoria() {
    return this.request('/auditoria');
  }
}
