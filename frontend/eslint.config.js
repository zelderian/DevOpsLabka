import js from '@eslint/js';
import globals from 'globals';

export default [
  { ignores: ['dist/', 'node_modules/'] },
  js.configs.recommended,
  {
    files: ['src/**/*.js'],
    languageOptions: { globals: globals.browser },
  },
  {
    files: ['test/**/*.js', 'scripts/**/*.mjs', 'eslint.config.js'],
    languageOptions: { globals: globals.node },
  },
  {
    rules: {
      'no-unused-vars': 'error',
      'no-undef': 'error',
      eqeqeq: 'error',
      curly: 'error',
      'no-var': 'error',
      'prefer-const': 'error',
    },
  },
];
