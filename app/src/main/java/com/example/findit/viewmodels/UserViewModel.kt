package com.example.findit.viewmodels

import android.app.Application
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.findit.Utils
import com.example.findit.models.Orders
import com.example.findit.models.Product
import com.example.findit.roomdb.CartProductDao
import com.example.findit.roomdb.CartProductDatabase
import com.example.findit.roomdb.CartProducts
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow

class UserViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPreferences: SharedPreferences = application.getSharedPreferences("My_Pref", MODE_PRIVATE)
    private val cartProductDao: CartProductDao = CartProductDatabase.getDatabaseInstance(application).cartsProductsDao()
    private val _paymentStatus = MutableStateFlow<Boolean>(false)
    val paymentStatus = _paymentStatus

    // Room DB
    suspend fun insertCartProduct(products: CartProducts) {
        cartProductDao.insertCartProduct(products)
    }

    fun getAll(): LiveData<List<CartProducts>> {
        return cartProductDao.getAllCartProducts()
    }

    fun deleteCartProducts() {
        cartProductDao.deleteCartProducts()
    }

    suspend fun updateCartProduct(products: CartProducts) {
        cartProductDao.updateCartProduct(products)
    }

    suspend fun deleteCartProduct(productId: String) {
        cartProductDao.deleteCartProduct(productId)
    }

    // Firebase call
    fun fetchAllTheProducts(): Flow<List<Product>> = callbackFlow {
        val db = FirebaseDatabase.getInstance().getReference("Admins").child("AllProducts")

        val eventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val products = ArrayList<Product>()
                for (product in snapshot.children) {
                    val prod = product.getValue(Product::class.java)
                    prod?.let { products.add(it) }
                }
                trySend(products)
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        }
        db.addValueEventListener(eventListener)
        awaitClose { db.removeEventListener(eventListener) }
    }

    fun onCategoryIconClicked(category: String): Flow<List<Product>> = callbackFlow {
        val db = FirebaseDatabase.getInstance().getReference("Admins").child("ProductCategory/${category}")

        val eventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val products = ArrayList<Product>()
                for (product in snapshot.children) {
                    val prod = product.getValue(Product::class.java)
                    prod?.let { products.add(it) }
                }
                trySend(products)
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        }
        db.addValueEventListener(eventListener)
        awaitClose { db.removeEventListener(eventListener) }
    }

    fun updateItemCount(product: Product, itemCount: Int) {
        val db = FirebaseDatabase.getInstance().getReference("Admins")
        val updates = mapOf(
            "AllProducts/${product.productRandomId}/itemCount" to itemCount,
            "ProductCategory/${product.productCategory}/${product.productRandomId}/itemCount" to itemCount,
            "ProductType/${product.productType}/${product.productRandomId}/itemCount" to itemCount
        )
        db.updateChildren(updates)
    }

    fun saveProductsAfterOrder(stock: Int, product: CartProducts) {
        val db = FirebaseDatabase.getInstance().getReference("Admins")
        val updates = mapOf(
            "AllProducts/${product.productId}/itemCount" to 0,
            "ProductCategory/${product.productCategory}/${product.productId}/itemCount" to 0,
            "AllProducts/${product.productId}/productStock" to stock,
            "ProductCategory/${product.productCategory}/${product.productId}/productStock" to stock
        )
        db.updateChildren(updates)
    }

    fun saveUserAddress(address: String) {
        FirebaseDatabase.getInstance().getReference("AllUsers/Users/${Utils.getCurrentUserId()}/userAddress").setValue(address)
    }

    fun getUserAddress(callback: (String?) -> Unit) {
        val db = FirebaseDatabase.getInstance().getReference("AllUsers/Users/${Utils.getCurrentUserId()}/userAddress")
        db.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                callback(snapshot.getValue(String::class.java))
            }

            override fun onCancelled(error: DatabaseError) {
                callback(null)
            }
        })
    }

    fun saveOrderedProducts(orders: Orders) {
        FirebaseDatabase.getInstance().getReference("Admins/Orders/${orders.orderId}").setValue(orders)
    }

    // SharedPreferences
    fun savingCartItemCount(itemCount: Int) {
        sharedPreferences.edit().putInt("itemCount", itemCount).apply()
    }

    fun fetchTotalCartItemCount(): LiveData<Int> {
        val totalItemCount = MutableLiveData<Int>()
        totalItemCount.value = sharedPreferences.getInt("itemCount", 0)
        return totalItemCount
    }

    fun saveAddressStatus() {
        sharedPreferences.edit().putBoolean("addressStatus", true).apply()
    }

    fun getAddressStatus(): LiveData<Boolean> {
        val status = MutableLiveData<Boolean>()
        status.value = sharedPreferences.getBoolean("addressStatus", false)
        return status
    }
}
