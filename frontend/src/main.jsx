
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './styles/global.css'
import './styles/sidebar.css'
import './styles/topbar.css'
import './styles/dashboard.css'
import './styles/members.css'
import './styles/components.css'
import './styles/responsive.css'
import './styles/professional.css'
import './styles/mobile.css'
import './styles/memberships.css'

import App from './App.jsx'
import AuthGate from './components/AuthGate.jsx'
import './styles/auth.css'
import './styles/member-portal.css'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <AuthGate>{(authProps) => <App {...authProps} />}</AuthGate>
  </StrictMode>,
)
