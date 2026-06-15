# Web Landing Page — AGENTS.md

## Purpose

Marketing and landing website for FieldMinds, built with Next.js and deployed on Vercel. Provides product information, feature highlights, and download/support links.

## Ownership

- `web/app/` — Next.js App Router pages (`page.tsx`, `layout.tsx`, `globals.css`)
- `web/assets/` — Static images (screenshots, hero, icons, update screens)
- Root-level static pages: `index.html`, `help.html`, `privacy.html`, `updates.html`, `style.css`, `script.js`
- `web/package.json` — Dependencies and build scripts
- `web/tsconfig.json` — TypeScript configuration
- `web/tailwind.config.ts` — Tailwind CSS configuration
- `web/next.config.mjs` — Next.js configuration
- `vercel.json` — Vercel deployment configuration

## Local Contracts

### Stack
- **Framework**: Next.js 14 (App Router)
- **Language**: TypeScript
- **Styling**: Tailwind CSS
- **Icons**: lucide-react
- **Package manager**: pnpm (workspace at root)

### Build & Deploy
- `pnpm --filter fieldminds-landing build` — production build
- `pnpm --filter fieldminds-landing dev` — development server
- `vercel.json` at repo root controls Vercel deployment settings
- Static HTML pages (`index.html`, `help.html`, etc.) are separate from Next.js app

### Conventions
- React functional components with TypeScript
- Tailwind utility classes for styling
- CSS modules or global CSS in `globals.css`
- Static assets in `assets/` directory

## Work Guidance

- Run `pnpm install` from workspace root to install dependencies
- Verify `vercel.json` configuration matches the deployment target
- Test static HTML pages independently (they don't use Next.js)

## Verification

- `pnpm --filter fieldminds-landing lint` — ESLint check
- `pnpm --filter fieldminds-landing build` — Production build check
- TypeScript compilation via `tsc --noEmit` in CI

## Child DOX Index

No child AGENTS.md files defined yet.
