/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlin.math.roundToInt
import mozilla.components.support.ktx.android.content.pixelSizeFor
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.requireComponents

class ShortcutRowsDialogFragment : DialogFragment() {
    private lateinit var input: TextInputEditText

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val inputLayout =
            TextInputLayout(context).apply {
                hint = getString(R.string.waterfox_shortcut_rows_hint)
                isErrorEnabled = true
            }
        input =
            TextInputEditText(context).apply {
                inputType = InputType.TYPE_CLASS_NUMBER
                setSingleLine()
                setText(
                    savedInstanceState?.getString(STATE_DRAFT)
                        ?: requireComponents.settings.homepageShortcutRows.toString()
                )
                setSelectAllOnFocus(true)
                requestFocus()
            }
        inputLayout.addView(input)
        val container =
            FrameLayout(context).apply {
                val padding = context.pixelSizeFor(R.dimen.dialog_edit_text_horizontal_padding)
                setPadding(padding, 0, padding, 0)
                addView(createAdjustmentRow(context, inputLayout))
            }
        return MaterialAlertDialogBuilder(context)
            .setTitle(R.string.waterfox_shortcut_rows_title)
            .setView(container)
            .setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(android.R.string.cancel, null)
            .create()
            .apply {
                setOnShowListener {
                    getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                        val rows = input.text?.toString()?.trim()?.toIntOrNull()
                        if (rows == null || rows !in 1..16) {
                            inputLayout.error = getString(R.string.waterfox_shortcut_rows_invalid)
                        } else {
                            requireComponents.settings.homepageShortcutRows = rows
                            parentFragmentManager.setFragmentResult(RESULT_KEY, Bundle.EMPTY)
                            this@ShortcutRowsDialogFragment.dismiss()
                        }
                    }
                }
                window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
            }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (::input.isInitialized) {
            outState.putString(STATE_DRAFT, input.text?.toString())
        }
    }

    private fun createAdjustmentRow(context: Context, inputLayout: TextInputLayout): LinearLayout {
        val decrease = createAdjustmentButton(context, -1)
        val increase = createAdjustmentButton(context, 1)
        fun updateButtons() {
            val rows = input.text?.toString()?.trim()?.toIntOrNull()
            decrease.isEnabled = rows == null || rows > 1
            increase.isEnabled = rows == null || rows < 16
        }
        input.doAfterTextChanged {
            inputLayout.error = null
            updateButtons()
        }
        updateButtons()
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(decrease)
            addView(inputLayout, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(increase)
        }
    }

    private fun createAdjustmentButton(context: Context, amount: Int): MaterialButton =
        MaterialButton(context).apply {
            val size = (48 * resources.displayMetrics.density).roundToInt()
            layoutParams = LinearLayout.LayoutParams(size, size)
            minimumWidth = size
            minWidth = size
            minimumHeight = size
            minHeight = size
            insetTop = 0
            insetBottom = 0
            setPaddingRelative(0, 0, 0, 0)
            gravity = Gravity.CENTER
            text = if (amount < 0) "−" else "+"
            contentDescription =
                getString(
                    if (amount < 0) R.string.waterfox_shortcut_rows_decrease
                    else R.string.waterfox_shortcut_rows_increase
                )
            setOnClickListener { adjustRows(amount) }
        }

    private fun adjustRows(amount: Int) {
        val rows = input.text?.toString()?.trim()?.toIntOrNull() ?: requireComponents.settings.homepageShortcutRows
        val adjusted = (rows.toLong() + amount).coerceIn(1L, 16L).toInt()
        input.setText(adjusted.toString())
        input.setSelection(input.length())
    }

    companion object {
        const val TAG = "shortcut_rows_dialog"
        const val RESULT_KEY = "shortcut_rows_changed"
        private const val STATE_DRAFT = "shortcut_rows_draft"
    }
}
