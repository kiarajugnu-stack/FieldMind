import type { Config } from "tailwindcss";

const config: Config = {
  content: [
    "./app/**/*.{js,ts,jsx,tsx,mdx}",
    "./components/**/*.{js,ts,jsx,tsx,mdx}",
  ],
  theme: {
    extend: {
      colors: {
        primary: "#6750A4",
        "primary-light": "#D0BCFF",
        "on-primary": "#FFFFFF",
        "primary-container": "#EADDFF",
        "on-primary-container": "#21005D",
        secondary: "#625B71",
        "secondary-light": "#CCC2DC",
        "on-secondary": "#FFFFFF",
        "secondary-container": "#E8DEF8",
        "on-secondary-container": "#1D192B",
        tertiary: "#7D5260",
        "tertiary-light": "#EFB8C8",
        "on-tertiary": "#FFFFFF",
        "tertiary-container": "#FFD8E4",
        "on-tertiary-container": "#31111D",
        background: "#FAF5FF",
        "on-background": "#1C1B1F",
        surface: "#FAF5FF",
        "on-surface": "#1C1B1F",
        "surface-variant": "#E4DCF0",
        "on-surface-variant": "#49454F",
        outline: "#79747E",
        "outline-variant": "#CAC4D0",
      },
    },
  },
  plugins: [],
};

export default config;
