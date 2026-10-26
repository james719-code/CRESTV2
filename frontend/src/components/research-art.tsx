import type { GroupStatus } from '../domain'

export function ResearchArt({ variant = 'paper' }: { variant?: 'paper' | 'empty' }) {
  return (
    <svg
      className={`research-art art-${variant}`}
      viewBox="0 0 240 140"
      fill="none"
      aria-hidden="true"
    >
      <circle cx="120" cy="70" r="58" fill="currentColor" opacity=".06" />
      <path
        d="M39 106H202M54 37h20M64 27v20M186 77h13M192 71v13"
        stroke="currentColor"
        strokeWidth="2"
        opacity=".2"
      />
      <rect
        x="76"
        y="30"
        width="83"
        height="96"
        rx="6"
        fill="var(--card)"
        stroke="currentColor"
        opacity=".3"
        transform="rotate(-9 76 30)"
      />
      <path
        d="M94 19h56l20 20v77a5 5 0 0 1-5 5H94a5 5 0 0 1-5-5V24a5 5 0 0 1 5-5Z"
        fill="var(--card)"
        stroke="currentColor"
        strokeWidth="1.5"
      />
      <path d="M150 19v16a4 4 0 0 0 4 4h16" stroke="currentColor" strokeWidth="1.5" />
      <rect x="102" y="38" width="28" height="5" rx="2.5" fill="currentColor" opacity=".6" />
      <path
        d="M102 54h53M102 62h43M102 70h50"
        stroke="currentColor"
        strokeWidth="3"
        strokeLinecap="round"
        opacity=".17"
      />
      {variant === 'paper' ? (
        <>
          <path
            d="M105 101V88M119 101V80M133 101V91M147 101V76"
            stroke="currentColor"
            strokeWidth="6"
            strokeLinecap="round"
            opacity=".55"
          />
          <path d="M100 108h55" stroke="currentColor" opacity=".2" />
        </>
      ) : (
        <>
          <circle
            cx="163"
            cy="99"
            r="19"
            fill="var(--card)"
            stroke="currentColor"
            strokeWidth="2.5"
          />
          <path
            d="m177 113 15 15M155 99h16M163 91v16"
            stroke="currentColor"
            strokeWidth="2.5"
            strokeLinecap="round"
          />
        </>
      )}
      <circle cx="49" cy="76" r="4" fill="currentColor" opacity=".3" />
      <circle cx="187" cy="27" r="3" fill="currentColor" opacity=".4" />
    </svg>
  )
}

export function ResearchSteps({ status }: { status?: GroupStatus }) {
  const step = !status ? -1 : status === 'approved' ? 2 : status === 'preparing' ? 0 : 1
  return (
    <ol
      className={`research-steps steps-${status || 'unassigned'}`}
      aria-label="Group submission stages"
    >
      {['Preparing', status === 'changes' ? 'Revising' : 'Review', 'Approved'].map(
        (label, index) => (
          <li
            key={label}
            className={index < step ? 'step-done' : index === step ? 'step-current' : ''}
            aria-current={index === step ? 'step' : undefined}
          >
            <span aria-hidden="true">{index < step ? '✓' : index + 1}</span>
            {label}
          </li>
        ),
      )}
    </ol>
  )
}
