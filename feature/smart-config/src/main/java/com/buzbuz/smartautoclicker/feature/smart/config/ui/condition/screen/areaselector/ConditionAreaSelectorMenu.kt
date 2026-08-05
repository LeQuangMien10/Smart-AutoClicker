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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.areaselector

import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.lifecycle.lifecycleScope

import com.buzbuz.smartautoclicker.core.common.overlays.base.viewModels
import com.buzbuz.smartautoclicker.core.common.overlays.menu.OverlayMenu
import com.buzbuz.smartautoclicker.core.ui.views.areaselector.AreaSelectorView
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.OverlayAreaSelectorMenuBinding
import com.buzbuz.smartautoclicker.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.areaselector.manual.ManualAreaInputDialog

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.monitoring.MonitoredOverlayType

class ConditionAreaSelectorMenu(
    private val onAreaSelected: (Rect) -> Unit
) : OverlayMenu() {

    override fun tutorialMonitoringTag(): String = MonitoredOverlayType.CONDITION_AREA_SELECTOR_MENU.name

    /** The view model for this dialog. */
    private val viewModel: ConditionAreaSelectorViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { imageConditionAreaSelectorViewModel() },
    )

    /** The view binding for the overlay menu. */
    private lateinit var viewBinding: OverlayAreaSelectorMenuBinding
    /** The view displaying selector for the area. */
    private lateinit var selectorView: AreaSelectorView
    /** The minimal area size allowed for the currently edited condition, kept in sync with [viewModel]. */
    private var minimalArea: Rect = Rect()
    /** Tells if the selector has already been initialized with the edited condition's area. */
    private var isSelectorInitialized = false

    override fun onCreateMenu(layoutInflater: LayoutInflater): ViewGroup {
        selectorView = AreaSelectorView(context, displayConfigManager)
        viewBinding = OverlayAreaSelectorMenuBinding.inflate(layoutInflater)
        return viewBinding.root
    }

    override fun onCreateOverlayView(): View = selectorView

    override fun onStart() {
        super.onStart()

        // Only initialize the selector once: this menu is re-started (without being recreated) each time the
        // manual area input dialog is closed, and re-collecting here would overwrite the user's edits with the
        // condition's original area.
        if (isSelectorInitialized) return
        isSelectorInitialized = true

        lifecycleScope.launch {
            val selectorState = viewModel.initialArea.first()
            minimalArea = selectorState.minimalArea
            selectorView.setSelection(selectorState.initialArea, selectorState.minimalArea)
        }
    }

    override fun onMenuItemClicked(viewId: Int) {
        when (viewId) {
            R.id.btn_confirm -> onConfirm()
            R.id.btn_cancel -> onCancel()
            R.id.btn_manual_input -> onManualInput()
        }
    }

    /** Called when the user press the confirmation button. */
    private fun onConfirm() {
        onAreaSelected(selectorView.getSelection())
        back()
    }

    /** Called when the user press the cancel button. */
    private fun onCancel() {
        back()
    }

    /** Called when the user press the manual coordinates input button. */
    private fun onManualInput() {
        overlayManager.navigateTo(
            context = context,
            newOverlay = ManualAreaInputDialog(
                initialArea = selectorView.getSelection(),
                minimalArea = minimalArea,
                maxArea = Rect(0, 0, displayConfigManager.displayConfig.sizePx.x, displayConfigManager.displayConfig.sizePx.y),
                onAreaConfirmed = { area -> selectorView.setSelection(area, minimalArea) },
            ),
            hideCurrent = true,
        )
    }
}
