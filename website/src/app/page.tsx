import { Shield, Fingerprint, Database, Zap, Github, Download } from 'lucide-react';

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
            <div className="w-8 h-8 rounded bg-[#3DDC84] flex items-center justify-center p-1">
              <img src="/logo.svg" alt="SolanaSuper Logo" className="w-full h-full object-contain" />
            </div>
            Solana<span className="text-[#3DDC84] font-light">Super</span>
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
        <div className="inline-block px-4 py-1.5 rounded-full glass-card text-xs font-semibold text-[#00FFA3] tracking-widest uppercase mb-8 border-[#00FFA3]/20">
          Powered by SovereignLife OS Infrastructure
        </div>

        <h1 className="text-6xl md:text-8xl font-black tracking-tighter mb-6 leading-[1.1]">
          The Decentralized <br className="hidden md:block" />
          <span className="text-[#3DDC84]">Super App.</span>
        </h1>

        <p className="text-xl md:text-2xl text-gray-400 max-w-3xl mb-12 font-light leading-relaxed">
          Your portal to the entire Solana network. Manage wealth, invest in DeFi, vote anonymously, and transact 100% offline. One app to rule them all.
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
      <div className="py-10 border-y border-white/5 bg-black/40 backdrop-blur-md z-10 relative">
        <div className="max-w-7xl mx-auto px-6 flex flex-wrap justify-center items-center gap-12 sm:gap-20 opacity-60 hover:opacity-100 transition-opacity duration-500">
          <img src="https://cryptologos.cc/logos/solana-sol-logo.svg?v=025" alt="Solana" title="Solana" className="h-8 md:h-10 w-auto hover:-translate-y-1 transition-transform" />
          <img src="https://logo.clearbit.com/jito.network" alt="Jito" title="Jito Networks" className="h-8 md:h-10 w-auto hover:-translate-y-1 transition-transform object-contain grayscale hover:grayscale-0" />
          <img src="https://logo.clearbit.com/arcium.com" alt="Arcium" title="Arcium" className="h-8 md:h-10 w-auto hover:-translate-y-1 transition-transform object-contain grayscale hover:grayscale-0" />
          <img src="https://upload.wikimedia.org/wikipedia/commons/1/18/Ipfs-logo-1024-ice-text.png" alt="IPFS" title="IPFS" className="h-8 md:h-10 w-auto hover:-translate-y-1 transition-transform object-contain" />
          <img src="https://logo.clearbit.com/jup.ag" alt="Jupiter" title="Jupiter" className="h-8 md:h-10 w-auto hover:-translate-y-1 transition-transform object-contain grayscale hover:grayscale-0 rounded-full" />
          <img src="https://logo.clearbit.com/drift.trade" alt="Drift" title="Drift Protocol" className="h-8 md:h-10 w-auto hover:-translate-y-1 transition-transform object-contain grayscale hover:grayscale-0" />
        </div>
      </div>

      {/* Features Grid & Investment Demo */}
      <section id="features" className="py-32 px-6 max-w-7xl mx-auto relative z-10 border-t border-white/5">
        <div className="flex flex-col md:flex-row gap-16 items-center">
          <div className="flex-1">
            <h2 className="text-4xl md:text-5xl font-bold mb-4">A Permissionless Economy</h2>
            <p className="text-xl text-gray-400 mb-10 leading-relaxed">Your treasury shouldn&apos;t sit idle. SolanaSuper gives you direct access to deep ecosystem liquidity straight from your mobile OS.</p>

            <div className="space-y-8">
              <div className="flex items-start gap-4">
                <div className="w-12 h-12 rounded-2xl bg-[#00FFA3]/10 flex items-center justify-center shrink-0">
                  <Zap className="w-6 h-6 text-[#00FFA3]" />
                </div>
                <div>
                  <h3 className="text-2xl font-bold mb-2">Instant Swaps (Jupiter)</h3>
                  <p className="text-gray-400">Instantly trade assets with zero custodial risk, routed seamlessly for the absolute best execution price across all DEXs.</p>
                </div>
              </div>
              <div className="flex items-start gap-4">
                <div className="w-12 h-12 rounded-2xl bg-[#03E1FF]/10 flex items-center justify-center shrink-0">
                  <Database className="w-6 h-6 text-[#03E1FF]" />
                </div>
                <div>
                  <h3 className="text-2xl font-bold mb-2">Passive Yield (JitoSOL)</h3>
                  <p className="text-gray-400">Your wealth grows while you sleep. Stake holdings natively to earn network yield while securing the Solana blockchain.</p>
                </div>
              </div>
            </div>
          </div>
          <div className="flex-1 flex justify-center floating">
            <div className="relative rounded-[2.5rem] overflow-hidden border border-white/10 shadow-[0_0_60px_rgba(3,225,255,0.15)] max-w-[300px] glass-card">
              {/* USER MUST PROVIDE THE ATTACHED INVEST SCREENSHOT HERE */}
              <img src="/invest.png" alt="SolanaSuper Invest Dashboard Demo" className="w-full h-auto object-cover" />
            </div>
          </div>
        </div>
      </section>

      {/* Offline Feature Highlights */}
      <section id="offline" className="py-32 relative z-10 bg-black/40 border-y border-white/5">
        <div className="max-w-7xl mx-auto px-6 grid md:grid-cols-2 gap-20 items-center">
          <div className="order-2 md:order-1 flex justify-center floating" style={{ animationDelay: "1s" }}>
            <div className="relative rounded-[2.5rem] overflow-hidden border border-white/10 shadow-[0_0_60px_rgba(153,69,255,0.15)] max-w-[300px] glass-card">
              {/* USER MUST PROVIDE THE ATTACHED PROFILE/WALLET SCREENSHOT HERE */}
              <img src="/profile.png" alt="SolanaSuper Sovereign Identity Demo" className="w-full h-auto object-cover" />
            </div>
          </div>
          <div className="order-1 md:order-2">
            <div className="inline-block px-4 py-1.5 rounded-full glass-card text-xs font-semibold text-purple-400 tracking-widest uppercase mb-6 border-purple-500/20">
              SovereignLife Infra: Bluetooth Mesh
            </div>
            <h2 className="text-4xl md:text-5xl font-bold mb-6 leading-tight">Send cash off-the-grid.<br />No ISP Required.</h2>
            <p className="text-lg text-gray-400 mb-8 leading-relaxed">
              If the grid goes down, commerce continues. We implement Jack Dorsey&apos;s &quot;Bitchat&quot; mesh vision natively on Solana using mathematically locked <strong>Durable Nonces.</strong>
            </p>
            <ul className="space-y-4 mb-8">
              <li className="flex items-start gap-3">
                <div className="w-6 h-6 rounded-full bg-purple-500/20 text-purple-400 flex items-center justify-center text-sm font-bold mt-1">1</div>
                <p className="text-gray-300">Biometrically sign an offline transfer directly to a peer via Bluetooth.</p>
              </li>
              <li className="flex items-start gap-3">
                <div className="w-6 h-6 rounded-full bg-purple-500/20 text-purple-400 flex items-center justify-center text-sm font-bold mt-1">2</div>
                <p className="text-gray-300">The 32-byte payload is instantly verified and locked locally by both phones.</p>
              </li>
              <li className="flex items-start gap-3">
                <div className="w-6 h-6 rounded-full bg-[#00FFA3]/20 text-[#00FFA3] flex items-center justify-center text-sm font-bold mt-1">3</div>
                <p className="text-gray-300">The stealth SyncWorker bridges the transaction to the Solana ledger automatically upon next Wi-Fi contact.</p>
              </li>
            </ul>
          </div>
        </div>
      </section>

      {/* Identity & Health Demo Panels */}
      <section id="data" className="py-32 px-6 max-w-7xl mx-auto relative z-10 text-center">
        <h2 className="text-4xl md:text-5xl font-bold mb-6">Unbreakable Sovereignty</h2>
        <p className="text-xl text-gray-400 max-w-3xl mx-auto mb-20 leading-relaxed">Centralized cloud providers are single points of failure. Become untethered with IPFS and Arcium confidential computing.</p>

        <div className="grid md:grid-cols-2 gap-16 text-left">

          <div>
            <div className="mb-10 px-6">
              <Fingerprint className="w-10 h-10 text-[#03E1FF] mb-6" />
              <h3 className="text-3xl font-bold mb-4">Health Vault (IPFS)</h3>
              <p className="text-gray-400 leading-relaxed">
                Medical records and property deeds encrypted natively on-device and pinned to decentralized IPFS. Powered securely by Arcium.
              </p>
            </div>
            <div className="flex justify-center floating" style={{ animationDelay: "2s" }}>
              <div className="relative rounded-[2.5rem] overflow-hidden border border-white/10 shadow-[0_0_60px_rgba(3,225,255,0.15)] max-w-[280px] glass-card">
                {/* USER MUST PROVIDE THE ATTACHED HEALTH SCREENSHOT HERE */}
                <img src="/health.png" alt="SolanaSuper Health Vault Demo" className="w-full h-auto object-cover" />
              </div>
            </div>
          </div>

          <div>
            <div className="mb-10 px-6">
              <Shield className="w-10 h-10 text-indigo-400 mb-6" />
              <h3 className="text-3xl font-bold mb-4">Immutable Governance</h3>
              <p className="text-gray-400 leading-relaxed">
                Submit transparent community referendums. The network executes democratic consensus using impenetrable smart contracts.
              </p>
            </div>
            <div className="flex justify-center floating" style={{ animationDelay: "0.5s" }}>
              <div className="relative rounded-[2.5rem] overflow-hidden border border-white/10 shadow-[0_0_60px_rgba(20,241,149,0.15)] max-w-[280px] glass-card">
                {/* USER MUST PROVIDE THE ATTACHED GOVERNANCE SCREENSHOT HERE */}
                <img src="/gov.png" alt="SolanaSuper Democracy Democracy Demo" className="w-full h-auto object-cover" />
              </div>
            </div>
          </div>

        </div>
      </section>

      {/* Footer */}
      <footer className="border-t border-white/10 mt-20 bg-black relative z-10 py-12">
        <div className="max-w-7xl mx-auto px-6 flex flex-col items-center">
          <div className="w-12 h-12 rounded bg-[#3DDC84] flex items-center justify-center p-1 mb-6">
            <img src="/logo.svg" alt="SolanaSuper Logo" className="w-full h-full object-contain" />
          </div>
          <h2 className="text-2xl font-bold tracking-tighter text-white mb-8">
            Solana<span className="text-[#3DDC84] font-light">Super</span>
          </h2>
          <div className="flex gap-6 mb-8">
            <a href="https://github.com/adidshaft/SolanaSuper/releases/latest/download/app-debug.apk" className="text-gray-400 hover:text-white transition-colors">Download Latest APK</a>
            <a href="https://github.com/adidshaft/SolanaSuper" target="_blank" className="text-gray-400 hover:text-white transition-colors">Github Source</a>
          </div>
          <p className="text-gray-600 text-sm mb-2">Powered by Native Decentralized Networks & SovereignLife OS. Zero Compromises.</p>
          <a href="https://kyokasuigetsu.xyz" target="_blank" rel="noopener noreferrer" className="text-[#3DDC84] hover:text-[#00FFA3] text-sm font-medium tracking-wide transition-colors">BUILT BY KYOKA SUIGETSU</a>
        </div>
      </footer>
    </main>
  );
}
