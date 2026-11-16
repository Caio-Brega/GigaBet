import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { X, Wallet, CheckCircle, AlertCircle, ArrowUpRight } from 'lucide-react';

export default function DepositModal() {
  const { depositModalOpen, setDepositModalOpen, user, depositar } = useAuth();
  const [valor, setValor] = useState('50.00');
  const [loading, setLoading] = useState(false);
  const [erro, setErro] = useState('');

  if (!depositModalOpen) return null;

  const presets = ['20.00', '50.00', '100.00', '250.00'];

  const handleDeposit = async (e) => {
    e.preventDefault();
    setErro('');
    const v = parseFloat(valor);
    if (isNaN(v) || v <= 0) {
      setErro('Informe um valor positivo para o depósito.');
      return;
    }

    setLoading(true);
    try {
      await depositar(v.toFixed(2));
    } catch (err) {
      setErro(err.message || 'Falha ao processar depósito.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="relative w-full max-w-md bg-slate-900 border border-slate-800 rounded-2xl shadow-2xl p-6 sm:p-8 text-slate-100">
        
        <button
          onClick={() => setDepositModalOpen(false)}
          className="absolute top-4 right-4 p-2 text-slate-400 hover:text-white rounded-lg hover:bg-slate-800 transition"
        >
          <X className="w-5 h-5" />
        </button>

        <div className="flex items-center gap-3 mb-6">
          <div className="w-12 h-12 rounded-xl bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-emerald-400">
            <Wallet className="w-6 h-6" />
          </div>
          <div>
            <h3 className="text-lg font-bold text-white">Depósito de Saldo Simulado</h3>
            <p className="text-xs text-slate-400">Ambiente de testes DCC603 — UFMG</p>
          </div>
        </div>

        {/* Current Balance Banner */}
        <div className="bg-slate-950 border border-slate-800 rounded-xl p-4 mb-5 flex items-center justify-between">
          <div>
            <span className="text-xs text-slate-400 font-medium">Saldo Atual</span>
            <p className="text-2xl font-black text-emerald-400">
              R$ {parseFloat(user?.saldo || 0).toFixed(2).replace('.', ',')}
            </p>
          </div>
          <span className="text-[11px] font-semibold text-slate-400 bg-slate-900 px-2.5 py-1 rounded-lg border border-slate-800">
            Disponível
          </span>
        </div>

        {erro && (
          <div className="mb-4 p-3 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-400 text-sm flex items-center gap-2">
            <AlertCircle className="w-4 h-4 shrink-0" />
            <span>{erro}</span>
          </div>
        )}

        <form onSubmit={handleDeposit} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-300 mb-2">Selecione ou digite um valor:</label>
            
            {/* Quick preset chips */}
            <div className="grid grid-cols-4 gap-2 mb-3">
              {presets.map((p) => (
                <button
                  key={p}
                  type="button"
                  onClick={() => setValor(p)}
                  className={`py-2 px-3 rounded-xl text-xs font-bold border transition ${
                    valor === p
                      ? 'bg-emerald-500/20 text-emerald-400 border-emerald-500/50 shadow-sm'
                      : 'bg-slate-950 text-slate-300 border-slate-800 hover:border-slate-700'
                  }`}
                >
                  R$ {p.replace('.00', '')}
                </button>
              ))}
            </div>

            <div className="relative">
              <span className="absolute left-3 top-2.5 text-sm font-bold text-slate-400">R$</span>
              <input
                type="number"
                step="0.01"
                min="0.01"
                required
                value={valor}
                onChange={(e) => setValor(e.target.value)}
                placeholder="0.00"
                className="w-full bg-slate-950 border border-slate-800 rounded-xl pl-10 pr-4 py-2.5 text-lg font-bold text-white placeholder-slate-600 focus:outline-none focus:border-emerald-500 focus:ring-1 focus:ring-emerald-500 transition"
              />
            </div>
          </div>

          <div className="p-3 rounded-xl bg-slate-950 border border-slate-800 text-[11px] text-slate-400 leading-relaxed">
            <span className="font-semibold text-slate-300">Nota de Escopo:</span> Conforme definido na especificação da disciplina, a integração com gateways de pagamento reais ficou fora do escopo. O crédito é instantâneo e seguro em memória.
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full py-3 px-4 bg-emerald-500 hover:bg-emerald-400 disabled:opacity-50 text-slate-950 font-bold text-sm rounded-xl shadow-lg shadow-emerald-500/20 transition-all flex items-center justify-center gap-2"
          >
            {loading ? (
              <span className="animate-pulse">Creditando Saldo...</span>
            ) : (
              <>
                <span>Confirmar Depósito Simulado</span>
                <ArrowUpRight className="w-4 h-4" />
              </>
            )}
          </button>
        </form>

      </div>
    </div>
  );
}
