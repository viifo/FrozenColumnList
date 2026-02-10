package com.viifo.frozencolumnlist.demo.ui

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.get
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.viifo.frozencolumnlist.demo.R
import com.viifo.frozencolumnlist.demo.databinding.ActivityMainBinding
import com.viifo.frozencolumnlist.demo.ui.adapter.MainPagerAdapter


class MainActivity : AppCompatActivity() {

    private val mBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(mBinding.root)
        ViewCompat.setOnApplyWindowInsetsListener(mBinding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, systemBars.top, v.paddingRight, systemBars.bottom)
            WindowInsetsCompat.Builder(insets)
                .setInsets(
                    WindowInsetsCompat.Type.systemBars(),
                    Insets.of(systemBars.left, 0, systemBars.right, 0)
                )
                .build()
        }
        initView()
    }

    private fun initView() {
        mBinding.viewPager2.adapter = MainPagerAdapter(this)
        mBinding.viewPager2.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                mBinding.bottomNavigation.menu[position].isChecked = true
            }
        })
        mBinding.bottomNavigation.setOnItemSelectedListener({ item ->
            val itemId: Int = item.itemId
            when (itemId) {
                R.id.nav_watchlist -> {
                    mBinding.viewPager2.setCurrentItem(0, true)
                }
                R.id.nav_market -> {
                    mBinding.viewPager2.setCurrentItem(1, true)
                }
                R.id.nav_profile -> {
                    mBinding.viewPager2.setCurrentItem(2, true)
                }
            }
            true
        })
    }

}