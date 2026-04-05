/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ["./app/**/*.{js,jsx,ts,tsx}", "./components/**/*.{js,jsx,ts,tsx}"],
  theme: {
    extend: {
      colors: {
        primary: "#2563EB",
        dark: "#111827",
        grayLight: "#F3F4F6",
        success: "#22C55E"
      }
    },
  },
  plugins: [],
}
