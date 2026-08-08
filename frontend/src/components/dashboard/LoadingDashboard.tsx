import { MycellisFlowerLoader } from '../shared/MycellisFlowerLoader';

const GRID_COLS = 'grid-cols-[2fr_1fr_2fr_0.5fr_0.5fr_0.3fr]';
const HEADER_LABELS = ['Stalk', 'State', 'Last 40 pulses', 'Latency', 'Uptime'];
const SKELETON_ROW_COUNT = 5;
const SPARKLINE_BAR_COUNT = 20;

export function LoadingDashboard() {
  return (
    <div>
      <div className="flex flex-col items-center justify-center gap-3 py-10">
        <MycellisFlowerLoader size="lg" />
        <p className="font-mono uppercase text-xs tracking-wider text-ink-subtle">
          Gathering pulses…
        </p>
      </div>

      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-8">
        {Array.from({ length: 4 }).map((_, i) => (
          <div key={i} className="rounded-md border border-hairline bg-surface-raised p-5">
            <SkeletonBar className="w-20 h-3 mb-3" />
            <SkeletonBar className="w-14 h-8" />
          </div>
        ))}
      </div>

      <div className="border border-hairline bg-surface-raised rounded-md overflow-hidden">
        <div className={`grid ${GRID_COLS} gap-4 py-3 px-4 border-b border-hairline`}>
          {HEADER_LABELS.map((label) => (
            <span key={label} className="font-mono text-xs uppercase tracking-wider text-ink-subtle">
              {label}
            </span>
          ))}
        </div>
        {Array.from({ length: SKELETON_ROW_COUNT }).map((_, i) => (
          <div
            key={i}
            className={`grid ${GRID_COLS} items-center gap-4 py-3 px-4 border-b border-hairline`}
          >
            <div className="flex items-center gap-3">
              <SkeletonBar className="w-2.5 h-2.5 rounded-full flex-shrink-0" />
              <div>
                <SkeletonBar className="w-32 h-3.5 mb-1.5" />
                <SkeletonBar className="w-20 h-2.5" />
              </div>
            </div>

            <SkeletonBar className="w-16 h-4" />

            <div className="flex items-end gap-[1px] h-5">
              {Array.from({ length: SPARKLINE_BAR_COUNT }).map((_, j) => (
                <span key={j} className="w-[3px] h-1 bg-surface-sunken" />
              ))}
            </div>

            <SkeletonBar className="w-10 h-4" />
            <SkeletonBar className="w-10 h-4" />
          </div>
        ))}
      </div>
    </div>
  );
}

function SkeletonBar({ className }: { className: string }) {
  return <span className={`block bg-surface-sunken rounded ${className}`} />;
}
