/*
 *  ---license-start
 *  eu-digital-green-certificates / dgca-verifier-app-android
 *  ---
 *  Copyright (C) 2021 T-Systems International GmbH and all other contributors
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
 */

package it.ministerodellasalute.verificaC19.ui

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.text.style.StyleSpan
import android.text.util.Linkify
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.text.bold
import androidx.core.view.isVisible
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import it.ministerodellasalute.verificaC19.BuildConfig
import it.ministerodellasalute.verificaC19.R
import it.ministerodellasalute.verificaC19.WhiteLabelApplication
import it.ministerodellasalute.verificaC19.databinding.ActivityFirstBinding
import it.ministerodellasalute.verificaC19.ui.base.doOnDebug
import it.ministerodellasalute.verificaC19.ui.extensions.hide
import it.ministerodellasalute.verificaC19.ui.extensions.show
import it.ministerodellasalute.verificaC19.ui.main.ExternalLink
import it.ministerodellasalute.verificaC19.ui.main.Extras
import it.ministerodellasalute.verificaC19.ui.main.MainActivity
import it.ministerodellasalute.verificaC19sdk.data.local.prefs.PrefKeys
import it.ministerodellasalute.verificaC19sdk.model.FirstViewModel
import it.ministerodellasalute.verificaC19sdk.model.ScanMode
import it.ministerodellasalute.verificaC19sdk.model.validation.RuleSet
import it.ministerodellasalute.verificaC19sdk.util.ConversionUtility
import it.ministerodellasalute.verificaC19sdk.util.FORMATTED_DATE_LAST_SYNC
import it.ministerodellasalute.verificaC19sdk.util.TimeUtility.parseTo
import it.ministerodellasalute.verificaC19sdk.util.Utility

@AndroidEntryPoint
class FirstActivity : AppCompatActivity(), View.OnClickListener,
    SharedPreferences.OnSharedPreferenceChangeListener, DialogInterface.OnDismissListener {

    private lateinit var binding: ActivityFirstBinding
    private lateinit var shared: SharedPreferences

    private val viewModel by viewModels<FirstViewModel>()

    private val whiteLabelApplication = WhiteLabelApplication()

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                openQrCodeReader()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFirstBinding.inflate(layoutInflater)
        shared = this.getSharedPreferences(PrefKeys.USER_PREF, Context.MODE_PRIVATE)
        setContentView(binding.root)
        setSecureWindowFlags()
        setOnClickListeners()
        setupUI()
        observeLiveData()
    }

    private fun observeLiveData() {
        observeSyncStatus()
        observeRetryCount()
        observeSizeOverThreshold()
        observeInitDownload()
        observeScanMode()
        doOnDebug {
            observeDebugInfo()
        }
    }

    private fun observeDebugInfo() {
        viewModel.debugInfoLiveData.observe(this) {
            it?.let { binding.debugButton.show() }
        }
    }

    private fun observeInitDownload() {
        viewModel.initDownloadLiveData.observe(this) {
            if (it) {
                enableInitDownload()
            }
        }
    }

    private fun observeSizeOverThreshold() {
        viewModel.sizeOverLiveData.observe(this) {
            if (it) {
                createDownloadAlert()
            }
        }
    }

    private fun observeRetryCount() {
        viewModel.maxRetryReached.observe(this) {
            if (it) {
                enableInitDownload()
            }
        }
    }

    private fun observeScanMode() {
        viewModel.scanMode.observe(this) {
            setScanModeButtonText(it)
        }
    }

    private fun observeSyncStatus() {
        viewModel.fetchStatus.observe(this) {
            if (it) {
                binding.qrButton.background.alpha = 128
            } else {
                if (!viewModel.getIsPendingDownload() && viewModel.maxRetryReached.value == false) {
                    viewModel.getDateLastSync().let { date ->
                        binding.dateLastSyncText.text = getString(
                            R.string.lastSyncDate,
                            if (date == -1L) getString(R.string.notAvailable) else date.parseTo(
                                FORMATTED_DATE_LAST_SYNC
                            )
                        )
                    }
                    binding.qrButton.background.alpha = 255
                    hideDownloadProgressViews()
                }
            }
        }
    }

    private fun setupUI() {
        val string = getString(R.string.version, BuildConfig.VERSION_NAME)
        val spannableString = SpannableString(string).also {
            it.setSpan(StyleSpan(Typeface.NORMAL), 0, it.length, 0)
        }
        binding.versionText.text = spannableString
        binding.dateLastSyncText.text = getString(R.string.loading)

        binding.updateProgressBar.max = viewModel.getTotalChunk().toInt()
        updateDownloadedPackagesCount()

        viewModel.getResumeAvailable().let {
            if (it != -1L) {
                if (it == 0.toLong() || viewModel.getIsPendingDownload()) {
                    binding.qrButton.background.alpha = 128
                    binding.resumeDownload.show()
                    binding.dateLastSyncText.text = getString(R.string.incompleteDownload)
                    binding.chunkCount.show()
                    binding.chunkSize.show()
                    binding.updateProgressBar.show()
                } else {
                    binding.resumeDownload.hide()
                }
            }
        }
    }

    private fun setSecureWindowFlags() {
        if (!BuildConfig.DEBUG) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        }
    }

    private fun setOnClickListeners() {
        binding.qrButton.setOnClickListener(this)
        binding.settings.setOnClickListener(this)
        binding.scanModeButton.setOnClickListener(this)
        binding.privacyPolicyCard.setOnClickListener {
            val browserIntent =
                Intent(Intent.ACTION_VIEW, Uri.parse(ExternalLink.PRIVACY_POLICY_URL))
            startActivity(browserIntent)
        }
        binding.faqCard.setOnClickListener {
            val browserIntent =
                Intent(Intent.ACTION_VIEW, Uri.parse(ExternalLink.FAQ_URL))
            startActivity(browserIntent)
        }
        binding.initDownload.setOnClickListener {
            if (Utility.isOnline(this)) {
                startDownload()
            } else {
                createCheckConnectionAlertDialog()
            }
        }
        binding.debugButton.setOnClickListener {
            val debugInfoIntent = Intent(this, DebugInfoActivity::class.java)
            debugInfoIntent.putExtra(
                Extras.DEBUG_INFO,
                Gson().toJson(viewModel.debugInfoLiveData.value)
            )
            startActivity(debugInfoIntent)
        }

        binding.resumeDownload.setOnClickListener {
            if (Utility.isOnline(this)) {
                viewModel.setResumeAsAvailable()
                binding.resumeDownload.hide()
                binding.dateLastSyncText.text = getString(R.string.updatingRevokedPass)
                startSyncData()
            } else {
                createCheckConnectionAlertDialog()
            }
        }
        binding.circleInfoContainer.setOnClickListener(this)
    }

    private fun setScanModeButtonText(currentScanMode: ScanMode) {
        if (!viewModel.getScanModeFlag()) {
            val s = SpannableStringBuilder()
                .bold { append(getString(R.string.label_choose_scan_mode)) }
            binding.scanModeButton.text = s
        } else {
            val chosenScanMode =
                when (currentScanMode) {
                    ScanMode.STANDARD -> getString(R.string.scan_mode_3G_header)
                    ScanMode.STRENGTHENED -> getString(R.string.scan_mode_2G_header)
                    ScanMode.BOOSTER -> getString(R.string.scan_mode_booster_header)
                    ScanMode.WORK -> getString(R.string.scan_mode_work_header)
                    ScanMode.ENTRY_ITALY -> getString(R.string.scan_mode_entry_italy_header)
                    ScanMode.SCHOOL -> getString(R.string.scan_mode_school_header)
                    else -> getString(R.string.scan_mode_3G_header)
                }
            binding.scanModeButton.text = chosenScanMode
        }
    }

    private fun startDownload() {
        prepareForDownload()
        showDownloadProgressViews()
        binding.initDownload.hide()
        binding.dateLastSyncText.text = getString(R.string.updatingRevokedPass)
        startSyncData()
    }

    private fun createCheckConnectionAlertDialog() {
        val builder = AlertDialog.Builder(this)
        val dialog: AlertDialog?
        builder.setTitle(
            getString(R.string.no_internet_title)
        )
        builder.setMessage(getString(R.string.no_internet_message))
        builder.setPositiveButton(getString(R.string.ok_label)) { _, _ -> }
        dialog = builder.create()
        dialog.show()
    }


    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_DENIED
        ) {
            createPermissionAlert()
        } else {
            openQrCodeReader()
        }
    }

    private fun createPermissionAlert() {
        try {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(getString(R.string.privacyTitle))
            builder.setMessage(getString(R.string.privacy))
            builder.setPositiveButton(getString(R.string.next)) { _, _ ->
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            builder.setNegativeButton(getString(R.string.back)) { _, _ ->
            }
            val dialog = builder.create()
            dialog.show()
        } catch (e: Exception) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun createDownloadAlert() {
        try {
            val builder = AlertDialog.Builder(this)
            var dialog: AlertDialog? = null
            builder.setTitle(
                getString(
                    R.string.titleDownloadAlert,
                    ConversionUtility.byteToMegaByte(viewModel.getTotalSizeInByte().toFloat())
                )
            )
            builder.setMessage(
                getString(
                    R.string.messageDownloadAlert,
                    ConversionUtility.byteToMegaByte(viewModel.getTotalSizeInByte().toFloat())
                )
            )
            builder.setPositiveButton(getString(R.string.label_download)) { _, _ ->
                dialog?.dismiss()
                if (Utility.isOnline(this)) {
                    startDownload()
                } else {
                    createCheckConnectionAlertDialog()
                    enableInitDownload()
                }
            }
            builder.setNegativeButton(getString(R.string.after_download)) { _, _ ->
                enableInitDownload()
                dialog?.dismiss()
            }
            dialog = builder.create()
            dialog.setCanceledOnTouchOutside(false)
            dialog.setCancelable(false)
            dialog.show()
        } catch (e: Exception) {
        }
    }

    private fun prepareForDownload() {
        viewModel.resetCurrentRetry()
        viewModel.setShouldInitDownload(true)
        viewModel.setDownloadAsAvailable()
    }

    private fun startSyncData() {
        whiteLabelApplication.setWorkManager()
    }

    private fun enableInitDownload() {
        binding.resumeDownload.hide()
        binding.initDownload.show()
        binding.qrButton.background.alpha = 128
        hideDownloadProgressViews()
        binding.dateLastSyncText.text = when (viewModel.getTotalSizeInByte()) {
            0L -> {
                hideDownloadProgressViews()
                getString(
                    R.string.label_download_alert_simple
                )
            }
            else ->
                getString(
                    R.string.label_download_alert_complete,
                    ConversionUtility.byteToMegaByte(viewModel.getTotalSizeInByte().toFloat())
                )
        }
    }

    override fun onResume() {
        super.onResume()
        setScanModeButtonText(viewModel.getScanMode())
        checkAppMinimumVersion()
    }

    private fun checkAppMinimumVersion() {
        viewModel.getAppMinVersion().let {
            if (Utility.versionCompare(
                    it,
                    BuildConfig.VERSION_NAME
                ) > 0 || viewModel.isSDKVersionObsolete()
            ) {
                createForceUpdateDialog()
            }
        }
    }

    private fun openQrCodeReader() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    private fun openSettings() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.qrButton -> {
                viewModel.getDateLastSync().let {
                    if (it == -1L) {
                        createNoSyncAlertDialog(getString(R.string.noKeyAlertMessage))
                        return
                    } else if (!viewModel.getScanModeFlag() && v.id != R.id.scan_mode_button) {
                        viewModel.getRuleSet()?.getErrorScanModePopup()?.run {
                            createNoScanModeChosenAlert()
                        } ?: run { createNoSyncAlertDialog(getString(R.string.noKeyAlertMessage)) }
                        return
                    }
                }
                viewModel.getDrlDateLastSync().let {
                    if (binding.resumeDownload.isVisible) {
                        createNoSyncAlertDialog(getString(R.string.label_drl_download_in_progress))
                        return
                    }
                    if ((viewModel.getIsDrlSyncActive() && System.currentTimeMillis() >= it + 24 * 60 * 60 * 1000) ||
                        (viewModel.getIsDrlSyncActive() && it == -1L)
                    ) {
                        createNoSyncAlertDialog(getString(R.string.noKeyAlertMessageForDrl))
                        return
                    }
                }
                checkCameraPermission()
            }
            R.id.settings -> openSettings()
            R.id.scan_mode_button -> {
                viewModel.getRuleSet()?.run {
                    ScanModeDialogFragment(viewModel.getRuleSet()!!).show(supportFragmentManager, "SCAN_MODE_DIALOG_FRAGMENT")
                } ?: run {
                    createNoSyncAlertDialog(getString(R.string.noKeyAlertMessage))
                }
            }
            R.id.circle_info_container -> {
                viewModel.getRuleSet()?.getBaseScanModeDescription()?.run {
                    createScanModeInfoAlert()
                } ?: run { createNoSyncAlertDialog(getString(R.string.noKeyAlertMessage)) }
            }
        }
    }

    private fun createNoScanModeChosenAlert() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.noKeyAlertTitle))
        val string =
            SpannableString(Html.fromHtml(viewModel.getRuleSet()?.getErrorScanModePopup(), HtmlCompat.FROM_HTML_MODE_LEGACY)).also {
                Linkify.addLinks(it, Linkify.ALL)
            }
        builder.setMessage(string)
        builder.setPositiveButton(getString(R.string.ok)) { _, _ ->
        }
        val dialog = builder.create()
        dialog.show()
        val alertMessage = dialog.findViewById<TextView>(android.R.id.message) as TextView
        alertMessage.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun createScanModeInfoAlert() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.label_scan_mode_types))
        val string = SpannableString(Html.fromHtml(viewModel.getRuleSet()?.getInfoScanModePopup(), HtmlCompat.FROM_HTML_MODE_LEGACY)).also {
            Linkify.addLinks(it, Linkify.ALL)
        }
        builder.setMessage(string)
        builder.setPositiveButton(getString(R.string.ok)) { _, _ ->
        }
        val dialog = builder.create()
        dialog.show()
        val alertMessage = dialog.findViewById<TextView>(android.R.id.message) as TextView
        alertMessage.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun createNoSyncAlertDialog(alertMessage: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.noKeyAlertTitle))
        builder.setMessage(alertMessage)
        builder.setPositiveButton(getString(R.string.ok)) { _, _ ->
        }
        val dialog = builder.create()
        dialog.show()
    }

    private fun createForceUpdateDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.updateTitle))
        builder.setMessage(getString(R.string.updateMessage))

        builder.setPositiveButton(getString(R.string.updateLabel)) { _, _ ->
            openGooglePlay()
        }
        val dialog = builder.create()
        dialog.setCancelable(false)
        dialog.show()
    }

    private fun openGooglePlay() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
        } catch (e: ActivityNotFoundException) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                )
            )
        }
    }

    override fun onStart() {
        super.onStart()
        shared.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key != null) {
            when (key) {
                PrefKeys.CURRENT_CHUNK -> {
                    updateDownloadedPackagesCount()
                    Log.i(key.toString(), viewModel.getCurrentChunk().toString())
                }
                PrefKeys.KEY_TOTAL_CHUNK -> {
                    val totalChunk = viewModel.getTotalChunk().toInt()
                    binding.updateProgressBar.max = totalChunk
                    binding.updateProgressBar.show()
                    binding.chunkCount.show()
                    binding.chunkSize.show()
                    updateDownloadedPackagesCount()
                    Log.i(PrefKeys.KEY_TOTAL_CHUNK, totalChunk.toString())
                }
                PrefKeys.AUTH_TO_RESUME -> {
                    val authToResume = viewModel.getResumeAvailable().toInt()
                    Log.i(PrefKeys.AUTH_TO_RESUME, authToResume.toString())
                    if (viewModel.getResumeAvailable() == 0L) {
                        binding.resumeDownload.show()
                        binding.qrButton.background.alpha = 128
                    }
                }
                PrefKeys.KEY_DRL_DATE_LAST_FETCH -> {
                    viewModel.getDateLastSync().let { date ->
                        binding.dateLastSyncText.text = getString(
                            R.string.lastSyncDate,
                            if (date == -1L) getString(R.string.notAvailable) else date.parseTo(
                                FORMATTED_DATE_LAST_SYNC
                            )
                        )
                    }
                    hideDownloadProgressViews()
                }
                else -> {

                }
            }
        }
    }

    private fun hideDownloadProgressViews() {
        binding.updateProgressBar.hide()
        binding.chunkCount.hide()
        binding.chunkSize.hide()
    }

    private fun showDownloadProgressViews() {
        binding.updateProgressBar.show()
        binding.chunkCount.show()
        binding.chunkSize.show()
    }

    override fun onBackPressed() {
        moveTaskToBack(true)
    }

    override fun onDestroy() {
        super.onDestroy()
        shared.unregisterOnSharedPreferenceChangeListener(this)
    }

    private fun updateDownloadedPackagesCount() {
        val lastDownloadedChunk = viewModel.getCurrentChunk().toInt()
        val lastChunk = viewModel.getTotalChunk().toInt()
        val singleChunkSize = viewModel.getSizeSingleChunkInByte()

        binding.updateProgressBar.progress = lastDownloadedChunk
        binding.chunkCount.text = getString(R.string.chunk_count, lastDownloadedChunk, lastChunk)
        binding.chunkSize.text = getString(
            R.string.chunk_size,
            ConversionUtility.byteToMegaByte((lastDownloadedChunk * singleChunkSize.toFloat())),
            ConversionUtility.byteToMegaByte(viewModel.getTotalSizeInByte().toFloat())
        )
    }

    override fun onDismiss(dialog: DialogInterface?) {
        setScanModeButtonText(viewModel.getScanMode())
    }

}
