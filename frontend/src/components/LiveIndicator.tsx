interface Props {
  live: boolean
}

export default function LiveIndicator({ live }: Props) {
  return (
    <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.375rem', fontSize: '0.75rem', color: live ? '#22c55e' : '#94a3b8' }}>
      <span
        style={{
          width: 8,
          height: 8,
          borderRadius: '50%',
          background: live ? '#22c55e' : '#94a3b8',
          animation: live ? 'pulse 1.5s infinite' : 'none',
          display: 'inline-block',
        }}
      />
      {live ? 'LIVE' : 'OFFLINE'}
      <style>{`@keyframes pulse { 0%,100%{opacity:1} 50%{opacity:0.3} }`}</style>
    </span>
  )
}
