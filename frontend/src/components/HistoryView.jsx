import React, { useState, useEffect } from 'react';
import { ApiClient } from '../api';
import { History, TrendingUp, CheckCircle, XCircle, Clock, Filter, ArrowUpRight, Award } from 'lucide-react';

export default function HistoryView() {
  const [apostas, setApostas] = useState([]);
  const [filtro, setFiltro] = useState('TODAS');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    carregarHistorico();
  }, []);

  const carregarHistorico = async () => {
    setLoading(true);
    try {
      const data = await ApiClient.historicoApostas();
      setApostas(data || []);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const filtradas = apostas.filter((a) => {
    if (filtro === 'TODAS') return true;
    return a.status === filtro;
  });

  const totalApostado = apostas.reduce((acc, a) => acc + parseFloat(a.valor || 0), 0);
  const totalGanhos = apostas
    .filter((a) => a.status === 'VENCEDORA')
    .reduce((acc, a) => acc + parseFloat(a.premioPotencial || 0), 0);
  const abertasCount = apostas.filter((a) => a.status === 'ABERTA').length;
  const lucroLiquido = totalGanhos - totalApostado;

  return (
    <div className="max-w-6xl mx-auto px-4 py-8 space-y-6">
      
      {/* Page Title */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-black text-white flex items-center gap-2">
            <History className="w-6 h-6 text-emerald-400" />
            Minhas Apostas
          </h2>
          <p className="text-xs text-slate-400">Acompanhe seu desempenho e status de palpites.</p>
        </div>

        {/* Filter Chips */}
        <div className="flex items-center gap-1.5 bg-slate-900 border border-slate-800 p-1.5 rounded-2xl">
          {['TODAS', 'ABERTA', 'VENCEDORA', 'PERDEDORA'].map((f) => (
            <button
              key={f}
              onClick={() => setFiltro(f)}
              className={`px-3 py-1.5 rounded-xl text-xs font-bold transition ${
                filtro === f
                  ? 'bg-emerald-500 text-slate-950 shadow-sm'
                  : 'text-slate-400 hover:text-white'
              }`}
            >
              {f === 'TODAS' ? 'Todas' : f === 'ABERTA' ? 'Abertas' : f === 'VENCEDORA' ? 'Vencedoras' : 'Perdedoras'}
            </button>
          ))}
        </div>
      </div>

      {/* Metric Cards Banner */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-4">
          <span className="text-[11px] text-slate-400 font-medium">Total Apostado</span>
          <p className="text-xl font-black text-white mt-1">
            R$ {totalApostado.toFixed(2).replace('.', ',')}
          </p>
        </div>

        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-4">
          <span className="text-[11px] text-slate-400 font-medium">Prêmios Recebidos</span>
          <p className="text-xl font-black text-emerald-400 mt-1">
            R$ {totalGanhos.toFixed(2).replace('.', ',')}
          </p>
        </div>

        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-4">
          <span className="text-[11px] text-slate-400 font-medium">Balanço Líquido</span>
          <p className={`text-xl font-black mt-1 ${lucroLiquido >= 0 ? 'text-emerald-400' : 'text-rose-400'}`}>
            {lucroLiquido >= 0 ? '+' : ''}R$ {lucroLiquido.toFixed(2).replace('.', ',')}
          </p>
        </div>

        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-4">
          <span className="text-[11px] text-slate-400 font-medium">Apostas em Aberto</span>
          <p className="text-xl font-black text-amber-400 mt-1">
            {abertasCount} <span className="text-xs font-normal text-slate-500">/ 10 máx</span>
          </p>
        </div>
      </div>

      {/* Bets List */}
      {loading ? (
        <div className="py-16 text-center text-slate-400 text-sm animate-pulse">
          Carregando histórico de apostas...
        </div>
      ) : filtradas.length === 0 ? (
        <div className="bg-slate-900 border border-slate-800 rounded-3xl p-12 text-center space-y-3">
          <div className="w-12 h-12 rounded-full bg-slate-800 flex items-center justify-center mx-auto text-slate-500">
            <History className="w-6 h-6" />
          </div>
          <h4 className="text-base font-bold text-slate-200">Nenhuma aposta encontrada</h4>
          <p className="text-xs text-slate-400 max-w-sm mx-auto">
            Você ainda não registrou palpites com este filtro. Acesse o mural de partidas para apostar.
          </p>
        </div>
      ) : (
        <div className="space-y-3">
          {filtradas.map((a) => {
            const dataCriada = new Date(a.criadaEm).toLocaleString('pt-BR', {
              day: '2-digit',
              month: '2-digit',
              hour: '2-digit',
              minute: '2-digit'
            });

            return (
              <div
                key={a.apostaId}
                className="bg-slate-900 border border-slate-800 hover:border-slate-700/80 rounded-2xl p-5 shadow-sm transition flex flex-col md:flex-row md:items-center justify-between gap-4"
              >
                <div className="space-y-1">
                  <div className="flex items-center gap-2">
                    <span className="text-xs font-bold text-slate-400">#{a.apostaId}</span>
                    <span className="text-sm font-extrabold text-white">{a.partida}</span>
                    <span className="text-[10px] text-slate-500">• {dataCriada}</span>
                  </div>

                  <div className="flex flex-wrap items-center gap-2 text-xs">
                    <span className="px-2 py-0.5 rounded bg-slate-800 text-slate-300 font-semibold border border-slate-700/50">
                      {a.tipo === 'EQUIPE_VENCEDORA' ? 'Vencedor' : a.tipo === 'PLACAR_EXATO' ? 'Placar Exato' : 'Cartões'}
                    </span>
                    <span className="text-emerald-400 font-bold">{a.previsao}</span>
                    <span className="text-slate-500">|</span>
                    <span className="text-slate-400">Prob. Poisson: {(parseFloat(a.probabilidade || 0) * 100).toFixed(1)}%</span>
                  </div>
                </div>

                <div className="flex items-center justify-between md:justify-end gap-6 border-t md:border-t-0 border-slate-800 pt-3 md:pt-0">
                  <div className="text-left md:text-right">
                    <span className="text-[10px] text-slate-400 block">Valor Apostado</span>
                    <span className="text-sm font-bold text-white">R$ {parseFloat(a.valor).toFixed(2).replace('.', ',')}</span>
                  </div>

                  <div className="text-left md:text-right">
                    <span className="text-[10px] text-slate-400 block">Prêmio Potencial</span>
                    <span className="text-base font-black text-emerald-400">
                      R$ {parseFloat(a.premioPotencial).toFixed(2).replace('.', ',')}
                    </span>
                  </div>

                  <div>
                    {a.status === 'ABERTA' && (
                      <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-amber-500/10 text-amber-400 border border-amber-500/30">
                        <Clock className="w-3.5 h-3.5" />
                        Aberta
                      </span>
                    )}
                    {a.status === 'VENCEDORA' && (
                      <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-emerald-500/10 text-emerald-400 border border-emerald-500/30">
                        <CheckCircle className="w-3.5 h-3.5" />
                        Ganhou!
                      </span>
                    )}
                    {a.status === 'PERDEDORA' && (
                      <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-slate-800 text-slate-400 border border-slate-700">
                        <XCircle className="w-3.5 h-3.5" />
                        Perdeu
                      </span>
                    )}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

    </div>
  );
}
