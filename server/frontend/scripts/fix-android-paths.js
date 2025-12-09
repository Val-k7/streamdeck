#!/usr/bin/env node

/**
 * Fix Android asset paths
 * Converts /assets/ to ./assets/ for WebView compatibility
 */
import fs from 'fs';
import path from 'path';

const indexPath = path.join(process.cwd(), 'dist', 'index.html');

try {
  let html = fs.readFileSync(indexPath, 'utf-8');
  
  // Remplacer les chemins absolus /assets/ par des chemins relatifs ./assets/
  const original = html;
  html = html.replace(/href="\/assets\//g, 'href="./assets/');
  html = html.replace(/src="\/assets\//g, 'src="./assets/');
  
  if (html !== original) {
    fs.writeFileSync(indexPath, html, 'utf-8');
    console.log('✓ Fixed Android asset paths: /assets/ → ./assets/');
  } else {
    console.log('✓ Asset paths already correct (./assets/)');
  }
} catch (err) {
  console.error('❌ Failed to fix asset paths:', err.message);
  process.exit(1);
}
