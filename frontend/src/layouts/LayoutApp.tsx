import { Navigate, Route, Routes } from 'react-router-dom'
import Navbar from '../components/Navbar'
import DashboardPage from '../pages/DashboardPage'
import DotacoesPage from '../pages/DotacoesPage'
import FornecedoresPage from '../pages/FornecedoresPage'
import LicitacoesPage from '../pages/LicitacoesPage'
import ContratosPage from '../pages/ContratosPage'
import EmpenhosPage from '../pages/EmpenhosPage'
import MembrosPage from '../pages/MembrosPage'
import GruposPage from '../pages/GruposPage'
import CreditosPage from '../pages/CreditosPage'
import MinhaContaPage from '../pages/MinhaContaPage'
import SuperPainelPage from '../pages/SuperPainelPage'

export default function LayoutApp() {
  return (
    <div className="layout">
      <Navbar />
      <main className="conteudo">
        <Routes>
          <Route index element={<DashboardPage />} />
          <Route path="dotacoes" element={<DotacoesPage />} />
          <Route path="fornecedores" element={<FornecedoresPage />} />
          <Route path="licitacoes" element={<LicitacoesPage />} />
          <Route path="contratos" element={<ContratosPage />} />
          <Route path="empenhos" element={<EmpenhosPage />} />
          <Route path="membros" element={<MembrosPage />} />
          <Route path="grupos" element={<GruposPage />} />
          <Route path="creditos" element={<CreditosPage />} />
          <Route path="conta" element={<MinhaContaPage />} />
          <Route path="superpainel" element={<SuperPainelPage />} />
          <Route path="*" element={<Navigate to="/app" replace />} />
        </Routes>
      </main>
    </div>
  )
}