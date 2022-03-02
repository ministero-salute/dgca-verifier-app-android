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
 *  Created by nicolamcornelio on 2/17/22, 2:54 PM
 */

package it.ministerodellasalute.verificaC19.ui

import android.annotation.SuppressLint
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import it.ministerodellasalute.verificaC19.databinding.ScanModeChoiceLayoutBinding
import it.ministerodellasalute.verificaC19.ui.ScanModeDialogFragment.ScanModeChoice

class ScanModeAdapter(
    private var adapterList: List<ScanModeChoice>,
    var mSelectedItem: Int,
    private val scanModeDialogFragment: ScanModeDialogCallback
) : RecyclerView.Adapter<ScanModeAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ScanModeChoiceLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(adapterList[position], position, mSelectedItem)
    }

    override fun getItemCount() = adapterList.size

    inner class ViewHolder(private val binding: ScanModeChoiceLayoutBinding) : RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("NotifyDataSetChanged")
        fun bind(
            scanMode: ScanModeChoice,
            position: Int,
            selectedPosition: Int
        ) {
            binding.title.text = scanMode.name
            binding.description.text = scanMode.description
            binding.radioButton.isChecked = selectedPosition == position
            binding.cardView.visibility = if (selectedPosition == position) View.VISIBLE else View.GONE

            binding.root.setOnClickListener {
                scanModeDialogFragment.enableConfirmButton()
                mSelectedItem = adapterPosition
                notifyDataSetChanged()
            }
        }
    }
}