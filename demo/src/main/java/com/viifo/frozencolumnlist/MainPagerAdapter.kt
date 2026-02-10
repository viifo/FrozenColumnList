package com.viifo.frozencolumnlist

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class MainPagerAdapter(
    fragmentActivity: FragmentActivity
) : FragmentStateAdapter(fragmentActivity) {

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            1 -> MarketFragment()
            2 -> ProfileFragment()
            else -> WatchlistFragment()
        }
    }

    override fun getItemCount() = 3

}