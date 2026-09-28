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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PeopleOutline
import androidx.compose.material.icons.filled.PersonAddAlt1
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.CreatorFollowUiState
import com.example.data.local.FollowRelationshipStatus
import com.example.data.local.FollowedCreatorInfo
import com.example.ui.theme.CyberViolet
import com.example.ui.theme.ElectricCoral
import com.example.ui.theme.MintSuccess
import com.example.ui.theme.PulseCyan

enum class FollowDirectoryTab {
    FOLLOWING,
    FOLLOWERS,
    SUGGESTED
}

/**
 * Interactive Follow / Unfollow button reflecting the current [FollowRelationshipStatus]
 * between the active user and a target creator.
 */
@Composable
fun CreatorFollowActionButton(
    creatorId: Long,
    relationshipStatus: FollowRelationshipStatus,
    onToggleFollow: (Long) -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "follow_action_button_$creatorId"
) {
    if (relationshipStatus == FollowRelationshipStatus.SELF) return

    val isFollowing = relationshipStatus.isFollowingCreator
    val containerColor by animateColorAsState(
        targetValue = when (relationshipStatus) {
            FollowRelationshipStatus.MUTUAL_FRIENDS -> CyberViolet.copy(alpha = 0.25f)
            FollowRelationshipStatus.FOLLOWING -> MaterialTheme.colorScheme.surfaceVariant
            FollowRelationshipStatus.FOLLOWED_BY -> ElectricCoral
            FollowRelationshipStatus.NOT_FOLLOWING -> ElectricCoral
            FollowRelationshipStatus.SELF -> Color.Transparent
        },
        animationSpec = tween(durationMillis = 180),
        label = "follow_btn_container"
    )

    val contentColor by animateColorAsState(
        targetValue = when (relationshipStatus) {
            FollowRelationshipStatus.MUTUAL_FRIENDS -> PulseCyan
            FollowRelationshipStatus.FOLLOWING -> MaterialTheme.colorScheme.onSurface
            else -> Color.White
        },
        animationSpec = tween(durationMillis = 180),
        label = "follow_btn_content"
    )

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(18.dp))
            .border(
                width = 1.dp,
                color = if (isFollowing) {
                    if (relationshipStatus == FollowRelationshipStatus.MUTUAL_FRIENDS) {
                        PulseCyan.copy(alpha = 0.55f)
                    } else {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                    }
                } else {
                    Color.Transparent
                },
                shape = RoundedCornerShape(18.dp)
            )
            .clickable { onToggleFollow(creatorId) }
            .testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = if (isFollowing) Icons.Default.Check else Icons.Default.PersonAddAlt1,
                contentDescription = if (isFollowing) {
                    stringResource(R.string.action_unfollow)
                } else {
                    stringResource(R.string.action_follow)
                },
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = relationshipStatus.actionButtonLabel,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = contentColor
            )
        }
    }
}

/**
 * Horizontal tracker strip displaying the creators currently followed by the user,
 * with quick unfollow/follow toggles and an option to open the full directory modal.
 */
@Composable
fun FollowedCreatorsTrackerSection(
    followUiState: CreatorFollowUiState,
    onToggleFollow: (Long) -> Unit,
    onOpenCreatorProfile: (Long) -> Unit,
    onOpenFullDirectory: (FollowDirectoryTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
            .fillMaxWidth()
            .testTag("followed_creators_tracker_section")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = null,
                        tint = PulseCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "${stringResource(R.string.followed_creators_title)} (${followUiState.followingCount})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.testTag("followed_creators_header_count")
                    )
                }

                TextButton(
                    onClick = { onOpenFullDirectory(FollowDirectoryTab.FOLLOWING) },
                    modifier = Modifier.testTag("open_followed_directory_button")
                ) {
                    Text(
                        text = "Manage All",
                        style = MaterialTheme.typography.labelLarge,
                        color = PulseCyan
                    )
                }
            }

            if (followUiState.followedCreators.isEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("followed_creators_empty_state"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.followed_creators_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    if (followUiState.suggestedCreators.isNotEmpty()) {
                        Button(
                            onClick = { onOpenFullDirectory(FollowDirectoryTab.SUGGESTED) },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricCoral),
                            modifier = Modifier.testTag("discover_suggested_creators_button")
                        ) {
                            Text("Discover")
                        }
                    }
                }
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.testTag("followed_creators_row")
                ) {
                    items(followUiState.followedCreators, key = { it.creatorId }) { info ->
                        TrackedCreatorCard(
                            info = info,
                            onOpenProfile = { onOpenCreatorProfile(info.creatorId) },
                            onToggleFollow = { onToggleFollow(info.creatorId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackedCreatorCard(
    info: FollowedCreatorInfo,
    onOpenProfile: () -> Unit,
    onToggleFollow: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        modifier = Modifier
            .width(150.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onOpenProfile() }
            .testTag("tracked_creator_card_${info.creatorId}")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(12.dp)
        ) {
            CreatorAvatar(
                username = info.creator.username,
                colorHex = info.creator.avatarColorHex,
                avatarUri = info.creator.avatarUri,
                size = 48.dp,
                isVerified = info.creator.isVerified,
                onAvatarClick = onOpenProfile
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = info.creator.displayName,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (info.creator.isVerified) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = "Verified",
                        tint = PulseCyan,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Text(
                text = "@${info.creator.username}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            CreatorFollowActionButton(
                creatorId = info.creatorId,
                relationshipStatus = info.relationshipStatus,
                onToggleFollow = { onToggleFollow() },
                testTag = "tracked_creator_follow_btn_${info.creatorId}"
            )
        }
    }
}

/**
 * Bottom sheet modal for viewing and managing Followed Creators, Followers, and Suggested Creators.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowedCreatorsBottomSheet(
    followUiState: CreatorFollowUiState,
    initialTab: FollowDirectoryTab = FollowDirectoryTab.FOLLOWING,
    onDismiss: () -> Unit,
    onToggleFollow: (Long) -> Unit,
    onOpenCreatorProfile: (Long) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.testTag("followed_creators_bottom_sheet")
    ) {
        FollowedCreatorsSheetContent(
            followUiState = followUiState,
            initialTab = initialTab,
            onDismiss = onDismiss,
            onToggleFollow = onToggleFollow,
            onOpenCreatorProfile = onOpenCreatorProfile
        )
    }
}

@Composable
fun FollowedCreatorsSheetContent(
    followUiState: CreatorFollowUiState,
    initialTab: FollowDirectoryTab = FollowDirectoryTab.FOLLOWING,
    onDismiss: () -> Unit,
    onToggleFollow: (Long) -> Unit,
    onOpenCreatorProfile: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(initialTab) }
    var searchFilter by remember { mutableStateOf("") }

    val baseList = when (selectedTab) {
        FollowDirectoryTab.FOLLOWING -> followUiState.followedCreators
        FollowDirectoryTab.FOLLOWERS -> followUiState.followerCreators
        FollowDirectoryTab.SUGGESTED -> followUiState.suggestedCreators
    }

    val filteredCreators = remember(baseList, searchFilter) {
        val trimmed = searchFilter.trim()
        if (trimmed.isBlank()) baseList
        else baseList.filter {
            it.creator.username.contains(trimmed, ignoreCase = true) ||
                it.creator.displayName.contains(trimmed, ignoreCase = true) ||
                it.creator.bio.contains(trimmed, ignoreCase = true)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.8f)
            .padding(horizontal = 16.dp)
            .testTag("followed_creators_sheet_content")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Creator Network",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "${followUiState.followingCount} Following • ${followUiState.followersCount} Followers",
                    style = MaterialTheme.typography.labelMedium,
                    color = PulseCyan,
                    modifier = Modifier.testTag("follow_network_summary_text")
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("close_followed_sheet_button")
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close creator network")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Directory Tabs: Following | Followers | Suggested
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedTab == FollowDirectoryTab.FOLLOWING,
                onClick = { selectedTab = FollowDirectoryTab.FOLLOWING },
                label = { Text("Following (${followUiState.followingCount})") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("follow_tab_following")
            )
            FilterChip(
                selected = selectedTab == FollowDirectoryTab.FOLLOWERS,
                onClick = { selectedTab = FollowDirectoryTab.FOLLOWERS },
                label = { Text("Followers (${followUiState.followersCount})") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("follow_tab_followers")
            )
            FilterChip(
                selected = selectedTab == FollowDirectoryTab.SUGGESTED,
                onClick = { selectedTab = FollowDirectoryTab.SUGGESTED },
                label = { Text("Discover (${followUiState.suggestedCreators.size})") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("follow_tab_suggested")
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = searchFilter,
            onValueChange = { searchFilter = it },
            placeholder = { Text("Filter creators by name or @handle...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("follow_directory_search_input")
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

        if (filteredCreators.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("follow_directory_empty_state"),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PeopleOutline,
                        contentDescription = null,
                        tint = CyberViolet,
                        modifier = Modifier.size(44.dp)
                    )
                    Text(
                        text = when (selectedTab) {
                            FollowDirectoryTab.FOLLOWING -> stringResource(R.string.followed_creators_empty)
                            FollowDirectoryTab.FOLLOWERS -> "No followers match this view yet."
                            FollowDirectoryTab.SUGGESTED -> "You are following all active creators!"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("follow_directory_list"),
                contentPadding = PaddingValues(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredCreators, key = { it.creatorId }) { info ->
                    FollowedCreatorRowItem(
                        info = info,
                        onOpenProfile = { onOpenCreatorProfile(info.creatorId) },
                        onToggleFollow = { onToggleFollow(info.creatorId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FollowedCreatorRowItem(
    info: FollowedCreatorInfo,
    onOpenProfile: () -> Unit,
    onToggleFollow: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onOpenProfile() }
            .testTag("follow_directory_row_${info.creatorId}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CreatorAvatar(
                username = info.creator.username,
                colorHex = info.creator.avatarColorHex,
                avatarUri = info.creator.avatarUri,
                size = 48.dp,
                isVerified = info.creator.isVerified,
                onAvatarClick = onOpenProfile
            )

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = info.creator.displayName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (info.isMutual) {
                        Surface(
                            color = MintSuccess.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.follow_status_friends),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MintSuccess,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else if (info.relationshipStatus == FollowRelationshipStatus.FOLLOWED_BY) {
                        Surface(
                            color = PulseCyan.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.follow_status_follows_you),
                                style = MaterialTheme.typography.labelSmall,
                                color = PulseCyan,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Text(
                    text = "@${info.creator.username} • ${formatCompactCount(info.creator.followersCount)} followers",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (info.creator.bio.isNotBlank()) {
                    Text(
                        text = info.creator.bio,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            CreatorFollowActionButton(
                creatorId = info.creatorId,
                relationshipStatus = info.relationshipStatus,
                onToggleFollow = { onToggleFollow() },
                testTag = "directory_follow_btn_${info.creatorId}"
            )
        }
    }
}
