/**
 * Jest config for E2E suites (ESM).
 * Run with: node --experimental-vm-modules server/node_modules/jest/bin/jest.js --config jest.e2e.config.mjs
 */

export default {
  rootDir: '.',
  testEnvironment: 'node',
  testMatch: ['<rootDir>/tests/e2e/**/*.test.js'],
  transform: {},
  moduleFileExtensions: ['js', 'json'],
  testPathIgnorePatterns: ['/node_modules/', '/server/node_modules/'],
};
