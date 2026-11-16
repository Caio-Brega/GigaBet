import React, { useState, useEffect } from 'react';
import { ApiClient } from '../api';
import { 
  LayoutDashboard, PlusCircle, CheckCircle, BarChart3, 
  Users, ShieldAlert, AlertCircle, FileText, Calendar, Shield 
} from 'lucide-react';

export default function AdminPanel({ onMatchesUpdated }) {
  const [subTab, setSubTab] = useState('partidas'); // 'partidas' | 'equipes' | 'rel-partida' | 'rel-apostador' | 'auditoria'
  
  // Data states
  const [equipes, setEquipes] = useState([]);
  const [partidas, setPartidas] = useState([]);
  const [auditoriaLogs, setAuditoriaLogs] = useState([]);

  // Form: Nova Equipe
  const [nomeEquipe, setNomeEquipe] = useState('');
  
  // Form: Nova Partida
  const [mandante, setMandante] = useState('');
  const [visitante, setVisitante] = useState('');
  const [inicioData, setInicioData] = useState('');
  const [inicioHora, setInicioHora] = useState('');

  // Form: Registrar Resultado Modal
  const [partidaParaApurar, setPartidaParaApurar] = useState(null);
  const [golsM, setGolsM] = useState(0);
  const [golsV, setGolsV] = useState(0);
  const [cartoes, setCartoes] = useState(0);
  const [resumoApuracao, setResumoApuracao] = useState(null);

  // Relatórios
  const [partidaIdRelatorio, setPartidaIdRelatorio] = useState('');
  const [dadosRelatorioPartida, setDadosRelatorioPartida] = useState(null);
  const [buscaApostador, setBuscaApostador] = useState('Carlos Apostador');
  const [dadosRelatorioApostador, setDadosRelatorioApostador] = useState(null);

  const [loading, setLoading] = useState(false);
  const [mensagem, setMensagem] = useState({ tipo: '', texto: '' });

  useEffect(() => {
    carregarTudo();
  }, []);

  const carregarTudo = async () => {
    try {
      const [eqs, parts, aud] = await Promise.all([
        ApiClient.listarEquipes().catch(() => []),
        ApiClient.listarPartidas().catch(() => []),
        ApiClient.listarAuditoria().catch(() => [])
      ]);
      setEquipes(eqs || []);
      setPartidas(parts || []);
      setAuditoriaLogs(aud || []);
      if (eqs && eqs.length >= 2) {
        setMandante(eqs[0].nome);
        setVisitante(eqs[1].nome);
      }
      if (parts && parts.length > 0) {
        setPartidaIdRelatorio(parts[0].id.toString());
      }
    } catch (e) {
      console.error(e);
    }
  };

  const handleCadastrarEquipe = async (e) => {
    e.preventDefault();
    if (!nomeEquipe.trim()) return;
    setLoading(true);
    setMensagem({ tipo: '', texto: '' });
    try {
      await ApiClient.cadastrarEquipe(nomeEquipe.trim());
      setNomeEquipe('');
      setMensagem({ tipo: 'sucesso', texto: `Equipe cadastrada com sucesso!` });
      await carregarTudo();
    } catch (err) {
      setMensagem({ tipo: 'erro', texto: err.message });
    } finally {
      setLoading(false);
    }
  };

  const handleCadastrarPartida = async (e) => {
    e.preventDefault();
    if (!mandante || !visitante || !inicioData || !inicioHora) {
      setMensagem({ tipo: 'erro', texto: 'Preencha todos os campos da partida.' });
      return;
    }
    if (mandante.toLowerCase() === visitante.toLowerCase()) {
      setMensagem({ tipo: 'erro', texto: 'Mandante e visitante devem ser equipes diferentes.' });
      return;
    }

    setLoading(true);
    setMensagem({ tipo: '', texto: '' });
    try {
      const dataIso = new Date(`${inicioData}T${inicioHora}:00`).toISOString();
      await ApiClient.cadastrarPartida({ mandante, visitante, inicio: dataIso });
      setMensagem({ tipo: 'sucesso', texto: 'Partida agendada com sucesso!' });
      await carregarTudo();
      if (onMatchesUpdated) onMatchesUpdated();
    } catch (err) {
      setMensagem({ tipo: 'erro', texto: err.message });
    } finally {
      setLoading(false);
    }
  };

  const handleApurarResultado = async (e) => {
    e.preventDefault();
    if (!partidaParaApurar) return;
    setLoading(true);
    setMensagem({ tipo: '', texto: '' });
    try {
      const res = await ApiClient.registrarResultado(partidaParaApurar.id, {
        golsMandante: golsM,
        golsVisitante: golsV,
        cartoesAmarelos: cartoes
      });
      setResumoApuracao(res);
      await carregarTudo();
      if (onMatchesUpdated) onMatchesUpdated();
    } catch (err) {
      setMensagem({ tipo: 'erro', texto: err.message });
    } finally {
      setLoading(false);
    }
  };

  const carregarRelatorioPartida = async (id) => {
    if (!id) return;
    setLoading(true);
    try {
      const data = await ApiClient.relatorioPartida(id);
      setDadosRelatorioPartida(data);
    } catch (err) {
      setDadosRelatorioPartida(null);
    } finally {
      setLoading(false);
    }
  };

  const carregarRelatorioApostador = async (e) => {
    if (e) e.preventDefault();
    if (!buscaApostador.trim()) return;
    setLoading(true);
    try {
      const data = await ApiClient.relatorioApostador(buscaApostador.trim());
      setDadosRelatorioApostador(data);
    } catch (err) {
      setDadosRelatorioApostador(null);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-6xl mx-auto px-4 py-8 space-y-6">
      
      {/* Title */}
      <div className="flex items-center justify-between pb-4 border-b border-slate-800">
        <div>
          <div className="flex items-center gap-2">
            <Shield className="w-6 h-6 text-amber-400" />
            <h2 className="text-2xl font-black text-white">Painel Administrativo</h2>
            <span className="text-[10px] uppercase font-bold tracking-wider px-2 py-0.5 rounded bg-amber-500/10 text-amber-400 border border-amber-500/20">
              Admin Master
            </span>
          </div>
          <p className="text-xs text-slate-400">Gerenciamento de equipes, partidas, apuração transacional e relatórios.</p>
        </div>
      </div>

      {/* Sub Tabs */}
      <div className="flex flex-wrap gap-2 border-b border-slate-800 pb-3">
        {[
          { id: 'partidas', label: 'Partidas e Apuração', icon: Calendar },
          { id: 'equipes', label: 'Cadastrar Equipes', icon: PlusCircle },
          { id: 'rel-partida', label: 'Relatório por Partida', icon: BarChart3 },
          { id: 'rel-apostador', label: 'Relatório por Apostador', icon: Users },
          { id: 'auditoria', label: 'Auditoria do Sistema', icon: ShieldAlert }
        ].map((tab) => {
          const Icon = tab.icon;
          return (
            <button
              key={tab.id}
              onClick={() => { setSubTab(tab.id); setMensagem({ tipo: '', texto: '' }); }}
              className={`px-3.5 py-2 rounded-xl text-xs font-bold transition flex items-center gap-2 ${
                subTab === tab.id
                  ? 'bg-amber-500 text-slate-950 shadow-md shadow-amber-500/20'
                  : 'bg-slate-900 text-slate-400 hover:text-white border border-slate-800'
              }`}
            >
              <Icon className="w-4 h-4" />
              {tab.label}
            </button>
          );
        })}
      </div>

      {/* Feedback Messages */}
      {mensagem.texto && (
        <div className={`p-4 rounded-xl text-xs font-bold flex items-center gap-2 ${
          mensagem.tipo === 'sucesso' ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20' : 'bg-rose-500/10 text-rose-400 border border-rose-500/20'
        }`}>
          {mensagem.tipo === 'sucesso' ? <CheckCircle className="w-4 h-4" /> : <AlertCircle className="w-4 h-4" />}
          <span>{mensagem.texto}</span>
        </div>
      )}

      {/* TAB 1: PARTIDAS & APURAÇÃO */}
      {subTab === 'partidas' && (
        <div className="space-y-6">
          {/* Card: Agendar Nova Partida */}
          <div className="bg-slate-900 border border-slate-800 rounded-3xl p-6">
            <h3 className="text-base font-bold text-white mb-4 flex items-center gap-2">
              <PlusCircle className="w-5 h-5 text-amber-400" />
              Agendar Nova Partida
            </h3>

            <form onSubmit={handleCadastrarPartida} className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
              <div>
                <label className="block text-xs font-semibold text-slate-400 mb-1">Mandante</label>
                <select
                  value={mandante}
                  onChange={(e) => setMandante(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2.5 text-xs text-white focus:outline-none focus:border-amber-500"
                >
                  {equipes.map((e) => (
                    <option key={e.id} value={e.nome}>{e.nome}</option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-400 mb-1">Visitante</label>
                <select
                  value={visitante}
                  onChange={(e) => setVisitante(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2.5 text-xs text-white focus:outline-none focus:border-amber-500"
                >
                  {equipes.map((e) => (
                    <option key={e.id} value={e.nome}>{e.nome}</option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-400 mb-1">Data</label>
                <input
                  type="date"
                  required
                  value={inicioData}
                  onChange={(e) => setInicioData(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-amber-500"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-400 mb-1">Horário (Brasília)</label>
                <input
                  type="time"
                  required
                  value={inicioHora}
                  onChange={(e) => setInicioHora(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-amber-500"
                />
              </div>

              <div className="sm:col-span-2 lg:col-span-4 flex justify-end">
                <button
                  type="submit"
                  disabled={loading}
                  className="px-6 py-2.5 bg-amber-500 hover:bg-amber-400 text-slate-950 font-bold text-xs rounded-xl shadow-md shadow-amber-500/20 transition"
                >
                  Confirmar Agendamento
                </button>
              </div>
            </form>
          </div>

          {/* Lista de Partidas com Ação de Apurar */}
          <div className="bg-slate-900 border border-slate-800 rounded-3xl p-6">
            <h3 className="text-base font-bold text-white mb-4">Partidas Cadastradas no Sistema</h3>
            <div className="space-y-3">
              {partidas.map((p) => (
                <div
                  key={p.id}
                  className="bg-slate-950 border border-slate-800 rounded-2xl p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-3"
                >
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="text-xs font-bold text-slate-400">#{p.id}</span>
                      <span className="text-sm font-extrabold text-white">{p.descricao}</span>
                    </div>
                    <span className="text-xs text-slate-400 block mt-0.5">
                      {new Date(p.inicio).toLocaleString('pt-BR')} • Status: <strong className="text-amber-400">{p.status}</strong>
                    </span>
                  </div>

                  <div>
                    {p.status !== 'ENCERRADA' ? (
                      <button
                        onClick={() => {
                          setPartidaParaApurar(p);
                          setResumoApuracao(null);
                        }}
                        className="px-4 py-2 bg-amber-500 hover:bg-amber-400 text-slate-950 font-bold text-xs rounded-xl transition"
                      >
                        Lançar Resultado & Apurar
                      </button>
                    ) : (
                      <span className="text-xs font-bold text-slate-500 px-3 py-1 bg-slate-900 rounded-lg border border-slate-800">
                        Resultado: {p.golsMandante} x {p.golsVisitante} (🟨 {p.cartoesAmarelos})
                      </span>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* TAB 2: CADASTRAR EQUIPES */}
      {subTab === 'equipes' && (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div className="bg-slate-900 border border-slate-800 rounded-3xl p-6 md:col-span-1">
            <h3 className="text-base font-bold text-white mb-4 flex items-center gap-2">
              <PlusCircle className="w-5 h-5 text-amber-400" />
              Nova Equipe
            </h3>
            <form onSubmit={handleCadastrarEquipe} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-400 mb-1">Nome do Clube</label>
                <input
                  type="text"
                  required
                  placeholder="Ex: Vasco da Gama"
                  value={nomeEquipe}
                  onChange={(e) => setNomeEquipe(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-sm text-white focus:outline-none focus:border-amber-500"
                />
              </div>
              <button
                type="submit"
                disabled={loading}
                className="w-full py-2.5 bg-amber-500 hover:bg-amber-400 text-slate-950 font-bold text-xs rounded-xl transition"
              >
                Cadastrar Equipe
              </button>
            </form>
          </div>

          <div className="bg-slate-900 border border-slate-800 rounded-3xl p-6 md:col-span-2">
            <h3 className="text-base font-bold text-white mb-4">Equipes Cadastradas ({equipes.length})</h3>
            <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
              {equipes.map((e) => (
                <div key={e.id} className="bg-slate-950 border border-slate-800 rounded-xl p-3 flex items-center gap-2.5">
                  <div className="w-8 h-8 rounded-lg bg-slate-900 border border-slate-800 flex items-center justify-center font-bold text-xs text-amber-400">
                    #{e.id}
                  </div>
                  <span className="text-xs font-bold text-white truncate">{e.nome}</span>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* TAB 3: RELATÓRIO POR PARTIDA */}
      {subTab === 'rel-partida' && (
        <div className="bg-slate-900 border border-slate-800 rounded-3xl p-6 space-y-6">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <h3 className="text-base font-bold text-white">Relatório de Apostas por Partida</h3>
            <div className="flex items-center gap-2">
              <select
                value={partidaIdRelatorio}
                onChange={(e) => {
                  setPartidaIdRelatorio(e.target.value);
                  carregarRelatorioPartida(e.target.value);
                }}
                className="bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white"
              >
                <option value="">Selecione uma partida...</option>
                {partidas.map((p) => (
                  <option key={p.id} value={p.id}>#{p.id} - {p.descricao}</option>
                ))}
              </select>
              <button
                onClick={() => carregarRelatorioPartida(partidaIdRelatorio)}
                className="px-3 py-2 bg-slate-800 hover:bg-slate-700 text-white rounded-xl text-xs font-bold"
              >
                Atualizar
              </button>
            </div>
          </div>

          {dadosRelatorioPartida ? (
            <div className="space-y-6">
              <div className="grid grid-cols-3 gap-4">
                <div className="bg-slate-950 p-4 rounded-2xl border border-slate-800">
                  <span className="text-xs text-slate-400">Total de Apostas</span>
                  <p className="text-xl font-black text-white mt-1">{dadosRelatorioPartida.quantidade}</p>
                </div>
                <div className="bg-slate-950 p-4 rounded-2xl border border-slate-800">
                  <span className="text-xs text-slate-400">Volume Total Apostado</span>
                  <p className="text-xl font-black text-white mt-1">R$ {parseFloat(dadosRelatorioPartida.valorTotal || 0).toFixed(2).replace('.', ',')}</p>
                </div>
                <div className="bg-slate-950 p-4 rounded-2xl border border-slate-800">
                  <span className="text-xs text-slate-400">Total de Prêmios Pagos</span>
                  <p className="text-xl font-black text-emerald-400 mt-1">R$ {parseFloat(dadosRelatorioPartida.totalPremiosPagos || 0).toFixed(2).replace('.', ',')}</p>
                </div>
              </div>

              <div>
                <h4 className="text-xs font-bold text-slate-300 uppercase tracking-wider mb-3">Lista de Apostas Registradas</h4>
                {dadosRelatorioPartida.apostas?.length > 0 ? (
                  <div className="divide-y divide-slate-800 border border-slate-800 rounded-2xl overflow-hidden bg-slate-950">
                    {dadosRelatorioPartida.apostas.map((a) => (
                      <div key={a.id} className="p-3 text-xs flex justify-between items-center">
                        <div>
                          <strong className="text-white">{a.apostador}</strong>
                          <span className="text-slate-400 ml-2">[{a.previsao}]</span>
                        </div>
                        <div className="flex items-center gap-4">
                          <span className="text-slate-400">R$ {parseFloat(a.valor).toFixed(2)}</span>
                          <span className="font-bold text-emerald-400">Prêmio: R$ {parseFloat(a.premioPotencial).toFixed(2)}</span>
                          <span className="font-semibold text-slate-300">({a.status})</span>
                        </div>
                      </div>
                    ))}
                  </div>
                ) : (
                  <p className="text-xs text-slate-500">Nenhuma aposta registrada nesta partida até o momento.</p>
                )}
              </div>
            </div>
          ) : (
            <p className="text-xs text-slate-500">Selecione uma partida para visualizar as estatísticas.</p>
          )}
        </div>
      )}

      {/* TAB 4: RELATÓRIO POR APOSTADOR */}
      {subTab === 'rel-apostador' && (
        <div className="bg-slate-900 border border-slate-800 rounded-3xl p-6 space-y-6">
          <form onSubmit={carregarRelatorioApostador} className="flex gap-2 max-w-md">
            <input
              type="text"
              placeholder="Digite o Nome ou CPF do Apostador..."
              value={buscaApostador}
              onChange={(e) => setBuscaApostador(e.target.value)}
              className="flex-1 bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-amber-500"
            />
            <button
              type="submit"
              className="px-4 py-2 bg-amber-500 hover:bg-amber-400 text-slate-950 font-bold text-xs rounded-xl"
            >
              Buscar
            </button>
          </form>

          {dadosRelatorioApostador ? (
            <div className="space-y-6">
              <div className="bg-slate-950 p-5 rounded-2xl border border-slate-800">
                <h4 className="text-lg font-bold text-white">{dadosRelatorioApostador.nome}</h4>
                <p className="text-xs text-slate-400">CPF: {dadosRelatorioApostador.cpf}</p>
              </div>

              <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
                <div className="bg-slate-950 p-4 rounded-xl border border-slate-800">
                  <span className="text-[11px] text-slate-400">Total Apostas</span>
                  <p className="text-lg font-bold text-white mt-1">{dadosRelatorioApostador.quantidade}</p>
                </div>
                <div className="bg-slate-950 p-4 rounded-xl border border-slate-800">
                  <span className="text-[11px] text-slate-400">Em Aberto</span>
                  <p className="text-lg font-bold text-amber-400 mt-1">{dadosRelatorioApostador.abertas}</p>
                </div>
                <div className="bg-slate-950 p-4 rounded-xl border border-slate-800">
                  <span className="text-[11px] text-slate-400">Vencedoras</span>
                  <p className="text-lg font-bold text-emerald-400 mt-1">{dadosRelatorioApostador.vencedoras}</p>
                </div>
                <div className="bg-slate-950 p-4 rounded-xl border border-slate-800">
                  <span className="text-[11px] text-slate-400">Total Premiado</span>
                  <p className="text-lg font-bold text-emerald-400 mt-1">R$ {parseFloat(dadosRelatorioApostador.totalPremiosRecebidos || 0).toFixed(2).replace('.', ',')}</p>
                </div>
              </div>
            </div>
          ) : (
            <p className="text-xs text-slate-500">Nenhum apostador carregado. Faça uma busca por nome ou CPF.</p>
          )}
        </div>
      )}

      {/* TAB 5: AUDITORIA DO SISTEMA */}
      {subTab === 'auditoria' && (
        <div className="bg-slate-900 border border-slate-800 rounded-3xl p-6">
          <h3 className="text-base font-bold text-white mb-4 flex items-center gap-2">
            <ShieldAlert className="w-5 h-5 text-amber-400" />
            Trilha de Auditoria Transacional
          </h3>
          <div className="divide-y divide-slate-800 border border-slate-800 rounded-2xl overflow-hidden bg-slate-950">
            {auditoriaLogs.map((log) => (
              <div key={log.id} className="p-3 text-xs flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                <div>
                  <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-amber-500/10 text-amber-400 border border-amber-500/20 mr-2">
                    {log.tipo}
                  </span>
                  <span className="text-slate-200">{log.detalhes}</span>
                </div>
                <span className="text-[10px] text-slate-500 shrink-0">
                  {new Date(log.quando).toLocaleString('pt-BR')}
                </span>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* MODAL: REGISTRAR RESULTADO & APURAR */}
      {partidaParaApurar && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm animate-in fade-in duration-200">
          <div className="bg-slate-900 border border-slate-800 rounded-3xl p-6 max-w-md w-full space-y-4">
            <h3 className="text-lg font-bold text-white">Lançar Resultado e Apurar</h3>
            <p className="text-xs text-slate-400">{partidaParaApurar.descricao}</p>

            {resumoApuracao ? (
              <div className="bg-slate-950 border border-slate-800 rounded-2xl p-4 text-center space-y-2">
                <CheckCircle className="w-8 h-8 text-emerald-400 mx-auto" />
                <h4 className="text-sm font-bold text-white">Apuração Concluída com Sucesso!</h4>
                <p className="text-xs text-slate-300">
                  Apostas Apuradas: <strong>{resumoApuracao.apostasApuradas}</strong>
                </p>
                <p className="text-xs text-emerald-400">
                  Vencedoras: <strong>{resumoApuracao.vencedoras}</strong>
                </p>
                <p className="text-xs text-slate-400">
                  Perdedoras: <strong>{resumoApuracao.perdedoras}</strong>
                </p>
                <button
                  onClick={() => setPartidaParaApurar(null)}
                  className="w-full mt-3 py-2 bg-emerald-500 text-slate-950 font-bold text-xs rounded-xl"
                >
                  Fechar
                </button>
              </div>
            ) : (
              <form onSubmit={handleApurarResultado} className="space-y-4">
                <div className="flex items-center justify-between gap-4">
                  <div className="text-center flex-1">
                    <label className="text-xs text-slate-400 block mb-1 truncate">{partidaParaApurar.mandante}</label>
                    <input
                      type="number"
                      min="0"
                      value={golsM}
                      onChange={(e) => setGolsM(parseInt(e.target.value) || 0)}
                      className="w-16 h-12 bg-slate-950 border border-slate-800 rounded-xl text-center font-bold text-xl text-white mx-auto block"
                    />
                  </div>
                  <span className="text-xl font-bold text-slate-600 mt-5">×</span>
                  <div className="text-center flex-1">
                    <label className="text-xs text-slate-400 block mb-1 truncate">{partidaParaApurar.visitante}</label>
                    <input
                      type="number"
                      min="0"
                      value={golsV}
                      onChange={(e) => setGolsV(parseInt(e.target.value) || 0)}
                      className="w-16 h-12 bg-slate-950 border border-slate-800 rounded-xl text-center font-bold text-xl text-white mx-auto block"
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-400 mb-1">Total de Cartões Amarelos na Partida</label>
                  <input
                    type="number"
                    min="0"
                    value={cartoes}
                    onChange={(e) => setCartoes(parseInt(e.target.value) || 0)}
                    className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-sm text-white"
                  />
                </div>

                <div className="flex gap-2 pt-2">
                  <button
                    type="button"
                    onClick={() => setPartidaParaApurar(null)}
                    className="flex-1 py-2.5 bg-slate-800 hover:bg-slate-700 text-slate-300 font-bold text-xs rounded-xl"
                  >
                    Cancelar
                  </button>
                  <button
                    type="submit"
                    disabled={loading}
                    className="flex-1 py-2.5 bg-amber-500 hover:bg-amber-400 text-slate-950 font-bold text-xs rounded-xl"
                  >
                    Confirmar Encerramento
                  </button>
                </div>
              </form>
            )}
          </div>
        </div>
      )}

    </div>
  );
}
