import { cp, mkdir, readFile, rm, writeFile } from 'node:fs/promises';

const version = process.env.APP_VERSION ?? process.env.GITHUB_SHA?.slice(0, 7) ?? 'local';

await rm('dist', { recursive: true, force: true });
await mkdir('dist');
await cp('src', 'dist', { recursive: true });

const html = await readFile('dist/index.html', 'utf8');
await writeFile('dist/index.html', html.replace('</main>', `  <footer class="hint">version ${version}</footer>\n  </main>`));

console.log(`Built frontend into dist/ (version ${version})`);
