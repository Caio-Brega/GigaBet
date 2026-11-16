import React, { createContext, useContext, useState, useEffect } from 'react';
import { ApiClient } from '../api';

const AuthContext = createContext();

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [authModalOpen, setAuthModalOpen] = useState(false);
  const [authInitialTab, setAuthInitialTab] = useState('login'); // 'login' | 'register'
  const [depositModalOpen, setDepositModalOpen] = useState(false);

  useEffect(() => {
    async function loadUser() {
      const token = ApiClient.getToken();
      if (!token) {
        setLoading(false);
        return;
      }
      try {
        const u = await ApiClient.me();
        setUser(u);
      } catch (err) {
        ApiClient.setToken(null);
        setUser(null);
      } finally {
        setLoading(false);
      }
    }
    loadUser();
  }, []);

  const login = async (email, senha) => {
    const data = await ApiClient.login(email, senha);
    setUser(data);
    setAuthModalOpen(false);
    return data;
  };

  const cadastro = async (dados) => {
    const data = await ApiClient.cadastro(dados);
    setUser(data);
    setAuthModalOpen(false);
    return data;
  };

  const logout = () => {
    ApiClient.setToken(null);
    setUser(null);
  };

  const depositar = async (valor) => {
    const res = await ApiClient.deposito(valor);
    if (res.novoSaldo !== undefined) {
      setUser(prev => ({ ...prev, saldo: res.novoSaldo }));
    } else {
      // Recarregar dados
      const u = await ApiClient.me();
      setUser(u);
    }
    setDepositModalOpen(false);
    return res;
  };

  const refreshUser = async () => {
    if (!ApiClient.getToken()) return;
    try {
      const u = await ApiClient.me();
      setUser(u);
    } catch (ignored) {}
  };

  const openLogin = () => {
    setAuthInitialTab('login');
    setAuthModalOpen(true);
  };

  const openRegister = () => {
    setAuthInitialTab('register');
    setAuthModalOpen(true);
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        loading,
        login,
        cadastro,
        logout,
        depositar,
        refreshUser,
        authModalOpen,
        setAuthModalOpen,
        authInitialTab,
        openLogin,
        openRegister,
        depositModalOpen,
        setDepositModalOpen
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}
