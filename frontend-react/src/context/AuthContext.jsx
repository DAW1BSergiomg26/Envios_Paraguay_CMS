import { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { checkSession, loginUser, logoutUser } from '../services/api';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    checkSession()
      .then(ok => { if (ok) setUser({ username: 'admin' }); })
      .finally(() => setLoading(false));
  }, []);

  const login = useCallback(async (username, password) => {
    const ok = await loginUser(username, password);
    if (ok) setUser({ username });
    return ok;
  }, []);

  const logout = useCallback(async () => {
    await logoutUser();
    setUser(null);
  }, []);

  return (
    <AuthContext.Provider value={{ user, loading, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
