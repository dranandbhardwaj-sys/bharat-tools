// Pure JavaScript ISO/IEC 18004 QR Code Generator (Byte mode, Versions 1..10, ECC Level M)
// Zero external dependencies — works 100% offline in PWA & GitHub Pages.

const QR_CAPACITY_M = [0, 14, 26, 42, 62, 84, 106, 122, 152, 180, 213];
const ALIGNMENT_POS = [
  [], [], [6, 18], [6, 22], [6, 26], [6, 30], [6, 34],
  [6, 22, 38], [6, 24, 42], [6, 26, 46], [6, 28, 50]
];

const GF_EXP = new Uint8Array(512);
const GF_LOG = new Uint8Array(256);
(() => {
  let x = 1;
  for (let i = 0; i < 255; i++) {
    GF_EXP[i] = x;
    GF_LOG[x] = i;
    x <<= 1;
    if (x & 0x100) x ^= 0x11d;
  }
  for (let i = 255; i < 512; i++) GF_EXP[i] = GF_EXP[i - 255];
})();

function gfMul(a, b) {
  return a === 0 || b === 0 ? 0 : GF_EXP[GF_LOG[a] + GF_LOG[b]];
}

function rsGenerator(degree) {
  let poly = [1];
  for (let i = 0; i < degree; i++) {
    const next = new Array(poly.length + 1).fill(0);
    for (let j = 0; j < poly.length; j++) {
      next[j] ^= poly[j];
      next[j + 1] ^= gfMul(poly[j], GF_EXP[i]);
    }
    poly = next;
  }
  return poly;
}

function rsEncode(data, eccLen) {
  const gen = rsGenerator(eccLen);
  const res = new Array(data.length + eccLen).fill(0);
  for (let i = 0; i < data.length; i++) res[i] = data[i];
  for (let i = 0; i < data.length; i++) {
    const factor = res[i];
    if (factor !== 0) {
      for (let j = 0; j < gen.length; j++) {
        res[i + j] ^= gfMul(gen[j], factor);
      }
    }
  }
  return res.slice(data.length);
}

function encodeBytes(text) {
  const bytes = new TextEncoder().encode(text);
  let version = 1;
  while (version <= 10 && bytes.length > QR_CAPACITY_M[version]) version++;
  if (version > 10) throw new Error('Input is too long for offline QR encoder (max 213 UTF-8 bytes).');

  // Exact ISO/IEC 18004 Level M block groups: [[count1, data1, count2?, data2?], eccPerBlock]
  const EXACT_M = {
    1: [[1, 16], 10],
    2: [[1, 28], 16],
    3: [[1, 44], 26],
    4: [[2, 32], 18],
    5: [[2, 43], 24],
    6: [[4, 27], 16],
    7: [[4, 31], 18],
    8: [[2, 38, 2, 39], 22],
    9: [[3, 36, 2, 37], 22],
    10: [[4, 43, 1, 44], 26]
  };
  const [groupSpec, eccLen] = EXACT_M[version];
  const dataCap = groupSpec.length === 2
    ? groupSpec[0] * groupSpec[1]
    : groupSpec[0] * groupSpec[1] + groupSpec[2] * groupSpec[3];

  const bits = [];
  const pushBits = (val, len) => {
    for (let i = len - 1; i >= 0; i--) bits.push((val >> i) & 1);
  };
  pushBits(0b0100, 4); // Byte mode
  pushBits(bytes.length, version >= 10 ? 16 : 8);
  for (const b of bytes) pushBits(b, 8);
  const maxBits = dataCap * 8;
  for (let i = 0; i < 4 && bits.length < maxBits; i++) bits.push(0);
  while (bits.length % 8 !== 0) bits.push(0);
  const dataCw = [];
  for (let i = 0; i < bits.length; i += 8) {
    let v = 0;
    for (let j = 0; j < 8; j++) v = (v << 1) | bits[i + j];
    dataCw.push(v);
  }
  const padBytes = [0xec, 0x11];
  let pi = 0;
  while (dataCw.length < dataCap) {
    dataCw.push(padBytes[pi % 2]);
    pi++;
  }

  const blockData = [];
  const blockEcc = [];
  let offset = 0;
  const addGroup = (count, size) => {
    for (let b = 0; b < count; b++) {
      const slice = dataCw.slice(offset, offset + size);
      offset += size;
      blockData.push(slice);
      blockEcc.push(rsEncode(slice, eccLen));
    }
  };
  addGroup(groupSpec[0], groupSpec[1]);
  if (groupSpec.length === 4) addGroup(groupSpec[2], groupSpec[3]);

  const interleaved = [];
  const maxDataLen = Math.max(...blockData.map((b) => b.length));
  for (let i = 0; i < maxDataLen; i++) {
    for (const b of blockData) if (i < b.length) interleaved.push(b[i]);
  }
  for (let i = 0; i < eccLen; i++) {
    for (const e of blockEcc) interleaved.push(e[i]);
  }
  return { version, codewords: interleaved };
}

function buildQrMatrix(text) {
  const { version, codewords } = encodeBytes(text);
  const size = version * 4 + 17;
  const mat = Array.from({ length: size }, () => new Int8Array(size));
  const isFunc = Array.from({ length: size }, () => new Uint8Array(size));

  const setFunc = (r, c, val) => {
    if (r >= 0 && r < size && c >= 0 && c < size) {
      mat[r][c] = val ? 1 : 0;
      isFunc[r][c] = 1;
    }
  };

  const drawFinder = (r0, c0) => {
    for (let r = -1; r <= 7; r++) {
      for (let c = -1; c <= 7; c++) {
        const dark =
          r >= 0 && r <= 6 && c >= 0 && c <= 6 &&
          (r === 0 || r === 6 || c === 0 || c === 6 || (r >= 2 && r <= 4 && c >= 2 && c <= 4));
        setFunc(r0 + r, c0 + c, dark);
      }
    }
  };

  drawFinder(0, 0);
  drawFinder(0, size - 7);
  drawFinder(size - 7, 0);

  for (let i = 8; i < size - 8; i++) {
    setFunc(6, i, i % 2 === 0);
    setFunc(i, 6, i % 2 === 0);
  }
  setFunc(size - 8, 8, true);

  const align = ALIGNMENT_POS[version];
  for (const r of align) {
    for (const c of align) {
      if (isFunc[r][c]) continue;
      for (let dr = -2; dr <= 2; dr++) {
        for (let dc = -2; dc <= 2; dc++) {
          const dark = Math.max(Math.abs(dr), Math.abs(dc)) !== 1;
          setFunc(r + dr, c + dc, dark);
        }
      }
    }
  }

  // Reserve format info cells
  for (let i = 0; i <= 8; i++) {
    if (i !== 6) {
      isFunc[8][i] = 1;
      isFunc[i][8] = 1;
    }
  }
  for (let i = size - 8; i < size; i++) {
    isFunc[8][i] = 1;
    isFunc[i][8] = 1;
  }
  if (version >= 7) {
    for (let i = 0; i < 6; i++) {
      for (let j = 0; j < 3; j++) {
        isFunc[i][size - 11 + j] = 1;
        isFunc[size - 11 + j][i] = 1;
      }
    }
  }

  const allBits = [];
  for (const cw of codewords) {
    for (let i = 7; i >= 0; i--) allBits.push((cw >> i) & 1);
  }
  let bitIdx = 0;
  let up = true;
  for (let right = size - 1; right >= 1; right -= 2) {
    if (right === 6) right = 5;
    for (let vert = 0; vert < size; vert++) {
      const r = up ? size - 1 - vert : vert;
      for (let j = 0; j < 2; j++) {
        const c = right - j;
        if (!isFunc[r][c]) {
          mat[r][c] = bitIdx < allBits.length ? allBits[bitIdx++] : 0;
        }
      }
    }
    up = !up;
  }

  const applyMask = (m) => {
    const out = mat.map((row) => Int8Array.from(row));
    for (let r = 0; r < size; r++) {
      for (let c = 0; c < size; c++) {
        if (isFunc[r][c]) continue;
        let inv = false;
        switch (m) {
          case 0: inv = (r + c) % 2 === 0; break;
          case 1: inv = r % 2 === 0; break;
          case 2: inv = c % 3 === 0; break;
          case 3: inv = (r + c) % 3 === 0; break;
          case 4: inv = (Math.floor(r / 2) + Math.floor(c / 3)) % 2 === 0; break;
          case 5: inv = ((r * c) % 2) + ((r * c) % 3) === 0; break;
          case 6: inv = (((r * c) % 2) + ((r * c) % 3)) % 2 === 0; break;
          case 7: inv = (((r + c) % 2) + ((r * c) % 3)) % 2 === 0; break;
        }
        if (inv) out[r][c] ^= 1;
      }
    }
    // Format info for ECC Level M (00) + mask (3 bits)
    const data = (0b00 << 3) | m;
    let rem = data << 10;
    const poly = 0b10100110111;
    for (let i = 14; i >= 10; i--) {
      if ((rem >> i) & 1) rem ^= poly << (i - 10);
    }
    const fmt = ((data << 10) | rem) ^ 0b101010000010010;
    const fmtCoordsA = [
      [8, 0], [8, 1], [8, 2], [8, 3], [8, 4], [8, 5], [8, 7], [8, 8],
      [7, 8], [5, 8], [4, 8], [3, 8], [2, 8], [1, 8], [0, 8]
    ];
    for (let i = 0; i < 15; i++) {
      const bit = (fmt >> (14 - i)) & 1;
      const [ar, ac] = fmtCoordsA[i];
      out[ar][ac] = bit;
      if (i < 8) out[8][size - 1 - i] = (fmt >> i) & 1;
      else out[size - 15 + i][8] = (fmt >> i) & 1;
    }
    if (version >= 7) {
      let vrem = version << 12;
      const vpoly = 0b1111100100101;
      for (let i = 17; i >= 12; i--) {
        if ((vrem >> i) & 1) vrem ^= vpoly << (i - 12);
      }
      const vbits = (version << 12) | vrem;
      for (let i = 0; i < 18; i++) {
        const bit = (vbits >> i) & 1;
        const a = Math.floor(i / 3);
        const b = size - 11 + (i % 3);
        out[a][b] = bit;
        out[b][a] = bit;
      }
    }
    return out;
  };

  let bestMat = null;
  let bestScore = Infinity;
  for (let m = 0; m < 8; m++) {
    const candidate = applyMask(m);
    let score = 0;
    for (let r = 0; r < size; r++) {
      let run = 1;
      for (let c = 1; c < size; c++) {
        if (candidate[r][c] === candidate[r][c - 1]) run++;
        else { if (run >= 5) score += 3 + (run - 5); run = 1; }
      }
      if (run >= 5) score += 3 + (run - 5);
    }
    for (let c = 0; c < size; c++) {
      let run = 1;
      for (let r = 1; r < size; r++) {
        if (candidate[r][c] === candidate[r - 1][c]) run++;
        else { if (run >= 5) score += 3 + (run - 5); run = 1; }
      }
      if (run >= 5) score += 3 + (run - 5);
    }
    if (score < bestScore) {
      bestScore = score;
      bestMat = candidate;
    }
  }
  return bestMat;
}

export function buildQrPayload(type, fields) {
  switch (type) {
    case 'url': {
      const raw = (fields.url || '').trim();
      if (!raw) throw new Error('Enter a website URL.');
      return /^https?:\/\//i.test(raw) ? raw : `https://${raw}`;
    }
    case 'text': {
      const txt = (fields.text || '').trim();
      if (!txt) throw new Error('Enter text content.');
      return txt;
    }
    case 'phone': {
      const p = (fields.phone || '').trim();
      if (!p) throw new Error('Enter a phone number.');
      return `tel:${p}`;
    }
    case 'wifi': {
      const ssid = (fields.ssid || '').trim();
      if (!ssid) throw new Error('Enter a Wi-Fi SSID.');
      const sec = fields.security || 'WPA';
      const pass = fields.password || '';
      return `WIFI:T:${sec};S:${ssid};P:${pass};;`;
    }
    case 'email': {
      const to = (fields.email || '').trim();
      if (!to) throw new Error('Enter a recipient email.');
      const q = new URLSearchParams();
      if (fields.subject) q.set('subject', fields.subject);
      if (fields.body) q.set('body', fields.body);
      const qs = q.toString();
      return qs ? `mailto:${to}?${qs}` : `mailto:${to}`;
    }
    case 'whatsapp': {
      const digits = (fields.waPhone || '').replace(/[^\d]/g, '');
      if (!digits) throw new Error('Enter WhatsApp number with country code.');
      const msg = (fields.waMsg || '').trim();
      return msg ? `https://wa.me/${digits}?text=${encodeURIComponent(msg)}` : `https://wa.me/${digits}`;
    }
    case 'upi': {
      const vpa = (fields.vpa || '').trim();
      if (!vpa || !vpa.includes('@')) throw new Error('Enter a valid UPI VPA (e.g. name@upi).');
      const params = new URLSearchParams({ pa: vpa, cu: 'INR' });
      if (fields.payee) params.set('pn', fields.payee.trim());
      if (fields.amount && Number(fields.amount) > 0) params.set('am', String(fields.amount).trim());
      if (fields.note) params.set('tn', fields.note.trim());
      return `upi://pay?${params.toString()}`;
    }
    default:
      throw new Error('Unknown QR type.');
  }
}

export function renderQrToCanvas(canvas, payload, fgHex = '#0F172A', bgHex = '#FFFFFF') {
  const mat = buildQrMatrix(payload);
  const n = mat.length;
  const quiet = 4;
  const totalModules = n + quiet * 2;
  const pxSize = 360;
  const cell = Math.floor(pxSize / totalModules);
  const actualSize = cell * totalModules;
  canvas.width = actualSize;
  canvas.height = actualSize;
  const ctx = canvas.getContext('2d');
  ctx.fillStyle = bgHex;
  ctx.fillRect(0, 0, actualSize, actualSize);
  ctx.fillStyle = fgHex;
  for (let r = 0; r < n; r++) {
    for (let c = 0; c < n; c++) {
      if (mat[r][c]) {
        ctx.fillRect((c + quiet) * cell, (r + quiet) * cell, cell, cell);
      }
    }
  }
}

export function renderQrToSvg(payload, fgHex = '#0F172A', bgHex = '#FFFFFF') {
  const mat = buildQrMatrix(payload);
  const n = mat.length;
  const quiet = 4;
  const dim = n + quiet * 2;
  let rects = `<rect width="${dim}" height="${dim}" fill="${bgHex}"/>`;
  for (let r = 0; r < n; r++) {
    for (let c = 0; c < n; c++) {
      if (mat[r][c]) rects += `<rect x="${c + quiet}" y="${r + quiet}" width="1" height="1" fill="${fgHex}"/>`;
    }
  }
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${dim} ${dim}" shape-rendering="crispEdges">${rects}</svg>`;
}
