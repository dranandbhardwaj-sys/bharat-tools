import { generateDemoTemplateContent } from './templates.js';
import { getRuntimeSessionToken } from './storage.js';

export const AI_PROVIDERS = [
  { id: 'custom_proxy', label: 'Custom Backend Proxy (Recommended)', models: ['gemini-2.5-flash', 'gpt-4o-mini', 'claude-3-5-sonnet'] },
  { id: 'openai_compat', label: 'OpenAI-Compatible Endpoint', models: ['gpt-4o-mini', 'gpt-4.1-mini', 'llama-3.3-70b'] },
  { id: 'local_ollama', label: 'Local Ollama Proxy', models: ['llama3.1', 'qwen2.5', 'mistral'] }
];

function buildStructuredPrompt(tool, { topic, context, tone, language, length }) {
  const langLabel = language === 'hi' ? 'Hindi (Devanagari)' : language === 'both' ? 'Bilingual English + Hindi' : 'English';
  return [
    `Tool: ${tool.titleEn} (${tool.descEn})`,
    `Topic / Input: ${topic}`,
    context ? `Audience / Context: ${context}` : null,
    `Tone: ${tone}`,
    `Language: ${langLabel}`,
    `Length: ${length}`,
    'Return clean, ready-to-publish output formatted for creators.'
  ].filter(Boolean).join('\n');
}

export async function generateCreatorContent(tool, params, aiConfig) {
  const useApi = aiConfig?.mode === 'api' && String(aiConfig?.endpointUrl || '').trim().startsWith('http');

  if (!useApi) {
    // Brief natural UI feedback delay while keeping Demo Mode fast & deterministic
    await new Promise((r) => setTimeout(r, 240));
    return {
      source: 'demo',
      modelUsed: 'Local Template Engine',
      text: generateDemoTemplateContent(tool, params)
    };
  }

  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), 20000);
  try {
    const token = getRuntimeSessionToken();
    const headers = { 'Content-Type': 'application/json' };
    if (token) headers.Authorization = `Bearer ${token}`;

    const resp = await fetch(aiConfig.endpointUrl.trim(), {
      method: 'POST',
      headers,
      signal: controller.signal,
      body: JSON.stringify({
        model: aiConfig.model || 'gemini-2.5-flash',
        messages: [
          { role: 'system', content: 'You are AI Creator Toolkit, an expert bilingual (English/Hindi) content assistant.' },
          { role: 'user', content: buildStructuredPrompt(tool, params) }
        ]
      })
    });

    if (!resp.ok) {
      throw new Error(`Endpoint returned HTTP ${resp.status}`);
    }
    const data = await resp.json();
    const reply = data?.choices?.[0]?.message?.content || data?.output || data?.text;
    if (!reply) {
      throw new Error('Endpoint responded without a standard message payload.');
    }
    return {
      source: 'api',
      modelUsed: aiConfig.model || 'Custom Endpoint',
      text: String(reply).trim()
    };
  } finally {
    clearTimeout(timer);
  }
}

export async function testAiConnection(aiConfig) {
  if (aiConfig?.mode !== 'api') {
    return { ok: true, mode: 'demo', message: 'Demo Template Mode is active (100% offline, no external API call needed).' };
  }
  const url = String(aiConfig?.endpointUrl || '').trim();
  if (!url.startsWith('https://') && !url.startsWith('http://localhost') && !url.startsWith('http://127.0.0.1')) {
    return { ok: false, mode: 'api', message: 'Enter a valid HTTPS proxy endpoint URL (or http://localhost for local dev).' };
  }
  try {
    const token = getRuntimeSessionToken();
    const resp = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {})
      },
      body: JSON.stringify({
        model: aiConfig.model || 'gemini-2.5-flash',
        messages: [{ role: 'user', content: 'ping' }],
        max_tokens: 8
      })
    });
    if (!resp.ok) return { ok: false, mode: 'api', message: `Connection failed: HTTP ${resp.status}` };
    return { ok: true, mode: 'api', message: `Connected successfully (HTTP ${resp.status}).` };
  } catch (err) {
    return { ok: false, mode: 'api', message: `Connection error: ${err.message}` };
  }
}
