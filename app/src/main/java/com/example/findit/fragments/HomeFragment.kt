package com.example.findit.fragments

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.findit.Constants
import com.example.findit.R
import com.example.findit.adapters.AdapterCaregory
import com.example.findit.databinding.FragmentHomeBinding
import com.example.findit.models.Category
import com.example.findit.viewmodels.UserViewModel


class HomeFragment : Fragment() {
    private val viewModel : UserViewModel by viewModels()
    private lateinit var binding : FragmentHomeBinding


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(layoutInflater)
        setStatusBarColor()
        setAllCategories()
        navigatingToSearchFragment()
        get()
        return binding.root
    }

    private fun get(){
        viewModel.getAll().observe(viewLifecycleOwner){
            for (i in it){
                Log.d("vvv", i.productTitle.toString())
                Log.d("vvv", i.productCount.toString())
            }
        }
    }

    private fun navigatingToSearchFragment() {
        binding.searchCv.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_searchFragment)
        }
    }

    private fun setAllCategories(){
        val categoryList = ArrayList<Category>()
        for(i in 0 until Constants.allProductCategoryIcon.size){
            categoryList.add(Category(Constants.allProductCategory[i], Constants.allProductCategoryIcon[i]))
        }

        binding.rvCategories.adapter = AdapterCaregory(categoryList, ::onCategoryIconClicked)

    }
    fun onCategoryIconClicked(category: Category){
        val bundle = Bundle()
        bundle.putString("category", category.title)
       findNavController().navigate(R.id.action_homeFragment_to_categoryFragment, bundle)
    }




    private fun setStatusBarColor() {
        activity?.window?.apply {
            // Set the status bar color to be fully transparent
            statusBarColor = android.graphics.Color.TRANSPARENT

            // Make sure the content doesn't resize when the system bars hide and show
            decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Optional: Keep light status bar if needed
                decorView.systemUiVisibility = decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
        }
    }


}