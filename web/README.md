# FieldMinds Landing Page

A modern, responsive landing page for FieldMinds built with Next.js 14, React 18, and Tailwind CSS.

## Features

- Fast, optimized Next.js application
- Material Design 3 color system matching the FieldMinds mobile app
- Responsive design that works on all devices
- GitHub integration
- Download links to mobile app
- Smooth animations and transitions

## Getting Started

### Prerequisites

- Node.js 18+ and pnpm

### Installation

```bash
cd web
pnpm install
pnpm run dev
```

Visit `http://localhost:3000` to see the landing page.

### Build for Production

```bash
pnpm run build
pnpm start
```

## Deployment

This project is configured to deploy on Vercel. Simply connect your GitHub repository to Vercel and it will automatically deploy the `web` directory.

### Environment Variables

No environment variables are required for this landing page.

## Project Structure

```
web/
├── app/
│   ├── layout.tsx      # Root layout with metadata
│   ├── page.tsx        # Main landing page
│   └── globals.css     # Global styles and Tailwind imports
├── tailwind.config.ts  # Tailwind configuration with FieldMinds colors
├── next.config.mjs     # Next.js configuration
├── postcss.config.mjs  # PostCSS configuration
├── package.json        # Dependencies and scripts
└── tsconfig.json       # TypeScript configuration
```

## Design System

The landing page uses the FieldMinds Material Design 3 color palette:

- **Primary**: #6750A4 (Purple)
- **Secondary**: #625B71 (Neutral Purple)
- **Tertiary**: #7D5260 (Rosy Pink)
- **Background**: #FAF5FF (Soft Purple)

## License

This project is part of FieldMinds and follows the same license.
