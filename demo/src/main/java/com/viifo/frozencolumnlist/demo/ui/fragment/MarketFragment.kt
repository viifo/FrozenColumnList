package com.viifo.frozencolumnlist.demo.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.viifo.frozencolumnlist.demo.databinding.FragementMarketBinding

class MarketFragment: Fragment() {

    private var mBinding: FragementMarketBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragementMarketBinding.inflate(
            inflater,
            container,
            false
        ).also { mBinding = it }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

}