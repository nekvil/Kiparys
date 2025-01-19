package com.example.kiparys.ui.projectdetails

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter.StateRestorationPolicy
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import coil.imageLoader
import coil.request.ImageRequest
import coil.transform.RoundedCornersTransformation
import com.example.kiparys.Constants.MAX_IMAGE_SIZE
import com.example.kiparys.KiparysApplication
import com.example.kiparys.R
import com.example.kiparys.data.model.ProjectIdea
import com.example.kiparys.databinding.BottomSheetCreateIdeaBinding
import com.example.kiparys.databinding.BottomSheetOptionsBinding
import com.example.kiparys.databinding.FragmentProjectIdeasBinding
import com.example.kiparys.ui.adapter.ProjectIdeaOptionsAdapter
import com.example.kiparys.ui.adapter.ProjectIdeasAdapter
import com.example.kiparys.ui.main.MainViewModel
import com.example.kiparys.util.ErrorUtil
import com.example.kiparys.util.ImageUtil.getBitmapFromUri
import com.example.kiparys.util.ImageUtil.getFileSizeFromUri
import com.example.kiparys.util.SystemUtil.isKeyboardVisible
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.getValue


class ProjectIdeasFragment : Fragment() {

    private var _binding: FragmentProjectIdeasBinding? = null
    private val fragmentProjectIdeasBinding get() = _binding!!
    private lateinit var captureImageLauncher: ActivityResultLauncher<Intent>
    private var createIdeaDialog: BottomSheetDialog? = null
    private var deleteIdeaDialog: AlertDialog? = null
    private var projectIdeaOptionsDialog: BottomSheetDialog? = null
    private lateinit var currentPhotoPath: String
    private lateinit var photoURI: Uri
    private lateinit var projectIdeasAdapter: ProjectIdeasAdapter
    private val projectDetailsViewModel: ProjectDetailsViewModel by viewModels({ requireParentFragment() })
    private val mainViewModel: MainViewModel by activityViewModels()
    private val imm by lazy {
        requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    }
    private var isGalleryOpen = false
    private val galleryLauncherSingle =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            isGalleryOpen = false
            uri?.let {
                val fileSizeInBytes = getFileSizeFromUri(requireContext(), uri)
                val imageBitmap = getBitmapFromUri(requireContext(), uri)
                if (imageBitmap != null) {
                    if (fileSizeInBytes > MAX_IMAGE_SIZE) {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.warning_image_size), Toast.LENGTH_LONG
                        ).show()
                    } else {
                        projectDetailsViewModel.setIdeaImageBitmap(imageBitmap)
                    }
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProjectIdeasBinding.inflate(inflater, container, false)
        return fragmentProjectIdeasBinding.root
    }

    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir: File =
            requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        captureImageLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val imageBitmap = BitmapFactory.decodeFile(currentPhotoPath)
                    val imageFile = File(currentPhotoPath)
                    val fileSizeInBytes = imageFile.length()

                    if (fileSizeInBytes > MAX_IMAGE_SIZE) {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.warning_image_size),
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        projectDetailsViewModel.setIdeaImageBitmap(imageBitmap)
                    }
                }
            }
        viewLifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                fragmentProjectIdeasBinding.rvIdeas.adapter = null
                super.onDestroy(owner)
            }
        })

        projectIdeasAdapter = ProjectIdeasAdapter(
            currentUserId = mainViewModel.userId.value.toString(),
            onIdeaLongClickListener = { idea ->
                showProjectIdeaOptionsDialog(idea)
            },
            onThumbUpClickListener = { idea ->
                projectDetailsViewModel.toggleIdeaVoteState(idea)
            }
        ).apply {
            registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
                override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                    super.onItemRangeInserted(positionStart, itemCount)
                    val layoutManager =
                        fragmentProjectIdeasBinding.rvIdeas.layoutManager as? LinearLayoutManager
                    layoutManager?.let {
                        val isAtTop = positionStart == 0
                        if (isAtTop) {
                            fragmentProjectIdeasBinding.rvIdeas.scrollToPosition(0)
                        }
                    }
                }
            })
            stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }
        fragmentProjectIdeasBinding.rvIdeas.apply {
            adapter = projectIdeasAdapter
            layoutManager =
                StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL).apply {
                    reverseLayout = false
                }
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy > 0) {
                        fragmentProjectIdeasBinding.fabAddIdea.hide()
                    } else if (dy < 0) {
                        fragmentProjectIdeasBinding.fabAddIdea.show()
                    }
                }
            })
        }

        fragmentProjectIdeasBinding.fabAddIdea.setOnClickListener {
            showCreateIdeaBottomSheetDialog()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {

                launch {
                    projectDetailsViewModel.projectIdeasUiState.collect { uiState ->
                        fragmentProjectIdeasBinding.lpiProjectIdeas.visibility =
                            if (uiState.isLoading) View.VISIBLE else View.GONE
                        projectIdeasAdapter.submitList(uiState.projectIdeas)
                        if (!uiState.isLoading) {
                            fragmentProjectIdeasBinding.clProjectIdeasPreview.visibility =
                                if (uiState.projectIdeas.isNotEmpty()) View.GONE else View.VISIBLE
                        }
                    }
                }

                launch {
                    projectDetailsViewModel.projectMainScreenUiState.collect { uiState ->
                        uiState.error?.let { error ->
                            requireView().postDelayed({
                                Snackbar.make(
                                    requireView(),
                                    ErrorUtil.getErrorMessage(requireContext(), error),
                                    Snackbar.LENGTH_LONG
                                )
                                    .show()
                            }, 250)
                            projectDetailsViewModel.errorMessageShown()
                        }

                        if (uiState.saveIdeaSuccess) {
                            createIdeaDialog?.dismiss()
                            projectDetailsViewModel.saveIdeaSuccessMessageShown()
                        }

                        if (uiState.showConfirmDeleteIdeaDialog) {
                            deleteIdeaDialog = MaterialAlertDialogBuilder(requireContext())
                                .setTitle(getString(R.string.dialog_title_delete_idea))
                                .setMessage(getString(R.string.dialog_message_delete_task))
                                .setNegativeButton(getString(R.string.action_cancel)) { dialog, _ ->
                                    dialog.dismiss()
                                }
                                .setPositiveButton(getString(R.string.action_delete_for_all)) { dialog, _ ->
                                    projectDetailsViewModel.confirmDeleteIdea()
                                    dialog.dismiss()
                                }
                                .setOnDismissListener {
                                    deleteIdeaDialog = null
                                }
                                .create()
                            deleteIdeaDialog?.show()
                            projectDetailsViewModel.confirmDeleteIdeaDialogShown()
                        }
                    }
                }
            }
        }
    }

    private fun showProjectIdeaOptionsDialog(idea: ProjectIdea) {
        if (projectIdeaOptionsDialog?.isShowing == true) {
            return
        }

        projectIdeaOptionsDialog = BottomSheetDialog(requireContext())
        projectIdeaOptionsDialog?.behavior?.isFitToContents = true
        projectIdeaOptionsDialog?.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }

        val bottomSheetSelectTwoFactorBinding = BottomSheetOptionsBinding.inflate(layoutInflater)

        val adapter = mainViewModel.userId.value?.let {
            ProjectIdeaOptionsAdapter(requireContext(), it, idea) { selectedOption ->
                projectDetailsViewModel.handleProjectIdeaOptionSelection(selectedOption, idea)
                projectIdeaOptionsDialog?.dismiss()
            }
        }
        bottomSheetSelectTwoFactorBinding.rvOptions.adapter = adapter
        bottomSheetSelectTwoFactorBinding.rvOptions.layoutManager = LinearLayoutManager(context)

        projectIdeaOptionsDialog?.setOnDismissListener {
            bottomSheetSelectTwoFactorBinding.root.parent?.let {
                (it as ViewGroup).removeView(bottomSheetSelectTwoFactorBinding.root)
            }
            projectIdeaOptionsDialog = null
        }

        projectIdeaOptionsDialog?.setContentView(bottomSheetSelectTwoFactorBinding.root)
        projectIdeaOptionsDialog?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED
        if (isKeyboardVisible(requireActivity())) {
            imm.hideSoftInputFromWindow(requireView().windowToken, 0)
            requireView().post { projectIdeaOptionsDialog?.show() }
        } else {
            projectIdeaOptionsDialog?.show()
        }

    }

    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(
                requireContext(),
                getString(R.string.toast_camera_permission_denied),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }

            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.toast_camera_permission_required),
                    Toast.LENGTH_LONG
                ).show()
            }

            else -> {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun openCamera() {
        val photoFile: File = createImageFile()
        photoURI = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            photoFile
        )

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
        }
        captureImageLauncher.launch(intent)
    }

    private fun showCreateIdeaBottomSheetDialog() {
        if (createIdeaDialog?.isShowing == true) {
            return
        }

        createIdeaDialog = BottomSheetDialog(requireContext())
        createIdeaDialog?.behavior?.isFitToContents = true
        createIdeaDialog?.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }

        val bottomSheetCreateIdeaBinding = BottomSheetCreateIdeaBinding.inflate(layoutInflater)

        bottomSheetCreateIdeaBinding.etIdeaDescription.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                bottomSheetCreateIdeaBinding.mbSaveIdea.isEnabled = !s.isNullOrEmpty()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        bottomSheetCreateIdeaBinding.mbAttachPhoto.setOnClickListener {
            bottomSheetCreateIdeaBinding.root.clearFocus()
            if (isKeyboardVisible(requireActivity())) {
                imm.hideSoftInputFromWindow(bottomSheetCreateIdeaBinding.root.windowToken, 0)
                bottomSheetCreateIdeaBinding.root.post { checkCameraPermission() }
            } else {
                checkCameraPermission()
            }
        }

        bottomSheetCreateIdeaBinding.mbAttachPicture.setOnClickListener {
            bottomSheetCreateIdeaBinding.root.clearFocus()
            if (!isGalleryOpen) {
                isGalleryOpen = true
                bottomSheetCreateIdeaBinding.root.clearFocus()
                if (isKeyboardVisible(requireActivity())) {
                    imm.hideSoftInputFromWindow(bottomSheetCreateIdeaBinding.root.windowToken, 0)
                    bottomSheetCreateIdeaBinding.root.postDelayed(
                        { galleryLauncherSingle.launch("image/*") },
                        300
                    )
                } else {
                    galleryLauncherSingle.launch("image/*")
                }
            }
        }

        bottomSheetCreateIdeaBinding.mbSaveIdea.setOnClickListener {
            val ideaDescriptionInput = bottomSheetCreateIdeaBinding.etIdeaDescription.text
                .toString().trim().replace("\\s+".toRegex(), " ")
            val messageToMember = getString(
                R.string.notification_message_new_idea,
                projectDetailsViewModel.getCurrentUserDisplayName(),
                ideaDescriptionInput
            )
            projectDetailsViewModel.saveIdea(
                ideaDescription = ideaDescriptionInput,
                messageToMember = messageToMember
            )
        }

        var previousImageBitmap: Bitmap? = null

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                projectDetailsViewModel.projectMainScreenUiState.collect { uiState ->
                    if (bottomSheetCreateIdeaBinding.etIdeaDescription.text?.isNotEmpty() == true) {
                        bottomSheetCreateIdeaBinding.mbSaveIdea.isEnabled = !uiState.isSaveIdea
                    }
                    bottomSheetCreateIdeaBinding.cpiIdeaSave.visibility =
                        if (uiState.isSaveIdea) View.VISIBLE else View.GONE
                    bottomSheetCreateIdeaBinding.mbSaveIdea.text =
                        if (uiState.isSaveIdea) getString(R.string.prompt_empty_string) else getString(
                            R.string.action_save
                        )

                    if (uiState.ideaImageBitmap != previousImageBitmap) {
                        previousImageBitmap = uiState.ideaImageBitmap

                        val chipGroup = bottomSheetCreateIdeaBinding.cgImage
                        chipGroup.removeAllViews()

                        bottomSheetCreateIdeaBinding.hsvChipGroup.visibility =
                            if (uiState.ideaImageBitmap != null) View.VISIBLE else View.GONE

                        uiState.ideaImageBitmap?.let { bitmap ->
                            val chipImage = layoutInflater.inflate(
                                R.layout.chip_image,
                                chipGroup,
                                false
                            ) as Chip
                            val imageLoader =
                                (requireContext().applicationContext as KiparysApplication).imageLoader
                            val request = ImageRequest.Builder(requireContext())
                                .data(bitmap)
                                .transformations(RoundedCornersTransformation(2f))
                                .placeholder(
                                    ColorDrawable(
                                        MaterialColors.getColor(
                                            fragmentProjectIdeasBinding.root,
                                            com.google.android.material.R.attr.colorSurfaceContainerLow
                                        )
                                    )
                                )
                                .error(
                                    ColorDrawable(
                                        MaterialColors.getColor(
                                            fragmentProjectIdeasBinding.root,
                                            com.google.android.material.R.attr.colorSurfaceContainerLow
                                        )
                                    )
                                )
                                .target { drawable -> chipImage.chipIcon = drawable }
                                .build()
                            imageLoader.enqueue(request)

                            chipImage.setOnCloseIconClickListener {
                                projectDetailsViewModel.setIdeaImageBitmap(null)
                            }
                            chipGroup.addView(chipImage)
                        }
                    }
                }
            }
        }

        createIdeaDialog?.setOnDismissListener {
            bottomSheetCreateIdeaBinding.root.parent?.let {
                (it as ViewGroup).removeView(bottomSheetCreateIdeaBinding.root)
            }
            projectDetailsViewModel.setIdeaImageBitmap(null)
            createIdeaDialog = null
        }

        createIdeaDialog?.setContentView(bottomSheetCreateIdeaBinding.root)
        createIdeaDialog?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED
        if (isKeyboardVisible(requireActivity())) {
            imm.hideSoftInputFromWindow(requireView().windowToken, 0)
            requireView().postDelayed({
                createIdeaDialog?.show()
            }, 60)
        } else {
            createIdeaDialog?.show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
