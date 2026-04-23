package com.dartdl.app.ui.page.settings.about

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dartdl.app.R
import com.dartdl.app.ui.common.AsyncImageImpl
import com.dartdl.app.ui.component.BackButton
import com.dartdl.app.ui.component.gitHubAvatar
import com.dartdl.app.ui.theme.DartDLTheme
import com.dartdl.app.util.AdManager
import com.dartdl.app.util.makeToast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SponsorsPage(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
            rememberTopAppBarState(),
            canScroll = { true },
        )
    val uriHandler = LocalUriHandler.current

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(modifier = Modifier, text = stringResource(id = R.string.sponsors)) },
                navigationIcon = { BackButton { onNavigateBack() } },
                scrollBehavior = scrollBehavior,
            )
        },
        content = { values ->
            Column(
                modifier =
                    Modifier.fillMaxSize()
                        .padding(values)
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Surface(
                    shape = CardDefaults.shape,
                    modifier = Modifier.padding(vertical = 24.dp).fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.VolunteerActivism,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp).padding(bottom = 16.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )

                        Text(
                            modifier = Modifier.padding(bottom = 8.dp),
                            text = stringResource(id = R.string.msg_from_developer),
                            style = MaterialTheme.typography.titleMedium,
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AsyncImageImpl(
                                model = gitHubAvatar("amangautamm"),
                                contentDescription = null,
                                modifier = Modifier.size(56.dp).aspectRatio(1f, true).clip(CircleShape),
                                contentScale = ContentScale.Crop,
                            )
                            Column(modifier = Modifier.padding(start = 12.dp)) {
                                Conversation(
                                    modifier = Modifier.padding(bottom = 8.dp),
                                    text = stringResource(id = R.string.sponsor_msg),
                                )
                                Conversation(
                                    modifier = Modifier,
                                    text = stringResource(R.string.sponsor_msg2),
                                )
                            }
                        }

                        Spacer(Modifier.size(16.dp))

                        Button(
                            onClick = { uriHandler.openUri("https://buymeacoffee.com/amanblaze") },
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                        ) {
                            Icon(
                                modifier = Modifier.padding(end = 8.dp).size(ButtonDefaults.IconSize),
                                imageVector = Icons.Outlined.VolunteerActivism,
                                contentDescription = null,
                            )
                            Text(text = stringResource(id = R.string.sponsor))
                        }

                        Spacer(Modifier.size(12.dp))

                        Button(
                            onClick = {
                                (context as? Activity)?.let { activity ->
                                    if (AdManager.isRewardedAdReady) {
                                        AdManager.showRewarded(activity) {
                                            context.makeToast("Support acknowledged! THANK YOU \u2764\ufe0f")
                                        }
                                    } else {
                                        context.makeToast("Loading ad... please wait")
                                        AdManager.loadRewarded(context) { ready ->
                                            if (ready) {
                                                AdManager.showRewarded(activity) {
                                                    context.makeToast("Support acknowledged! THANK YOU \u2764\ufe0f")
                                                }
                                            } else {
                                                context.makeToast("Ad not available right now. Please try again.")
                                            }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiary,
                                    contentColor = MaterialTheme.colorScheme.onTertiary,
                                ),
                        ) {
                            Icon(
                                modifier = Modifier.padding(end = 8.dp).size(ButtonDefaults.IconSize),
                                imageVector = Icons.Outlined.VolunteerActivism,
                                contentDescription = null,
                            )
                            Text(text = "Watch Ad to Support")
                        }
                    }
                }
            }
        },
    )
}

@Composable
fun Conversation(modifier: Modifier = Modifier, text: String) {
    Row(
        modifier =
            modifier
                .padding(horizontal = 4.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .padding(horizontal = 16.dp, vertical = 12.dp)
    ) { Text(text = text, style = MaterialTheme.typography.bodyLarge) }
}

@Composable
@Preview
fun SponsorPagePreview() {
    DartDLTheme { SponsorsPage {} }
}
