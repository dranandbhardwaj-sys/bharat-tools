// Bharat Tools Web Engines — 100% client-side PDF, Image, Text, Calculators & Productivity

export function formatBytes(bytes) {
  if (!Number.isFinite(bytes) || bytes <= 0) return '0 B';
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
}

// --- A. PDF ENGINE (Pure JS Image-to-PDF + pdf-lib for Merge/Split/Rotate/Extract/Watermark) ---

function loadImageElementFromFile(file) {
  return new Promise((resolve, reject) => {
    const url = URL.createObjectURL(file);
    const img = new Image();
    img.onload = () => {
      URL.revokeObjectURL(url);
      resolve(img);
    };
    img.onerror = () => {
      URL.revokeObjectURL(url);
      reject(new Error(`Could not decode image: ${file.name}`));
    };
    img.src = url;
  });
}

async function imageFileToJpegBytes(file) {
  const img = await loadImageElementFromFile(file);
  const canvas = document.createElement('canvas');
  const maxDim = 1800;
  let w = img.naturalWidth;
  let h = img.naturalHeight;
  if (w > maxDim || h > maxDim) {
    const scale = maxDim / Math.max(w, h);
    w = Math.round(w * scale);
    h = Math.round(h * scale);
  }
  canvas.width = w;
  canvas.height = h;
  const ctx = canvas.getContext('2d');
  ctx.fillStyle = '#FFFFFF';
  ctx.fillRect(0, 0, w, h);
  ctx.drawImage(img, 0, 0, w, h);
  const blob = await new Promise((res) => canvas.toBlob(res, 'image/jpeg', 0.88));
  const buf = await blob.arrayBuffer();
  return { bytes: new Uint8Array(buf), width: w, height: h };
}

export async function imagesToPdfBlob(files) {
  if (!files || files.length === 0) throw new Error('Select at least one image.');
  const enc = new TextEncoder();
  const chunks = [];
  const offsets = [0]; // index 0 unused; 1-based PDF objects
  let bytePos = 0;

  const pushBytes = (u8) => {
    chunks.push(u8);
    bytePos += u8.length;
  };
  const pushText = (str) => pushBytes(enc.encode(str));

  pushText('%PDF-1.4\n%\xFF\xFF\xFF\xFF\n');

  const pageCount = files.length;
  // Obj 1: Catalog, Obj 2: Pages, then per page: Page (3 + i*3), Content (4 + i*3), Image (5 + i*3)
  const totalObjects = 2 + pageCount * 3;
  const pageObjNums = [];
  for (let i = 0; i < pageCount; i++) pageObjNums.push(3 + i * 3);

  const startObj = (id) => {
    offsets[id] = bytePos;
    pushText(`${id} 0 obj\n`);
  };

  startObj(1);
  pushText('<< /Type /Catalog /Pages 2 0 R >>\nendobj\n');

  startObj(2);
  pushText(`<< /Type /Pages /Kids [${pageObjNums.map((n) => `${n} 0 R`).join(' ')}] /Count ${pageCount} >>\nendobj\n`);

  const a4W = 595.28;
  const a4H = 841.89;
  const margin = 24;

  for (let i = 0; i < pageCount; i++) {
    const { bytes, width, height } = await imageFileToJpegBytes(files[i]);
    const pageId = 3 + i * 3;
    const contentId = pageId + 1;
    const imgId = pageId + 2;

    const scale = Math.min((a4W - margin * 2) / width, (a4H - margin * 2) / height);
    const drawW = (width * scale).toFixed(2);
    const drawH = (height * scale).toFixed(2);
    const drawX = ((a4W - width * scale) / 2).toFixed(2);
    const drawY = ((a4H - height * scale) / 2).toFixed(2);

    const streamStr = `q\n${drawW} 0 0 ${drawH} ${drawX} ${drawY} cm\n/Im0 Do\nQ\n`;

    startObj(pageId);
    pushText(
      `<< /Type /Page /Parent 2 0 R /MediaBox [0 0 ${a4W} ${a4H}] /Resources << /XObject << /Im0 ${imgId} 0 R >> >> /Contents ${contentId} 0 R >>\nendobj\n`
    );

    startObj(contentId);
    pushText(`<< /Length ${streamStr.length} >>\nstream\n${streamStr}endstream\nendobj\n`);

    startObj(imgId);
    pushText(
      `<< /Type /XObject /Subtype /Image /Width ${width} /Height ${height} /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode /Length ${bytes.length} >>\nstream\n`
    );
    pushBytes(bytes);
    pushText('\nendstream\nendobj\n');
  }

  const xrefPos = bytePos;
  pushText(`xref\n0 ${totalObjects + 1}\n0000000000 65535 f \n`);
  for (let id = 1; id <= totalObjects; id++) {
    pushText(`${String(offsets[id]).padStart(10, '0')} 00000 n \n`);
  }
  pushText(`trailer\n<< /Size ${totalObjects + 1} /Root 1 0 R >>\nstartxref\n${xrefPos}\n%%EOF`);

  return new Blob(chunks, { type: 'application/pdf' });
}

export async function inspectPdfFile(file) {
  const buf = await file.arrayBuffer();
  const sample = new TextDecoder('latin1').decode(new Uint8Array(buf.slice(0, Math.min(buf.byteLength, 250000))));
  const versionMatch = sample.match(/%PDF-(\d\.\d)/);
  const pageMatches = sample.match(/\/Type\s*\/Page\b(?!s)/g);
  return {
    name: file.name,
    sizeBytes: file.size,
    pdfVersion: versionMatch ? versionMatch[1] : '1.x',
    estimatedPages: pageMatches ? pageMatches.length : 1
  };
}

function parsePageIndices(spec, maxPages) {
  const out = [];
  const tokens = String(spec || '1').split(',').map((s) => s.trim()).filter(Boolean);
  for (const tok of tokens) {
    if (tok.includes('-')) {
      const [a, b] = tok.split('-').map((n) => parseInt(n, 10));
      if (!Number.isFinite(a) || !Number.isFinite(b)) throw new Error(`Invalid range: ${tok}`);
      const step = a <= b ? 1 : -1;
      for (let p = a; step > 0 ? p <= b : p >= b; p += step) {
        if (p >= 1 && p <= maxPages) out.push(p - 1);
      }
    } else {
      const p = parseInt(tok, 10);
      if (Number.isFinite(p) && p >= 1 && p <= maxPages) out.push(p - 1);
    }
  }
  if (out.length === 0) throw new Error(`No valid page indices within 1..${maxPages}.`);
  return out;
}

export async function runPdfTool(action, files, options = {}) {
  if (action === 'image_to_pdf') {
    const blob = await imagesToPdfBlob(files);
    return [{ name: 'bharat_images.pdf', blob, summary: `Created A4 PDF (${files.length} image page(s), ${formatBytes(blob.size)})` }];
  }
  if (!files || files.length === 0) throw new Error('Select at least one PDF file.');
  if (action === 'pdf_preview' || action === 'pdf_download') {
    const info = await inspectPdfFile(files[0]);
    return [{
      name: files[0].name,
      blob: files[0],
      previewUrl: URL.createObjectURL(files[0]),
      summary: `PDF v${info.pdfVersion} • ~${info.estimatedPages} page(s) • ${formatBytes(info.sizeBytes)}`
    }];
  }

  const PDFLib = window.PDFLib;
  if (!PDFLib) {
    throw new Error('PDF library (pdf-lib) failed to load. Reload the page once and try again. Image-to-PDF and PDF Preview work without it.');
  }

  const { PDFDocument, degrees, rgb, StandardFonts } = PDFLib;

  if (action === 'pdf_merge') {
    if (files.length < 2) throw new Error('Select at least 2 PDF files to merge.');
    const merged = await PDFDocument.create();
    for (const file of files) {
      const bytes = await file.arrayBuffer();
      const doc = await PDFDocument.load(bytes);
      const copied = await merged.copyPages(doc, doc.getPageIndices());
      copied.forEach((p) => merged.addPage(p));
    }
    const out = await merged.save();
    const blob = new Blob([out], { type: 'application/pdf' });
    return [{ name: 'bharat_merged.pdf', blob, summary: `Merged ${files.length} PDFs (${merged.getPageCount()} pages • ${formatBytes(blob.size)})` }];
  }

  const srcBytes = await files[0].arrayBuffer();
  const srcDoc = await PDFDocument.load(srcBytes);
  const total = srcDoc.getPageCount();

  if (action === 'pdf_split') {
    const results = [];
    const limit = Math.min(total, 10);
    for (let i = 0; i < limit; i++) {
      const part = await PDFDocument.create();
      const [page] = await part.copyPages(srcDoc, [i]);
      part.addPage(page);
      const out = await part.save();
      const blob = new Blob([out], { type: 'application/pdf' });
      results.push({ name: `page_${i + 1}.pdf`, blob, summary: `Split ${limit} of ${total} pages` });
    }
    return results;
  }

  if (action === 'pdf_compress') {
    const out = await srcDoc.save({ useObjectStreams: true });
    const blob = new Blob([out], { type: 'application/pdf' });
    return [{
      name: `compressed_${files[0].name}`,
      blob,
      summary: `Original ${formatBytes(files[0].size)} → ${formatBytes(blob.size)}`
    }];
  }

  if (action === 'pdf_rotate') {
    const deg = Number(options.rotateDeg || 90);
    srcDoc.getPages().forEach((p) => p.setRotation(degrees((p.getRotation().angle + deg) % 360)));
    const out = await srcDoc.save();
    const blob = new Blob([out], { type: 'application/pdf' });
    return [{ name: `rotated_${files[0].name}`, blob, summary: `Rotated ${total} page(s) by ${deg}°` }];
  }

  if (action === 'pdf_extract' || action === 'pdf_reorder') {
    const indices = parsePageIndices(options.pageSpec || '1', total);
    const outDoc = await PDFDocument.create();
    const copied = await outDoc.copyPages(srcDoc, indices);
    copied.forEach((p) => outDoc.addPage(p));
    const out = await outDoc.save();
    const blob = new Blob([out], { type: 'application/pdf' });
    return [{
      name: `${action === 'pdf_extract' ? 'extracted' : 'reordered'}_${files[0].name}`,
      blob,
      summary: `Created ${indices.length}-page PDF (${formatBytes(blob.size)})`
    }];
  }

  if (action === 'pdf_watermark') {
    const text = (options.watermarkText || 'BHARAT TOOLS').trim() || 'BHARAT TOOLS';
    const font = await srcDoc.embedFont(StandardFonts.HelveticaBold);
    srcDoc.getPages().forEach((page) => {
      const { width, height } = page.getSize();
      page.drawText(text, {
        x: width * 0.18,
        y: height * 0.45,
        size: Math.max(24, Math.min(48, width / 11)),
        font,
        color: rgb(0.12, 0.23, 0.54),
        opacity: 0.25,
        rotate: degrees(35)
      });
    });
    const out = await srcDoc.save();
    const blob = new Blob([out], { type: 'application/pdf' });
    return [{ name: `watermarked_${files[0].name}`, blob, summary: `Stamped "${text}" across ${total} page(s)` }];
  }

  if (action === 'pdf_to_image') {
    throw new Error('To snapshot PDF pages as images locally in-browser, use PDF Preview or your browser Print → Save as Image, or use the Bharat Tools Android app for direct multi-page PNG/JPG rendering.');
  }

  throw new Error('Unsupported PDF action.');
}

// --- B. IMAGE ENGINE (100% Native HTML5 Canvas) ---

export async function inspectImageFile(file) {
  const img = await loadImageElementFromFile(file);
  const w = img.naturalWidth;
  const h = img.naturalHeight;
  const mp = ((w * h) / 1_000_000).toFixed(2);
  return { name: file.name, width: w, height: h, megapixels: `${mp} MP`, bytes: file.size };
}

export async function processImageFile(file, mode, opts = {}) {
  const img = await loadImageElementFromFile(file);
  let srcX = 0;
  let srcY = 0;
  let srcW = img.naturalWidth;
  let srcH = img.naturalHeight;
  let dstW = srcW;
  let dstH = srcH;
  let mime = 'image/jpeg';
  let ext = 'jpg';
  const quality = Math.min(1, Math.max(0.15, (Number(opts.quality || 80)) / 100));

  if (mode === 'img_resize') {
    dstW = Math.max(16, Math.min(4096, Number(opts.width || srcW)));
    dstH = Math.max(16, Math.min(4096, Number(opts.height || srcH)));
  } else if (mode === 'img_crop') {
    const [rw, rh] = String(opts.ratio || '1:1').split(':').map(Number);
    const targetRatio = rw / rh;
    const currentRatio = srcW / srcH;
    if (currentRatio > targetRatio) {
      srcW = Math.round(srcH * targetRatio);
      srcX = Math.round((img.naturalWidth - srcW) / 2);
    } else {
      srcH = Math.round(srcW / targetRatio);
      srcY = Math.round((img.naturalHeight - srcH) / 2);
    }
    dstW = srcW;
    dstH = srcH;
  } else if (mode === 'img_jpg_to_png') {
    mime = 'image/png';
    ext = 'png';
  } else if (mode === 'img_webp') {
    mime = 'image/webp';
    ext = 'webp';
  }

  const canvas = document.createElement('canvas');
  const deg = mode === 'img_rotate' ? Number(opts.rotateDeg || 90) : 0;
  if (deg === 90 || deg === 270) {
    canvas.width = dstH;
    canvas.height = dstW;
  } else {
    canvas.width = dstW;
    canvas.height = dstH;
  }

  const ctx = canvas.getContext('2d');
  if (mime === 'image/jpeg') {
    ctx.fillStyle = '#FFFFFF';
    ctx.fillRect(0, 0, canvas.width, canvas.height);
  }

  ctx.save();
  if (mode === 'img_rotate') {
    ctx.translate(canvas.width / 2, canvas.height / 2);
    ctx.rotate((deg * Math.PI) / 180);
    ctx.scale(opts.flipH ? -1 : 1, opts.flipV ? -1 : 1);
    ctx.drawImage(img, srcX, srcY, srcW, srcH, -dstW / 2, -dstH / 2, dstW, dstH);
  } else {
    ctx.drawImage(img, srcX, srcY, srcW, srcH, 0, 0, dstW, dstH);
  }
  ctx.restore();

  if (mode === 'img_bg_util') {
    mime = 'image/png';
    ext = 'png';
    const tolerance = Number(opts.tolerance || 35);
    const data = ctx.getImageData(0, 0, canvas.width, canvas.height);
    const px = data.data;
    const r0 = px[0], g0 = px[1], b0 = px[2];
    const fillHex = opts.backdropHex || 'transparent';
    for (let i = 0; i < px.length; i += 4) {
      const dist = Math.abs(px[i] - r0) + Math.abs(px[i + 1] - g0) + Math.abs(px[i + 2] - b0);
      if (dist < tolerance * 2.5) {
        if (fillHex === 'transparent') px[i + 3] = 0;
        else if (fillHex === '#FFFFFF') { px[i] = 255; px[i + 1] = 255; px[i + 2] = 255; }
        else { px[i] = 30; px[i + 1] = 58; px[i + 2] = 138; }
      }
    }
    ctx.putImageData(data, 0, 0);
  }

  const blob = await new Promise((res) => canvas.toBlob(res, mime, quality));
  return {
    name: `bharat_${file.name.replace(/\.[^.]+$/, '')}.${ext}`,
    blob,
    width: canvas.width,
    height: canvas.height,
    originalBytes: file.size,
    outputBytes: blob.size,
    previewUrl: URL.createObjectURL(blob)
  };
}

// --- C. TEXT ENGINE ---

export function analyzeText(input) {
  const trimmed = input.trim();
  const words = trimmed ? trimmed.split(/\s+/).length : 0;
  const charsWithSpaces = input.length;
  const charsNoSpaces = input.replace(/\s/g, '').length;
  const lines = input ? input.split(/\r?\n/).length : 0;
  const sentences = trimmed ? trimmed.split(/[.!?।]+/).filter((s) => s.trim()).length : 0;
  const readMins = Math.max(1, Math.ceil(words / 200));
  return { words, charsWithSpaces, charsNoSpaces, lines, sentences, readMins };
}

export function transformText(mode, input, compareInput = '') {
  switch (mode) {
    case 'upper': return input.toUpperCase();
    case 'lower': return input.toLowerCase();
    case 'title': return input.toLowerCase().replace(/\b\w/g, (c) => c.toUpperCase());
    case 'sentence': return input.toLowerCase().replace(/(^\s*\w|[.!?]\s+\w)/g, (c) => c.toUpperCase());
    case 'toggle': return [...input].map((ch) => (ch === ch.toUpperCase() ? ch.toLowerCase() : ch.toUpperCase())).join('');
    case 'spaces': return input.replace(/[ \t]+/g, ' ').replace(/\n{3,}/g, '\n\n').trim();
    case 'reverse': return [...input].reverse().join('');
    case 'clean': return input.replace(/<[^>]*>/g, '').replace(/[^\w\s.,!?@:/()\-\u0900-\u097F]/g, '').replace(/[ \t]+/g, ' ').trim();
    case 'dedup': {
      const unique = [...new Set(input.split(/\r?\n/).map((l) => l.trim()).filter(Boolean))];
      return unique.sort((a, b) => a.localeCompare(b)).join('\n');
    }
    case 'bullets': return input.split(/\r?\n/).filter((l) => l.trim()).map((l) => `• ${l.trim()}`).join('\n');
    case 'numbered': return input.split(/\r?\n/).filter((l) => l.trim()).map((l, i) => `${i + 1}. ${l.trim()}`).join('\n');
    case 'json': {
      try { return JSON.stringify(JSON.parse(input), null, 2); }
      catch (e) { throw new Error(`Invalid JSON: ${e.message}`); }
    }
    case 'compare': {
      const a = input.split(/\r?\n/);
      const b = compareInput.split(/\r?\n/);
      const max = Math.max(a.length, b.length);
      const diff = [];
      for (let i = 0; i < max; i++) {
        const la = a[i] ?? '';
        const lb = b[i] ?? '';
        diff.push(la === lb ? `= [${i + 1}] ${la}` : `- [${i + 1}] ${la}\n+ [${i + 1}] ${lb}`);
      }
      return diff.join('\n');
    }
    default: return input;
  }
}

// --- D. CALCULATORS & PRODUCTIVITY ENGINE ---

export function evaluateExpression(expr) {
  const clean = String(expr || '').replace(/\s+/g, '');
  if (!clean) throw new Error('Enter an arithmetic expression.');
  if (!/^[0-9+\-*/().%]+$/.test(clean)) throw new Error('Only numbers and + - * / ( ) % are allowed.');
  const tokens = clean.match(/\d+(\.\d+)?|[+\-*/()%]/g) || [];
  const output = [];
  const ops = [];
  const prec = { '+': 1, '-': 1, '*': 2, '/': 2, '%': 2 };

  for (let i = 0; i < tokens.length; i++) {
    const tok = tokens[i];
    if (!Number.isNaN(Number(tok))) {
      output.push(Number(tok));
    } else if (tok === '(') {
      ops.push(tok);
    } else if (tok === ')') {
      while (ops.length && ops[ops.length - 1] !== '(') output.push(ops.pop());
      if (ops.pop() !== '(') throw new Error('Mismatched parentheses.');
    } else {
      if ((tok === '-' || tok === '+') && (i === 0 || '+-*/%('.includes(tokens[i - 1]))) {
        output.push(0);
      }
      while (ops.length && ops[ops.length - 1] !== '(' && prec[ops[ops.length - 1]] >= prec[tok]) {
        output.push(ops.pop());
      }
      ops.push(tok);
    }
  }
  while (ops.length) {
    const op = ops.pop();
    if (op === '(') throw new Error('Mismatched parentheses.');
    output.push(op);
  }
  const stack = [];
  for (const item of output) {
    if (typeof item === 'number') {
      stack.push(item);
    } else {
      const b = stack.pop();
      const a = stack.pop();
      if (a === undefined || b === undefined) throw new Error('Invalid expression.');
      if (item === '/' && b === 0) throw new Error('Cannot divide by zero.');
      if (item === '+') stack.push(a + b);
      else if (item === '-') stack.push(a - b);
      else if (item === '*') stack.push(a * b);
      else if (item === '/') stack.push(a / b);
      else if (item === '%') stack.push(a % b);
    }
  }
  if (stack.length !== 1 || !Number.isFinite(stack[0])) throw new Error('Invalid calculation.');
  return Number(stack[0].toFixed(8));
}

export function calculateGst(amount, ratePercent, inclusive) {
  const amt = Number(amount);
  const rate = Number(ratePercent);
  if (!(amt > 0)) throw new Error('Enter a valid amount > 0.');
  const base = inclusive ? amt / (1 + rate / 100) : amt;
  const gst = inclusive ? amt - base : amt * (rate / 100);
  const total = inclusive ? amt : amt + gst;
  return { base, gst, cgst: gst / 2, sgst: gst / 2, total };
}

export function calculateEmi(principal, annualRate, months) {
  const p = Number(principal);
  const r = Number(annualRate) / 12 / 100;
  const n = Math.round(Number(months));
  if (!(p > 0) || !(n > 0)) throw new Error('Enter valid principal and tenure months.');
  const emi = r === 0 ? p / n : (p * r * Math.pow(1 + r, n)) / (Math.pow(1 + r, n) - 1);
  const totalPayment = emi * n;
  return { emi, totalInterest: totalPayment - p, totalPayment };
}

export function calculateAge(dobStr) {
  if (!dobStr) throw new Error('Select a valid date of birth.');
  const dob = new Date(`${dobStr}T00:00:00`);
  const now = new Date();
  if (dob > now) throw new Error('Date of birth cannot be in the future.');
  let years = now.getFullYear() - dob.getFullYear();
  let months = now.getMonth() - dob.getMonth();
  let days = now.getDate() - dob.getDate();
  if (days < 0) {
    months--;
    days += new Date(now.getFullYear(), now.getMonth(), 0).getDate();
  }
  if (months < 0) {
    years--;
    months += 12;
  }
  const totalDays = Math.floor((now - dob) / (1000 * 60 * 60 * 24));
  return { years, months, days, totalDays };
}

export function generateSecurePassword(length = 16, { upper = true, lower = true, digits = true, symbols = true } = {}) {
  let pool = '';
  if (upper) pool += 'ABCDEFGHJKLMNPQRSTUVWXYZ';
  if (lower) pool += 'abcdefghijkmnopqrstuvwxyz';
  if (digits) pool += '23456789';
  if (symbols) pool += '!@#$%&*_-+=';
  if (!pool) throw new Error('Select at least one character group.');
  const arr = new Uint32Array(length);
  crypto.getRandomValues(arr);
  return Array.from(arr, (n) => pool[n % pool.length]).join('');
}

export async function fetchCurrencyRates(base = 'INR') {
  try {
    const resp = await fetch(`https://api.frankfurter.app/latest?from=${encodeURIComponent(base)}`);
    if (!resp.ok) throw new Error('Rate service unavailable');
    const data = await resp.json();
    return { live: true, base: data.base, date: data.date, rates: data.rates };
  } catch {
    return {
      live: false,
      base: 'INR',
      date: 'Offline Reference',
      rates: { USD: 0.0119, EUR: 0.0110, GBP: 0.0094, AED: 0.0437, SGD: 0.0159, JPY: 1.82 }
    };
  }
}
