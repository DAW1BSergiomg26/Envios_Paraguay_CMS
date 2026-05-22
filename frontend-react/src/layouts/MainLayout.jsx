import { Outlet } from 'react-router-dom';

export default function MainLayout() {
  return (
    <div className="app-layout">
      <main className="main-content">
        <Outlet />
      </main>
    </div>
  );
}
