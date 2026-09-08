import { BrowserRouter, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './context/AuthProvider'
import { SessaoExpiradaListener } from './context/SessaoExpirada'
import { ToastProvider } from './components/Toasts'
import RotaProtegida from './components/RotaProtegida'
import GuardiaOrganizacao from './components/GuardiaOrganizacao'
import LayoutApp from './layouts/LayoutApp'
import LandingPage from './pages/LandingPage'
import LoginPage from './pages/LoginPage'
import CadastroPage from './pages/CadastroPage'
import OnboardingPage from './pages/OnboardingPage'
import SelecionarGrupoPage from './pages/SelecionarGrupoPage'

export default function App() {
  return (
    <BrowserRouter>
      <ToastProvider>
        <AuthProvider>
          <SessaoExpiradaListener />
          <Routes>
            <Route path="/" element={<LandingPage />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/cadastro" element={<CadastroPage />} />
            <Route path="/onboarding" element={<RotaProtegida><OnboardingPage /></RotaProtegida>} />
            <Route
              path="/selecionar-grupo"
              element={<RotaProtegida><SelecionarGrupoPage /></RotaProtegida>}
            />
            <Route
              path="/app/*"
              element={
                <RotaProtegida>
                  <GuardiaOrganizacao>
                    <LayoutApp />
                  </GuardiaOrganizacao>
                </RotaProtegida>
              }
            />
          </Routes>
        </AuthProvider>
      </ToastProvider>
    </BrowserRouter>
  )
}