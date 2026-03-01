import { Shield, Fingerprint, Database, Zap, Cpu, Network, Github, Download } from 'lucide-react';

export default function Home() {
  return (
    <main className="min-h-screen relative overflow-hidden bg-[#0A0D14] font-sans selection:bg-[#03E1FF] selection:text-black">
      {/* Background Orbs */}
      <div className="glow-bg top-[-20%] left-[-10%] opacity-50" />
      <div className="glow-bg bottom-[-20%] right-[-10%] opacity-30 bg-purple-500/20" />

      {/* Navigation */}
      <nav className="fixed w-full z-50 glass-card border-b-0 border-white/5 py-4">
        <div className="max-w-7xl mx-auto px-6 flex justify-between items-center">
          <div className="text-2xl font-bold tracking-tighter text-white flex items-center gap-2">
            <div className="w-8 h-8 rounded bg-gradient-to-br from-[#00FFA3] to-[#03E1FF] flex items-center justify-center">
              <Shield className="w-5 h-5 text-black" />
            </div>
            SovereignLife<span className="text-gray-400 font-light">OS</span>
          </div>
          <div className="hidden md:flex gap-8 text-sm font-medium text-gray-400">
            <a href="#features" className="hover:text-white transition-colors">Features</a>
            <a href="#offline" className="hover:text-white transition-colors">Mesh Network</a>
            <a href="#data" className="hover:text-white transition-colors">Sovereignty</a>
          </div>
          <div className="flex gap-4">
            <a
              href="https://github.com/adidshaft/SolanaSuper"
              target="_blank"
              rel="noopener noreferrer"
              className="flex items-center gap-2 text-sm font-medium text-white hover:text-[#03E1FF] transition-colors"
            >
              <Github className="w-4 h-4" /> Source Code
            </a>
          </div>
        </div>
      </nav>

      {/* Hero Section */}
      <section className="relative pt-40 pb-20 px-6 max-w-7xl mx-auto z-10 flex flex-col items-center text-center">
        <div className="inline-block px-4 py-1.5 rounded-full glass-card text-xs font-semibold text-[#03E1FF] tracking-widest uppercase mb-8 border-[#03E1FF]/20">
          The Operating System for Protocol Nations
        </div>

        <h1 className="text-6xl md:text-8xl font-black tracking-tighter mb-6 leading-[1.1]">
          Launch Your <br className="hidden md:block" />
          <span className="text-gradient">Digital Country.</span>
        </h1>

        <p className="text-xl md:text-2xl text-gray-400 max-w-3xl mb-12 font-light leading-relaxed">
          Manage money, vote on governance, hold encrypted health records, and transact 100% offline via Bluetooth. Built natively on the Solana ecosystem.
        </p>

        <div className="flex flex-col sm:flex-row gap-6">
          <a
            href="https://github.com/adidshaft/SolanaSuper/releases/latest/download/app-debug.apk"
            className="flex items-center justify-center gap-3 px-8 py-4 rounded-xl bg-white text-black font-bold text-lg hover:bg-gray-200 transition-all hover:scale-105 active:scale-95 shadow-[0_0_40px_rgba(255,255,255,0.3)]"
          >
            <Download className="w-5 h-5" />
            Download APK
          </a>
          <a
            href="https://github.com/adidshaft/SolanaSuper"
            target="_blank"
            rel="noopener noreferrer"
            className="flex items-center justify-center gap-3 px-8 py-4 rounded-xl glass-card text-white font-bold text-lg hover:bg-white/10 transition-all hover:scale-105"
          >
            <Github className="w-5 h-5" />
            View Source Code
          </a>
        </div>
      </section>

      {/* Ecosystem Integration Bar */}
      <div className="py-10 border-y border-white/5 bg-black/20 z-10 relative">
        <div className="max-w-7xl mx-auto px-6 flex flex-wrap justify-center gap-12 sm:gap-24 opacity-50 grayscale hover:grayscale-0 transition-all duration-500">
          {/* Ecosystem Logos - stylized text for demo */}
          <div className="text-xl font-bold flex items-center gap-2"><div className="w-3 h-3 rounded-full bg-[#00FFA3]"></div> Solana</div>
          <div className="text-xl font-bold flex items-center gap-2"><div className="w-3 h-3 rounded-full bg-[#03E1FF]"></div> Arcium</div>
          <div className="text-xl font-bold flex items-center gap-2"><div className="w-3 h-3 rounded-full bg-blue-400"></div> IPFS</div>
          <div className="text-xl font-bold flex items-center gap-2"><div className="w-3 h-3 rounded-full bg-green-400"></div> Jupiter</div>
          <div className="text-xl font-bold flex items-center gap-2"><div className="w-3 h-3 rounded-full bg-purple-500"></div> Drift</div>
        </div>
      </div>

      {/* Features Grid */}
      <section id="features" className="py-32 px-6 max-w-7xl mx-auto relative z-10">
        <div className="mb-20">
          <h2 className="text-4xl md:text-5xl font-bold mb-4">A Permissionless Economy</h2>
          <p className="text-xl text-gray-400 max-w-2xl">A sovereign treasury shouldn&apos;t sit idle. Execute trades, earn yield, and manage wealth directly from your smartphone.</p>
        </div>

        <div className="grid md:grid-cols-3 gap-8">
          <div className="glass-card p-8 rounded-3xl hover:-translate-y-2 transition-transform duration-300">
            <Zap className="w-10 h-10 text-[#00FFA3] mb-6" />
            <h3 className="text-2xl font-bold mb-4">Instant Swaps (Jupiter)</h3>
            <p className="text-gray-400 leading-relaxed">Instantly trade assets with zero custodial risk, routed seamlessly for the absolute best execution price across all DEXs.</p>
          </div>
          <div className="glass-card p-8 rounded-3xl hover:-translate-y-2 transition-transform duration-300">
            <Database className="w-10 h-10 text-[#03E1FF] mb-6" />
            <h3 className="text-2xl font-bold mb-4">Passive Yield (JitoSOL)</h3>
            <p className="text-gray-400 leading-relaxed">Your treasury grows while you sleep. Stake holdings natively to earn network yield while securing the Solana blockchain.</p>
          </div>
          <div className="glass-card p-8 rounded-3xl hover:-translate-y-2 transition-transform duration-300">
            <Cpu className="w-10 h-10 text-[#DC1FFF] mb-6" />
            <h3 className="text-2xl font-bold mb-4">Derivatives (Drift)</h3>
            <p className="text-gray-400 leading-relaxed">For advanced states. Access on-chain leverage, borrowing, and lending directly inside your localized operating system.</p>
          </div>
        </div>
      </section>

      {/* Offline Feature Highlights */}
      <section id="offline" className="py-32 relative z-10 bg-black/40 border-y border-white/5">
        <div className="max-w-7xl mx-auto px-6 grid md:grid-cols-2 gap-20 items-center">
          <div>
            <div className="inline-block px-4 py-1.5 rounded-full glass-card text-xs font-semibold text-purple-400 tracking-widest uppercase mb-6 border-purple-500/20">
              True Peer-to-Peer Mesh
            </div>
            <h2 className="text-4xl md:text-5xl font-bold mb-6 leading-tight">Send cash off-the-grid.<br />No ISP Required.</h2>
            <p className="text-lg text-gray-400 mb-8 leading-relaxed">
              If the grid goes down, your nation survives. SovereignLife OS implements Jack Dorsey&apos;s &quot;Bitchat&quot; mesh vision natively on Solana using mathematically locked <strong>Durable Nonces.</strong>
            </p>
            <ul className="space-y-4 mb-8">
              <li className="flex items-start gap-3">
                <div className="w-6 h-6 rounded-full bg-purple-500/20 text-purple-400 flex items-center justify-center text-sm font-bold mt-1">1</div>
                <p className="text-gray-300">Biometrically sign an offline transfer directly to a peer via Bluetooth.</p>
              </li>
              <li className="flex items-start gap-3">
                <div className="w-6 h-6 rounded-full bg-purple-500/20 text-purple-400 flex items-center justify-center text-sm font-bold mt-1">2</div>
                <p className="text-gray-300">The 32-byte cryptographic payload is instantly verified and locked locally by both phones.</p>
              </li>
              <li className="flex items-start gap-3">
                <div className="w-6 h-6 rounded-full bg-[#00FFA3]/20 text-[#00FFA3] flex items-center justify-center text-sm font-bold mt-1">3</div>
                <p className="text-gray-300">The stealth SyncWorker bridges the transaction to the Solana ledger automatically upon next Wi-Fi contact.</p>
              </li>
            </ul>
          </div>
          <div className="relative aspect-square flex items-center justify-center">
            {/* Abstract Representation of Mesh Networking */}
            <div className="w-full h-full glass-card rounded-full absolute animate-pulse opacity-20"></div>
            <div className="w-3/4 h-3/4 border border-white/10 rounded-full absolute border-dashed rotate-45"></div>
            <div className="w-1/2 h-1/2 bg-gradient-to-tr from-purple-500 to-[#03E1FF] rounded-full blur-3xl opacity-50 absolute"></div>
            <Network className="w-32 h-32 text-white relative z-10 drop-shadow-2xl" />
          </div>
        </div>
      </section>

      {/* Identity & Health */}
      <section id="data" className="py-32 px-6 max-w-7xl mx-auto relative z-10 text-center">
        <h2 className="text-4xl md:text-5xl font-bold mb-6">Unbreakable Sovereignty</h2>
        <p className="text-xl text-gray-400 max-w-3xl mx-auto mb-20 leading-relaxed">Your money is yours, but what about your private data? Centralized cloud providers are single points of failure. Become untethered.</p>

        <div className="grid md:grid-cols-2 gap-8text-left">
          <div className="glass-card p-10 rounded-[40px] text-left border-l-4 border-[#03E1FF]">
            <Fingerprint className="w-12 h-12 text-[#03E1FF] mb-6" />
            <h3 className="text-3xl font-bold mb-4">Confidential IPFS Records</h3>
            <p className="text-gray-400 leading-relaxed mb-6">
              Critical life data—from medical files to property deeds—are natively encrypted directly on your device. It is then permanently pinned to the decentralized IPFS network making it immune to takedowns.
            </p>
            <p className="text-gray-400 leading-relaxed">
              We leverage <strong>Arcium&apos;s</strong> confidential computing environment, allowing smart contracts to calculate and verify data without ever exposing the raw contents.
            </p>
          </div>
          <div className="glass-card p-10 rounded-[40px] text-left border-l-4 border-indigo-400 mt-8 md:mt-0 ml-0 md:ml-8">
            <Shield className="w-12 h-12 text-indigo-400 mb-6" />
            <h3 className="text-3xl font-bold mb-4">Immutable Governance</h3>
            <p className="text-gray-400 leading-relaxed mb-6">
              A physical country needs a constitution and transparent voting. Why should your digital collective be any different?
            </p>
            <p className="text-gray-400 leading-relaxed">
              Submit community referendums cryptographically tied to your wallet weight. No stuffed ballot boxes. The network executes democratic consensus at light speed using Solana smart contracts.
            </p>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="border-t border-white/10 mt-20 bg-black relative z-10 py-12">
        <div className="max-w-7xl mx-auto px-6 flex flex-col items-center">
          <div className="w-12 h-12 rounded bg-gradient-to-br from-[#00FFA3] to-[#03E1FF] flex items-center justify-center mb-6">
            <Shield className="w-6 h-6 text-black" />
          </div>
          <h2 className="text-2xl font-bold tracking-tighter text-white mb-8">
            SovereignLife<span className="text-gray-400 font-light">OS</span>
          </h2>
          <div className="flex gap-6 mb-8">
            <a href="https://github.com/adidshaft/SolanaSuper/releases/latest/download/app-debug.apk" className="text-gray-400 hover:text-white transition-colors">Download Latest APK</a>
            <a href="https://github.com/adidshaft/SolanaSuper" target="_blank" className="text-gray-400 hover:text-white transition-colors">Github Source</a>
          </div>
          <p className="text-gray-600 text-sm">Powered by Native Decentralized Networks. Zero Compromises.</p>
        </div>
      </footer>
    </main>
  );
}
