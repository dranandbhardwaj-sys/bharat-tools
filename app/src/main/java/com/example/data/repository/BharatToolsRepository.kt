package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Announcement
import com.example.data.model.AppConfig
import com.example.data.model.PurchaseRecord
import com.example.data.model.ReferralAttribution
import com.example.data.model.SupportTicket
import com.example.data.model.ToolHistory
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BharatToolsRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

    private fun <T> Flow<T>.guardFirestoreErrors(op: OperationType, path: String, expectedUid: String): Flow<T> =
        this.catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val exceptionToLog = firestoreError ?: (error as? Exception)
            if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
                handleFirestoreError(exceptionToLog, op, path)
            }
            throw error
        }

    suspend fun ensureUserProfile(user: FirebaseUser): Result<Unit> {
        val uid = user.uid
        val path = "users/$uid"
        return try {
            val docRef = db.collection("users").document(uid)
            val existing = docRef.get().await()
            if (!existing.exists()) {
                val safeName = (user.displayName?.takeIf { it.isNotBlank() } ?: "Bharat User").take(80)
                val safeEmail = (user.email ?: "").take(120)
                val safePhoto = user.photoUrl?.toString()?.take(500)
                val codeSuffix = uid.takeLast(6).uppercase(Locale.US).replace(Regex("[^A-Z0-9]"), "X")
                val refCode = "BT$codeSuffix".padEnd(6, '0')
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "email" to safeEmail,
                    "photoUrl" to safePhoto,
                    "preferredLanguage" to "en",
                    "themeMode" to "system",
                    "referralCode" to refCode,
                    "favorites" to emptyList<String>(),
                    "recentTools" to emptyList<String>(),
                    "subscriptionStatus" to "free",
                    "autoRenewConsent" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null } as Map<String, Any>
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        require(userId.isNotBlank()) { "Blank userId" }
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snap -> snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .guardFirestoreErrors(OperationType.GET, path, userId)
        )
    }

    suspend fun updatePreferences(
        language: String? = null,
        themeMode: String? = null,
        displayName: String? = null,
        autoRenewConsent: Boolean? = null
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid"
        return try {
            val updates = mutableMapOf<String, Any>("updatedAt" to FieldValue.serverTimestamp())
            if (language != null && language in listOf("en", "hi")) updates["preferredLanguage"] = language
            if (themeMode != null && themeMode in listOf("system", "light", "dark")) updates["themeMode"] = themeMode
            if (displayName != null && displayName.trim().isNotEmpty()) updates["displayName"] = displayName.trim().take(80)
            if (autoRenewConsent != null) updates["autoRenewConsent"] = autoRenewConsent
            db.collection("users").document(uid).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleFavorite(toolId: String, currentFavorites: List<String>): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid"
        return try {
            val next = if (toolId in currentFavorites) {
                currentFavorites.filterNot { it == toolId }
            } else {
                (listOf(toolId) + currentFavorites).distinct().take(50)
            }
            db.collection("users").document(uid).update(
                mapOf("favorites" to next, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun recordRecentTool(toolId: String, currentRecent: List<String>, title: String, summary: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid"
        return try {
            val next = (listOf(toolId) + currentRecent.filterNot { it == toolId }).take(20)
            db.collection("users").document(uid).update(
                mapOf("recentTools" to next, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            val histRef = db.collection("users").document(uid).collection("tool_history").document()
            histRef.set(
                mapOf(
                    "userId" to uid,
                    "toolId" to toolId.take(60),
                    "title" to title.take(120),
                    "summary" to summary.take(400),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun observeToolHistory(userId: String): Flow<List<ToolHistory>> = flow {
        val path = "users/$userId/tool_history"
        emitAll(
            db.collection("users").document(userId).collection("tool_history")
                .snapshots()
                .map { snap ->
                    snap.toObjects(ToolHistory::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .guardFirestoreErrors(OperationType.LIST, path, userId)
        )
    }

    suspend fun clearToolHistory(): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val histCol = db.collection("users").document(uid).collection("tool_history")
        return try {
            val docs = histCol.get().await().documents
            if (docs.isNotEmpty()) {
                val batch = db.batch()
                docs.forEach { batch.delete(it.reference) }
                batch.commit().await()
            }
            db.collection("users").document(uid).update(
                mapOf("recentTools" to emptyList<String>(), "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/$uid/tool_history")
            Result.failure(e)
        }
    }

    fun observeUserPurchases(userId: String): Flow<List<PurchaseRecord>> = flow {
        val path = "purchases"
        emitAll(
            db.collection("purchases")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(PurchaseRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .guardFirestoreErrors(OperationType.LIST, path, userId)
        )
    }

    suspend fun submitSubscriptionVerification(
        paymentMethod: String,
        referenceId: String,
        consentAccepted: Boolean,
        notes: String
    ): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(IllegalStateException("Not signed in"))
        require(consentAccepted) { "Please approve the recurring ₹399/month billing terms before submitting." }
        val uid = user.uid
        val trimmedRef = referenceId.trim().take(80)
        require(trimmedRef.length >= 4) { "Mandate / UTR Reference ID must be at least 4 characters." }
        val fmt = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
        val start = Date()
        val end = Date(start.time + 30L * 24L * 60L * 60L * 1000L)
        val periodLabel = "${fmt.format(start)} – ${fmt.format(end)}"
        val docRef = db.collection("purchases").document()
        return try {
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "userEmail" to (user.email ?: "").take(120),
                    "amountInr" to 399,
                    "billingCycle" to "monthly_recurring",
                    "paymentMethod" to paymentMethod,
                    "referenceId" to trimmedRef,
                    "status" to "pending",
                    "userConsentAccepted" to true,
                    "receiptNumber" to "",
                    "periodLabel" to periodLabel,
                    "notes" to notes.trim().take(300),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            db.collection("users").document(uid).update(
                mapOf("autoRenewConsent" to true, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun cancelSubscription(activePurchaseId: String?): Result<Unit> {
        val uid = requireUserId()
        return try {
            if (!activePurchaseId.isNullOrBlank()) {
                db.collection("purchases").document(activePurchaseId).update(
                    mapOf(
                        "status" to "cancelled",
                        "notes" to "Cancelled by user via Account Dashboard",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            db.collection("users").document(uid).update(
                mapOf(
                    "subscriptionStatus" to "cancelled",
                    "autoRenewConsent" to false,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
            Result.failure(e)
        }
    }

    suspend fun requestPurchaseRefund(purchaseId: String, reason: String): Result<Unit> {
        requireUserId()
        val path = "purchases/$purchaseId"
        return try {
            db.collection("purchases").document(purchaseId).update(
                mapOf(
                    "status" to "refund_requested",
                    "notes" to reason.trim().take(300),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    fun observeUserTickets(userId: String): Flow<List<SupportTicket>> = flow {
        val path = "support_tickets"
        emitAll(
            db.collection("support_tickets")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(SupportTicket::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .guardFirestoreErrors(OperationType.LIST, path, userId)
        )
    }

    suspend fun createSupportTicket(category: String, subject: String, message: String): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(IllegalStateException("Not signed in"))
        val s = subject.trim().take(120)
        val m = message.trim().take(1000)
        require(s.length >= 3) { "Subject must be at least 3 characters." }
        require(m.length >= 5) { "Message must be at least 5 characters." }
        val docRef = db.collection("support_tickets").document()
        return try {
            docRef.set(
                mapOf(
                    "userId" to user.uid,
                    "userEmail" to (user.email ?: "").take(120),
                    "category" to category,
                    "subject" to s,
                    "message" to m,
                    "status" to "open",
                    "adminReply" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun applyReferralCode(referrerCode: String): Result<Unit> {
        val uid = requireUserId()
        val code = referrerCode.trim().uppercase(Locale.US).take(24)
        require(code.length >= 4) { "Referral code must be at least 4 characters." }
        return try {
            db.collection("users").document(uid).update(
                mapOf("referredByCode" to code, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            db.collection("referrals").document(uid).set(
                mapOf(
                    "referrerCode" to code,
                    "referredUserId" to uid,
                    "status" to "registered",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "referrals/$uid")
            Result.failure(e)
        }
    }

    fun observeActiveAnnouncements(userId: String): Flow<List<Announcement>> = flow {
        val path = "announcements"
        emitAll(
            db.collection("announcements")
                .whereEqualTo("active", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Announcement::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .guardFirestoreErrors(OperationType.LIST, path, userId)
        )
    }

    fun observeAppConfig(userId: String): Flow<AppConfig?> = flow {
        val path = "app_config/global"
        emitAll(
            db.collection("app_config").document("global")
                .snapshots()
                .map { snap -> snap.toObject(AppConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .guardFirestoreErrors(OperationType.GET, path, userId)
        )
    }

    suspend fun checkIsAdmin(user: FirebaseUser?): Boolean {
        if (user == null) return false
        if (user.email == "dranandbhardwaj@gmail.com" && user.isEmailVerified) return true
        return try {
            db.collection("admins").document(user.uid).get().await().exists()
        } catch (_: Exception) {
            false
        }
    }

    // --- ADMIN OPERATIONS (Protected by isAdmin() in firestore.rules) ---
    fun observeAllUsers(adminUid: String): Flow<List<UserProfile>> = flow {
        emitAll(
            db.collection("users").snapshots()
                .map { it.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .guardFirestoreErrors(OperationType.LIST, "users", adminUid)
        )
    }

    fun observeAllPurchases(adminUid: String): Flow<List<PurchaseRecord>> = flow {
        emitAll(
            db.collection("purchases").snapshots()
                .map {
                    it.toObjects(PurchaseRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { r -> r.createdAt?.seconds ?: 0L }
                }
                .guardFirestoreErrors(OperationType.LIST, "purchases", adminUid)
        )
    }

    fun observeAllTickets(adminUid: String): Flow<List<SupportTicket>> = flow {
        emitAll(
            db.collection("support_tickets").snapshots()
                .map {
                    it.toObjects(SupportTicket::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { t -> t.createdAt?.seconds ?: 0L }
                }
                .guardFirestoreErrors(OperationType.LIST, "support_tickets", adminUid)
        )
    }

    fun observeAllReferrals(adminUid: String): Flow<List<ReferralAttribution>> = flow {
        emitAll(
            db.collection("referrals").snapshots()
                .map { it.toObjects(ReferralAttribution::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .guardFirestoreErrors(OperationType.LIST, "referrals", adminUid)
        )
    }

    suspend fun adminUpdateSubscriptionStatus(
        purchaseId: String,
        targetUserId: String,
        newStatus: String
    ): Result<Unit> {
        requireUserId()
        return try {
            val userSubStatus = when (newStatus) {
                "active" -> "active"
                "expired" -> "expired"
                "cancelled" -> "cancelled"
                "refunded" -> "free"
                "failed" -> "expired"
                else -> "free"
            }
            val receipt = if (newStatus == "active") "BT-SUB-${System.currentTimeMillis().toString().takeLast(6)}" else ""
            val purchaseUpdates = mutableMapOf<String, Any>(
                "status" to newStatus,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (receipt.isNotEmpty()) purchaseUpdates["receiptNumber"] = receipt
            db.collection("purchases").document(purchaseId).update(purchaseUpdates).await()

            val userUpdates = mutableMapOf<String, Any>(
                "subscriptionStatus" to userSubStatus,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (newStatus == "active") {
                val periodEnd = Timestamp(Date(System.currentTimeMillis() + 30L * 24L * 60L * 60L * 1000L))
                userUpdates["subscriptionPeriodEnd"] = periodEnd
            }
            db.collection("users").document(targetUserId).update(userUpdates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "purchases/$purchaseId")
            Result.failure(e)
        }
    }

    suspend fun adminReplyTicket(ticketId: String, status: String, reply: String): Result<Unit> {
        requireUserId()
        return try {
            db.collection("support_tickets").document(ticketId).update(
                mapOf(
                    "status" to status,
                    "adminReply" to reply.trim().take(1000),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "support_tickets/$ticketId")
            Result.failure(e)
        }
    }

    suspend fun adminPublishAnnouncement(title: String, titleHi: String, body: String, bodyHi: String): Result<Unit> {
        requireUserId()
        val docRef = db.collection("announcements").document()
        return try {
            docRef.set(
                mapOf(
                    "title" to title.trim().take(120),
                    "titleHi" to titleHi.trim().take(120),
                    "body" to body.trim().take(600),
                    "bodyHi" to bodyHi.trim().take(600),
                    "active" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun adminSaveConfig(trialLimit: Int, maintenance: Boolean, supportEmail: String): Result<Unit> {
        requireUserId()
        val docRef = db.collection("app_config").document("global")
        return try {
            docRef.set(
                mapOf(
                    "priceInr" to 399,
                    "trialToolsLimit" to trialLimit.coerceIn(1, 100),
                    "maintenanceMode" to maintenance,
                    "supportEmail" to supportEmail.trim().take(120),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }
}
