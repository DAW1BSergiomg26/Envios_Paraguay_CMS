export function SkeletonRow() {
  return (
    <tr className="skeleton-row">
      <td><div className="skeleton skeleton--sm" /></td>
      <td><div className="skeleton skeleton--md" /></td>
      <td><div className="skeleton skeleton--lg" /></td>
      <td><div className="skeleton skeleton--md" /></td>
      <td><div className="skeleton skeleton--md" /></td>
      <td><div className="skeleton skeleton--sm" /></td>
    </tr>
  );
}

export function SkeletonCard() {
  return (
    <div className="skeleton-card">
      <div className="skeleton skeleton--icon" />
      <div className="skeleton-card-body">
        <div className="skeleton skeleton--value" />
        <div className="skeleton skeleton--label" />
      </div>
    </div>
  );
}
