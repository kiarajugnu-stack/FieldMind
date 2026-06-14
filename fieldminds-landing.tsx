'use client';

import { Github, Download, ArrowRight, Zap, Shield, Users } from 'lucide-react';
import { useState } from 'react';

export default function FieldMindsLanding() {
  const [isHovering, setIsHovering] = useState(false);

  const stats = [
    { label: 'Active Users', value: '10K+' },
    { label: 'Research Sessions', value: '50K+' },
    { label: 'Observations', value: '500K+' },
  ];

  const features = [
    {
      icon: Zap,
      title: 'Lightning Fast',
      description: 'Capture and analyze your observations in real-time with our optimized mobile app.',
    },
    {
      icon: Shield,
      title: 'Secure & Private',
      description: 'Your research data is encrypted and protected with enterprise-grade security.',
    },
    {
      icon: Users,
      title: 'Collaborate',
      description: 'Share findings and collaborate with your research team seamlessly.',
    },
  ];

  return (
    <div className="min-h-screen overflow-hidden">
      {/* Background Elements */}
      <div className="fixed inset-0 -z-10 overflow-hidden pointer-events-none">
        <div
          className="absolute top-0 left-1/4 w-96 h-96 rounded-full"
          style={{
            background: 'radial-gradient(circle, rgba(103, 80, 164, 0.1) 0%, transparent 70%)',
            filter: 'blur(40px)',
          }}
        />
        <div
          className="absolute bottom-0 right-1/4 w-96 h-96 rounded-full"
          style={{
            background: 'radial-gradient(circle, rgba(125, 82, 96, 0.1) 0%, transparent 70%)',
            filter: 'blur(40px)',
          }}
        />
      </div>

      {/* Header */}
      <header className="relative z-10 border-b border-outline-variant/20 backdrop-blur-md bg-background/80">
        <div className="max-w-6xl mx-auto px-6 py-4 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div
              className="w-10 h-10 rounded-lg"
              style={{ backgroundColor: 'var(--primary)' }}
            />
            <span className="text-xl font-semibold" style={{ color: 'var(--on-background)' }}>
              FieldMinds
            </span>
          </div>
          <nav className="hidden md:flex gap-8" style={{ color: 'var(--on-surface-variant)' }}>
            <a href="#features" className="hover:opacity-70 transition-opacity">
              Features
            </a>
            <a href="#stats" className="hover:opacity-70 transition-opacity">
              Impact
            </a>
            <a href="#download" className="hover:opacity-70 transition-opacity">
              Download
            </a>
          </nav>
        </div>
      </header>

      {/* Hero Section */}
      <section className="relative z-10 max-w-6xl mx-auto px-6 py-20 md:py-32">
        <div className="text-center mb-12">
          <h1
            className="text-4xl md:text-6xl font-bold mb-6 leading-tight"
            style={{ color: 'var(--on-background)' }}
          >
            Research Observation
            <br />
            <span style={{ color: 'var(--primary)' }}>Simplified</span>
          </h1>
          <p
            className="text-lg md:text-xl max-w-2xl mx-auto mb-8 leading-relaxed"
            style={{ color: 'var(--on-surface-variant)' }}
          >
            Capture, organize, and analyze field observations with the power of AI-assisted research.
            FieldMinds helps researchers work smarter, not harder.
          </p>

          {/* CTA Buttons */}
          <div className="flex flex-col sm:flex-row gap-4 justify-center mb-12">
            <button
              id="download"
              className="px-8 py-4 rounded-xl font-semibold flex items-center justify-center gap-2 transition-all duration-300 text-lg"
              style={{
                backgroundColor: 'var(--primary)',
                color: 'var(--on-primary)',
              }}
              onMouseEnter={() => setIsHovering(true)}
              onMouseLeave={() => setIsHovering(false)}
            >
              <Download size={20} />
              Download App
            </button>
            <button
              className="px-8 py-4 rounded-xl font-semibold flex items-center justify-center gap-2 border-2 transition-all duration-300 text-lg"
              style={{
                borderColor: 'var(--outline)',
                color: 'var(--on-background)',
                backgroundColor: isHovering ? 'var(--surface-variant)' : 'transparent',
              }}
            >
              <Github size={20} />
              View on GitHub
            </button>
          </div>

          {/* GitHub Options */}
          <div
            className="inline-flex items-center gap-2 px-4 py-2 rounded-full"
            style={{ backgroundColor: 'var(--primary-container)' }}
          >
            <Github size={16} style={{ color: 'var(--on-primary-container)' }} />
            <span style={{ color: 'var(--on-primary-container)' }} className="text-sm font-medium">
              Open source on GitHub
            </span>
          </div>
        </div>

        {/* Mock Phone/Preview */}
        <div className="flex justify-center mb-12">
          <div
            className="w-full max-w-sm aspect-video rounded-3xl border-8 flex items-center justify-center relative overflow-hidden"
            style={{
              borderColor: 'var(--outline-variant)',
              backgroundColor: 'var(--surface-variant)',
            }}
          >
            <div className="text-center" style={{ color: 'var(--on-surface-variant)' }}>
              <Zap size={48} className="mx-auto mb-4 opacity-50" />
              <p className="text-sm">FieldMinds App Preview</p>
            </div>
          </div>
        </div>
      </section>

      {/* Stats Section */}
      <section id="stats" className="relative z-10 max-w-6xl mx-auto px-6 py-16">
        <div className="grid grid-cols-3 gap-8 md:gap-12">
          {stats.map((stat, idx) => (
            <div
              key={idx}
              className="text-center p-6 rounded-2xl border border-outline-variant/30 transition-all hover:border-outline-variant/60"
              style={{
                backgroundColor: 'rgba(255, 255, 255, 0.02)',
              }}
            >
              <div
                className="text-3xl md:text-4xl font-bold mb-2"
                style={{ color: 'var(--primary)' }}
              >
                {stat.value}
              </div>
              <div style={{ color: 'var(--on-surface-variant)' }} className="text-sm md:text-base">
                {stat.label}
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* Features Section */}
      <section id="features" className="relative z-10 max-w-6xl mx-auto px-6 py-24">
        <h2
          className="text-3xl md:text-4xl font-bold text-center mb-4"
          style={{ color: 'var(--on-background)' }}
        >
          Powerful Features
        </h2>
        <p
          className="text-center mb-16 text-lg"
          style={{ color: 'var(--on-surface-variant)' }}
        >
          Everything you need to manage your research observations
        </p>

        <div className="grid md:grid-cols-3 gap-8">
          {features.map((feature, idx) => {
            const Icon = feature.icon;
            return (
              <div
                key={idx}
                className="p-8 rounded-2xl border border-outline-variant/30 hover:border-outline-variant/60 transition-all hover:shadow-lg"
                style={{
                  backgroundColor: 'rgba(255, 255, 255, 0.02)',
                }}
              >
                <Icon
                  size={32}
                  className="mb-4"
                  style={{ color: 'var(--tertiary)' }}
                />
                <h3
                  className="text-xl font-semibold mb-2"
                  style={{ color: 'var(--on-background)' }}
                >
                  {feature.title}
                </h3>
                <p style={{ color: 'var(--on-surface-variant)' }}>
                  {feature.description}
                </p>
              </div>
            );
          })}
        </div>
      </section>

      {/* CTA Section */}
      <section className="relative z-10 max-w-6xl mx-auto px-6 py-24">
        <div
          className="rounded-3xl p-12 md:p-20 text-center border border-outline-variant/50"
          style={{
            background: `linear-gradient(135deg, var(--primary-container) 0%, var(--tertiary-container) 100%)`,
          }}
        >
          <h2
            className="text-3xl md:text-4xl font-bold mb-6"
            style={{ color: 'var(--on-primary-container)' }}
          >
            Ready to streamline your research?
          </h2>
          <p
            className="text-lg mb-8 max-w-2xl mx-auto"
            style={{ color: 'var(--on-primary-container)' }}
          >
            Join thousands of researchers using FieldMinds to capture and analyze observations.
          </p>
          <button
            className="px-10 py-4 rounded-xl font-semibold inline-flex items-center gap-2 transition-all duration-300"
            style={{
              backgroundColor: 'var(--on-primary-container)',
              color: 'var(--primary)',
            }}
          >
            Download Now
            <ArrowRight size={20} />
          </button>
        </div>
      </section>

      {/* Footer */}
      <footer
        className="relative z-10 border-t border-outline-variant/20 mt-24 py-12"
        style={{ color: 'var(--on-surface-variant)' }}
      >
        <div className="max-w-6xl mx-auto px-6">
          <div className="grid md:grid-cols-4 gap-8 mb-8">
            <div>
              <div className="font-semibold mb-4" style={{ color: 'var(--on-background)' }}>
                FieldMinds
              </div>
              <p className="text-sm">Simplifying field research for modern scientists.</p>
            </div>
            <div>
              <div className="font-semibold mb-4" style={{ color: 'var(--on-background)' }}>
                Product
              </div>
              <ul className="space-y-2 text-sm">
                <li><a href="#features" className="hover:opacity-70">Features</a></li>
                <li><a href="#" className="hover:opacity-70">Pricing</a></li>
                <li><a href="#" className="hover:opacity-70">Security</a></li>
              </ul>
            </div>
            <div>
              <div className="font-semibold mb-4" style={{ color: 'var(--on-background)' }}>
                Community
              </div>
              <ul className="space-y-2 text-sm">
                <li><a href="#" className="hover:opacity-70">GitHub</a></li>
                <li><a href="#" className="hover:opacity-70">Discord</a></li>
                <li><a href="#" className="hover:opacity-70">Docs</a></li>
              </ul>
            </div>
            <div>
              <div className="font-semibold mb-4" style={{ color: 'var(--on-background)' }}>
                Legal
              </div>
              <ul className="space-y-2 text-sm">
                <li><a href="#" className="hover:opacity-70">Privacy</a></li>
                <li><a href="#" className="hover:opacity-70">Terms</a></li>
                <li><a href="#" className="hover:opacity-70">Contact</a></li>
              </ul>
            </div>
          </div>
          <div
            className="border-t border-outline-variant/20 pt-8 text-center text-sm"
            style={{ color: 'var(--on-surface-variant)' }}
          >
            © 2024 FieldMinds. Open source on GitHub.
          </div>
        </div>
      </footer>
    </div>
  );
}
