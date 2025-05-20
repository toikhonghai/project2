package app.vitune.android.ui.components.themed

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Companion.Down
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Companion.Up
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.Log
import app.vitune.android.R
import app.vitune.android.preferences.UIStatePreferences
import app.vitune.core.ui.Dimensions.NavigationRail
import app.vitune.core.ui.LocalAppearance
import kotlinx.collections.immutable.toImmutableList

enum class MainTab { Music, Podcast }

@Composable
fun Scaffold(
    key: String,
    topIconButtonId: Int,
    iconAccountButtonId: Int? = null,
    onTopIconButtonClick: () -> Unit,
    tabIndex: Int,
    onTabChange: (Int) -> Unit,
    tabColumnContent: TabsBuilder.() -> Unit,
    modifier: Modifier = Modifier,
    tabsEditingTitle: String = stringResource(R.string.tabs),
    showMainTabs: Boolean = false,
    selectedMainTab: MainTab? = null,
    onMainTabSelected: (MainTab) -> Unit = {},
    onAccountIconClick: () -> Unit = {},
    content: @Composable AnimatedVisibilityScope.(Int) -> Unit
) {
    val (colorPalette) = LocalAppearance.current
    var hiddenTabs by UIStatePreferences.mutableTabStateOf(key)

    Column(
        modifier = modifier
            .background(colorPalette.background0)
            .fillMaxSize()
            .padding(top = 36.dp)
    ) {
        Log.e("screen podcast", "Scaffold: Rendering TopBar with selectedMainTab = $selectedMainTab, key = $key")

        TopBar(
            items = TopBarBuilder.rememberTopBar(selectedMainTab) {
                iconButton(
                    icon = topIconButtonId,
                    onClick = onTopIconButtonClick
                )
                if(iconAccountButtonId != null) {
                    iconButton(
                        icon = iconAccountButtonId,
                        onClick = onAccountIconClick
                    )
                }
                if(showMainTabs){
                    titleRes(
                        when (selectedMainTab) {
                            MainTab.Music -> {
                                Log.e("screen podcast", "Scaffold: selectedMainTab = $selectedMainTab")
                                R.string.music
                            }
                            MainTab.Podcast -> {
                                Log.e("screen podcast", "Scaffold: selectedMainTab = $selectedMainTab")
                                R.string.podcast
                            }
                            else -> {
                                Log.e("screen podcast", "Scaffold: selectedMainTab = $selectedMainTab")
                                R.string.music
                            }
                        }
                    )
                    iconButton(
                        icon = R.drawable.musical_notes,
                        onClick = { onMainTabSelected(MainTab.Music) }
                    )
                    iconButton(
                        icon = R.drawable.podcast,
                        onClick = { onMainTabSelected(MainTab.Podcast) }
                    )
                }
            },
            modifier = Modifier,
            showMainTabs = showMainTabs,
            selectedMainTab = selectedMainTab
        )

        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // Sử dụng key để buộc NavigationRail tái tạo khi MainTab thay đổi
            key(selectedMainTab) {
                NavigationRail(
                    tabIndex = tabIndex,
                    onTabIndexChange = onTabChange,
                    hiddenTabs = hiddenTabs,
                    setHiddenTabs = { hiddenTabs = it.toImmutableList() },
                    tabsEditingTitle = tabsEditingTitle,
                    content = tabColumnContent
                )
            }

            // Animated Content cho phần hiển thị tab content
            AnimatedContent(
                modifier = Modifier.weight(1f),
                targetState = tabIndex,
                transitionSpec = {
                    val slideDirection = if (targetState > initialState) Up else Down
                    val slideAnimationSpec = spring(
                        dampingRatio = 0.8f,
                        stiffness = Spring.StiffnessMediumLow,
                        visibilityThreshold = IntOffset.VisibilityThreshold
                    )
                    val fadeAnimationSpec = tween<Float>(durationMillis = 300)

                    ContentTransform(
                        targetContentEnter = slideIntoContainer(
                            slideDirection,
                            slideAnimationSpec,
                            initialOffset = { size -> size / 2 }
                        ) + fadeIn(fadeAnimationSpec),
                        initialContentExit = slideOutOfContainer(
                            slideDirection,
                            slideAnimationSpec,
                            targetOffset = { size -> size / 2 }
                        ) + fadeOut(fadeAnimationSpec),
                        sizeTransform = null
                    )
                },
                content = content,
                label = "TabTransition"
            )
        }
    }
}
