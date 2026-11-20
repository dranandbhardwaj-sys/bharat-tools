package com.example.data.model

import androidx.annotation.Keep
import androidx.compose.ui.graphics.Color
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class ToolCategory(
    val id: String,
    val titleEn: String,
    val titleHi: String,
    val subtitleEn: String,
    val subtitleHi: String,
    val accentHex: Long
) {
    PDF(
        id = "pdf",
        titleEn = "PDF Toolkit",
        titleHi = "PDF टूलकिट",
        subtitleEn = "Merge, split, compress, rotate, protect & preview",
        subtitleHi = "मर्ज, स्प्लिट, कंप्रेस, रोटेट, पासवर्ड व प्रीव्यू",
        accentHex = 0xFFDC2626
    ),
    IMAGE(
        id = "image",
        titleEn = "Image Toolkit",
        titleHi = "इमेज टूलकिट",
        subtitleEn = "Compress, resize, crop, convert & backdrop",
        subtitleHi = "कंप्रेस, रीसाइज़, क्रॉप, कन्वर्ट व बैकग्राउंड",
        accentHex = 0xFF2563EB
    ),
    TEXT(
        id = "text",
        titleEn = "Text Toolkit",
        titleHi = "टेक्स्ट टूलकिट",
        subtitleEn = "Count, clean, diff, format & speech (EN/HI)",
        subtitleHi = "वर्ड काउंट, क्लीनर, तुलना, फॉर्मेट व स्पीच",
        accentHex = 0xFF1E3A8A
    ),
    QR(
        id = "qr",
        titleEn = "QR Code Toolkit",
        titleHi = "QR कोड टूलकिट",
        subtitleEn = "UPI, Wi-Fi, WhatsApp, URL & PNG/SVG export",
        subtitleHi = "UPI, वाई-फाई, व्हाट्सएप, लिंक व PNG/SVG डाउनलोड",
        accentHex = 0xFF7C3AED
    ),
    UTILITY(
        id = "utility",
        titleEn = "Document & Utilities",
        titleHi = "दस्तावेज़ व उपयोगिताएं",
        subtitleEn = "GST, EMI, BMI, Currency, Units, Timer & more",
        subtitleHi = "GST, EMI, BMI, मुद्रा, यूनिट, टाइमर और अधिक",
        accentHex = 0xFF0D9488
    );

    val accentColor: Color get() = Color(accentHex)
}

data class ToolDefinition(
    val id: String,
    val category: ToolCategory,
    val titleEn: String,
    val titleHi: String,
    val descEn: String,
    val descHi: String,
    val popular: Boolean = false,
    val proOnly: Boolean = false,
    val initialSubTab: Int = 0
)

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val preferredLanguage: String = "en",
    val themeMode: String = "system",
    val referralCode: String = "",
    val referredByCode: String? = null,
    val favorites: List<String> = emptyList(),
    val recentTools: List<String> = emptyList(),
    val subscriptionStatus: String = "free",
    val autoRenewConsent: Boolean = false,
    val subscriptionPeriodEnd: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val isSubscriptionExpiredByDate: Boolean
        get() = subscriptionPeriodEnd != null && subscriptionPeriodEnd.toDate().time <= System.currentTimeMillis()

    val hasProAccess: Boolean
        get() = subscriptionStatus == "active" && !isSubscriptionExpiredByDate

    val daysRemaining: Long
        get() {
            val end = subscriptionPeriodEnd?.toDate()?.time ?: return 0L
            val diff = end - System.currentTimeMillis()
            return if (diff <= 0L) 0L else (diff / (1000L * 60L * 60L * 24L)) + 1L
        }
}

@Keep
@IgnoreExtraProperties
data class ToolHistory(
    @DocumentId val id: String = "",
    val userId: String = "",
    val toolId: String = "",
    val title: String = "",
    val summary: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PurchaseRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val amountInr: Long = 399L,
    val billingCycle: String = "monthly_recurring",
    val paymentMethod: String = "upi_autopay",
    val referenceId: String = "",
    val status: String = "pending",
    val userConsentAccepted: Boolean = false,
    val receiptNumber: String = "",
    val periodLabel: String = "",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SupportTicket(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val category: String = "bug",
    val subject: String = "",
    val message: String = "",
    val status: String = "open",
    val adminReply: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ReferralAttribution(
    @DocumentId val id: String = "",
    val referrerCode: String = "",
    val referredUserId: String = "",
    val status: String = "registered",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Announcement(
    @DocumentId val id: String = "",
    val title: String = "",
    val titleHi: String = "",
    val body: String = "",
    val bodyHi: String = "",
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AppConfig(
    @DocumentId val id: String = "",
    val priceInr: Long = 399L,
    val trialToolsLimit: Long = 45L,
    val maintenanceMode: Boolean = false,
    val supportEmail: String = "support@bharattools.in",
    @ServerTimestamp val updatedAt: Timestamp? = null
)
