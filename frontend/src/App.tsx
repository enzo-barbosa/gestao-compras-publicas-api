import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './context/AuthProvider'
import { SessaoExpiradaListener } from './context/SessaoExpirada'
import Navbar from './components/Navbar'
import RotaProtegida from './components/RotaProtegida'
import LoginPage from './pages/LoginPage'
import DashboardPage from './pages/DashboardPage'
import DotacoesPage from './pages/DotacoesPage'
import FornecedoresPage from './pages/FornecedoresPage'
import LicitacoesPage from './pages/LicitacoesPage'
import ContratosPage from './pages/ContratosPage'
import EmpenhosPage from './pages/EmpenhosPage'

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <SessaoExpiradaListener />
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route
            path="/*"
            element={
              <RotaProtegida>
                <div className="layout">
                  <Navbar />
                  <main className="conteudo">
                    <Routes>
                      <Route path="/" element={<DashboardPage />} />
                      <Route path="dotacoes" element={<DotacoesPage />} />
                      <Route path="fornecedores" element={<FornecedoresPage />} />
                      <Route path="licitacoes" element={<LicitacoesPage />} />
                      <Route path="contratos" element={<ContratosPage />} />
                      <Route path="empenhos" element={<EmpenhosPage />} />
                      <Route path="*" element={<Navigate to="/" replace />} />
                    </Routes>
                  </main>
                </div>
              </RotaProtegida>
            }
          />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  )
}
