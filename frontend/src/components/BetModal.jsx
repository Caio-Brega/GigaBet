import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { ApiClient } from '../api';
import { X, Trophy, DollarSign, Calculator, CheckCircle2, AlertCircle, ArrowRight, ShieldCheck } from 'lucide-react';

export default function BetModal({ partida, isOpen, onClose, onBetPlaced }) {
  const { user, refreshUser, openLogin } = useAuth();

  const [tipo, setTipo] = useState('EQUIPE_VENCEDORA');
  const [desfecho, setDesfecho] = useState('MANDANTE');
  const [golsMandante, setGolsMandante] = useState(2);
  const [golsVisitante, setGolsVisitante] = useState(1);
  const [cartoesAmarelos, setCartoesAmarelos] = useState(4);
  const [valor, setValor] = useState('20.00');

  const [cotacao, setCotacao] = useState(null);
  const [calculando, setCalculando] = useState(false);
  const [confirmando, setConfirmando] = useState(false);
  const [erro, setErro] = useState('');
  const [comprovante, setComprovante] = useState(null);

  useEffect(() => {
    if (!partida || !isOpen || comprovante) return;
    calcularCotacao();
  }, [partida, tipo, desfecho, golsMandante, golsVisitante, cartoesAmarelos, valor, isOpen]);

  if (!isOpen || !partida) return null;

  const getPayload = () => ({
    partidaId: partida.id.toString(),
    tipo,
    desfecho,
    golsMandante: golsMandante.toString(),
    golsVisitante: golsVisitante.toString(),
    cartoesAmarelos: cartoesAmarelos.toString(),
    valor: (parseFloat(valor) || 1).toFixed(2)
  });

  const calcularCotacao = async () => {
    if (!user || user.perfil !== 'APOSTADOR') return;
    const v = parseFloat(valor);
    if (isNaN(v) || v <= 0) return;

    setCalculando(true);
    setErro('');
    try {
      const res = await ApiClient.cotar(getPayload());
      setCotacao(res);
    } catch (err) {
      setErro(err.message || 'Erro ao cotar aposta.');
      setCotacao(null);
    } finally {
      setCalculando(false);
    }
  };

  const handleConfirmar = async (e) => {
    e.preventDefault();
    if (!user) {
      openLogin();
      return;
    }

    setConfirmando(true);
    setErro('');

    try {
      const res = await ApiClient.confirmarAposta(getPayload());
      setComprovante(res);
      await refreshUser();
      if (onBetPlaced) onBetPlaced();
    } catch (err) {
      setErro(err.message || 'Falha ao confirmar aposta.');
    } finally {
      setConfirmando(false);
    }
  };

  const fechar = () => {
    setComprovante(null);
    setErro('');
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="relative w-full max-w-lg bg-slate-900 border border-slate-800 rounded-3xl shadow-2xl p-6 sm:p-8 text-slate-100 max-h-[90vh] overflow-y-auto">
        
        <button
          onClick={fechar}
          className="absolute top-4 right-4 p-2 text-slate-400 hover:text-white rounded-lg hover:bg-slate-800 transition"
        >
          <X className="w-5 h-5" />
        </button>

        {/* TELA DE COMPROVANTE (Sucesso) */}
        {comprovante ? (
          <div className="text-center py-4 space-y-5 animate-in zoom-in-95 duration-200">
            <div className="w-16 h-16 rounded-full bg-emerald-500/10 border border-emerald-500/30 flex items-center justify-center mx-auto text-emerald-400">
              <CheckCircle2 className="w-10 h-10" />
            </div>

            <div>
              <span className="text-xs font-bold uppercase tracking-wider text-emerald-400 bg-emerald-500/10 px-3 py-1 rounded-full border border-emerald-500/20">
                Aposta Confirmada #{comprovante.apostaId}
              </span>
              <h3 className="text-xl font-extrabold text-white mt-2">Comprovante de Registro</h3>
              <p className="text-xs text-slate-400">{partida.descricao}</p>
            </div>

            <div className="bg-slate-950 border border-slate-800 rounded-2xl p-5 text-left space-y-3">
              <div className="flex justify-between text-xs pb-2 border-b border-slate-800">
                <span className="text-slate-400">Previsão:</span>
                <span className="font-bold text-white">{comprovante.previsao}</span>
              </div>
              <div className="flex justify-between text-xs pb-2 border-b border-slate-800">
                <span className="text-slate-400">Valor Apostado:</span>
                <span className="font-bold text-white">R$ {parseFloat(comprovante.valor).toFixed(2).replace('.', ',')}</span>
              </div>
              <div className="flex justify-between text-xs pb-2 border-b border-slate-800">
                <span className="text-slate-400">Prêmio Potencial:</span>
                <span className="font-black text-emerald-400 text-sm">
                  R$ {parseFloat(comprovante.premioPotencial).toFixed(2).replace('.', ',')}
                </span>
              </div>
              <div className="flex justify-between text-xs">
                <span className="text-slate-400">Novo Saldo Disponível:</span>
                <span className="font-semibold text-slate-200">
                  R$ {parseFloat(comprovante.saldoRestante).toFixed(2).replace('.', ',')}
                </span>
              </div>
            </div>

            <button
              onClick={fechar}
              className="w-full py-3 bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-bold text-sm rounded-xl transition shadow-lg shadow-emerald-500/20"
            >
              Concluir e Voltar
            </button>
          </div>
        ) : (
          /* FORMULÁRIO DE APOSTA */
          <div>
            <div className="mb-6">
              <span className="text-[10px] font-bold uppercase tracking-wider text-emerald-400 bg-emerald-500/10 px-2.5 py-0.5 rounded-md border border-emerald-500/20">
                Cupom de Aposta
              </span>
              <h3 className="text-xl font-extrabold text-white mt-1.5">{partida.descricao}</h3>
              <p className="text-xs text-slate-400">Escolha uma previsão e confira as odds calculadas via Poisson.</p>
            </div>

            {/* SELEÇÃO DO TIPO DE PREVISÃO */}
            <div className="mb-5">
              <label className="block text-xs font-semibold text-slate-300 mb-2">Tipo de Previsão:</label>
              <div className="grid grid-cols-3 gap-2">
                <button
                  type="button"
                  onClick={() => setTipo('EQUIPE_VENCEDORA')}
                  className={`py-2 px-3 rounded-xl text-xs font-bold border transition ${
                    tipo === 'EQUIPE_VENCEDORA'
                      ? 'bg-emerald-500 text-slate-950 border-emerald-500 shadow-sm'
                      : 'bg-slate-950 text-slate-400 border-slate-800 hover:border-slate-700'
                  }`}
                >
                  Vencedor
                </button>
                <button
                  type="button"
                  onClick={() => setTipo('PLACAR_EXATO')}
                  className={`py-2 px-3 rounded-xl text-xs font-bold border transition ${
                    tipo === 'PLACAR_EXATO'
                      ? 'bg-emerald-500 text-slate-950 border-emerald-500 shadow-sm'
                      : 'bg-slate-950 text-slate-400 border-slate-800 hover:border-slate-700'
                  }`}
                >
                  Placar Exato
                </button>
                <button
                  type="button"
                  onClick={() => setTipo('CARTOES_AMARELOS')}
                  className={`py-2 px-3 rounded-xl text-xs font-bold border transition ${
                    tipo === 'CARTOES_AMARELOS'
                      ? 'bg-emerald-500 text-slate-950 border-emerald-500 shadow-sm'
                      : 'bg-slate-950 text-slate-400 border-slate-800 hover:border-slate-700'
                  }`}
                >
                  Cartões (0-10)
                </button>
              </div>
            </div>

            {/* CAMPOS ESPECÍFICOS DE ACORDO COM O TIPO */}
            <div className="bg-slate-950 border border-slate-800 rounded-2xl p-4 mb-5">
              {tipo === 'EQUIPE_VENCEDORA' && (
                <div>
                  <span className="block text-xs text-slate-400 mb-2 font-medium">Quem vence a partida?</span>
                  <div className="grid grid-cols-3 gap-2">
                    <button
                      type="button"
                      onClick={() => setDesfecho('MANDANTE')}
                      className={`p-3 rounded-xl text-xs font-bold border flex flex-col items-center gap-1 transition ${
                        desfecho === 'MANDANTE'
                          ? 'bg-emerald-500/20 text-emerald-400 border-emerald-500'
                          : 'bg-slate-900 text-slate-300 border-slate-800 hover:border-slate-700'
                      }`}
                    >
                      <span className="line-clamp-1">{partida.mandante}</span>
                      <span className="text-[10px] text-slate-500 font-semibold">(Mandante)</span>
                    </button>

                    <button
                      type="button"
                      onClick={() => setDesfecho('EMPATE')}
                      className={`p-3 rounded-xl text-xs font-bold border flex flex-col items-center gap-1 transition ${
                        desfecho === 'EMPATE'
                          ? 'bg-emerald-500/20 text-emerald-400 border-emerald-500'
                          : 'bg-slate-900 text-slate-300 border-slate-800 hover:border-slate-700'
                      }`}
                    >
                      <span>Empate</span>
                      <span className="text-[10px] text-slate-500 font-semibold">(X)</span>
                    </button>

                    <button
                      type="button"
                      onClick={() => setDesfecho('VISITANTE')}
                      className={`p-3 rounded-xl text-xs font-bold border flex flex-col items-center gap-1 transition ${
                        desfecho === 'VISITANTE'
                          ? 'bg-emerald-500/20 text-emerald-400 border-emerald-500'
                          : 'bg-slate-900 text-slate-300 border-slate-800 hover:border-slate-700'
                      }`}
                    >
                      <span className="line-clamp-1">{partida.visitante}</span>
                      <span className="text-[10px] text-slate-500 font-semibold">(Visitante)</span>
                    </button>
                  </div>
                </div>
              )}

              {tipo === 'PLACAR_EXATO' && (
                <div>
                  <span className="block text-xs text-slate-400 mb-3 font-medium">Previsão de Placar Final (0 a 9 gols):</span>
                  <div className="flex items-center justify-center gap-4">
                    <div className="flex-1 text-center">
                      <label className="block text-[11px] text-slate-400 font-semibold mb-1 truncate">{partida.mandante}</label>
                      <input
                        type="number"
                        min="0"
                        max="9"
                        value={golsMandante}
                        onChange={(e) => setGolsMandante(Math.max(0, Math.min(9, parseInt(e.target.value) || 0)))}
                        className="w-16 h-14 bg-slate-900 border border-slate-700 rounded-xl text-center font-black text-2xl text-emerald-400 focus:outline-none focus:border-emerald-500"
                      />
                    </div>
                    <span className="text-xl font-black text-slate-600 mt-5">×</span>
                    <div className="flex-1 text-center">
                      <label className="block text-[11px] text-slate-400 font-semibold mb-1 truncate">{partida.visitante}</label>
                      <input
                        type="number"
                        min="0"
                        max="9"
                        value={golsVisitante}
                        onChange={(e) => setGolsVisitante(Math.max(0, Math.min(9, parseInt(e.target.value) || 0)))}
                        className="w-16 h-14 bg-slate-900 border border-slate-700 rounded-xl text-center font-black text-2xl text-emerald-400 focus:outline-none focus:border-emerald-500"
                      />
                    </div>
                  </div>
                </div>
              )}

              {tipo === 'CARTOES_AMARELOS' && (
                <div>
                  <div className="flex justify-between items-center mb-2">
                    <span className="text-xs text-slate-400 font-medium">Total de Cartões Amarelos:</span>
                    <span className="text-sm font-black text-amber-400">{cartoesAmarelos} {cartoesAmarelos >= 10 ? '(10+)' : ''}</span>
                  </div>
                  <input
                    type="range"
                    min="0"
                    max="10"
                    value={cartoesAmarelos}
                    onChange={(e) => setCartoesAmarelos(parseInt(e.target.value))}
                    className="w-full accent-amber-500 cursor-pointer"
                  />
                  <div className="flex justify-between text-[10px] text-slate-500 mt-1 font-mono">
                    <span>0</span>
                    <span>2</span>
                    <span>4</span>
                    <span>6</span>
                    <span>8</span>
                    <span>10+</span>
                  </div>
                </div>
              )}
            </div>

            {/* VALOR DA APOSTA */}
            <div className="mb-5">
              <div className="flex justify-between items-center mb-1.5">
                <label className="text-xs font-semibold text-slate-300">Valor da Aposta (R$):</label>
                {user && (
                  <span className="text-[11px] text-slate-400">
                    Disponível: <strong className="text-emerald-400">R$ {parseFloat(user.saldo || 0).toFixed(2).replace('.', ',')}</strong>
                  </span>
                )}
              </div>

              <div className="relative mb-2">
                <span className="absolute left-3 top-2.5 text-sm font-bold text-slate-400">R$</span>
                <input
                  type="number"
                  step="1.00"
                  min="0.50"
                  value={valor}
                  onChange={(e) => setValor(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl pl-10 pr-4 py-2.5 text-lg font-bold text-white focus:outline-none focus:border-emerald-500"
                />
              </div>

              {/* Botões rápidos */}
              <div className="flex gap-2">
                {['10', '20', '50', '100'].map((v) => (
                  <button
                    key={v}
                    type="button"
                    onClick={() => setValor(v + '.00')}
                    className="flex-1 py-1 px-2 rounded-lg bg-slate-950 border border-slate-800 text-[11px] font-semibold text-slate-300 hover:border-slate-700 transition"
                  >
                    +R${v}
                  </button>
                ))}
              </div>
            </div>

            {/* COTAÇÃO CALCULADA (POISSON) */}
            <div className="bg-gradient-to-br from-emerald-950/30 to-slate-950 border border-emerald-500/20 rounded-2xl p-4 mb-5">
              <div className="flex items-center justify-between">
                <div>
                  <span className="text-[10px] uppercase tracking-wider font-bold text-emerald-400 flex items-center gap-1">
                    <Calculator className="w-3 h-3" /> Cotação Estimada (Poisson)
                  </span>
                  <p className="text-xs text-slate-400 mt-0.5">
                    Probabilidade: {cotacao?.probabilidade ? (parseFloat(cotacao.probabilidade) * 100).toFixed(1) + '%' : 'Calculando...'}
                  </p>
                </div>

                <div className="text-right">
                  <span className="text-[10px] text-slate-400 font-medium">Retorno Potencial</span>
                  <p className="text-xl font-black text-emerald-400">
                    R$ {cotacao?.premioPotencial ? parseFloat(cotacao.premioPotencial).toFixed(2).replace('.', ',') : '0,00'}
                  </p>
                </div>
              </div>
            </div>

            {/* MENSAGEM DE ERRO SE HOUVER */}
            {erro && (
              <div className="mb-4 p-3 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-400 text-xs flex items-center gap-2">
                <AlertCircle className="w-4 h-4 shrink-0" />
                <span>{erro}</span>
              </div>
            )}

            {/* BOTÃO DE CONFIRMAR */}
            <button
              onClick={handleConfirmar}
              disabled={confirmando || calculando}
              className="w-full py-3.5 px-4 bg-emerald-500 hover:bg-emerald-400 disabled:opacity-50 text-slate-950 font-black text-sm rounded-xl shadow-lg shadow-emerald-500/20 transition-all flex items-center justify-center gap-2"
            >
              {confirmando ? (
                <span className="animate-pulse">Registrando Aposta...</span>
              ) : (
                <>
                  <span>Confirmar Palpite</span>
                  <ArrowRight className="w-4 h-4" />
                </>
              )}
            </button>
          </div>
        )}

      </div>
    </div>
  );
}
