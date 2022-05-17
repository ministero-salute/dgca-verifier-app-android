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
 *  Created by nicolamcornelio on 17/05/22, 17:07
 */

package it.ministerodellasalute.verificaC19.ui.scanmode

import android.annotation.SuppressLint
import android.app.ActionBar
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import it.ministerodellasalute.verificaC19.databinding.FragmentScanModeDialogBinding
import it.ministerodellasalute.verificaC19.ui.ScanModeAdapter
import it.ministerodellasalute.verificaC19.ui.extensions.addTransparency
import it.ministerodellasalute.verificaC19.ui.extensions.removeTransparency
import it.ministerodellasalute.verificaC19sdk.model.ScanMode

@AndroidEntryPoint
@SuppressLint("NotifyDataSetChanged")
class ScanModeDialogFragment(private var scanModeChoices: List<ScanModeChoice>, private val onChoiceSelected: (ScanMode) -> Unit) : DialogFragment() {

    private var _binding: FragmentScanModeDialogBinding? = null
    private val binding get() = _binding!!

    private var scanModeAdapter = ScanModeAdapter {
        onScanModeSelected(it)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.MATCH_PARENT)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = FragmentScanModeDialogBinding.inflate(LayoutInflater.from(context))
        val dialog = activity?.let {
            Dialog(it)
        }
        dialog?.setContentView(binding.root)
        return dialog!!
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        initRecycler()
        handleChosenScanMode()
        setOnClickListener()
        return view
    }

    private fun setOnClickListener() {
        binding.closeImageView.setOnClickListener {
            dismiss()
        }

        binding.confirmButton.setOnClickListener {
            onChoiceSelected(scanModeChoices.find { it.isChecked }?.scanMode ?: ScanMode.STANDARD)
            dismiss()
        }
    }

    private fun handleChosenScanMode() {
        scanModeChoices.find { it.isChecked }?.let {
            binding.recyclerView.post {
                binding.recyclerView.smoothScrollToPosition(scanModeChoices.indexOf(it))
            }
        } ?: run {
            binding.confirmButton.isEnabled = false
            binding.confirmButton.addTransparency()
        }
    }

    private fun initRecycler() {
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(activity)
            adapter = scanModeAdapter
        }
        scanModeAdapter.submitList(scanModeChoices)
    }

    private fun onScanModeSelected(scanModeChoice: ScanModeChoice) {
        scanModeChoices.forEach { it.isChecked = false }
        scanModeChoice.isChecked = true
        scanModeAdapter.notifyDataSetChanged()

        binding.confirmButton.isEnabled = true
        binding.confirmButton.removeTransparency()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun show(supportFragmentManager: FragmentManager) {
        super.show(supportFragmentManager, this.javaClass.name)
    }
}