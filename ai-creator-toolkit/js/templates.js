// Demo Mode Template & Local Text-Processing Engine for all 64 AI Creator Toolkit tools.
// Used strictly in Demo Mode when no live backend AI proxy is connected.

function cleanText(str) {
  return String(str || '').trim().replace(/\s+/g, ' ');
}

function summarizeLocally(raw, length) {
  const sentences = String(raw || '')
    .replace(/\r?\n+/g, ' ')
    .split(/(?<=[.!?।])\s+/)
    .map((s) => s.trim())
    .filter(Boolean);
  const words = cleanText(raw).split(' ').filter(Boolean);
  const target = length === 'short' ? 2 : length === 'long' ? 5 : 3;
  const picked = sentences.slice(0, Math.min(target, sentences.length));
  return {
    wordCount: words.length,
    sentenceCount: sentences.length,
    points: picked.length ? picked : [cleanText(raw)]
  };
}

function grammarCheckLocally(raw) {
  const issues = [];
  let fixed = String(raw || '').trim();
  if (/  +/.test(fixed)) {
    issues.push('Collapsed multiple consecutive spaces.');
    fixed = fixed.replace(/ {2,}/g, ' ');
  }
  if (/^[a-z]/.test(fixed)) {
    issues.push('Capitalized the opening letter.');
    fixed = fixed.charAt(0).toUpperCase() + fixed.slice(1);
  }
  if (/\bi\b/.test(fixed)) {
    issues.push('Capitalized standalone pronoun "I".');
    fixed = fixed.replace(/\bi\b/g, 'I');
  }
  if (/\s+([,.!?])/.test(fixed)) {
    issues.push('Removed space before punctuation.');
    fixed = fixed.replace(/\s+([,.!?])/g, '$1');
  }
  if (fixed && !/[.!?।]$/.test(fixed)) {
    issues.push('Added closing punctuation.');
    fixed += '.';
  }
  return { fixed, issues };
}

function translatePhraseLocal(text, direction) {
  const hiToEnDict = [
    [/नमस्ते/g, 'Hello'],
    [/धन्यवाद/g, 'Thank you'],
    [/स्वागत है/g, 'Welcome'],
    [/भारत/g, 'India'],
    [/डिजिटल/g, 'Digital'],
    [/वीडियो/g, 'video'],
    [/कंटेंट/g, 'content'],
    [/व्यापार|बिज़नेस/g, 'business'],
    [/सफलता/g, 'success'],
    [/आज/g, 'today'],
    [/नया|नई|नए/g, 'new'],
    [/कैसे/g, 'how'],
    [/और/g, 'and'],
    [/है/g, 'is']
  ];
  const enToHiDict = [
    [/\bhello\b/gi, 'नमस्ते'],
    [/\bwelcome\b/gi, 'स्वागत है'],
    [/\bthank you\b/gi, 'धन्यवाद'],
    [/\bthanks\b/gi, 'धन्यवाद'],
    [/\bcreator\b/gi, 'क्रिएटर'],
    [/\bcontent\b/gi, 'कंटेंट'],
    [/\bvideo\b/gi, 'वीडियो'],
    [/\bbusiness\b/gi, 'व्यापार'],
    [/\bmarketing\b/gi, 'मार्केटिंग'],
    [/\bproduct\b/gi, 'उत्पाद'],
    [/\btoday\b/gi, 'आज'],
    [/\bnew\b/gi, 'नया'],
    [/\bsuccess\b/gi, 'सफलता'],
    [/\bindia\b/gi, 'भारत']
  ];
  let out = String(text || '').trim();
  const dict = direction === 'hi2en' ? hiToEnDict : enToHiDict;
  let hits = 0;
  for (const [pattern, replacement] of dict) {
    if (pattern.test(out)) {
      out = out.replace(pattern, replacement);
      hits++;
    }
  }
  return { out, hits };
}

function toneAdjective(tone, isHi) {
  const map = {
    professional: isHi ? 'पेशेवर और स्पष्ट' : 'clear, authoritative, and results-focused',
    friendly: isHi ? 'दोस्ताना और आत्मीय' : 'warm, approachable, and conversational',
    viral: isHi ? 'तेज़, ऊर्जावान और आकर्षक' : 'high-energy, curiosity-driven, and scroll-stopping',
    persuasive: isHi ? 'प्रभावशाली और रूपांतरण-केंद्रित' : 'benefit-led and conversion-focused',
    casual: isHi ? 'सहज और सरल' : 'relaxed, relatable, and everyday',
    storytelling: isHi ? 'रोचक कहानी शैली' : 'narrative-driven with vivid imagery',
    academic: isHi ? 'तथ्यात्मक और सुव्यवस्थित' : 'analytical, structured, and well-reasoned'
  };
  return map[tone] || map.professional;
}

function slugTag(topic) {
  return cleanText(topic)
    .replace(/[^a-zA-Z0-9\s]/g, '')
    .split(' ')
    .filter(Boolean)
    .slice(0, 3)
    .map((w) => w.charAt(0).toUpperCase() + w.slice(1).toLowerCase())
    .join('') || 'CreatorStudio';
}

function buildEnglishOutput(tool, { topic, context, tone, length }) {
  const tpc = cleanText(topic);
  const ctx = cleanText(context) || 'creators, students, and growing brands';
  const adj = toneAdjective(tone, false);
  const tag = slugTag(tpc);
  const extraDetail = length === 'long'
    ? `\n\nDeep-Dive Execution Notes:\n- Primary audience angle: ${ctx}\n- Style cue: Keep delivery ${adj}.\n- Repurpose this asset into a carousel, a 45s short clip, and an email teaser.`
    : '';

  switch (tool.id) {
    case 'ai_blog_writer':
    case 'article_generator':
      return `# ${tpc}: A Practical Guide for ${ctx}\n\n## Introduction\nIf you want real progress with ${tpc}, focus on repeatable systems rather than guesswork. Written in a ${adj} voice for ${ctx}, this guide breaks down what works right now.\n\n## 1. Core Foundation\nStart with the single biggest bottleneck around ${tpc}. Document your goal in one clear sentence before choosing tools or channels.\n\n## 2. Step-by-Step Workflow\n- Audit your current approach and remove friction.\n- Build a weekly template for ${tpc} that takes under 30 minutes.\n- Measure engagement, saves, and conversions after 7 days.\n\n## Conclusion & CTA\nSmall consistent improvements compound fast. Save this checklist and put step one into practice today.${extraDetail}`;

    case 'paragraph_generator':
      return `${tpc} plays a decisive role for ${ctx} looking to stand out with ${adj} communication. Instead of overcomplicating the message, focus on clarity, tangible value, and a concrete next step so readers immediately understand why ${tpc} matters today.${length !== 'short' ? ` When combined with consistent feedback loops and clean visual presentation, ${tpc} becomes a reliable growth lever week after week.` : ''}`;

    case 'essay_writer':
      return `Title: ${tpc} — Opportunities, Challenges, and the Way Forward\n\nIntroduction:\n${tpc} has become a defining topic for ${ctx}. Understanding its practical impact requires looking at both immediate benefits and long-term responsibilities.\n\nBody Analysis:\nFirst, ${tpc} lowers barriers to entry and speeds up daily execution. Second, sustainable outcomes depend on ethics, quality control, and continuous learning rather than shortcuts.\n\nConclusion:\nIn summary, adopting ${tpc} thoughtfully empowers ${ctx} to create lasting value while staying adaptable for the future.${extraDetail}`;

    case 'story_generator':
      return `Title: The Turning Point of ${tpc}\n\nAarav stared at the blank notebook on his desk. Everyone around him said ${tpc} was too crowded to matter—yet he decided to test one bold idea aimed at ${ctx}.\n\nBy midnight, three drafts had failed, but the fourth carried an unmistakable spark: honest, ${adj}, and impossible to ignore. The next morning, the first message arrived: "This spoke directly to what I needed today."\n\nLesson: Momentum starts the moment you ship version one.${extraDetail}`;

    case 'email_writer':
    case 'professional_email':
      return `Subject: Quick idea regarding ${tpc}\n\nHi [Name],\n\nHope your week is going smoothly. I am reaching out regarding ${tpc} for ${ctx}.\n\nHere is a quick summary of how we can move forward:\n• Objective: Deliver measurable value on ${tpc}\n• Timeline: Ready to start this week\n• Next Step: Let me know if a 10-minute call on Thursday works\n\nWarm regards,\n[Your Name]`;

    case 'letter_writer':
      return `[Your Name]\n[City, State]\nDate: ${new Date().toLocaleDateString('en-IN')}\n\nTo,\nThe Recipient / Authority\n\nSubject: Application / Request regarding ${tpc}\n\nRespected Sir/Madam,\n\nI am writing to formally bring to your kind attention the matter of ${tpc} concerning ${ctx}. I request your consideration and guidance so this can be addressed promptly.\n\nThank you for your time and support.\n\nSincerely,\n[Your Name]`;

    case 'rewrite_text': {
      const sentences = summarizeLocally(tpc, 'long').points;
      return `Polished Rewrite (${tone} tone):\n\n${sentences.map((s) => s.replace(/\bvery\b/gi, 'remarkably')).join(' ')}\n\nKey Improvement Notes:\n- Tightened pacing for ${ctx}\n- Aligned voice to ${adj} style`;
    }

    case 'text_summarizer': {
      const sum = summarizeLocally(tpc, length);
      return `Summary (${sum.wordCount} words → ${sum.points.length} key points):\n\n${sum.points.map((p, i) => `${i + 1}. ${p}`).join('\n')}`;
    }

    case 'grammar_checker': {
      const chk = grammarCheckLocally(tpc);
      return `Corrected Version:\n${chk.fixed}\n\nChecks Applied:\n${chk.issues.length ? chk.issues.map((x) => `• ${x}`).join('\n') : '• No spacing, capitalization, or closing punctuation issues detected by local rules.'}`;
    }

    case 'hindi_to_english': {
      const res = translatePhraseLocal(tpc, 'hi2en');
      return `English Adaptation (Demo Rule-Based Helper):\n${res.out}\n\nNote: ${res.hits} common vocabulary match(es) applied locally. Connect a backend AI endpoint in Settings for full neural Hindi↔English translation.`;
    }

    case 'english_to_hindi': {
      const res = translatePhraseLocal(tpc, 'en2hi');
      return `हिन्दी रूपांतरण (डेमो रूल-आधारित सहायक):\n${res.out}\n\nनोट: ${res.hits} सामान्य शब्दों का स्थानीय अनुवाद लागू हुआ। पूर्ण न्यूरल अनुवाद के लिए सेटिंग्स में अपना AI बैकएंड एंडपॉइंट जोड़ें।`;
    }

    case 'text_translator':
      return `Bilingual Adaptation (${tone}):\n\n[EN]: ${tpc}\n[HI]: ${translatePhraseLocal(tpc, 'en2hi').out}\n\nAudience: ${ctx}`;

    case 'instagram_caption':
      return `Stop scrolling if you care about ${tpc} ✨\n\nMost ${ctx} overthink step one—start with consistency, clear value, and a ${adj} story.\n\nSave this post for your next upload and drop "YES" in the comments if you want Part 2! 👇\n\n#${tag} #CreatorTips #BharatCreators #ContentCreation #GrowthMindset`;

    case 'instagram_bio':
      return `Option 1:\n✨ Helping ${ctx} master ${tpc}\n🎯 Practical tips • Daily workflows\n👇 Free checklist & links\n\nOption 2:\n🚀 ${tpc} Specialist\n🇮🇳 Creating in EN & HI\n📩 DM "START" for collabs`;

    case 'facebook_post':
      return `Quick question for everyone working on ${tpc} 👇\n\nWhat has helped you the most this month—better planning or faster execution? For ${ctx}, keeping things ${adj} makes the biggest difference.\n\nShare your experience in the comments so others can learn too!`;

    case 'youtube_description':
      return `In today's video, we break down ${tpc} step-by-step for ${ctx}.\n\n⏱️ Timestamps:\n00:00 Hook & What You'll Learn\n00:45 Common Mistake to Avoid\n02:15 Core Framework for ${tpc}\n05:30 Action Checklist & Final Tips\n\n🔔 Subscribe for weekly creator tutorials!\n📌 Resources & Links: [Add Your Link]\n\n#${tag} #YouTubeIndia #CreatorToolkit`;

    case 'youtube_title':
      return `1. ${tpc} Explained in 5 Minutes (Zero Fluff!)\n2. Stop Making This ${tpc} Mistake in 2026\n3. How ${ctx} Can Master ${tpc} Faster\n4. The Honest Truth About ${tpc} Nobody Shares\n5. ${tpc}: Complete Beginner-to-Pro Blueprint`;

    case 'youtube_shorts_script':
    case 'reel_script':
    case 'short_video_script':
      return `[0:00 - 0:03 | HOOK]\n(Camera close-up, bold caption on screen): "If you're struggling with ${tpc}, watch the next 30 seconds!"\n\n[0:03 - 0:20 | VALUE]\nHere are 3 quick rules for ${ctx}:\n1. Keep your opening ${adj}.\n2. Focus on one clear problem around ${tpc}.\n3. End with a direct action step.\n\n[0:20 - 0:30 | CTA]\n"Save this Reel before you forget and follow for daily creator tips!"`;

    case 'viral_hook':
      return `10 Viral Hooks for "${tpc}":\n1. Nobody talks about this hidden shortcut in ${tpc}...\n2. Stop doing ${tpc} the hard way—try this instead.\n3. I tested ${tpc} for 7 days so you don't have to.\n4. 3 mistakes silently ruining your ${tpc} results.\n5. If I had to start ${tpc} from zero today, I'd do this.\n6. Read this before spending a rupee on ${tpc}.\n7. Why 90% of ${ctx} get ${tpc} wrong.\n8. The 60-second rule for mastering ${tpc}.\n9. Watch till the end for the ${tpc} template.\n10. Save this—future you will thank you for ${tpc}.`;

    case 'hashtag_generator':
      return `Broad Reach:\n#${tag} #ContentCreator #DigitalMarketing #CreatorEconomy #ExplorePage\n\nNiche & Targeted:\n#${tag}Tips #${tag}Guide #CreatorWorkflow #GrowthStrategy #OnlineBusiness\n\nIndian Creator Community:\n#BharatCreators #MadeInIndia #HindiCreators #StartupIndia`;

    case 'linkedin_post':
      return `Most people underestimate ${tpc}.\n\nOver the last few months working with ${ctx}, one pattern kept repeating:\n\n→ Clarity beats complexity.\n→ Consistency beats one-time bursts.\n→ A ${adj} message earns trust faster than hype.\n\nIf you're building around ${tpc} this quarter, focus on shipping small improvements every week.\n\nWhat's one lesson ${tpc} taught you recently?`;

    case 'twitter_post':
      return `1/ ${tpc} doesn't need to be complicated.\n\nHere is a 3-step playbook for ${ctx}:\n• Define 1 outcome\n• Ship a clean draft in 20 mins\n• Iterate on real feedback\n\n2/ Save this thread starter & build momentum today.`;

    case 'ad_copy':
      return `Primary Text:\nTired of wasting hours on ${tpc}? Built for ${ctx}, our solution gives you faster results with a ${adj} workflow—no steep learning curve.\n\nHeadline: Master ${tpc} in Minutes Today\nDescription: Simple, reliable & ready for instant launch.\nCTA Button: Get Started`;

    case 'product_description':
      return `**${tpc}**\n\nDesigned for ${ctx}, ${tpc} combines reliability with effortless everyday usability.\n\nKey Highlights:\n• Built for fast, consistent performance\n• Clean, modern finish ideal for daily use\n• Backed by responsive customer support\n\nOrder now and upgrade your experience today.`;

    case 'sales_copy':
      return `[AIDA Framework — ${tpc}]\n• Attention: Still stuck trying to figure out ${tpc}?\n• Interest: Most ${ctx} lose hours every week to scattered tools.\n• Desire: Imagine completing ${tpc} in half the time with ${adj} output ready to publish.\n• Action: Tap below to start right now.`;

    case 'landing_page_copy':
      return `[HERO HEADLINE]\nBuild Better ${tpc} in Half the Time.\n[SUBHEADLINE]\nThe smart, ${adj} workspace built for ${ctx}.\n\n[3 VALUE PROPS]\n1. Instant Templates — Start without blank-page stress.\n2. Consistent Quality — Keep every asset on-brand.\n3. Export Anywhere — Copy, TXT, or Markdown in one click.\n\n[CTA BUTTON]\nStart Creating Free →`;

    case 'business_name':
      return `Brand Name Ideas for "${tpc}":\n1. ${tag}Craft Studio (craft.in)\n2. Nova${tag} Labs\n3. Bharat${tag} Co.\n4. Spark${tag} Hub\n5. Pure${tag} Works\n6. Pulse${tag} Digital\n7. Veda${tag} Collective\n8. Zenith${tag} India`;

    case 'slogan_generator':
      return `1. ${tpc}. Built Smarter.\n2. Every Idea, Ready to Ship.\n3. Powering ${ctx} Every Day.\n4. Simple Tools. Serious Growth.\n5. From Spark to Publish in Minutes.`;

    case 'seo_meta':
      return `SEO Title Tag (~55 chars):\n${tpc} Guide & Templates for ${ctx}\n\nMeta Description (~148 chars):\nDiscover practical ${tpc} strategies, templates, and step-by-step tips built for ${ctx}. Start creating faster today.`;

    case 'seo_keywords':
      return `Primary Keywords:\n• ${tpc}\n• ${tpc} for ${ctx}\n• best ${tpc} guide\n\nInformational Long-Tail:\n• how to start ${tpc} step by step\n• ${tpc} examples and templates 2026\n\nHigh-Intent:\n• ${tpc} tool online\n• ${tpc} checklist free download`;

    case 'marketing_email':
      return `Subject: Unlock faster ${tpc} this week 🚀\nPreview Text: Plus a ready-to-use checklist inside.\n\nHi [First Name],\n\nWhether you're launching something new or scaling ${tpc}, your workflow shouldn't slow you down.\n\nHere's what's inside for ${ctx}:\n✓ Ready-to-use templates\n✓ Clear 15-minute execution guide\n\n[CTA: Claim Your Template Now]`;

    case 'cta_generator':
      return `High-Converting CTAs for "${tpc}":\n• Start Free Now →\n• Grab Your ${tpc} Checklist\n• Try It in 30 Seconds\n• Unlock Full Access Today\n• Book Your Free Demo`;

    case 'image_prompt':
    case 'thumbnail_idea':
    case 'logo_prompt':
    case 'poster_prompt':
    case 'product_image_prompt':
    case 'social_image_prompt':
      return `[${tool.titleEn} — Structured Prompt]\nSubject: ${tpc}\nTarget Vibe: ${adj}, tailored for ${ctx}\nComposition: Clean focal subject, rule-of-thirds framing, crisp negative space for headline text\nLighting & Color: Deep navy & electric purple studio rim light, soft key light, high contrast\nCamera / Medium: ${tool.id === 'logo_prompt' ? 'Flat geometric vector emblem, scalable SVG aesthetic, minimal 2-color palette' : '35mm lens, f/2.0 shallow depth of field, ultra-sharp editorial photography'}\nAspect Ratio: ${tool.id === 'thumbnail_idea' ? '16:9 (1280x720) — Overlay Text: "3 SECRETS"' : tool.id === 'poster_prompt' ? '4:5 Portrait Poster' : '1:1 or 16:9'}`;

    case 'video_script':
    case 'youtube_outline':
      return `Video Title: Mastering ${tpc}\n\n[00:00 Hook] Show the end result of ${tpc} immediately.\n[00:30 Problem] Explain why ${ctx} get stuck.\n[01:30 Core Steps]\n  - Step 1: Simplify your setup\n  - Step 2: Use a ${adj} template\n  - Step 3: Publish and review metrics\n[04:30 Outro & CTA] Ask viewers to comment their biggest takeaway and subscribe.`;

    case 'reel_ideas':
    case 'shorts_ideas':
      return `10 Short Video Ideas for "${tpc}":\n1. "3 things I wish I knew before starting ${tpc}"\n2. Behind-the-scenes 30-second workflow\n3. Myth vs Reality on ${tpc}\n4. Before & After transformation\n5. Common rookie mistake + quick fix\n6. Free tool stack for ${ctx}\n7. 1-minute beginner challenge\n8. Answering a top comment question\n9. Honest budget breakdown\n10. Weekly checklist you can screenshot`;

    case 'storyboard_generator':
    case 'scene_description':
      return `Shot 1 (0-3s) | Medium close-up, warm desk lamp, creator points to screen | Text: "${tpc}" | Audio: Energetic intro hook\nShot 2 (3-12s) | Over-the-shoulder B-roll showing workflow steps | Text: "Step 1-2-3" | Audio: Crisp voiceover (${tone})\nShot 3 (12-22s) | Split-screen Before vs After result | Text: "Real Results" | Audio: Key takeaway for ${ctx}\nShot 4 (22-30s) | Direct-to-camera smile + subscribe pointer | Text: "Save & Share" | Audio: Clear CTA`;

    case 'invoice_text':
      return `INVOICE SUMMARY\nInvoice #: INV-${new Date().getFullYear()}-001\nDate: ${new Date().toLocaleDateString('en-IN')}\nService / Item: ${tpc}\nClient / Billed To: ${ctx}\n\nNotes & Payment Terms:\n• Subtotal: ₹[Amount]\n• GST (18% if applicable): ₹[GST]\n• Total Payable: ₹[Total]\n• Payment Mode: UPI / Bank Transfer within 7 days\n\nThank you for your business!`;

    case 'proposal_generator':
      return `PROJECT PROPOSAL: ${tpc}\nPrepared for: ${ctx}\n\n1. Executive Summary\nWe propose a structured engagement for ${tpc} designed with ${adj} execution.\n\n2. Scope of Work\n• Discovery & strategy alignment\n• Core delivery & milestone review\n• Handover documentation\n\n3. Timeline & Investment\nEstimated Delivery: 2–4 Weeks | Investment: ₹[Quote]`;

    case 'business_plan':
      return `LEAN BUSINESS PLAN: ${tpc}\n\n• Problem: ${ctx} lack a fast, affordable way to solve ${tpc}.\n• Solution: Streamlined product/service focused on speed & clarity.\n• Revenue Model: Monthly subscription (₹399/mo) + service packages.\n• Go-To-Market: Short-form Reels/Shorts, SEO guides, WhatsApp referrals.\n• 90-Day Goal: Validate 50 paying users and refine onboarding.`;

    case 'resume_generator':
      return `[YOUR NAME] | [Phone] | [Email] | [LinkedIn]\n\nPROFESSIONAL SUMMARY\nResults-oriented specialist in ${tpc}, helping ${ctx} achieve measurable improvements through structured execution.\n\nKEY IMPACT BULLETS\n• Led ${tpc} initiatives improving turnaround speed by 35%.\n• Collaborated cross-functionally to deliver clean, user-focused outcomes.\n• Built repeatable checklists and documentation for team onboarding.`;

    case 'cover_letter':
    case 'job_application':
      return `Subject: Application for ${tpc} Role — [Your Name]\n\nDear Hiring Manager,\n\nI am excited to apply for the ${tpc} opportunity. With hands-on experience supporting ${ctx}, I bring a ${adj} work ethic and proven execution skills.\n\nI would welcome the chance to discuss how I can contribute from day one. Thank you for your time.\n\nBest regards,\n[Your Name]`;

    case 'customer_reply':
      return `Hi [Customer Name],\n\nThank you for reaching out regarding ${tpc}. We completely understand your concern and are happy to help.\n\nWe have reviewed your request and will share an update within 24 hours. Please reply with your Order/Reference ID if you haven't already.\n\nWarm regards,\nSupport Team`;

    case 'whatsapp_business':
      return `Namaste [Name] ji 🙏\n\nQuick update from us regarding *${tpc}*!\n✅ Specially curated for ${ctx}\n✅ Fast support & transparent pricing\n\nReply *"INFO"* to get details or tap our catalog link below.`;

    case 'ai_brainstorming':
      return `10 Creative Angles for "${tpc}":\n1. Beginner starter kit\n2. 7-day challenge format\n3. Cost-saving comparison\n4. Real customer case study\n5. Interactive quiz or checklist\n6. Hinglish explainer series\n7. Common mistakes breakdown\n8. Template bundle giveaway\n9. Collaboration with micro-creators\n10. Live Q&A / AMA session`;

    case 'todo_generator':
      return `PRIORITIZED TO-DO LIST — ${tpc}\n\n[MUST DO TODAY]\n[ ] Define clear goal & deadline for ${tpc}\n[ ] Complete primary draft / core task (45 mins focus)\n\n[SHOULD DO]\n[ ] Review quality & format for ${ctx}\n[ ] Schedule follow-up or publish\n\n[NICE TO HAVE]\n[ ] Archive notes & plan next iteration`;

    case 'meeting_summary':
      return `MEETING SUMMARY — ${tpc}\n\nKey Discussion Points:\n• Reviewed current progress and blockers for ${ctx}.\n• Agreed on a ${adj} approach for the next sprint.\n\nDecisions & Action Items:\n1. Finalize ${tpc} outline — Owner: [Team Lead] (Due: Friday)\n2. Share assets & review metrics — Owner: [Creator] (Due: Monday)`;

    case 'notes_generator':
      return `STUDY & REVISION NOTES: ${tpc}\n\n• Core Concept: Essential principles of ${tpc} for ${ctx}.\n• Key Takeaways:\n  1. Break big topics into 3 simple building blocks.\n  2. Connect theory with a real-world example.\n• Quick Memory Cue: Focus on Clarity → Practice → Review.`;

    case 'presentation_outline':
      return `8-SLIDE PRESENTATION OUTLINE: ${tpc}\n\nSlide 1: Title & Hook\nSlide 2: The Core Problem for ${ctx}\nSlide 3: Why Now?\nSlide 4: Our ${tpc} Framework\nSlide 5: Live Example / Case Study\nSlide 6: Metrics & Impact\nSlide 7: Implementation Roadmap\nSlide 8: Q&A & Call-to-Action`;

    case 'faq_generator':
      return `FREQUENTLY ASKED QUESTIONS — ${tpc}\n\nQ1: Who is ${tpc} best suited for?\nA: It is built specifically for ${ctx} looking for fast, reliable results.\n\nQ2: How quickly can I get started?\nA: Within minutes—follow the 3-step checklist and customize to your needs.\n\nQ3: Can I use this in both English and Hindi?\nA: Yes! Content adapts seamlessly for bilingual audiences.`;

    case 'checklist_generator':
      return `LAUNCH & QUALITY CHECKLIST — ${tpc}\n\n[ ] Target audience (${ctx}) clearly defined\n[ ] Tone checked (${tone}) and free of typos\n[ ] Strong hook / opening headline added\n[ ] Visuals or thumbnail aligned\n[ ] Clear CTA and contact/link verified\n[ ] Final preview tested on mobile screen`;

    case 'content_calendar':
      return `7-DAY CREATOR CONTENT CALENDAR — ${tpc}\n\nMon | Blog / LinkedIn | Deep-dive guide for ${ctx}\nTue | Instagram Reel | 30s quick tip on ${tpc}\nWed | YouTube Short | Common mistake + fix\nThu | X / Twitter | 5-tweet mini playbook\nFri | Email / WhatsApp | Weekly resource & offer\nSat | Carousel Post | Step-by-step visual checklist\nSun | Community Poll | Audience Q&A for next week`;

    default:
      return `${tool.titleEn} — Draft for "${tpc}"\n\nAudience: ${ctx} | Tone: ${adj}\n\n1. Opening Hook: Start with a clear promise around ${tpc}.\n2. Core Value: Deliver 3 actionable takeaways.\n3. Closing CTA: Invite ${ctx} to save, reply, or take the next step.${extraDetail}`;
  }
}

function buildHindiOutput(tool, { topic, context, tone, length }) {
  const tpc = cleanText(topic);
  const ctx = cleanText(context) || 'क्रिएटर्स, छात्र और छोटे व्यवसाय';
  const adj = toneAdjective(tone, true);
  const tag = slugTag(tpc);
  const detail = length === 'long'
    ? `\n\nविस्तृत निष्पादन नोट्स:\n• लक्षित दर्शक: ${ctx}\n• शैली निर्देश: भाषा ${adj} रखें।\n• इस कंटेंट को रील, कैरोसेल और व्हाट्सएप ब्रॉडकास्ट में भी उपयोग करें।`
    : '';

  if (tool.id === 'text_summarizer') {
    const sum = summarizeLocally(tpc, length);
    return `सारांश (${sum.wordCount} शब्द → ${sum.points.length} मुख्य बिंदु):\n\n${sum.points.map((p, i) => `${i + 1}. ${p}`).join('\n')}`;
  }
  if (tool.id === 'grammar_checker') {
    const chk = grammarCheckLocally(tpc);
    return `सुधारा गया टेक्स्ट:\n${chk.fixed}\n\nकिए गए सुधार:\n${chk.issues.length ? chk.issues.map((x) => `• ${x}`).join('\n') : '• स्पेसिंग और विराम चिह्न सही पाए गए।'}`;
  }
  if (tool.id === 'hindi_to_english') {
    return buildEnglishOutput(tool, { topic, context, tone, length });
  }
  if (tool.id === 'english_to_hindi') {
    const res = translatePhraseLocal(tpc, 'en2hi');
    return `हिन्दी अनुवाद (डेमो रूल-आधारित सहायक):\n${res.out}\n\nनोट: पूर्ण न्यूरल अनुवाद के लिए सेटिंग्स में अपना AI बैकएंड एंडपॉइंट जोड़ें।`;
  }

  return `[${tool.titleHi} — डेमो टेम्पलेट ड्राफ्ट]\nविषय: ${tpc}\nलक्षित दर्शक: ${ctx} | टोन: ${adj}\n\n1. आकर्षक शुरुआत (Hook):\n"${tpc}" को सरल और असरदार बनाने का सबसे आसान तरीका जानें।\n\n2. मुख्य बिंदु (Core Content):\n• स्पष्ट लक्ष्य तय करें और अनावश्यक जटिलता हटाएं।\n• ${ctx} के लिए उपयोगी और व्यावहारिक उदाहरण जोड़ें।\n• भाषा को ${adj} और मोबाइल-पठनीय रखें।\n\n3. समापन व CTA:\nइस पोस्ट/ड्राफ्ट को सेव करें और अपने अगले प्रोजेक्ट में तुरंत लागू करें!\n\n#${tag} #BharatCreators #AIहाइलाइट्स${detail}`;
}

export function generateDemoTemplateContent(tool, params) {
  if (params.language === 'hi') {
    return buildHindiOutput(tool, params);
  }
  if (params.language === 'both') {
    const en = buildEnglishOutput(tool, params);
    const hi = buildHindiOutput(tool, params);
    return `=== ENGLISH ===\n${en}\n\n=== हिन्दी ===\n${hi}`;
  }
  return buildEnglishOutput(tool, params);
}
