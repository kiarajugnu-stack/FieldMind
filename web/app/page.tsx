'use client';

import { Github, Download, Zap, Lock, Users, ArrowRight, ExternalLink } from 'lucide-react';

export default function Home() {
  return (
    <main className="min-h-screen flex flex-col">
      {/* Navigation */}
      <nav className="border-b border-outline-variant/20 backdrop-blur-sm sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-6 py-4 flex items-center justify-between">
          <div className="text-2xl font-bold gradient-text">FieldMinds</div>
          <a
            href="https://github.com/firefly-sylestia/FieldMinds"
            target="_blank"
            rel="noopener noreferrer"
            className="flex items-center gap-2 text-on-surface-variant hover:text-primary transition-colors"
          >
            <Github size={20} />
            GitHub
          </a>
        </div>
      </nav>

      {/* Hero Section */}
      <section className="flex-1 flex items-center justify-center px-6 py-20 max-w-7xl mx-auto w-full">
        <div className="text-center space-y-8 max-w-3xl">
          <div>
            <div className="inline-flex items-center gap-2 bg-primary-container text-on-primary-container px-4 py-2 rounded-full text-sm font-semibold mb-6">
              <span className="relative flex h-2 w-2">
                <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-primary opacity-75"></span>
                <span className="relative inline-flex rounded-full h-2 w-2 bg-primary"></span>
              </span>
              Open Source on GitHub
            </div>
            <h1 className="text-5xl md:text-6xl font-bold tracking-tight mb-6">
              Track Observations,
              <br />
              <span className="gradient-text">Discover Insights</span>
            </h1>
            <p className="text-xl text-on-surface-variant max-w-2xl mx-auto leading-relaxed">
              FieldMinds helps researchers and field workers document, organize, and analyze observations with powerful tools designed for modern research workflows.
            </p>
          </div>

          {/* CTA Buttons */}
          <div className="flex flex-col sm:flex-row gap-4 justify-center pt-4">
            <a
              href="https://play.google.com/store/apps/details?id=chromahub.rhythm"
              target="_blank"
              rel="noopener noreferrer"
              className="btn-primary flex items-center justify-center gap-2"
            >
              <Download size={20} />
              Download App
            </a>
            <a
              href="https://github.com/firefly-sylestia/FieldMinds"
              target="_blank"
              rel="noopener noreferrer"
              className="btn-outline flex items-center justify-center gap-2"
            >
              <Github size={20} />
              View on GitHub
            </a>
          </div>
        </div>
      </section>

      {/* Stats Section */}
      <section className="bg-primary-container/10 border-y border-outline-variant/20 py-16 px-6">
        <div className="max-w-7xl mx-auto grid grid-cols-1 md:grid-cols-3 gap-8">
          <div className="text-center">
            <div className="text-4xl font-bold text-primary mb-2">10K+</div>
            <p className="text-on-surface-variant">Active Users</p>
          </div>
          <div className="text-center">
            <div className="text-4xl font-bold text-primary mb-2">50K+</div>
            <p className="text-on-surface-variant">Research Sessions</p>
          </div>
          <div className="text-center">
            <div className="text-4xl font-bold text-primary mb-2">500K+</div>
            <p className="text-on-surface-variant">Observations Tracked</p>
          </div>
        </div>
      </section>

      {/* Features Section */}
      <section className="py-20 px-6 max-w-7xl mx-auto w-full">
        <div className="text-center mb-16">
          <h2 className="text-4xl font-bold mb-4">Powerful Features</h2>
          <p className="text-on-surface-variant text-lg max-w-2xl mx-auto">
            Everything you need to capture, organize, and analyze field observations
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {/* Lightning Fast */}
          <div className="card">
            <Zap className="text-primary mb-4" size={32} />
            <h3 className="text-xl font-bold mb-2">Lightning Fast</h3>
            <p className="text-on-surface-variant">
              Instant syncing and responsive interface designed for seamless offline-first experience
            </p>
          </div>

          {/* Secure & Private */}
          <div className="card">
            <Lock className="text-tertiary mb-4" size={32} />
            <h3 className="text-xl font-bold mb-2">Secure & Private</h3>
            <p className="text-on-surface-variant">
              Your data stays yours. End-to-end encryption and local-first architecture
            </p>
          </div>

          {/* Collaborate */}
          <div className="card">
            <Users className="text-secondary mb-4" size={32} />
            <h3 className="text-xl font-bold mb-2">Collaborate</h3>
            <p className="text-on-surface-variant">
              Share sessions, notes, and observations with your research team in real-time
            </p>
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="py-20 px-6 bg-gradient-to-r from-primary/10 to-tertiary/10 border-y border-outline-variant/20">
        <div className="max-w-4xl mx-auto text-center space-y-6">
          <h2 className="text-4xl font-bold">Ready to Start Observing?</h2>
          <p className="text-on-surface-variant text-lg">
            Download FieldMinds today and transform how you conduct field research
          </p>
          <div className="flex flex-col sm:flex-row gap-4 justify-center pt-4">
            <a
              href="https://play.google.com/store/apps/details?id=chromahub.rhythm"
              target="_blank"
              rel="noopener noreferrer"
              className="btn-primary flex items-center justify-center gap-2"
            >
              <Download size={20} />
              Get FieldMinds
            </a>
            <a
              href="https://github.com/firefly-sylestia/FieldMinds"
              target="_blank"
              rel="noopener noreferrer"
              className="btn-secondary flex items-center justify-center gap-2"
            >
              <Github size={20} />
              Contribute on GitHub
            </a>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="border-t border-outline-variant/20 bg-surface py-16 px-6">
        <div className="max-w-7xl mx-auto">
          <div className="grid grid-cols-1 md:grid-cols-4 gap-8 mb-12">
            {/* Brand */}
            <div>
              <div className="text-xl font-bold gradient-text mb-4">FieldMinds</div>
              <p className="text-on-surface-variant text-sm">
                Structured observation platform for modern researchers
              </p>
            </div>

            {/* Product */}
            <div>
              <h4 className="font-semibold mb-4">Product</h4>
              <ul className="space-y-3 text-sm text-on-surface-variant">
                <li>
                  <a href="#" className="hover:text-primary transition-colors">Features</a>
                </li>
                <li>
                  <a href="#" className="hover:text-primary transition-colors">Pricing</a>
                </li>
                <li>
                  <a href="#" className="hover:text-primary transition-colors">Documentation</a>
                </li>
              </ul>
            </div>

            {/* Community */}
            <div>
              <h4 className="font-semibold mb-4">Community</h4>
              <ul className="space-y-3 text-sm text-on-surface-variant">
                <li>
                  <a href="https://github.com/firefly-sylestia/FieldMinds" target="_blank" rel="noopener noreferrer" className="hover:text-primary transition-colors flex items-center gap-2">
                    GitHub <ExternalLink size={14} />
                  </a>
                </li>
                <li>
                  <a href="#" className="hover:text-primary transition-colors">Issues</a>
                </li>
                <li>
                  <a href="#" className="hover:text-primary transition-colors">Discussions</a>
                </li>
              </ul>
            </div>

            {/* Legal */}
            <div>
              <h4 className="font-semibold mb-4">Legal</h4>
              <ul className="space-y-3 text-sm text-on-surface-variant">
                <li>
                  <a href="#" className="hover:text-primary transition-colors">Privacy</a>
                </li>
                <li>
                  <a href="#" className="hover:text-primary transition-colors">Terms</a>
                </li>
                <li>
                  <a href="#" className="hover:text-primary transition-colors">License</a>
                </li>
              </ul>
            </div>
          </div>

          {/* Bottom */}
          <div className="border-t border-outline-variant/20 pt-8 flex flex-col sm:flex-row justify-between items-center gap-4 text-sm text-on-surface-variant">
            <p>&copy; 2024 FieldMinds. All rights reserved.</p>
            <p>Built with passion for researchers and field workers</p>
          </div>
        </div>
      </footer>
    </main>
  );
}
