function App() {
  return (
    <div className="min-h-screen bg-mycellis-bg-page flex items-center justify-center p-8">
      <div className="max-w-md w-full bg-mycellis-bg-card rounded-md border border-mycellis-border p-12">
        <div className="w-10 h-0.5 bg-mycellis-teal mb-10"></div>
        <h1 className="text-mycellis-teal text-sm font-extrabold tracking-[3px] uppercase mb-12">
          Mycellis
        </h1>
        <h2 className="text-mycellis-ink text-3xl font-bold tracking-tight mb-4">
          Tailwind is working.
        </h2>
        <p className="text-mycellis-text-secondary leading-relaxed">
          If you can see this with the brand colors applied, the Mycellis design system is wired up. Time to build the real app.
        </p>
      </div>
    </div>
  )
}

export default App