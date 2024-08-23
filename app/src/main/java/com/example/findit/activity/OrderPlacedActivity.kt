package com.example.findit.activity

import android.app.AlertDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.findit.R
import com.example.findit.adapters.AdapterCartProducts
import com.example.findit.databinding.ActivityOrderPlacedBinding
import com.example.findit.databinding.AddressLayoutBinding
import com.example.findit.models.Orders
import com.example.findit.utils.CartListener
import com.example.findit.utils.Constants
import com.example.findit.utils.Utils
import com.example.findit.viewmodels.UserViewmodel
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import kotlinx.coroutines.launch
import org.json.JSONObject

class OrderPlacedActivity : AppCompatActivity(), PaymentResultListener {

    private lateinit var binding: ActivityOrderPlacedBinding
    private val viewModel: UserViewmodel by viewModels()
    private lateinit var adapterCartProducts: AdapterCartProducts
    private var cartListener: CartListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderPlacedBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setStatusBarColor()
        setupRecyclerView()
        getAllProducts()
        backToMainActivity()
        onPlaceOrderClicked()
    }

    private fun setupRecyclerView() {
        adapterCartProducts = AdapterCartProducts()
        binding.rvProductsItem.adapter = adapterCartProducts
    }

    private fun onPlaceOrderClicked() {
        binding.btnNext.setOnClickListener {
            viewModel.getAddressStatus().observe(this) { status ->
                if (status) {
                    val totalAmount = calculateTotalAmount()
                    initializeRazorpay(totalAmount)
                } else {
                    showAddressDialog()
                }
            }
        }
    }

    private fun initializeRazorpay(totalAmount: Int) {
        val checkout = Checkout()
        checkout.setKeyID("rzp_test_PEKWjHi3ynDCro") // Replace with your Razorpay Key ID

        try {
            val options = JSONObject()
            options.put("name", "Om Gaikwad")
            options.put("description", "Order Payment")
            options.put("image", "https://example.com/your_logo.png") // Optional
            options.put("currency", "INR")
            options.put("amount", totalAmount * 100) // Convert to paise

            val prefill = JSONObject()
            prefill.put("email", "omgaikwad7361@gmail.com") // Fetch from user's data
            prefill.put("contact", "9370156386") // Fetch from user's data

            options.put("prefill", prefill)

            checkout.open(this, options)
        } catch (e: Exception) {
            Log.e("Razorpay", "Error in starting Razorpay Checkout", e)
        }
    }

    override fun onPaymentSuccess(razorpayPaymentID: String) {
        Log.d("Razorpay", "Payment Successful: $razorpayPaymentID")
        Utils.showToast(this, "Payment Successful!")
        saveOrder()
    }

    override fun onPaymentError(code: Int, response: String?) {
        Log.e("Razorpay", "Payment failed: $code\n$response")
        Utils.showToast(this, "Payment Failed: $response")
    }

    private fun saveOrder() {
        viewModel.getAll().observe(this) { cartProductList ->
            if (cartProductList.isNotEmpty()) {
                viewModel.getUserAddress { address ->
                    val orders = Orders(
                        orderId = Utils.getRandomId(),
                        orderList = cartProductList,
                        userAddress = address,
                        orderStatus = 0,
                        orderDate = Utils.getCurrentDate(),
                        orderingUserId = Utils.getCurrentUserId()
                    )
                    viewModel.saveOrderProducts(orders)
                    viewModel.deleteCartProducts()
                    viewModel.savingCartItemCount(0)
                    cartListener?.hideCartLayout()
                    Utils.showToast(this, "Order Placed Successfully!")
                    startActivity(Intent(this, UsersMainActivity::class.java))
                    finish()
                }
                for (product in cartProductList) {
                    val count = product.productCount ?: 0
                    val stock = (product.productStock ?: 0) - count
                    viewModel.saveProductsAfterOrder(stock, product)
                }
            }
        }
    }

    private fun showAddressDialog() {
        val addressLayoutBinding = AddressLayoutBinding.inflate(LayoutInflater.from(this))
        val alertDialog = AlertDialog.Builder(this)
            .setView(addressLayoutBinding.root)
            .create()
        alertDialog.show()

        addressLayoutBinding.btnAddAddress.setOnClickListener {
            saveAddress(alertDialog, addressLayoutBinding)
        }
    }

    private fun saveAddress(
        alertDialog: AlertDialog,
        addressLayoutBinding: AddressLayoutBinding
    ) {
        Utils.showDialog(this, "Processing...")
        val userPinCode = addressLayoutBinding.etPinCode.text.toString().trim()
        val userPhoneNumber = addressLayoutBinding.etPhoneNo.text.toString().trim()
        val userState = addressLayoutBinding.etState.text.toString().trim()
        val userDistrict = addressLayoutBinding.etDistrict.text.toString().trim()
        val userAddress = addressLayoutBinding.etAddress.text.toString().trim()

        if (userPinCode.isEmpty() || userPhoneNumber.isEmpty() || userState.isEmpty() ||
            userDistrict.isEmpty() || userAddress.isEmpty()
        ) {
            Utils.showToast(this, "Please fill all the fields")
            Utils.hideDialog()
            return
        }

        val address = "$userAddress, $userDistrict, $userState - $userPinCode, Phone: $userPhoneNumber"

        lifecycleScope.launch {
            viewModel.saveUserAddress(address)
            viewModel.saveAddressStatus()
            Utils.hideDialog()
            alertDialog.dismiss()
            val totalAmount = calculateTotalAmount()
            initializeRazorpay(totalAmount)
        }
    }

    private fun calculateTotalAmount(): Int {
        var totalPrice = 0
        val cartProductsList = adapterCartProducts.differ.currentList

        for (product in cartProductsList) {
            val price = product.productPrice?.replace("₹", "")?.toIntOrNull() ?: 0
            val itemCount = product.productCount ?: 0
            totalPrice += price * itemCount
        }

        // Update UI
        binding.tvSubTotal.text = "₹$totalPrice"

        var deliveryCharge = 0
        if (totalPrice < 200) {
            deliveryCharge = 40
            binding.tvDeliveryCharge.text =     "₹$deliveryCharge"
        } else {
            binding.tvDeliveryCharge.text = "Free"
        }

        val finalTotal = totalPrice + deliveryCharge
        binding.tvFinalTotal.text = "₹$finalTotal"

        return finalTotal
    }

    private fun getAllProducts() {
        viewModel.getAll().observe(this) { cartProductsList ->
            adapterCartProducts.differ.submitList(cartProductsList)
            calculateTotalAmount()
        }
    }

    private fun backToMainActivity() {
        binding.tbOrderFragment.setNavigationOnClickListener {
            startActivity(Intent(this, UsersMainActivity::class.java))
            finish()
        }
    }

    private fun setStatusBarColor() {
        window?.apply {
            val statusBarColor = ContextCompat.getColor(this@OrderPlacedActivity, R.color.diyaFlame)
            this.statusBarColor = statusBarColor
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
        }
    }
}
