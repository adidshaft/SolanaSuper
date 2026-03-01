'use client';

import { Shield, Fingerprint, Database, Zap, Github, Download, Wifi, Lock, ChevronRight } from 'lucide-react';

const APK_URL = 'https://github.com/adidshaft/SolanaSuper/releases/tag/latest';
const GITHUB_URL = 'https://github.com/adidshaft/SolanaSuper';

const PARTNERS = [
  { name: 'Solana',   logo: '/logo-solana.svg',  href: 'https://solana.com' },
  { name: 'Jito',     logo: '/logo-jito.svg',    href: 'https://jito.network' },
  { name: 'Jupiter',  logo: '/logo-jupiter.svg', href: 'https://jup.ag' },
  { name: 'Drift',    logo: '/logo-drift.svg',   href: 'https://drift.trade' },
  { name: 'Arcium',   logo: '/logo-arcium.svg',  href: 'https://arcium.com' },
  { name: 'IPFS',     logo: '/logo-ipfs.svg',    href: 'https://ipfs.tech' },
];

const STATS = [
  { value: '400ms', label: 'Avg. Transaction Speed' },
  { value: '100%', label: 'Self-Custody' },
  { value: '0', label: 'Central Servers' },
  { value: '∞', label: 'Offline Range (BT Mesh)' },
];

const TECH_PILLS = [
  'Durable Nonces', 'BIP-39 HD Wallet', 'AES-256 Encryption',
  'SQLCipher DB', 'Biometric Auth', 'ZK Proofs', 'MPC Computing',
  'Bluetooth Mesh', 'Ed25519 Signing', 'IPFS Pinning',
];

export default function Home() {
  return (
    <main className="min-h-screen relative overflow-hidden bg-[#080B12] font-sans selection:bg-[#14F195] selection:text-black">

      {/* === Background === */}
      <div className="pointer-events-none fixed inset-0 overflow-hidden">
        <div className="absolute top-[-30%] left-[-15%] w-[800px] h-[800px] bg-[#9945FF]/10 rounded-full blur-[120px]" />
        <div className="absolute bottom-[-20%] right-[-10%] w-[700px] h-[700px] bg-[#14F195]/8 rounded-full blur-[100px]" />
        <div className="absolute top-[40%] left-[50%] w-[500px] h-[500px] bg-[#03E1FF]/6 rounded-full blur-[80px]" />
        {/* Grid overlay */}
        <div className="absolute inset-0 opacity-[0.025]" style={{
          backgroundImage: 'linear-gradient(#fff 1px, transparent 1px), linear-gradient(90deg, #fff 1px, transparent 1px)',
          backgroundSize: '60px 60px'
        }} />
      </div>

      {/* === Nav === */}
      <nav className="fixed w-full z-50 py-4 border-b border-white/5 backdrop-blur-xl bg-black/30">
        <div className="max-w-7xl mx-auto px-6 flex justify-between items-center">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-gradient-to-br from-[#14F195] to-[#9945FF] p-[2px]">
              <div className="w-full h-full rounded-[10px] bg-[#080B12] flex items-center justify-center">
                <img src="/logo.svg" alt="Logo" className="w-5 h-5 object-contain" />
              </div>
            </div>
            <span className="text-lg font-bold tracking-tight text-white">
              Solana<span className="text-[#14F195]">Super</span>
            </span>
          </div>

          <div className="hidden md:flex items-center gap-8 text-sm font-medium text-gray-400">
            <a href="#features" className="hover:text-white transition-colors">Features</a>
            <a href="#offline"  className="hover:text-white transition-colors">Mesh Network</a>
            <a href="#data"     className="hover:text-white transition-colors">Sovereignty</a>
            <a href="#tech"     className="hover:text-white transition-colors">Tech</a>
          </div>

          <div className="flex items-center gap-3">
            <a
              href={GITHUB_URL}
              target="_blank"
              rel="noopener noreferrer"
              className="flex items-center gap-1.5 text-sm font-medium text-gray-400 hover:text-white transition-colors px-3 py-2 rounded-lg hover:bg-white/5"
            >
              <Github className="w-4 h-4" />
              <span className="hidden sm:inline">GitHub</span>
            </a>
            <a
              href={APK_URL}
              target="_blank"
              rel="noopener noreferrer"
              className="flex items-center gap-1.5 text-sm font-bold px-4 py-2 rounded-lg bg-[#14F195] text-black hover:bg-[#00FFA3] transition-all hover:scale-105 active:scale-95"
            >
              <Download className="w-4 h-4" />
              Download APK
            </a>
          </div>
        </div>
      </nav>

      {/* === Hero === */}
      <section className="relative pt-44 pb-24 px-6 max-w-7xl mx-auto z-10 flex flex-col items-center text-center">
        {/* Badge */}
        <div className="flex items-center gap-2 px-4 py-1.5 rounded-full border border-[#14F195]/30 bg-[#14F195]/5 text-xs font-semibold text-[#14F195] tracking-widest uppercase mb-10">
          <span className="w-1.5 h-1.5 rounded-full bg-[#14F195] animate-pulse inline-block" />
          Powered by SovereignLife OS Infrastructure
        </div>

        <h1 className="text-6xl md:text-8xl font-black tracking-tighter mb-6 leading-[1.05]">
          The Decentralized
          <br className="hidden md:block" />
          <span className="bg-gradient-to-r from-[#14F195] via-[#03E1FF] to-[#9945FF] bg-clip-text text-transparent">
            Super App.
          </span>
        </h1>

        <p className="text-lg md:text-xl text-gray-400 max-w-2xl mb-12 font-light leading-relaxed">
          Your portal to the entire Solana network. Manage wealth, invest in DeFi,
          vote anonymously, and transact <strong className="text-white font-semibold">100% offline</strong>.
          One app to rule them all.
        </p>

        <div className="flex flex-col sm:flex-row gap-4 mb-16">
          <a
            href={APK_URL}
            target="_blank"
            rel="noopener noreferrer"
            className="group flex items-center justify-center gap-2.5 px-8 py-4 rounded-2xl bg-white text-black font-bold text-base hover:bg-[#14F195] transition-all duration-200 hover:scale-105 active:scale-95 shadow-[0_0_40px_rgba(20,241,149,0.3)]"
          >
            <Download className="w-5 h-5 group-hover:animate-bounce" />
            Download APK
            <ChevronRight className="w-4 h-4 opacity-50 group-hover:translate-x-1 transition-transform" />
          </a>
          <a
            href={GITHUB_URL}
            target="_blank"
            rel="noopener noreferrer"
            className="flex items-center justify-center gap-2.5 px-8 py-4 rounded-2xl border border-white/10 bg-white/5 backdrop-blur-sm text-white font-bold text-base hover:bg-white/10 hover:border-white/20 transition-all duration-200 hover:scale-105"
          >
            <Github className="w-5 h-5" />
            View Source Code
          </a>
        </div>

        {/* Stats Bar */}
        <div className="grid grid-cols-2 md:grid-cols-4 gap-px bg-white/5 rounded-2xl overflow-hidden border border-white/5 w-full max-w-3xl">
          {STATS.map((s) => (
            <div key={s.label} className="bg-[#080B12] px-6 py-5 text-center hover:bg-white/[0.03] transition-colors">
              <div className="text-2xl md:text-3xl font-black text-white mb-1">{s.value}</div>
              <div className="text-xs text-gray-500 font-medium">{s.label}</div>
            </div>
          ))}
        </div>
      </section>

      {/* === Ecosystem Bar === */}
      <div className="py-10 border-y border-white/5 bg-black/50 backdrop-blur-md z-10 relative">
        <p className="text-center text-xs font-semibold tracking-widest uppercase text-gray-600 mb-8">Built on the best infrastructure in crypto</p>
        <div className="max-w-5xl mx-auto px-6 flex flex-wrap justify-center items-center gap-10 sm:gap-16">
          {PARTNERS.map((p) => (
            <a
              key={p.name}
              href={p.href}
              target="_blank"
              rel="noopener noreferrer"
              className="group flex flex-col items-center gap-2 opacity-40 hover:opacity-100 transition-all duration-300 hover:-translate-y-1"
              title={p.name}
            >
              <img
                src={p.logo}
                alt={p.name}
                className="h-8 md:h-9 w-auto object-contain filter grayscale group-hover:grayscale-0 transition-all duration-300"
              />
              <span className="text-[10px] text-gray-600 group-hover:text-gray-300 font-medium tracking-widest uppercase transition-colors">{p.name}</span>
            </a>
          ))}
        </div>
      </div>

      {/* === Features: DeFi === */}
      <section id="features" className="py-32 px-6 max-w-7xl mx-auto relative z-10">
        <div className="flex flex-col md:flex-row gap-16 items-center">
          <div className="flex-1">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full border border-[#14F195]/20 bg-[#14F195]/5 text-xs font-bold text-[#14F195] tracking-widest uppercase mb-6">
              DeFi Hub
            </div>
            <h2 className="text-4xl md:text-5xl font-black tracking-tighter mb-5 leading-tight">
              A Permissionless<br />Economy in Your Pocket.
            </h2>
            <p className="text-lg text-gray-400 mb-10 leading-relaxed max-w-lg">
              Your treasury shouldn&apos;t sit idle. SolanaSuper gives you direct access to
              deep ecosystem liquidity straight from your mobile.
            </p>

            <div className="space-y-6">
              {[
                {
                  icon: Zap,
                  color: '#14F195',
                  title: 'Instant Swaps via Jupiter',
                  desc: 'Trade assets with zero custodial risk, routed for the absolute best execution price across all DEXs on Solana.',
                },
                {
                  icon: Database,
                  color: '#03E1FF',
                  title: 'Passive Yield via JitoSOL',
                  desc: 'Your wealth grows while you sleep. Stake natively to earn MEV-boosted validator yield, directly in-app.',
                },
                {
                  icon: Shield,
                  color: '#9945FF',
                  title: 'Perps via Drift Protocol',
                  desc: 'Go long or short on-chain with up to 10× leverage. No CEX, no KYC, no compromise.',
                },
              ].map(({ icon: Icon, color, title, desc }) => (
                <div key={title} className="flex items-start gap-4 group">
                  <div
                    className="w-11 h-11 rounded-2xl flex items-center justify-center shrink-0 transition-transform group-hover:scale-110"
                    style={{ background: `${color}15`, border: `1px solid ${color}25` }}
                  >
                    <Icon className="w-5 h-5" style={{ color }} />
                  </div>
                  <div>
                    <h3 className="font-bold text-white mb-1">{title}</h3>
                    <p className="text-sm text-gray-400 leading-relaxed">{desc}</p>
                  </div>
                </div>
              ))}
            </div>
          </div>

          <div className="flex-1 flex justify-center">
            <div className="relative">
              {/* Glow */}
              <div className="absolute inset-0 rounded-[2.5rem] bg-[#03E1FF]/20 blur-3xl scale-75" />
              <div className="relative rounded-[2.5rem] overflow-hidden border border-white/10 shadow-[0_0_60px_rgba(3,225,255,0.15)] max-w-[290px] bg-white/[0.02] backdrop-blur-sm floating">
                <img src="/invest.png" alt="SolanaSuper Invest Dashboard" className="w-full h-auto object-cover" />
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* === Features: Offline Mesh === */}
      <section id="offline" className="py-32 relative z-10 bg-gradient-to-b from-transparent via-black/40 to-transparent border-y border-white/5">
        <div className="max-w-7xl mx-auto px-6 grid md:grid-cols-2 gap-20 items-center">

          <div className="flex justify-center order-2 md:order-1">
            <div className="relative">
              <div className="absolute inset-0 rounded-[2.5rem] bg-[#9945FF]/20 blur-3xl scale-75" />
              <div className="relative rounded-[2.5rem] overflow-hidden border border-white/10 shadow-[0_0_60px_rgba(153,69,255,0.15)] max-w-[290px] bg-white/[0.02] floating" style={{ animationDelay: '1s' }}>
                <img src="/wallet.jpg" alt="SolanaSuper Offline Wallet" className="w-full h-auto object-cover" />
              </div>
            </div>
          </div>

          <div className="order-1 md:order-2">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full border border-purple-500/20 bg-purple-500/5 text-xs font-bold text-purple-400 tracking-widest uppercase mb-6">
              <Wifi className="w-3 h-3" /> Bluetooth Mesh
            </div>
            <h2 className="text-4xl md:text-5xl font-black tracking-tighter mb-5 leading-tight">
              Send cash off-the-grid.<br />No ISP Required.
            </h2>
            <p className="text-base text-gray-400 mb-8 leading-relaxed">
              If the grid goes down, commerce continues. We implement Jack Dorsey&apos;s
              &ldquo;Bitchat&rdquo; mesh vision natively on Solana using mathematically locked{' '}
              <strong className="text-white">Durable Nonces.</strong>
            </p>

            <div className="space-y-4">
              {[
                { n: '01', text: 'Biometrically sign an offline transfer directly to a peer via Bluetooth.' },
                { n: '02', text: 'The 32-byte Ed25519 payload is instantly verified and locked locally by both phones.' },
                { n: '03', text: 'The stealth SyncWorker bridges the transaction to the Solana ledger automatically upon next Wi-Fi contact.', highlight: true },
              ].map(({ n, text, highlight }) => (
                <div key={n} className="flex items-start gap-4 p-4 rounded-xl border border-white/5 bg-white/[0.02] hover:bg-white/[0.04] transition-colors">
                  <span className={`text-xs font-black font-mono mt-0.5 shrink-0 ${highlight ? 'text-[#14F195]' : 'text-gray-600'}`}>{n}</span>
                  <p className="text-sm text-gray-300 leading-relaxed">{text}</p>
                </div>
              ))}
            </div>
          </div>
        </div>
      </section>

      {/* === Features: Sovereignty === */}
      <section id="data" className="py-32 px-6 max-w-7xl mx-auto relative z-10 text-center">
        <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full border border-[#03E1FF]/20 bg-[#03E1FF]/5 text-xs font-bold text-[#03E1FF] tracking-widest uppercase mb-6">
          Data Sovereignty
        </div>
        <h2 className="text-4xl md:text-5xl font-black tracking-tighter mb-5">Unbreakable Sovereignty.</h2>
        <p className="text-lg text-gray-400 max-w-2xl mx-auto mb-20 leading-relaxed">
          Centralized cloud providers are single points of failure. Become untethered with
          IPFS distributed storage and Arcium confidential computing.
        </p>

        <div className="grid md:grid-cols-2 gap-12 text-left">
          {/* Health Vault */}
          <div className="group rounded-3xl border border-white/5 bg-white/[0.02] p-8 hover:bg-white/[0.04] hover:border-[#03E1FF]/20 transition-all duration-300">
            <div className="w-12 h-12 rounded-2xl bg-[#03E1FF]/10 border border-[#03E1FF]/20 flex items-center justify-center mb-6 group-hover:scale-110 transition-transform">
              <Fingerprint className="w-6 h-6 text-[#03E1FF]" />
            </div>
            <h3 className="text-2xl font-bold mb-3">Health Vault (IPFS)</h3>
            <p className="text-gray-400 leading-relaxed mb-8 text-sm">
              Medical records and property deeds encrypted natively on-device using AES-256
              and pinned to decentralized IPFS. Arcium MPC computes over encrypted data —
              no one, not even us, can read your records.
            </p>
            <div className="flex justify-center">
              <div className="relative">
                <div className="absolute inset-0 rounded-[2rem] bg-[#03E1FF]/15 blur-2xl" />
                <div className="relative rounded-[2rem] overflow-hidden border border-white/10 max-w-[260px] bg-white/[0.02] floating" style={{ animationDelay: '2s' }}>
                  <img src="/health.png" alt="SolanaSuper Health Vault" className="w-full h-auto object-cover" />
                </div>
              </div>
            </div>
          </div>

          {/* Governance */}
          <div className="group rounded-3xl border border-white/5 bg-white/[0.02] p-8 hover:bg-white/[0.04] hover:border-[#9945FF]/20 transition-all duration-300">
            <div className="w-12 h-12 rounded-2xl bg-[#9945FF]/10 border border-[#9945FF]/20 flex items-center justify-center mb-6 group-hover:scale-110 transition-transform">
              <Shield className="w-6 h-6 text-[#9945FF]" />
            </div>
            <h3 className="text-2xl font-bold mb-3">Immutable Governance</h3>
            <p className="text-gray-400 leading-relaxed mb-8 text-sm">
              Submit transparent community referendums with anonymous ZK-proof verified votes.
              The network executes democratic consensus using impenetrable smart contracts —
              censorship is mathematically impossible.
            </p>
            <div className="flex justify-center">
              <div className="relative">
                <div className="absolute inset-0 rounded-[2rem] bg-[#9945FF]/15 blur-2xl" />
                <div className="relative rounded-[2rem] overflow-hidden border border-white/10 max-w-[260px] bg-white/[0.02] floating" style={{ animationDelay: '0.5s' }}>
                  <img src="/gov.png" alt="SolanaSuper Governance" className="w-full h-auto object-cover" />
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* === Profile / Income Screenshots === */}
      <section className="py-20 px-6 max-w-7xl mx-auto relative z-10">
        <div className="grid md:grid-cols-2 gap-12 items-center">
          <div>
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full border border-[#14F195]/20 bg-[#14F195]/5 text-xs font-bold text-[#14F195] tracking-widest uppercase mb-6">
              <Lock className="w-3 h-3" /> Self-Sovereign Identity
            </div>
            <h2 className="text-3xl md:text-4xl font-black tracking-tighter mb-4 leading-tight">
              Your keys. Your identity.<br />
              <span className="text-[#14F195]">Your rules.</span>
            </h2>
            <p className="text-base text-gray-400 leading-relaxed mb-6">
              Hardware-backed Ed25519 keypair. BIP-39 mnemonic locked behind biometrics.
              Durable nonces that survive network outages. This is what real self-custody looks like.
            </p>
            <div className="flex flex-wrap gap-2">
              {TECH_PILLS.map((pill) => (
                <span key={pill} className="px-3 py-1 rounded-full bg-white/5 border border-white/10 text-xs text-gray-400 font-medium">
                  {pill}
                </span>
              ))}
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4">
            {[
              { src: '/profile.png', alt: 'Profile & Identity', delay: '0s' },
              { src: '/health.png',  alt: 'Health Vault',       delay: '0.8s' },
            ].map(({ src, alt, delay }) => (
              <div key={src} className="relative floating" style={{ animationDelay: delay }}>
                <div className="absolute inset-0 rounded-3xl bg-[#9945FF]/10 blur-xl" />
                <div className="relative rounded-3xl overflow-hidden border border-white/10 bg-white/[0.02]">
                  <img src={src} alt={alt} className="w-full h-auto object-cover" />
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* === Tech Stack === */}
      <section id="tech" className="py-24 relative z-10 bg-gradient-to-b from-transparent via-black/50 to-transparent border-y border-white/5">
        <div className="max-w-7xl mx-auto px-6 text-center">
          <h2 className="text-3xl md:text-4xl font-black tracking-tighter mb-4">Built on Uncompromising Tech</h2>
          <p className="text-gray-400 max-w-xl mx-auto mb-16 text-sm leading-relaxed">
            Every layer is open-source, auditable, and battle-tested. No proprietary black boxes.
          </p>

          <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4 text-left max-w-4xl mx-auto">
            {[
              { icon: Lock,        color: '#14F195', title: 'Android Keystore + Biometrics', desc: 'Hardware-backed key generation. Biometric gating for every signature.' },
              { icon: Shield,      color: '#9945FF', title: 'ZK Proof Engine (Rust)',         desc: 'Rust-compiled zero-knowledge prover for anonymous governance ballots.' },
              { icon: Database,    color: '#03E1FF', title: 'SQLCipher Encrypted DB',         desc: 'AES-256 encrypted local database — unreadable without your biometric key.' },
              { icon: Wifi,        color: '#FF6B6B', title: 'Google Nearby (BT Mesh)',        desc: 'Sub-second peer discovery and payload transfer without internet.' },
              { icon: Zap,         color: '#FFB84C', title: 'QuickNode RPC + Durable Nonces', desc: 'High-throughput RPC with offline-resilient transactions that never expire.' },
              { icon: Fingerprint, color: '#C850C0', title: 'Arcium MPC + IPFS',              desc: 'Confidential computation over encrypted data. Storage that can\'t be taken down.' },
            ].map(({ icon: Icon, color, title, desc }) => (
              <div
                key={title}
                className="p-5 rounded-2xl border border-white/5 bg-white/[0.02] hover:bg-white/[0.04] hover:border-white/10 transition-all group"
              >
                <div className="flex items-center gap-3 mb-3">
                  <div className="w-8 h-8 rounded-xl flex items-center justify-center shrink-0 group-hover:scale-110 transition-transform"
                    style={{ background: `${color}15` }}>
                    <Icon className="w-4 h-4" style={{ color }} />
                  </div>
                  <h3 className="text-sm font-bold text-white">{title}</h3>
                </div>
                <p className="text-xs text-gray-500 leading-relaxed">{desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* === Final CTA === */}
      <section className="py-32 px-6 max-w-4xl mx-auto relative z-10 text-center">
        <div className="relative rounded-3xl border border-white/10 bg-gradient-to-br from-[#14F195]/5 via-transparent to-[#9945FF]/5 p-12 overflow-hidden">
          {/* BG glow */}
          <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-96 h-96 bg-[#14F195]/10 rounded-full blur-3xl pointer-events-none" />
          <div className="relative">
            <h2 className="text-4xl md:text-5xl font-black tracking-tighter mb-4">
              Ready to become<br />
              <span className="bg-gradient-to-r from-[#14F195] to-[#9945FF] bg-clip-text text-transparent">
                sovereign?
              </span>
            </h2>
            <p className="text-gray-400 mb-10 max-w-md mx-auto leading-relaxed">
              Download the APK, load your wallet, and experience what financial freedom
              actually feels like — no middlemen, no servers, no compromise.
            </p>
            <div className="flex flex-col sm:flex-row justify-center gap-4">
              <a
                href={APK_URL}
                target="_blank"
                rel="noopener noreferrer"
                className="group flex items-center justify-center gap-2.5 px-8 py-4 rounded-2xl bg-white text-black font-bold text-base hover:bg-[#14F195] transition-all duration-200 hover:scale-105 active:scale-95 shadow-[0_0_40px_rgba(20,241,149,0.4)]"
              >
                <Download className="w-5 h-5 group-hover:animate-bounce" />
                Download APK
              </a>
              <a
                href={GITHUB_URL}
                target="_blank"
                rel="noopener noreferrer"
                className="flex items-center justify-center gap-2.5 px-8 py-4 rounded-2xl border border-white/10 bg-white/5 text-white font-bold text-base hover:bg-white/10 transition-all duration-200 hover:scale-105"
              >
                <Github className="w-5 h-5" />
                Read the Source
              </a>
            </div>
          </div>
        </div>
      </section>

      {/* === Footer === */}
      <footer className="border-t border-white/5 bg-black/60 backdrop-blur-md relative z-10 py-14">
        <div className="max-w-7xl mx-auto px-6">
          <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-8 mb-10">
            <div>
              <div className="flex items-center gap-2.5 mb-3">
                <div className="w-8 h-8 rounded-xl bg-gradient-to-br from-[#14F195] to-[#9945FF] p-[1.5px]">
                  <div className="w-full h-full rounded-[10px] bg-black flex items-center justify-center">
                    <img src="/logo.svg" alt="Logo" className="w-4 h-4 object-contain" />
                  </div>
                </div>
                <span className="font-bold text-white tracking-tight">Solana<span className="text-[#14F195]">Super</span></span>
              </div>
              <p className="text-xs text-gray-600 max-w-xs leading-relaxed">
                Powered by Native Decentralized Networks & SovereignLife OS. Zero Compromises.
              </p>
            </div>

            <div className="flex flex-col sm:flex-row gap-8 text-sm">
              <div className="flex flex-col gap-3">
                <span className="text-xs text-gray-600 font-semibold tracking-widest uppercase">App</span>
                <a href={APK_URL} target="_blank" rel="noopener noreferrer" className="text-gray-400 hover:text-[#14F195] transition-colors">Download APK</a>
                <a href={GITHUB_URL} target="_blank" rel="noopener noreferrer" className="text-gray-400 hover:text-white transition-colors">GitHub Source</a>
              </div>
              <div className="flex flex-col gap-3">
                <span className="text-xs text-gray-600 font-semibold tracking-widest uppercase">Ecosystem</span>
                {PARTNERS.slice(0, 3).map((p) => (
                  <a key={p.name} href={p.href} target="_blank" rel="noopener noreferrer" className="text-gray-400 hover:text-white transition-colors">{p.name}</a>
                ))}
              </div>
              <div className="flex flex-col gap-3">
                <span className="text-xs text-gray-600 font-semibold tracking-widest uppercase"> </span>
                {PARTNERS.slice(3).map((p) => (
                  <a key={p.name} href={p.href} target="_blank" rel="noopener noreferrer" className="text-gray-400 hover:text-white transition-colors">{p.name}</a>
                ))}
              </div>
            </div>
          </div>

          <div className="flex flex-col sm:flex-row justify-between items-center gap-4 pt-8 border-t border-white/5">
            <p className="text-xs text-gray-700">© 2025 SolanaSuper. All rights reserved.</p>
            <a
              href="https://kyokasuigetsu.xyz"
              target="_blank"
              rel="noopener noreferrer"
              className="text-xs font-semibold tracking-widest text-[#14F195]/60 hover:text-[#14F195] transition-colors uppercase"
            >
              Built by Kyoka Suigetsu
            </a>
          </div>
        </div>
      </footer>
    </main>
  );
}
