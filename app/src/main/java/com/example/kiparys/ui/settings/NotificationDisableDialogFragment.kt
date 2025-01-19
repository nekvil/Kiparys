package com.example.kiparys.ui.settings

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.example.kiparys.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder


class NotificationDisableDialogFragment : DialogFragment() {

    interface NotificationDialogListener {
        fun onConfirm()
        fun onCancel()
    }

    private var listener: NotificationDialogListener? = null
    private var isConfirmed: Boolean = false

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = try {
            parentFragment as NotificationDialogListener
        } catch (_: ClassCastException) {
            throw ClassCastException("$context must implement NotificationDialogListener")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_title_disable_notifications)
            .setMessage(R.string.dialog_message_disable_notifications)
            .setPositiveButton(R.string.action_confirm) { _, _ ->
                isConfirmed = true
                listener?.onConfirm()
            }
            .setNegativeButton(R.string.action_cancel) { _, _ ->
                listener?.onCancel()
            }
            .create()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (!isConfirmed) {
            listener?.onCancel()
        }
    }

    override fun onPause() {
        super.onPause()
        dismiss()
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

}
