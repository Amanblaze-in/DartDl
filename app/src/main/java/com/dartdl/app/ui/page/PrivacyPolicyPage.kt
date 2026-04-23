package com.dartdl.app.ui.page

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dartdl.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyPage(onNavigateBack: () -> Unit) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.privacy_policy)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(id = R.string.back),
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            item {
                SectionHeader("Introduction")
                SectionBody(
                    "This Privacy Policy describes how DartDL handles your information. " +
                            "DartDL is a video and audio downloader application created by Amanblaze as an ad-supported service. " +
                            "By using the Application, you agree to the terms outlined in this policy."
                )
            }

            item {
                SectionHeader("Information We DO NOT Collect")
                SectionBody(
                    "• Personal Identity: We do not require account registration. We do not collect your name, email, phone number, or address.\n" +
                            "• Real-time Location: The Application does not track your GPS or precise location.\n" +
                            "• Personal Contacts/Files: We do not access your contacts. We only access storage to save the media files you choose to download."
                )
            }

            item {
                SectionHeader("Information Stored Locally")
                SectionBody(
                    "To provide its functionality, the Application stores the following data locally on your device ONLY:\n" +
                            "• Download History: A record of your previous downloads to help you manage your files.\n" +
                            "• App Preferences: Settings such as theme, language, and download directories.\n" +
                            "This data is never uploaded to our servers and remains under your control."
                )
            }

            item {
                SectionHeader("Third-Party Services")
                SectionBody(
                    "The Application uses third-party services that may collect information used to identify you or provide services:\n" +
                            "• Google Play Services: For app delivery and core Android features.\n" +
                            "• AdMob: To display advertisements. AdMob may collect anonymized device identifiers for ad personalization.\n" +
                            "Please consult their respective privacy policies for more details."
                )
            }

            item {
                SectionHeader("Storage Permissions")
                SectionBody(
                    "The Application requires access to your device's storage to save downloaded videos and audio files. " +
                            "Files are saved to public folders (like Downloads or Movies) or a custom directory of your choice."
                )
            }

            item {
                SectionHeader("Children")
                SectionBody(
                    "We do not knowingly collect personal information from children under 13. " +
                            "If you believe we have accidentally collected such information, please contact us at the email below for immediate deletion."
                )
            }

            item {
                SectionHeader("Security")
                SectionBody(
                    "We use industry-standard procedural safeguards to protect any data handled by the app. " +
                            "However, since no electronic storage is 100% secure, we recommend keeping your device updated."
                )
            }

            item {
                SectionHeader("Contact Us")
                SectionBody(
                    "For any questions regarding this Privacy Policy or data inquiries, please reach out to us at:\n" +
                            "Email: support@amanblaze.in"
                )
            }

            item {
                SectionHeader("Changes to Policy")
                SectionBody(
                    "We may update this Privacy Policy from time to time. The latest version will always be available within the app."
                )
            }

            item {
                SectionHeader("Effective Date")
                SectionBody("This privacy policy is effective as of 2026-04-18.")
            }

            item {
                SectionBody(
                    "\nBy using the Application, you consent to the processing of your information as set forth in this Privacy Policy.",
                    modifier = Modifier.padding(bottom = 32.dp)
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun SectionBody(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier.padding(bottom = 8.dp),
        color = MaterialTheme.colorScheme.onSurface
    )
}
