package com.viifo.frozencolumnlist.demo.ui.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.viifo.frozencolumnlist.demo.ui.fragment.Watchlist2Fragment
import com.viifo.frozencolumnlist.demo.ui.fragment.Watchlist3Fragment
import com.viifo.frozencolumnlist.demo.ui.fragment.Watchlist1Fragment

class MainPagerAdapter(
    fragmentActivity: FragmentActivity
) : FragmentStateAdapter(fragmentActivity) {

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            1 -> Watchlist2Fragment()
            2 -> Watchlist3Fragment()
            else -> Watchlist1Fragment()
        }
    }

    override fun getItemCount() = 3

}