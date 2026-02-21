/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.crashes

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import mozilla.components.compose.base.LinkText
import mozilla.components.compose.base.LinkTextState
import mozilla.components.compose.base.button.TextButton
import mozilla.components.lib.crash.store.CrashAction
import org.mozilla.fenix.BuildConfig
import org.mozilla.fenix.R
import org.mozilla.fenix.settings.SupportUtils
import org.mozilla.fenix.theme.FirefoxTheme

/** Callback for dispatching [CrashAction]s from the [UnsubmittedCrashDialog]. */
fun interface CrashActionDispatcher {

    /** Dispatches the received [CrashAction] */
    fun dispatchCrashAction(action: CrashAction)
}

/** Dialog to request whether a user wants to submit crashes that have not been reported. */
class UnsubmittedCrashDialog : DialogFragment() {

    private val crashIDs: List<String>
        get() = arguments?.getStringArrayList(BUNDLE_ARG_CRASH_IDS) ?: emptyList()

    @VisibleForTesting internal var dispatcher: CrashActionDispatcher? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!BuildConfig.CRASH_REPORTING) {
            showsDialog = false
            dismissAllowingStateLoss()
            return
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let { activity ->
            Dialog(activity).apply {
                setContentView(
                    ComposeView(activity).apply {
                        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                        setContent {
                            FirefoxTheme {
                                CrashCard(
                                    dismiss = ::dismiss,
                                    dispatcher = {
                                        dispatcher?.dispatchCrashAction(it)
                                    },
                                    crashIDs = crashIDs,
                                )
                            }
                        }
                    },
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    ),
                )
                window?.setBackgroundDrawableResource(android.R.color.transparent)
            }
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (dispatcher == null) {
            dispatcher = context as? CrashActionDispatcher
        }
    }

    override fun onDetach() {
        dispatcher = null
        super.onDetach()
    }

    companion object {
        private const val BUNDLE_ARG_CRASH_IDS = "bundle-arg-crash-ids"

        const val TAG = "unsubmitted crash dialog tag"

        /** Creates an instance of [UnsubmittedCrashDialog] with a list of [crashIDs] */
        fun create(crashIDs: List<String>? = null): UnsubmittedCrashDialog {
            return UnsubmittedCrashDialog().apply {
                arguments =
                    Bundle().apply {
                        putStringArrayList(BUNDLE_ARG_CRASH_IDS, ArrayList(crashIDs ?: emptyList()))
                    }
            }
        }
    }
}

@Composable
private fun CrashCard(
    dismiss: () -> Unit,
    dispatcher: (action: CrashAction) -> Unit,
    crashIDs: List<String>?,
) {
    dispatcher(CrashAction.PromptShown)

    val requestedByDevs = !crashIDs.isNullOrEmpty()
    val msg =
        if (requestedByDevs) {
            if (crashIDs.size == 1) {
                stringResource(
                    R.string.unsubmitted_crash_requested_by_devs_dialog_title,
                    stringResource(R.string.app_name),
                )
            } else {
                stringResource(
                    R.string.unsubmitted_crashes_requested_by_devs_dialog_title,
                    crashIDs.size,
                    stringResource(R.string.app_name),
                )
            }
        } else {
            stringResource(
                R.string.unsubmitted_crash_dialog_title_2,
                stringResource(R.string.app_name),
            )
        }

    Surface(shape = MaterialTheme.shapes.small) {
        if (!requestedByDevs) {
            CrashDialog(
                msg = msg,
                dismiss = dismiss,
                dispatcher = dispatcher,
            )
        } else {
            CrashPullDialog(
                msg = msg,
                dismiss = dismiss,
                dispatcher = dispatcher,
                crashIDs = crashIDs,
            )
        }
    }
}

@Composable
private fun CrashDialog(
    msg: String,
    dismiss: () -> Unit,
    dispatcher: (action: CrashAction) -> Unit,
) {
    var checkboxChecked by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.width(280.dp).padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 0.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = msg,
            style = FirefoxTheme.typography.headline7,
            modifier = Modifier.semantics { heading() },
        )

        AnnotatedStringBody()

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.widthIn(max = 248.dp),
        ) {
            Checkbox(
                checked = checkboxChecked,
                onCheckedChange = { checkboxChecked = it },
                modifier = Modifier.padding(start = 0.dp, end = 0.dp),
            )

            Text(
                text = stringResource(R.string.unsubmitted_crash_dialog_checkbox_label),
                style = FirefoxTheme.typography.subtitle1,
            )
        }

        Row(
            horizontalArrangement =
                Arrangement.spacedBy(
                    space = 8.dp,
                    alignment = Alignment.End,
                ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            TextButton(
                text = stringResource(R.string.unsubmitted_crash_dialog_negative_button_2),
                onClick = {
                    dispatcher(CrashAction.CancelTapped)
                    dismiss()
                },
            )

            TextButton(
                text = stringResource(R.string.unsubmitted_crash_dialog_positive_button_2),
                onClick = {
                    dispatcher(
                        CrashAction.ReportTapped(
                            checkboxChecked,
                            listOf(),
                        )
                    )
                    dismiss()
                },
            )
        }
    }
}

@Composable
private fun AnnotatedStringBody() {
    val context = LocalContext.current
    val learnMoreText = stringResource(R.string.unsubmitted_crash_dialog_learn_more)
    val linkStateLearnMore =
        LinkTextState(
            text = learnMoreText,
            url = "",
            onClick = {
                SupportUtils.launchSandboxCustomTab(
                    context = context,
                    url =
                        SupportUtils.getSumoURLForTopic(
                            context = context,
                            topic = SupportUtils.SumoTopic.CRASH_REPORTS,
                        ),
                )
            },
        )
    LinkText(
        text =
            stringResource(
                R.string.unsubmitted_crash_dialog_body,
                stringResource(R.string.unsubmitted_crash_dialog_learn_more),
            ),
        linkTextStates = listOf(linkStateLearnMore),
        style = FirefoxTheme.typography.body2,
        linkTextColor = MaterialTheme.colorScheme.tertiary,
        linkTextDecoration = TextDecoration.Underline,
        textAlign = null,
        shouldApplyAccessibleSize = false,
    )
}

@Composable
private fun CrashPullDialog(
    msg: String,
    dismiss: () -> Unit,
    dispatcher: (action: CrashAction) -> Unit,
    crashIDs: List<String>,
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = msg,
            modifier = Modifier.semantics { heading() },
            style = FirefoxTheme.typography.headline5,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            val context = LocalContext.current
            Text(
                text = stringResource(R.string.unsubmitted_crash_requested_by_devs_learn_more).uppercase(),
                color = MaterialTheme.colorScheme.tertiary,
                modifier =
                    Modifier.clickable {
                        SupportUtils.launchSandboxCustomTab(
                            context = context,
                            url =
                                SupportUtils.getSumoURLForTopic(
                                    context = context,
                                    topic = SupportUtils.SumoTopic.REQUESTED_CRASH_MINIDUMP,
                                ),
                        )
                    },
            )

            Text(
                text = stringResource(R.string.unsubmitted_crash_requested_by_devs_dialog_never_button).uppercase(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier =
                    Modifier.clickable {
                        dispatcher(CrashAction.CancelForEverTapped)
                        dismiss()
                    },
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.unsubmitted_crash_dialog_negative_button).uppercase(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier =
                    Modifier.clickable {
                        dispatcher(CrashAction.CancelTapped)
                        dismiss()
                    },
            )

            Text(
                text = stringResource(R.string.unsubmitted_crash_dialog_positive_button).uppercase(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier =
                    Modifier.clickable {
                        dispatcher(
                            CrashAction.ReportTapped(
                                automaticallySendChecked = false,
                                crashIDs = crashIDs,
                            )
                        )
                        dismiss()
                    },
            )
        }
    }
}

private data class CrashDialogState(val crashIDs: List<String>?)

private class CrashDialogPreviewProvider : PreviewParameterProvider<CrashDialogState> {
    override val values =
        sequenceOf(
            CrashDialogState(crashIDs = null),
            CrashDialogState(crashIDs = listOf("12345")),
            CrashDialogState(crashIDs = listOf("12345", "67890")),
        )
}

@PreviewLightDark
@Composable
private fun UnsubmittedCrashDialogPreview(
    @PreviewParameter(CrashDialogPreviewProvider::class) state: CrashDialogState
) {
    FirefoxTheme {
        Column(modifier = Modifier.padding(all = 16.dp)) {
            CrashCard(
                dismiss = {},
                dispatcher = {},
                crashIDs = state.crashIDs,
            )
        }
    }
}
