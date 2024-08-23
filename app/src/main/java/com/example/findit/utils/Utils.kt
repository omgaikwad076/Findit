package com.example.findit.utils

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.Toast
import com.example.findit.databinding.ProgressDialogBinding
import com.google.firebase.auth.FirebaseAuth
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object Utils {

    private var dialog: AlertDialog? = null
    private var lastToast: Toast? = null
    private var lastToastTime: Long = 0
    private const val TOAST_DELAY = 2000 // Minimum delay between toasts in milliseconds

    fun showDialog(context: Context, message: String) {
        hideDialog() // Hide any existing dialog before showing a new one
        val builder = AlertDialog.Builder(context)
        builder.setMessage(message)
        builder.setCancelable(false)
        dialog = builder.create()
        dialog?.show()
    }

    fun hideDialog() {
        dialog?.dismiss()
        dialog = null
    }

    fun showToast(context: Context, message: String) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastToastTime > TOAST_DELAY) {
            lastToast?.cancel()
            lastToast = Toast.makeText(context, message, Toast.LENGTH_SHORT)
            lastToast?.show()
            lastToastTime = currentTime
        }
    }

    //to get the instance of user
    private var firebaseAuthInstance: FirebaseAuth? = null
    fun getAuthInstance(): FirebaseAuth {
        if (firebaseAuthInstance == null) {
            firebaseAuthInstance = FirebaseAuth.getInstance()
        }
        return firebaseAuthInstance!!
    }

    fun getCurrentUserId(): String? {
        val currentUser = FirebaseAuth.getInstance().currentUser
        return currentUser?.uid
    }

    fun getRandomId(): String {
        return (1..25).map { (('A'..'Z') + ('a'..'z') + ('0'..'9')).random() }.joinToString("")


    }

    fun getCurrentDate(): String {
        val currentDate = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
        return currentDate.format(formatter)


    }
}