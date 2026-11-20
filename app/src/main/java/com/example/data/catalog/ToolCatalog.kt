package com.example.data.catalog

import com.example.data.model.ToolCategory
import com.example.data.model.ToolDefinition

object ToolCatalog {
    val allTools: List<ToolDefinition> = listOf(
        // A. PDF TOOLKIT (13 tools)
        ToolDefinition("pdf_merge", ToolCategory.PDF, "PDF Merge", "PDF मर्ज", "Combine multiple PDFs into one file", "कई PDF फ़ाइलों को एक में जोड़ें", popular = true, initialSubTab = 0),
        ToolDefinition("pdf_split", ToolCategory.PDF, "PDF Split", "PDF विभाजन", "Split PDF into separate page files", "PDF को अलग-अलग पेजों में विभाजित करें", initialSubTab = 1),
        ToolDefinition("pdf_compress", ToolCategory.PDF, "PDF Compress", "PDF कंप्रेस", "Reduce PDF file size cleanly", "PDF फ़ाइल का आकार कम करें", popular = true, initialSubTab = 2),
        ToolDefinition("pdf_to_image", ToolCategory.PDF, "PDF to Image", "PDF से इमेज", "Export PDF pages as crisp PNG/JPG", "PDF पेजों को PNG/JPG इमेज में बदलें", initialSubTab = 3),
        ToolDefinition("image_to_pdf", ToolCategory.PDF, "Image to PDF", "इमेज से PDF", "Convert photos into an A4 PDF", "फ़ोटो को A4 PDF में बदलें", popular = true, initialSubTab = 4),
        ToolDefinition("pdf_rotate", ToolCategory.PDF, "PDF Page Rotation", "PDF पेज रोटेट", "Rotate pages 90°, 180°, or 270°", "पेजों को 90°, 180° या 270° घुमाएँ", initialSubTab = 5),
        ToolDefinition("pdf_extract", ToolCategory.PDF, "PDF Page Extraction", "पेज निकालें", "Extract specific page ranges", "चुनिंदा पेज रेंज को अलग करें", initialSubTab = 6),
        ToolDefinition("pdf_reorder", ToolCategory.PDF, "PDF Page Reordering", "पेज क्रम बदलें", "Rearrange PDF pages in custom order", "PDF पेजों को नए क्रम में व्यवस्थित करें", initialSubTab = 7),
        ToolDefinition("pdf_watermark", ToolCategory.PDF, "PDF Watermark", "PDF वॉटरमार्क", "Stamp diagonal watermark text", "सभी पेजों पर वॉटरमार्क जोड़ें", initialSubTab = 8),
        ToolDefinition("pdf_protect", ToolCategory.PDF, "PDF Password Protect", "PDF पासवर्ड सुरक्षा", "Encrypt PDF with a user password", "PDF को पासवर्ड से सुरक्षित करें", proOnly = true, initialSubTab = 9),
        ToolDefinition("pdf_unlock", ToolCategory.PDF, "PDF Unlock", "PDF अनलॉक", "Remove password from authorized PDFs", "अधिकृत PDF से पासवर्ड हटाएं", proOnly = true, initialSubTab = 10),
        ToolDefinition("pdf_preview", ToolCategory.PDF, "PDF Preview", "PDF प्रीव्यू", "Inspect PDF pages in-app", "ऐप के अंदर PDF पेज देखें", initialSubTab = 11),
        ToolDefinition("pdf_download", ToolCategory.PDF, "PDF Export & Share", "PDF सेव व शेयर", "Save or share processed PDF", "तैयार PDF को सेव या शेयर करें", initialSubTab = 12),

        // B. IMAGE TOOLKIT (10 tools)
        ToolDefinition("img_compress", ToolCategory.IMAGE, "Image Compressor", "इमेज कंप्रेसर", "Shrink image size with quality slider", "क्वालिटी नियंत्रण के साथ आकार घटाएं", popular = true, initialSubTab = 0),
        ToolDefinition("img_resize", ToolCategory.IMAGE, "Image Resizer", "इमेज रीसाइज़र", "Resize width & height with ratio lock", "सही अनुपात में लंबाई-चौड़ाई बदलें", initialSubTab = 1),
        ToolDefinition("img_crop", ToolCategory.IMAGE, "Image Cropper", "इमेज क्रॉपर", "Crop to 1:1, 4:3, 16:9, or custom", "1:1, 4:3, 16:9 या कस्टम क्रॉप करें", initialSubTab = 2),
        ToolDefinition("img_jpg_to_png", ToolCategory.IMAGE, "JPG to PNG", "JPG से PNG", "Lossless PNG format conversion", "लॉसलेस PNG फ़ॉर्मेट में बदलें", initialSubTab = 3),
        ToolDefinition("img_png_to_jpg", ToolCategory.IMAGE, "PNG to JPG", "PNG से JPG", "Convert PNG to compact JPG", "PNG को कॉम्पैक्ट JPG में बदलें", initialSubTab = 4),
        ToolDefinition("img_webp", ToolCategory.IMAGE, "WebP Converter", "WebP कन्वर्टर", "Convert photos to modern WebP", "फ़ोटो को आधुनिक WebP में बदलें", initialSubTab = 5),
        ToolDefinition("img_to_pdf_b", ToolCategory.IMAGE, "Image to PDF", "इमेज से PDF", "Turn selected photos into a PDF", "चुनी गई फ़ोटो से PDF बनाएं", initialSubTab = 6),
        ToolDefinition("img_bg_util", ToolCategory.IMAGE, "Background Utility", "बैकग्राउंड टूल", "Remove solid backdrop or replace color", "सॉलिड बैकग्राउंड हटाएं या रंग बदलें", proOnly = true, initialSubTab = 7),
        ToolDefinition("img_rotate", ToolCategory.IMAGE, "Image Rotate & Flip", "इमेज रोटेट", "Rotate 90°/180°/270° & mirror", "90°/180°/270° घुमाएं और फ्लिप करें", initialSubTab = 8),
        ToolDefinition("img_inspect", ToolCategory.IMAGE, "Quality & Dimensions", "गुणवत्ता और आकार", "Inspect resolution, MP, aspect & bytes", "रिज़ॉल्यूशन, MP और आकार जांचें", initialSubTab = 9),

        // C. TEXT TOOLKIT (13 tools)
        ToolDefinition("txt_word_count", ToolCategory.TEXT, "Word Counter", "शब्द गिनती", "Count words, sentences & reading time", "शब्द, वाक्य और पढ़ने का समय गिनें", popular = true, initialSubTab = 0),
        ToolDefinition("txt_char_count", ToolCategory.TEXT, "Character Counter", "अक्षर गिनती", "Characters with & without spaces", "स्पेस सहित व रहित अक्षर गिनें", initialSubTab = 0),
        ToolDefinition("txt_case", ToolCategory.TEXT, "Case Converter", "केस कन्वर्टर", "Switch Sentence, Toggle & Camel case", "Sentence, Toggle और Camel केस बदलें", initialSubTab = 1),
        ToolDefinition("txt_upper", ToolCategory.TEXT, "Uppercase Converter", "अपरकेस", "Convert all text to CAPITAL letters", "सभी अक्षरों को CAPITAL में बदलें", initialSubTab = 1),
        ToolDefinition("txt_lower", ToolCategory.TEXT, "Lowercase Converter", "लोअरकेस", "Convert all text to lowercase", "सभी अक्षरों को lowercase में बदलें", initialSubTab = 1),
        ToolDefinition("txt_title", ToolCategory.TEXT, "Title Case", "टाइटल केस", "Capitalize Every Word Cleanly", "प्रत्येक शब्द का पहला अक्षर बड़ा करें", initialSubTab = 1),
        ToolDefinition("txt_spaces", ToolCategory.TEXT, "Remove Extra Spaces", "अतिरिक्त स्पेस हटाएं", "Collapse duplicate spaces & blank lines", "अतिरिक्त स्पेस और खाली लाइन हटाएं", initialSubTab = 2),
        ToolDefinition("txt_reverse", ToolCategory.TEXT, "Text Reverser", "टेक्स्ट रिवर्सर", "Reverse characters or word order", "अक्षरों या शब्दों का क्रम उलटें", initialSubTab = 2),
        ToolDefinition("txt_clean", ToolCategory.TEXT, "Text Cleaner", "टेक्स्ट क्लीनर", "Strip HTML tags, symbols & noise", "HTML टैग, चिह्न और अवांछित टेक्स्ट हटाएं", initialSubTab = 2),
        ToolDefinition("txt_compare", ToolCategory.TEXT, "Text Compare", "टेक्स्ट तुलना", "Line-by-line diff highlighter", "दो टेक्स्ट की पंक्ति-दर-पंक्ति तुलना करें", initialSubTab = 3),
        ToolDefinition("txt_dedup", ToolCategory.TEXT, "Duplicate Line Remover", "डुप्लिकेट लाइन हटाएं", "Keep unique lines & sort alphabetically", "डुप्लिकेट लाइनें हटाएं और क्रमबद्ध करें", initialSubTab = 4),
        ToolDefinition("txt_tts", ToolCategory.TEXT, "Text-to-Speech (EN/HI)", "टेक्स्ट-टू-स्पीच", "Read aloud in English or Hindi", "अंग्रेज़ी या हिंदी में टेक्स्ट सुनें", popular = true, initialSubTab = 5),
        ToolDefinition("txt_format", ToolCategory.TEXT, "Text Formatter", "टेक्स्ट फ़ॉर्मेटर", "Add numbering, bullets or format JSON", "नंबरिंग, बुलेट्स या JSON फ़ॉर्मेट करें", initialSubTab = 6),

        // D. QR CODE TOOLKIT (10 tools)
        ToolDefinition("qr_url", ToolCategory.QR, "URL QR Generator", "लिंक QR कोड", "Create scannable website link QR", "वेबसाइट लिंक के लिए QR कोड बनाएं", popular = true, initialSubTab = 0),
        ToolDefinition("qr_text", ToolCategory.QR, "Text QR Generator", "टेक्स्ट QR कोड", "Encode plain text or notes into QR", "सादा टेक्स्ट या नोट्स से QR बनाएं", initialSubTab = 1),
        ToolDefinition("qr_phone", ToolCategory.QR, "Phone QR Generator", "फ़ोन QR कोड", "One-tap call dialer QR code", "एक-टैप कॉल डायलर QR कोड", initialSubTab = 2),
        ToolDefinition("qr_wifi", ToolCategory.QR, "Wi-Fi QR Generator", "वाई-फाई QR कोड", "Instant Wi-Fi guest login QR", "तुरंत वाई-फाई कनेक्ट QR कोड", initialSubTab = 3),
        ToolDefinition("qr_email", ToolCategory.QR, "Email QR Generator", "ईमेल QR कोड", "Pre-filled recipient, subject & body", "विषय और संदेश सहित ईमेल QR", initialSubTab = 4),
        ToolDefinition("qr_whatsapp", ToolCategory.QR, "WhatsApp QR Generator", "व्हाट्सएप QR कोड", "Direct WhatsApp chat link QR", "सीधे व्हाट्सएप चैट लिंक का QR", initialSubTab = 5),
        ToolDefinition("qr_upi", ToolCategory.QR, "UPI QR Generator", "UPI भुगतान QR", "Generate BHIM/UPI QR from VPA & ₹", "UPI ID और ₹ राशि से भुगतान QR बनाएं", popular = true, initialSubTab = 6),
        ToolDefinition("qr_color", ToolCategory.QR, "QR Color Customizer", "QR रंग कस्टमाइज़र", "Customize foreground & background tint", "QR के आगे और पीछे का रंग चुनें", initialSubTab = 0),
        ToolDefinition("qr_png", ToolCategory.QR, "QR Download PNG", "PNG डाउनलोड", "Export high-res PNG QR image", "उच्च-गुणवत्ता PNG में QR सेव करें", initialSubTab = 0),
        ToolDefinition("qr_svg", ToolCategory.QR, "QR Download SVG", "SVG वेक्टर डाउनलोड", "Export scalable SVG vector QR", "स्केलेबल SVG वेक्टर में QR सेव करें", proOnly = true, initialSubTab = 0),

        // E. DOCUMENT & UTILITY TOOLS (14 tools)
        ToolDefinition("util_calc", ToolCategory.UTILITY, "Calculator", "कैलकुलेटर", "Fast expression calculator", "तेज़ और सटीक कैलकुलेटर", initialSubTab = 0),
        ToolDefinition("util_age", ToolCategory.UTILITY, "Age Calculator", "आयु कैलकुलेटर", "Exact years, months, days & birthday", "सटीक उम्र और अगला जन्मदिन गणना", initialSubTab = 1),
        ToolDefinition("util_datediff", ToolCategory.UTILITY, "Date Difference", "तिथि अंतर", "Days, weeks & months between dates", "दो तारीखों के बीच दिन और सप्ताह", initialSubTab = 2),
        ToolDefinition("util_unit", ToolCategory.UTILITY, "Unit Converter", "यूनिट कन्वर्टर", "Length, weight, area (Gaj/Bigha) & data", "लंबाई, वजन, गज/बीघा और डेटा बदलें", initialSubTab = 3),
        ToolDefinition("util_percent", ToolCategory.UTILITY, "Percentage Calculator", "प्रतिशत कैलकुलेटर", "% of number, discount & % change", "प्रतिशत, छूट और बदलाव गणना", initialSubTab = 4),
        ToolDefinition("util_bmi", ToolCategory.UTILITY, "BMI Calculator", "BMI कैलकुलेटर", "Informational Body Mass Index check", "जानकारी हेतु बॉडी मास इंडेक्स जांच", initialSubTab = 5),
        ToolDefinition("util_gst", ToolCategory.UTILITY, "GST Calculator", "GST कैलकुलेटर", "5/12/18/28% inclusive & CGST/SGST", "5/12/18/28% CGST और SGST गणना", popular = true, initialSubTab = 6),
        ToolDefinition("util_emi", ToolCategory.UTILITY, "EMI Calculator", "EMI कैलकुलेटर", "Monthly loan EMI & interest split", "मासिक लोन किस्त और ब्याज विवरण", popular = true, initialSubTab = 7),
        ToolDefinition("util_currency", ToolCategory.UTILITY, "Currency Converter", "मुद्रा कन्वर्टर", "Live ECB FX rates (INR, USD, EUR...)", "लाइव विदेशी मुद्रा विनिमय दर", initialSubTab = 8),
        ToolDefinition("util_time", ToolCategory.UTILITY, "Time Zone Converter", "समय क्षेत्र कन्वर्टर", "IST, UTC, EST, GST, SGT, JST & GMT", "IST से वैश्विक समय क्षेत्र बदलें", initialSubTab = 9),
        ToolDefinition("util_password", ToolCategory.UTILITY, "Password Generator", "पासवर्ड जेनरेटर", "SecureRandom client-side passwords", "मज़बूत और सुरक्षित पासवर्ड बनाएं", initialSubTab = 10),
        ToolDefinition("util_random", ToolCategory.UTILITY, "Random Number Gen", "रैंडम नंबर", "Generate unique numbers in any range", "किसी भी रेंज में रैंडम नंबर चुनें", initialSubTab = 11),
        ToolDefinition("util_stopwatch", ToolCategory.UTILITY, "Stopwatch", "स्टॉपवॉच", "Millisecond timer with lap splits", "लैप रिकॉर्ड के साथ स्टॉपवॉच", initialSubTab = 12),
        ToolDefinition("util_timer", ToolCategory.UTILITY, "Countdown Timer", "काउंटडाउन टाइमर", "Preset & custom countdown timer", "प्रीसेट और कस्टम काउंटडाउन टाइमर", initialSubTab = 13)
    )

    fun byId(id: String): ToolDefinition? = allTools.firstOrNull { it.id == id }
}
