package com.example.kiparys.ui.projectgallery

import android.os.Build
import android.view.Window
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.getValue
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.areStatusBarsVisible
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.imageLoader
import coil.request.ImageRequest
import com.example.kiparys.ui.projectdetails.ProjectDetailsViewModel
import com.example.kiparys.util.StringUtil.formatMediaUploadedTimestamp
import me.saket.telephoto.zoomable.coil.ZoomableAsyncImage
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.example.kiparys.R


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GalleryScreen(
    window: Window,
    projectDetailsViewModel: ProjectDetailsViewModel,
    onBack: () -> Unit
) {
    val projectGalleryUiState by projectDetailsViewModel.projectGalleryScreenUiState.collectAsStateWithLifecycle()
    val mediaUiState by projectDetailsViewModel.projectMedia.collectAsStateWithLifecycle()
    var isStatusBarVisible: Boolean by remember { mutableStateOf(true) }
    val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)

    WindowCompat.setDecorFitsSystemWindows(window, false)
    windowInsetsController.systemBarsBehavior =
        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    if (Build.VERSION.SDK_INT >= 30) {
        window.attributes.layoutInDisplayCutoutMode =
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        window.isNavigationBarContrastEnforced = false
    }

    isStatusBarVisible = WindowInsets.areStatusBarsVisible
    LaunchedEffect(projectGalleryUiState.showSystemBars) {
        if (projectGalleryUiState.showSystemBars) {
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        } else {
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim)
    ) {
        if (mediaUiState.projectMedia.isNotEmpty() && projectGalleryUiState.initMediaIndex != null) {

            val mediaList = mediaUiState.projectMedia
            val initialIndex = projectGalleryUiState.initMediaIndex ?: 0

            val pagerState = rememberPagerState(
                initialPage = initialIndex,
                pageCount = { mediaList.size }
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                pageSpacing = 16.dp
            ) { page ->
                val media = mediaList[page]
                ZoomableAsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(media.mediaUrl)
                        .placeholderMemoryCacheKey(media.mediaUrl)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    onClick = { projectDetailsViewModel.updateSystemBars() },
                    imageLoader = context.imageLoader
                )
            }

            val currentImageIndex = pagerState.currentPage + 1
            val totalImages = mediaList.size

            Column(Modifier.fillMaxWidth()) {
                Row {
                    AnimatedVisibility(
                        visible = projectGalleryUiState.showSystemBars,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        TopAppBar(
                            title = {
                                val media = mediaList[pagerState.currentPage]
                                Column(modifier = Modifier.widthIn(max = 200.dp)) {
                                    media.uploaderName?.let {
                                        Text(
                                            text = it,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Color.White,
                                        )
                                    }
                                    media.uploaded?.let {
                                        Text(
                                            text = formatMediaUploadedTimestamp(context, it),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White,
                                        )
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.66f)
                            ),
                            navigationIcon = {
                                IconButton(onClick = onBack) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = stringResource(R.string.content_description_back),
                                        tint = Color.White
                                    )
                                }
                            }
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                ) {
                    AnimatedVisibility(
                        visible = projectGalleryUiState.showSystemBars,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier
                            .padding(16.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.66f)
                            ),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = stringResource(
                                        id = R.string.image_position_format,
                                        currentImageIndex,
                                        totalImages
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        }
                    }
                }
            }
        }
        if (mediaUiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}
