/*
 *  ---license-start
 *  eu-digital-green-certificates / dgca-verifier-app-android
 *  ---
 *  Copyright (C) 2022 T-Systems International GmbH and all other contributors
 *  ---
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  ---license-end
 *
 *  Created by nicolamcornelio on 2/22/22, 11:48 AM
 */

package it.ministerodellasalute.verificaC19.ui

import android.app.ActionBar
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.widget.AppCompatImageView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import it.ministerodellasalute.verificaC19.R
import it.ministerodellasalute.verificaC19sdk.model.FirstViewModel
import it.ministerodellasalute.verificaC19sdk.model.ScanMode
import it.ministerodellasalute.verificaC19sdk.model.validation.RuleSet

@AndroidEntryPoint
class ScanModeDialogFragment(private val ruleSet: RuleSet) : DialogFragment(), ScanModeDialogCallback {

    private val viewModel by viewModels<FirstViewModel>()
    private lateinit var scanModeAdapter: ScanModeAdapter
    private lateinit var scanModeBodyLayout: RecyclerView
    private lateinit var scanModes: List<ScanModeChoice>
    private lateinit var confirmButton: Button

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.MATCH_PARENT)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_scan_mode_dialog, container)
        setScanModeList()
        val chosenScanMode = if (viewModel.getScanModeFlag()) getChosenScanMode() else -1

        scanModeBodyLayout = view.findViewById<ViewGroup>(R.id.bodyLayout) as RecyclerView
        scanModeBodyLayout.layoutManager = LinearLayoutManager(this.activity)
        scanModeAdapter = ScanModeAdapter(scanModes, chosenScanMode, this)
        scanModeBodyLayout.adapter = scanModeAdapter

        val btnClose = view.findViewById<AppCompatImageView>(R.id.closeImageView)
        btnClose.setOnClickListener {
            dismiss()
        }

        confirmButton = view.findViewById(R.id.confirmButton)
        if (!viewModel.getScanModeFlag()) {
            confirmButton.isEnabled = false
            confirmButton.background.alpha = 128
        }
        confirmButton.setOnClickListener {
            if (!viewModel.getScanModeFlag()) viewModel.setScanModeFlag(true)
            setChosenScanMode()
            dismiss()
        }

        return view
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        val activity = activity
        if (activity is DialogInterface.OnDismissListener) activity.onDismiss(dialog)
    }

    private fun setChosenScanMode() {
        when (scanModeAdapter.mSelectedItem) {
            0 -> viewModel.setScanMode(ScanMode.STANDARD)
            1 -> viewModel.setScanMode(ScanMode.STRENGTHENED)
            2 -> viewModel.setScanMode(ScanMode.BOOSTER)
            3 -> viewModel.setScanMode(ScanMode.WORK)
            4 -> viewModel.setScanMode(ScanMode.ENTRY_ITALY)
            5 -> viewModel.setScanMode(ScanMode.SCHOOL)
        }
    }

    private fun getChosenScanMode(): Int {
        val chosenScanMode = when (viewModel.getScanMode()) {
            ScanMode.STANDARD -> 0
            ScanMode.STRENGTHENED -> 1
            ScanMode.BOOSTER -> 2
            ScanMode.WORK -> 3
            ScanMode.ENTRY_ITALY -> 4
            ScanMode.SCHOOL -> 5
            else -> 0
        }
        return chosenScanMode
    }

    private fun setScanModeList() {
        scanModes = mutableListOf(
            ScanModeChoice(getString(R.string.scan_mode_3G_header), ruleSet.getBaseScanModeDescription() ?: ""),
            ScanModeChoice(getString(R.string.scan_mode_2G_header), ruleSet.getReinforcedScanModeDescription()),
            ScanModeChoice(getString(R.string.scan_mode_booster_header), ruleSet.getBoosterScanModeDescription()),
            ScanModeChoice(getString(R.string.scan_mode_work_header), ruleSet.getWorkScanModeDescription()),
            ScanModeChoice(getString(R.string.scan_mode_entry_italy_header), ruleSet.getItalyEntryScanModeDescription()),
            ScanModeChoice(getString(R.string.scan_mode_school_header), ruleSet.getSchoolScanModeDescription())
        )
    }

    data class ScanModeChoice(
        val name: String,
        var description: String,
        var isChecked: Boolean = false
    )

    override fun enableConfirmButton() {
        confirmButton.isEnabled = true
        confirmButton.background.alpha = 255
    }
}