export const CATEGORIES = [
  { id: 'pdf', titleEn: 'PDF Toolkit', titleHi: 'PDF टूलकिट', color: '#DC2626', icon: 'pdf' },
  { id: 'image', titleEn: 'Image Toolkit', titleHi: 'इमेज टूलकिट', color: '#2563EB', icon: 'image' },
  { id: 'text', titleEn: 'Text Toolkit', titleHi: 'टेक्स्ट टूलकिट', color: '#1E3A8A', icon: 'text' },
  { id: 'qr', titleEn: 'QR Code Toolkit', titleHi: 'QR कोड टूलकिट', color: '#7C3AED', icon: 'qr' },
  { id: 'utility', titleEn: 'Calculators & Productivity', titleHi: 'कैलकुलेटर व उपयोगिताएं', color: '#0D9488', icon: 'calc' }
];

export const TOOL_CATALOG = [
  // A. PDF TOOLKIT (13)
  { id: 'pdf_merge', category: 'pdf', titleEn: 'PDF Merge', titleHi: 'PDF मर्ज', descEn: 'Combine multiple PDFs with drag/order controls', descHi: 'कई PDF फ़ाइलों को क्रम में जोड़ें', popular: true, proOnly: false },
  { id: 'pdf_split', category: 'pdf', titleEn: 'PDF Split', titleHi: 'PDF विभाजन', descEn: 'Split a PDF into separate single-page files', descHi: 'PDF को अलग-अलग पेजों में विभाजित करें', popular: false, proOnly: false },
  { id: 'pdf_compress', category: 'pdf', titleEn: 'PDF Compress', titleHi: 'PDF कंप्रेस', descEn: 'Optimize PDF streams cleanly in-browser', descHi: 'ब्राउज़र में PDF आकार कम करें', popular: true, proOnly: false },
  { id: 'pdf_to_image', category: 'pdf', titleEn: 'PDF to Image', titleHi: 'PDF से इमेज', descEn: 'Render PDF pages and snapshot to PNG/JPG', descHi: 'PDF पेजों को इमेज में बदलें', popular: false, proOnly: false },
  { id: 'image_to_pdf', category: 'pdf', titleEn: 'Image to PDF', titleHi: 'इमेज से PDF', descEn: 'Convert photos into a crisp A4 PDF offline', descHi: 'फ़ोटो को A4 PDF में बदलें', popular: true, proOnly: false },
  { id: 'pdf_rotate', category: 'pdf', titleEn: 'PDF Page Rotation', titleHi: 'PDF पेज रोटेट', descEn: 'Rotate PDF pages 90°, 180°, or 270°', descHi: 'पेजों को 90°, 180° या 270° घुमाएँ', popular: false, proOnly: false },
  { id: 'pdf_extract', category: 'pdf', titleEn: 'PDF Page Extraction', titleHi: 'पेज निकालें', descEn: 'Extract selected page ranges (e.g. 1,3-5)', descHi: 'चुनिंदा पेज अलग करें', popular: false, proOnly: false },
  { id: 'pdf_reorder', category: 'pdf', titleEn: 'PDF Page Reordering', titleHi: 'पेज क्रम बदलें', descEn: 'Rearrange PDF pages in custom order', descHi: 'PDF पेजों को नए क्रम में लगाएं', popular: false, proOnly: false },
  { id: 'pdf_watermark', category: 'pdf', titleEn: 'PDF Watermark', titleHi: 'PDF वॉटरमार्क', descEn: 'Stamp diagonal watermark text onto pages', descHi: 'पेजों पर वॉटरमार्क जोड़ें', popular: false, proOnly: false },
  { id: 'pdf_protect', category: 'pdf', titleEn: 'PDF Password Protect', titleHi: 'PDF पासवर्ड सुरक्षा', descEn: 'Subscriber password protection workflow', descHi: 'सदस्यता PDF पासवर्ड सुरक्षा', popular: false, proOnly: true },
  { id: 'pdf_unlock', category: 'pdf', titleEn: 'PDF Unlock', titleHi: 'PDF अनलॉक', descEn: 'Authorized subscriber PDF unlock', descHi: 'अधिकृत PDF अनलॉक', popular: false, proOnly: true },
  { id: 'pdf_preview', category: 'pdf', titleEn: 'PDF Preview', titleHi: 'PDF प्रीव्यू', descEn: 'Inspect PDF metadata & view inline', descHi: 'PDF मेटाडेटा और प्रीव्यू देखें', popular: false, proOnly: false },
  { id: 'pdf_download', category: 'pdf', titleEn: 'PDF Export', titleHi: 'PDF डाउनलोड', descEn: 'Export processed PDF cleanly', descHi: 'तैयार PDF डाउनलोड करें', popular: false, proOnly: false },

  // B. IMAGE TOOLKIT (10)
  { id: 'img_compress', category: 'image', titleEn: 'Image Compressor', titleHi: 'इमेज कंप्रेसर', descEn: 'Shrink image file size with quality slider', descHi: 'क्वालिटी नियंत्रण के साथ आकार घटाएं', popular: true, proOnly: false },
  { id: 'img_resize', category: 'image', titleEn: 'Image Resizer', titleHi: 'इमेज रीसाइज़र', descEn: 'Resize pixel dimensions cleanly', descHi: 'पिक्सेल लंबाई-चौड़ाई बदलें', popular: false, proOnly: false },
  { id: 'img_crop', category: 'image', titleEn: 'Image Cropper', titleHi: 'इमेज क्रॉपर', descEn: 'Crop to 1:1, 4:3, 16:9, or 3:4 aspect', descHi: '1:1, 4:3, 16:9 या 3:4 क्रॉप करें', popular: false, proOnly: false },
  { id: 'img_jpg_to_png', category: 'image', titleEn: 'JPG to PNG', titleHi: 'JPG से PNG', descEn: 'Convert JPEG images to lossless PNG', descHi: 'JPG को लॉसलेस PNG में बदलें', popular: false, proOnly: false },
  { id: 'img_png_to_jpg', category: 'image', titleEn: 'PNG to JPG', titleHi: 'PNG से JPG', descEn: 'Convert PNG to compact JPG', descHi: 'PNG को कॉम्पैक्ट JPG में बदलें', popular: false, proOnly: false },
  { id: 'img_webp', category: 'image', titleEn: 'WebP Converter', titleHi: 'WebP कन्वर्टर', descEn: 'Convert images to modern WebP format', descHi: 'आधुनिक WebP फ़ॉर्मेट में बदलें', popular: false, proOnly: false },
  { id: 'img_to_pdf_b', category: 'image', titleEn: 'Image to PDF', titleHi: 'इमेज से PDF', descEn: 'Export photo directly as an A4 PDF', descHi: 'फ़ोटो से सीधे PDF बनाएं', popular: false, proOnly: false },
  { id: 'img_bg_util', category: 'image', titleEn: 'Background Utility', titleHi: 'बैकग्राउंड टूल', descEn: 'Color-key backdrop removal & passport fill', descHi: 'बैकग्राउंड हटाएं या रंग बदलें', popular: false, proOnly: true },
  { id: 'img_rotate', category: 'image', titleEn: 'Image Rotate & Flip', titleHi: 'इमेज रोटेट', descEn: 'Rotate 90°/180°/270° & mirror H/V', descHi: '90°/180°/270° घुमाएं व फ्लिप करें', popular: false, proOnly: false },
  { id: 'img_inspect', category: 'image', titleEn: 'Quality & Dimensions', titleHi: 'गुणवत्ता और आकार', descEn: 'Inspect width, height, MP, ratio & bytes', descHi: 'रिज़ॉल्यूशन, MP और आकार जांचें', popular: false, proOnly: false },

  // C. TEXT TOOLKIT (13)
  { id: 'txt_word_count', category: 'text', titleEn: 'Word Counter', titleHi: 'शब्द गिनती', descEn: 'Count words, sentences & reading time', descHi: 'शब्द, वाक्य और पढ़ने का समय गिनें', popular: true, proOnly: false },
  { id: 'txt_char_count', category: 'text', titleEn: 'Character Counter', titleHi: 'अक्षर गिनती', descEn: 'Characters with & without spaces', descHi: 'स्पेस सहित व रहित अक्षर गिनें', popular: false, proOnly: false },
  { id: 'txt_case', category: 'text', titleEn: 'Case Converter', titleHi: 'केस कन्वर्टर', descEn: 'Sentence, Toggle & Title case converter', descHi: 'Sentence, Toggle व Title केस', popular: false, proOnly: false },
  { id: 'txt_upper', category: 'text', titleEn: 'Uppercase Converter', titleHi: 'अपरकेस', descEn: 'Convert text to ALL CAPS', descHi: 'सभी अक्षरों को CAPITAL में बदलें', popular: false, proOnly: false },
  { id: 'txt_lower', category: 'text', titleEn: 'Lowercase Converter', titleHi: 'लोअरकेस', descEn: 'Convert text to lowercase', descHi: 'सभी अक्षरों को lowercase में बदलें', popular: false, proOnly: false },
  { id: 'txt_title', category: 'text', titleEn: 'Title Case', titleHi: 'टाइटल केस', descEn: 'Capitalize Every Word Cleanly', descHi: 'प्रत्येक शब्द का पहला अक्षर बड़ा करें', popular: false, proOnly: false },
  { id: 'txt_spaces', category: 'text', titleEn: 'Remove Extra Spaces', titleHi: 'अतिरिक्त स्पेस हटाएं', descEn: 'Collapse duplicate spaces & blank lines', descHi: 'अतिरिक्त स्पेस व खाली लाइन हटाएं', popular: false, proOnly: false },
  { id: 'txt_reverse', category: 'text', titleEn: 'Text Reverser', titleHi: 'टेक्स्ट रिवर्सर', descEn: 'Reverse character or word order', descHi: 'अक्षरों या शब्दों का क्रम उलटें', popular: false, proOnly: false },
  { id: 'txt_clean', category: 'text', titleEn: 'Text Cleaner', titleHi: 'टेक्स्ट क्लीनर', descEn: 'Strip HTML tags & noise characters', descHi: 'HTML टैग व अवांछित चिह्न हटाएं', popular: false, proOnly: false },
  { id: 'txt_compare', category: 'text', titleEn: 'Text Compare', titleHi: 'टेक्स्ट तुलना', descEn: 'Line-by-line diff comparator', descHi: 'पंक्ति-दर-पंक्ति अंतर जांचें', popular: false, proOnly: false },
  { id: 'txt_dedup', category: 'text', titleEn: 'Duplicate Line Remover', titleHi: 'डुप्लिकेट लाइन हटाएं', descEn: 'Keep unique lines & sort alphabetically', descHi: 'डुप्लिकेट लाइन हटाएं और क्रमबद्ध करें', popular: false, proOnly: false },
  { id: 'txt_tts', category: 'text', titleEn: 'Text-to-Speech (EN/HI)', titleHi: 'टेक्स्ट-टू-स्पीच', descEn: 'Speak text aloud in English or Hindi', descHi: 'अंग्रेज़ी या हिंदी में सुनें', popular: true, proOnly: false },
  { id: 'txt_format', category: 'text', titleEn: 'Text Formatter', titleHi: 'टेक्स्ट फ़ॉर्मेटर', descEn: 'Bullets, numbering & JSON pretty-print', descHi: 'बुलेट्स, नंबरिंग व JSON फ़ॉर्मेट', popular: false, proOnly: false },

  // D. QR CODE TOOLKIT (10)
  { id: 'qr_url', category: 'qr', titleEn: 'URL QR Generator', titleHi: 'लिंक QR कोड', descEn: 'Create scannable website link QR', descHi: 'वेबसाइट लिंक QR कोड बनाएं', popular: true, proOnly: false },
  { id: 'qr_text', category: 'qr', titleEn: 'Text QR Generator', titleHi: 'टेक्स्ट QR कोड', descEn: 'Encode plain text or notes into QR', descHi: 'टेक्स्ट या नोट्स का QR बनाएं', popular: false, proOnly: false },
  { id: 'qr_phone', category: 'qr', titleEn: 'Phone QR Generator', titleHi: 'फ़ोन QR कोड', descEn: 'One-tap call dialer QR code', descHi: 'एक-टैप कॉल QR कोड', popular: false, proOnly: false },
  { id: 'qr_wifi', category: 'qr', titleEn: 'Wi-Fi QR Generator', titleHi: 'वाई-फाई QR कोड', descEn: 'Instant Wi-Fi guest login QR', descHi: 'तुरंत वाई-फाई कनेक्ट QR', popular: false, proOnly: false },
  { id: 'qr_email', category: 'qr', titleEn: 'Email QR Generator', titleHi: 'ईमेल QR कोड', descEn: 'Pre-filled email address & subject QR', descHi: 'विषय सहित ईमेल QR कोड', popular: false, proOnly: false },
  { id: 'qr_whatsapp', category: 'qr', titleEn: 'WhatsApp QR Generator', titleHi: 'व्हाट्सएप QR कोड', descEn: 'Direct WhatsApp chat link QR', descHi: 'सीधा व्हाट्सएप चैट QR', popular: false, proOnly: false },
  { id: 'qr_upi', category: 'qr', titleEn: 'UPI ₹ QR Generator', titleHi: 'UPI भुगतान QR', descEn: 'Generate BHIM/UPI QR from VPA & ₹', descHi: 'UPI ID व ₹ राशि का भुगतान QR', popular: true, proOnly: false },
  { id: 'qr_color', category: 'qr', titleEn: 'QR Color Customizer', titleHi: 'QR रंग कस्टमाइज़र', descEn: 'Customize foreground & background tint', descHi: 'QR का रंग कस्टमाइज़ करें', popular: false, proOnly: false },
  { id: 'qr_png', category: 'qr', titleEn: 'QR Download PNG', titleHi: 'PNG डाउनलोड', descEn: 'Export crisp high-res PNG QR', descHi: 'उच्च-गुणवत्ता PNG डाउनलोड', popular: false, proOnly: false },
  { id: 'qr_svg', category: 'qr', titleEn: 'QR Download SVG', titleHi: 'SVG वेक्टर डाउनलोड', descEn: 'Export scalable SVG vector QR', descHi: 'स्केलेबल SVG वेक्टर डाउनलोड', popular: false, proOnly: true },

  // E. CALCULATORS & PRODUCTIVITY (14)
  { id: 'util_calc', category: 'utility', titleEn: 'Calculator', titleHi: 'कैलकुलेटर', descEn: 'Fast arithmetic expression evaluator', descHi: 'तेज़ और सटीक कैलकुलेटर', popular: false, proOnly: false },
  { id: 'util_age', category: 'utility', titleEn: 'Age Calculator', titleHi: 'आयु कैलकुलेटर', descEn: 'Exact years, months & days from DOB', descHi: 'जन्म तिथि से सटीक उम्र निकालें', popular: false, proOnly: false },
  { id: 'util_datediff', category: 'utility', titleEn: 'Date Difference', titleHi: 'तिथि अंतर', descEn: 'Exact days & weeks between two dates', descHi: 'दो तारीखों के बीच दिन व सप्ताह', popular: false, proOnly: false },
  { id: 'util_unit', category: 'utility', titleEn: 'Unit Converter', titleHi: 'यूनिट कन्वर्टर', descEn: 'Length, weight, area (Gaj/Bigha) & data', descHi: 'लंबाई, वजन, गज/बीघा व डेटा', popular: false, proOnly: false },
  { id: 'util_percent', category: 'utility', titleEn: 'Percentage Calculator', titleHi: 'प्रतिशत कैलकुलेटर', descEn: '% of number, discount & % change', descHi: 'प्रतिशत, छूट व बदलाव गणना', popular: false, proOnly: false },
  { id: 'util_bmi', category: 'utility', titleEn: 'BMI Calculator', titleHi: 'BMI कैलकुलेटर', descEn: 'Informational Body Mass Index check', descHi: 'जानकारी हेतु BMI जांच', popular: false, proOnly: false },
  { id: 'util_gst', category: 'utility', titleEn: 'GST Calculator', titleHi: 'GST कैलकुलेटर', descEn: '5/12/18/28% inclusive & CGST/SGST', descHi: '5/12/18/28% CGST व SGST गणना', popular: true, proOnly: false },
  { id: 'util_emi', category: 'utility', titleEn: 'EMI Calculator', titleHi: 'EMI कैलकुलेटर', descEn: 'Monthly loan EMI & interest split', descHi: 'मासिक लोन किस्त व ब्याज विवरण', popular: true, proOnly: false },
  { id: 'util_currency', category: 'utility', titleEn: 'Currency Converter', titleHi: 'मुद्रा कन्वर्टर', descEn: 'Live ECB FX rates (INR, USD, EUR...)', descHi: 'लाइव विदेशी मुद्रा दरें', popular: false, proOnly: false },
  { id: 'util_time', category: 'utility', titleEn: 'Time Zone Converter', titleHi: 'समय क्षेत्र कन्वर्टर', descEn: 'IST, UTC, EST, GST, SGT, JST & GMT', descHi: 'IST से वैश्विक समय क्षेत्र', popular: false, proOnly: false },
  { id: 'util_password', category: 'utility', titleEn: 'Password Generator', titleHi: 'पासवर्ड जेनरेटर', descEn: 'Web Crypto CSPRNG password builder', descHi: 'मज़बूत और सुरक्षित पासवर्ड', popular: false, proOnly: false },
  { id: 'util_random', category: 'utility', titleEn: 'Random Number Gen', titleHi: 'रैंडम नंबर', descEn: 'Generate cryptographically random numbers', descHi: 'किसी भी रेंज में रैंडम नंबर', popular: false, proOnly: false },
  { id: 'util_stopwatch', category: 'utility', titleEn: 'Stopwatch', titleHi: 'स्टॉपवॉच', descEn: 'Millisecond stopwatch with lap splits', descHi: 'लैप रिकॉर्ड के साथ स्टॉपवॉच', popular: false, proOnly: false },
  { id: 'util_timer', category: 'utility', titleEn: 'Countdown Timer', titleHi: 'काउंटडाउन टाइमर', descEn: 'Preset & custom countdown timer', descHi: 'प्रीसेट व कस्टम टाइमर', popular: false, proOnly: false }
];

export function getToolById(id) {
  return TOOL_CATALOG.find((t) => t.id === id) || null;
}
