import ShipmentStatusChart from './ShipmentStatusChart';
import ActivityChart from './ActivityChart';
import AnalyticsKPIs from './AnalyticsKPIs';

export default function AnalyticsSection({ envios = [], loading }) {
  return (
    <section className="analytics-section">
      <h2 className="section-title">Analytics</h2>
      <AnalyticsKPIs envios={envios} loading={loading} />
      <div className="charts-grid">
        <ShipmentStatusChart envios={envios} loading={loading} />
        <ActivityChart envios={envios} loading={loading} />
      </div>
    </section>
  );
}
