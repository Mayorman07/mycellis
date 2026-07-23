import { Outlet } from 'react-router-dom';
import { DashboardHeader } from '../dashboard/DashboardHeader';
import { useSession } from '../../lib/hooks/useSession';

export default function AuthenticatedLayout() {
  const session = useSession();
  const orgName = session.data?.organization.name ?? '';
  const planTier = session.data?.organization.planTier ?? 'FREE';
  const userInitials = session.data
    ? `${session.data.user.firstName.charAt(0)}${session.data.user.lastName.charAt(0)}`.toUpperCase()
    : '';

  return (
    <>
      <DashboardHeader orgName={orgName} planTier={planTier} userInitials={userInitials} />
      <Outlet />
    </>
  );
}
