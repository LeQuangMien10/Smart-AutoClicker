/*
 * Copyright (C) 2026 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.areaselector.manual

import android.graphics.Rect
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.buzbuz.smartautoclicker.core.common.overlays.dialog.OverlayDialog
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.monitoring.MonitoredOverlayType
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.DialogNavigationButton
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.setButtonEnabledState
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setError
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setLabel
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setOnTextChangedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setText
import com.buzbuz.smartautoclicker.core.ui.utils.MinMaxInputFilter
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.DialogManualAreaInputBinding

import com.google.android.material.bottomsheet.BottomSheetDialog

/**
 * Dialog allowing the user to type the detection area coordinates instead of dragging the selector handles.
 *
 * @param initialArea the area to pre-fill the fields with.
 * @param minimalArea the minimal size the area must have once confirmed.
 * @param maxArea the bounds the area must stay within (i.e. the screen bounds).
 * @param onAreaConfirmed called with the validated area when the user saves.
 */
class ManualAreaInputDialog(
    private val initialArea: Rect,
    private val minimalArea: Rect,
    private val maxArea: Rect,
    private val onAreaConfirmed: (Rect) -> Unit,
) : OverlayDialog(R.style.ScenarioConfigTheme) {

    override fun tutorialMonitoringTag(): String = MonitoredOverlayType.MANUAL_AREA_INPUT.name

    private lateinit var viewBinding: DialogManualAreaInputBinding

    private var left: Int = initialArea.left
    private var top: Int = initialArea.top
    private var right: Int = initialArea.right
    private var bottom: Int = initialArea.bottom

    override fun onCreateView(): ViewGroup {
        viewBinding = DialogManualAreaInputBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                dialogTitle.setText(R.string.dialog_title_manual_area_input)
                buttonDismiss.setDebouncedOnClickListener { back() }
                buttonDelete.visibility = View.GONE
                buttonSave.apply {
                    visibility = View.VISIBLE
                    setDebouncedOnClickListener { onSaveButtonClicked() }
                }
            }

            fieldLeft.apply {
                setLabel(R.string.field_manual_area_left)
                textField.filters = arrayOf(MinMaxInputFilter(0, maxArea.right))
                setText(left.toString(), InputType.TYPE_CLASS_NUMBER)
                setOnTextChangedListener { editable ->
                    left = editable.toString().toIntOrNull() ?: left
                    updateValidity()
                }
            }
            hideSoftInputOnFocusLoss(fieldLeft.textField)

            fieldTop.apply {
                setLabel(R.string.field_manual_area_top)
                textField.filters = arrayOf(MinMaxInputFilter(0, maxArea.bottom))
                setText(top.toString(), InputType.TYPE_CLASS_NUMBER)
                setOnTextChangedListener { editable ->
                    top = editable.toString().toIntOrNull() ?: top
                    updateValidity()
                }
            }
            hideSoftInputOnFocusLoss(fieldTop.textField)

            fieldRight.apply {
                setLabel(R.string.field_manual_area_right)
                textField.filters = arrayOf(MinMaxInputFilter(0, maxArea.right))
                setText(right.toString(), InputType.TYPE_CLASS_NUMBER)
                setOnTextChangedListener { editable ->
                    right = editable.toString().toIntOrNull() ?: right
                    updateValidity()
                }
            }
            hideSoftInputOnFocusLoss(fieldRight.textField)

            fieldBottom.apply {
                setLabel(R.string.field_manual_area_bottom)
                textField.filters = arrayOf(MinMaxInputFilter(0, maxArea.bottom))
                setText(bottom.toString(), InputType.TYPE_CLASS_NUMBER)
                setOnTextChangedListener { editable ->
                    bottom = editable.toString().toIntOrNull() ?: bottom
                    updateValidity()
                }
            }
            hideSoftInputOnFocusLoss(fieldBottom.textField)
        }

        return viewBinding.root
    }

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        updateValidity()
    }

    private fun onSaveButtonClicked() {
        onAreaConfirmed(Rect(left, top, right, bottom))
        back()
    }

    private fun updateValidity() {
        val isLeftValid = left in 0 until right
        val isTopValid = top in 0 until bottom
        val isRightValid = right in (left + 1)..maxArea.right
        val isBottomValid = bottom in (top + 1)..maxArea.bottom
        val isSizeValid = (right - left) >= minimalArea.width() && (bottom - top) >= minimalArea.height()

        viewBinding.fieldLeft.setError(R.string.error_manual_area_left_right, !isLeftValid)
        viewBinding.fieldTop.setError(R.string.error_manual_area_top_bottom, !isTopValid)
        viewBinding.fieldRight.setError(R.string.error_manual_area_left_right, !isRightValid)
        viewBinding.fieldBottom.setError(R.string.error_manual_area_top_bottom, !isBottomValid)

        val boundsValid = isLeftValid && isTopValid && isRightValid && isBottomValid
        viewBinding.textErrorMinSize.apply {
            visibility = if (boundsValid && !isSizeValid) View.VISIBLE else View.GONE
            text = context.getString(
                R.string.error_manual_area_min_size,
                minimalArea.width(),
                minimalArea.height(),
            )
        }

        viewBinding.layoutTopBar.setButtonEnabledState(DialogNavigationButton.SAVE, boundsValid && isSizeValid)
    }
}
