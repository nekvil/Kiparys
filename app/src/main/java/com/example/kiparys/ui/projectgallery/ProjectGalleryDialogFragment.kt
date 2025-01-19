package com.example.kiparys.ui.projectgallery

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import com.example.kiparys.R
import com.example.kiparys.ui.projectdetails.ProjectChatFragment
import com.example.kiparys.ui.projectdetails.ProjectDetailsViewModel
import com.example.kiparys.ui.theme.KiparysTheme
import kotlin.getValue


class ProjectGalleryDialogFragment : DialogFragment() {

    private val projectDetailsViewModel: ProjectDetailsViewModel by viewModels(
        { requireParentFragment().requireParentFragment() }
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return ComposeView(requireContext()).apply {
            setContent {
                dialog?.window?.let {
                    KiparysTheme {
                        GalleryScreen(
                            window = it,
                            projectDetailsViewModel = projectDetailsViewModel,
                            onBack = { dismiss() }
                        )
                    }
                }
            }
        }
    }


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext(), R.style.FullScreenDialogGalleryStyle)
        return dialog
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (parentFragment as? ProjectChatFragment)?.projectGalleryDialog = null
    }

    companion object {
        const val TAG = "ProjectGalleryDialogFragment"

        fun show(fragmentManager: FragmentManager) {
            if (fragmentManager.findFragmentByTag(TAG) == null) {
                val dialog = ProjectGalleryDialogFragment()
                dialog.show(fragmentManager, TAG)
            }
        }
    }

}
