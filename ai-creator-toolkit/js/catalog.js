export const CATEGORIES = [
  { id: 'writing', icon: '✍️', color: '#4F46E5', titleEn: 'AI Writing', titleHi: 'AI लेखन' },
  { id: 'social', icon: '📱', color: '#EC4899', titleEn: 'Social Media', titleHi: 'सोशल मीडिया' },
  { id: 'marketing', icon: '📣', color: '#7C3AED', titleEn: 'Marketing', titleHi: 'मार्केटिंग' },
  { id: 'image', icon: '🎨', color: '#0284C7', titleEn: 'AI Image Creator', titleHi: 'AI इमेज प्रॉम्प्ट' },
  { id: 'video', icon: '🎬', color: '#EA580C', titleEn: 'Video Creator', titleHi: 'वीडियो क्रिएटर' },
  { id: 'business', icon: '💼', color: '#0D9488', titleEn: 'Business Creator', titleHi: 'बिज़नेस क्रिएटर' },
  { id: 'productivity', icon: '⚡', color: '#6366F1', titleEn: 'Productivity', titleHi: 'प्रोडक्टिविटी' }
];

// All 64 creator tools across the 7 toolkits
export const TOOLS = [
  // 1. AI WRITING (14)
  { id: 'ai_blog_writer', cat: 'writing', tier: 'free', quick: true, titleEn: 'AI Blog Writer', titleHi: 'AI ब्लॉग राइटर', descEn: 'Structured SEO-ready blog posts with intro, headings & CTA', descHi: 'शीर्षक और निष्कर्ष के साथ SEO ब्लॉग पोस्ट' },
  { id: 'article_generator', cat: 'writing', tier: 'free', titleEn: 'Article Generator', titleHi: 'आर्टिकल जेनरेटर', descEn: 'Long-form editorial articles with clear sections', descHi: 'विस्तृत संपादकीय लेख तैयार करें' },
  { id: 'paragraph_generator', cat: 'writing', tier: 'free', titleEn: 'Paragraph Generator', titleHi: 'पैराग्राफ जेनरेटर', descEn: 'Crisp, cohesive topic paragraphs in seconds', descHi: 'किसी भी विषय पर सटीक अनुच्छेद लिखें' },
  { id: 'essay_writer', cat: 'writing', tier: 'free', titleEn: 'Essay Writer', titleHi: 'निबंध लेखक', descEn: 'Academic & competitive exam essays with thesis & conclusion', descHi: 'छात्रों और परीक्षाओं के लिए निबंध लेखन' },
  { id: 'story_generator', cat: 'writing', tier: 'free', titleEn: 'Story Generator', titleHi: 'कहानी जेनरेटर', descEn: 'Creative short stories with characters, conflict & twist', descHi: 'रोचक पात्रों और मोड़ के साथ कहानियाँ' },
  { id: 'email_writer', cat: 'writing', tier: 'free', titleEn: 'Email Writer', titleHi: 'ईमेल राइटर', descEn: 'Clear everyday emails with subject line & sign-off', descHi: 'विषय पंक्ति के साथ साफ़ ईमेल लिखें' },
  { id: 'professional_email', cat: 'writing', tier: 'pro', titleEn: 'Professional Email', titleHi: 'प्रोफेशनल ईमेल', descEn: 'Executive outreach, follow-up & client communication', descHi: 'कॉर्पोरेट और क्लाइंट ईमेल ड्राफ्ट' },
  { id: 'letter_writer', cat: 'writing', tier: 'free', titleEn: 'Letter Writer', titleHi: 'पत्र लेखक', descEn: 'Formal & informal letters with proper salutation format', descHi: 'औपचारिक और अनौपचारिक पत्र प्रारूप' },
  { id: 'rewrite_text', cat: 'writing', tier: 'free', titleEn: 'Rewrite Text', titleHi: 'टेक्स्ट रीराइट', descEn: 'Paraphrase, polish & restructure pasted text cleanly', descHi: 'मौजूदा टेक्स्ट को बेहतर रूप में दोबारा लिखें' },
  { id: 'text_summarizer', cat: 'writing', tier: 'free', titleEn: 'Text Summarizer', titleHi: 'टेक्स्ट सारांश', descEn: 'Extract key takeaways & concise bullet summaries', descHi: 'लंबे टेक्स्ट का संक्षिप्त सारांश निकालें' },
  { id: 'grammar_checker', cat: 'writing', tier: 'free', titleEn: 'Grammar Checker', titleHi: 'व्याकरण जांच', descEn: 'Spot spacing, punctuation, capitalization & clarity fixes', descHi: 'विराम चिह्न, स्पेसिंग और स्पष्टता सुधारें' },
  { id: 'hindi_to_english', cat: 'writing', tier: 'free', titleEn: 'Hindi to English', titleHi: 'हिन्दी से अंग्रेज़ी', descEn: 'Translate Hindi phrases & notes into clear English', descHi: 'हिन्दी वाक्यों को अंग्रेज़ी में अनुवाद करें' },
  { id: 'english_to_hindi', cat: 'writing', tier: 'free', titleEn: 'English to Hindi', titleHi: 'अंग्रेज़ी से हिन्दी', descEn: 'Translate English copy into natural Hindi', descHi: 'अंग्रेज़ी टेक्स्ट को सरल हिन्दी में बदलें' },
  { id: 'text_translator', cat: 'writing', tier: 'pro', titleEn: 'Text Translator', titleHi: 'टेक्स्ट अनुवादक', descEn: 'Multi-style EN / HI / Hinglish adaptation helper', descHi: 'EN / HI / Hinglish भाषा रूपांतरण सहायक' },

  // 2. SOCIAL MEDIA CREATOR (11)
  { id: 'instagram_caption', cat: 'social', tier: 'free', quick: true, titleEn: 'Instagram Caption Generator', titleHi: 'इंस्टाग्राम कैप्शन', descEn: 'Scroll-stopping IG captions with hook & hashtags', descHi: 'हुक और हैशटैग के साथ आकर्षक कैप्शन' },
  { id: 'instagram_bio', cat: 'social', tier: 'free', titleEn: 'Instagram Bio Generator', titleHi: 'इंस्टाग्राम बायो', descEn: 'Punchy 150-char creator & brand bio options', descHi: 'प्रोफ़ाइल के लिए सटीक क्रिएटर बायो' },
  { id: 'facebook_post', cat: 'social', tier: 'free', titleEn: 'Facebook Post Generator', titleHi: 'फेसबुक पोस्ट', descEn: 'Community-friendly Facebook updates & discussion prompts', descHi: 'एंगेजमेंट बढ़ाने वाली फेसबुक पोस्ट' },
  { id: 'youtube_description', cat: 'social', tier: 'free', titleEn: 'YouTube Description Generator', titleHi: 'यूट्यूब डिस्क्रिप्शन', descEn: 'SEO video descriptions with timestamps & links block', descHi: 'टाइमस्टैम्प और SEO के साथ वीडियो विवरण' },
  { id: 'youtube_title', cat: 'social', tier: 'free', quick: true, titleEn: 'YouTube Title Generator', titleHi: 'यूट्यूब टाइटल', descEn: 'High-CTR YouTube title variations for your video', descHi: 'अधिक क्लिक लाने वाले यूट्यूब शीर्षक' },
  { id: 'youtube_shorts_script', cat: 'social', tier: 'pro', titleEn: 'YouTube Shorts Script', titleHi: 'यूट्यूब शॉर्ट्स स्क्रिप्ट', descEn: '45-second vertical script paced for retention', descHi: '45 सेकंड की तेज़ यूट्यूब शॉर्ट्स स्क्रिप्ट' },
  { id: 'reel_script', cat: 'social', tier: 'free', quick: true, titleEn: 'Reel Script Generator', titleHi: 'रील स्क्रिप्ट जेनरेटर', descEn: 'Hook + visual cues + voiceover for Instagram Reels', descHi: 'हुक और सीन निर्देशों के साथ रील स्क्रिप्ट' },
  { id: 'viral_hook', cat: 'social', tier: 'free', titleEn: 'Viral Hook Generator', titleHi: 'वायरल हुक जेनरेटर', descEn: '10 opening 3-second hooks to stop the scroll', descHi: 'शुरुआती 3 सेकंड के 10 दमदार हुक' },
  { id: 'hashtag_generator', cat: 'social', tier: 'free', titleEn: 'Hashtag Generator', titleHi: 'हैशटैग जेनरेटर', descEn: 'Niche, broad & regional hashtag sets for reach', descHi: 'रीच बढ़ाने के लिए सटीक हैशटैग सेट' },
  { id: 'linkedin_post', cat: 'social', tier: 'pro', titleEn: 'LinkedIn Post Generator', titleHi: 'लिंक्डइन पोस्ट', descEn: 'Thought-leadership posts with clean line breaks', descHi: 'पेशेवर नेटवर्किंग और करियर पोस्ट' },
  { id: 'twitter_post', cat: 'social', tier: 'free', titleEn: 'X / Twitter Post Generator', titleHi: 'X / ट्विटर पोस्ट', descEn: 'Crisp single posts & 5-part thread starters', descHi: 'संक्षिप्त पोस्ट और 5-भाग थ्रेड ड्राफ्ट' },

  // 3. MARKETING TOOLS (10)
  { id: 'ad_copy', cat: 'marketing', tier: 'free', quick: true, titleEn: 'Ad Copy Generator', titleHi: 'विज्ञापन कॉपी', descEn: 'Primary text, headline & CTA for Meta/Google Ads', descHi: 'Meta और Google Ads के लिए हाई-कन्वर्टिंग कॉपी' },
  { id: 'product_description', cat: 'marketing', tier: 'free', titleEn: 'Product Description', titleHi: 'उत्पाद विवरण', descEn: 'Benefit-driven e-commerce & D2C product listings', descHi: 'ऑनलाइन स्टोर के लिए आकर्षक उत्पाद विवरण' },
  { id: 'sales_copy', cat: 'marketing', tier: 'pro', titleEn: 'Sales Copy', titleHi: 'सेल्स कॉपी', descEn: 'PAS & AIDA persuasion frameworks for offers', descHi: 'AIDA और PAS फ़्रेमवर्क पर आधारित सेल्स कॉपी' },
  { id: 'landing_page_copy', cat: 'marketing', tier: 'pro', titleEn: 'Landing Page Copy', titleHi: 'लैंडिंग पेज कॉपी', descEn: 'Hero, value props, social proof & FAQ copy blocks', descHi: 'वेबसाइट हीरो, फ़ायदे और CTA सेक्शन' },
  { id: 'business_name', cat: 'marketing', tier: 'free', titleEn: 'Business Name Generator', titleHi: 'बिज़नेस नाम जेनरेटर', descEn: 'Brandable startup & shop names with domain ideas', descHi: 'नए ब्रांड और स्टार्टअप के नाम सुझाव' },
  { id: 'slogan_generator', cat: 'marketing', tier: 'free', titleEn: 'Slogan Generator', titleHi: 'स्लोगन जेनरेटर', descEn: 'Memorable taglines & brand jingles in EN/HI', descHi: 'यादगार ब्रांड टैगलाइन और स्लोगन' },
  { id: 'seo_meta', cat: 'marketing', tier: 'free', titleEn: 'SEO Meta Description', titleHi: 'SEO मेटा विवरण', descEn: 'Character-counted SEO title tags & meta snippets', descHi: 'सटीक अक्षर सीमा में SEO टाइटल व मेटा' },
  { id: 'seo_keywords', cat: 'marketing', tier: 'pro', titleEn: 'SEO Keyword Ideas', titleHi: 'SEO कीवर्ड सुझाव', descEn: 'Long-tail, informational & buyer-intent keyword clusters', descHi: 'लॉन्ग-टेल और बायर-इंटेंट कीवर्ड समूह' },
  { id: 'marketing_email', cat: 'marketing', tier: 'pro', titleEn: 'Marketing Email', titleHi: 'मार्केटिंग ईमेल', descEn: 'Product launch, festive offer & newsletter campaigns', descHi: 'ऑफ़र, लॉन्च और न्यूज़लेटर कैंपेन ईमेल' },
  { id: 'cta_generator', cat: 'marketing', tier: 'free', titleEn: 'Call-to-Action Generator', titleHi: 'कॉल-टू-एक्शन (CTA)', descEn: 'High-intent buttons & closing action lines', descHi: 'क्लिक और रूपांतरण बढ़ाने वाले CTA' },

  // 4. AI IMAGE CREATOR (6)
  { id: 'image_prompt', cat: 'image', tier: 'free', quick: true, titleEn: 'Image Prompt Generator', titleHi: 'इमेज प्रॉम्प्ट जेनरेटर', descEn: 'Detailed lighting, lens, style & aspect ratio prompts', descHi: 'लाइटिंग, कैमरा और स्टाइल के साथ इमेज प्रॉम्प्ट' },
  { id: 'thumbnail_idea', cat: 'image', tier: 'free', titleEn: 'Thumbnail Idea Generator', titleHi: 'थंबनेल आइडिया', descEn: 'YouTube thumbnail compositions, colors & 3-word text', descHi: 'यूट्यूब थंबनेल लेआउट और टेक्स्ट आइडिया' },
  { id: 'logo_prompt', cat: 'image', tier: 'pro', titleEn: 'Logo Prompt Generator', titleHi: 'लोगो प्रॉम्प्ट जेनरेटर', descEn: 'Vector minimalist emblem & typography prompt specs', descHi: 'मॉडर्न मिनिमल लोगो डिज़ाइन प्रॉम्प्ट' },
  { id: 'poster_prompt', cat: 'image', tier: 'pro', titleEn: 'Poster Prompt Generator', titleHi: 'पोस्टर प्रॉम्प्ट जेनरेटर', descEn: 'Event, sale & festival poster visual direction prompts', descHi: 'इवेंट, सेल और त्योहार पोस्टर डिज़ाइन प्रॉम्प्ट' },
  { id: 'product_image_prompt', cat: 'image', tier: 'pro', titleEn: 'Product Image Prompt', titleHi: 'प्रोडक्ट फ़ोटो प्रॉम्प्ट', descEn: 'Studio tabletop, rim lighting & lifestyle scene prompts', descHi: 'ई-कॉमर्स स्टूडियो प्रोडक्ट फ़ोटोग्राफ़ी प्रॉम्प्ट' },
  { id: 'social_image_prompt', cat: 'image', tier: 'free', titleEn: 'Social Media Image Prompt', titleHi: 'सोशल मीडिया इमेज प्रॉम्प्ट', descEn: 'Carousel cover & quote graphic visual prompts', descHi: 'इंस्टाग्राम कैरोसेल व सोशल ग्राफ़िक प्रॉम्प्ट' },

  // 5. VIDEO CREATOR (7)
  { id: 'video_script', cat: 'video', tier: 'free', titleEn: 'Video Script Generator', titleHi: 'वीडियो स्क्रिप्ट जेनरेटर', descEn: 'Full narrative script with hook, body & outro', descHi: 'हुक, मुख्य भाग और आउटरों के साथ पूरी स्क्रिप्ट' },
  { id: 'short_video_script', cat: 'video', tier: 'free', titleEn: 'Short Video Script', titleHi: 'शॉर्ट वीडियो स्क्रिप्ट', descEn: '30–60 sec fast-paced vertical video script', descHi: '30–60 सेकंड की तेज़ वर्टिकल स्क्रिप्ट' },
  { id: 'youtube_outline', cat: 'video', tier: 'free', titleEn: 'YouTube Video Outline', titleHi: 'यूट्यूब वीडियो आउटलाइन', descEn: 'Chapter-by-chapter talking points & B-roll cues', descHi: 'चैप्टर और B-roll संकेतों के साथ वीडियो रूपरेखा' },
  { id: 'reel_ideas', cat: 'video', tier: 'free', titleEn: 'Reel Ideas', titleHi: 'रील आइडियाज़', descEn: '10 ready-to-shoot Instagram Reel concepts', descHi: 'शूट करने के लिए तैयार 10 रील कॉन्सेप्ट' },
  { id: 'shorts_ideas', cat: 'video', tier: 'free', titleEn: 'Shorts Ideas', titleHi: 'शॉर्ट्स आइडियाज़', descEn: '10 daily YouTube Shorts angles for your niche', descHi: 'आपकी श्रेणी के 10 दैनिक यूट्यूब शॉर्ट्स आइडिया' },
  { id: 'storyboard_generator', cat: 'video', tier: 'pro', titleEn: 'Storyboard Generator', titleHi: 'स्टोरीबोर्ड जेनरेटर', descEn: 'Shot-by-shot framing, audio & on-screen text table', descHi: 'शॉट-दर-शॉट विजुअल, ऑडियो व टेक्स्ट स्टोरीबोर्ड' },
  { id: 'scene_description', cat: 'video', tier: 'pro', titleEn: 'Scene Description Generator', titleHi: 'सीन विवरण जेनरेटर', descEn: 'Cinematic camera movement, mood & lighting cues', descHi: 'सिनेमैटिक कैमरा एंगल, मूड व लाइटिंग विवरण' },

  // 6. BUSINESS CREATOR (8)
  { id: 'invoice_text', cat: 'business', tier: 'free', titleEn: 'Invoice Text Generator', titleHi: 'इनवॉइस टेक्स्ट जेनरेटर', descEn: 'Clean GST-ready invoice summary & payment note', descHi: 'GST व भुगतान शर्तों सहित इनवॉइस विवरण' },
  { id: 'proposal_generator', cat: 'business', tier: 'pro', titleEn: 'Proposal Generator', titleHi: 'प्रपोज़ल जेनरेटर', descEn: 'Client project proposal with scope, timeline & pricing', descHi: 'कार्यक्षेत्र, समयसीमा और शुल्क के साथ क्लाइंट प्रपोज़ल' },
  { id: 'business_plan', cat: 'business', tier: 'pro', titleEn: 'Business Plan Generator', titleHi: 'बिज़नेस प्लान जेनरेटर', descEn: 'Lean executive summary, revenue & GTM plan', descHi: 'स्टार्टअप व व्यवसाय के लिए संक्षिप्त बिज़नेस प्लान' },
  { id: 'resume_generator', cat: 'business', tier: 'pro', titleEn: 'Resume Generator', titleHi: 'रिज्यूमे जेनरेटर', descEn: 'ATS-friendly resume summary & impact bullet points', descHi: 'ATS-अनुकूल रिज्यूमे सारांश और अनुभव बुलेट्स' },
  { id: 'cover_letter', cat: 'business', tier: 'free', titleEn: 'Cover Letter Generator', titleHi: 'कवर लेटर जेनरेटर', descEn: 'Tailored role-specific job cover letters', descHi: 'नौकरी के लिए सटीक कवर लेटर ड्राफ्ट' },
  { id: 'job_application', cat: 'business', tier: 'free', titleEn: 'Job Application Writer', titleHi: 'नौकरी आवेदन पत्र', descEn: 'Direct recruiter email / application message', descHi: 'भर्ती प्रबंधक के लिए नौकरी आवेदन संदेश' },
  { id: 'customer_reply', cat: 'business', tier: 'free', titleEn: 'Customer Reply Generator', titleHi: 'ग्राहक उत्तर जेनरेटर', descEn: 'Polite support, refund & order status responses', descHi: 'ग्राहक सहायता, ऑर्डर और रिफंड हेतु विनम्र उत्तर' },
  { id: 'whatsapp_business', cat: 'business', tier: 'free', titleEn: 'WhatsApp Business Message', titleHi: 'व्हाट्सएप बिज़नेस संदेश', descEn: 'Catalog, follow-up & festive broadcast templates', descHi: 'ग्राहक फॉलो-अप, कैटलॉग व ऑफ़र व्हाट्सएप संदेश' },

  // 7. PRODUCTIVITY (8)
  { id: 'ai_brainstorming', cat: 'productivity', tier: 'free', titleEn: 'AI Brainstorming', titleHi: 'AI ब्रेनस्टॉर्मिंग', descEn: '10 structured angles, experiments & quick wins', descHi: 'किसी समस्या या लक्ष्य पर 10 रचनात्मक विचार' },
  { id: 'todo_generator', cat: 'productivity', tier: 'free', titleEn: 'To-Do Generator', titleHi: 'टू-डू सूची जेनरेटर', descEn: 'Prioritized Must-Do / Should-Do daily action list', descHi: 'प्राथमिकता के अनुसार दैनिक कार्य सूची' },
  { id: 'meeting_summary', cat: 'productivity', tier: 'pro', titleEn: 'Meeting Summary', titleHi: 'मीटिंग सारांश', descEn: 'Convert rough meeting notes into decisions & owners', descHi: 'मीटिंग नोट्स से निर्णय और एक्शन आइटम निकालें' },
  { id: 'notes_generator', cat: 'productivity', tier: 'free', titleEn: 'Notes Generator', titleHi: 'नोट्स जेनरेटर', descEn: 'Clean study & revision notes with key definitions', descHi: 'अध्ययन और रिवीजन के लिए साफ़ नोट्स' },
  { id: 'presentation_outline', cat: 'productivity', tier: 'pro', titleEn: 'Presentation Outline', titleHi: 'प्रेजेंटेशन आउटलाइन', descEn: '8-slide pitch or seminar deck structure', descHi: '8-स्लाइड प्रेजेंटेशन डेक की रूपरेखा' },
  { id: 'faq_generator', cat: 'productivity', tier: 'free', titleEn: 'FAQ Generator', titleHi: 'FAQ जेनरेटर', descEn: 'Top customer questions & clear answers for pages', descHi: 'वेबसाइट और उत्पादों के लिए सामान्य प्रश्न-उत्तर' },
  { id: 'checklist_generator', cat: 'productivity', tier: 'free', titleEn: 'Checklist Generator', titleHi: 'चेकलिस्ट जेनरेटर', descEn: 'Step-by-step launch, shoot & quality checklists', descHi: 'लॉन्च, शूट और कार्य की स्टेप-बाय-स्टेप चेकलिस्ट' },
  { id: 'content_calendar', cat: 'productivity', tier: 'pro', titleEn: 'Content Calendar', titleHi: 'कंटेंट कैलेंडर', descEn: '7-day multi-platform creator publishing schedule', descHi: '7 दिनों की मल्टी-प्लेटफ़ॉर्म पब्लिशिंग योजना' }
];

export function getToolById(id) {
  return TOOLS.find((t) => t.id === id) || null;
}
