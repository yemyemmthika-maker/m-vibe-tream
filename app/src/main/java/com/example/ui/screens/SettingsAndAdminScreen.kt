package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.AppConfig
import com.example.data.engine.EpicEngineBenchmarkReport
import com.example.data.engine.EpicEngineModel
import com.example.data.engine.SynthAudioEngine
import com.example.data.engine.TrustAndSafetyEngine
import com.example.data.local.BlockedUserEntity
import com.example.data.local.ReportEntity
import com.example.data.local.UserEntity
import com.example.data.local.VideoEntity
import com.example.ui.components.CreatorAvatar
import com.example.ui.components.formatCompactCount
import com.example.ui.theme.CyberViolet
import com.example.ui.theme.ElectricCoral
import com.example.ui.theme.MintSuccess
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.PulseCyan

@Composable
fun SettingsScreen(
    currentUser: UserEntity,
    isDarkTheme: Boolean,
    blockedUsers: List<BlockedUserEntity>,
    onBack: () -> Unit,
    onToggleDarkTheme: () -> Unit,
    onUpdateSettings: (UserEntity) -> Unit,
    onChangePassword: (String) -> Unit,
    onUnblockUser: (Long) -> Unit,
    onSubmitProblemReport: (String) -> Unit,
    onOpenAdminDashboard: () -> Unit,
    onLogout: () -> Unit,
    onDeleteAccount: () -> Unit
) {
    BackHandler { onBack() }

    var newPassword by remember { mutableStateOf("") }
    var problemText by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var benchmarkReport by remember {
        mutableStateOf<EpicEngineBenchmarkReport?>(
            EpicEngineModel.runLiveDiagnosticBenchmark(
                videos = emptyList(),
                users = listOf(currentUser),
                hashtags = emptyList(),
                sounds = emptyList()
            )
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Settings, Privacy & Safety",
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }

        // Theme & Admin Shortcut
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DarkMode, contentDescription = null, tint = CyberViolet)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Dark Theme Appearance", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "Switch between Obsidian Dark and Daylight Light",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = isDarkTheme,
                            onCheckedChange = { onToggleDarkTheme() },
                            modifier = Modifier.testTag("settings_theme_switch")
                        )
                    }

                    if (currentUser.isAdmin) {
                        HorizontalDivider()
                        Button(
                            onClick = onOpenAdminDashboard,
                            colors = ButtonDefaults.buttonColors(containerColor = CyberViolet),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_open_admin_button")
                        ) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open Platform Admin Dashboard")
                        }
                    }
                }
            }
        }

        // Unified Epic Engine Architecture & Live Diagnostic Benchmark Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("epic_engine_status_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Epic Engine Architecture (7 Active)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = PulseCyan
                            )
                            Text(
                                text = "Real hardware & algorithmic subsystems powering VibeStream",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            color = MintSuccess.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "ONLINE",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MintSuccess,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    benchmarkReport?.let { report ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Live Benchmark Pass: ${report.totalExecutionMs} ms  •  Safety Index: ${report.safetyAuditScore}/100",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MintSuccess
                                )
                                Text(
                                    text = "PCM Synth: ${report.synthesizedPcmSamples} samples  •  SHA-256: ${report.cryptoHashPreview}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        report.engineNodes.forEach { node ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = node.engineName,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        text = node.subsystemSpec,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    color = if (node.isHardwareBacked) PulseCyan.copy(alpha = 0.16f) else CyberViolet.copy(alpha = 0.18f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = if (node.isHardwareBacked) "HARDWARE" else "ALGORITHMIC",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (node.isHardwareBacked) PulseCyan else Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            benchmarkReport = EpicEngineModel.runLiveDiagnosticBenchmark(
                                videos = emptyList(),
                                users = listOf(currentUser),
                                hashtags = emptyList(),
                                sounds = emptyList()
                            )
                            SynthAudioEngine.playPreviewClip(bpm = 128, themeIndex = 0, durationMs = 450)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCoral),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("run_epic_engine_benchmark_button")
                    ) {
                        Text("Run Live 7-Engine Diagnostic Benchmark")
                    }
                }
            }
        }

        // Privacy & Age-Appropriate Safety Controls
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = PulseCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Privacy & Age-Appropriate Safety Controls", style = MaterialTheme.typography.titleMedium)
                    }

                    SettingsToggleRow(
                        title = "Private Account",
                        subtitle = "Only approved followers can view your full profile reels",
                        checked = currentUser.isPrivateAccount,
                        onCheckedChange = { onUpdateSettings(currentUser.copy(isPrivateAccount = it)) }
                    )
                    SettingsToggleRow(
                        title = "Allow Comments From Everyone",
                        subtitle = "Let creators and viewers comment on your videos",
                        checked = currentUser.allowCommentsFromAll,
                        onCheckedChange = { onUpdateSettings(currentUser.copy(allowCommentsFromAll = it)) }
                    )
                    SettingsToggleRow(
                        title = "Allow Direct Messages",
                        subtitle = "Receive direct messages in your Inbox",
                        checked = currentUser.allowDirectMessages,
                        onCheckedChange = { onUpdateSettings(currentUser.copy(allowDirectMessages = it)) }
                    )
                    SettingsToggleRow(
                        title = "Age-Appropriate Safety Filter (${AppConfig.MIN_AGE_REQUIREMENT}+ Mode)",
                        subtitle = "Automatically hide mature or age-restricted clips from your feed",
                        checked = currentUser.ageSafetyMode,
                        onCheckedChange = { onUpdateSettings(currentUser.copy(ageSafetyMode = it)) }
                    )
                }
            }
        }

        // Notification Settings
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = ElectricCoral)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Notification Preferences", style = MaterialTheme.typography.titleMedium)
                    }
                    SettingsToggleRow(
                        title = "Likes & Reactions",
                        subtitle = "Notify when someone likes your videos",
                        checked = currentUser.notifyLikes,
                        onCheckedChange = { onUpdateSettings(currentUser.copy(notifyLikes = it)) }
                    )
                    SettingsToggleRow(
                        title = "Comments, Replies & @Mentions",
                        subtitle = "Notify on comments, nested replies, and @username tags",
                        checked = currentUser.notifyComments,
                        onCheckedChange = { onUpdateSettings(currentUser.copy(notifyComments = it)) }
                    )
                    SettingsToggleRow(
                        title = "New Followers",
                        subtitle = "Notify when creators follow your account",
                        checked = currentUser.notifyFollowers,
                        onCheckedChange = { onUpdateSettings(currentUser.copy(notifyFollowers = it)) }
                    )
                    SettingsToggleRow(
                        title = "Direct Messages",
                        subtitle = "Notify when new private messages arrive",
                        checked = currentUser.notifyMessages,
                        onCheckedChange = { onUpdateSettings(currentUser.copy(notifyMessages = it)) }
                    )
                }
            }
        }

        // Change Password & Login Details
        item {
            val defaultPwd = AppConfig.defaultPasswordFor(currentUser.username)
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.testTag("settings_login_details_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = NeonAmber)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Account Login Details & Password Security", style = MaterialTheme.typography.titleMedium)
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Username: @${currentUser.username}   •   Role: ${if (currentUser.isAdmin) "Admin & Creator" else "Creator"}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = "Email: ${currentUser.email}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Default Password: $defaultPwd",
                                style = MaterialTheme.typography.labelLarge,
                                color = PulseCyan
                            )
                        }
                    }
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("New Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_new_password_input")
                    )
                    Button(
                        onClick = {
                            onChangePassword(newPassword)
                            newPassword = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCoral),
                        modifier = Modifier.testTag("settings_change_password_button")
                    ) {
                        Text("Update Password")
                    }
                }
            }
        }

        // Blocked Users Management
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Blocked Users (${blockedUsers.size})",
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (blockedUsers.isEmpty()) {
                        Text(
                            text = "You haven't blocked any accounts.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        blockedUsers.forEach { blocked ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CreatorAvatar(
                                        username = blocked.blockedUsername,
                                        colorHex = blocked.blockedAvatarHex,
                                        size = 38.dp
                                    )
                                    Text("@${blocked.blockedUsername}", style = MaterialTheme.typography.bodyLarge)
                                }
                                OutlinedButton(onClick = { onUnblockUser(blocked.blockedUserId) }) {
                                    Text("Unblock")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Community Guidelines & Report a Problem
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Community Guidelines", style = MaterialTheme.typography.titleMedium)
                    AppConfig.COMMUNITY_GUIDELINES.forEach { rule ->
                        Text(
                            text = "• $rule",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text("Report a Problem", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = problemText,
                        onValueChange = { problemText = it },
                        label = { Text("Describe a bug, safety concern, or feedback...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_report_problem_input")
                    )
                    Button(
                        onClick = {
                            if (problemText.isNotBlank()) {
                                onSubmitProblemReport(problemText)
                                problemText = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberViolet)
                    ) {
                        Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Send Problem Report")
                    }
                }
            }
        }

        // Logout & Delete Account
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onLogout,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("settings_logout_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Logout")
                }
                Button(
                    onClick = { showDeleteConfirm = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("settings_delete_account_button")
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete Account")
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Account Permanently?") },
            text = {
                Text("This will permanently remove @${currentUser.username}, your profile, and your uploaded videos.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteAccount()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Confirm Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/**
 * Full Admin Dashboard for Authorized Administrators:
 * - View basic platform statistics
 * - Review reports & reported content
 * - View uploaded videos & approve/remove content
 * - View users & suspend/ban/restore accounts
 */
@Composable
fun AdminDashboardScreen(
    users: List<UserEntity>,
    videos: List<VideoEntity>,
    reports: List<ReportEntity>,
    onBack: () -> Unit,
    onUpdateReport: (ReportEntity, String, String) -> Unit,
    onSetVideoModeration: (VideoEntity, String) -> Unit,
    onSetUserStatus: (UserEntity, String) -> Unit
) {
    BackHandler { onBack() }

    var activeSection by remember { mutableIntStateOf(0) } // 0 = Reports Queue, 1 = Videos, 2 = Users, 3 = Engines
    var adminBenchmark by remember(videos, users) {
        mutableStateOf(
            EpicEngineModel.runLiveDiagnosticBenchmark(
                videos = videos,
                users = users,
                hashtags = emptyList(),
                sounds = emptyList()
            )
        )
    }

    val openReportsCount = reports.count { it.status == "OPEN" }
    val flaggedVideosCount = videos.count { it.moderationStatus == "FLAGGED" }
    val suspendedOrBannedCount = users.count { it.accountStatus != "ACTIVE" }
    val totalViews = videos.sumOf { it.viewsCount }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Icon(
                    imageVector = Icons.Default.Gavel,
                    contentDescription = null,
                    tint = PulseCyan,
                    modifier = Modifier.size(28.dp)
                )
                Column {
                    Text(
                        text = "Admin & Moderation Console",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "Review reports, remove violating videos, and suspend/ban accounts",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Platform Statistics Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AdminStatCard(
                        title = "Total Users",
                        value = users.size.toString(),
                        subtitle = "$suspendedOrBannedCount restricted",
                        accent = PulseCyan,
                        modifier = Modifier.weight(1f)
                    )
                    AdminStatCard(
                        title = "Uploaded Videos",
                        value = videos.size.toString(),
                        subtitle = "${formatCompactCount(totalViews)} total views",
                        accent = CyberViolet,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AdminStatCard(
                        title = "Open Reports",
                        value = openReportsCount.toString(),
                        subtitle = "${reports.size} total filed",
                        accent = ElectricCoral,
                        modifier = Modifier.weight(1f)
                    )
                    AdminStatCard(
                        title = "Flagged Clips",
                        value = flaggedVideosCount.toString(),
                        subtitle = "Awaiting review",
                        accent = NeonAmber,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Section Switcher
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = activeSection == 0,
                    onClick = { activeSection = 0 },
                    label = { Text("Reports (${reports.size})") },
                    modifier = Modifier.testTag("admin_tab_reports")
                )
                FilterChip(
                    selected = activeSection == 1,
                    onClick = { activeSection = 1 },
                    label = { Text("Videos (${videos.size})") },
                    modifier = Modifier.testTag("admin_tab_videos")
                )
                FilterChip(
                    selected = activeSection == 2,
                    onClick = { activeSection = 2 },
                    label = { Text("Users (${users.size})") },
                    modifier = Modifier.testTag("admin_tab_users")
                )
                FilterChip(
                    selected = activeSection == 3,
                    onClick = { activeSection = 3 },
                    label = { Text("Engines (7)") },
                    modifier = Modifier.testTag("admin_tab_engines")
                )
            }
        }

        when (activeSection) {
            0 -> {
                items(reports, key = { it.id }) { report ->
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = when (report.status) {
                                        "OPEN" -> ElectricCoral.copy(alpha = 0.2f)
                                        "ACTION_TAKEN" -> MintSuccess.copy(alpha = 0.2f)
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "${report.targetType} • ${report.status}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = when (report.status) {
                                            "OPEN" -> ElectricCoral
                                            "ACTION_TAKEN" -> MintSuccess
                                            else -> MaterialTheme.colorScheme.onSurface
                                        },
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Text(
                                    text = "Reported by @${report.reporterUsername}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            val reportAudit = remember(report.targetSummary, report.reason) {
                                TrustAndSafetyEngine.evaluateTextSafety("${report.targetSummary} ${report.reason}")
                            }
                            Text(
                                text = report.targetSummary,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "Reason: ${report.reason}  •  NLP Risk: ${reportAudit.riskScore}/100 (${reportAudit.severityLabel})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = NeonAmber
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        onUpdateReport(report, "ACTION_TAKEN", "Content removed / restricted by admin")
                                        if (report.targetType == "VIDEO") {
                                            videos.find { it.id == report.targetId }?.let { v ->
                                                onSetVideoModeration(v, "REMOVED")
                                            }
                                        } else if (report.targetType == "USER") {
                                            users.find { it.id == report.targetId }?.let { u ->
                                                onSetUserStatus(u, "SUSPENDED")
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCoral),
                                    modifier = Modifier.testTag("admin_action_report_${report.id}")
                                ) {
                                    Text("Remove & Enforce")
                                }
                                OutlinedButton(
                                    onClick = {
                                        onUpdateReport(report, "DISMISSED", "Reviewed — no policy violation")
                                    }
                                ) {
                                    Text("Dismiss")
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                items(videos, key = { it.id }) { video ->
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "#${video.id} • @${video.creatorUsername}",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Surface(
                                    color = when (video.moderationStatus) {
                                        "APPROVED" -> MintSuccess.copy(alpha = 0.2f)
                                        "FLAGGED" -> NeonAmber.copy(alpha = 0.2f)
                                        else -> ElectricCoral.copy(alpha = 0.2f)
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = video.moderationStatus,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = when (video.moderationStatus) {
                                            "APPROVED" -> MintSuccess
                                            "FLAGGED" -> NeonAmber
                                            else -> ElectricCoral
                                        },
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Text(
                                text = video.caption,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (video.moderationStatus != "APPROVED") {
                                    Button(
                                        onClick = { onSetVideoModeration(video, "APPROVED") },
                                        colors = ButtonDefaults.buttonColors(containerColor = MintSuccess)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Approve", color = Color.Black)
                                    }
                                }
                                if (video.moderationStatus != "REMOVED") {
                                    Button(
                                        onClick = { onSetVideoModeration(video, "REMOVED") },
                                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCoral),
                                        modifier = Modifier.testTag("admin_remove_video_${video.id}")
                                    ) {
                                        Text("Remove Video")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                items(users, key = { it.id }) { user ->
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CreatorAvatar(
                                        username = user.username,
                                        colorHex = user.avatarColorHex,
                                        size = 42.dp,
                                        isVerified = user.isVerified
                                    )
                                    Column {
                                        Text(user.displayName, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            "@${user.username} • ${user.email}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Surface(
                                    color = when (user.accountStatus) {
                                        "ACTIVE" -> MintSuccess.copy(alpha = 0.2f)
                                        "SUSPENDED" -> NeonAmber.copy(alpha = 0.2f)
                                        else -> ElectricCoral.copy(alpha = 0.2f)
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = user.accountStatus,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = when (user.accountStatus) {
                                            "ACTIVE" -> MintSuccess
                                            "SUSPENDED" -> NeonAmber
                                            else -> ElectricCoral
                                        },
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (user.accountStatus != "ACTIVE") {
                                    OutlinedButton(onClick = { onSetUserStatus(user, "ACTIVE") }) {
                                        Text("Restore Active")
                                    }
                                }
                                if (user.accountStatus != "SUSPENDED") {
                                    OutlinedButton(
                                        onClick = { onSetUserStatus(user, "SUSPENDED") },
                                        modifier = Modifier.testTag("admin_suspend_user_${user.id}")
                                    ) {
                                        Text("Suspend")
                                    }
                                }
                                if (user.accountStatus != "BANNED") {
                                    Button(
                                        onClick = { onSetUserStatus(user, "BANNED") },
                                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCoral),
                                        modifier = Modifier.testTag("admin_ban_user_${user.id}")
                                    ) {
                                        Text("Ban Account")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            3 -> {
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_engines_telemetry_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Epic Engine Cluster Telemetry (${adminBenchmark.totalExecutionMs} ms pass)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = PulseCyan
                            )
                            Text(
                                text = "PCM Samples: ${adminBenchmark.synthesizedPcmSamples} • Ranked Reels: ${adminBenchmark.rankedFeedVideosCount} • Indexed Entities: ${adminBenchmark.indexedSearchEntitiesCount}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MintSuccess
                            )
                            Button(
                                onClick = {
                                    adminBenchmark = EpicEngineModel.runLiveDiagnosticBenchmark(
                                        videos = videos,
                                        users = users,
                                        hashtags = emptyList(),
                                        sounds = emptyList()
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberViolet),
                                modifier = Modifier.testTag("admin_rerun_benchmark_button")
                            ) {
                                Text("Re-Run Full Cluster Benchmark")
                            }
                        }
                    }
                }
                items(adminBenchmark.engineNodes, key = { it.id }) { node ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = node.engineName,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = node.subsystemSpec,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = node.telemetryMetric,
                                style = MaterialTheme.typography.labelLarge,
                                color = PulseCyan
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminStatCard(
    title: String,
    value: String,
    subtitle: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineLarge.copy(
                    color = accent,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
