package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.engine.SearchAndDiscoveryEngine
import com.example.data.local.CreatorFollowUiState
import com.example.data.local.HashtagEntity
import com.example.data.local.SoundEntity
import com.example.data.local.UserEntity
import com.example.data.local.VideoEntity
import com.example.ui.components.CreatorAvatar
import com.example.ui.components.VideoDurationCornerOverlay
import com.example.ui.components.formatCompactCount
import com.example.ui.components.formatCompactLong
import com.example.ui.components.parseHexColor
import com.example.ui.theme.CyberViolet
import com.example.ui.theme.ElectricCoral
import com.example.ui.theme.MintSuccess
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.PulseCyan

/**
 * Search screen component with a top search bar for querying users, hashtags, and videos,
 * while prominently displaying suggested creators and trending hashtags.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiscoverSearchScreen(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    users: List<UserEntity>,
    videos: List<VideoEntity>,
    hashtags: List<HashtagEntity>,
    sounds: List<SoundEntity>,
    followingUserIds: Set<Long>,
    currentUserId: Long?,
    onToggleFollow: (Long) -> Unit,
    onOpenUserProfile: (Long) -> Unit,
    onSelectHashtag: (String) -> Unit,
    onSelectSound: (SoundEntity) -> Unit,
    onPlayVideoInFeed: (VideoEntity) -> Unit,
    followUiState: CreatorFollowUiState = CreatorFollowUiState()
) {
    val focusManager = LocalFocusManager.current
    var selectedCategory by remember { mutableIntStateOf(0) }
    val categories = listOf("All Pulse", "Videos", "Creators", "Hashtags", "Sounds")

    val queryTrimmed = searchQuery.trim()

    val filteredUsers = remember(users, queryTrimmed, currentUserId) {
        SearchAndDiscoveryEngine.searchAndRankUsers(
            users = users,
            rawQuery = queryTrimmed,
            currentUserId = currentUserId
        )
    }

    val filteredHashtags = remember(hashtags, videos, queryTrimmed) {
        SearchAndDiscoveryEngine.searchAndRankHashtags(
            hashtags = hashtags,
            videos = videos,
            rawQuery = queryTrimmed
        )
    }

    val filteredVideos = remember(videos, queryTrimmed) {
        SearchAndDiscoveryEngine.searchAndRankVideos(
            videos = videos,
            rawQuery = queryTrimmed
        )
    }

    val filteredSounds = remember(sounds, queryTrimmed) {
        SearchAndDiscoveryEngine.searchAndRankSounds(
            sounds = sounds,
            rawQuery = queryTrimmed
        )
    }

    val hasAnyResults = filteredUsers.isNotEmpty() ||
        filteredHashtags.isNotEmpty() ||
        filteredVideos.isNotEmpty() ||
        filteredSounds.isNotEmpty()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .testTag("discover_search_screen"),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Top Search Bar Header for querying users, hashtags, and videos
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.search_screen_title),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                )

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = {
                        Text(
                            text = stringResource(R.string.search_bar_placeholder),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = PulseCyan
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { onSearchQueryChange("") },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("clear_search_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search"
                                )
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricCoral,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("discover_search_input")
                )

                // Active Search Query Result Summary
                if (queryTrimmed.isNotBlank()) {
                    Surface(
                        color = CyberViolet.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Results for \"$queryTrimmed\": ${filteredUsers.size} users • ${filteredHashtags.size} hashtags • ${filteredVideos.size} videos",
                            style = MaterialTheme.typography.labelLarge,
                            color = PulseCyan,
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("search_query_summary")
                        )
                    }
                }

                // Category Filter Chips
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories.size) { index ->
                        FilterChip(
                            selected = selectedCategory == index,
                            onClick = { selectedCategory = index },
                            label = { Text(categories[index]) },
                            modifier = Modifier.testTag("discover_category_$index")
                        )
                    }
                }
            }
        }

        // Empty State when a search query matches nothing
        if (queryTrimmed.isNotBlank() && !hasAnyResults) {
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("search_no_results_state")
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = null,
                            tint = ElectricCoral,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = stringResource(R.string.search_no_results_title),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "No users, #hashtags, or videos matched \"$queryTrimmed\". Try searching for another creator or tag.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = { onSearchQueryChange("") },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricCoral),
                            modifier = Modifier.testTag("reset_search_button")
                        ) {
                            Text("Reset Search")
                        }
                    }
                }
            }
        }

        // Suggested Creators Section (shown in All Pulse and Creators tabs)
        if ((selectedCategory == 0 || selectedCategory == 2) && filteredUsers.isNotEmpty()) {
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.testTag("suggested_creators_section")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (queryTrimmed.isBlank()) {
                                stringResource(R.string.search_suggested_creators)
                            } else {
                                "Matching Creators (${filteredUsers.size})"
                            },
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${filteredUsers.size} creators",
                            style = MaterialTheme.typography.labelMedium,
                            color = PulseCyan
                        )
                    }

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.testTag("suggested_creators_row")
                    ) {
                        items(filteredUsers, key = { it.id }) { creator ->
                            val isFollowing = creator.id in followingUserIds || followUiState.isFollowing(creator.id)
                            val isMutual = followUiState.isMutualFriend(creator.id)
                            val followsMe = followUiState.isFollowedBy(creator.id)
                            val isMe = creator.id == currentUserId

                            SuggestedCreatorCard(
                                creator = creator,
                                isMe = isMe,
                                isFollowing = isFollowing,
                                isMutual = isMutual,
                                followsMe = followsMe,
                                onOpenProfile = { onOpenUserProfile(creator.id) },
                                onToggleFollow = { onToggleFollow(creator.id) }
                            )
                        }
                    }
                }
            }
        }

        // Trending Hashtags Section (shown in All Pulse and Hashtags tabs)
        if ((selectedCategory == 0 || selectedCategory == 3) && filteredHashtags.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("trending_hashtags_section"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = NeonAmber,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = if (queryTrimmed.isBlank()) {
                                stringResource(R.string.search_trending_hashtags)
                            } else {
                                "Matching Hashtags (${filteredHashtags.size})"
                            },
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    Text(
                        text = "Tap to filter feed",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            itemsIndexed(filteredHashtags, key = { _, item -> item.tag }) { index, hashtag ->
                TrendingHashtagRow(
                    rank = index + 1,
                    hashtag = hashtag,
                    onClick = { onSelectHashtag(hashtag.displayTag) }
                )
            }
        }

        // Featured Hero Banner & Quick Search Chips when browsing All Pulse with no query
        if (queryTrimmed.isBlank() && selectedCategory == 0) {
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(172.dp)
                        .padding(horizontal = 16.dp)
                        .clickable { onSelectHashtag("#NeonPulse") }
                        .testTag("discover_hero_banner")
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            painter = painterResource(id = R.drawable.img_discover_banner_1790496612399),
                            contentDescription = "Featured Creator Challenge Banner",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color.Black.copy(alpha = 0.85f),
                                            Color.Black.copy(alpha = 0.35f)
                                        )
                                    )
                                )
                                .padding(20.dp)
                        ) {
                            Column(
                                modifier = Modifier.align(Alignment.BottomStart),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    color = ElectricCoral,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "TRENDING CHALLENGE",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Text(
                                    text = "#NeonPulse Vertical Cinema",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = Color.White
                                )
                                Text(
                                    text = "18.4M views • Tap to watch featured entries",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }
            }

            // Quick Search Suggestion Chips
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Popular Search Queries",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    val trendingTerms = listOf(
                        "rooftop choreography",
                        "modular synth",
                        "#ShaderArt",
                        "35mm street grade",
                        "@kai.motion",
                        "126 BPM"
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        trendingTerms.forEachIndexed { idx, term ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { onSearchQueryChange(term) }
                                    .testTag("popular_query_chip_$idx")
                            ) {
                                Text(
                                    text = term,
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Trending / Matching Videos Grid
        if ((selectedCategory == 0 || selectedCategory == 1) && filteredVideos.isNotEmpty()) {
            item {
                Text(
                    text = if (queryTrimmed.isBlank()) {
                        stringResource(R.string.search_trending_videos)
                    } else {
                        "Matching Videos (${filteredVideos.size})"
                    },
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .testTag("trending_videos_section")
                )
            }
            items(filteredVideos.chunked(2)) { pair ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    pair.forEach { video ->
                        DiscoverVideoCard(
                            video = video,
                            modifier = Modifier.weight(1f),
                            onClick = { onPlayVideoInFeed(video) }
                        )
                    }
                    if (pair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // Popular Sounds Section
        if ((selectedCategory == 0 || selectedCategory == 4) && filteredSounds.isNotEmpty()) {
            item {
                Text(
                    text = if (queryTrimmed.isBlank()) {
                        "Popular Sounds & Stems"
                    } else {
                        "Matching Sounds (${filteredSounds.size})"
                    },
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            items(filteredSounds, key = { it.id }) { sound ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onSelectSound(sound) }
                        .testTag("search_sound_item_${sound.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(parseHexColor(sound.coverColorHex), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = sound.title,
                                    tint = Color.White
                                )
                            }
                            Column {
                                Text(
                                    text = sound.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${sound.artist} • ${sound.bpm} BPM • ${sound.durationSec}s",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Text(
                            text = "${formatCompactCount(sound.usageCount)} reels",
                            style = MaterialTheme.typography.labelLarge,
                            color = ElectricCoral
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestedCreatorCard(
    creator: UserEntity,
    isMe: Boolean,
    isFollowing: Boolean,
    isMutual: Boolean,
    followsMe: Boolean,
    onOpenProfile: () -> Unit,
    onToggleFollow: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .width(168.dp)
            .clickable { onOpenProfile() }
            .testTag("suggested_creator_card_${creator.id}")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CreatorAvatar(
                username = creator.username,
                colorHex = creator.avatarColorHex,
                avatarUri = creator.avatarUri,
                size = 56.dp,
                isVerified = creator.isVerified,
                onAvatarClick = onOpenProfile
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = creator.displayName,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (creator.isVerified) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = "Verified",
                        tint = PulseCyan,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Text(
                text = "@${creator.username}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${formatCompactCount(creator.followersCount)} followers",
                style = MaterialTheme.typography.labelMedium,
                color = PulseCyan
            )
            if (!isMe) {
                val buttonLabel = when {
                    isFollowing && isMutual -> "Friends"
                    isFollowing -> "Following"
                    followsMe -> "Follow Back"
                    else -> "Follow"
                }
                Button(
                    onClick = onToggleFollow,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when {
                            isFollowing && isMutual -> CyberViolet.copy(alpha = 0.35f)
                            isFollowing -> MaterialTheme.colorScheme.surfaceVariant
                            else -> ElectricCoral
                        }
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("suggested_creator_follow_button_${creator.id}")
                ) {
                    Text(
                        text = buttonLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isFollowing && isMutual) MintSuccess else Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun TrendingHashtagRow(
    rank: Int,
    hashtag: HashtagEntity,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("trending_hashtag_item_${hashtag.tag}")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(CyberViolet.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Tag,
                        contentDescription = hashtag.displayTag,
                        tint = CyberViolet
                    )
                }
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = hashtag.displayTag,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        if (hashtag.isTrending) {
                            Surface(
                                color = ElectricCoral.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "#$rank Trending",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = ElectricCoral,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "${hashtag.category} • ${formatCompactCount(hashtag.videosCount)} videos",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "${formatCompactLong(hashtag.viewsCount)} views",
                    style = MaterialTheme.typography.labelLarge,
                    color = PulseCyan,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}

@Composable
private fun DiscoverVideoCard(
    video: VideoEntity,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val accent = parseHexColor(video.creatorAvatarHex)
    Card(
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .height(230.dp)
            .clickable { onClick() }
            .testTag("search_video_card_${video.id}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF1A142E),
                            accent.copy(alpha = 0.55f),
                            Color(0xFF0A0812)
                        )
                    )
                )
                .padding(12.dp)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.45f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Views",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = formatCompactCount(video.viewsCount),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                }
            }

            VideoDurationCornerOverlay(
                durationSec = video.durationSec,
                videoId = video.id,
                testTagPrefix = "discover_video_duration",
                modifier = Modifier.align(Alignment.TopEnd)
            )

            Text(
                text = video.thumbnailHeadline.ifBlank { "PULSE CLIP" },
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.align(Alignment.Center)
            )

            Column(
                modifier = Modifier.align(Alignment.BottomStart),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = video.caption,
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "@${video.creatorUsername}",
                        style = MaterialTheme.typography.labelMedium,
                        color = PulseCyan
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Likes",
                            tint = ElectricCoral,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = formatCompactCount(video.likesCount),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
