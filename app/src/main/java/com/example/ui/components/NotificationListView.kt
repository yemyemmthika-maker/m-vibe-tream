package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.NotificationCenterUiState
import com.example.data.local.NotificationEntity
import com.example.data.local.NotificationEventType
import com.example.ui.theme.CyberViolet
import com.example.ui.theme.ElectricCoral
import com.example.ui.theme.MintSuccess
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.PulseCyan

/**
 * UI Composable that displays local notification events (Likes, Follows, Comments, Replies, Mentions)
 * stored in Room inside an interactive, filterable list view.
 */
@Composable
fun NotificationListView(
    notifications: List<NotificationEntity>,
    modifier: Modifier = Modifier,
    selectedFilter: NotificationEventType? = null,
    onFilterChange: ((NotificationEventType) -> Unit)? = null,
    blockedActorIds: Set<Long> = emptySet(),
    onNotificationClick: (NotificationEntity) -> Unit = {},
    onMarkNotificationRead: (Long) -> Unit = {},
    onMarkAllRead: () -> Unit = {},
    onDeleteNotification: (Long) -> Unit = {},
    onClearAllNotifications: () -> Unit = {}
) {
    var internalFilter by remember { mutableStateOf(NotificationEventType.ALL) }
    val activeFilter = selectedFilter ?: internalFilter
    val notificationSnapshot = notifications.toList()

    val uiState = remember(notificationSnapshot, activeFilter, blockedActorIds) {
        NotificationCenterUiState.fromNotifications(
            recipientId = notificationSnapshot.firstOrNull()?.recipientId,
            notifications = notificationSnapshot,
            selectedFilter = activeFilter,
            blockedActorIds = blockedActorIds
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("notification_center_container")
    ) {
        // Summary Stats & Actions Card
        NotificationSummaryBanner(
            uiState = uiState,
            onMarkAllRead = onMarkAllRead,
            onClearAll = onClearAllNotifications
        )

        // Category Filter Chips Row
        val filterOptions = listOf(
            NotificationEventType.ALL to uiState.totalCount,
            NotificationEventType.LIKE to uiState.likesCount,
            NotificationEventType.FOLLOW to uiState.followsCount,
            NotificationEventType.COMMENT to uiState.commentsCount,
            NotificationEventType.MENTION to uiState.mentionsCount
        )

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("notification_filter_row"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filterOptions, key = { it.first.code }) { (filterType, count) ->
                val isSelected = activeFilter == filterType
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        if (onFilterChange != null) {
                            onFilterChange(filterType)
                        } else {
                            internalFilter = filterType
                        }
                    },
                    label = {
                        Text(
                            text = "${filterType.displayLabel} ($count)",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ElectricCoral.copy(alpha = 0.2f),
                        selectedLabelColor = ElectricCoral
                    ),
                    modifier = Modifier.testTag("notification_filter_${filterType.code}")
                )
            }
        }

        // Notification Events List View or Empty State
        if (uiState.filteredNotifications.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp)
                    .testTag("notifications_empty_state"),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsNone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Text(
                        text = stringResource(R.string.notifications_empty_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.notifications_empty_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("notification_list_view"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = uiState.filteredNotifications,
                    key = { it.id }
                ) { notification ->
                    NotificationEventItemCard(
                        notification = notification,
                        onClick = {
                            if (!notification.isRead) {
                                onMarkNotificationRead(notification.id)
                            }
                            onNotificationClick(notification)
                        },
                        onDelete = { onDeleteNotification(notification.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationSummaryBanner(
    uiState: NotificationCenterUiState,
    onMarkAllRead: () -> Unit,
    onClearAll: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("notification_summary_banner")
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            ElectricCoral.copy(alpha = 0.12f),
                            CyberViolet.copy(alpha = 0.12f),
                            PulseCyan.copy(alpha = 0.08f)
                        )
                    )
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SummaryMetricPill(
                        label = "Likes",
                        count = uiState.likesCount,
                        accentColor = ElectricCoral,
                        testTag = "notif_metric_likes"
                    )
                    SummaryMetricPill(
                        label = "Follows",
                        count = uiState.followsCount,
                        accentColor = PulseCyan,
                        testTag = "notif_metric_follows"
                    )
                    SummaryMetricPill(
                        label = "Comments",
                        count = uiState.commentsCount,
                        accentColor = CyberViolet,
                        testTag = "notif_metric_comments"
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (uiState.unreadCount > 0) {
                        TextButton(
                            onClick = onMarkAllRead,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("mark_notifications_read_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = stringResource(R.string.notifications_mark_all_read),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Read (${uiState.unreadCount})",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                    if (uiState.totalCount > 0) {
                        IconButton(
                            onClick = onClearAll,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("clear_all_notifications_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = stringResource(R.string.notifications_clear_all),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryMetricPill(
    label: String,
    count: Int,
    accentColor: Color,
    testTag: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(accentColor, CircleShape)
        )
        Text(
            text = "$count $label",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.testTag(testTag)
        )
    }
}

/**
 * Individual card component representing a single notification event (Like, Follow, Comment, Reply, Mention).
 */
@Composable
fun NotificationEventItemCard(
    notification: NotificationEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit = {}
) {
    val (badgeIcon, badgeColor, categoryLabel) = remember(notification.type) {
        resolveNotificationStyle(notification.type)
    }

    val containerColor by animateColorAsState(
        targetValue = if (notification.isRead) {
            MaterialTheme.colorScheme.surface
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
        },
        animationSpec = tween(durationMillis = 200),
        label = "notif_card_bg"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (!notification.isRead) {
                    Modifier.border(
                        width = 1.dp,
                        color = badgeColor.copy(alpha = 0.38f),
                        shape = RoundedCornerShape(16.dp)
                    )
                } else {
                    Modifier
                }
            )
            .clickable { onClick() }
            .testTag("notification_item_${notification.id}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Actor Avatar + Event Type Mini Badge
            Box(contentAlignment = Alignment.BottomEnd) {
                CreatorAvatar(
                    username = notification.actorUsername,
                    colorHex = notification.actorAvatarHex,
                    size = 46.dp
                )
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(badgeColor, CircleShape)
                        .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = badgeIcon,
                        contentDescription = notification.type,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            // Notification Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "@${notification.actorUsername}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = badgeColor.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = categoryLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = formatNotificationTimeAgo(notification.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = notification.messageText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (notification.isRead) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }

            // Unread Indicator Dot + Dismiss Action
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (!notification.isRead) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .background(ElectricCoral, CircleShape)
                            .testTag("notification_unread_dot_${notification.id}")
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(32.dp)
                        .testTag("delete_notification_${notification.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.notifications_delete_item),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

private fun resolveNotificationStyle(type: String): Triple<ImageVector, Color, String> {
    return when (type.uppercase()) {
        "FOLLOW" -> Triple(Icons.Default.PersonAdd, PulseCyan, "Follow")
        "LIKE" -> Triple(Icons.Default.Favorite, ElectricCoral, "Like")
        "REPLY" -> Triple(Icons.AutoMirrored.Filled.Reply, MintSuccess, "Reply")
        "MENTION" -> Triple(Icons.Default.AlternateEmail, NeonAmber, "Mention")
        else -> Triple(Icons.Default.ChatBubble, CyberViolet, "Comment")
    }
}

private fun formatNotificationTimeAgo(timestampMs: Long): String {
    val diffSec = ((System.currentTimeMillis() - timestampMs) / 1000L).coerceAtLeast(1L)
    return when {
        diffSec < 60L -> "Just now"
        diffSec < 3600L -> "${diffSec / 60L}m"
        diffSec < 86_400L -> "${diffSec / 3600L}h"
        else -> "${diffSec / 86_400L}d"
    }
}
