import { BrowserRouter, Routes, Route } from 'react-router-dom';
import HomePage from './pages/HomePage';
import LoginPage from './pages/LoginPage';
import SignupPage from './pages/SignupPage';
import VerifyPage from './pages/VerifyPage';
import ForgotPasswordPage from './pages/ForgotPasswordPage';
import ResetPasswordPage from './pages/ResetPasswordPage';
import ResendVerificationPage from './pages/ResendVerificationPage';
import VerifyPendingPage from './pages/VerifyPendingPage';
import GuidePage from './pages/GuidePage';
import DashboardPage from './pages/DashboardPage';
import CreateStalkPage from './pages/CreateStalkPage';
import StalkDetailPage from './pages/StalkDetailPage';
import EditStalkPage from './pages/EditStalkPage';
import SettingsPage from './pages/SettingsPage';
import ChangePasswordPage from './pages/ChangePasswordPage';
import ChangeEmailPage from './pages/ChangeEmailPage';
import PublicStatusPage from './pages/PublicStatusPage';
import SuperAdminOrganizationsPage from './pages/SuperAdminOrganizationsPage';
import SuperAdminUsersPage from './pages/SuperAdminUsersPage';
import SuperAdminOrgStalksPage from './pages/SuperAdminOrgStalksPage';
import { ProtectedRoute } from './routes/ProtectedRoute';
import { SuperAdminRoute } from './components/superadmin/SuperAdminRoute';
import AuthenticatedLayout from './components/layout/AuthenticatedLayout';

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/signup" element={<SignupPage />} />
        <Route path="/verify" element={<VerifyPage />} />
        <Route path="/forgot-password" element={<ForgotPasswordPage />} />
        <Route path="/reset-password" element={<ResetPasswordPage />} />
        <Route path="/resend-verification" element={<ResendVerificationPage />} />
        <Route path="/verify-pending" element={<VerifyPendingPage />} />
        <Route path="/guide" element={<GuidePage />} />
        <Route path="/status/:slug" element={<PublicStatusPage />} />
        <Route element={<ProtectedRoute />}>
          <Route element={<AuthenticatedLayout />}>
            <Route path="/dashboard" element={<DashboardPage />} />
            <Route path="/stalks/new" element={<CreateStalkPage />} />
            <Route path="/stalks/:id" element={<StalkDetailPage />} />
            <Route path="/stalks/:id/edit" element={<EditStalkPage />} />
            <Route path="/settings" element={<SettingsPage />} />
            <Route path="/settings/change-password" element={<ChangePasswordPage />} />
            <Route path="/settings/change-email" element={<ChangeEmailPage />} />
            <Route
              path="/super-admin/organizations"
              element={
                <SuperAdminRoute>
                  <SuperAdminOrganizationsPage />
                </SuperAdminRoute>
              }
            />
            <Route
              path="/super-admin/users"
              element={
                <SuperAdminRoute>
                  <SuperAdminUsersPage />
                </SuperAdminRoute>
              }
            />
            <Route
              path="/super-admin/organizations/:orgId/stalks"
              element={
                <SuperAdminRoute>
                  <SuperAdminOrgStalksPage />
                </SuperAdminRoute>
              }
            />
          </Route>
        </Route>
      </Routes>
    </BrowserRouter>
  );
}