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

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import it.ministerodellasalute.verificaC19.databinding.ScanModeChoiceLayoutBinding
import it.ministerodellasalute.verificaC19.ui.scanmode.ScanModeChoice

class ScanModeAdapter(
    private val onItemClicked: (ScanModeChoice) -> Unit
) : ListAdapter<ScanModeChoice, ScanModeAdapter.ScanModeHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScanModeHolder {
        return ScanModeHolder(ScanModeChoiceLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount() = currentList.size

    inner class ScanModeHolder(private val binding: ScanModeChoiceLayoutBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(choice: ScanModeChoice) {
            binding.title.text = choice.title
            binding.description.text = choice.description
            binding.radioButton.isChecked = choice.isChecked
            binding.cardView.visibility = if (choice.isChecked) View.VISIBLE else View.GONE

            binding.root.setOnClickListener {
                onItemClicked(choice)
            }
        }
    }

    override fun onBindViewHolder(holder: ScanModeAdapter.ScanModeHolder, position: Int) {
        val item = currentList[position]
        holder.bind(item)
    }

    private class DiffCallback : DiffUtil.ItemCallback<ScanModeChoice>() {
        override fun areItemsTheSame(oldItem: ScanModeChoice, newItem: ScanModeChoice) =
            oldItem.scanMode == newItem.scanMode

        override fun areContentsTheSame(oldItem: ScanModeChoice, newItem: ScanModeChoice) =
            oldItem.isChecked == newItem.isChecked
    }
}