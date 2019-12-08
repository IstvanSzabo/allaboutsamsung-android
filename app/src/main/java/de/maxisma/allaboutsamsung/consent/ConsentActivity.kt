package de.maxisma.allaboutsamsung.consent

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.ads.consent.ConsentInfoUpdateListener
import com.google.ads.consent.ConsentInformation
import com.google.ads.consent.ConsentStatus
import de.maxisma.allaboutsamsung.BaseActivity
import de.maxisma.allaboutsamsung.BuildConfig
import de.maxisma.allaboutsamsung.LegalNoticeActivity
import de.maxisma.allaboutsamsung.MainActivity
import de.maxisma.allaboutsamsung.R
import de.maxisma.allaboutsamsung.databinding.ActivityConsentBinding
import de.maxisma.allaboutsamsung.utils.retry
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ConsentActivity : BaseActivity(useDefaultMenu = false) {

    private var detailsDialogIsQueued = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityConsentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val consentInformation = ConsentInformation.getInstance(this)
        val publisherId = BuildConfig.APPMOB_PUBLISHER_ID

        binding.consentDetails.setOnClickListener {
            Toast.makeText(this, R.string.loading_providers, Toast.LENGTH_SHORT).show()
            detailsDialogIsQueued = true
        }

        binding.consentEnablePrivateMode.setOnClickListener {
            preferenceHolder.gdprMode = true
            markConsentActivityAsAnswered(this)
            startActivity(Intent(this, MainActivity::class.java))
        }

        binding.consentDisablePrivateMode.setOnClickListener {
            preferenceHolder.gdprMode = false
            markConsentActivityAsAnswered(this)
            startActivity(Intent(this, MainActivity::class.java))
        }

        uiLaunch {
            @Suppress("SENSELESS_COMPARISON")
            val admobServiceNames = if (publisherId != null) {
                try {
                    retry(RuntimeException::class) {
                        consentInformation.awaitConsentInfoUpdate(listOf(publisherId))
                    }
                    consentInformation.adProviders.map { it.name }
                } catch (e: RuntimeException) {
                    emptyList<String>()
                }
            } else {
                emptyList()
            }

            binding.consentDetails.setOnClickListener {
                val text = (resources.getStringArray(R.array.thirdPartyServices) + admobServiceNames)
                    .joinToString(separator = "\n") { getString(R.string.third_party_service_ad, it) }
                AlertDialog.Builder(this@ConsentActivity)
                    .setTitle(R.string.third_party_services)
                    .setMessage(text)
                    .setNegativeButton(R.string.legal_notice) { _, _ -> startActivity(Intent(this@ConsentActivity, LegalNoticeActivity::class.java)) }
                    .setPositiveButton(R.string.close, null)
                    .show()
            }

            if (detailsDialogIsQueued) {
                binding.consentDetails.performClick()
                detailsDialogIsQueued = false
            }
        }
    }
}

private suspend fun ConsentInformation.awaitConsentInfoUpdate(publisherIds: List<String>): ConsentStatus = suspendCancellableCoroutine { continuation ->
    requestConsentInfoUpdate(publisherIds.toTypedArray(), object : ConsentInfoUpdateListener {
        override fun onFailedToUpdateConsentInfo(reason: String?) {
            if (!continuation.isCancelled) {
                continuation.resumeWithException(RuntimeException("Could not retrieve consent status: ${reason ?: "No reason given."}"))
            }
        }

        override fun onConsentInfoUpdated(consentStatus: ConsentStatus) {
            if (!continuation.isCancelled) {
                continuation.resume(consentStatus)
            }
        }

    })
}

private const val prefsName = "consent"
private const val keyConsentAnswered = "consent_answered"

fun needsToShowConsentActivity(context: Context) = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE).getBoolean(keyConsentAnswered, true)

private fun markConsentActivityAsAnswered(context: Context) {
    context.getSharedPreferences(prefsName, Context.MODE_PRIVATE).edit().putBoolean(keyConsentAnswered, false).apply()
}