package com.example

/**
 * Centralized configuration for the platform so the placeholder app name,
 * tagline, CDN endpoints, and safety rules can be updated in a single location.
 */
object AppConfig {
    const val APP_NAME = "VibeStream"
    const val APP_TAGLINE = "Original Short-Form Pulse & Creator Network"
    const val CDN_BASE_URL = "https://cdn.vibestream.social/streams"
    const val DEFAULT_MAX_VIDEO_SECONDS = 60
    const val MIN_AGE_REQUIREMENT = 13

    val PLACEHOLDER_VIDEO_URLS = listOf(
        "https://cdn.vibestream.social/placeholders/stream_01_rooftop_neon.mp4",
        "https://cdn.vibestream.social/placeholders/stream_02_modular_synth.mp4",
        "https://cdn.vibestream.social/placeholders/stream_03_shader_cloth.mp4",
        "https://cdn.vibestream.social/placeholders/stream_04_35mm_street.mp4",
        "https://cdn.vibestream.social/placeholders/stream_05_cyber_horizon.mp4",
        "https://cdn.vibestream.social/placeholders/stream_06_flagged_promo.mp4"
    )

    val REPORT_REASONS = listOf(
        "Spam or misleading engagement",
        "Harassment or bullying",
        "Hate speech or abusive behavior",
        "Inappropriate or unsafe content",
        "Copyright or impersonation",
        "Minor safety concern"
    )

    val COMMUNITY_GUIDELINES = listOf(
        "Be original and respectful: Celebrate creativity without harassment, hate speech, or bullying.",
        "Age-appropriate safety: Users must be 13+ (or local minimum age). Restricted content is filtered automatically.",
        "Anti-spam & authenticity: Automated spam, fake engagement, and repetitive flooding are blocked.",
        "Zero tolerance for abuse: Reported videos, comments, and accounts are reviewed in the Admin Moderation Queue."
    )

    val BANNED_SPAM_KEYWORDS = listOf(
        "free crypto scam",
        "click this malware link",
        "buy fake followers now",
        "hate speech attack"
    )

    data class LoginCredentialSpec(
        val username: String,
        val displayName: String,
        val email: String,
        val defaultPassword: String,
        val roleLabel: String,
        val isAdmin: Boolean,
        val avatarColorHex: String
    )

    val DEMO_LOGIN_CREDENTIALS = listOf(
        LoginCredentialSpec(
            username = "nova.pulse",
            displayName = "Nova Pulse",
            email = "nova@vibestream.social",
            defaultPassword = "pulse123",
            roleLabel = "Admin & Verified Creator",
            isAdmin = true,
            avatarColorHex = "#FF3366"
        ),
        LoginCredentialSpec(
            username = "kai.motion",
            displayName = "Kai Motion",
            email = "kai@vibestream.social",
            defaultPassword = "motion123",
            roleLabel = "Verified Creator",
            isAdmin = false,
            avatarColorHex = "#7C4DFF"
        ),
        LoginCredentialSpec(
            username = "aria.synth",
            displayName = "Aria SynthLab",
            email = "aria@vibestream.social",
            defaultPassword = "synth123",
            roleLabel = "Verified Creator",
            isAdmin = false,
            avatarColorHex = "#00E5FF"
        ),
        LoginCredentialSpec(
            username = "milo.frame",
            displayName = "Milo Frame",
            email = "milo@vibestream.social",
            defaultPassword = "frame123",
            roleLabel = "Creator",
            isAdmin = false,
            avatarColorHex = "#FFB300"
        ),
        LoginCredentialSpec(
            username = "zoe.kinetic",
            displayName = "Zoe Kinetic",
            email = "zoe@vibestream.social",
            defaultPassword = "kinetic123",
            roleLabel = "Verified Creator",
            isAdmin = false,
            avatarColorHex = "#00E676"
        )
    )

    fun defaultPasswordFor(username: String): String {
        return DEMO_LOGIN_CREDENTIALS.find {
            it.username.equals(username.removePrefix("@"), ignoreCase = true)
        }?.defaultPassword ?: "pulse123"
    }
}
