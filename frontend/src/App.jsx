import React, { useState, useEffect } from 'react';
import { AuthProvider, useAuth } from './context/AuthContext';
import Navbar from './components/Navbar';
import MatchCard from './components/MatchCard';
import BetModal from './components/BetModal';
import DepositModal from './components/DepositModal';
import AuthModal from './components/AuthModal';
import HistoryView from './components/HistoryView';
import AdminPanel from './components/AdminPanel';
import { ApiClient } from './api';
import { Flame, Trophy, Shield, Calculator, CheckCircle2, AlertTriangle, Sparkles } from 'lucide-react';

function MainContent() {
  const { user } = useAuth();
  const [activeTab, setActiveTab] = useState('partidas'); // 'partidas' | 'historico' | 'admin'
  const [partidas, setPartidas] = useState([]);
  const [filtroPartida, setFiltroPartida] = useState('TODAS'); // 'TODAS' | 'AGENDADA' | 'ENCERRADA'
  const [selectedMatchForBet, setSelectedMatchForBet] = useState(null);
  const [selectedMatchForResult, setSelectedMatchForResult] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    carregarPartidas();
  }, []);

  const carregarPartidas = async () => {
    setLoading(true);
    try {
      const data = await ApiClient.listarPartidas();
      setPartidas(data || []);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const partidasFiltradas = partidas.filter((p) => {
    if (filtroPartida === 'TODAS') return true;
    return p.status === filtroPartida;
  });

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col font-sans">
      <Navbar activeTab={activeTab} setActiveTab={setActiveTab} />

      <main className="flex-1">
        {/* TAB 1: MURAL DE PARTIDAS */}
        {activeTab === 'partidas' && (
          <div>
            {/* HERO BANNER */}
            <section className="relative overflow-hidden border-b border-slate-800 bg-gradient-to-b from-slate-900/50 via-slate-950 to-slate-950 py-12 sm:py-16">
              <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 relative z-10">
                <div className="max-w-3xl">
                  
                  <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 text-xs font-bold mb-4">
                    <Sparkles className="w-3.5 h-3.5" />
                    <span>Sprint 3 e 4 — Engenharia de Software (UFMG)</span>
                  </div>

                  <h1 className="text-3xl sm:text-5xl font-black tracking-tight text-white leading-tight">
                    Apostas Esportivas com{' '}
                    <span className="bg-gradient-to-r from-emerald-400 to-teal-300 bg-clip-text text-transparent">
                      Probabilidade Matemática
                    </span>
                  </h1>

                  <p className="mt-4 text-sm sm:text-base text-slate-400 leading-relaxed">
                    Motor de cálculo com Distribuição de Poisson, histórico geral recente e médias globais da liga como prior,
                    prêmio proporcional com gestão de risco (piso de 1.01, tetos de odds por mercado e teto máximo de pagamento),
                    integridade transacional na apuração e controle rigoroso de apostas.
                  </p>

                  {/* Highlights Grid */}
                  <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 mt-8">
                    <div className="bg-slate-900/80 border border-slate-800 rounded-2xl p-3">
                      <span className="text-[10px] uppercase font-bold text-slate-400">3 Tipos de Aposta</span>
                      <p className="text-xs font-bold text-white mt-0.5">Vencedor, Placar e Cartões</p>
                    </div>

                    <div className="bg-slate-900/80 border border-slate-800 rounded-2xl p-3">
                      <span className="text-[10px] uppercase font-bold text-slate-400">Distribuição de Poisson</span>
                      <p className="text-xs font-bold text-emerald-400 mt-0.5">Odds realistas e calibradas</p>
                    </div>

                    <div className="bg-slate-900/80 border border-slate-800 rounded-2xl p-3">
                      <span className="text-[10px] uppercase font-bold text-slate-400">Segurança & Regras</span>
                      <p className="text-xs font-bold text-white mt-0.5">Max 10 apostas abertas</p>
                    </div>

                    <div className="bg-slate-900/80 border border-slate-800 rounded-2xl p-3">
                      <span className="text-[10px] uppercase font-bold text-slate-400">Apuração Atômica</span>
                      <p className="text-xs font-bold text-amber-400 mt-0.5">Reversão em falhas</p>
                    </div>
                  </div>

                </div>
              </div>
            </section>

            {/* MATCHES SECTION */}
            <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
              
              {/* Filter Tabs */}
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-8">
                <div>
                  <h2 className="text-xl font-black text-white flex items-center gap-2">
                    <Flame className="w-5 h-5 text-emerald-400" />
                    Mural de Jogos
                  </h2>
                  <p className="text-xs text-slate-400">Partidas ativas para apostar ou consultar placares.</p>
                </div>

                <div className="flex items-center gap-1.5 bg-slate-900 border border-slate-800 p-1.5 rounded-2xl">
                  {[
                    { id: 'TODAS', label: 'Todas as Partidas' },
                    { id: 'AGENDADA', label: 'Abertas p/ Apostas' },
                    { id: 'ENCERRADA', label: 'Finalizadas' }
                  ].map((f) => (
                    <button
                      key={f.id}
                      onClick={() => setFiltroPartida(f.id)}
                      className={`px-3 py-1.5 rounded-xl text-xs font-bold transition ${
                        filtroPartida === f.id
                          ? 'bg-emerald-500 text-slate-950 shadow-sm'
                          : 'text-slate-400 hover:text-white'
                      }`}
                    >
                      {f.label}
                    </button>
                  ))}
                </div>
              </div>

              {/* Grid of Matches */}
              {loading ? (
                <div className="py-20 text-center text-slate-500 text-sm animate-pulse">
                  Carregando partidas esportivas...
                </div>
              ) : partidasFiltradas.length === 0 ? (
                <div className="bg-slate-900 border border-slate-800 rounded-3xl p-12 text-center space-y-3">
                  <Trophy className="w-12 h-12 text-slate-600 mx-auto" />
                  <h3 className="text-base font-bold text-white">Nenhuma partida disponível</h3>
                  <p className="text-xs text-slate-400 max-w-sm mx-auto">
                    Não encontramos partidas para o filtro selecionado. Use o painel administrativo para cadastrar novos jogos.
                  </p>
                </div>
              ) : (
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
                  {partidasFiltradas.map((partida) => (
                    <MatchCard
                      key={partida.id}
                      partida={partida}
                      onOpenBet={(p) => setSelectedMatchForBet(p)}
                      onOpenResult={(p) => setActiveTab('admin')}
                    />
                  ))}
                </div>
              )}

            </section>
          </div>
        )}

        {/* TAB 2: MINHAS APOSTAS (HISTÓRICO) */}
        {activeTab === 'historico' && <HistoryView />}

        {/* TAB 3: PAINEL ADMINISTRATIVO */}
        {activeTab === 'admin' && (
          <AdminPanel onMatchesUpdated={carregarPartidas} />
        )}
      </main>

      {/* FOOTER */}
      <footer className="border-t border-slate-800 bg-slate-950 py-8 text-center text-xs text-slate-400">
        <div className="max-w-7xl mx-auto px-4 space-y-2">
          <p className="font-semibold text-slate-300">
            GigaBet — Sistema para Gestão de Apostas Esportivas
          </p>
          <p className="text-[11px] text-slate-400">
            DCC603: Engenharia de Software — Universidade Federal de Minas Gerais (UFMG) • Grupo 8
          </p>
          <p className="text-[10px] text-slate-500">
            Regras implementadas: UC01 a UC13, RN01 a RN08, Distribuição de Poisson, Gestão de Risco com Tetos e Transações Atômicas de Apuração.
          </p>
        </div>
      </footer>

      {/* MODALS */}
      <BetModal
        partida={selectedMatchForBet}
        isOpen={Boolean(selectedMatchForBet)}
        onClose={() => setSelectedMatchForBet(null)}
        onBetPlaced={() => {
          carregarPartidas();
        }}
      />

      <DepositModal />
      <AuthModal />

    </div>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <MainContent />
    </AuthProvider>
  );
}
