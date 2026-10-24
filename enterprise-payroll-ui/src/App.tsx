import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import ProtectedRoute from './routes/ProtectedRoute';
import AppLayout from './components/layout/AppLayout';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import EmployeeDirectory from './pages/EmployeeDirectory';
import FormulaStudio from './pages/FormulaStudio';
import PayrollConsole from './pages/PayrollConsole';
import Reports from './pages/Reports';
import './index.css';

/**
 * Root application component.
 *
 * Route hierarchy:
 *   /login                → Public login page
 *   /                     → Protected shell (AppLayout)
 *     /dashboard          → Executive Dashboard (default)
 *     /employees          → HR Employee Directory
 *     /formula-studio     → Formula Studio
 *     /payroll-console    → Payroll Console
 *     /reports            → Reports & Payslips
 *     /                   → Redirects to /dashboard
 *   *                     → Catch-all → /login
 */
function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Public */}
        <Route path="/login" element={<Login />} />

        {/* Protected shell */}
        <Route element={<ProtectedRoute />}>
          <Route element={<AppLayout />}>
            <Route path="/" element={<Navigate to="/dashboard" replace />} />
            <Route path="/dashboard" element={<Dashboard />} />
            <Route path="/employees" element={<EmployeeDirectory />} />
            <Route path="/formula-studio" element={<FormulaStudio />} />
            <Route path="/payroll-console" element={<PayrollConsole />} />
            <Route path="/reports" element={<Reports />} />
          </Route>
        </Route>

        {/* Catch-all */}
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
