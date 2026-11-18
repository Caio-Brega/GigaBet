import React from 'react';
import { Calendar, Clock, Trophy, Flame, CheckCircle, ShieldAlert } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export default function MatchCard({ partida, onOpenBet, onOpenResult }) {
  const { user } = useAuth();

  const dataInicio = new Date(partida.inicio);
  const dataFormatada = dataInicio.toLocaleDateString('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric'
  });
  const horaFormatada = dataInicio.toLocaleTimeString('pt-BR', {
    hour: '2-digit',
    minute: '2-digit'
  });

  const isAgendada = partida.status === 'AGENDADA';
  const isAndamento = partida.status === 'EM_ANDAMENTO';
  const isEncerrada = partida.status === 'ENCERRADA';

  return (
    <div className="bg-slate-900 border border-slate-800 hover:border-slate-700/80 rounded-2xl p-5 shadow-lg transition-all flex flex-col justify-between group">
      
      {/* Top Bar: Status & Time */}
      <div className="flex items-center justify-between gap-2 mb-4 pb-3 border-b border-slate-800/80">
        <div className="flex items-center gap-1.5 text-xs text-slate-400 font-medium">
          <Calendar className="w-3.5 h-3.5 text-slate-500" />
          <span>{dataFormatada} às {horaFormatada}</span>
          {partida.externalId && (
            <span className="text-[10px] font-bold px-1.5 py-0.5 bg-blue-500/10 text-blue-400 border border-blue-500/20 rounded">
              ESPN
            </span>
          )}
        </div>

        <div>
          {isAgendada && (
            <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-400"></span>
              Aberta p/ Apostas
            </span>
          )}
          {isAndamento && (
            <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-amber-500/10 text-amber-400 border border-amber-500/20 animate-pulse">
              <span className="w-1.5 h-1.5 rounded-full bg-amber-400"></span>
              Em Andamento
            </span>
          )}
          {isEncerrada && (
            <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-slate-800 text-slate-300 border border-slate-700">
              <CheckCircle className="w-3 h-3 text-slate-400" />
              Finalizada
            </span>
          )}
        </div>
      </div>

      {/* Teams & Score Display */}
      <div className="py-2">
        <div className="flex items-center justify-between gap-4">
          
          {/* Mandante */}
          <div className="flex-1 flex flex-col items-center text-center">
            <div className="w-12 h-12 rounded-xl bg-slate-950 border border-slate-800 flex items-center justify-center font-bold text-base text-white shadow-inner mb-2 group-hover:border-slate-700 transition">
              {partida.mandante?.substring(0, 3).toUpperCase()}
            </div>
            <span className="text-sm font-bold text-white line-clamp-1">{partida.mandante}</span>
            <span className="text-[10px] text-slate-400 uppercase tracking-wider font-semibold">Mandante</span>
          </div>

          {/* VS / Score */}
          <div className="flex flex-col items-center justify-center px-2">
            {isEncerrada ? (
              <div className="flex items-center gap-2 bg-slate-950 px-3.5 py-1.5 rounded-xl border border-slate-800 shadow-inner">
                <span className="text-xl font-black text-emerald-400">{partida.golsMandante}</span>
                <span className="text-xs text-slate-500 font-bold">×</span>
                <span className="text-xl font-black text-emerald-400">{partida.golsVisitante}</span>
              </div>
            ) : (
              <div className="w-8 h-8 rounded-full bg-slate-950 border border-slate-800 flex items-center justify-center text-xs font-black text-slate-400 shadow-sm">
                VS
              </div>
            )}
            {isEncerrada && partida.cartoesAmarelos !== undefined && (
              <span className="text-[10px] text-amber-400/90 font-medium mt-1">
                🟨 {partida.cartoesAmarelos} cartões
              </span>
            )}
          </div>

          {/* Visitante */}
          <div className="flex-1 flex flex-col items-center text-center">
            <div className="w-12 h-12 rounded-xl bg-slate-950 border border-slate-800 flex items-center justify-center font-bold text-base text-white shadow-inner mb-2 group-hover:border-slate-700 transition">
              {partida.visitante?.substring(0, 3).toUpperCase()}
            </div>
            <span className="text-sm font-bold text-white line-clamp-1">{partida.visitante}</span>
            <span className="text-[10px] text-slate-400 uppercase tracking-wider font-semibold">Visitante</span>
          </div>

        </div>
      </div>

      {/* Action Footer */}
      <div className="mt-5 pt-4 border-t border-slate-800/80 flex items-center gap-2">
        {isAgendada && (
          <button
            onClick={() => onOpenBet(partida)}
            className="flex-1 py-2.5 px-4 bg-emerald-500/10 hover:bg-emerald-500 text-emerald-400 hover:text-slate-950 border border-emerald-500/30 hover:border-emerald-500 font-bold text-xs rounded-xl shadow-sm hover:shadow-emerald-500/20 transition-all flex items-center justify-center gap-1.5"
          >
            <Flame className="w-4 h-4" />
            <span>Fazer Aposta</span>
          </button>
        )}

        {isEncerrada && (
          <div className="flex-1 text-center py-2 text-xs font-semibold text-slate-400 bg-slate-950/60 rounded-xl border border-slate-800/50">
            Apurada e encerrada
          </div>
        )}

        {/* Admin Quick Action to settle result */}
        {user?.perfil === 'ADMINISTRADOR' && !isEncerrada && (
          <button
            onClick={() => onOpenResult(partida)}
            className="py-2.5 px-3 bg-amber-500/10 hover:bg-amber-500 text-amber-400 hover:text-slate-950 border border-amber-500/30 font-bold text-xs rounded-xl transition-all"
            title="Lançar Resultado e Apurar Partida"
          >
            Apurar
          </button>
        )}
      </div>

    </div>
  );
}
