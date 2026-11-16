import React from 'react';
import { useAuth } from '../context/AuthContext';
import { Trophy, Wallet, User, LogOut, Shield, PlusCircle, History, LayoutDashboard, Flame } from 'lucide-react';

export default function Navbar({ activeTab, setActiveTab }) {
  const { user, logout, openLogin, openRegister, setDepositModalOpen } = useAuth();

  return (
    <header className="sticky top-0 z-40 w-full border-b border-slate-800 bg-slate-950/80 backdrop-blur-md">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          
          {/* Logo & Brand */}
          <div className="flex items-center gap-3 cursor-pointer" onClick={() => setActiveTab('partidas')}>
            <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-emerald-600 to-emerald-400 flex items-center justify-center shadow-lg shadow-emerald-500/20 text-slate-950 font-black text-xl">
              <Trophy className="w-6 h-6 stroke-[2.5]" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="font-extrabold text-2xl tracking-tight bg-gradient-to-r from-white via-slate-100 to-emerald-400 bg-clip-text text-transparent">
                  GigaBet
                </span>
                <span className="text-[10px] uppercase font-bold tracking-wider px-1.5 py-0.5 rounded bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                  DCC603
                </span>
              </div>
              <p className="text-[10px] text-slate-400 font-medium -mt-1">Engenharia de Software UFMG</p>
            </div>
          </div>

          {/* Navigation Links */}
          <nav className="hidden md:flex items-center gap-1">
            <button
              onClick={() => setActiveTab('partidas')}
              className={`px-3 py-2 rounded-lg text-sm font-semibold transition-all flex items-center gap-2 ${
                activeTab === 'partidas'
                  ? 'bg-slate-800 text-emerald-400 shadow-sm'
                  : 'text-slate-300 hover:text-white hover:bg-slate-900'
              }`}
            >
              <Flame className="w-4 h-4" />
              Mural de Partidas
            </button>

            {user && user.perfil === 'APOSTADOR' && (
              <button
                onClick={() => setActiveTab('historico')}
                className={`px-3 py-2 rounded-lg text-sm font-semibold transition-all flex items-center gap-2 ${
                  activeTab === 'historico'
                    ? 'bg-slate-800 text-emerald-400 shadow-sm'
                    : 'text-slate-300 hover:text-white hover:bg-slate-900'
                }`}
              >
                <History className="w-4 h-4" />
                Minhas Apostas
              </button>
            )}

            {user && user.perfil === 'ADMINISTRADOR' && (
              <button
                onClick={() => setActiveTab('admin')}
                className={`px-3 py-2 rounded-lg text-sm font-semibold transition-all flex items-center gap-2 ${
                  activeTab === 'admin'
                    ? 'bg-amber-500/10 text-amber-400 border border-amber-500/30'
                    : 'text-slate-300 hover:text-white hover:bg-slate-900'
                }`}
              >
                <LayoutDashboard className="w-4 h-4" />
                Painel Administrativo
              </button>
            )}
          </nav>

          {/* Right Section: User Profile & Actions */}
          <div className="flex items-center gap-3">
            {user ? (
              <div className="flex items-center gap-3">
                {user.perfil === 'APOSTADOR' && (
                  <div className="flex items-center gap-2 bg-slate-900/90 border border-emerald-500/30 rounded-xl px-3 py-1.5 shadow-sm">
                    <Wallet className="w-4 h-4 text-emerald-400" />
                    <div>
                      <p className="text-[10px] text-slate-400 font-medium leading-none">Saldo</p>
                      <p className="text-sm font-bold text-emerald-400 leading-tight">
                        R$ {parseFloat(user.saldo || 0).toFixed(2).replace('.', ',')}
                      </p>
                    </div>
                    <button
                      onClick={() => setDepositModalOpen(true)}
                      title="Depositar saldo simulado"
                      className="ml-1 text-slate-400 hover:text-emerald-400 p-1 hover:bg-emerald-500/10 rounded-lg transition"
                    >
                      <PlusCircle className="w-4 h-4" />
                    </button>
                  </div>
                )}

                {user.perfil === 'ADMINISTRADOR' && (
                  <div className="flex items-center gap-1.5 bg-amber-500/10 text-amber-400 border border-amber-500/20 px-2.5 py-1 rounded-lg text-xs font-bold">
                    <Shield className="w-3.5 h-3.5" />
                    ADMIN
                  </div>
                )}

                <div className="hidden sm:flex flex-col text-right">
                  <span className="text-xs font-semibold text-slate-200">{user.nome}</span>
                  <span className="text-[10px] text-slate-400 capitalize">{user.perfil?.toLowerCase()}</span>
                </div>

                <button
                  onClick={logout}
                  title="Sair da conta"
                  className="p-2 text-slate-400 hover:text-rose-400 hover:bg-rose-500/10 rounded-xl transition-all"
                >
                  <LogOut className="w-4 h-4" />
                </button>
              </div>
            ) : (
              <div className="flex items-center gap-2">
                <button
                  onClick={openLogin}
                  className="px-4 py-2 text-sm font-medium text-slate-200 hover:text-white hover:bg-slate-800 rounded-xl transition"
                >
                  Entrar
                </button>
                <button
                  onClick={openRegister}
                  className="px-4 py-2 text-sm font-bold bg-emerald-500 hover:bg-emerald-400 text-slate-950 rounded-xl shadow-lg shadow-emerald-500/20 transition-all hover:scale-[1.02] active:scale-[0.98]"
                >
                  Cadastrar
                </button>
              </div>
            )}
          </div>
        </div>
      </div>
    </header>
  );
}
