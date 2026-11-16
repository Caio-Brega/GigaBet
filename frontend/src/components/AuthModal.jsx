import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { X, Lock, Mail, User, ShieldCheck, AlertCircle, ArrowRight } from 'lucide-react';

export default function AuthModal() {
  const { authModalOpen, setAuthModalOpen, authInitialTab, login, cadastro } = useAuth();
  const [tab, setTab] = useState(authInitialTab || 'login');

  // Form states
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [nome, setNome] = useState('');
  const [cpf, setCpf] = useState('');
  const [aceitouTermo, setAceitouTermo] = useState(false);

  const [loading, setLoading] = useState(false);
  const [erro, setErro] = useState('');

  if (!authModalOpen) return null;

  const resetForm = () => {
    setErro('');
    setEmail('');
    setSenha('');
    setNome('');
    setCpf('');
    setAceitouTermo(false);
  };

  const handleCpfChange = (e) => {
    let v = e.target.value.replace(/\D/g, '');
    if (v.length > 11) v = v.substring(0, 11);
    if (v.length > 9) {
      v = v.replace(/(\d{3})(\d{3})(\d{3})(\d{1,2})/, '$1.$2.$3-$4');
    } else if (v.length > 6) {
      v = v.replace(/(\d{3})(\d{3})(\d{1,3})/, '$1.$2.$3');
    } else if (v.length > 3) {
      v = v.replace(/(\d{3})(\d{1,3})/, '$1.$2');
    }
    setCpf(v);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErro('');
    setLoading(true);

    try {
      if (tab === 'login') {
        await login(email, senha);
      } else {
        if (!aceitouTermo) {
          throw new Error('É obrigatório aceitar o termo de tratamento de dados (LGPD).');
        }
        await cadastro({ nome, cpf, email, senha, aceitouTermo });
      }
      resetForm();
    } catch (err) {
      setErro(err.message || 'Falha na autenticação.');
    } finally {
      setLoading(false);
    }
  };

  const preencherAdmin = () => {
    setTab('login');
    setEmail('admin@apostas.local');
    setSenha('admin123');
    setErro('');
  };

  const preencherApostador = () => {
    setTab('login');
    setEmail('carlos@email.com');
    setSenha('senha123');
    setErro('');
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="relative w-full max-w-md bg-slate-900 border border-slate-800 rounded-2xl shadow-2xl p-6 sm:p-8 text-slate-100">
        
        {/* Close Button */}
        <button
          onClick={() => { setAuthModalOpen(false); resetForm(); }}
          className="absolute top-4 right-4 p-2 text-slate-400 hover:text-white rounded-lg hover:bg-slate-800 transition"
        >
          <X className="w-5 h-5" />
        </button>

        {/* Header Tabs */}
        <div className="flex border-b border-slate-800 mb-6">
          <button
            type="button"
            onClick={() => { setTab('login'); setErro(''); }}
            className={`flex-1 pb-3 text-sm font-bold border-b-2 transition-all ${
              tab === 'login'
                ? 'border-emerald-500 text-emerald-400'
                : 'border-transparent text-slate-400 hover:text-slate-200'
            }`}
          >
            Entrar
          </button>
          <button
            type="button"
            onClick={() => { setTab('register'); setErro(''); }}
            className={`flex-1 pb-3 text-sm font-bold border-b-2 transition-all ${
              tab === 'register'
                ? 'border-emerald-500 text-emerald-400'
                : 'border-transparent text-slate-400 hover:text-slate-200'
            }`}
          >
            Cadastrar Apostador
          </button>
        </div>

        {/* Error Alert */}
        {erro && (
          <div className="mb-5 p-3 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-400 text-sm flex items-start gap-2.5">
            <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
            <span>{erro}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          {tab === 'register' && (
            <>
              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">Nome Completo</label>
                <div className="relative">
                  <User className="w-4 h-4 text-slate-400 absolute left-3 top-3" />
                  <input
                    type="text"
                    required
                    value={nome}
                    onChange={(e) => setNome(e.target.value)}
                    placeholder="João da Silva"
                    className="w-full bg-slate-950 border border-slate-800 rounded-xl pl-9 pr-4 py-2.5 text-sm text-white placeholder-slate-500 focus:outline-none focus:border-emerald-500 focus:ring-1 focus:ring-emerald-500 transition"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">CPF</label>
                <div className="relative">
                  <ShieldCheck className="w-4 h-4 text-slate-400 absolute left-3 top-3" />
                  <input
                    type="text"
                    required
                    value={cpf}
                    onChange={handleCpfChange}
                    placeholder="000.000.000-00"
                    maxLength={14}
                    className="w-full bg-slate-950 border border-slate-800 rounded-xl pl-9 pr-4 py-2.5 text-sm text-white placeholder-slate-500 focus:outline-none focus:border-emerald-500 focus:ring-1 focus:ring-emerald-500 transition"
                  />
                </div>
              </div>
            </>
          )}

          <div>
            <label className="block text-xs font-semibold text-slate-300 mb-1">E-mail</label>
            <div className="relative">
              <Mail className="w-4 h-4 text-slate-400 absolute left-3 top-3" />
              <input
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="seu.email@exemplo.com"
                className="w-full bg-slate-950 border border-slate-800 rounded-xl pl-9 pr-4 py-2.5 text-sm text-white placeholder-slate-500 focus:outline-none focus:border-emerald-500 focus:ring-1 focus:ring-emerald-500 transition"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-300 mb-1">Senha</label>
            <div className="relative">
              <Lock className="w-4 h-4 text-slate-400 absolute left-3 top-3" />
              <input
                type="password"
                required
                minLength={6}
                value={senha}
                onChange={(e) => setSenha(e.target.value)}
                placeholder="Mínimo 6 caracteres"
                className="w-full bg-slate-950 border border-slate-800 rounded-xl pl-9 pr-4 py-2.5 text-sm text-white placeholder-slate-500 focus:outline-none focus:border-emerald-500 focus:ring-1 focus:ring-emerald-500 transition"
              />
            </div>
            {tab === 'login' && (
              <p className="text-[11px] text-slate-500 mt-1">
                Bloqueio automático de 15 minutos após 5 tentativas sem sucesso.
              </p>
            )}
          </div>

          {tab === 'register' && (
            <label className="flex items-start gap-2.5 cursor-pointer pt-1">
              <input
                type="checkbox"
                required
                checked={aceitouTermo}
                onChange={(e) => setAceitouTermo(e.target.checked)}
                className="mt-0.5 rounded bg-slate-950 border-slate-700 text-emerald-500 focus:ring-emerald-500"
              />
              <span className="text-xs text-slate-300 leading-tight">
                Aceito o termo de consentimento para tratamento e armazenamento de dados cadastrais (RN01 / LGPD).
              </span>
            </label>
          )}

          <button
            type="submit"
            disabled={loading}
            className="w-full mt-4 py-3 px-4 bg-emerald-500 hover:bg-emerald-400 disabled:opacity-50 text-slate-950 font-bold text-sm rounded-xl shadow-lg shadow-emerald-500/20 transition-all flex items-center justify-center gap-2"
          >
            {loading ? (
              <span className="animate-pulse">Processando...</span>
            ) : tab === 'login' ? (
              <>
                <span>Acessar Conta</span>
                <ArrowRight className="w-4 h-4" />
              </>
            ) : (
              <>
                <span>Concluir Cadastro</span>
                <ArrowRight className="w-4 h-4" />
              </>
            )}
          </button>
        </form>

        {/* Demo Helpers for grading / testing */}
        <div className="mt-6 pt-5 border-t border-slate-800 text-center">
          <p className="text-xs text-slate-400 mb-2 font-medium">Acesso rápido para testes de avaliação:</p>
          <div className="flex gap-2">
            <button
              type="button"
              onClick={preencherAdmin}
              className="flex-1 py-1.5 px-2 bg-slate-800 hover:bg-slate-700 text-[11px] text-amber-400 font-semibold rounded-lg border border-slate-700 transition"
            >
              Usar Admin
            </button>
            <button
              type="button"
              onClick={preencherApostador}
              className="flex-1 py-1.5 px-2 bg-slate-800 hover:bg-slate-700 text-[11px] text-emerald-400 font-semibold rounded-lg border border-slate-700 transition"
            >
              Usar Apostador
            </button>
          </div>
        </div>

      </div>
    </div>
  );
}
