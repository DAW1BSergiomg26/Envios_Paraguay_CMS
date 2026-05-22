export default function StatsCard({ label, value, icon, color }) {
  return (
    <div className="stats-card" style={{ '--accent': color }}>
      <div className="stats-card-icon">{icon}</div>
      <div className="stats-card-body">
        <span className="stats-card-value">{value}</span>
        <span className="stats-card-label">{label}</span>
      </div>
    </div>
  );
}
