package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.BlockedUserEntity
import com.example.data.local.MessageEntity
import com.example.data.local.NotificationEntity
import com.example.data.local.NotificationEventType
import com.example.data.local.UserEntity
import com.example.ui.components.CreatorAvatar
import com.example.ui.components.NotificationListView
import com.example.ui.theme.CyberViolet
import com.example.ui.theme.ElectricCoral
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.PulseCyan

@Composable
fun InboxScreen(
    notifications: List<NotificationEntity>,
    messages: List<MessageEntity>,
    allUsers: List<UserEntity>,
    blockedUsers: List<BlockedUserEntity>,
    currentUser: UserEntity?,
    onMarkAllNotificationsRead: () -> Unit,
    onOpenChatThread: (Long) -> Unit,
    onOpenUserProfile: (Long) -> Unit,
    selectedNotificationFilter: NotificationEventType? = null,
    onNotificationFilterChange: ((NotificationEventType) -> Unit)? = null,
    onMarkNotificationRead: (Long) -> Unit = {},
    onDeleteNotification: (Long) -> Unit = {},
    onClearAllNotifications: () -> Unit = {}
) {
    var selectedSection by remember { mutableIntStateOf(0) } // 0 = Notifications, 1 = Direct Messages

    val blockedIds = remember(blockedUsers) { blockedUsers.map { it.blockedUserId }.toSet() }

    // Group messages by peerUserId for conversation list
    val conversations = remember(messages, blockedIds, currentUser) {
        messages
            .filter { it.peerUserId !in blockedIds }
            .groupBy { it.peerUserId }
            .map { (peerId, msgList) ->
                val latest = msgList.maxByOrNull { it.createdAt }!!
                val unreadCount = msgList.count { !it.isRead && it.receiverId == currentUser?.id }
                Triple(peerId, latest, unreadCount)
            }
            .sortedByDescending { it.second.createdAt }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // Top Header + Segmented Switcher
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Inbox & Direct Messages",
                    style = MaterialTheme.typography.headlineMedium
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { selectedSection = 0 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedSection == 0) ElectricCoral else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (selectedSection == 0) Color.White else MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("inbox_tab_notifications")
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Activity (${notifications.size})")
                }
                Button(
                    onClick = { selectedSection = 1 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedSection == 1) CyberViolet else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (selectedSection == 1) Color.White else MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("inbox_tab_messages")
                ) {
                    Icon(Icons.Default.ChatBubble, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Messages (${conversations.size})")
                }
            }
        }

        if (selectedSection == 0) {
            NotificationListView(
                notifications = notifications,
                selectedFilter = selectedNotificationFilter,
                onFilterChange = onNotificationFilterChange,
                blockedActorIds = blockedIds,
                onNotificationClick = { notif -> onOpenUserProfile(notif.actorId) },
                onMarkNotificationRead = onMarkNotificationRead,
                onMarkAllRead = onMarkAllNotificationsRead,
                onDeleteNotification = onDeleteNotification,
                onClearAllNotifications = onClearAllNotifications
            )
        } else {
            // Direct Messages Section: Quick Start Conversation Row + Conversation Threads
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Text(
                        text = "Start New Conversation",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        val otherCreators = allUsers.filter {
                            it.id != currentUser?.id &&
                                it.id !in blockedIds &&
                                it.accountStatus == "ACTIVE"
                        }
                        items(otherCreators, key = { it.id }) { user ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable { onOpenChatThread(user.id) }
                                    .testTag("start_chat_${user.id}")
                            ) {
                                CreatorAvatar(
                                    username = user.username,
                                    colorHex = user.avatarColorHex,
                                    size = 52.dp,
                                    isVerified = user.isVerified
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "@${user.username}",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }

                item {
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Recent Conversations",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                items(conversations, key = { it.first }) { (peerId, lastMsg, unread) ->
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenChatThread(peerId) }
                            .testTag("conversation_row_$peerId")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CreatorAvatar(
                                username = lastMsg.peerUsername,
                                colorHex = lastMsg.peerAvatarHex,
                                size = 50.dp
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = lastMsg.peerDisplayName,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    if (unread > 0) {
                                        Surface(
                                            shape = CircleShape,
                                            color = ElectricCoral
                                        ) {
                                            Text(
                                                text = unread.toString(),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "@${lastMsg.peerUsername}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = PulseCyan
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = lastMsg.content,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DirectMessageChatScreen(
    peerUser: UserEntity,
    currentUser: UserEntity,
    messages: List<MessageEntity>,
    onBack: () -> Unit,
    onSendMessage: (String) -> Unit,
    onBlockPeer: () -> Unit,
    onReportPeer: () -> Unit,
    onOpenPeerProfile: () -> Unit
) {
    BackHandler { onBack() }

    var draftText by remember { mutableStateOf("") }
    val threadMessages = remember(messages, peerUser.id) {
        messages.filter { it.peerUserId == peerUser.id }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // Top Chat Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    CreatorAvatar(
                        username = peerUser.username,
                        colorHex = peerUser.avatarColorHex,
                        size = 40.dp,
                        isVerified = peerUser.isVerified,
                        onAvatarClick = onOpenPeerProfile
                    )
                    Column(modifier = Modifier.clickable { onOpenPeerProfile() }) {
                        Text(
                            text = peerUser.displayName,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "@${peerUser.username}",
                            style = MaterialTheme.typography.labelMedium,
                            color = PulseCyan
                        )
                    }
                }

                Row {
                    IconButton(
                        onClick = onReportPeer,
                        modifier = Modifier.testTag("chat_report_user_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = "Report user",
                            tint = NeonAmber
                        )
                    }
                    IconButton(
                        onClick = onBlockPeer,
                        modifier = Modifier.testTag("chat_block_user_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = "Block user",
                            tint = ElectricCoral
                        )
                    }
                }
            }
        }

        // Message Bubble List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(threadMessages, key = { it.id }) { msg ->
                val isMine = msg.senderId == currentUser.id
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = if (isMine) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    Surface(
                        shape = RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (isMine) 18.dp else 4.dp,
                            bottomEnd = if (isMine) 4.dp else 18.dp
                        ),
                        color = if (isMine) ElectricCoral else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.widthIn(max = 280.dp)
                    ) {
                        Text(
                            text = msg.content,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isMine) Color.White else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }

        // Bottom Message Input Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = draftText,
                    onValueChange = { draftText = it },
                    placeholder = { Text("Message @${peerUser.username}...") },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_message_input")
                )
                IconButton(
                    onClick = {
                        if (draftText.isNotBlank()) {
                            onSendMessage(draftText)
                            draftText = ""
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(ElectricCoral, CircleShape)
                        .testTag("chat_send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send message",
                        tint = Color.White
                    )
                }
            }
        }
    }
}
