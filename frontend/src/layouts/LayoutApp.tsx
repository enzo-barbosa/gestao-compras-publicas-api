import { Navigate, Route, Routes } from 'react-router-dom'
import Navbar from '../components/Navbar'
import DashboardPage from '../pages/DashboardPage'
import DotacoesPage from '../pages/DotacoesPage'
import FornecedoresPage from '../pages/FornecedoresPage'
import LicitacoesPage from '../pages/LicitacoesPage'
import ContratosPage from '../pages/ContratosPage'
import EmpenhosPage from '../pages/EmpenhosPage'

export default function LayoutApp() {
  return (
    <div className="layout">
      <Navbar />
      <main className="conteudo">
        <Routes>
          <Route path="/app" element={<DashboardPage />} />
          <Route path="/app/dotacoes" element={<DotacoesPage />} />
          <Route path="/app/fornecedores" element={<FornecedoresPage />} />
          <Route path="/app/licitacoes" element={<LicitacoesPage />} />
          <Route path="/app/contratos" element={<ContratosPage />} />
          <Route path="/app/empenhos" element={<EmpenhosPage />} />
          <Route path="*" element={<Navigate to="/app" replace />} />
        </Routes>
      </main>
    </div>
  )
}