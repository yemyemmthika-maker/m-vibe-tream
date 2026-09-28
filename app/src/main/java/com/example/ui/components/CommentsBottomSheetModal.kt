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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.CommentEntity
import com.example.data.local.UserEntity
import com.example.data.local.VideoEntity
import com.example.ui.theme.CyberViolet
import com.example.ui.theme.ElectricCoral
import com.example.ui.theme.PulseCyan

enum class CommentSortOrder {
    TOP,
    NEWEST
}

private val QUICK_REACTION_EMOJIS = listOf("🔥", "❤️", "👏", "⚡", "✨", "🙌")

/**
 * Bottom sheet modal component that opens when the comment icon is clicked on a video,
 * allowing users to view and add comments for that specific video.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsBottomSheet(
    video: VideoEntity,
    comments: List<CommentEntity>,
    currentUser: UserEntity?,
    onDismiss: () -> Unit,
    onSubmitComment: (String, CommentEntity?) -> Unit,
    onToggleLikeComment: (CommentEntity) -> Unit,
    onDeleteComment: (CommentEntity) -> Unit,
    onReportComment: (CommentEntity) -> Unit,
    onOpenUserProfile: (Long) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.testTag("comments_bottom_sheet_${video.id}")
    ) {
        CommentsSheetContent(
            video = video,
            comments = comments,
            currentUser = currentUser,
            onDismiss = onDismiss,
            onSubmitComment = onSubmitComment,
            onToggleLikeComment = onToggleLikeComment,
            onDeleteComment = onDeleteComment,
            onReportComment = onReportComment,
            onOpenUserProfile = onOpenUserProfile
        )
    }
}

/**
 * Interactive content inside the Comments Bottom Sheet modal for a specific video.
 */
@Composable
fun CommentsSheetContent(
    video: VideoEntity,
    comments: List<CommentEntity>,
    currentUser: UserEntity?,
    onDismiss: () -> Unit,
    onSubmitComment: (String, CommentEntity?) -> Unit,
    onToggleLikeComment: (CommentEntity) -> Unit,
    onDeleteComment: (CommentEntity) -> Unit,
    onReportComment: (CommentEntity) -> Unit,
    onOpenUserProfile: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var commentText by remember(video.id) { mutableStateOf("") }
    var replyingTo by remember(video.id) { mutableStateOf<CommentEntity?>(null) }
    var sortOrder by remember(video.id) { mutableStateOf(CommentSortOrder.NEWEST) }

    val videoComments = comments.filter { it.videoId == video.id }

    val topLevelComments = remember(videoComments, sortOrder) {
        val roots = videoComments.filter { it.parentCommentId == null }
        when (sortOrder) {
            CommentSortOrder.TOP -> roots.sortedWith(
                compareByDescending<CommentEntity> { it.likesCount }
                    .thenByDescending { it.createdAt }
                    .thenByDescending { it.id }
            )
            CommentSortOrder.NEWEST -> roots.sortedWith(
                compareByDescending<CommentEntity> { it.createdAt }
                    .thenByDescending { it.id }
            )
        }
    }

    val repliesByParent = remember(videoComments) {
        videoComments
            .filter { it.parentCommentId != null }
            .sortedBy { it.createdAt }
            .groupBy { it.parentCommentId }
    }

    val submitCurrentDraft = {
        val trimmed = commentText.trim()
        if (trimmed.isNotEmpty()) {
            onSubmitComment(trimmed, replyingTo)
            commentText = ""
            replyingTo = null
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.78f)
            .padding(horizontal = 16.dp)
            .testTag("comments_sheet_content_${video.id}")
    ) {
        // Header Row: Comment count, Sort toggle (Top / Newest), and Close button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.comments_sheet_title, videoComments.size),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.testTag("comments_count_header_${video.id}")
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayCircleOutline,
                        contentDescription = null,
                        tint = PulseCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "@${video.creatorUsername} • ${video.caption}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .width(210.dp)
                            .testTag("comments_video_context_${video.id}")
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CommentSortChip(
                    label = stringResource(R.string.comments_sort_top),
                    selected = sortOrder == CommentSortOrder.TOP,
                    testTag = "sort_comments_top",
                    onClick = { sortOrder = CommentSortOrder.TOP }
                )
                CommentSortChip(
                    label = stringResource(R.string.comments_sort_newest),
                    selected = sortOrder == CommentSortOrder.NEWEST,
                    testTag = "sort_comments_newest",
                    onClick = { sortOrder = CommentSortOrder.NEWEST }
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("close_comments_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.comments_close_action)
                    )
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Comments List or Empty State
        if (topLevelComments.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("comments_empty_state"),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = null,
                        tint = CyberViolet,
                        modifier = Modifier.size(44.dp)
                    )
                    Text(
                        text = stringResource(R.string.comments_empty_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.comments_empty_subtitle, video.creatorUsername),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("comments_list_${video.id}"),
                contentPadding = PaddingValues(vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(topLevelComments, key = { it.id }) { comment ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CommentRowItem(
                            comment = comment,
                            isVideoCreator = comment.authorId == video.creatorId,
                            isOwnComment = currentUser?.id == comment.authorId,
                            isReply = false,
                            onReplyClick = { replyingTo = comment },
                            onLikeClick = { onToggleLikeComment(comment) },
                            onDeleteClick = { onDeleteComment(comment) },
                            onReportClick = { onReportComment(comment) },
                            onAuthorClick = { onOpenUserProfile(comment.authorId) }
                        )
                        val replies = repliesByParent[comment.id].orEmpty()
                        replies.forEach { reply ->
                            Box(modifier = Modifier.padding(start = 40.dp)) {
                                CommentRowItem(
                                    comment = reply,
                                    isVideoCreator = reply.authorId == video.creatorId,
                                    isOwnComment = currentUser?.id == reply.authorId,
                                    isReply = true,
                                    onReplyClick = { replyingTo = comment },
                                    onLikeClick = { onToggleLikeComment(reply) },
                                    onDeleteClick = { onDeleteComment(reply) },
                                    onReportClick = { onReportComment(reply) },
                                    onAuthorClick = { onOpenUserProfile(reply.authorId) }
                                )
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(top = 6.dp))

        // Replying-to context pill
        if (replyingTo != null) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .testTag("replying_to_banner")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Replying to @${replyingTo?.authorUsername}",
                        style = MaterialTheme.typography.labelLarge,
                        color = PulseCyan
                    )
                    IconButton(
                        onClick = { replyingTo = null },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("cancel_reply_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel reply",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Quick reaction emoji bar for fast engagement
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            QUICK_REACTION_EMOJIS.forEachIndexed { index, emoji ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            commentText = if (commentText.isBlank()) emoji else "$commentText $emoji"
                        }
                        .testTag("quick_emoji_$index")
                ) {
                    Text(
                        text = emoji,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Comment Composer Row: Current user avatar + TextField + Send button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (currentUser != null) {
                CreatorAvatar(
                    username = currentUser.username,
                    colorHex = currentUser.avatarColorHex,
                    size = 38.dp
                )
            }

            OutlinedTextField(
                value = commentText,
                onValueChange = { commentText = it },
                placeholder = {
                    Text(
                        text = if (replyingTo != null) {
                            stringResource(
                                R.string.comments_reply_placeholder,
                                replyingTo?.authorUsername.orEmpty()
                            )
                        } else {
                            stringResource(
                                R.string.comments_input_placeholder,
                                video.creatorUsername
                            )
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("comment_input_field"),
                shape = RoundedCornerShape(24.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { submitCurrentDraft() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricCoral,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )

            val canSubmit = commentText.isNotBlank()
            val sendBgColor by animateColorAsState(
                targetValue = if (canSubmit) ElectricCoral else MaterialTheme.colorScheme.surfaceVariant,
                animationSpec = tween(durationMillis = 150),
                label = "send_comment_bg"
            )

            IconButton(
                onClick = { submitCurrentDraft() },
                enabled = canSubmit,
                modifier = Modifier
                    .size(48.dp)
                    .minimumInteractiveComponentSize()
                    .background(sendBgColor, CircleShape)
                    .testTag("send_comment_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(R.string.comments_post_action),
                    tint = if (canSubmit) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CommentSortChip(
    label: String,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) CyberViolet.copy(alpha = 0.24f) else Color.Transparent,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = 1.dp,
                color = if (selected) CyberViolet else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (selected) PulseCyan else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun CommentRowItem(
    comment: CommentEntity,
    isVideoCreator: Boolean,
    isOwnComment: Boolean,
    isReply: Boolean,
    onReplyClick: () -> Unit,
    onLikeClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onReportClick: () -> Unit,
    onAuthorClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("comment_item_${comment.id}"),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        CreatorAvatar(
            username = comment.authorUsername,
            colorHex = comment.authorAvatarHex,
            size = if (isReply) 30.dp else 36.dp,
            onAvatarClick = onAuthorClick
        )
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "@${comment.authorUsername}",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { onAuthorClick() }
                )
                if (isVideoCreator) {
                    Surface(
                        color = ElectricCoral.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.comments_creator_badge),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = ElectricCoral,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }
                if (comment.replyToUsername != null) {
                    Text(
                        text = "▸ @${comment.replyToUsername}",
                        style = MaterialTheme.typography.labelMedium,
                        color = PulseCyan
                    )
                }
                Text(
                    text = formatRelativeCommentTime(comment.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.68f)
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = comment.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag("comment_text_${comment.id}")
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { onReplyClick() }
                        .padding(vertical = 2.dp)
                        .testTag("comment_reply_button_${comment.id}")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Reply,
                        contentDescription = "Reply",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Reply",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isOwnComment) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { onDeleteClick() }
                            .padding(vertical = 2.dp)
                            .testTag("comment_delete_button_${comment.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete comment",
                            modifier = Modifier.size(14.dp),
                            tint = ElectricCoral
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Delete",
                            style = MaterialTheme.typography.labelMedium,
                            color = ElectricCoral
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { onReportClick() }
                            .padding(vertical = 2.dp)
                            .testTag("comment_report_button_${comment.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = "Report comment",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Report",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .clip(RoundedCornerShape(10.dp))
                .clickable { onLikeClick() }
                .padding(4.dp)
                .testTag("comment_like_button_${comment.id}")
        ) {
            Icon(
                imageVector = if (comment.isLikedByCurrentUser) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "Like comment",
                tint = if (comment.isLikedByCurrentUser) ElectricCoral else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = comment.likesCount.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatRelativeCommentTime(createdAtMillis: Long): String {
    val diffSec = ((System.currentTimeMillis() - createdAtMillis) / 1000L).coerceAtLeast(0L)
    return when {
        diffSec < 60L -> "Just now"
        diffSec < 3600L -> "${diffSec / 60L}m"
        diffSec < 86_400L -> "${diffSec / 3600L}h"
        else -> "${diffSec / 86_400L}d"
    }
}
