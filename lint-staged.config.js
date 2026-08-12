const quote = (file) => `"${file.replaceAll('"', '\\"')}"`;

export default {
  "frontend/**/*.{js,jsx,ts,tsx}": (files) => {
    const paths = files.map(quote).join(" ");
    return [
      `frontend/node_modules/.bin/eslint --config frontend/eslint.config.js ${paths}`,
      `frontend/node_modules/.bin/prettier --write ${paths}`,
    ];
  },
  "*.{css,html,json,md,yaml,yml}": (files) =>
    `frontend/node_modules/.bin/prettier --write ${files.map(quote).join(" ")}`,
};
