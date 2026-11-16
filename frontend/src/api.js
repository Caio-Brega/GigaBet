/**
 * Cliente de API do GigaBet
 * Conecta-se ao backend Java (porta 8080) e oferece fallback inteligente em memória/localStorage
 * caso o backend esteja offline durante testes de frontend.
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
      // Se a conexão falhou (backend offline), aciona o motor de simulação local
      if (err.message.includes('Failed to fetch') || err.message.includes('NetworkError') || err.message.includes('Load failed')) {
        console.warn(`[GigaBet API] Backend offline para ${endpoint}. Usando fallback local.`);
        return MockBackend.handle(endpoint, options, token);
      }
      throw err;
    }
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

/**
 * Motor de simulação local com regras de negócio da UFMG (Laplace + Odds mínimas)
 * Garante que o frontend funcione instantaneamente mesmo sem ligar o Java primeiro.
 */
class MockBackend {
  static storageKey = 'gigabet_mock_db_v1';

  static getDb() {
    let raw = localStorage.getItem(this.storageKey);
    if (!raw) {
      const agora = new Date();
      const em1Dia = new Date(agora.getTime() + 24 * 3600 * 1000).toISOString();
      const em2Dias = new Date(agora.getTime() + 48 * 3600 * 1000).toISOString();
      const em3Dias = new Date(agora.getTime() + 72 * 3600 * 1000).toISOString();

      const initial = {
        usuarios: [
          {
            id: 1,
            nome: 'Administrador',
            email: 'admin@apostas.local',
            senha: 'admin123',
            perfil: 'ADMINISTRADOR',
            token: 'mock-admin-token'
          },
          {
            id: 2,
            nome: 'Carlos Apostador',
            email: 'carlos@email.com',
            senha: 'senha123',
            cpf: '529.982.247-25',
            saldo: '150.00',
            perfil: 'APOSTADOR',
            token: 'mock-user-token'
          }
        ],
        equipes: [
          { id: 1, nome: 'Flamengo' },
          { id: 2, nome: 'Palmeiras' },
          { id: 3, nome: 'São Paulo' },
          { id: 4, nome: 'Corinthians' },
          { id: 5, nome: 'Cruzeiro' },
          { id: 6, nome: 'Atlético-MG' },
          { id: 7, nome: 'Grêmio' },
          { id: 8, nome: 'Internacional' }
        ],
        partidas: [
          {
            id: 1,
            descricao: 'Flamengo x Palmeiras',
            mandante: 'Flamengo',
            visitante: 'Palmeiras',
            inicio: em1Dia,
            status: 'AGENDADA'
          },
          {
            id: 2,
            descricao: 'Corinthians x São Paulo',
            mandante: 'Corinthians',
            visitante: 'São Paulo',
            inicio: em2Dias,
            status: 'AGENDADA'
          },
          {
            id: 3,
            descricao: 'Cruzeiro x Atlético-MG',
            mandante: 'Cruzeiro',
            visitante: 'Atlético-MG',
            inicio: em3Dias,
            status: 'AGENDADA'
          },
          {
            id: 4,
            descricao: 'Grêmio x Internacional',
            mandante: 'Grêmio',
            visitante: 'Internacional',
            inicio: new Date(agora.getTime() - 24 * 3600 * 1000).toISOString(),
            status: 'ENCERRADA',
            golsMandante: 2,
            golsVisitante: 1,
            cartoesAmarelos: 4
          }
        ],
        historicoPartidas: [
          { mandante: 'Flamengo', visitante: 'Palmeiras', gm: 2, gv: 1, cartoes: 3 },
          { mandante: 'Flamengo', visitante: 'Palmeiras', gm: 1, gv: 0, cartoes: 2 },
          { mandante: 'Flamengo', visitante: 'Palmeiras', gm: 3, gv: 0, cartoes: 5 },
          { mandante: 'Flamengo', visitante: 'Palmeiras', gm: 2, gv: 0, cartoes: 1 },
          { mandante: 'Flamengo', visitante: 'Palmeiras', gm: 0, gv: 1, cartoes: 4 },
          { mandante: 'Flamengo', visitante: 'Palmeiras', gm: 0, gv: 2, cartoes: 3 },
          { mandante: 'Flamengo', visitante: 'Palmeiras', gm: 1, gv: 1, cartoes: 2 },
          { mandante: 'Flamengo', visitante: 'Palmeiras', gm: 2, gv: 2, cartoes: 4 }
        ],
        apostas: [
          {
            apostaId: 101,
            partidaId: 4,
            partida: 'Grêmio x Internacional',
            tipo: 'EQUIPE_VENCEDORA',
            previsao: 'Vitória do mandante',
            valor: '50.00',
            probabilidade: '0.4545',
            premioPotencial: '99.00',
            status: 'VENCEDORA',
            criadaEm: new Date(agora.getTime() - 30 * 3600 * 1000).toISOString(),
            apostadorId: 2,
            apostador: 'Carlos Apostador'
          }
        ],
        auditoria: [
          {
            id: 1,
            usuarioId: 1,
            tipo: 'CADASTRO_EQUIPE',
            detalhes: 'Equipes semeadas no sistema inicial.',
            quando: agora.toISOString()
          }
        ]
      };
      localStorage.setItem(this.storageKey, JSON.stringify(initial));
      return initial;
    }
    return JSON.parse(raw);
  }

  static saveDb(db) {
    localStorage.setItem(this.storageKey, JSON.stringify(db));
  }

  static handle(endpoint, options, token) {
    const db = this.getDb();
    const method = (options.method || 'GET').toUpperCase();
    const body = options.body ? JSON.parse(options.body) : {};
    const usuarioAtual = db.usuarios.find(u => u.token === token);

    // Auth Login
    if (endpoint === '/auth/login' && method === 'POST') {
      const u = db.usuarios.find(x => x.email.toLowerCase() === body.email.toLowerCase());
      if (!u || u.senha !== body.senha) {
        throw new Error('E-mail ou senha inválidos.');
      }
      return {
        id: u.id,
        nome: u.nome,
        email: u.email,
        perfil: u.perfil,
        saldo: u.saldo || '0.00',
        cpf: u.cpf,
        token: u.token
      };
    }

    // Auth Cadastro
    if (endpoint === '/auth/cadastro' && method === 'POST') {
      if (!body.aceitouTermo) throw new Error('O cadastro exige o aceite do termo de tratamento de dados.');
      if (db.usuarios.some(x => x.email === body.email)) throw new Error('E-mail já cadastrado.');
      if (db.usuarios.some(x => x.cpf === body.cpf)) throw new Error('CPF já cadastrado.');

      const novo = {
        id: db.usuarios.length + 1,
        nome: body.nome,
        email: body.email,
        senha: body.senha,
        cpf: body.cpf,
        saldo: '0.00',
        perfil: 'APOSTADOR',
        token: 'token-' + Math.random().toString(36).substring(2)
      };
      db.usuarios.push(novo);
      this.saveDb(db);
      return novo;
    }

    // Auth Me
    if (endpoint === '/auth/me') {
      if (!usuarioAtual) throw new Error('Sessão inválida. Faça login novamente.');
      return usuarioAtual;
    }

    // Deposito
    if (endpoint === '/auth/deposito' && method === 'POST') {
      if (!usuarioAtual || usuarioAtual.perfil !== 'APOSTADOR') throw new Error('Acesso negado.');
      const v = parseFloat(body.valor);
      if (isNaN(v) || v <= 0) throw new Error('Valor inválido.');
      usuarioAtual.saldo = (parseFloat(usuarioAtual.saldo || 0) + v).toFixed(2);
      this.saveDb(db);
      return { sucesso: true, novoSaldo: usuarioAtual.saldo };
    }

    // Partidas
    if (endpoint === '/partidas' && method === 'GET') {
      return db.partidas;
    }

    if (endpoint === '/partidas' && method === 'POST') {
      if (!usuarioAtual || usuarioAtual.perfil !== 'ADMINISTRADOR') throw new Error('Acesso negado.');
      const nova = {
        id: db.partidas.length + 1,
        descricao: `${body.mandante} x ${body.visitante}`,
        mandante: body.mandante,
        visitante: body.visitante,
        inicio: body.inicio,
        status: 'AGENDADA'
      };
      db.partidas.push(nova);
      this.saveDb(db);
      return nova;
    }

    // Resultado Partida & Apuração
    const matchRes = endpoint.match(/^\/partidas\/(\d+)\/resultado$/);
    if (matchRes && method === 'POST') {
      if (!usuarioAtual || usuarioAtual.perfil !== 'ADMINISTRADOR') throw new Error('Acesso negado.');
      const pid = parseInt(matchRes[1]);
      const p = db.partidas.find(x => x.id === pid);
      if (!p) throw new Error('Partida não encontrada.');
      p.status = 'ENCERRADA';
      p.golsMandante = parseInt(body.golsMandante);
      p.golsVisitante = parseInt(body.golsVisitante);
      p.cartoesAmarelos = parseInt(body.cartoesAmarelos);

      // Apurar apostas
      let apuradas = 0, vencedoras = 0, perdedoras = 0;
      db.apostas.filter(a => a.partidaId === pid && a.status === 'ABERTA').forEach(a => {
        apuradas++;
        let venceu = false;
        if (a.tipo === 'EQUIPE_VENCEDORA') {
          const desfechoReal = p.golsMandante > p.golsVisitante ? 'MANDANTE' : p.golsMandante < p.golsVisitante ? 'VISITANTE' : 'EMPATE';
          venceu = a.desfecho === desfechoReal;
        } else if (a.tipo === 'PLACAR_EXATO') {
          venceu = a.golsMandante === p.golsMandante && a.golsVisitante === p.golsVisitante;
        } else if (a.tipo === 'CARTOES_AMARELOS') {
          const totalReal = Math.min(10, p.cartoesAmarelos);
          venceu = a.cartoesAmarelos === totalReal;
        }

        if (venceu) {
          a.status = 'VENCEDORA';
          vencedoras++;
          const apostador = db.usuarios.find(u => u.id === a.apostadorId);
          if (apostador) {
            apostador.saldo = (parseFloat(apostador.saldo) + parseFloat(a.premioPotencial)).toFixed(2);
          }
        } else {
          a.status = 'PERDEDORA';
          perdedoras++;
        }
      });

      this.saveDb(db);
      return { sucesso: true, apostasApuradas: apuradas, vencedoras, perdedoras };
    }

    // Equipes
    if (endpoint === '/equipes' && method === 'GET') {
      return db.equipes;
    }

    if (endpoint === '/equipes' && method === 'POST') {
      if (!usuarioAtual || usuarioAtual.perfil !== 'ADMINISTRADOR') throw new Error('Acesso negado.');
      const nova = { id: db.equipes.length + 1, nome: body.nome.trim() };
      db.equipes.push(nova);
      this.saveDb(db);
      return nova;
    }

    // Apostas Cotar & Confirmar (Fórmula de Laplace)
    if (endpoint === '/apostas/cotar' || endpoint === '/apostas/confirmar') {
      if (!usuarioAtual || usuarioAtual.perfil !== 'APOSTADOR') throw new Error('Acesso negado.');
      const pid = parseInt(body.partidaId);
      const partida = db.partidas.find(x => x.id === pid);
      if (!partida) throw new Error('Partida não encontrada.');

      const valor = parseFloat(body.valor);
      const saldo = parseFloat(usuarioAtual.saldo || 0);
      if (valor <= 0) throw new Error('Valor inválido.');
      if (valor > saldo) throw new Error('Saldo insuficiente.');

      // Laplace
      let n = 0, N = 8, K = 3;
      if (body.tipo === 'EQUIPE_VENCEDORA') {
        K = 3;
        n = 4; // histórico padrão de exemplo da UFMG
      } else if (body.tipo === 'PLACAR_EXATO') {
        K = 100;
        n = 1;
      } else if (body.tipo === 'CARTOES_AMARELOS') {
        K = 11;
        n = 1;
      }

      const p = (n + 1) / (N + K);
      const odd = Math.max(1.01, 0.90 / p);
      const premio = (valor * odd).toFixed(2);

      if (endpoint === '/apostas/cotar') {
        return {
          probabilidade: p.toFixed(4),
          premioPotencial: premio,
          saldoAtual: usuarioAtual.saldo
        };
      }

      // Confirmar
      usuarioAtual.saldo = (saldo - valor).toFixed(2);
      const novaAposta = {
        apostaId: db.apostas.length + 101,
        partidaId: pid,
        partida: partida.descricao,
        tipo: body.tipo,
        desfecho: body.desfecho,
        golsMandante: body.golsMandante,
        golsVisitante: body.golsVisitante,
        cartoesAmarelos: body.cartoesAmarelos,
        previsao: body.tipo === 'EQUIPE_VENCEDORA' ? `Vitória ${body.desfecho}` : body.tipo === 'PLACAR_EXATO' ? `${body.golsMandante} x ${body.golsVisitante}` : `${body.cartoesAmarelos} cartões`,
        valor: valor.toFixed(2),
        probabilidade: p.toFixed(4),
        premioPotencial: premio,
        status: 'ABERTA',
        criadaEm: new Date().toISOString(),
        apostadorId: usuarioAtual.id,
        apostador: usuarioAtual.nome
      };
      db.apostas.unshift(novaAposta);
      this.saveDb(db);
      return {
        ...novaAposta,
        descricaoPartida: partida.descricao,
        saldoRestante: usuarioAtual.saldo
      };
    }

    // Histórico
    if (endpoint === '/apostas/historico') {
      if (!usuarioAtual) throw new Error('Não autenticado.');
      return db.apostas.filter(a => a.apostadorId === usuarioAtual.id);
    }

    // Relatório Partida
    const matchRel = endpoint.match(/^\/relatorios\/partida\/(\d+)$/);
    if (matchRel) {
      const pid = parseInt(matchRel[1]);
      const p = db.partidas.find(x => x.id === pid);
      const apostasPartida = db.apostas.filter(x => x.partidaId === pid);
      const totalVolume = apostasPartida.reduce((acc, a) => acc + parseFloat(a.valor), 0).toFixed(2);
      const totalPremios = apostasPartida.filter(a => a.status === 'VENCEDORA').reduce((acc, a) => acc + parseFloat(a.premioPotencial), 0).toFixed(2);
      return {
        partidaId: pid,
        partida: p ? p.descricao : 'Partida ' + pid,
        quantidade: apostasPartida.length,
        valorTotal: totalVolume,
        totalPremiosPagos: totalPremios,
        apostas: apostasPartida.map(a => ({
          id: a.apostaId,
          apostador: a.apostador || 'Apostador',
          tipo: a.tipo,
          previsao: a.previsao,
          valor: a.valor,
          premioPotencial: a.premioPotencial,
          status: a.status
        }))
      };
    }

    // Relatório Apostador
    if (endpoint.startsWith('/relatorios/apostador')) {
      const apostasUser = db.apostas;
      const totalVolume = apostasUser.reduce((acc, a) => acc + parseFloat(a.valor), 0).toFixed(2);
      const totalPremios = apostasUser.filter(a => a.status === 'VENCEDORA').reduce((acc, a) => acc + parseFloat(a.premioPotencial), 0).toFixed(2);
      return {
        apostadorId: 2,
        nome: 'Carlos Apostador',
        cpf: '529.982.247-25',
        quantidade: apostasUser.length,
        valorTotal: totalVolume,
        abertas: apostasUser.filter(a => a.status === 'ABERTA').length,
        vencedoras: apostasUser.filter(a => a.status === 'VENCEDORA').length,
        perdedoras: apostasUser.filter(a => a.status === 'PERDEDORA').length,
        totalPremiosRecebidos: totalPremios
      };
    }

    // Auditoria
    if (endpoint === '/auditoria') {
      return db.auditoria;
    }

    return {};
  }
}
