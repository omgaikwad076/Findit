package com.example.findit.activity

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.findit.R
import com.example.findit.Utils
import com.example.findit.adapters.AdapterCartProducts
import com.example.findit.databinding.ActivityOrderPlaceBinding
import com.example.findit.databinding.AddressLayoutBinding
import com.example.findit.viewmodels.UserViewModel
import com.phonepe.intent.sdk.api.B2BPGRequest
import com.razorpay.Checkout
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import kotlinx.coroutines.launch
import org.json.JSONObject


class OrderPlaceActivity : AppCompatActivity(), PaymentResultWithDataListener {
    private lateinit var binding : ActivityOrderPlaceBinding
    private val viewModel : UserViewModel by viewModels()
    private lateinit var adapterCartProducts: AdapterCartProducts

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderPlaceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Checkout.preload(applicationContext)
        val co = Checkout()
        co.setKeyID("rzp_test_zobLFgUK2yDhuK")

        getAllCartProducts()
        setStatusBarColor()
        backToUserMainActivity()
        OnPlaceOrderClicked()


    }

    private fun initializeRazorpay() {
        val activity: Activity = this
        val co = Checkout()

        try {
            val options = JSONObject()
            options.put("name","Findit")
            options.put("description","India ka apna app")
            //You can omit the image option to fetch the image from the dashboard
            options.put("image","findit_icon.png")
            options.put("theme.color", "#ffbe0b");
            options.put("currency","INR");
            options.put("amount","50000")

            val retryObj = JSONObject();
            retryObj.put("enabled", true);
            retryObj.put("max_count", 4);
            options.put("retry", retryObj);

            val prefill = JSONObject()
            prefill.put("email","omgaikwad7361@gmail.com")
            prefill.put("contact","9370156386")

            options.put("prefill",prefill)
            co.open(activity,options)
        }catch (e: Exception){
            Toast.makeText(activity,"Error in payment: "+ e.message,Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }




    private fun OnPlaceOrderClicked() {
        binding.btnNext.setOnClickListener {
            viewModel.getAddressStatus().observe(this){status ->
                if(status){
                    // Payment
                    initializeRazorpay()

                }
                else{
                    // Enter address
                    val addressLayoutBinding = AddressLayoutBinding.inflate(LayoutInflater.from(this))
                    val alertDialog = AlertDialog.Builder(this)
                        .setView(addressLayoutBinding.root)
                        .create()
                    alertDialog.show()

                    addressLayoutBinding.btnAdd.setOnClickListener {
                        saveAddress(alertDialog, addressLayoutBinding)
                    }
                }
            }
        }
    }


    private fun saveAddress(alertDialog: AlertDialog, addressLayoutBinding: AddressLayoutBinding) {
        Utils.showDialog(this, "Processing...")
        val userPinCode = addressLayoutBinding.etPinCode.text.toString()
        val userPhoneNumber = addressLayoutBinding.etPhoneNumber.text.toString()
        val userState = addressLayoutBinding.etState.text.toString()
        val userDistrict = addressLayoutBinding.etDistrict.text.toString()
        val userAddress = addressLayoutBinding.etDiscriptiveAddress.text.toString()

        val address = "$userPinCode, $userDistrict($userState), $userAddress, $userPhoneNumber"


        lifecycleScope.launch {
            viewModel.saveUserAddress(address)
            viewModel.saveAddressStatus()
        }
        Utils.showToast(this, "Saved...")
        alertDialog.dismiss()
        Utils.hideDialog()
    }

    private fun backToUserMainActivity() {
        binding.tbOrderFragment.setNavigationOnClickListener {
            finish()
        }
    }

    private fun getAllCartProducts() {
        viewModel.getAll().observe(this){cartProductList ->
            adapterCartProducts = AdapterCartProducts()
            binding.rvProductsItems.adapter = adapterCartProducts
            adapterCartProducts.differ.submitList(cartProductList)

            var totalPrice = 0
            for(products in cartProductList){
                val price = products.productPrice.substring(1)?.toInt()
                val itemCount = products.productCount!!
                totalPrice += (price?.times(itemCount))!!
            }

            binding.tvSubTotal.text = totalPrice.toString()

            if(totalPrice < 500){
                binding.tvDeliveryCharge.text = "₹30"
                totalPrice += 30
            }

            binding.tvGrandTotal.text = totalPrice.toString()
        }
    }
    private fun setStatusBarColor(){
        window?.apply {
            val statusBarColors = ContextCompat.getColor(this@OrderPlaceActivity, R.color.yellow)
            statusBarColor = statusBarColors
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
        }
    }

    override fun onPaymentSuccess(p0: String?, p1: PaymentData?) {
        Utils.showToast(this, "Payment Successful")
    }

    override fun onPaymentError(p0: Int, p1: String?, p2: PaymentData?) {
        Utils.showToast(this, "Error : ${p1}")
    }
}