package com.example.findit.auth

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController

import com.example.findit.R
import com.example.findit.activity.UsersMainActivity
import com.example.findit.databinding.FragmentOTPBinding
import com.example.findit.models.Users
import com.example.findit.utils.Utils
import com.example.findit.viewmodels.AuthViewModel
import kotlinx.coroutines.launch


class OTPFragment : Fragment() {
    private val viewModel : AuthViewModel by viewModels()
    private lateinit var binding: FragmentOTPBinding
    private lateinit var userNumber: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentOTPBinding.inflate(inflater, container, false)
        getUserNumber()
        sendOTP()
        customizingEnteringOTP()
        onLoginButtonClicked()
        onBackButtonClicked()
        return binding.root
    }

    private fun sendOTP(){
        Utils.showDialog(requireContext(), "Sending OTP...")
        viewModel.apply {
            sendOTP(userNumber, requireActivity())
            lifecycleScope.launch {
                otpSent.collect{ otpSent ->
                    if(otpSent){
                        Utils.hideDialog()
                        Utils.showToast(requireContext(), "OTP Sent to the number")
                    }
                    1}
            }

        }
    }

    private fun verifyOtp(otp: String) {
        val user = Users(uid = null, userPhoneNumber = userNumber, userAddress = " ")

        viewModel.signInWithPhoneAuthCredential(otp, userNumber, user)
        lifecycleScope.launch {
            try {
                viewModel.isSignedInSuccessfully.collect { isSuccess ->
                    Utils.hideDialog() // Ensure dialog is hidden
                    if (isSuccess) {
                        Utils.showToast(requireContext(), "Logged In...")
                        startActivity(Intent(requireActivity(), UsersMainActivity::class.java))
                        requireActivity().finish()
                    } else {
                        Utils.showToast(requireContext(), "Login Failed")
                        binding.btnlogin.isEnabled = true // Re-enable button on failure
                    }
                }
            } catch (e: Exception) {
                Utils.hideDialog()
                Utils.showToast(requireContext(), "An error occurred")
                binding.btnlogin.isEnabled = true // Re-enable button on error
            }
        }
    }



    private fun onLoginButtonClicked() {
        binding.btnlogin.setOnClickListener {
            // Disable the button to prevent multiple clicks
            binding.btnlogin.isEnabled = false

            Utils.showDialog(requireContext(), "Signing you in...")
            val editTexts = arrayOf(binding.etOtp1, binding.etOtp2, binding.etOtp3, binding.etOtp4, binding.etOtp5, binding.etOtp6)
            val otp = editTexts.joinToString("") { it.text.toString() }

            if (otp.length < editTexts.size) {
                Utils.showToast(requireContext(), "Please enter the correct OTP")
                // Re-enable the button if there's an error
                binding.btnlogin.isEnabled = true
            } else {
                // Clear OTP fields and focus
                editTexts.forEach { it.text?.clear(); it.clearFocus() }
                verifyOtp(otp)
            }
        }
    }


    private fun customizingEnteringOTP(){
        val editTexts = arrayOf(binding.etOtp1,binding.etOtp2,binding.etOtp3,binding.etOtp4,binding.etOtp5,binding.etOtp6)
        for (i in editTexts.indices){
            editTexts[i].addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int
                ) {

                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                }

                override fun afterTextChanged(s: Editable?) {
                    if (s?. length == 1){
                        if(i < editTexts.size-1){
                            editTexts[i+1].requestFocus()
                        }
                    } else if (s?. length == 0){
                        if(i > 0){
                            editTexts[i-1].requestFocus()
                        }
                    }
                }
            })
        }
    }

    private fun onBackButtonClicked(){
        binding.tbOtpFragment.setNavigationOnClickListener {
            findNavController().navigate(R.id.action_OTPFragment_to_signInFragment)
        }
    }

    private fun getUserNumber() {
        val bundle = arguments
        userNumber = bundle?.getString("number").toString()
        binding.tvUserNumber.text = userNumber
    }
}