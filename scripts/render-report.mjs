import fs from 'node:fs/promises';
import path from 'node:path';
import os from 'node:os';
import { fileURLToPath } from 'node:url';
import { chromium } from '../frontend/node_modules/playwright/index.mjs';

// Export the controlled Markdown report and its real screenshots without extra dependencies.
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const report = path.join(root, 'docs/reports/bt2-implementation-report.md');
const output = path.join(root, 'docs/reports/BT2_L8_implementation_2026-11-12.pdf');
const escape = value => value.replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;').replaceAll('"', '&quot;');
function inline(text) {
  return escape(text).replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
    .replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2">$1</a>');
}
const source = (await fs.readFile(report, 'utf8')).replace(/^\uFEFF/, '');
async function section(markdown, index) {
  const lines = markdown.trim().split(/\r?\n/);
  let html = '';
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i].trim();
    if (!line) continue;
    if (line.startsWith('```')) {
      const code = [];
      while (++i < lines.length && !lines[i].startsWith('```')) code.push(lines[i]);
      html += `<pre>${escape(code.join('\n'))}</pre>`;
    } else if (line.startsWith('|')) {
      const rows = [];
      while (i < lines.length && lines[i].trim().startsWith('|')) {
        const cells = lines[i].trim().slice(1, -1).split('|').map(cell => cell.trim());
        if (!cells.every(cell => /^:?-+:?$/.test(cell))) rows.push(cells);
        i++;
      }
      i--;
      html += `<table><thead><tr>${rows[0].map(cell => `<th>${inline(cell)}</th>`).join('')}</tr></thead><tbody>${rows.slice(1).map(row => `<tr>${row.map(cell => `<td>${inline(cell)}</td>`).join('')}</tr>`).join('')}</tbody></table>`;
    } else if (/^!\[/.test(line)) {
      const [, caption, relative] = line.match(/^!\[([^\]]+)\]\(([^)]+)\)$/);
      const file = path.resolve(path.dirname(report), relative);
      const png = await fs.readFile(file);
      html += `<figure><img alt="${escape(caption)}" src="data:image/png;base64,${png.toString('base64')}"><figcaption>${inline(caption)}</figcaption></figure>`;
    } else if (/^#{1,3} /.test(line)) {
      const [, marks, text] = line.match(/^(#{1,3}) (.*)$/);
      html += `<h${marks.length}>${inline(text)}</h${marks.length}>`;
    } else {
      const paragraph = [line];
      while (i + 1 < lines.length && lines[i + 1].trim() && !/^[#|!`]/.test(lines[i + 1].trim())) paragraph.push(lines[++i].trim());
      html += `<p>${inline(paragraph.join(' '))}</p>`;
    }
  }
  return `<section class="page page-${index + 1}">${html}</section>`;
}
const pages = await Promise.all(source.split('<!-- page -->').map(section));
const html = `<!doctype html><html lang="vi"><meta charset="utf-8"><title>BT2 Smart CRM L8</title><style>
@page{size:A4;margin:14mm 14mm 16mm}*{box-sizing:border-box}body{font:10pt/1.4 Arial,sans-serif;color:#203347;margin:0}.page{break-after:page}.page:last-child{break-after:auto}h1{font-size:28pt;letter-spacing:.02em;margin:14mm 0 8mm;color:#172e40}h2{font-size:16pt;margin:0 0 5mm;color:#172e40}h3{font-size:11pt;margin:5mm 0 3mm}p{margin:3mm 0}table{border-collapse:collapse;width:100%;font-size:8.2pt;line-height:1.3;margin:4mm 0;table-layout:auto}th,td{border:1px solid #c9d3dc;padding:2.1mm 2mm;vertical-align:top;overflow-wrap:anywhere}th{background:#eaf0f5;text-align:left;color:#172e40}tr{break-inside:avoid}code{font:8.5pt Consolas,monospace;overflow-wrap:anywhere}pre{white-space:pre-wrap;font:9pt/1.4 Consolas,monospace;background:#f0f4f7;padding:4mm;border-left:1mm solid #2c5877}a{color:#255877;text-decoration:none}figure{margin:3mm 0;break-inside:avoid;text-align:center}img{max-width:100%;object-fit:contain}figcaption{font-size:8pt;color:#50697b;margin:1mm 0 2mm}.page-1 table{font-size:10pt}.page-1 td{padding:3mm}.page-2 table{font-size:7.9pt}.page-5 img{max-height:192mm}.page-6 img{max-height:64mm}.page-7 img{max-height:60mm}.page-8 table{font-size:8pt}.page-8 p{font-size:9.5pt}
</style><body>${pages.join('')}</body></html>`;
const browser = await chromium.launch({ headless: true });
try {
  const page = await browser.newPage({ viewport: { width: 688, height: 1009 } });
  await page.setContent(html, { waitUntil: 'load' });
  await page.emulateMedia({ media: 'print' });
  const heights = await page.locator('.page').evaluateAll(elements => elements.map(el => Math.ceil(el.getBoundingClientRect().height)));
  if (process.argv.includes('--preview')) {
    const previews = await fs.mkdtemp(path.join(os.tmpdir(), 'smartcrm-report-preview-'));
    for (const index of [0, 1, 5, 6, 7]) {
      await page.locator('.page').nth(index).screenshot({ path: path.join(previews, `page-${index + 1}.png`) });
    }
    console.log(JSON.stringify({ previews }));
  }
  await page.pdf({ path: output, format: 'A4', printBackground: true, displayHeaderFooter: true,
    headerTemplate: '<div style="font-size:8px;color:#678;width:100%;margin:0 14mm">SMART CRM · MEKONG MOBILE · L8 · BÀI TẬP 2</div>',
    footerTemplate: '<div style="font-size:8px;color:#678;width:100%;margin:0 14mm;display:flex;justify-content:space-between"><span>Võ Nguyễn Thành Nhân · 2374802010360 · 12/11/2026</span><span>Trang <span class="pageNumber"></span>/<span class="totalPages"></span></span></div>',
    margin: { top: '14mm', right: '14mm', bottom: '16mm', left: '14mm' } });
  console.log(JSON.stringify({ output: path.relative(root, output), sections: pages.length, sectionHeightsPx: heights }));
} finally { await browser.close(); }
